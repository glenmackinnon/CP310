
package com.heymonk.homework312;

import com.heymonk.homework312.contentprovider.RssFeed;
import com.heymonk.homework312.util.ImageCacheHelper;
import com.heymonk.homework312.util.ProgressDialogHelper;
import com.heymonk.homework312.util.ShakeDetector;
import com.heymonk.homework312.util.ShakeDetector.OnShakeListener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * Notes:
 * 
 * -- Shake functionality unchanged from HW311 (incl menu item for emulator)
 * -- ContentProvider enhanced to store icon, link, etc;  "datetime" changed to string /w millis (for sortability)
 * -- added CBC RSS feed for more content and a third set of test content - found issues
 * -- XML parsing isn't terribly robust - each item must have all 4 entries
 * -- Title bar in detail view is clickable and opens browser with full article/link
 * -- classes moved into folders by area of focus :-)  ... utils will end up in some lib, eventually
 * -- Images are cached (by URL) via ImageCacheHelper class (based on lruCache) owned by MainActivity - check
 *    out that file for a few known issues including bitmap 'flashing' and unnecessary loads (cache doesn't check if
 *    bitmap is currently being loaded)
 *
 * -- glen
 */

public class MainActivity extends ActionBarActivity   {

    // statics for logging
    static final String TAG = "Homework311";

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    // Fragments & UI flags
    private static MainListFragment mListFrag=null;
    private static RssDetailFragment mDetailFrag=null;
    private ActionBar mActionBar=null;
    private ProgressDialogHelper mProgressDialog = new ProgressDialogHelper();

    private static boolean mDetailsShowing = false;
    private boolean mRefreshing = false;
    private static boolean mFirstRun = true;

    // app global image cache - get via getCache() call - note that it's static
    private static ImageCacheHelper mImgCache = null;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled( mDetailsShowing );   // set up affordance

        // create list view fragment
        if (savedInstanceState == null ) {
            if ( mListFrag == null ) {
                mListFrag = new MainListFragment();
                //Log.v(TAG,"New Main List Fragment");
            }
            getSupportFragmentManager().beginTransaction().add(R.id.container, mListFrag ).commit();
            //Log.v(TAG,"add List Fragment into container");
        }

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override public void onShake(int count) {
                handleShake();   // we don't use count for anything ;single shake suffices
            }
        });

        // Load RSS feed each time we run (using static var)
        if ( mFirstRun ) {
            loadXMLs();
            mFirstRun = false;
        }
    }

    /**
     * User shook device or chose "Shake" from menu - refresh content
     */
    private void handleShake() {

        if ( !mRefreshing ) {    // don't allow shake if we're already processing a shake :-|
            mRefreshing = true;
            if ( mDetailsShowing ) {
                // go back to main list if details up and pushed
                FragmentManager fm= getSupportFragmentManager();
                if(fm.getBackStackEntryCount()>0){
                    fm.popBackStack();  // pop fragment off (phone view)
                } else {
                    // remove fragment (tablet view)
                    getSupportFragmentManager().beginTransaction().remove(mDetailFrag).commit();
                    Log.v(TAG,"Shake - remove details frag - tablet");
                }
            }
            mDetailsShowing = false;
            mDetailFrag = null;
            mActionBar.setDisplayHomeAsUpEnabled(false);

            // Reload the RSS - first show shake dialog w spinner
            mProgressDialog.show( this, true );
            // then fire up async task to load XMLs into ContentProvider
            loadXMLs();
        }
    }

    /**
     * Loads XML streams into the ContentProvider.
     *
     * Note: This clears the CP first
     */
    private void loadXMLs() {

        // Start by deleting data in table - eg. all rows with _ID >= 0

        String mSelectionClause = RssFeed.RssEntry.rowID + " >= ?";
        String[] mSelectionArgs = {"0"};
        int mRowsDeleted = getContentResolver().delete(RssFeed.CONTENT_URI, mSelectionClause, mSelectionArgs );
        Log.v(TAG,"loadXMLs started by deleting " + mRowsDeleted +" Rows");

        // now build source URLs - added CBC just because

        URL google=null;
        URL yahoo=null;
        URL cbc =null;
        //URL geekwire =null;       //ATOM 2.0 feed - images don't work

        try {
            google = new URL( "https://news.google.com/news/section?topic=w&output=rss" );
            yahoo = new URL( "http://news.yahoo.com/rss/world/" );
            cbc = new URL( "http://www.cbc.ca/cmlink/rss-topstories" );
            //geekwire = new URL( "http://feeds.geekwire.com/geekwire?format=xml" );
        } catch (MalformedURLException e) {
            e.printStackTrace();            // shouldn't happen unless we bork the URLs above
        }

        // load in background using our reader - the callback to XMLLoaded() will tell us we're done
        new AsyncXMLReader( this ).execute(  google, yahoo, cbc );
    }

    /**
     * Callback from AsyncXMLReader when finished.
     * Turn off progress dialog if it's up.  Thats about it
     * 
     */
    public void XMLLoaded() {
        if ( mRefreshing  ) {
            mProgressDialog.show( this, false );
        }
        mRefreshing = false;
    }

    /**
     * Handle action bar item clicks here.
     * Explicitly handle Home/Up button to pop detail view off fragment stack if appropriate
     */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch( id ) {
            case R.id.action_shake:  {  // menu item to test shake action without actually shaking
                handleShake();
                break;
            }
            case android.R.id.home: {
                // Log.v(TAG,"back/home/up affordance button ");
                FragmentManager fm= getSupportFragmentManager();
                if(fm.getBackStackEntryCount()>0){
                    fm.popBackStack();
                    // we only have one level of up/back in this app
                    mActionBar.setDisplayHomeAsUpEnabled(false);
                    mDetailsShowing = false;
                }
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called from listview fragment to tell us to show article with ID #"id"
     * 
     * @param id - id to use to get actual article data from contentProvider
     */
    public void ShowDetails(long id) {

        if ( findViewById( R.id.detailcontainer ) != null ) {
            // tablet mode, list +detail fragments on screen at once: use detailcontainer and skip backstack
            if ( mDetailFrag != null ) {
                // remove existing details fragment
                getSupportFragmentManager().beginTransaction().remove(mDetailFrag).commit();
                // Log.v(TAG,"removed old details fragment in detailcontainer(tablet)");
            }
            mDetailFrag = new RssDetailFragment();
            // Log.v(TAG,"new details fragment in detailcontainer(tablet)");
            getSupportFragmentManager().beginTransaction().add(R.id.detailcontainer, mDetailFrag ).commit();
        } else {
            // phone mode - push detail on top of listview and add to back stack
            mDetailFrag = new RssDetailFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, mDetailFrag ).addToBackStack(null).commit();
            // Log.v(TAG,"new details fragment in container(phone)");
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        // update content pointer/id in the detail fragment, it'll get the data and draw itself
        mDetailFrag.setContent( id );
        mDetailsShowing = true;
    }

    /**
     * Let the list and details fragment get ahold of the shared image cache
     * This function creates it if it hasnt been initialized yet
     * 
     * @return  ImageCacheHelper of current image cache
     */
    public ImageCacheHelper getCache ()
    {
        if ( mImgCache == null ) {
            mImgCache = new ImageCacheHelper();     // need to create it
            Log.v(TAG,"Created image cache");
        }
        return mImgCache;
    }

    /**
     * onResume - remember to register the sensor manager
     */
    @Override public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * onPause - need to unregister the sensor manager
     */
    @Override public void onPause() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    /**
     *  Inflate the menu; this adds items to the action bar if it is present.
     */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}