package com.passport.ocr.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passport.ocr.config.OcrProperties;
import com.passport.ocr.mrz.MrzParser;
import com.passport.ocr.mrz.MrzResult;
import com.passport.ocr.service.OcrService;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "ocr.mode", havingValue = "paddle", matchIfMissing = true)
public class PaddleOcrService implements OcrService {
  private final OcrProperties ocrProperties;
  private final ObjectMapper objectMapper;
  private final MrzParser mrzParser = new MrzParser();

  public PaddleOcrService(OcrProperties ocrProperties, ObjectMapper objectMapper) {
    this.ocrProperties = ocrProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<Map<String, String>> extract(List<MultipartFile> images) {
    List<Path> tempFiles = new ArrayList<>();
    try {
      for (MultipartFile image : images) {
        tempFiles.add(writeTempFile(image));
      }

      WorkerResponse response = runWorker(tempFiles);
      Map<Integer, WorkerResult> resultMap = new HashMap<>();
      for (WorkerResult result : response.results()) {
        resultMap.put(result.id(), result);
      }

      List<Map<String, String>> records = new ArrayList<>();
      for (int i = 0; i < images.size(); i++) {
        WorkerResult workerResult = resultMap.get(i);
        Map<String, String> record = new HashMap<>();
        if (workerResult == null) {
          record.put("error", "OCR worker returned no result");
          records.add(record);
          continue;
        }

        if (workerResult.error() != null && !workerResult.error().isBlank()) {
          record.put("error", workerResult.error());
          record.put("mrzRaw", "");
          records.add(record);
          continue;
        }

        Optional<MrzResult> parsed = mrzParser.parse(workerResult.lines());
        if (parsed.isEmpty()) {
          record.put("error", "MRZ not found");
          record.put("mrzRaw", String.join("\n", mrzParser.collectCandidates(workerResult.lines())));
          records.add(record);
          continue;
        }

        MrzResult mrz = parsed.get();
        record.put("passportNo", mrz.passportNumber());
        record.put("name", mrz.fullName());
        record.put("nationality", mrz.nationality());
        record.put("dateOfBirth", mrz.dateOfBirth());
        record.put("sex", mrz.sex());
        record.put("dateOfIssue", "");
        record.put("dateOfExpiry", mrz.dateOfExpiry());
        record.put("issuingCountry", mrz.issuingCountry());
        record.put("mrzRaw", mrz.raw());
        record.put("error", "");
        records.add(record);
      }

      return records;
    } finally {
      cleanup(tempFiles);
    }
  }

  private Path writeTempFile(MultipartFile image) {
    try {
      String extension = "";
      String originalName = image.getOriginalFilename();
      if (originalName != null && originalName.contains(".")) {
        extension = originalName.substring(originalName.lastIndexOf('.'));
      }
      Path tempDir = resolveTempDir();
      Path tempFile = Files.createTempFile(tempDir, "passport-", extension);
      image.transferTo(tempFile.toFile());
      return tempFile;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write temp image", e);
    }
  }

  private Path resolveTempDir() {
    String configured = ocrProperties.getWorker().getTempDir();
    try {
      if (configured != null && !configured.isBlank()) {
        Path dir = Paths.get(configured);
        Files.createDirectories(dir);
        return dir;
      }
      return Paths.get(System.getProperty("java.io.tmpdir"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to resolve temp directory", e);
    }
  }

  private WorkerResponse runWorker(List<Path> tempFiles) {
    List<WorkerImage> images = new ArrayList<>();
    for (int i = 0; i < tempFiles.size(); i++) {
      images.add(new WorkerImage(i, tempFiles.get(i).toString()));
    }

    WorkerRequest request = new WorkerRequest(images, ocrProperties.getWorker().getMaxSide());
    String payload;
    try {
      payload = objectMapper.writeValueAsString(request);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize OCR request", e);
    }

    Path scriptPath = resolveWorkerScript();
    ProcessBuilder builder = new ProcessBuilder(
        resolvePythonCommand(),
        scriptPath.toString()
    );
    builder.redirectErrorStream(true);
    configureWorkerEnvironment(builder);

    try {
      Process process = builder.start();
      try (BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
        writer.write(payload);
      }

      boolean finished = process.waitFor(ocrProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("OCR worker timeout after " + ocrProperties.getTimeoutSeconds() + "s");
      }

      String stdout = readAll(process.getInputStream());
      if (process.exitValue() != 0) {
        throw new IllegalStateException("OCR worker failed: " + stdout);
      }

      return objectMapper.readValue(stdout, WorkerResponse.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OCR worker interrupted", e);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to run OCR worker", e);
    }
  }

  private Path resolveWorkerScript() {
    Path script = Paths.get(ocrProperties.getWorker().getScript());
    if (!script.isAbsolute()) {
      return resolveBaseDir().resolve(script).normalize();
    }
    return script;
  }

  private void configureWorkerEnvironment(ProcessBuilder builder) {
    Map<String, String> env = builder.environment();
    Path modelDir = resolveModelDir();
    ensureDirectory(modelDir);
    env.put("PADDLEOCR_HOME", modelDir.toString());
    env.put("PADDLE_DOWNLOAD_HOME", modelDir.toString());

    Path mplDir = resolveTempDir().resolve("matplotlib");
    ensureDirectory(mplDir);
    env.put("MPLCONFIGDIR", mplDir.toString());
  }

  private Path resolveModelDir() {
    String configured = ocrProperties.getWorker().getModelDir();
    if (configured != null && !configured.isBlank()) {
      Path dir = Paths.get(configured);
      if (!dir.isAbsolute()) {
        return resolveBaseDir().resolve(dir).normalize();
      }
      return dir;
    }
    return resolveTempDir().resolve("paddleocr");
  }

  private String resolvePythonCommand() {
    String python = ocrProperties.getWorker().getPython();
    if (python == null || python.isBlank()) {
      return "python";
    }
    Path candidate = Paths.get(python);
    if (!candidate.isAbsolute()) {
      Path resolved = resolveBaseDir().resolve(candidate).normalize();
      if (Files.exists(resolved)) {
        return resolved.toString();
      }
    }
    return python;
  }

  private Path resolveBaseDir() {
    ApplicationHome home = new ApplicationHome(PaddleOcrService.class);
    if (home.getDir() != null) {
      return home.getDir().toPath();
    }
    return Paths.get(System.getProperty("user.dir"));
  }

  private String readAll(InputStream inputStream) throws IOException {
    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
  }

  private void cleanup(List<Path> tempFiles) {
    for (Path path : tempFiles) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ignored) {
      }
    }
  }

  private void ensureDirectory(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create directory: " + path, e);
    }
  }

  public record WorkerImage(int id, String path) {}

  public record WorkerRequest(List<WorkerImage> images, int max_side) {}

  public record WorkerResult(int id, List<String> lines, String error) {}

  public record WorkerResponse(List<WorkerResult> results) {}
}
