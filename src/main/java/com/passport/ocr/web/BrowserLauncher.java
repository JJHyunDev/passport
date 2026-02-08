package com.passport.ocr.web;

import com.passport.ocr.config.AppProperties;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BrowserLauncher {
  private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

  private final AppProperties appProperties;
  private final Environment environment;

  public BrowserLauncher(AppProperties appProperties, Environment environment) {
    this.appProperties = appProperties;
    this.environment = environment;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void openBrowser() {
    if (!appProperties.isAutoOpenBrowser()) {
      return;
    }

    String url = appProperties.getUiUrl();
    if (url == null || url.isBlank()) {
      String port = environment.getProperty("server.port", "8080");
      url = "http://localhost:" + port + "/";
    }

    try {
      if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
        log.info("UI available at {}", url);
        return;
      }
      Desktop.getDesktop().browse(URI.create(url));
      log.info("Opened browser at {}", url);
    } catch (Exception e) {
      log.warn("Failed to open browser. UI available at {}", url, e);
    }
  }
}
