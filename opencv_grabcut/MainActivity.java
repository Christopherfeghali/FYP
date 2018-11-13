package com.example.opencv_grabcut;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_OPEN_IMAGE = 1;

    String mCurrentPhotoPath;
    ImageView imageResult, imageDrawingPane;
    Uri source;

    Bitmap bitmapMaster;
    Bitmap bitmapDrawingPane;
    Canvas canvasMaster;
    Canvas canvasDrawingPane;

    long tStart;
    long tEnd;
    long tDelta;
    double elapsedSeconds;


    projectPt startPt;
    org.opencv.core.Point tl;
    org.opencv.core.Point br;
    boolean targetChose = false;
    ProgressDialog dlg;
    Mat img;

    class projectPt{
        int x;
        int y;

        projectPt(int tx, int ty){
            x = tx;
            y = ty;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System. loadLibrary ("opencv_java3");
        setContentView(R.layout.activity_main);

        imageResult = (ImageView)findViewById(R.id.result);
        imageDrawingPane = (ImageView)findViewById(R.id.drawingpane);
        dlg = new ProgressDialog(this);
        tl = new org.opencv.core.Point();
        br = new org.opencv.core.Point();
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap tempBitmap;

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQUEST_OPEN_IMAGE:

                    source = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(source, filePathColumn,null, null, null);
                    cursor.moveToFirst();

                    int colIndex = cursor.getColumnIndex(filePathColumn[0]);
                    mCurrentPhotoPath = cursor.getString(colIndex);
                    cursor.close();

                    try {
                        //tempBitmap is Immutable bitmap,
                        //cannot be passed to Canvas constructor
                        tempBitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(source));

                        Bitmap.Config config;
                        if(tempBitmap.getConfig() != null){
                            config = tempBitmap.getConfig();
                        }else{
                            config = Bitmap.Config.ARGB_8888;
                        }

                        //bitmapMaster is Mutable bitmap
                        bitmapMaster = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), config);
                        canvasMaster = new Canvas(bitmapMaster);
                        canvasMaster.drawBitmap(tempBitmap, 0, 0, null);

                        imageResult.setImageBitmap(bitmapMaster);

                        //Create bitmap of same size for drawing
                        bitmapDrawingPane = Bitmap.createBitmap(
                                tempBitmap.getWidth(),
                                tempBitmap.getHeight(),
                                config.ARGB_8888);
                        canvasDrawingPane = new Canvas(bitmapDrawingPane);
                        imageDrawingPane.setImageBitmap(bitmapDrawingPane);



                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    break;
            }
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_open_img:


                Intent getPictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getPictureIntent.setType("image/*");
                Intent pickPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooserIntent = Intent.createChooser(getPictureIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickPictureIntent});

                startActivityForResult(chooserIntent, REQUEST_OPEN_IMAGE);

                return true;

            case R.id.action_choose_target:
                if (mCurrentPhotoPath != null)
                    targetChose = false;
                    imageResult.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {

                            int action = event.getAction();
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            switch (action) {
                                case MotionEvent.ACTION_DOWN:
                                    startPt = projectXY((ImageView) v, bitmapMaster, x, y);
                                    tl.x = event.getX();
                                    tl.y = event.getY();
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    drawOnRectProjectedBitMap((ImageView) v, bitmapMaster, x, y);
                                    break;
                                case MotionEvent.ACTION_UP:
                                    drawOnRectProjectedBitMap((ImageView) v, bitmapMaster, x, y);
                                    br.x = event.getX();
                                    br.y = event.getY();
                                    finalizeDrawing();
                                    targetChose = true;
                                    imageResult.setOnTouchListener(null);
                                    break;
                            }
    /*
     * Return 'true' to indicate that the event have been consumed.
     * If auto-generated 'false', your code can detect ACTION_DOWN only,
     * cannot detect ACTION_MOVE and ACTION_UP.
     */
                            return true;
                        }
                    });


                return true;
            case R.id.action_cut_image:
                if (mCurrentPhotoPath != null && targetChose) {
                    new ProcessImageTask().execute();
                    targetChose = false;
                }
                return true;

            case R.id.action_help:
                int  help = 2;
                Intent intent = new Intent(MainActivity.this,Help.class);
                startActivityForResult(intent,help);

        }

        return super.onOptionsItemSelected(item);
    }



    private class ProcessImageTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg.setMessage("Processing Image...");
            dlg.setCancelable(false);
            dlg.setIndeterminate(true);
            dlg.show();
            tStart = System.currentTimeMillis();
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            img = Imgcodecs.imread(mCurrentPhotoPath);
            Mat background = new Mat(img.size(), CvType.CV_8UC3,new Scalar(255, 255, 255));
            Mat firstMask = new Mat();
            Mat bgModel = new Mat();
            Mat fgModel = new Mat();
            Mat mask;
            Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
            Mat dst = new Mat();
            Rect rect = new Rect(tl, br);

            Imgproc.grabCut(img, firstMask, rect, bgModel, fgModel,5, Imgproc.GC_INIT_WITH_RECT);
            Core.compare(firstMask, source, firstMask, Core.CMP_EQ);

            Mat foreground = new Mat(img.size(), CvType.CV_8UC3,
                    new Scalar(255, 255, 255));
            img.copyTo(foreground, firstMask);

            Scalar color = new Scalar(255, 0, 0, 255);
            Imgproc.rectangle(img,tl,br,color);

            Mat tmp = new Mat();
            Imgproc.resize(background, tmp, img.size());
            background = tmp;
            mask = new Mat(foreground.size(), CvType.CV_8UC1, new Scalar(255, 255, 255));

            Imgproc.cvtColor(foreground, mask, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(mask, mask, 254, 255, Imgproc.THRESH_BINARY_INV);
            System.out.println();
            Mat vals = new Mat(1, 1, CvType.CV_8UC3, new Scalar(0.0));
            background.copyTo(dst);

            background.setTo(vals, mask);

            Core.add(background, foreground, dst, mask);

            firstMask.release();
            source.release();
            bgModel.release();
            fgModel.release();
            vals.release();

            Imgcodecs.imwrite(mCurrentPhotoPath + ".png", dst);

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            tEnd = System.currentTimeMillis();
            tDelta = tEnd - tStart;
            elapsedSeconds = tDelta / 1000.0;
            Toast.makeText(MainActivity.this, "Time to process = " +elapsedSeconds + "s", Toast.LENGTH_LONG).show();
            Bitmap jpg = BitmapFactory.decodeFile(mCurrentPhotoPath + ".png");

            imageResult.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageResult.setAdjustViewBounds(true);
            imageResult.setPadding(2, 2, 2, 2);
            imageResult.setImageBitmap(jpg);
            imageResult.invalidate();

            dlg.dismiss();
        }
    }


    private projectPt projectXY(ImageView iv, Bitmap bm, int x, int y){
        if(x<0 || y<0 || x > iv.getWidth() || y > iv.getHeight()){
            //outside ImageView
            return null;
        }else{
            int projectedX = (int)((double)x * ((double)bm.getWidth()/(double)iv.getWidth()));
            int projectedY = (int)((double)y * ((double)bm.getHeight()/(double)iv.getHeight()));

            return new projectPt(projectedX, projectedY);
        }
    }

    private void drawOnRectProjectedBitMap(ImageView iv, Bitmap bm, int x, int y){
        if(x<0 || y<0 || x > iv.getWidth() || y > iv.getHeight()){
            //outside ImageView
            return;
        }else{
            int projectedX = (int)((double)x * ((double)bm.getWidth()/(double)iv.getWidth()));
            int projectedY = (int)((double)y * ((double)bm.getHeight()/(double)iv.getHeight()));

            //clear canvasDrawingPane
            canvasDrawingPane.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(3);
            canvasDrawingPane.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
            imageDrawingPane.invalidate();
        }
    }

    private void finalizeDrawing(){
        canvasMaster.drawBitmap(bitmapDrawingPane, 0, 0, null);
    }

















    }
