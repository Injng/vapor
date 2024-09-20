package org.vapor;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Random;

public class RANSAC {
    private static final int NUM_HYPOTHESES = 500;
    private static final int MIN_SAMPLE_SIZE = 4; // For a PnP problem
    private static final int PREEMPTION_GROUP_SIZE = 10;

    /**
     * Randomly selects a sample of matches from the input list.
     *
     * @param matches     the list of feature correspondences
     * @param sampleSize  the number of matches to select
     * @param random      the random number generator
     *
     * @return a list of feature correspondences representing the random sample
     */
    private static List<Feature[]> getRandomSample(List<Feature[]> matches, int sampleSize, Random random) {
        ArrayList<Feature[]> sample = new ArrayList<>(matches);
        Collections.shuffle(sample, random);
        return sample.subList(0, sampleSize);
    }

    /**
     * Estimates the camera motion from a sample of feature correspondences, using a PNP algorithm.
     *
     * @param points2D the 2D points in the image from the reference camera
     * @param points3D the 3D points in the world frame
     * @param cameras the stereo camera system
     *
     * @return the estimated camera motion
     */
    private static Motion estimateMotion(MatOfPoint2f points2D, MatOfPoint3f points3D, Stereo cameras) {
        // solve the PnP problem, storing the result in rvec and tvec
        Mat rvec = new Mat();
        Mat tvec = new Mat();
        Calib3d.solvePnP(points3D, points2D, cameras.getCamera(), cameras.getDistortion(), rvec, tvec);
        return new Motion(rvec, tvec);
    }

    /**
     * Scores a list of hypotheses by computing the reprojection error of each hypothesis.
     *
     * @param hypotheses a list of the motion hypotheses to score
     * @param points3D the 3D points to reproject
     * @param points2D the 2D points to compare against
     * @param cameras the stereo camera system
     *
     * @return the best hypothesis
     */
    private static Motion score(List<Motion> hypotheses, MatOfPoint3f points3D, MatOfPoint2f points2D, Stereo cameras) {
        // split the points into buckets of PREEMPTION_GROUP_SIZE points each
        List<MatOfPoint3f> groups3D = new ArrayList<>();
        List<MatOfPoint2f> groups2D = new ArrayList<>();
        for (int i = 0; i < points3D.rows(); i += PREEMPTION_GROUP_SIZE) {
            MatOfPoint3f group3D = new MatOfPoint3f();
            MatOfPoint2f group2D = new MatOfPoint2f();
            for (int j = 0; j < PREEMPTION_GROUP_SIZE; j++) {
                group3D.push_back(points3D.row(i + j));
                group2D.push_back(points2D.row(i + j));
            }
            groups3D.add(group3D);
            groups2D.add(group2D);
        }

        // score each hypothesis by computing the reprojection error, assuming a Cauchy distribution
        Motion best_hypothesis = null;
        for (Motion hypothesis: hypotheses) {
            for (int i = 0; i < groups3D.size(); i++) {
                double error = computeReprojectionError(hypothesis, groups3D.get(i), groups2D.get(i), cameras);
                hypothesis.score += error;
            }
            if (best_hypothesis == null || hypothesis.score > best_hypothesis.score) {
                best_hypothesis = hypothesis;
            }
        }

        return best_hypothesis;
    }

    /**
     * Computes the reprojection error of a hypothesis.
     *
     * @param hypothesis the motion hypothesis to test
     * @param points3D the 3D points to reproject
     * @param points2D the 2D points to compare against
     * @param cameras the stereo camera system
     *
     * @return the reprojection error, log-likeliness of the product of all the scaled squared magnitudes of the errors
     */
    private static double computeReprojectionError(Motion hypothesis, MatOfPoint3f points3D, MatOfPoint2f points2D, Stereo cameras) {
        MatOfPoint2f projectedPoints = new MatOfPoint2f();
        Mat rvec = hypothesis.rotation;
        Mat tvec = hypothesis.translation;
        Calib3d.projectPoints(points3D, rvec, tvec, cameras.getCamera(), cameras.getDistortion(), projectedPoints);
        // compute the error
        Mat diff = new Mat();
        Core.subtract(projectedPoints, points2D, diff);
        Mat squaredDiff = new Mat();
        Core.multiply(diff, diff, squaredDiff);
        Core.add(squaredDiff, Scalar.all(1), squaredDiff);

        // calculate product of all the scaled squared magnitudes of the errors
        double product = 1;
        for (int i = 0; i < squaredDiff.rows(); i++) {
            double error = squaredDiff.get(i, 0)[0] + squaredDiff.get(i, 1)[0];
            product *= error;
        }

        // get log-likeness
        return -1 * Math.log(product);
    }

    /**
     * Refines the initial solution using a Levenberg-Marquardt optimization.
     *
     * @param initialSolution the initial solution to refine
     * @param cameras the stereo camera system
     * @param points2D the 2D points of the features
     * @param points3D the triangulated 3D points of the features
     * @param maxIterations the maximum number of iterations
     * @param lambda the damping factor
     * @param epsilon the convergence threshold
     */
    private static void refineSolution(Motion initialSolution, Stereo cameras, MatOfPoint2f points2D, MatOfPoint3f points3D, int maxIterations, double lambda, double epsilon) {
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Mat jacobian = new Mat();
            MatOfPoint2f projectedPoints = new MatOfPoint2f();

            Mat rvec = initialSolution.rotation;
            Mat tvec = initialSolution.translation;
            Calib3d.projectPoints(points3D, rvec, tvec, cameras.getCamera(), cameras.getDistortion(), projectedPoints, jacobian);

            Mat residuals = new Mat();
            Core.subtract(points2D, projectedPoints, residuals);
            residuals = residuals.reshape(1, residuals.rows() * 2);

            Mat JTJ = new Mat();
            Mat JTr = new Mat();
            Core.gemm(jacobian.t(), jacobian, 1, new Mat(), 0, JTJ);
            Core.gemm(jacobian.t(), residuals, 1, new Mat(), 0, JTr);

            // Add lambda to diagonal of JTJ
            for (int i = 0; i < JTJ.rows(); i++) {
                JTJ.put(i, i, JTJ.get(i, i)[0] + lambda);
            }

            Mat delta = new Mat();
            Core.solve(JTJ, JTr, delta, Core.DECOMP_CHOLESKY);

            // Update rvec and tvec
            for (int i = 0; i < 3; i++) {
                rvec.put(i, 0, rvec.get(i, 0)[0] + delta.get(i, 0)[0]);
                tvec.put(i, 0, tvec.get(i, 0)[0] + delta.get(i + 3, 0)[0]);
            }

            // Check for convergence
            if (Core.norm(delta) < epsilon) {
                break;
            }
        }
    }

    /**
     * Generates a list of hypotheses by randomly selecting samples of matches.
     *
     * @param matches the list of feature correspondences
     * @param cameras the stereo camera system
     *
     * @return a list of hypotheses
     */
    private static List<Motion> generateHypotheses(List<Feature[]> matches, Stereo cameras) {
        ArrayList<Motion> hypotheses = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < NUM_HYPOTHESES; i++) {
            List<Feature[]> sample = getRandomSample(matches, MIN_SAMPLE_SIZE, random);

            // build matrices of 2D and 3D points, using the first image as the reference
            ArrayList<Point> list2D = new ArrayList<Point>();
            ArrayList<Point3> list3D = new ArrayList<Point3>();
            for (Feature[] correspondence : sample) {
                list2D.add(correspondence[0].toPoint());
                double[] first = { correspondence[0].x, correspondence[0].y };
                double[] second = { correspondence[1].x, correspondence[1].y };
                list3D.add(cameras.triangulate(first, second));
            }
            MatOfPoint2f points2D = new MatOfPoint2f();
            MatOfPoint3f points3D = new MatOfPoint3f();
            points2D.fromList(list2D);
            points3D.fromList(list3D);

            // estimate the motion hypothesis
            Motion hypothesis = estimateMotion(points2D, points3D, cameras);
            hypotheses.add(hypothesis);
        }

        return hypotheses;
    }

    /**
     * Runs the RANSAC algorithm to estimate the camera motion.
     *
     * @param matches the list of feature correspondences
     * @param cameras the stereo camera system
     *
     * @return the estimated camera pose
     */
    public static Motion ransac(List<Feature[]> matches, Stereo cameras) {
        // Convert matches to points
        ArrayList<Point> points2D = new ArrayList<Point>();
        ArrayList<Point3> triangulatedPts = new ArrayList<Point3>();
        for (Feature[] match : matches) {
            points2D.add(match[0].toPoint());
            double[] first = { match[0].x, match[0].y };
            double[] second = { match[1].x, match[1].y };
            triangulatedPts.add(cameras.triangulate(first, second));
        }
        MatOfPoint2f srcMat = new MatOfPoint2f();
        MatOfPoint3f triangulatedMat = new MatOfPoint3f();
        srcMat.fromList(points2D);
        triangulatedMat.fromList(triangulatedPts);

        // run preemptive RANSAC by generating hypotheses, scoring them, and refining the best one
        List<Motion> hypotheses = generateHypotheses(matches, cameras);
        Motion best_hypothesis = score(hypotheses, triangulatedMat, srcMat, cameras);
        refineSolution(best_hypothesis, cameras, srcMat, triangulatedMat, 100, 0.1, 0.01);

        return best_hypothesis;
    }
}
