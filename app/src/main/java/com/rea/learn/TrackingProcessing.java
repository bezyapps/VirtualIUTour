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
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerKltPyramid;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Created by BezyApps on 9/12/2015.
 */
public class TrackingProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<ImageFloat32> {

    DetectDescribePoint<ImageFloat32, Desc> detDesc;
    AssociateDescription<Desc> associate;

    FastQueue<Desc> listSrc;
    FastQueue<Desc> listCalSrc;
    FastQueue<Desc> listDst;
    FastQueue<Point2D_F64> locationCalSrc = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    FastQueue<Point2D_F64> locationSrc = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    FastQueue<Point2D_F64> locationDst = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    PointTrackerKltPyramid<ImageFloat32, ImageFloat32> imageFloat32ImageFloat32PointTrackerKltPyramid;
     List<Point2D_F64> pointsSrc = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDst = new ArrayList<Point2D_F64>();
    PointTracker<ImageFloat32> tracker ;

    Paint paint = new Paint();

    public TrackingProcessing(Context context, int width, int height) {
        super(ImageType.single(ImageFloat32.class));

        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageFloat32.class);
        //imageFloat32ImageFloat32PointTrackerKltPyramid = new PointTrackerKltPyramid<ImageFloat32, ImageFloat32>();
        PkltConfig config = new PkltConfig();
        config.pyramidScaling = new int[]{1, 2, 4};
        config.templateRadius = 3;
      //  tracker = FactoryPointTracker.combined_FH_SURF_KLT(config,0,detDesc.);
               // = PointTrackerKltPyramid
                //FactoryPointTracker.klt(new int[]{1, 2, 4}, detDesc., 3, ImageUInt8.class, ImageSInt16.class);

        //// AMMAR BEGINS
        //        ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        ///   associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);

        listSrc = UtilFeature.createQueue(detDesc, 10);
        listDst = UtilFeature.createQueue(detDesc, 10);

        // ScoreAssociation score = FactoryAssociation.scoreEuclidean(TupleDesc.class,true);
        // associate = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);


        //ScoreAssociation score = FactoryAssociation.scoreSad(detDesc.getDescriptionType());

        // ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        // Log.e("ERBL",score.);
        // Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        ScoreAssociation score = FactoryAssociation.scoreEuclidean(detDesc.getDescriptionType(), true);
        Log.e("ERBL", score.getScoreType().toString());
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(-1, 1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(1, -1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        associate = FactoryAssociation.greedy(score, 0.2, true);


//// AMMAR ENDS

        ///// We read the object image from resources. If you want to try you own image, then put a image in the
        ///// res/drawable folder with the name of 'camera_image.jpg' because that is the image we get here
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_image);
        ImageFloat32 imageFloat32 = new ImageFloat32(bitmap.getWidth(), bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap, imageFloat32, storage);
        ///// This is the main line which i don't understand, i am trying to figure it out. This line
        ///// detects interest points from the image, but where does it store them???

        //// We are processing the object image in the constructor because we don't want to recompute
        //// it feature whenever a new feature is received. Hence saving computation.
        //ok
        detDesc.detect(imageFloat32);
        describeImage(listSrc, locationSrc);
        associate.setSource(listSrc);

        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);

    }


    @Override
    protected void process(ImageFloat32 gray) {

        /// This is the callback in which each frame is received. Processing is done in same way
        /// as in constructor
        detDesc.detect(gray);
        describeImage(listDst, locationDst);
        associate.setDestination(listDst);
        associate.associate();
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        synchronized (new Object()) {
            pointsSrc.clear();
            pointsDst.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for (int i = 0; i < matches.size; i++) {
                AssociatedIndex m = matches.get(i);
                //   m.
                pointsSrc.add(locationSrc.get(m.src));
                pointsDst.add(locationDst.get(m.dst));
            }
        }
    }

    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object()) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            for (int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())), Float.parseFloat(String.valueOf(point2D_f64.getY())), 2, paint);
            }
            Log.e("Features_D", String.valueOf(pointsDst.size()));
            Log.e("Features_S", String.valueOf(pointsSrc.size()));

        }
    }

    private void describeImage(FastQueue<Desc> listDesc, FastQueue<Point2D_F64> listLoc) {
        listDesc.reset();
        listLoc.reset();
        int N = detDesc.getNumberOfFeatures();
        for (int i = 0; i < N; i++) {
            listLoc.grow().set(detDesc.getLocation(i));
            listDesc.grow().setTo(detDesc.getDescription(i));
        }
    }
}
