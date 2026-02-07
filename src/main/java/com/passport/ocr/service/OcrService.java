package com.passport.ocr.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
  List<Map<String, String>> extract(List<MultipartFile> images);
}
