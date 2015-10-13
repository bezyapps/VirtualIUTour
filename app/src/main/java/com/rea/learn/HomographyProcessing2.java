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
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.geo.robust.DistanceHomographySq;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homography.HomographyPointOps_F64;

/**
 * Created by ericbhatti on 9/8/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 08 September, 2015
 */
public class HomographyProcessing2<Desc extends TupleDesc> extends VideoRenderProcessing<ImageFloat32> {


    DetectDescribePoint<ImageFloat32,Desc> detDesc;
    AssociateDescription<Desc> associate;

    FastQueue<Desc> listSrc;
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
    List<Point2D_F64> pointsDst = new ArrayList<Point2D_F64>();

    private ImageFloat32 object;
    private ImageFloat32 frame;


    Paint paint = new Paint();
    ModelManager<Homography2D_F64> manager = new ModelManagerHomography2D_F64();
    GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(true);
    DistanceHomographySq distance = new DistanceHomographySq();
    ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher = new Ransac<Homography2D_F64,AssociatedPair>(123,manager,modelFitter,distance,60,9);

    Homography2D_F64 homography2D_f64 = null;

    public HomographyProcessing2(Context context, int width, int height) {
        super(ImageType.single(ImageFloat32.class));

        Log.e("LEARN", String.valueOf(modelFitter.getMinimumPoints()));
        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageFloat32.class);
        listSrc = UtilFeature.createQueue(detDesc, 100);
        listDst = UtilFeature.createQueue(detDesc,100);
        ScoreAssociation score = FactoryAssociation.scoreEuclidean(detDesc.getDescriptionType(),true);
        associate = FactoryAssociation.greedy(score,Double.MAX_VALUE,true);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.camera_image);
        object = new ImageFloat32(bitmap.getWidth(),bitmap.getHeight());
        byte[] storage = null;
        ConvertBitmap.declareStorage(bitmap, storage);
        ConvertBitmap.bitmapToGray(bitmap,object,storage);

        detDesc.detect(object);
        describeImage(listSrc, locationSrc);
        associate.setSource(listSrc);

        output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);

    }


        /**
     * Using abstracted code, find a transform which minimizes the difference between corresponding features
     * in both images.  This code is completely model independent and is the core algorithms.
     */
    public static<T extends ImageSingleBand, FD extends TupleDesc> Homography2D_F64
    computeTransform( T imageA , T imageB ,
                      DetectDescribePoint<T,FD> detDesc ,
                      AssociateDescription<FD> associate ,
                      ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher )
    {
        // get the length of the description
        List<Point2D_F64> pointsA = new ArrayList<Point2D_F64>();
        FastQueue<FD> descA = UtilFeature.createQueue(detDesc, 100);
        List<Point2D_F64> pointsB = new ArrayList<Point2D_F64>();
        FastQueue<FD> descB = UtilFeature.createQueue(detDesc,100);



        // extract feature locations and descriptions from each image
        describeImage(imageA, detDesc, pointsA, descA);
        describeImage(imageB, detDesc, pointsB, descB);

        // Associate features between the two images
        associate.setSource(descA);
        associate.setDestination(descB);
        associate.associate();

        // create a list of AssociatedPairs that tell the model matcher how a feature moved
        FastQueue<AssociatedIndex> matches = associate.getMatches();
        List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();

        for( int i = 0; i < matches.size(); i++ ) {
            AssociatedIndex match = matches.get(i);

            Point2D_F64 a = pointsA.get(match.src);
            Point2D_F64 b = pointsB.get(match.dst);

            pairs.add( new AssociatedPair(a,b,false));
        }

        // find the best fit model to describe the change between these images
        if( !modelMatcher.process(pairs) )
            throw new RuntimeException("Model Matcher failed!");

        // return the found image transform
        return modelMatcher.getModelParameters().copy();
    }

    /**
     * Detects features inside the two images and computes descriptions at those points.
     */
    private static <T extends ImageSingleBand, FD extends TupleDesc>
    void describeImage(T image,
                       DetectDescribePoint<T,FD> detDesc,
                       List<Point2D_F64> points,
                       FastQueue<FD> listDescs) {
        detDesc.detect(image);

        listDescs.reset();
        for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
            points.add( detDesc.getLocation(i).copy() );
            listDescs.grow().setTo(detDesc.getDescription(i));
        }
    }


    private static Point2D_I32 renderPoint( int x0 , int y0 , Homography2D_F64 fromBtoWork )
    {
        Point2D_F64 result = new Point2D_F64();
        HomographyPointOps_F64.transform(fromBtoWork, new Point2D_F64(x0, y0), result);
        return new Point2D_I32((int)result.x,(int)result.y);
    }

    @Override
    protected void process(ImageFloat32 imageFloat32) {
        detDesc.detect(imageFloat32);
        describeImage(listDst,locationDst);
        associate.setDestination(listDst);
        associate.associate();
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.grayToBitmap(imageFloat32, output, storage);
            outputGUI = output;
        }
        synchronized (new Object())
        {
            pointsSrc.clear();
            pointsDst.clear();

            FastQueue<AssociatedIndex> matches = associate.getMatches();
            List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();

            for( int i = 0; i < matches.size(); i++ ) {
                AssociatedIndex match = matches.get(i);

                Point2D_F64 a = locationSrc.get(match.src);
                Point2D_F64 b = locationDst.get(match.dst);

                pairs.add( new AssociatedPair(a,b,false));
                pointsDst.add(locationDst.get(match.dst));
            }



            if(pairs.size() >= 4) {
                // find the best fit model to describe the change between these images
                if (!modelMatcher.process(pairs))
                    throw new RuntimeException("Model Matcher failed!");

                // return the found image transform
                homography2D_f64 = modelMatcher.getModelParameters().copy();
            }
            }


    }

    @Override
    protected void render(Canvas canvas, double v) {
        synchronized (new Object())
        {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            frame = ConvertBitmap.bitmapToGray(outputGUI,frame,storage);
            if(homography2D_f64 != null)
            {
                // Convert into a BoofCV color format
                MultiSpectral<ImageFloat32> colorA = new MultiSpectral<ImageFloat32>(ImageFloat32.class,3);
                MultiSpectral<ImageFloat32> colorB = new MultiSpectral<ImageFloat32>(ImageFloat32.class,3);
                colorB.setBands(new ImageFloat32[]{frame,frame,frame});
                colorA.setBands(new ImageFloat32[]{object,object,object});
                MultiSpectral<ImageFloat32> work = new MultiSpectral<ImageFloat32>(ImageFloat32.class,outputWidth,outputHeight,3);

                Homography2D_F64 fromAToWork = new Homography2D_F64(0.5,0,colorA.width/4,0,scale,colorA.height/4,0,0,1);
                Homography2D_F64 fromWorkToA = fromAToWork.invert(null);

                // Used to render the results onto an image
                PixelTransformHomography_F32 model = new PixelTransformHomography_F32();
                ImageDistort<MultiSpectral<ImageFloat32>,MultiSpectral<ImageFloat32>> distort = null;
                //    DistortSupport.createDistortMS(ImageFloat32.class, model, new ImplBilinearPixel_F32(), false, null);


                // Render first image
                model.set(fromWorkToA);
                distort.apply(colorA,work);

                // Render second image
                Homography2D_F64 fromWorkToB = fromWorkToA.concat(homography2D_f64,null);
                model.set(fromWorkToB);
                distort.apply(colorB,work);

                Homography2D_F64 fromBtoWork = fromWorkToB.invert(null);
                Point2D_I32 corners[] = new Point2D_I32[4];
                corners[0] = renderPoint (0,0,fromBtoWork);
                corners[1] = renderPoint(object.width,0,fromBtoWork);
                corners[2] = renderPoint(object.width,object.height,fromBtoWork);
                corners[3] = renderPoint(0,object.height,fromBtoWork);

                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(5);
                canvas.drawLine(corners[0].x,corners[0].y,corners[1].x,corners[1].y,paint);
                canvas.drawLine(corners[1].x,corners[1].y,corners[2].x,corners[2].y,paint);
                canvas.drawLine(corners[2].x,corners[2].y,corners[3].x,corners[3].y,paint);
                canvas.drawLine(corners[3].x,corners[3].y,corners[0].x,corners[0].y,paint);
            }
            else {

                for (int i = 0; i < pointsDst.size(); i++) {
                    Point2D_F64 point2D_f64 = pointsDst.get(i);
                    canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())), Float.parseFloat(String.valueOf(point2D_f64.getY())), 2, paint);
                }
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
