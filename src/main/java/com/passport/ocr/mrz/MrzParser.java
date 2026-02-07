package com.passport.ocr.mrz;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MrzParser {
  private static final int TD3_LENGTH = 44;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public Optional<MrzResult> parse(List<String> lines) {
    List<String> cleaned = collectCandidates(lines);
    if (cleaned.size() < 2) {
      return Optional.empty();
    }

    String line1 = normalizeToTd3(cleaned.get(0));
    String line2 = normalizeToTd3(cleaned.get(1));

    if (line1.length() != TD3_LENGTH || line2.length() != TD3_LENGTH) {
      return Optional.empty();
    }

    String issuingCountry = safeSubstring(line1, 2, 5).replace("<", "");
    String nameRaw = safeSubstring(line1, 5, TD3_LENGTH);
    String fullName = parseName(nameRaw);

    String passportNumber = safeSubstring(line2, 0, 9).replace("<", "");
    String nationality = safeSubstring(line2, 10, 13).replace("<", "");
    String birthRaw = safeSubstring(line2, 13, 19);
    String sex = safeSubstring(line2, 20, 21).replace("<", "");
    String expiryRaw = safeSubstring(line2, 21, 27);

    String birthDate = formatDate(birthRaw);
    String expiryDate = formatDate(expiryRaw);

    String raw = line1 + "\n" + line2;

    return Optional.of(new MrzResult(
        passportNumber,
        fullName,
        nationality,
        birthDate,
        sex,
        expiryDate,
        issuingCountry,
        raw
    ));
  }

  private static String normalize(String input) {
    String upper = input.toUpperCase(Locale.ROOT);
    String normalized = upper.replace(' ', '<');
    return normalized.replaceAll("[^A-Z0-9<]", "");
  }

  private static int scoreLine(String line) {
    int score = line.length();
    if (line.startsWith("P<")) {
      score += 50;
    }
    for (char c : line.toCharArray()) {
      if (c == '<') {
        score += 2;
      }
    }
    return score;
  }

  public List<String> collectCandidates(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return List.of();
    }

    List<String> cleaned = new ArrayList<>();
    for (String line : lines) {
      if (line == null) {
        continue;
      }
      String normalized = normalize(line);
      if (normalized.length() >= 30) {
        cleaned.add(normalized);
      }
    }

    if (cleaned.isEmpty()) {
      return List.of();
    }

    cleaned.sort(Comparator.comparingInt(MrzParser::scoreLine).reversed());
    if (cleaned.size() > 2) {
      return cleaned.subList(0, 2);
    }
    return cleaned;
  }

  private static String normalizeToTd3(String line) {
    if (line.length() > TD3_LENGTH) {
      return line.substring(0, TD3_LENGTH);
    }
    if (line.length() < TD3_LENGTH) {
      return line + "<".repeat(TD3_LENGTH - line.length());
    }
    return line;
  }

  private static String safeSubstring(String value, int start, int end) {
    if (value.length() <= start) {
      return "";
    }
    int safeEnd = Math.min(end, value.length());
    return value.substring(start, safeEnd);
  }

  private static String parseName(String raw) {
    String[] parts = raw.split("<<");
    String surname = parts.length > 0 ? parts[0].replace("<", " ").trim() : "";
    String given = parts.length > 1 ? parts[1].replace("<", " ").trim() : "";
    if (!given.isEmpty()) {
      return surname + " " + given;
    }
    return surname;
  }

  private static String formatDate(String yyMMdd) {
    if (yyMMdd == null || yyMMdd.length() != 6 || !yyMMdd.matches("\\d{6}")) {
      return yyMMdd == null ? "" : yyMMdd;
    }
    int yy = Integer.parseInt(yyMMdd.substring(0, 2));
    int mm = Integer.parseInt(yyMMdd.substring(2, 4));
    int dd = Integer.parseInt(yyMMdd.substring(4, 6));

    int currentYear = Year.now().getValue() % 100;
    int century = (yy <= currentYear + 1) ? 2000 : 1900;
    LocalDate date = LocalDate.of(century + yy, mm, dd);
    return DATE_FORMAT.format(date);
  }
}
