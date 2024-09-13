package org.vapor;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

public class Triangulation {
    public static SimpleMatrix triangulate(SimpleMatrix P1, SimpleMatrix P2, double[] point1, double[] point2) {
        // construct matrix A for triangulation
        SimpleMatrix A = new SimpleMatrix(4, 4);
        
        // fill in matrix A with appropriate values
        A.setRow(0, 0, new double[]{
            point1[1] * P1.get(2, 0) - P1.get(1, 0),
            point1[1] * P1.get(2, 1) - P1.get(1, 1),
            point1[1] * P1.get(2, 2) - P1.get(1, 2),
            point1[1] * P1.get(2, 3) - P1.get(1, 3)
        });
        
        A.setRow(1, 0, new double[]{
            P1.get(0, 0) - point1[0] * P1.get(2, 0),
            P1.get(0, 1) - point1[0] * P1.get(2, 1),
            P1.get(0, 2) - point1[0] * P1.get(2, 2),
            P1.get(0, 3) - point1[0] * P1.get(2, 3)
        });
        
        A.setRow(2, 0, new double[]{
            point2[1] * P2.get(2, 0) - P2.get(1, 0),
            point2[1] * P2.get(2, 1) - P2.get(1, 1),
            point2[1] * P2.get(2, 2) - P2.get(1, 2),
            point2[1] * P2.get(2, 3) - P2.get(1, 3)
        });
        
        A.setRow(3, 0, new double[]{
            P2.get(0, 0) - point2[0] * P2.get(2, 0),
            P2.get(0, 1) - point2[0] * P2.get(2, 1),
            P2.get(0, 2) - point2[0] * P2.get(2, 2),
            P2.get(0, 3) - point2[0] * P2.get(2, 3)
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
        
        return new SimpleMatrix(new double[][]{normalizedPoint});
    }
}

