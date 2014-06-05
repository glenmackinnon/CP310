package com.heymonk.hw314weather.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Class that reads an JSON feed and stores it - based heavily on HW312 XML reader
 * 
 * @author Glen
 */

public class AsyncJSONReader extends AsyncTask<URL, Integer, Long > {

    private static final String TAG = "Homework314-AsyncJSONreader";
    private JSONObject mJSON;
    private OnJSONCompleteListener mCallback;

    static InputStream is = null;
    static JSONObject jObj = null;
    static String strJSONLoaded = "";       // holds string of JSON we read

    public interface OnJSONCompleteListener {
        void onJSONComplete(JSONObject result);
    }

    public  AsyncJSONReader(  OnJSONCompleteListener li ) {
        mCallback= li;
    }

    /**
     *  Read and parse JSON from given URL
     *
     * @return JSONobject with data
     */
    private JSONObject parseJSON( URL url )
    {
        // Sample stream:  http://api.wunderground.com/api/9a8a3a60760a7d22/conditions/forecast10day/q/98040.json

        Log.v(TAG,"JSONReader: start read");
        // Make HTTP request
        try {
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url.toURI());

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            strJSONLoaded = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }

        // Try parse the string to a JSON object
        try {
            jObj = new JSONObject(strJSONLoaded);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        Log.v(TAG,"JSONReader: finish read/parse");

        // return JSON String
        return jObj;

    }


    /**
     * Load our JSON url(s) in the background, calling parseJSON for each
     */
    @Override protected Long doInBackground(URL... urls ) {
        int count = urls.length;
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            mJSON = parseJSON( urls[i] );
            publishProgress((int) ((i / (float) count) * 100));
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return totalSize;
    }

    @Override protected void onPostExecute(Long result) {
        Log.v(TAG,"Done parsing!");
        if ( mCallback != null ) {
            mCallback.onJSONComplete( mJSON );      // call the callback to say we're done
        }
        super.onPostExecute(result);
    }
}