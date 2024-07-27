package com.kfels.shorturl.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class Text2Image {
    private static Random random = new Random();

    public static byte[] generate(String code) {
        return generate(code, 40, 200, 40);
    }

    public static byte[] generate(String code, int lines) {
        return generate(code, lines, 200, 40);
    }

    public static byte[] generate(String code, int lines, int width, int height) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = image.createGraphics();

        g2d.fillRect(0, 0, width, height);
        g2d.setFont(new Font("Times New Roman", Font.ROMAN_BASELINE, height - 4));
        g2d.setColor(getRandColor(110, 133));

        int margin = width / code.length();
        for (int i = 0; i < code.length(); ++i) {
            Font font = new Font("Fixedsys", Font.CENTER_BASELINE, height - 4);
            g2d.setFont(font);
            g2d.setColor(getRandColor(0, 100));
            g2d.translate(random.nextInt(3), random.nextInt(3));
            g2d.drawString(String.valueOf(code.charAt(i)), margin * i, (height * 3) / 4);
        }

        for (int i = 0; i <= lines; i++) {
            int x = random.nextInt((width * 3) / 4);
            int y = random.nextInt((height * 3) / 4);
            int xl = random.nextInt(80);
            int yl = random.nextInt(80);
            int h = random.nextInt(3) + 1;
            g2d.setColor(getRandColor(100, 200));
            g2d.setStroke(new BasicStroke(h));
            g2d.drawLine(x, y, x + xl, y + yl);
        }
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            System.out.println(e);
        }
        byte[] captchaImage = baos.toByteArray();
        return captchaImage;
    }

    private static Color getRandColor(int fc, int bc) {
        if (fc > 255)
            fc = 255;
        if (bc > 255)
            bc = 255;
        int r = fc + random.nextInt(bc - fc - 16);
        int g = fc + random.nextInt(bc - fc - 14);
        int b = fc + random.nextInt(bc - fc - 18);
        return new Color(r, g, b);
    }
}
