package com.cloudpos.diagonallinetest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectMethods extends Activity { //Scan image with OpenCV to find print errors
    private static final String TAG = "PATH";
    private ImageView mImageView;
    public Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
    }

    private void show_wait_destroy(Context context, Bitmap bmp) { //display image

        //display onandorid device
        setContentView(R.layout.print_detect);
        mImageView = findViewById(R.id.detected);
//        mImageView.setImageResource();


//        mImageView = mImageView.findViewById(R.id.tutorial1_activity_java_surface_view);
//        String path = System.getProperty("user.dir") + filename; //img path
//        Log.i(TAG, path);
//        mImageView.setImageBitmap(BitmapFactory.decodeFile(path));
//        path = "User/home/tiana/Downloads/wizarpos/DiagonalLines/app/src/main/java/com/cloudpos/diagonallinetest/diaglines-both.png";
        mImageView.setImageBitmap(bmp);
    }

    private Mat read_img(String img) throws IOException { //read file
//        File f = new File(img);
//        String absolute = f.getAbsolutePath();
//        Path path = Paths.get(img);
//        Log.i(TAG, path.toString());
//        Path absolute = path.getRoot();
//        String absolute = System.getProperty("user.dir") + img; //img path
//        mContext = this;
//
        Bitmap bitmap = BitmapFactory.decodeStream(mContext.getResources().getAssets().open(img));
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
//        show_wait_destroy(context, bmp2);
        return resized;
    }

    private Mat preprocess(Mat src) { //Process the image
        Mat norm = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
        Core.normalize(src, norm, 0, 255, Core.NORM_MINMAX); //normalize image
        Mat denoised = new Mat(src.rows(), src.cols(), src.type());
        Photo.fastNlMeansDenoisingColored(src, denoised, 10, 10, 7, 15); //reduce noise
        Mat buf = new Mat(src.rows(), src.cols(), src.type());
        Core.addWeighted(denoised, 1.2, denoised, 0, 0, buf); //increase contrast
        Mat gray = new Mat(src.rows(), src.cols(), src.type());
        Imgproc.cvtColor(buf, gray, Imgproc.COLOR_BGR2GRAY, 0); //change to grayscale
        Mat gray_not = new Mat(src.rows(), src.cols(), src.type());
        Core.bitwise_not(gray, gray_not); //invert image
        return gray_not;
    }

    private Mat find_square(Mat gray) {
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

        Imgproc.rectangle(blank, new Point(left,top), new Point(right,bottom), new Scalar (255, 255, 255), Imgproc.FILLED); //draw rectangle around contours

        Mat gray_not = new Mat(gray.rows(), gray.cols(), gray.type());
        Core.bitwise_not(gray, gray_not);
        Core.bitwise_and(gray_not, blank, blank);
        return blank;
    }

    private Mat find_horiz(Mat filtered) { //find horizontal lines
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

    private Mat find_vert(Mat filtered) { //find vertical lines
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

    private int getAxis(double theta) {
        if (1.35 < theta && theta < 1.7) return 0; //horizontal
        if (0 <= theta && theta < 0.2) return 1; //vertical
        return 2;
    }
    private int[] draw_lines(Mat lines, Mat draw) { //draw horizontal and vertical lines where there are errors in printing
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
        vert.sort((o1, o2) -> Double.compare(o2[0], o1[0])); //check if similar lines have already been drawn
        double prevh = 0;
        if (!vert.isEmpty()) prevh = vert.get(0)[0];
        List<double[]> new_vert = new ArrayList<>();
        for (double[] i : vert) {
            if (i[0] - prevh > 6) new_vert.add(i);
        }

        if (!new_vert.isEmpty()) {
            new_vert.remove(new_vert.size() - 1);
        }

        for (double[] i : horiz) {
            Imgproc.line(draw, new Point(i[1], i[2]), new Point(i[3], i[4]), new Scalar(255, 200, 180), 2);
        }
        for (double[] i : new_vert) {
            Imgproc.line(draw, new Point(i[1], i[2]), new Point(i[3], i[4]), new Scalar(255, 105, 180), 2);
        }
        return new int[] {horiz.size(), vert.size()};
    }
    private Bitmap find_lines(String name, Mat src) { //find where the errors are
        Mat draw = src.clone();
        Mat gray = preprocess(draw);

        Mat buf = find_square(gray);

        Mat hlines = find_horiz(gray);
        Mat vlines = find_vert(buf);
        int[] h = draw_lines(hlines, draw);
        int[] v = draw_lines(vlines, draw);

        int horiz_errors = h[0] + v[0];
        int vert_errors = h[1] + v[1];
        if (horiz_errors > 0) System.out.println(horiz_errors + " horizontal error(s) detected");
        if (vert_errors > 2) System.out.println(vert_errors + " vertical error(s) detected");
        boolean saved = Imgcodecs.imwrite("HERE.png", draw);
        Log.i(TAG, "detected image saved: " + saved);
//        show_wait_destroy("detected.png");
        Bitmap bmp = Bitmap.createBitmap(draw.cols(), draw.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(draw, bmp);
        return bmp;
    }

    public void detect(String arg, Context context, View v) throws IOException { //main function to call
        //read file
        CameraMethods cameraMethods = new CameraMethods();
        cameraMethods.init(context, v);
        cameraMethods.dispatchTakePictureIntent();
        cameraMethods.setPic();
        Mat src = read_img(arg);
        Bitmap bmp = find_lines(arg, src);
        cameraMethods.setPic(bmp);
    }
}
