package com.passport.ocr;

import com.passport.ocr.config.ExcelProperties;
import com.passport.ocr.config.OcrProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = {ExcelProperties.class, OcrProperties.class})
public class PassportOcrApplication {
  public static void main(String[] args) {
    SpringApplication.run(PassportOcrApplication.class, args);
  }
}
