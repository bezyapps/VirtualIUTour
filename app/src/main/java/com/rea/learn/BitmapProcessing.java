package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import org.ddogleg.struct.FastQueue;

import java.io.File;
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

/**
 * Created by ericbhatti on 11/17/15.
 * <p>
 * <p>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p>
 *         Jira Ticket: NULL
 * @since 17 November, 2015
 */
public class BitmapProcessing extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

        // output image which is displayed by the GUI
        private Bitmap outputGUI;
        // storage used during image convert
        private byte[] storage;
        // output image which is modified by processing thread
        private Bitmap output;
        private int width, height;

        public BitmapProcessing(Context context, int width, int height){
                super(ImageType.ms(3,ImageFloat32.class));
                this.width = width;
                this.height = height;
                output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
                outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
        }


        @Override
        protected void render(Canvas canvas, double imageToOutput) {
                synchronized (new Object()) {canvas.drawBitmap(outputGUI, 0, 0, null); }
        }


        @Override
        protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
                ImageFloat32 gray = new ImageFloat32(imageFloat32MultiSpectral.width,imageFloat32MultiSpectral.height);
                ConvertImage.average(imageFloat32MultiSpectral, gray);
                synchronized (new Object()) {
                        //ConvertBitmap.declareStorage(gray ,storage);
                        ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
                        //  ConvertBitmap.grayToBitmap(gray, output, storage);
                        outputGUI = output;

                     //   File file = new File(E)
                }


                /*
                private String convert(byte[] data) throws Exception
	{
		BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
		ImageFloat32 float32 = new ImageFloat32();
		ConvertBufferedImage.convertFrom(bufferedImage, float32);
		return null;
	}

                 */

        }

}


