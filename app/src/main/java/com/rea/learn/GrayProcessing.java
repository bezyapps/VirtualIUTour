package com.rea.learn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * Created by Eric Bhatti on 8/19/2015.
 */
public class GrayProcessing extends VideoImageProcessing<ImageUInt8> {

    protected GrayProcessing() {
        super(ImageType.single(ImageUInt8.class));
    }

    @Override
    protected void process(ImageUInt8 image, Bitmap output, byte[] storage) {
        ConvertBitmap.grayToBitmap(image,output,storage);

    }
}
