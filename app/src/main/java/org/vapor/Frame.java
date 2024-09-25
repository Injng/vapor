package org.vapor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Represents two paired images in the stereo camera system making up one frame.
 */
public class Frame {
    /** The source images for the left and right cameras. */
    public BufferedImage leftImage;
    public BufferedImage rightImage;

    /** The dimensions of the left and right images. */
    private int leftWidth;
    private int leftHeight;
    private int rightWidth;
    private int rightHeight;

    /** The grayscale values of the left and right images. */
    public int[][] leftGrayImage;
    public int[][] rightGrayImage;

    public Frame(String left, String right) {
        // get left image from resources
        BufferedImage image1;
        try {
            InputStream is = App.class.getResourceAsStream("/left");
            assert is != null;
            image1 = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        leftImage = image1;
        leftWidth = leftImage.getWidth();
        leftHeight = leftImage.getHeight();

        // get right image from resources
        BufferedImage image2;
        try {
            InputStream is = App.class.getResourceAsStream("/right");
            assert is != null;
            image2 = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        rightImage = image2;
        rightWidth = rightImage.getWidth();
        rightHeight = rightImage.getHeight();

        // set image values to grayscale and update grayscale array
        for (int i = 0; i < leftHeight; i++) {
            for (int j = 0; j < leftWidth; j++) {
                int gray = toGrayscale(leftImage.getRGB(j, i));
                leftGrayImage[i][j] = gray;
            }
        }
        for (int i = 0; i < rightHeight; i++) {
            for (int j = 0; j < rightWidth; j++) {
                int gray = toGrayscale(rightImage.getRGB(j, i));
                rightGrayImage[i][j] = gray;
            }
        }
    }

    /**
     * Converts a color pixel value to a grayscale intensity.
     *
     * @param rgb the RGB color value as an integer, where the red component is in the high byte, followed by green,
     *            and blue in the low byte. This value should be obtained from methods like {@link java.awt.image.BufferedImage#getRGB(int, int)}.
     *
     * @return the grayscale value as an integer. This value represents the average of the red, green, and blue
     *         components of the input RGB value, which ranges from 0 (black) to 255 (white).
     */
    private int toGrayscale(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb & 0xFF);
        return (r + g + b) / 3;
    }
}
