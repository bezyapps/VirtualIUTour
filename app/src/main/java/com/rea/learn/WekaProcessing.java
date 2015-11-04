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
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import weka.classifiers.functions.VotedPerceptron;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 * Created by ericbhatti on 11/4/15.
 * <p>
 * <p>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p>
 *         Jira Ticket: NULL
 * @since 04 November, 2015
 */
public class WekaProcessing<Desc extends TupleDesc> extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

    DetectDescribePoint<ImageFloat32, Desc> detDesc;
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
    private VotedPerceptron votedPerceptron;
    Context context;

    Paint paint = new Paint();

    private void buildHeader(){
        headBuilder = new StringBuilder("@relation test_set\n\n");
        for(int i = 1 ; i < 65 ; i++) {
            headBuilder.append("@attribute data_" + i + " numeric\n");
        }
        headBuilder.append("@attribute class {e303,e304}\n\n");
        headBuilder.append("@data\n");
    }

    private String getARFF_Header() {
        return headBuilder.toString();
    }

    private Object getModel(String modelName)
    {
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
        super(ImageType.ms(3, ImageFloat32.class));
        detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageFloat32.class);
        this.width = width;
        this.height = height;
        listDst = UtilFeature.createQueue(detDesc, 10);
        this.context = context;
        buildHeader();
        votedPerceptron = (VotedPerceptron) getModel("voted.model");
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paint.setColor(Color.BLUE);
        paint.setTextSize(20);
    }


    private boolean allzeros(Desc desc)
    {
        boolean allzero = true;
        for(int i  = 0; i <desc.size(); i++)
        {
            if(desc.getDouble(i) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
        ImageFloat32 gray = new ImageFloat32(imageFloat32MultiSpectral.width, imageFloat32MultiSpectral.height);
        ConvertImage.average(imageFloat32MultiSpectral, gray);
        detDesc.detect(gray);
        describeImage(listDst, locationDst);
        StringBuilder dataBuilder = new StringBuilder(getARFF_Header());
        for(Desc element : listDst.data)
        {
            for(int i = 0; i < element.size() ; i++)
            {
                if(allzeros(element)) continue;
                dataBuilder.append(element.getDouble(i) + "," );
                if(i == element.size() - 1) dataBuilder.append("?\n");
            }
        }
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(new ByteArrayInputStream(dataBuilder.toString().getBytes()));
        Instances data = null;
        try {
            data = source.getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.setClassIndex(data.numAttributes() - 1);
        int e303 = 0; int e304 = 0;
        for(int i = 0; i < data.numInstances() ; i++ )
        {
            Instance ins = data.instance(i);
            int in = -1;
            try {
                in = (int)votedPerceptron.classifyInstance(ins);
            } catch (Exception e) {
                e.printStackTrace();
            }
            switch(in)
            {
                case 0:
                {
                    e303++;
                    break;
                }
                case 1:
                {
                    e304++;
                    break;
                }
            }
        }
        System.out.println("E303:" + e303);
        System.out.println("E304:" + e304);
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
            //  ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
    }


    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object()) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
  /*          paint.setColor(Color.BLUE);
            for (int i = 0; i < pointsDst.size(); i++) {
                Point2D_F64 point2D_f64 = pointsDst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())), Float.parseFloat(String.valueOf(point2D_f64.getY())), 2, paint);
            }
            paint.setColor(Color.RED);
            for (int i = 0; i < points303Dst.size(); i++) {
                Point2D_F64 point2D_f64 = points303Dst.get(i);
                canvas.drawCircle(Float.parseFloat(String.valueOf(point2D_f64.getX())), Float.parseFloat(String.valueOf(point2D_f64.getY())), 2, paint);
            }
*/

          /*  if (points303Dst.size() > pointsDst.size()) {
                canvas.drawText("Room 303", 100, 100, paint);
            } else if (points303Dst.size() < pointsDst.size()) {
                canvas.drawText("Room 304", 100, 100, paint);
            }*/


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