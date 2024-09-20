package org.vapor;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point3;

/**
 * Represents a stereo camera system.
 */
public class Stereo {
    /* A 3x3 intrinsics matrix for camera 1. */
    private final SimpleMatrix mtx1;

    /* A 3x3 intrinsics matrix for camera 2. */
    private final SimpleMatrix mtx2;

    /* A 1x5 distortion matrix for camera 1. */
    public final MatOfDouble dist1;

    /* A 1x5 distortion matrix for camera 2. */
    public final MatOfDouble dist2;

    /* A 3x4 projection matrix for camera 1. */
    private final SimpleMatrix proj1;

    /* A 3x4 projection matrix for camera 2. */
    private final SimpleMatrix proj2;

    /* A pointer to the reference camera's instrinsics matrix. */
    private final SimpleMatrix camera;

    public Stereo(SimpleMatrix mtx1, SimpleMatrix mtx2, MatOfDouble dist1, MatOfDouble dist2, SimpleMatrix R, SimpleMatrix T) {
        // store the camera and distortion matrices
        this.mtx1 = mtx1;
        this.mtx2 = mtx2;
        this.dist1 = dist1;
        this.dist2 = dist2;

        // set reference camera to be the first camera
        this.camera = mtx1;

        // RT matrix for C1 is identity (3x4)
        SimpleMatrix RT1 = SimpleMatrix.identity(4);
        RT1 = RT1.extractMatrix(0, 3, 0, 4);

        // projection matrix for C1 (3x4)
        this.proj1 = mtx1.mult(RT1);

        // RT matrix for C2 is the R and T obtained from stereo calibration (3x4)
        SimpleMatrix RT2 = new SimpleMatrix(3, 4);
        RT2.insertIntoThis(0, 0, R);
        RT2.insertIntoThis(0, 3, T);

        // projection matrix for C2 (3x4)
        this.proj2 =  mtx2.mult(RT2);
    }

    /**
     * Triangulates a 3D point from two camera matrices and corresponding 2D points using DLT.
     *
     * @param point1 the 2D point in the first image
     * @param point2 the 2D point in the second image
     *
     * @return the triangulated 3D point
     */
    public Point3 triangulate(double[] point1, double[] point2) {
        // construct matrix A for triangulation
        SimpleMatrix A = new SimpleMatrix(4, 4);

        // fill in matrix A with appropriate values
        A.setRow(0, 0, new double[]{
                point1[1] * proj1.get(2, 0) - proj1.get(1, 0),
                point1[1] * proj1.get(2, 1) - proj1.get(1, 1),
                point1[1] * proj1.get(2, 2) - proj1.get(1, 2),
                point1[1] * proj1.get(2, 3) - proj1.get(1, 3)
        });

        A.setRow(1, 0, new double[]{
                proj1.get(0, 0) - point1[0] * proj1.get(2, 0),
                proj1.get(0, 1) - point1[0] * proj1.get(2, 1),
                proj1.get(0, 2) - point1[0] * proj1.get(2, 2),
                proj1.get(0, 3) - point1[0] * proj1.get(2, 3)
        });

        A.setRow(2, 0, new double[]{
                point2[1] * proj2.get(2, 0) - proj2.get(1, 0),
                point2[1] * proj2.get(2, 1) - proj2.get(1, 1),
                point2[1] * proj2.get(2, 2) - proj2.get(1, 2),
                point2[1] * proj2.get(2, 3) - proj2.get(1, 3)
        });

        A.setRow(3, 0, new double[]{
                proj2.get(0, 0) - point2[0] * proj2.get(2, 0),
                proj2.get(0, 1) - point2[0] * proj2.get(2, 1),
                proj2.get(0, 2) - point2[0] * proj2.get(2, 2),
                proj2.get(0, 3) - point2[0] * proj2.get(2, 3)
        });

        // compute SVD of A transpose times A
        SimpleMatrix At = A.transpose();
        SimpleMatrix B = At.mult(A);
        SimpleSVD<SimpleMatrix> svd = B.svd();
        SimpleMatrix V = svd.getV();

        // extract and normalize the last row of V
        double[] triangulatedPoint = V.extractVector(false, 3).getDDRM().getData();
        double w = triangulatedPoint[3];
        double[] normalizedPoint = {
                triangulatedPoint[0] / w,
                triangulatedPoint[1] / w,
                triangulatedPoint[2] / w
        };

        System.out.println("Triangulated point: ");
        System.out.println("x: " + normalizedPoint[0] + ", y: " + normalizedPoint[1] + ", z: " + normalizedPoint[2]);

        return new Point3(normalizedPoint[0], normalizedPoint[1], normalizedPoint[2]);
    }

    /**
     * Converts an EJML SimpleMatrix to an OpenCV Mat.
     *
     * @param simpleMatrix The EJML SimpleMatrix to convert
     *
     * @return An OpenCV Mat containing the same data
     */
    private Mat simpleMatrixToMat(SimpleMatrix simpleMatrix) {
        int rows = simpleMatrix.getNumRows();
        int cols = simpleMatrix.getNumCols();
        Mat mat = new Mat(rows, cols, CvType.CV_64F);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                mat.put(i, j, simpleMatrix.get(i, j));
            }
        }

        return mat;
    }

    /**
     * Returns the camera matrix for the reference camera.
     *
     * @return the camera matrix for the reference camera
     */
    public Mat getCamera() {
        return simpleMatrixToMat(camera);
    }

    /**
     * Returns the distortion matrix for the reference camera
     *
     * @return the distortion matrix for the reference camera
     */
    public MatOfDouble getDistortion() {
        return camera == mtx1 ? dist1 : dist2;
    }
}
