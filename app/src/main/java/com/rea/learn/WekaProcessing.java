package com.rea.learn;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.ddogleg.struct.FastQueue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;


/**
 * Created by ericbhatti on 11/4/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 04 November, 2015
 */
public class WekaProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<MultiSpectral<ImageUInt8>> {
    protected WekaProcessing(ImageType<MultiSpectral<ImageUInt8>> imageType) {
        super(imageType);
    }

    /**
     * <p>Image processing is done here and is invoked in its own thread, removing any hard time constraints. When modifying data structures that are read inside of
     * {@link #render} be sure to use synchronize with {@link #lockGui} to avoid crashes or weird visual artifacts.  Use of lockGui should be minimized to ensure a fast
     * and responsive GUI</p>
     *
     * @param gray
     */
    @Override
    protected void process(MultiSpectral<ImageUInt8> gray) {

    }

    /**
     * Results computed by {@link #process} are visualized here.  This function is called inside a synchronize(lockGui) block.  The provided canvas has been adjusted to
     * be centered in the view and to account for the resolution difference of the preview image and the display.
     *
     * @param canvas        Canvas which is to be displayed.
     * @param imageToOutput Scale factor from input image to output display.  Can also be accessed via {@link #getScale}
     */
    @Override
    protected void render(Canvas canvas, double imageToOutput) {

    }
/*

    DetectDescribePoint<ImageUInt8, Desc> detDesc;
    StringBuilder headBuilder;
    FastQueue<Desc> listDst;
    FastQueue<Point2D_F64> locationDst = new FastQueue<Point2D_F64>(Point2D_F64.class, true);
    List<Point2D_F64> points303Dst = new ArrayList<Point2D_F64>();
    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;
    private BayesNet bayesNet;
    Context context;

    String[] tags = {"E - 303", "E - 304"};
    int[] tag_count = {0, 0};

    Paint paint = new Paint();

    private void buildHeader() {
        headBuilder = new StringBuilder("@relation test_set\n\n");
        for (int i = 1; i < 65; i++) {
            headBuilder.append("@attribute data_" + i + " numeric\n");
        }
        headBuilder.append("@attribute class {e303,e304}\n\n");
        headBuilder.append("@data\n");
    }

    private StringBuilder getARFF_Header() {
        return headBuilder;
    }

    private Object getModel(String modelName) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(modelName);
            Object model = ((new ObjectInputStream(inputStream)).readObject());
            return model;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public WekaProcessing(Context context, int width, int height) {
        super(ImageType.ms(3, ImageUInt8.class));
        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageUInt8.class);
        this.width = width;
        this.height = height;
        listDst = UtilFeature.createQueue(detDesc, 10);
        this.context = context;
        buildHeader();
        bayesNet = (BayesNet) getModel("bayesnet.model");
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);
    }


    private boolean allzeros(Desc desc) {
        boolean allzero = true;
        for (int i = 0; i < desc.size(); i++) {
            if (desc.getDouble(i) != 0) {
                return false;
            }
        }
        return true;
    }

    //   volatile int feature_count = 0;


    @Override
    protected void process(MultiSpectral<ImageUInt8> ImageUInt8MultiSpectral) {

        ConverterUtils.DataSource source;
        Instances data;
        StringBuilder dataBuilder;

        ImageUInt8 gray = new ImageUInt8(ImageUInt8MultiSpectral.width, ImageUInt8MultiSpectral.height);
        ConvertImage.average(ImageUInt8MultiSpectral, gray);
        detDesc.detect(gray);
        describeImage(listDst, locationDst);
        long syn_start = System.currentTimeMillis();

        synchronized (lockGui) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.multiToBitmap(ImageUInt8MultiSpectral, output, storage);
            //  ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }


        long syn_end = System.currentTimeMillis();
        Log.e("SYNC TIME" , String.valueOf(syn_end - syn_start));
        long model_start = System.currentTimeMillis();
        //System.out.println("Model Start: " + System.currentTimeMillis());

        dataBuilder = new StringBuilder(getARFF_Header());

        for (Desc element : listDst.data) {
            for (int i = 0; i < element.size(); i++) {
                if (allzeros(element)) continue;
                dataBuilder.append(element.getDouble(i) + ",");
                if (i == element.size() - 1) { //
                    dataBuilder.append("?\n");

                }
            }
        }

        System.gc();

        String model2Str = dataBuilder.toString();
        byte[] bytes = model2Str.getBytes();


        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        source = new ConverterUtils.DataSource(byteArrayInputStream);

        long start = System.currentTimeMillis();
        data = null;
        try {
            data = source.getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("Model Time: " + String.valueOf(end - start));
        int[] tag_count = {0, 0};
        this.tag_count = tag_count;
        data.setClassIndex(data.numAttributes() - 1);
    //    Instances instances =
        int e303 = 0; int e304 = 0;

        for (int i = 0; i < data.numInstances(); i++) {
            Instance ins = data.instance(i);
            int in = -1;
          */
/*  Instance instance = new Instance(64);
            Attribute attribute= new Attribute("data_1",0);
            attribute.setWeight(1.0);
            instance.setValue(attribute, Double.NaN);
            Attribute attribute2= new Attribute("data_2",1);
            attribute.setWeight(1.0);
            instance.setValue(attribute2, 1.5);
     //       Attribute attribute1 = new Attribute("class",)*//*

            try {
                Instance instance = createInstance();
                int tem = (int) bayesNet.classifyInstance(instance);
                in = (int) bayesNet.classifyInstance(ins);
              //  this.tag_count[in]++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.gc();


        // System.out.println("Features Count:" + feature_count);
        System.out.println("E303:" + tag_count[0]);
        System.out.println("E304:" + tag_count[1]);
        long end = System.currentTimeMillis();
        Log.e("TIME", String.valueOf(end - start));
    }



    private Instance createInstance(){
        Instance instance = new Instance(65);
        for(int i = 0; i < 65; i++) {
            Attribute attribute = new Attribute("data_"+(i+1), i);
            attribute.setWeight(1.0);
            instance.setValue(attribute, Math.random());
        }
        Attribute attribute = new Attribute("class", 64);
        instance.setValue(attribute, Double.NaN);
        return instance;
        }




    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        int total = 100;
        synchronized (lockGui) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
        }
        System.gc();
        int largest = 0;
        for (int i = 0; i < tag_count.length; i++) {
            if (tag_count[i] > largest) {
                largest = i;
            }
        }
        int should_be_atleast = (int) Math.ceil(total / tags.length);
        System.out.println("Should be atleast: " + should_be_atleast);
      //  if (tag_count[largest] >= should_be_atleast) canvas.drawText(tags[largest], 100, 100, paint);
        canvas.drawText(tags[largest], 100, 100, paint);
        System.gc();
    }

    private void describeImage(FastQueue<Desc> listDesc, FastQueue<Point2D_F64> listLoc) {
        listDesc.reset();
        listLoc.reset();
        int N = detDesc.getNumberOfFeatures();
        //     feature_count = N;
        for (int i = 0; i < N; i++) {
            listLoc.grow().set(detDesc.getLocation(i));
            listDesc.grow().setTo(detDesc.getDescription(i));
        }
    }
*/

}