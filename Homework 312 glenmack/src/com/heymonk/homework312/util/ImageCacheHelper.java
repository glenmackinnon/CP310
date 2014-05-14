package com.heymonk.homework312.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Code based on lruCache example from http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
 * 
 * A couple of known issues:
 *  - cache does not coalesce calls for a particular image into a single load; the ListView appears to load 3x
 *    (measuring passes?) in some cases so occasionally we do three loads of the bitmap  - could fix this by
 *    tracking 'pending' bitmap URLs on the inbound request and rejecting dupes
 * 
 *  - The cache naively writes the bitmap to the imageView when it's finishing loaded - great in most cases but
 *    not optimal in the case where the imageview (+listview) has already been recycled for another item...
 *    not 100% sure the best way to prevent;
 *    -  could track imageviews passed in and only draw bitmap when it was the last in (chronologically) - but
 *       there are still cases the wrong image ends in place (cache hits, for example)
 *    - could re-architect to write bitmaps into the CP instead and trigger a content change
 *    - could subclass the listview to get more control over the actual drawing
 *    - or ???
 * 
 * 
 *  In either case, I figured that caching was not key to this exercise* so I left it for future work.
 *  (* had just implemented an hour earlier when prof said "no need to do caching or anything" in class :-) )
 * 
 * @author Glen
 */
public class ImageCacheHelper {

    private static final String TAG = "Homework312:ImageCacheHelper";
    private LruCache<String, Bitmap> mMemoryCache;

    /**
     * Stock code from android example, HandleImageId + DownloadImageTask is custom
     * 
     */
    public ImageCacheHelper( ) {

        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        // Stored in kilobytes as LruCache takes an int in its constructor.

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        Log.v(TAG,"mem cache of "+cacheSize + "KB");

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override  protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than number of items.
                return BitmapSize( bitmap )/ 1024;
            }
        };
    }

    /**
     * Code to calc bitmap size, use HONEYCOMB_MR1 call for getByteCount() if available
     * 
     * @param bitmap
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected int BitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getRowBytes() * bitmap.getHeight();
        } else {
            return bitmap.getByteCount();
        }
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        Bitmap ret= mMemoryCache.get(key);
        return ret;
    }

    /**
     * Resolve image string and put bitmap in view
     * if image = integer, assume it's a resourceID and do that
     * Otherwise, try to find URL value in bitmap cache.  If a cache miss, load and add to cache. Easy!
     * 
     * @param c - Context
     * @param str - string value of image - URI or Integer resource ID
     * @param v - ImageView to populate with bitmap
     */
    public void HandleImageId( Context c, String str, ImageView v ) {
        int icon;

        //Log.v(TAG,"Handle image (View/Str) " + v + ", ["+str+"]");
        try {
            icon = Integer.parseInt( str );
            // if no exception, set it and return
            v.setImageDrawable(c.getResources().getDrawable( icon  ));

        } catch(NumberFormatException nfe) {
            // Not a number - try to load bitmap.  First, try to get it from cache - Note that we ignore
            // the possibility of the image being updated on the web after we have downloaded it.
            // if the URL matches and we've downloaded it this session, it's good enough!

            Bitmap b = getBitmapFromMemCache( str );

            if ( b!= null  ) {
                v.setImageBitmap(b );
                //Log.v(TAG, "Cache hit!");
            } else {
                // download and update the view later .. this adds img to cache and updates the view
                new DownloadImageTask(v).execute( str  );
                //Log.v(TAG, "Cache miss! " + str );
            }
        }
    }

    /**
     * DownloadImageTask - get bitmap, when done, write to ImageView and add to cache
     */
    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmView;
        String mUrl;


        public DownloadImageTask(ImageView bmImage) {
            this.bmView = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            mUrl = urls[0];
            //Log.v(TAG,"doinBackground: "+ urls[0].toString() );
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }


        /**
         *      Done. Write bitmap to cache and to the view indicated
         */
        @Override protected void onPostExecute(Bitmap result) {
            addBitmapToMemoryCache( mUrl , result );
            bmView.setImageBitmap(result);
        }
    }
}