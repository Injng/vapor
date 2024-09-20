package org.vapor;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Motion {
    public Mat rotation;    // 3x3 rotation matrix
    public Mat translation; // 3x1 translation vector
    public double score;    // score of the motion

    public Motion() {
        // initialize with identity rotation and zero translation
        rotation = Mat.eye(3, 3, CvType.CV_64F);
        translation = Mat.zeros(3, 1, CvType.CV_64F);
    }

    public Motion(Mat rotation, Mat translation) {
        this.rotation = rotation;
        this.translation = translation;
        this.score = 0;
    }
}