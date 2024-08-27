package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class App {
    int[][] ixx;
    int[][] iyy;
    int[][] ixy;

    private static int toGrayscale (int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        return (r + g + b) / 3;
    }

    public static void main(String[] args) {
        // get image from resources
        BufferedImage image;
        try {
            InputStream is = App.class.getResourceAsStream("/picture.png");
            image = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);  
        }

        // get image width and length
        int width = image.getWidth();
        int height = image.getHeight();

        // set image values to grayscale
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int gray = toGrayscale(image.getRGB(i, j));
                Color grayColor = new Color(gray, gray, gray);
                image.setRGB(i, j, grayColor.getRGB());
            }
        }

        // save new grayscale image
        try {
            File file = new File("grayscale.png");
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
