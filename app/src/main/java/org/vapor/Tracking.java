package org.vapor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.lang.Math;

public class Tracking {
    /** The FeatureInfo object representing the first image. */
    static FeatureInfo infoA;

    /** The FeatureInfo object representing the second image. */
    static FeatureInfo infoB;

    /**
     * Calculates the sum of absolute differences between two 11x11 windows in two images.
     *
     * @param ax the x-coordinate of the center of the window in the first image.
     * @param ay the y-coordinate of the center of the window in the first image.
     * @param bx the x-coordinate of the center of the window in the second image.
     * @param by the y-coordinate of the center of the window in the second image.
     *
     * @return the sum of absolute differences between the two windows.
     */
    private static int sad(int ax, int ay, int bx, int by) {
        // iterate through 11x11 window in both images
        int sum = 0;
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                // get pixel values
                int a = infoA.image[ax + i][ay + j];
                int b = infoB.image[bx + i][by + j];
                // add absolute difference
                sum += Math.abs(a - b);
            }
        }
        return sum;
    }

    /**
     * Tracks features between two images.
     *
     * @param infoA the FeatureInfo object representing the first image.
     * @param infoB the FeatureInfo object representing the second image.
     */
    public static ArrayList<Feature[]> track(FeatureInfo infoAIn, FeatureInfo infoBIn) {
        // set infoA and infoB
        infoA = infoAIn;
        infoB = infoBIn;

        // get height and width
        int height = infoA.height;
        int width = infoA.width;

        // initialize hashmaps to track maximally correlated features between infoA and infoB
        HashMap<Feature, Feature> mapA = new HashMap<Feature, Feature>();
        HashMap<Feature, Feature> mapB = new HashMap<Feature, Feature>();

        // iterate through the features in infoA
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // check if feature exists
                if (infoA.features[i][j] == 0) { continue; }

                // search within 10% of image size for features in infoB
                int xMin = Math.max(i - height / 10, 0);
                int xMax = Math.min(i + height / 10, height);
                int yMin = Math.max(j - width / 10, 0);
                int yMax = Math.min(j + width / 10, width);
                int minSAD = Integer.MAX_VALUE;
                Feature bestMatch = null;
                for (int x = xMin; x < xMax; x++) {
                    for (int y = yMin; y < yMax; y++) {
                        // check if feature exists
                        if (infoB.features[x][y] == 0) { continue; }

                        // get SAD
                        int sad = sad(i, j, x, y);
                        if (sad < minSAD) {
                            minSAD = sad;
                            bestMatch = new Feature(infoB.strengths[x-3][y-3], x, y, infoB.features);
                        }
                    }
                }

                // if no match found, continue
                if (bestMatch == null) { continue; }

                // map bestMatch from image A to image B
                Feature matchA = new Feature(infoA.strengths[i-3][j-3], i, j, infoA.features);
                matchA.setSad(minSAD);

                // map bestMatch from image B to image A if it doesn't exist
                if (mapB.get(bestMatch) == null) {
                    mapA.put(matchA, bestMatch);
                    mapB.put(bestMatch, matchA);
                }

                // otherwise, check if the SAD is lower
                else if (minSAD < mapB.get(bestMatch).getSAD()) {
                    mapA.remove(mapB.get(bestMatch));
                    mapA.put(matchA, bestMatch);
                    mapB.put(bestMatch, matchA);
                }
            }
        }

        // check for mutual consistency between mapA and mapB
        ArrayList<Feature[]> matches = new ArrayList<Feature[]>();
        for (Feature f : mapA.keySet()) {
            if (mapB.get(mapA.get(f)).equals(f)) {
                matches.add(new Feature[] {f, mapA.get(f)});
            }
        }

        return matches;
    }
}
