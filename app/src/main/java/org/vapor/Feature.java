package org.vapor;

/**
 * Represents a feature in an image with its strength value and coordinates.
 */
public class Feature {
    /** The strength value of the feature. */
    double value;

    /** The SAD correlation value of the feature. WARNING: may be null! */
    Integer sad;

    /** The x-coordinate of the feature in the image. */
    int x;

    /** The y-coordinate of the feature in the image. */
    int y;

    /** The cumulative sum of the pixel intensities in a 11x11 window around the feature. */
    int a;

    /** The cumulative sum of the pixel intensities squared in a 11x11 window around the feature. */
    int b;

    /** A value inversely proportional to the standard deviation of the pixel intensities in a 11x11 window around the feature. */
    double c;

    /**
     * Constructs a new Feature with the specified strength value and coordinates.
     *
     * @param value The strength value of the feature.
     * @param x The x-coordinate of the feature in the image.
     * @param y The y-coordinate of the feature in the image.
     */
    Feature(double value, int x, int y, int[][] image) {
        // check if the coordinates are not within 5 pixels of the border
        if (x < 5 || x >= image.length - 5 || y < 5 || y >= image[0].length - 5) {
            throw new IllegalArgumentException("Feature coordinates must be at least 5 pixels away from the border.");
        }
        
        // set and compute values
        this.value = value;
        this.x = x;
        this.y = y;
        this.a = 0;
        this.b = 0;
        this.c = 0;
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                int pixel = image[x + i][y + j];
                this.a += pixel;
                this.b += pixel * pixel;
            }
        }
        this.c = 1 / Math.sqrt((121 * this.b - this.a * this.a));
    }

    /**
     * Sets the SAD correlation value of the feature.
     *
     * @param sad The SAD correlation value of the feature.
     */
    public void setSad(int sad) {
        this.sad = sad;
    }

    /**
     * Gets the SAD correlation value of the feature.
     *
     * @return The SAD correlation value of the feature.
     */
    public int getSAD() {
        if (this.sad == null) {
            throw new IllegalStateException("SAD correlation value has not been set.");
        }
        return this.sad;
    }
}

