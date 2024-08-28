package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class App {
    static int[][] ixx;
    static int[][] iyy;
    static int[][] ixy;
    static int[] gxx;
    static int[] gyy;
    static int[] gxy;
    static int[][] s;

    /// The row count, which starts at 1 to avoid the first row of the image.
    static int row = 1;

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

    /**
     * Calculates the derivative of the pixel intensity in the vertical direction.
     *
     * @param image the grayscale image as a 2D array of integers.
     * @param x the x-coordinate of the pixel.
     * @param y the y-coordinate of the pixel.
     *
     * @return the derivative of the pixel intensity in the vertical direction.
     */
    private static int getXDerivative(int[][] image, int x, int y) {
        return image[x + 1][y] - image[x - 1][y];
    }

    /**
     * Calculates the derivative of the pixel intensity in the horizontal direction.
     *
     * @param image the grayscale image as a 2D array of integers.
     * @param x the x-coordinate of the pixel.
     * @param y the y-coordinate of the pixel.
     *
     * @return the derivative of the pixel intensity in the horizontal direction.
     */
    private static int getYDerivative(int[][] image, int x, int y) {
        return image[x][y + 1] - image[x][y - 1];
    }

    /**
     * Updates the derivative buffers with the derivatives of the current row of the image.
     *
     * @param image the grayscale image as a 2D array of integers.
     */
    private static void updateDerivatives(int[][] image) {
        for (int i = 1; i < image[0].length - 1; i++) {
            int ix = getXDerivative(image, row, i);
            int iy = getYDerivative(image, row, i);
            int x = (row - 1) % 5;
            ixx[x][i - 1] = ix * ix;
            iyy[x][i - 1] = iy * iy;
            ixy[x][i - 1] = ix * iy;
        }
        row++;
    }

    private static void updateGaussian() {
        int[] gaussian = { 1, 4, 6, 4, 1 };
        for (int i = 0; i < gxx.length; i++) {
            gxx[i] += ixx[(row + 1) % 5][i];
            gxx[i] += ixx[(row + 2) % 5][i] << 2;
            gxx[i] += (ixx[(row + 3) % 5][i] << 2 + ixx[(row + 3) % 5][i]) << 2;
            gxx[i] += ixx[(row + 4) % 5][i] << 2;
            gxx[i] += ixx[(row + 5) % 5][i];
        }
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

        // get image width and length and init grayscale and strength arrays
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] grayImage = new int[height][width];
        s = new int[height - 6][width - 6];

        // initialize derivative buffers 
        ixx = new int[5][width - 2];
        iyy = new int[5][width - 2];
        ixy = new int[5][width - 2];

        // initialize autocorrelation buffers
        gxx = new int[width - 2];
        gyy = new int[width - 2];
        gxy = new int[width - 2];

        // set image values to grayscale and update grayscale array
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int gray = toGrayscale(image.getRGB(i, j));
                grayImage[i][j] = gray;
                Color grayColor = new Color(gray, gray, gray);
                image.setRGB(i, j, grayColor.getRGB());
            }
        }

        // initially calculate first 5 lines of derivatives
        for (int i = 0; i < 5; i++) {
            updateDerivatives(grayImage);
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
