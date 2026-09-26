package com.kfels.shorturl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class Text2ImageTests {

    @Test
    void rendersDefaultAndCustomPngDimensions() throws Exception {
        byte[] defaultImage = Text2Image.generate("CODE");
        assertEquals(200, ImageIO.read(new ByteArrayInputStream(defaultImage)).getWidth());
        assertEquals(40, ImageIO.read(new ByteArrayInputStream(defaultImage)).getHeight());

        byte[] fewerLines = Text2Image.generate("", 0);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(fewerLines)));

        byte[] customImage = Text2Image.generate("A", 1, 180, 60);
        assertEquals(180, ImageIO.read(new ByteArrayInputStream(customImage)).getWidth());
        assertEquals(60, ImageIO.read(new ByteArrayInputStream(customImage)).getHeight());
    }
}
