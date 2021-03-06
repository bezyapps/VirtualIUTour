package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Created by BezyApps on 10/4/2015.
 */
public class RoomTagProcessing <Desc extends TupleDesc> extends VideoRenderProcessing<ImageFloat32> {

    DetectDescribePoint<ImageFloat32,Desc> detDesc;
    AssociateDescription<Desc> associate;

    FastQueue<Desc> list304Src;
    FastQueue<Desc> list303Src;
    FastQueue<Desc> listDst;
    FastQueue<Point2D_F64> location303Src = new FastQueue<>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> location304Src = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> locationSrc = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> locationDst = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    List<Point2D_F64> points303Dst = new ArrayList<Point2D_F64>();
    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;
    List<Point2D_F64> pointsSrc = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDst = new ArrayList<Point2D_F64>();

    Paint paint = new Paint();
    public RoomTagProcessing(Context context, int width, int height){
        super(ImageType.single(ImageFloat32.class));

        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH,CreateDetectorDescriptor.DESC_SURF,ImageFloat32.class);

        this.width = width;
        this.height = height;
        //// AMMAR BEGINS
        //        ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        ///   associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);

        list303Src = UtilFeature.createQueue(detDesc, 10);
        list304Src = UtilFeature.createQueue(detDesc, 10);
        listDst = UtilFeature.createQueue(detDesc,10);

        // ScoreAssociation score = FactoryAssociation.scoreEuclidean(TupleDesc.class,true);
        // associate = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);






        //ScoreAssociation score = FactoryAssociation.scoreSad(detDesc.getDescriptionType());

        // ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        // Log.e("ERBL",score.);
        // Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        ScoreAssociation score = FactoryAssociation.scoreEuclidean(detDesc.getDescriptionType(), true);
        Log.e("ERBL", score.getScoreType().toString());
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(-1, 1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(1,-1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        associate = FactoryAssociation.greedy(score,0.09,true);




//// AMMAR ENDS

        ///// We read the object image from resources. If you want to try you own image, then put a image in the
        ///// res/drawable folder with the name of 'camera_image.jpg' because that is the image we get here
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.room_e_304);
        ImageFloat32 imageFloat32 = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap,imageFloat32,storage);
        ///// This is the main line which i don't understand, i am trying to figure it out. This line
        ///// detects interest points from the image, but where does it store them???

        //// We are processing the object image in the constructor because we don't want to recompute
        //// it feature whenever a new feature is received. Hence saving computation.
        //ok
        detDesc.detect(imageFloat32);
        describeImage(list304Src, location304Src);

        bitmap.recycle();
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.room_e_303);
        imageFloat32 = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap, imageFloat32, storage);
        detDesc.detect(imageFloat32);
        describeImage(list303Src, location303Src);

        Log.e("ERBL CAL: ", String.valueOf(list303Src.size()));


        output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
       /* paint.setColor(Color.BLUE);
        paint.setTextSize(20);*/

    }



    @Override
    protected void process(ImageFloat32 gray) {

        /// This is the callback in which each frame is received. Processing is done in same way
        /// as in constructor
        detDesc.detect(gray);
        describeImage(listDst, locationDst);
        associate.setSource(list304Src);
        associate.setDestination(listDst);
        associate.associate();
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        synchronized (new Object())
        {
            pointsSrc.clear();
            pointsDst.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex m = matches.get(i);
                //   m.
                pointsSrc.add(location304Src.get(m.src));
                pointsDst.add(locationDst.get(m.dst));
            }
        }
        associate.setSource(list303Src);
        associate.associate();
        synchronized (new Object())
        {
            points303Dst.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex m = matches.get(i);
                points303Dst.add(locationDst.get(m.dst));
            }
        }
    }
/*
    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            float avg_X = 0;
            float avg_Y = 0;
            for(int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                avg_X = (float) (avg_X + point2D_f64.getX());
                avg_Y = (float) (avg_Y + point2D_f64.getY());
                //            canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }

            avg_X = avg_X / pointsDst.size();
            avg_Y = avg_Y / pointsDst.size();

            if(!(avg_X == 0 && avg_Y == 0 && pointsDst.size() == 0))
            {
                float left , right , top, bottom;
                left = right = top = bottom = 0;
                if(avg_X - 50 > 0) left = avg_X - 50;
                if(avg_Y - 30 > 0) top = avg_Y - 30;
                if(avg_X + 50 > width)
                {
                    float temp = avg_X + 50 - width;
                    left = left - temp;
                    right = width;
                }
                else
                {
                    right = avg_X + 50;
                }
                if(avg_Y + 30 > height)
                {
                    float temp = avg_Y + 30 - height;
                    top = top - temp;
                    bottom = height;
                }
                else
                {
                    bottom = avg_Y + 30;
                }

                canvas.drawRect(left, top, right, bottom, paint);
                Paint paint2 = new Paint();
                paint2.setColor(Color.RED);
                paint2.setTextSize(20);
                if(left != 0 && top != 0) {
                    canvas.drawText("Card", left + 25, top + 30, paint2);
                }
            }

            Log.e("Features_D", String.valueOf(pointsDst.size()));
            Log.e("Features_S", String.valueOf(pointsSrc.size()));

        }
    }
*/

    @Override

    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            paint.setColor(Color.BLUE);
            for(int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            paint.setColor(Color.RED);
            for(int i = 0; i < points303Dst.size(); i++) {
                Point2D_F64 point2D_f64 = points303Dst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            Log.e("Features_Card", String.valueOf(pointsDst.size()));
            Log.e("Features_Cal", String.valueOf(points303Dst.size()));

        }
    }
   /* protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            for(int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            Paint p = new Paint();
            p.setColor(Color.GREEN);
            p.setTextSize(15);
            canvas.drawText("Features: " + pointsDst.size(), 50, 10, p);
            Log.e("Features_D", String.valueOf(pointsDst.size()));
            Log.e("Features_S", String.valueOf(pointsSrc.size()));
            //303

            canvas.drawBitmap(outputGUI, 0, 0, null);
            paint.setColor(Color.MAGENTA);
            for(int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            Paint p2 = new Paint();
            p.setColor(Color.RED);
            p.setTextSize(15);
            canvas.drawText("Features: " + pointsDst.size(), 50, 10, p2);
            Log.e("Features_D", String.valueOf(points303Dst.size()));
           // Log.e("Features_S", String.valueOf(points303Src.size()));

        }
    }*/
    private void describeImage(FastQueue<Desc> listDesc, FastQueue<Point2D_F64> listLoc) {
        listDesc.reset();
        listLoc.reset();
        int N = detDesc.getNumberOfFeatures();
        for( int i = 0; i < N; i++ ) {
            listLoc.grow().set(detDesc.getLocation(i));
            listDesc.grow().setTo(detDesc.getDescription(i));
        }
    }
}
