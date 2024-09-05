package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

    private static void drawLine(BufferedImage image, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            image.setRGB(y1, x1, color);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    public static void main(String[] args) {
        // get images from resources
        BufferedImage image1;
        try {
            InputStream is = App.class.getResourceAsStream("/picture1.png");
            image1 = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);  
        }
        BufferedImage image2;
        try {
            InputStream is = App.class.getResourceAsStream("/picture2.png");
            image2 = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);  
        }

        // use benchmarking
        Benchmark benchmark = new Benchmark();
        benchmark.mark();

        // get height and width
        int width1 = image1.getWidth();
        int height1 = image1.getHeight();
        int width2 = image2.getWidth();
        int height2 = image2.getHeight();

        // set image values to grayscale and update grayscale array
        int[][] grayImage1 = new int[height1][width1];
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                int gray = toGrayscale(image1.getRGB(j, i));
                grayImage1[i][j] = gray;
            }
        }
        int[][] grayImage2 = new int[height2][width2];
        for (int i = 0; i < height2; i++) {
            for (int j = 0; j < width2; j++) {
                int gray = toGrayscale(image2.getRGB(j, i));
                grayImage2[i][j] = gray;
            }
        }

        // run image detection
        benchmark.mark();
        FeatureInfo infoA = Detection.detect(image1, grayImage1);
        benchmark.mark();
        benchmark.print("cornerA detection");
        FeatureInfo infoB = Detection.detect(image2, grayImage2);
        benchmark.mark();
        benchmark.print("cornerB detection");

        // run feature matching
        ArrayList<Feature[]> matches = Tracking.track(infoA, infoB);
        benchmark.mark();
        benchmark.print("feature matching");

        // get total benchmark
        benchmark.mark();
        benchmark.total();

        // visualize matches
        for (Feature[] match : matches) {
            Feature a = match[0];
            Feature b = match[1];
            drawLine(image1, a.x, a.y, b.x, b.y, 0x0000FF);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    image1.setRGB(a.y + j, a.x + i, 0xFF0000);
                    image1.setRGB(b.y + j, b.x + i, 0x00FF00);
                }
            }
        }

        // write image to file
        try {
            File file = new File("output.png");
            ImageIO.write(image1, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

