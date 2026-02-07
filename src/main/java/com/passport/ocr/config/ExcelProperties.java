package com.passport.ocr.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "excel")
public class ExcelProperties {
  private List<Column> columns = new ArrayList<>();

  public List<Column> getColumns() {
    return columns;
  }

  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public static class Column {
    private String key;
    private String label;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }
  }
}
