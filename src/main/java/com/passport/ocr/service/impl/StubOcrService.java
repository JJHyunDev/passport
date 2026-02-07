package com.passport.ocr.service.impl;

import com.passport.ocr.service.OcrService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "ocr.mode", havingValue = "stub")
public class StubOcrService implements OcrService {
  @Override
  public List<Map<String, String>> extract(List<MultipartFile> images) {
    List<Map<String, String>> results = new ArrayList<>();
    int index = 1;
    for (MultipartFile image : images) {
      Map<String, String> row = new HashMap<>();
      row.put("passportNo", "TEST" + index);
      row.put("name", "TEST USER");
      row.put("nationality", "UTO");
      row.put("dateOfBirth", "1974-08-12");
      row.put("sex", "F");
      row.put("dateOfIssue", "");
      row.put("dateOfExpiry", "2012-04-15");
      row.put("issuingCountry", "UTO");
      row.put("mrzRaw", "");
      row.put("error", "");
      results.add(row);
      index++;
    }
    return results;
  }
}
