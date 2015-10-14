package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.Image;
import android.util.Log;
import org.ddogleg.struct.FastQueue;
import java.util.ArrayList;
import java.util.List;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerKltPyramid;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;

/**
 * Created by ericbhatti on 10/13/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 13 October, 2015
 */
public class ColouredTrackingProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

    private Bitmap bm;
    DetectDescribePoint<ImageFloat32,Desc> detDesc;
    AssociateDescription<Desc> associate;

    FastQueue<Desc> listSrc;
    FastQueue<Desc> listCalSrc;
    FastQueue<Desc> listDst;
    FastQueue<Point2D_F64> locationCalSrc = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> locationSrc = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    FastQueue<Point2D_F64> locationDst = new FastQueue<Point2D_F64>(Point2D_F64.class,true);
    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;
    List<Point2D_F64> pointsSrc = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDst = new ArrayList<Point2D_F64>();
    PkltConfig pkltConfig = new PkltConfig();
    //PointTracker<ImageFloat32> tracker;
    Paint paint = new Paint();
    VirtualTourPointTracker<ImageFloat32,ImageFloat32> virtualTourPointTracker;
    private boolean initDone = false;
    private boolean trackLost = true;
    public ColouredTrackingProcessing(Context context, int width, int height){
        super(ImageType.ms(3, ImageFloat32.class));

        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH,CreateDetectorDescriptor.DESC_SURF,ImageFloat32.class);
        bm= BitmapFactory.decodeResource(context.getResources(), R.drawable.mark);
        this.width = width;
        this.height = height;
        listSrc = UtilFeature.createQueue(detDesc, 10);
        listDst = UtilFeature.createQueue(detDesc,10);
        ScoreAssociation score = FactoryAssociation.scoreEuclidean(detDesc.getDescriptionType(), true);
        Log.e("ERBL", score.getScoreType().toString());
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(-1, 1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(1,-1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        associate = FactoryAssociation.greedy(score,0.09,true);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_image);
        ImageFloat32 imageFloat32 = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap, imageFloat32, storage);
        detDesc.detect(imageFloat32);
        describeImage(listSrc, locationSrc);
        associate.setSource(listSrc);
        output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);
        pkltConfig.templateRadius = 3;
        pkltConfig.pyramidScaling = new int[]{1,2,4,8};
        PkltConfig pkltConfig = new PkltConfig();
        pkltConfig.templateRadius = 3;
        pkltConfig.pyramidScaling = new int[]{1,2,4,8};
        InterpolateRectangle<ImageFloat32> interpInput = FactoryInterpolation.<ImageFloat32>bilinearRectangle(ImageFloat32.class);
        InterpolateRectangle<ImageFloat32> interpDeriv = FactoryInterpolation.<ImageFloat32>bilinearRectangle(ImageFloat32.class);

        ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel(ImageFloat32.class, ImageFloat32.class);

        PyramidDiscrete<ImageFloat32> pyramid = FactoryPyramid.discreteGaussian(pkltConfig.pyramidScaling, -1, 2, true, ImageFloat32.class);

        virtualTourPointTracker = new VirtualTourPointTracker<>(pkltConfig.config, pkltConfig.templateRadius, pyramid, gradient, interpInput, interpDeriv,
                ImageFloat32.class);
  }

    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            Paint paint2 = new Paint();
            paint2.setColor(Color.RED);
            paint2.setStrokeWidth(5);
            paint2.setTextSize(20);
            Paint paint3 = new Paint();
            paint3.setColor(Color.GREEN);
            paint3.setStrokeWidth(5);
            paint2.setTextSize(20);

            Log.e("VIU: Active Tracks", String.valueOf(virtualTourPointTracker.getActiveTracks(null).size()));
            Log.e("VIU: InActive Tracks", String.valueOf(virtualTourPointTracker.getInactiveTracks(null).size()));
            Log.e("VIU: All Tracks", String.valueOf(virtualTourPointTracker.getAllTracks(null).size()));
            Log.e("VIU: New Tracks", String.valueOf(virtualTourPointTracker.getDroppedTracks(null).size()));

            if(initDone) {
                for (Object o : virtualTourPointTracker.getNewTracks(null)) {
                    PointTrack p = (PointTrack) o;
                    canvas.drawPoint(
                            (int) p.x, (int) p.y, paint3);
                }
            }
            else {
                for (Object o : virtualTourPointTracker.getActiveTracks(null)) {
                    PointTrack p = (PointTrack) o;
                    int red = (int) (2.5 * (p.featureId % 100));
                    int green = (int) ((255.0 / 150.0) * (p.featureId % 150));
                    int blue = (int) (p.featureId % 255);
                    canvas.drawPoint((int) p.x, (int) p.y, paint2);
                }
                if(virtualTourPointTracker.getActiveTracks(null).size() > 0)
                {
                    initDone = true;
                    trackLost = false;
                }
                else
                {
                    initDone = false;
                    trackLost = true;
                }
            }
            Log.e("Features_D", String.valueOf(pointsDst.size()));
            Log.e("Features_S", String.valueOf(pointsSrc.size()));
        }
    }

    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {

        synchronized (new Object()) {
            ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
            outputGUI = output;
        }
        synchronized (new Object()) {
            ImageFloat32 gray = new ImageFloat32(imageFloat32MultiSpectral.width,imageFloat32MultiSpectral.height);
            if(trackLost) {
                ConvertImage.average(imageFloat32MultiSpectral, gray);
                detDesc.detect(gray);
                describeImage(listDst,locationDst);
                associate.setDestination(listDst);
                associate.associate();
                pointsSrc.clear();
                pointsDst.clear();
                FastQueue<AssociatedIndex> matches = associate.getMatches();
                for (int i = 0; i < matches.size; i++) {
                    AssociatedIndex m = matches.get(i);
                    pointsSrc.add(locationSrc.get(m.src));
                    pointsDst.add(locationDst.get(m.dst));
                    Point2D_F64 point = locationDst.get(m.dst);
                    virtualTourPointTracker.found.add((int) point.getX(), (int) point.getY());
                    virtualTourPointTracker.addTrack((int) point.getX(), (int) point.getY());
                }
                initDone = false;
            }
            virtualTourPointTracker.process(gray);
            if( virtualTourPointTracker.getActiveTracks(null).size() < 130 )
                virtualTourPointTracker.spawnTracks();
            if(virtualTourPointTracker.getActiveTracks(null).size() == 0) {
                    trackLost = true;
                int size = virtualTourPointTracker.found.size();
                    for(int i = 0 ; i < size; i++ )
                    virtualTourPointTracker.found.remove(i);
            }



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

