package com.rea.learn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * Created by ericbhatti on 11/17/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 17 November, 2015
 */
public class BitmapVariableProcessing extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;

    private Bitmap grayBitmap;

    int frameCount = 0;

    int skipRate;

    String ipAddress;

    String location = "";

    Paint paint;

    public BitmapVariableProcessing(Context context, int width, int height) {
        super(ImageType.ms(3, ImageFloat32.class));
        this.width = width;
        this.height = height;
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.skipRate = 7;
        this.ipAddress = "http://192.168.11.252:8080/StrutsMavenProject/image.json";
        initPaint(Color.RED,17);
    }
    public BitmapVariableProcessing(Context context, int width, int height,String ipAddress, int skipRate) {
        super(ImageType.ms(3, ImageFloat32.class));
        this.width = width;
        this.height = height;
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.ipAddress = ipAddress;
        this.skipRate = skipRate;
        initPaint(Color.RED,17);
    }

    private void initPaint(int color, float textSize)
    {
        paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(textSize);
    }


    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object()) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            canvas.drawText(location,75,75,paint);
        }
    }


    int count = 0;
    long time = 0;


    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
        frameCount++;
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
            //  ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        if(frameCount % skipRate == 0) {
            ImageFloat32 gray = new ImageFloat32(imageFloat32MultiSpectral.width, imageFloat32MultiSpectral.height);
            ConvertImage.average(imageFloat32MultiSpectral, gray);
            ConvertBitmap.grayToBitmap(gray, grayBitmap, storage);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            grayBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();
            String img = Base64.encodeToString(data, Base64.DEFAULT);
            GetLocationAsync getLocationAsync = new GetLocationAsync();
            getLocationAsync.execute(img);
        }
    }


    class GetLocationAsync extends AsyncTask<String,Void, String>
    {
        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for(Map.Entry<String, String> entry : params.entrySet()){
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            return result.toString();
        }


        @Override
        protected String doInBackground(String... params) {

            count++;
            long start = System.currentTimeMillis();

            URL url;
            String response = "";
            try {
                url = new URL(ipAddress);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                HashMap<String ,String> postDataParams = new HashMap<>();
                postDataParams.put("data",params[0]);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();
                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line=br.readLine()) != null) {
                        response+=line;
                    }
                }
                else {
                    response="";

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            long stop = System.currentTimeMillis();
            long diff = stop - start;
            time = time + diff;
            Log.e("SERVER_REST", String.valueOf(diff));
            Log.e("SERVER_AVG", String.valueOf(time / count));
            try {
                JSONObject jsonObject = new JSONObject(response);
                location = jsonObject.getString("location");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }
    }
}


