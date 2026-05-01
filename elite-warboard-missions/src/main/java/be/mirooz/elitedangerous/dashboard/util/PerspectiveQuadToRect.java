package be.mirooz.elitedangerous.dashboard.util;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Redresse un quadrilatère source (coins dans l’image) vers un rectangle de sortie.
 * <p>
 * Convention des coins : identique à {@link javafx.scene.effect.PerspectiveTransform} —
 * haut-gauche, haut-droite, bas-droite, bas-gauche (UL, UR, LR, LL).
 * L’effet JavaFX applique la perspective dans le sens « rectangle → quadrilatère » ;
 * ici on applique l’<strong>inverse</strong> pour ne garder que la zone du panneau dans un rectangle.
 */
public final class PerspectiveQuadToRect {

    private PerspectiveQuadToRect() {
    }

    /**
     * @param src        image source (snapshot du root)
     * @param ulx, uly   coin haut-gauche du panneau dans {@code src}
     * @param urx, ury   coin haut-droit
     * @param lrx, lry   coin bas-droit
     * @param llx, lly   coin bas-gauche
     * @param outWidth   largeur de l’image rectifiée
     * @param outHeight  hauteur de l’image rectifiée
     */
    public static WritableImage unwrap(
            Image src,
            double ulx, double uly,
            double urx, double ury,
            double lrx, double lry,
            double llx, double lly,
            int outWidth,
            int outHeight) {
        if (outWidth < 1 || outHeight < 1) {
            return new WritableImage(1, 1);
        }
        double[][] srcPts = {
                {ulx, uly},
                {urx, ury},
                {lrx, lry},
                {llx, lly}
        };
        double[][] dstPts = {
                {0, 0},
                {outWidth - 1.0, 0},
                {outWidth - 1.0, outHeight - 1.0},
                {0, outHeight - 1.0}
        };
        double[][] h = homographySrcToDst(srcPts, dstPts);
        double[][] hInv = invert3x3(h);

        PixelReader reader = src.getPixelReader();
        int srcW = (int) src.getWidth();
        int srcH = (int) src.getHeight();
        WritableImage out = new WritableImage(outWidth, outHeight);
        PixelWriter writer = out.getPixelWriter();

        for (int y = 0; y < outHeight; y++) {
            for (int x = 0; x < outWidth; x++) {
                double wx = hInv[0][0] * x + hInv[0][1] * y + hInv[0][2];
                double wy = hInv[1][0] * x + hInv[1][1] * y + hInv[1][2];
                double wz = hInv[2][0] * x + hInv[2][1] * y + hInv[2][2];
                if (Math.abs(wz) < 1e-9) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                    continue;
                }
                double sx = wx / wz;
                double sy = wy / wz;
                Color c = sampleBilinear(reader, srcW, srcH, sx, sy);
                writer.setColor(x, y, c);
            }
        }
        return out;
    }

    private static Color sampleBilinear(PixelReader reader, int srcW, int srcH, double sx, double sy) {
        if (sx < -0.5 || sy < -0.5 || sx > srcW - 0.5 || sy > srcH - 0.5) {
            return Color.TRANSPARENT;
        }
        int x0 = (int) Math.floor(sx);
        int y0 = (int) Math.floor(sy);
        int x1 = Math.min(x0 + 1, srcW - 1);
        int y1 = Math.min(y0 + 1, srcH - 1);
        x0 = Math.max(0, Math.min(x0, srcW - 1));
        y0 = Math.max(0, Math.min(y0, srcH - 1));

        double fx = sx - x0;
        double fy = sy - y0;
        Color c00 = reader.getColor(x0, y0);
        Color c10 = reader.getColor(x1, y0);
        Color c01 = reader.getColor(x0, y1);
        Color c11 = reader.getColor(x1, y1);

        double a00 = (1 - fx) * (1 - fy);
        double a10 = fx * (1 - fy);
        double a01 = (1 - fx) * fy;
        double a11 = fx * fy;

        double r = c00.getRed() * a00 + c10.getRed() * a10 + c01.getRed() * a01 + c11.getRed() * a11;
        double g = c00.getGreen() * a00 + c10.getGreen() * a10 + c01.getGreen() * a01 + c11.getGreen() * a11;
        double b = c00.getBlue() * a00 + c10.getBlue() * a10 + c01.getBlue() * a01 + c11.getBlue() * a11;
        double o = c00.getOpacity() * a00 + c10.getOpacity() * a10 + c01.getOpacity() * a01 + c11.getOpacity() * a11;
        return new Color(clamp01(r), clamp01(g), clamp01(b), clamp01(o));
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /**
     * Inverse d’un {@link javafx.scene.effect.PerspectiveTransform} défini par les quatre coins de sortie
     * (même convention UL, UR, LR, LL que l’effet JavaFX) : point dans le repère local <em>après</em> warp
     * visuel → point dans le rectangle contenu {@code [0, inW] × [0, inH]} avant perspective.
     */
    public static Point2D mapPerspectiveWarpedLocalToContentLocal(
            double ulx, double uly,
            double urx, double ury,
            double lrx, double lry,
            double llx, double lly,
            double warpedLocalX,
            double warpedLocalY,
            double inW,
            double inH) {
        double[][] srcPts = {
                {0, 0},
                {inW, 0},
                {inW, inH},
                {0, inH}
        };
        double[][] dstPts = {
                {ulx, uly},
                {urx, ury},
                {lrx, lry},
                {llx, lly}
        };
        double[][] h = homographySrcToDst(srcPts, dstPts);
        double[][] hInv = invert3x3(h);
        double ox = warpedLocalX;
        double oy = warpedLocalY;
        double wx = hInv[0][0] * ox + hInv[0][1] * oy + hInv[0][2];
        double wy = hInv[1][0] * ox + hInv[1][1] * oy + hInv[1][2];
        double wz = hInv[2][0] * ox + hInv[2][1] * oy + hInv[2][2];
        if (Math.abs(wz) < 1e-9) {
            throw new IllegalStateException("Perspective inverse singulière");
        }
        return new Point2D(wx / wz, wy / wz);
    }

    /**
     * Homographie H telle que (dx,dy,1) ~ H * (sx,sy,1) (coordonnées homogènes).
     */
    private static double[][] homographySrcToDst(double[][] src, double[][] dst) {
        double[][] aug = new double[8][9];
        for (int i = 0; i < 4; i++) {
            double sx = src[i][0];
            double sy = src[i][1];
            double dx = dst[i][0];
            double dy = dst[i][1];
            aug[2 * i][0] = sx;
            aug[2 * i][1] = sy;
            aug[2 * i][2] = 1;
            aug[2 * i][6] = -sx * dx;
            aug[2 * i][7] = -sy * dx;
            aug[2 * i][8] = dx;

            aug[2 * i + 1][3] = sx;
            aug[2 * i + 1][4] = sy;
            aug[2 * i + 1][5] = 1;
            aug[2 * i + 1][6] = -sx * dy;
            aug[2 * i + 1][7] = -sy * dy;
            aug[2 * i + 1][8] = dy;
        }
        solveAugmented(aug);
        double h00 = aug[0][8];
        double h01 = aug[1][8];
        double h02 = aug[2][8];
        double h10 = aug[3][8];
        double h11 = aug[4][8];
        double h12 = aug[5][8];
        double h20 = aug[6][8];
        double h21 = aug[7][8];
        return new double[][]{
                {h00, h01, h02},
                {h10, h11, h12},
                {h20, h21, 1.0}
        };
    }

    private static void solveAugmented(double[][] aug) {
        int n = 8;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            double best = Math.abs(aug[col][col]);
            for (int row = col + 1; row < n; row++) {
                double v = Math.abs(aug[row][col]);
                if (v > best) {
                    best = v;
                    pivot = row;
                }
            }
            if (best < 1e-12) {
                throw new IllegalStateException("Homographie indéterminée (points alignés ?)");
            }
            if (pivot != col) {
                double[] tmp = aug[col];
                aug[col] = aug[pivot];
                aug[pivot] = tmp;
            }
            double div = aug[col][col];
            for (int c = col; c <= n; c++) {
                aug[col][c] /= div;
            }
            for (int row = 0; row < n; row++) {
                if (row == col) {
                    continue;
                }
                double f = aug[row][col];
                if (f == 0) {
                    continue;
                }
                for (int c = col; c <= n; c++) {
                    aug[row][c] -= f * aug[col][c];
                }
            }
        }
    }

    private static double[][] invert3x3(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2];
        double d = m[1][0], e = m[1][1], f = m[1][2];
        double g = m[2][0], h = m[2][1], i = m[2][2];
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-12) {
            throw new IllegalStateException("Matrice perspective singulière");
        }
        double invDet = 1.0 / det;
        return new double[][]{
                {(e * i - f * h) * invDet, (c * h - b * i) * invDet, (b * f - c * e) * invDet},
                {(f * g - d * i) * invDet, (a * i - c * g) * invDet, (c * d - a * f) * invDet},
                {(d * h - e * g) * invDet, (b * g - a * h) * invDet, (a * e - b * d) * invDet}
        };
    }
}
