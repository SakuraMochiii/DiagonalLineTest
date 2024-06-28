package com.cloudpos.diagonallinetest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectMethods {
    private static final String TAG = "PATH";
    public Context mContext;


    public void show_wait_destroy(Bitmap bmp) { //display image

        //display onandorid device
        ImageView mImageView = null;
//        mImageView = mImageView.findViewById(R.id.detected);
//        String path = System.getProperty("user.dir") + filename; //img path
//        Log.i(TAG, path);
//        mImageView.setImageBitmap(BitmapFactory.decodeFile(path));
//        path = "User/home/tiana/Downloads/wizarpos/DiagonalLines/app/src/main/java/com/cloudpos/diagonallinetest/diaglines-both.png";
        mImageView.setImageBitmap(bmp);
    }
    public Mat read_img(Context context, String img) throws IOException {
//        File f = new File(img);
//        String absolute = f.getAbsolutePath();
//        Path path = Paths.get(img);
//        Log.i(TAG, path.toString());
//        Path absolute = path.getRoot();
//        String absolute = System.getProperty("user.dir") + img; //img path
//        mContext = this;
//
        Bitmap bitmap = BitmapFactory.decodeStream(context.getResources().getAssets().open(img));
//
//        Log.i(TAG, absolute.toString());
//        String s = "/home/tiana/Downloads/wizarpos/DiagonalLines/app/src/main/java/com/cloudpos/diagonallinetest/diaglines-both.png";

//        Mat src = Imgcodecs.imread(bitmap, Imgcodecs.IMREAD_COLOR);
        Mat src = new Mat();
        Bitmap bmp2 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmap, src);

        int w = src.width();
        int h = src.height();

        Mat resized = new Mat();
        Size sz = new Size(500, (double) (500 * w) / h);
        Imgproc.resize(src, resized, sz); //resize image to 500 px wide
        show_wait_destroy(bmp2);
        return resized;
    }

    public Mat preprocess(Mat src) {
        Mat norm = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        Core.normalize(src, norm, 0, 255, Core.NORM_MINMAX);
        Mat denoised = new Mat(src.rows(), src.cols(), src.type());
        Photo.fastNlMeansDenoisingColored(src, denoised, 10, 10, 7, 15);
        Mat buf = new Mat(src.rows(), src.cols(), src.type());
        Core.addWeighted(denoised, 1.2, denoised, 0, 0, buf);
        Mat gray = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(buf, gray, Imgproc.COLOR_BGR2GRAY, 0);
        Mat gray_not = new Mat(src.rows(), src.cols(), src.type());
        Core.bitwise_not(gray, gray_not);
        return gray_not;
    }

    public Mat find_square(Mat gray) {
        Mat edges = new Mat(gray.rows(), gray.cols(), gray.type());
        Imgproc.Canny(gray, edges, 200, 200, 3, false);
        List<MatOfPoint> cnts = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, cnts, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); //draw outline
//        MatOfPoint cnt;
//        if (cnts.size() == 2) cnt = cnts.get(0);
//        else cnt = cnts.get(1);
        Mat blank = new Mat(gray.rows(), gray.cols(), gray.type(), new Scalar(0));
//        List<int[]> boxes = new ArrayList<>();
        int left = gray.cols();
        int top = gray.rows();
        int right = 0;
        int bottom = 0;
        for (MatOfPoint c : cnts) {
            Rect r = Imgproc.boundingRect(c);
//            int[] coords = {r.x, r.y, r.x + r.width, r.y + r.height};
            if (r.x < left) left = r.x;
            if (r.x + r.width > right) right = r.x + r.width;
            if (r.y < top) top = r.y;
            if (r.y + r.height > bottom) bottom = r.y + r.height;
//            boxes.add(coords);
        }

        Imgproc.rectangle(blank, new Point(left,top), new Point(right,bottom), new Scalar (255, 255, 255), Imgproc.FILLED);

        Mat gray_not = new Mat(gray.rows(), gray.cols(), gray.type());
        Core.bitwise_not(gray, gray_not);
        Core.bitwise_and(gray_not, blank, blank);
        return blank;
    }

    public Mat find_horiz(Mat filtered) {
        Mat bw = new Mat(filtered.rows(), filtered.cols(), filtered.type());
        Imgproc.adaptiveThreshold(filtered, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);
        Mat horizontal = bw.clone();
        Size horizontal_size = new Size((double) horizontal.width() / 20, 1);
        Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, horizontal_size);
        Imgproc.erode(horizontal, horizontal, horizontalStructure);
        Imgproc.dilate(horizontal, horizontal, horizontalStructure);
        Mat edges = new Mat(horizontal.rows(), horizontal.cols(), horizontal.type());
        Imgproc.Canny(horizontal, edges, 100, 150, 3);
        Mat lines = new Mat(edges.rows(), edges.cols(), edges.type());
        Imgproc.HoughLines(edges, lines, 2, Math.PI/180, 130);
        return lines;
    }

    public Mat find_vert(Mat filtered) {
        Mat bw = new Mat(filtered.rows(), filtered.cols(), filtered.type());
        Imgproc.adaptiveThreshold(filtered, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);
        // find vertical
        Mat vertical = bw.clone();
        Size verticalsize = new Size(1, vertical.height());
        Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, verticalsize);
        Imgproc.erode(vertical, vertical, verticalStructure);
        Imgproc.dilate(vertical, vertical, verticalStructure);
        Core.bitwise_not(vertical, vertical);

        Mat edges = new Mat(vertical.rows(), vertical.cols(), vertical.type());
        Imgproc.adaptiveThreshold(vertical, edges, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);
        Mat kernel = Mat.ones(2, 2, CvType.CV_8U);
        Imgproc.dilate(edges, edges, kernel);
        Mat smooth = vertical.clone();
        Imgproc.blur(smooth, smooth, new Size(2, 2));

        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (int i = 0; i < edges.rows(); i++) {
            for (int j = 0; j < edges.cols(); j++) {
                double[] zeros = {0, 0, 0};
                if (edges.get(i, j) == zeros) {
                    rows.add(i);
                    cols.add(j);
                }
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < cols.size(); j++) {
                vertical.put(rows.get(i), cols.get(j), smooth.get(rows.get(i), cols.get(j)));
            }
        }

        Imgproc.Canny(vertical, edges, 100, 150, 3);
        Mat lines = new Mat(edges.rows(), edges.cols(), edges.type());
        Imgproc.HoughLines(edges, lines, 1, Math.PI/180, 130);
        return lines;
    }

    public int getAxis(double theta) {
        if (1.35 < theta && theta < 1.7) return 0;
        if (0 <= theta && theta < 0.2) return 1;
        return 2;
    }
    public int[] draw_lines(Mat lines, Mat draw) {
        if (lines == null) return new int[] {0, 0};

        List<double[]> horiz = new ArrayList<>();
        List<double[]> vert = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            for (int j = 0; j < lines.cols(); j++) {
                double[] r_theta = lines.get(i, j);
                double r = r_theta[0];
                double theta = r_theta[1];
                double a = Math.cos(theta);
                double b = Math.sin(theta);
                double x0 = a * r;
                double y0 = b * r;
                int x1 = (int) (x0 + 1000 * (-b));
                int y1 = (int) (y0 + 1000 * (a));
                int x2 = (int) (x0 - 1000 * (-b));
                int y2 = (int) (y0 - 1000 * (a));

                int axis = getAxis(theta);

                boolean drawLine;
                if (axis == 0) {
                    drawLine = true;
                    for (double[] h : horiz) {
                        if (y0 - 0.6 < h[0] && h[0] < y0 + 0.6) {
                            drawLine = false;
                            break;
                        }
                    }
                    if (drawLine) {
                        double[] points = new double[] {y0, x1, y1, x2, y2};
                        horiz.add(points);
                    }
                }

                if (axis == 1) {
                    drawLine = true;
                    for (double[] v : vert) {
                        if (x0 - 0.1 < v[0] && v[0] < x0 + 0.1) {
                            drawLine = false;
                            break;
                        }
                    }
                    if (drawLine) {
                        double[] points = new double[] {x0, x1, y1, x2, y2};
                        vert.add(points);
                    }
                }
            }
        }
        Collections.sort(vert, (o1, o2) -> Double.compare(o2[0], o1[0]));
        double prevh = 0;
        if (!vert.isEmpty()) prevh = vert.get(0)[0];
        List<double[]> new_vert = new ArrayList<>();
        for (double[] i : vert) {
            if (i[0] - prevh > 6) new_vert.add(i);
        }
        new_vert.remove(new_vert.size() - 1);

        for (double[] i : horiz) {
            Imgproc.line(draw, new Point(i[1], i[2]), new Point(i[3], i[4]), new Scalar(255, 200, 180), 2);
        }
        for (double[] i : new_vert) {
            Imgproc.line(draw, new Point(i[1], i[2]), new Point(i[3], i[4]), new Scalar(255, 105, 180), 2);
        }
        return new int[] {horiz.size(), vert.size()};
    }
    public void find_lines(String name, Mat src) {
        Mat draw = src.clone();
        Mat gray = preprocess(draw);

        Mat buf = find_square(gray);

        Mat hlines = find_horiz(gray);
        Mat vlines = find_vert(buf);
        int[] h = draw_lines(hlines, draw);
        int[] v = draw_lines(vlines, draw);

        if (h[0] + v[0] > 0) System.out.println("horizontal error detected");
        if (h[1] + v[1] > 2) System.out.println("vertical error detected");
        Imgcodecs.imwrite("HERE.png", draw);
//        show_wait_destroy("detected.png");
    }

    public void detect(Context context, String arg) throws IOException {
        //read file
        Mat src = read_img(context, arg);
        find_lines(arg, src);
    }
}
