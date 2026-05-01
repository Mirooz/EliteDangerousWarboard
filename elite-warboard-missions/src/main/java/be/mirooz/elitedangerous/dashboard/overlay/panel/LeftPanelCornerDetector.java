package be.mirooz.elitedangerous.dashboard.overlay.panel;

import javafx.geometry.Point2D;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Détecte le panneau gauche orange sur une capture cockpit (BGR) et en extrait quatre coins
 * ordonnés {@link PanelCorners}.
 */
public final class LeftPanelCornerDetector {

    /** HSV OpenCV (H 0–180) : plage large pour l’UI orange / ambre du jeu. */
    private static final Scalar HSV_LOWER = new Scalar(6, 60, 70);
    private static final Scalar HSV_UPPER = new Scalar(32, 255, 255);

    private LeftPanelCornerDetector() {
    }

    /**
     * @param bgr image couleur OpenCV (canal BGR), typiquement {@code Imgcodecs.imread} /
     *            {@code imdecode}
     */
    public static Optional<PanelCorners> detect(Mat bgr) {
        if (bgr == null || bgr.empty()) {
            return Optional.empty();
        }
        Mat hsv = new Mat();
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);
        Mat mask = new Mat();
        Core.inRange(hsv, HSV_LOWER, HSV_UPPER, mask);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(7, 7));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hsv.release();
        mask.release();
        hierarchy.release();
        kernel.release();

        try {
            MatOfPoint best = null;
            double bestArea = 0;
            for (MatOfPoint c : contours) {
                double a = Imgproc.contourArea(c);
                if (a > bestArea) {
                    bestArea = a;
                    best = c;
                }
            }

            if (best == null || bestArea < 500) {
                return Optional.empty();
            }

            MatOfPoint2f contour2f = new MatOfPoint2f(best.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Optional<Point[]> quad = Optional.empty();
            for (double frac = 0.004; frac <= 0.12 && quad.isEmpty(); frac += 0.003) {
                Imgproc.approxPolyDP(contour2f, approx, frac * peri, true);
                if (approx.rows() == 4) {
                    quad = Optional.of(approx.toArray());
                }
            }
            if (quad.isEmpty()) {
                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(best, hull);
                Point[] hp = new Point[hull.rows()];
                int[] hi = hull.toArray();
                Point[] pts = best.toArray();
                for (int i = 0; i < hi.length; i++) {
                    hp[i] = pts[hi[i]];
                }
                MatOfPoint2f hull2f = new MatOfPoint2f(hp);
                double periH = Imgproc.arcLength(hull2f, true);
                for (double frac = 0.004; frac <= 0.12 && quad.isEmpty(); frac += 0.003) {
                    Imgproc.approxPolyDP(hull2f, approx, frac * periH, true);
                    if (approx.rows() == 4) {
                        quad = Optional.of(approx.toArray());
                    }
                }
                hull.release();
                hull2f.release();
            }
            if (quad.isEmpty()) {
                org.opencv.core.RotatedRect rect = Imgproc.minAreaRect(contour2f);
                Point[] box = new Point[4];
                rect.points(box);
                quad = Optional.of(box);
            }
            contour2f.release();
            approx.release();

            Point[] ordered = orderCornersClockwiseFromTopLeft(quad.orElseThrow());
            return Optional.of(new PanelCorners(
                    toFx(ordered[0]),
                    toFx(ordered[1]),
                    toFx(ordered[2]),
                    toFx(ordered[3])
            ));
        } finally {
            for (MatOfPoint c : contours) {
                c.release();
            }
        }
    }

    private static Point2D toFx(Point p) {
        return new Point2D(p.x, p.y);
    }

    /**
     * Ordre TL → TR → BR → BL (sens horaire, y vers le bas comme OpenCV / JavaFX).
     * Heuristique classique « document scan » sur un quadrilatère à peu près axial.
     */
    static Point[] orderCornersClockwiseFromTopLeft(Point[] four) {
        if (four.length != 4) {
            throw new IllegalArgumentException("4 points requis");
        }
        Point[] rect = new Point[4];
        double[] sums = new double[4];
        double[] diffs = new double[4];
        for (int i = 0; i < 4; i++) {
            sums[i] = four[i].x + four[i].y;
            diffs[i] = four[i].y - four[i].x;
        }
        int iMinSum = indexOfMin(sums);
        int iMaxSum = indexOfMax(sums);
        int iMinDiff = indexOfMin(diffs);
        int iMaxDiff = indexOfMax(diffs);
        rect[0] = four[iMinSum];
        rect[2] = four[iMaxSum];
        rect[1] = four[iMinDiff];
        rect[3] = four[iMaxDiff];
        return rect;
    }

    private static int indexOfMin(double[] a) {
        int ix = 0;
        for (int i = 1; i < a.length; i++) {
            if (a[i] < a[ix]) {
                ix = i;
            }
        }
        return ix;
    }

    private static int indexOfMax(double[] a) {
        int ix = 0;
        for (int i = 1; i < a.length; i++) {
            if (a[i] > a[ix]) {
                ix = i;
            }
        }
        return ix;
    }
}
