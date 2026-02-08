package com.passport.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private boolean autoOpenBrowser = true;
  private String uiUrl = "";

  public boolean isAutoOpenBrowser() {
    return autoOpenBrowser;
  }

  public void setAutoOpenBrowser(boolean autoOpenBrowser) {
    this.autoOpenBrowser = autoOpenBrowser;
  }

  public String getUiUrl() {
    return uiUrl;
  }

  public void setUiUrl(String uiUrl) {
    this.uiUrl = uiUrl;
  }
}
