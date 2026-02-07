package com.passport.ocr.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"ocr.timeout-seconds=30", "ocr.mode=stub"})
@AutoConfigureMockMvc
class OcrControllerIntegrationTest {
  private static final String MRZ_LINE1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<";
  private static final String MRZ_LINE2 = "L898902C36UTO7408122F1204159ZE184226B<<<<<10";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void previewEndpointReturnsJson() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "images",
        "mrz.png",
        "image/png",
        generateMrzImage()
    );

    mockMvc.perform(multipart("/api/ocr/preview").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.columns").exists())
        .andExpect(jsonPath("$.records").exists());
  }

  private byte[] generateMrzImage() throws Exception {
    BufferedImage image = new BufferedImage(1200, 300, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setFont(new Font("Monospaced", Font.PLAIN, 28));
    graphics.drawString(MRZ_LINE1, 20, 120);
    graphics.drawString(MRZ_LINE2, 20, 220);
    graphics.dispose();

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
