package org.vapor;

public class FeatureInfo {
    /** The grayscale values of the image. */
    public int[][] image;

    /** The strengths of the features, offset by 3 in both dimensions from the image. */
    public double[][] strengths;

    /** The features of the image. */
    public int[][] features;

    /** The height of the image. */
    public int height;

    /** The width of the image. */
    public int width;

    public FeatureInfo(double[][] strengths, int[][] features, int[][] image) {
        this.image = image;
        this.strengths = strengths;
        this.features = features;
        this.height = features.length;
        this.width = features[0].length;
    }
}

