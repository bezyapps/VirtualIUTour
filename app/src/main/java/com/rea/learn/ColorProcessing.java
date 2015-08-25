package com.rea.learn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * Created by Eric Bhatti on 8/19/2015.
 */
public class ColorProcessing extends VideoImageProcessing<MultiSpectral<ImageUInt8>> {
    protected ColorProcessing() {
        super(ImageType.ms(3, ImageUInt8.class));
    }



    @Override
    protected void process(MultiSpectral<ImageUInt8> image, Bitmap output, byte[] storage) {
        ConvertBitmap.multiToBitmap(image, output, storage);

    }

    Paint paint;
    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        super.render(canvas,imageToOutput);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(20);
        canvas.drawText("Test Text",30,60,paint);
    }
}