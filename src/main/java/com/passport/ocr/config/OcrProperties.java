package com.passport.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {
  private int maxImages = 50;
  private int timeoutSeconds = 180;
  private Worker worker = new Worker();

  public int getMaxImages() {
    return maxImages;
  }

  public void setMaxImages(int maxImages) {
    this.maxImages = maxImages;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public Worker getWorker() {
    return worker;
  }

  public void setWorker(Worker worker) {
    this.worker = worker;
  }

  public static class Worker {
    private String python = "python";
    private String script = "ocr-worker/paddle_ocr_worker.py";
    private String tempDir = "";
    private int maxSide = 2000;
    private String modelDir = "ocr-models";

    public String getPython() {
      return python;
    }

    public void setPython(String python) {
      this.python = python;
    }

    public String getScript() {
      return script;
    }

    public void setScript(String script) {
      this.script = script;
    }

    public String getTempDir() {
      return tempDir;
    }

    public void setTempDir(String tempDir) {
      this.tempDir = tempDir;
    }

    public int getMaxSide() {
      return maxSide;
    }

    public void setMaxSide(int maxSide) {
      this.maxSide = maxSide;
    }

    public String getModelDir() {
      return modelDir;
    }

    public void setModelDir(String modelDir) {
      this.modelDir = modelDir;
    }
  }
}
