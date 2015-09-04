package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
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
import boofcv.alg.feature.UtilFeature;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Created by ericbhatti on 9/1/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 01 September, 2015
 */
public class CSVProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<ImageFloat32> {

    DetectDescribePoint<ImageFloat32,Desc> detDesc;
    AssociateDescription<Desc> associate;

    FastQueue<Desc> listHelmetSrc;

    FastQueue<Desc> listCalSrc;
    FastQueue<Desc> listDst;
    FastQueue<Point2D_F64> locationSrc = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> locationDst = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;

    List<Point2D_F64> pointsSrc = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDstHel = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDstCal = new ArrayList<Point2D_F64>();

    Paint paint = new Paint();
    public CSVProcessing(Context context, int width, int height){
        super(ImageType.single(ImageFloat32.class));

        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH,CreateDetectorDescriptor.DESC_SURF,ImageFloat32.class);


        //// AMMAR BEGINS
        //        ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        ///   associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);

        listHelmetSrc = UtilFeature.createQueue(detDesc, 10);

        listCalSrc = UtilFeature.createQueue(detDesc, 10);
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
        associate = FactoryAssociation.greedy(score,0.1,true);





//// AMMAR ENDS

        ///// We read the object image from resources. If you want to try you own image, then put a image in the
        ///// res/drawable folder with the name of 'camera_image.jpg' because that is the image we get here
       /* Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_image);
        ImageFloat32 imageFloat32 = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap,imageFloat32,storage);

        ///// This is the main line which i don't understand, i am trying to figure it out. This line
        ///// detects interest points from the image, but where does it store them???

        //// We are processing the object image in the constructor because we don't want to recompute
        //// it feature whenever a new feature is received. Hence saving computation.

        detDesc.detect(imageFloat32);
        describeImage(listHelmetSrc, locationSrc);*/
        try {
            CSVToList csvToList = new CSVToList(context);
            csvToList.convertCSVsToList("data.csv");
            Log.e("ERBL_HELMET", String.valueOf(csvToList.listSrc.size()));
            for(SurfFeature surfFeature : csvToList.listSrc.data)
            {
                listHelmetSrc.add((Desc) surfFeature);
                Log.e("TEST", "");
            }
            csvToList.convertCSVsToList("data_2.csv");
            Log.e("ERBL_CAL", String.valueOf(csvToList.listSrc.size()));
            for(SurfFeature surfFeature : csvToList.listSrc.data)
            {
                listCalSrc.add((Desc) surfFeature);
                Log.e("TEST", "");
            }
//            listHelmetSrc = csvToList.listHelmetSrc;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);

    }



    @Override
    protected void process(ImageFloat32 gray) {

        /// This is the callback in which each frame is received. Processing is done in same way
        /// as in constructor
        detDesc.detect(gray);
        describeImage(listDst,locationDst);
        associate.setSource(listHelmetSrc);
        associate.setDestination(listDst);
        associate.associate();
        Log.e("DEST_SIZE" , String.valueOf(listDst.size()));
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        synchronized (new Object())
        {
            //pointsSrc.clear();
            pointsDstHel.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex m = matches.get(i);
                //   m.
          //      pointsSrc.add(locationSrc.get(m.src));
                pointsDstHel.add(locationDst.get(m.dst));
            }
        }
        associate.setSource(listCalSrc);
        associate.associate();

        synchronized (new Object())
        {
            //pointsSrc.clear();
            pointsDstCal.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex m = matches.get(i);
                //   m.
                //      pointsSrc.add(locationSrc.get(m.src));
                pointsDstCal.add(locationDst.get(m.dst));
            }
        }
    }

    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            paint.setColor(Color.RED);
            for(int i = 0; i < pointsDstHel.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDstHel.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            paint.setColor(Color.BLUE);
            for(int i = 0; i < pointsDstCal.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDstCal.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }
            Log.e("Features_HEL", String.valueOf(pointsDstHel.size()));
            Log.e("Features_CAL", String.valueOf(pointsDstCal.size()));

        }
    }

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
