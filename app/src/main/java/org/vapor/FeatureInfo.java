package org.vapor;

public class FeatureInfo {
    public double[][] strengths;
    public int[][] features;
    public int height;
    public int width;

    public FeatureInfo(double[][] strengths, int[][] features) {
        this.strengths = strengths;
        this.features = features;
        this.height = features.length;
        this.width = features[0].length;
    }
}

