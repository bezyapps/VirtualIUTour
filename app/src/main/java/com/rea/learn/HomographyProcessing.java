package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_F32;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.impl.ImplConvertMsToSingle;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homography.HomographyPointOps_F64;

/**
 * Created by Eric Bhatti on 8/19/2015.
 *
 * I have copied the logic in this class from the Android App demo of BoofCV.
 * I don't know how some of the things work in this class, i have pointed them out further in it.
 * Please help us understand the pointed out methods.
 *
 *
 */
public class HomographyProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

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

    List<Point2D_F64> pointsSrc = new ArrayList<Point2D_F64>();
    List<Point2D_F64> pointsDst = new ArrayList<Point2D_F64>();

    private ImageFloat32 object;
    private ImageFloat32 frame;


    Paint paint = new Paint();
    ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
    GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(true);
    DistanceHomographySq distance = new DistanceHomographySq();
    ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher = new Ransac<Homography2D_F64,AssociatedPair>(123,manager,modelFitter,distance,60,9);

    public HomographyProcessing(Context context, int width, int height){
        super(ImageType.ms(3,ImageFloat32.class));

        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH,CreateDetectorDescriptor.DESC_SURF,ImageFloat32.class);


        //// AMMAR BEGINS
        //        ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
     ///   associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);

        listSrc = UtilFeature.createQueue(detDesc, 10);
        listDst = UtilFeature.createQueue(detDesc,10);

       // ScoreAssociation score = FactoryAssociation.scoreEuclidean(TupleDesc.class,true);
       // associate = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);






        //ScoreAssociation score = FactoryAssociation.scoreSad(detDesc.getDescriptionType());

       // ScoreAssociation score = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
       // Log.e("ERBL",score.);
       // Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        ScoreAssociation score = FactoryAssociation.scoreEuclidean(detDesc.getDescriptionType(),true);
        Log.e("ERBL",score.getScoreType().toString());
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(-1, 1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().compareTo(1,-1)));
        Log.e("ERBL", String.valueOf(score.getScoreType().isZeroBest()));
        associate = FactoryAssociation.greedy(score,0.2,true);




//// AMMAR ENDS

        ///// We read the object image from resources. If you want to try you own image, then put a image in the
        ///// res/drawable folder with the name of 'camera_image.jpg' because that is the image we get here
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_image);
        object = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap,object,storage);

        ///// This is the main line which i don't understand, i am trying to figure it out. This line
        ///// detects interest points from the image, but where does it store them???

        //// We are processing the object image in the constructor because we don't want to recompute
        //// it feature whenever a new feature is received. Hence saving computation.
        //ok
        detDesc.detect(object);
        describeImage(listSrc, locationSrc);
        associate.setSource(listSrc);

        output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);

    }


    Homography2D_F64 homography2D_f64;

//    @Override
    protected void process(ImageFloat32 gray) {

        /// This is the callback in which each frame is received. Processing is done in same way
        /// as in constructor
        detDesc.detect(gray);
        describeImage(listDst,locationDst);
        associate.setDestination(listDst);
        associate.associate();
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(gray, output, storage);
            frame = gray;
            outputGUI = output;
        }
        synchronized (new Object())
        {
            pointsSrc.clear();
            pointsDst.clear();

            List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex match = matches.get(i);
             //   m.
                pointsSrc.add(locationSrc.get(match.src));
                pointsDst.add(locationDst.get(match.dst));
                Point2D_F64 a = pointsSrc.get(match.src);
                Point2D_F64 b = pointsSrc.get(match.dst);
                pairs.add( new AssociatedPair(a,b,false));

                if(!modelMatcher.process(pairs))
                    throw new RuntimeException("Model Matcher failed!");

                 homography2D_f64 = modelMatcher.getModelParameters().copy();
            }

        }


    }

    private static Point2D_I32 renderPoint( int x0 , int y0 , Homography2D_F64 fromBtoWork )
    {
        Point2D_F64 result = new Point2D_F64();
        HomographyPointOps_F64.transform(fromBtoWork, new Point2D_F64(x0, y0), result);
        return new Point2D_I32((int)result.x,(int)result.y);
    }


    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
        ImageFloat32 gray = new ImageFloat32(imageFloat32MultiSpectral.width,imageFloat32MultiSpectral.height);
        ImplConvertMsToSingle.average(imageFloat32MultiSpectral,gray);
        detDesc.detect(gray);
        describeImage(listDst,locationDst);
        associate.setDestination(listDst);
        associate.associate();
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(gray, output, storage);
            frame = gray;
            outputGUI = output;
        }
        synchronized (new Object())
        {
            pointsSrc.clear();
            pointsDst.clear();

            List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
            FastQueue<AssociatedIndex> matches = associate.getMatches();
            for( int i = 0; i < matches.size; i++ ) {
                AssociatedIndex match = matches.get(i);
                //   m.
                pointsSrc.add(locationSrc.get(match.src));
                pointsDst.add(locationDst.get(match.dst));
                Point2D_F64 a = pointsSrc.get(match.src);
                Point2D_F64 b = pointsSrc.get(match.dst);
                pairs.add( new AssociatedPair(a,b,false));

                if(!modelMatcher.process(pairs))
                    throw new RuntimeException("Model Matcher failed!");

                homography2D_f64 = modelMatcher.getModelParameters().copy();
            }

        }

    }

    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object())
        {
            MultiSpectral<ImageFloat32> colorA = new MultiSpectral<ImageFloat32>(ImageFloat32.class,frame.width,frame.height,3);
            colorA.setBands(new ImageFloat32[]{frame,frame,frame});
            MultiSpectral<ImageFloat32> colorB =
                    new MultiSpectral<ImageFloat32>(ImageFloat32.class,object.width,object.height,3);
            colorB.setBands(new ImageFloat32[]{object,object,object});
            // Where the output images are rendered into
            MultiSpectral<ImageFloat32> work = new MultiSpectral<ImageFloat32>(ImageFloat32.class,frame.width,frame.height,3);
            // Adjust the transform so that the whole image can appear inside of it
            Homography2D_F64 fromAToWork = new Homography2D_F64(scale,0,colorA.width/4,0,scale,colorA.height/4,0,0,1);
            Homography2D_F64 fromWorkToA = fromAToWork.invert(null);

            // Used to render the results onto an image
            PixelTransformHomography_F32 model = new PixelTransformHomography_F32();
            ImageDistort<MultiSpectral<ImageFloat32>,MultiSpectral<ImageFloat32>> distort =
                    DistortSupport.createDistortMS(ImageFloat32.class, model, new ImplBilinearPixel_F32(),false, null);
            // Render first image
            model.set(fromWorkToA);
            distort.apply(colorA, work);

            // Render second image
            Homography2D_F64 fromWorkToB = fromWorkToA.concat(homography2D_f64 ,null);
            model.set(fromWorkToB);
            distort.apply(colorB, work);
// draw lines around the distorted image to make it easier to see
            Homography2D_F64 fromBtoWork = fromWorkToB.invert(null);
            Point2D_I32 corners[] = new Point2D_I32[4];
            corners[0] = renderPoint(0,0,fromBtoWork);
            corners[1] = renderPoint(colorB.width,0,fromBtoWork);
            corners[2] = renderPoint(colorB.width,colorB.height,fromBtoWork);
            corners[3] = renderPoint(0,colorB.height,fromBtoWork);

            paint.setColor(Color.RED);
            paint.setStrokeWidth(5);
            canvas.drawLine(corners[0].x,corners[0].y,corners[1].x,corners[1].y,paint);
            canvas.drawLine(corners[1].x,corners[1].y,corners[2].x,corners[2].y,paint);
            canvas.drawLine(corners[2].x,corners[2].y,corners[3].x,corners[3].y,paint);
            canvas.drawLine(corners[3].x,corners[3].y,corners[0].x,corners[0].y,paint);
          /*  for(int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())),Float.parseFloat(String.valueOf(point2D_f64.getY())),2,paint);
            }*/
            Log.e("Features_D", String.valueOf(pointsDst.size()));
            Log.e("Features_S", String.valueOf(pointsSrc.size()));

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
