package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a feature in an image with its strength value and coordinates.
 */
class Feature {
    /** The strength value of the feature. */
    double value;

    /** The x-coordinate of the feature in the image. */
    int x;

    /** The y-coordinate of the feature in the image. */
    int y;

    /**
     * Constructs a new Feature with the specified strength value and coordinates.
     *
     * @param value The strength value of the feature.
     * @param x The x-coordinate of the feature in the image.
     * @param y The y-coordinate of the feature in the image.
     */
    Feature(double value, int x, int y) {
        this.value = value;
        this.x = x;
        this.y = y;
    }
}

/**
 * Comparator for sorting Feature objects based on their strength value in descending order.
 */
class SortStrength implements Comparator<Feature> {
    /**
     * Compares two Feature objects based on their strength values.
     *
     * @param a the first Feature to be compared.
     * @param b the second Feature to be compared.
     * @return a negative integer, zero, or a positive integer as the first argument
     *         is less than, equal to, or greater than the second.
     */
    public int compare(Feature a, Feature b) {
        return Double.compare(b.value, a.value);
    }
}

public class App {
    static int[][] ixx;
    static int[][] iyy;
    static int[][] ixy;
    static int[] gxx;
    static int[] gyy;
    static int[] gxy;
    static double[][] s;

    /** The row count, which starts at 1 to avoid the first row of the image. */
    static int row = 1;

    /** The constant k in the Harris corner response function. */
    static final double k = 0.06;

    /** The cap on the number of features in each of the 50 buckets. */
    static final int bucketCap = 100;

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

    /**
     * Updates the Gaussian buffers with the current derivatives stored in the buffers.
     */
    private static void updateGaussian() {
        // perform vertical convolution with matrix [1, 4, 6, 4, 1]
        for (int i = 0; i < gxx.length; i++) {
            gxx[i] += ixx[(row - 1) % 5][i];
            gxx[i] += ixx[row % 5][i] << 2;
            gxx[i] += (ixx[(row + 1) % 5][i] << 2 + ixx[(row + 3) % 5][i]) << 2;
            gxx[i] += ixx[(row + 2) % 5][i] << 2;
            gxx[i] += ixx[(row + 3) % 5][i];

            gyy[i] += iyy[(row - 1) % 5][i];
            gyy[i] += iyy[row % 5][i] << 2;
            gyy[i] += (iyy[(row + 1) % 5][i] << 2 + iyy[(row + 3) % 5][i]) << 2;
            gyy[i] += iyy[(row + 2) % 5][i] << 2;
            gyy[i] += iyy[(row + 3) % 5][i];

            gxy[i] += ixy[(row - 1) % 5][i];
            gxy[i] += ixy[row % 5][i] << 2;
            gxy[i] += (ixy[(row + 1) % 5][i] << 2 + ixy[(row + 3) % 5][i]) << 2;
            gxy[i] += ixy[(row + 2) % 5][i] << 2;
            gxy[i] += ixy[(row + 3) % 5][i];
        }

        // perform horizontal convolution with matrix [1, 4, 6, 4, 1]
        for (int i = 0; i < gxx.length - 4; i++) {
            gxx[i] += gxx[i + 1] << 2;
            gxx[i] += (gxx[i + 2] << 2 + gxx[i + 2]) << 2;
            gxx[i] += gxx[i + 3] << 2;
            gxx[i] += gxx[i + 4];

            gyy[i] += gyy[i + 1] << 2;
            gyy[i] += (gyy[i + 2] << 2 + gyy[i + 2]) << 2;
            gyy[i] += gyy[i + 3] << 2;
            gyy[i] += gyy[i + 4];

            gxy[i] += gxy[i + 1] << 2;
            gxy[i] += (gxy[i + 2] << 2 + gxy[i + 2]) << 2;
            gxy[i] += gxy[i + 3] << 2;
            gxy[i] += gxy[i + 4];
        }
    }

    /*
     * Updates the strength array with the current Gaussian buffers.
     */
    private static void updateStrength() {
        for (int i = 0; i < gxx.length - 4; i++) {
            int det = gxx[i] * gyy[i] - gxy[i] * gxy[i];
            int trace = gxx[i] + gyy[i];
            double strength = det - k * trace * trace;
            s[row - 6][i] = strength;
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
        s = new double[height - 6][width - 6];

        // initialize derivative buffers 
        ixx = new int[5][width - 2];
        iyy = new int[5][width - 2];
        ixy = new int[5][width - 2];

        // initialize autocorrelation buffers
        gxx = new int[width - 2];
        gyy = new int[width - 2];
        gxy = new int[width - 2];

        // set image values to grayscale and update grayscale array
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int gray = toGrayscale(image.getRGB(j, i));
                grayImage[i][j] = gray;
                Color grayColor = new Color(gray, gray, gray);
                image.setRGB(j, i, grayColor.getRGB());
            }
        }

        // initially calculate first 5 lines of derivatives
        for (int i = 0; i < 5; i++) {
            updateDerivatives(grayImage);
        }

        // loop to calculate derivatives and strengths
        for (int i = 5; i < height - 6; i++) {
            updateGaussian();
            updateStrength();
            updateDerivatives(grayImage);
        }

        // initialize list of features
        ArrayList<ArrayList<Feature>> features = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            features.add(new ArrayList<Feature>());
        }

        // use non-max suppression to declare features, split into 50 buckets
        int rowBound = s.length / 5;
        int colBound = s[0].length / 10;
        for (int i = 2; i < s.length - 2; i++) {
            int rowBucket = (int) (i / rowBound) > 4 ? 4 : (int) (i / rowBound); 
            for (int j = 2; j < s[0].length - 2; j++) {
                double v = s[i][j];
                
                // if strength is greater than all other strengths in a 5x5 window
                if (v > s[i-2][j-2] && v > s[i-2][j-1] && v > s[i-2][j] && v > s[i-2][j+1] && v > s[i-2][j+2] &&
                    v > s[i-1][j-2] && v > s[i-1][j-1] && v > s[i-1][j] && v > s[i-1][j+1] && v > s[i-1][j+2] &&
                    v > s[i][j-2] && v > s[i][j-1] && v > s[i][j+1] && v > s[i][j+2] &&
                    v > s[i+1][j-2] && v > s[i+1][j-1] && v > s[i+1][j] && v > s[i+1][j+1] && v > s[i+1][j+2] &&
                    v > s[i+2][j-2] && v > s[i+2][j-1] && v > s[i+2][j] && v > s[i+2][j+1] && v > s[i+2][j+2]) {
                    int colBucket = (int) (j / colBound) > 9 ? 9 : (int) (j / colBound);
                    int bucket = rowBucket * 10 + colBucket;
                    features.get(bucket).add(new Feature(v, i, j));
                    j += 2;
                }
            }
        }

        // sort features in each bucket by strength and use top features
        for (int i = 0; i < 50; i++) {
            features.get(i).sort(new SortStrength());
            int top = features.get(i).size() > bucketCap ? bucketCap : features.get(i).size();
            features.set(i, new ArrayList<Feature>(features.get(i).subList(0, top)));
        }

        // draw features on image
        for (int i = 0; i < 50; i++) {
            for (Feature f : features.get(i)) {
                for (int j = f.x - 2; j <= f.x + 2; j++) {
                    for (int k = f.y - 2; k <= f.y + 2; k++) {
                        image.setRGB(k, j, Color.RED.getRGB());
                    }
                }
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
