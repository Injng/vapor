package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class App {
    /**
     * Converts a color pixel value to a grayscale intensity.
     * 
     * @param rgb the RGB color value as an integer, where the red component is in the high byte, followed by green,
     *            and blue in the low byte. This value should be obtained from methods like {@link java.awt.image.BufferedImage#getRGB(int, int)}.
     * @return the grayscale value as an integer. This value represents the average of the red, green, and blue
     *         components of the input RGB value, which ranges from 0 (black) to 255 (white).
     */
    private static int toGrayscale(int rgb) {
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

        // get height and width
        int width = image.getWidth();
        int height = image.getHeight();

        // set image values to grayscale and update grayscale array
        int[][] grayImage = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int gray = toGrayscale(image.getRGB(j, i));
                grayImage[i][j] = gray;
            }
        }

        // run image detection
        FeatureInfo infoA = Detection.detect(image, grayImage);
    }
}

