package com.kfels.shorturl.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class Text2Image {
    private static Random random = new Random();
    private static int width = 200;
    private static int height = 40;
    private static int lines = 80;

    public static byte[] generate(String code) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = image.createGraphics();

        g2d.fillRect(0, 0, width, height);
        g2d.setFont(new Font("Times New Roman", Font.ROMAN_BASELINE, 36));
        g2d.setColor(getRandColor(110, 133));

        int margin = width / code.length();
        for (int i = 0; i < code.length(); ++i) {
            Font font = new Font("Fixedsys", Font.CENTER_BASELINE, 36);
            g2d.setFont(font);
            g2d.setColor(getRandColor(0, 100));
            g2d.translate(random.nextInt(3), random.nextInt(3));
            g2d.drawString(String.valueOf(code.charAt(i)), margin * i, 30);
        }

        for (int i = 0; i <= lines; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int xl = random.nextInt(20);
            int yl = random.nextInt(20);
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
