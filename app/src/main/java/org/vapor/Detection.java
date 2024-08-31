package org.vapor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

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

public class Detection {
    // the derivative, strength, and gaussian buffers.
    static long[][] ixx;
    static long[][] iyy;
    static long[][] ixy;
    static long[] gxx;
    static long[] gyy;
    static long[] gxy;
    static double[][] s;

    /** The grayscale values of the buffered image. */
    static int[][] grayImage;

    /** The row count, which starts at 1 to avoid the first row of the image. */
    static int row = 1;

    /** The constant k in the Harris corner response function. */
    static final double k = 0.06;

    /** The cap on the number of features in each of the 50 buckets. */
    static final int bucketCap = 100;
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

    private static final int[] WEIGHTS = {1, 4, 6, 4, 1};
    private static final int KERNEL_SIZE = 5;
    private static final int KERNEL_RADIUS = KERNEL_SIZE / 2;

    /**
     * Updates the Gaussian buffers with the current derivatives stored in the buffers.
     */
    private static void updateGaussian() {
        // Perform vertical convolution
        for (int i = 0; i < gxx.length; i++) {
            long sumXX = 0, sumYY = 0, sumXY = 0;
            for (int j = 0; j < KERNEL_SIZE; j++) {
                int index = (row + j - KERNEL_RADIUS + 5) % 5;
                sumXX += ixx[index][i] * WEIGHTS[j];
                sumYY += iyy[index][i] * WEIGHTS[j];
                sumXY += ixy[index][i] * WEIGHTS[j];
            }
            gxx[i] = sumXX;
            gyy[i] = sumYY;
            gxy[i] = sumXY;
        }

        // Perform horizontal convolution
        for (int i = 0; i < gxx.length - KERNEL_SIZE; i++) {
            long sumXX = gxx[i], sumYY = gyy[i], sumXY = gxy[i];
            for (int j = 1; j < KERNEL_SIZE; j++) {
                sumXX += gxx[i + j] * WEIGHTS[j];
                sumYY += gyy[i + j] * WEIGHTS[j];
                sumXY += gxy[i + j] * WEIGHTS[j];
            }
            gxx[i] = sumXX;
            gyy[i] = sumYY;
            gxy[i] = sumXY;
        }
    }

    /**
     * Updates the strength array with the current Gaussian buffers.
     */
    private static void updateStrength() {
        for (int i = 0; i < gxx.length - 4; i++) {
            long det = (long) gxx[i] * gyy[i] - (long) gxy[i] * gxy[i];
            long trace = (long) gxx[i] + gyy[i];
            double strength = det - k * trace * trace;
            s[row - 6][i] = strength;
        }
    }

    private static final int WINDOW_SIZE = 5;
    private static final int WINDOW_RADIUS = WINDOW_SIZE / 2;
    private static final int ROW_BUCKETS = 5;
    private static final int COL_BUCKETS = 10;

    /**
     * Performs non-maximum suppression to get the corners for features.
     *
     * @param s the strength array of the image.
     * @param features the list of list of features to store the corners divided into buckets.
     */
    private static void nonMaxSuppression(double[][] s, ArrayList<ArrayList<Feature>> features) {
        int rows = s.length;
        int cols = s[0].length;
        double rowBucketSize = (double) rows / ROW_BUCKETS;
        double colBucketSize = (double) cols / COL_BUCKETS;

        for (int i = WINDOW_RADIUS; i < rows - WINDOW_RADIUS; i++) {
            int rowBucket = Math.min((int) (i / rowBucketSize), ROW_BUCKETS - 1);
            for (int j = WINDOW_RADIUS; j < cols - WINDOW_RADIUS; j++) {
                double v = s[i][j];
                if (isLocalMaximum(s, i, j, v)) {
                    int colBucket = Math.min((int) (j / colBucketSize), COL_BUCKETS - 1);
                    int bucket = rowBucket * COL_BUCKETS + colBucket;
                    features.get(bucket).add(new Feature(v, i + 3, j + 3, grayImage));
                    j += WINDOW_RADIUS; // Skip the next WINDOW_RADIUS columns
                }
            }
        }
    }

    private static boolean isLocalMaximum(double[][] s, int row, int col, double value) {
        for (int i = -WINDOW_RADIUS; i <= WINDOW_RADIUS; i++) {
            for (int j = -WINDOW_RADIUS; j <= WINDOW_RADIUS; j++) {
                if (i == 0 && j == 0) continue; // Skip the center pixel
                if (value <= s[row + i][col + j]) return false;
            }
        }
        return true;
    }

    public static FeatureInfo detect(BufferedImage image, int[][] grayscale) {
        // get image width and length and init strength array
        grayImage = grayscale;
        int width = image.getWidth();
        int height = image.getHeight();
        s = new double[height - 6][width - 6];

        // initialize derivative buffers 
        ixx = new long[5][width - 2];
        iyy = new long[5][width - 2];
        ixy = new long[5][width - 2];

        // initialize gaussian buffers
        gxx = new long[width - 2];
        gyy = new long[width - 2];
        gxy = new long[width - 2];

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

        // perform non-maximum suppression to get corners for features
        nonMaxSuppression(s, features);

        // sort features in each bucket by strength and use top features
        for (int i = 0; i < 50; i++) {
            features.get(i).sort(new SortStrength());
            int top = features.get(i).size() > bucketCap ? bucketCap : features.get(i).size();
            features.set(i, new ArrayList<Feature>(features.get(i).subList(0, top)));
        }

        // for each feature, mark it on an array
        int[][] featureArray = new int[height][width];
        for (int i = 0; i < 50; i++) {
            for (Feature f : features.get(i)) {
                featureArray[f.x][f.y] = 1;
            }
        }

        // draw features on image
        for (int i = 0; i < 50; i++) {
            for (Feature f : features.get(i)) {
                if (f.value < 4E9) {
                    continue;
                }
                for (int j = f.x - 1; j <= f.x + 1; j++) {
                    for (int k = f.y - 1; k <= f.y + 1; k++) {
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

        return new FeatureInfo(s, featureArray);
    }
}
