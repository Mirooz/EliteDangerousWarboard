package be.mirooz.elitedangerous.dashboard.overlay.panel;

import javafx.geometry.Point2D;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Détecte le quadrilatère bleu de calibration (tracé sur une capture type {@code leftpanelposition2.bmp})
 * et retourne les quatre coins en <strong>pixels</strong> image (ordre {@link PanelCorners}).
 */
public final class BlueLeftPanelPositionDetector {

    private static final Scalar HSV_BLUE_A_LOW = new Scalar(90, 40, 40);
    private static final Scalar HSV_BLUE_A_HIGH = new Scalar(140, 255, 255);
    private static final Scalar HSV_BLUE_B_LOW = new Scalar(170, 40, 40);
    private static final Scalar HSV_BLUE_B_HIGH = new Scalar(180, 255, 255);

    private BlueLeftPanelPositionDetector() {
    }

    /**
     * @param bgr image BGR OpenCV (non null, non vide)
     * @return coins en coordonnées pixel (origine haut-gauche), ou vide si échec
     */
    public static Optional<PanelCorners> detectPixelCorners(Mat bgr) {
        if (bgr == null || bgr.empty()) {
            return Optional.empty();
        }
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hsv = new Mat();
        Mat m1 = new Mat();
        Mat m2 = new Mat();
        Mat mask = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        Mat hierarchy = new Mat();
        try {
            Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);
            Core.inRange(hsv, HSV_BLUE_A_LOW, HSV_BLUE_A_HIGH, m1);
            Core.inRange(hsv, HSV_BLUE_B_LOW, HSV_BLUE_B_HIGH, m2);
            Core.bitwise_or(m1, m2, mask);
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint best = null;
            double bestArea = 0;
            for (MatOfPoint c : contours) {
                double a = Imgproc.contourArea(c);
                if (a > bestArea) {
                    bestArea = a;
                    best = c;
                }
            }
            if (best == null || bestArea < 2000) {
                return Optional.empty();
            }

            MatOfPoint2f contour2f = new MatOfPoint2f(best.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Optional<Point[]> quad = Optional.empty();
            for (double epsFrac = 0.025; epsFrac <= 0.07 && quad.isEmpty(); epsFrac += 0.0025) {
                Imgproc.approxPolyDP(contour2f, approx, epsFrac * peri, true);
                if (approx.rows() == 4) {
                    quad = Optional.of(approx.toArray());
                }
            }
            if (quad.isEmpty()) {
                org.opencv.core.RotatedRect rect = Imgproc.minAreaRect(contour2f);
                Point[] box = new Point[4];
                rect.points(box);
                quad = Optional.of(box);
            }
            contour2f.release();
            approx.release();

            Point[] ordered = LeftPanelCornerDetector.orderCornersClockwiseFromTopLeft(quad.orElseThrow());

            Mat gray = new Mat();
            Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfPoint2f corners = new MatOfPoint2f(ordered);
            TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 80, 0.001);
            Imgproc.cornerSubPix(gray, corners, new Size(7, 7), new Size(-1, -1), criteria);
            gray.release();

            Point[] refined = corners.toArray();
            corners.release();
            Point[] orderedRefined = LeftPanelCornerDetector.orderCornersClockwiseFromTopLeft(refined);

            return Optional.of(new PanelCorners(
                    toFx(orderedRefined[0]),
                    toFx(orderedRefined[1]),
                    toFx(orderedRefined[2]),
                    toFx(orderedRefined[3])
            ));
        } finally {
            hsv.release();
            m1.release();
            m2.release();
            mask.release();
            kernel.release();
            hierarchy.release();
            for (MatOfPoint c : contours) {
                c.release();
            }
        }
    }

    private static Point2D toFx(Point p) {
        return new Point2D(p.x, p.y);
    }
}
