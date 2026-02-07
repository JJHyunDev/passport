package com.passport.ocr.web;

import com.passport.ocr.config.ExcelProperties;
import com.passport.ocr.config.OcrProperties;
import com.passport.ocr.service.ExcelExportService;
import com.passport.ocr.service.OcrService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {
  private final OcrService ocrService;
  private final ExcelExportService excelExportService;
  private final ExcelProperties excelProperties;
  private final OcrProperties ocrProperties;

  public OcrController(
      OcrService ocrService,
      ExcelExportService excelExportService,
      ExcelProperties excelProperties,
      OcrProperties ocrProperties
  ) {
    this.ocrService = ocrService;
    this.excelExportService = excelExportService;
    this.excelProperties = excelProperties;
    this.ocrProperties = ocrProperties;
  }

  @PostMapping(value = "/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> export(@RequestPart("images") List<MultipartFile> images) {
    validateImages(images);
    List<Map<String, String>> records = ocrService.extract(images);
    byte[] excelBytes = excelExportService.export(records);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename("passport_export.xlsx", StandardCharsets.UTF_8)
        .build();
    headers.setContentDisposition(disposition);

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }

  @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public OcrPreviewResponse preview(@RequestPart("images") List<MultipartFile> images) {
    validateImages(images);
    List<Map<String, String>> records = ocrService.extract(images);
    List<ColumnDef> columns = excelProperties.getColumns().stream()
        .map(column -> new ColumnDef(column.getKey(), column.getLabel()))
        .collect(Collectors.toList());
    return new OcrPreviewResponse(columns, records);
  }

  private void validateImages(List<MultipartFile> images) {
    if (images == null || images.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "images is required");
    }
    if (images.size() > ocrProperties.getMaxImages()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Too many images. Max: " + ocrProperties.getMaxImages());
    }
  }

  public record ColumnDef(String key, String label) {}

  public record OcrPreviewResponse(List<ColumnDef> columns, List<Map<String, String>> records) {}
}
