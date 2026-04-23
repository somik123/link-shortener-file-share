package com.kfels.shorturl.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

public class Text2Image {

    private static final int width = 200;
    private static final int height = 40;
    private static final int lines = 40;

    private static final Random random = new Random();

    public static byte[] generate(String code) {
        return generate(code, lines, width, height);
    }

    public static byte[] generate(String code, int lines) {
        return generate(code, lines, width, height);
    }

    public static byte[] generate(String code, int lines, int width, int height) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Background
        g2d.setColor(new Color(245, 245, 245));
        g2d.fillRect(0, 0, width, height);

        // Text
        drawDistortedText(g2d, code, width, height);

        // Keep your simple line strokes
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
        return baos.toByteArray();
    }

    private static void drawDistortedText(Graphics2D g2d, String code, int width, int height) {
        if (code == null || code.isEmpty()) {
            return;
        }

        int len = code.length();
        int leftPadding = 10;
        int rightPadding = 10;
        int usableWidth = width - leftPadding - rightPadding;
        int step = usableWidth / len;

        Font baseFont = new Font("Fixedsys", Font.BOLD, height - 8);

        for (int i = 0; i < len; i++) {
            char ch = code.charAt(i);

            int fontSize = Math.max(18, (height - 8) + random.nextInt(5) - 2);
            Font font = baseFont.deriveFont((float) fontSize);
            g2d.setFont(font);

            FontMetrics fm = g2d.getFontMetrics(font);
            int charWidth = fm.charWidth(ch);
            int textHeight = fm.getAscent() + fm.getDescent();

            int baseX = leftPadding + (i * step) + (step - charWidth) / 2;
            int baseY = ((height - textHeight) / 2) + fm.getAscent();

            int jitterX = random.nextInt(5) - 2;
            int jitterY = random.nextInt(7) - 3;

            double angle = Math.toRadians(random.nextInt(25) - 12); // -12 to +12 deg
            double shearX = (random.nextDouble() - 0.5) * 0.5; // -0.25 to +0.25
            double scaleY = 0.9 + (random.nextDouble() * 0.25); // 0.9 to 1.15

            AffineTransform old = g2d.getTransform();

            g2d.setColor(getRandColor(0, 100));

            // Transform around each character's draw position
            g2d.translate(baseX + jitterX, baseY + jitterY);
            g2d.rotate(angle);
            g2d.shear(shearX, 0.0);
            g2d.scale(1.0, scaleY);

            g2d.drawString(String.valueOf(ch), 0, 0);

            g2d.setTransform(old);
        }
    }

    private static Color getRandColor(int fc, int bc) {
        if (fc > 255)
            fc = 255;
        if (bc > 255)
            bc = 255;
        if (bc <= fc + 20)
            bc = Math.min(255, fc + 21);

        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);

        return new Color(r, g, b);
    }
}