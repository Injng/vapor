package org.vapor;

import org.ejml.simple.SimpleMatrix;
import org.opencv.core.Core;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class App {
    /** The intrinsics matrix for camera 1. */
    private static final SimpleMatrix mtx1 = new SimpleMatrix(new double[][]{
            {1235.45723, 0.0, 586.197756},
            {0.0, 1159.97889, 487.408735},
            {0.0, 0.0, 1.0}
    });

    /** The distortion coefficients for camera 1. */
    private static final MatOfDouble dist1 = new MatOfDouble(new double[]{0.03852127, -0.2639929, 0.01173549, -0.00741738, 0.32940914});

    /** The intrinsics matrix for camera 2. */
    private static final SimpleMatrix mtx2 = new SimpleMatrix(new double[][]{
            {831.928232, 0.0, 616.3016761},
            {0.0, 779.51864043, 467.44607183},
            {0.0, 0.0, 1.0}
    });

    /** The distortion coefficients for camera 2. */
    private static final MatOfDouble dist2 = new MatOfDouble(new double[]{0.05883428, -0.13611729, 0.00960835, -0.00353916, 0.01426129});

    /** The rotation matrix for the stereo camera system. */
    private static final SimpleMatrix R = new SimpleMatrix(new double[][]{
            {0.19364412, -0.07078774, 0.97851472},
            {0.22308861, 0.97444211, 0.02634478},
            {-0.95537083, 0.21319398, 0.20448692}
    });

    /** The translation vector for the stereo camera system. */
    private static final SimpleMatrix T = new SimpleMatrix(new double[][]{
            {-24.85136975},
            {-1.77457249},
            {16.61001315}
    });

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
        System.load("/usr/lib/libopencv_java.so");

        // initialize Stereo object
        Stereo cameras = new Stereo(mtx1, mtx2, dist1, dist2, R, T);

        // get stereo image frames
        Frame frame1 = new Frame("./1A.png", "./1B.png");
        Frame frame2 = new Frame("./2A.png", "./2B.png");

        // run image detection
        FeatureInfo info1A = Detection.detect(frame1.leftImage, frame1.leftGrayImage);
        FeatureInfo info1B = Detection.detect(frame1.rightImage, frame1.rightGrayImage);
        FeatureInfo info2A = Detection.detect(frame2.leftImage, frame2.leftGrayImage);
        FeatureInfo info2B = Detection.detect(frame2.rightImage, frame2.rightGrayImage);

        // run feature matching between the stereo pairs
        HashMap<Feature, Feature> stereo1Matches = Tracking.track(info1A, info1B);
        HashMap<Feature, Feature> stereo2Matches = Tracking.track(info2A, info2B);

        // run feature matching between the two frames
        HashMap<Feature, Feature> frameMatches = Tracking.track(info1A, info2A);

        // for each feature match between the two frames, triangulate the 3D point in the first frame
        ArrayList<Point> points2D = new ArrayList<>();
        ArrayList<Point3> points3D = new ArrayList<>();
        for (Feature feature1A : frameMatches.keySet()) {
            Feature feature2A = frameMatches.get(feature1A);

            // ensure feature1A and feature2A has a match in the stereo pair
            if (!stereo1Matches.containsKey(feature1A)) continue;
            if (!stereo2Matches.containsKey(feature2A)) continue;

            // get the corresponding features in the stereo pair
            Feature feature1B = stereo1Matches.get(feature1A);
            // Feature feature2B = stereo2Matches.get(feature2A);

            // triangulate the feature in the first frame
            double[] point1 = new double[]{feature1A.x, feature1A.y};
            double[] point2 = new double[]{feature1B.x, feature1B.y};
            Point3 point3D = cameras.triangulate(point1, point2);

            // add the 2D and 3D points to the list
            points2D.add(new Point(feature2A.x, feature2A.y));
            points3D.add(new Point3(point3D.x, point3D.y, point3D.z));
        }

        // run preemptive RANSAC
        Motion best_hypothesis = RANSAC.ransac(points2D, points3D, cameras);

        // print best hypothesis
        System.out.println(best_hypothesis);

/*
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
 */
    }
}
