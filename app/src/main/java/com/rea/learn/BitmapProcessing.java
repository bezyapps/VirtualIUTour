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
public class BitmapProcessing extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;

    private Bitmap grayBitmap;
    Paint paint = new Paint();
    int frameCount = 0;

    String location = "";

    public BitmapProcessing(Context context, int width, int height) {
        super(ImageType.ms(3, ImageFloat32.class));
        this.width = width;
        this.height = height;
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        paint.setColor(Color.GREEN);
        paint.setTextSize(15);
    }


    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object()) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            canvas.drawText(location,50,50,paint);
        }
    }


    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
        frameCount++;
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
            //  ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        if(frameCount % 7 == 0) {
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
    //    String img = new String(data);
    //    Log.e("STRING_DATA",img);

       /* File file = new File(Environment.getExternalStorageDirectory() + "/learn");
        if (!file.exists()) {
            file.mkdirs();
        }
        File image = new File(file.getAbsolutePath() + "/" + System.currentTimeMillis());

        try {
            PrintWriter printWriter =  new PrintWriter(image);
            printWriter.println(img);
            /*FileOutputStream fileOutputStream = new FileOutputStream(image);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
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


    class GetLocationAsync extends AsyncTask<String,Void, String>
    {

        private static final String BASE_URL = "http://192.168.1.178:8080/";
        //private static final String BASE_URL = "http://192.168.1.104:8080/";
        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The specified result is the value returned by {@link #doInBackground}.</p>
         * <p/>
         * <p>This method won't be invoked if the task was cancelled.</p>
         *
         * @params The result of the operation computed by {@link #doInBackground}.
         * @see #onPreExecute
         * @see #doInBackground
         * @see #onCancelled(Object)
         */

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


        /**
         * Override this method to perform a computation on a background thread. The specified parameters are the parameters passed to {@link #execute} by the caller
         * of this
         * task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String doInBackground(String... params) {

            long start = System.currentTimeMillis();

            URL url;
            String response = "";
            try {
                url = new URL(BASE_URL + "image.json");

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
            Log.e("SERVER_REST", String.valueOf(diff) + " " + response);
            if(!response.equals(""))
            {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    location = jsonObject.getString("location");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return response;

        }


    }






}


