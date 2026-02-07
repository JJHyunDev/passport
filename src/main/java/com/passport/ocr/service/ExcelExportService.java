package com.passport.ocr.service;

import com.passport.ocr.config.ExcelProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {
  private final ExcelProperties excelProperties;

  public ExcelExportService(ExcelProperties excelProperties) {
    this.excelProperties = excelProperties;
  }

  public byte[] export(List<Map<String, String>> records) {
    try (Workbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("passports");

      Row header = sheet.createRow(0);
      List<ExcelProperties.Column> columns = excelProperties.getColumns();
      for (int i = 0; i < columns.size(); i++) {
        ExcelProperties.Column column = columns.get(i);
        Cell cell = header.createCell(i);
        cell.setCellValue(nullToEmpty(column.getLabel()));
      }

      int rowIndex = 1;
      for (Map<String, String> record : records) {
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < columns.size(); i++) {
          ExcelProperties.Column column = columns.get(i);
          Cell cell = row.createCell(i);
          String value = record.get(column.getKey());
          cell.setCellValue(nullToEmpty(value));
        }
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate Excel file", e);
    }
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
