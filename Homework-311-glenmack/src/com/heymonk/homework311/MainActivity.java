
package com.heymonk.homework311;

import com.heymonk.homework311.ShakeDetector.OnShakeListener;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.xpath.XPath ;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


/**
 * Notes
 * 
 * -- implemented both phone view and tablet (two column, list+details) using fragments
 * -- implemented a cursorLoader for the listview (in MainListFragment) in case there are a lot of entries,
 *      but just used the content provider (in UI thread) to get detail view/content *and* for loading the XML
 *      file from resource.   Will definitely rewrite that to load content asynchronously for HW312, figured I
 *      should leave something to do in that exercise :-)
 * 
 * -- used sensor shake code provided by example mentioned in class (http://www.survivingwithandroid.com/2014/04/android-shake-to-refresh-tutorial.html)
 * -- add, display and store date field that's mostly unused for now (HW312 sorts on date)
 * -- XML parsing code (using XPath) assumes well-formed, item/content pairs for this exercise
 * -- "visualization" of refresh is a progress dialog - minimally useful given how fast loading 5 xml entries is
 *    (had to add delay logic to show it for at least 2.5s - again, expect it'll be more flushed out in HW312)
 * -- I added a menu item to "Shake" for easier testing in emulator
 * -- use pref key to track whether or not we've loaded the data in the DB at all (do it automatically when first run)
 */

public class MainActivity extends ActionBarActivity   {

    // statics for logging and a pref key
    static final String TAG = "Homework311";
    private static final String HW311_PREFS = "com.heymonk.homework311";
    private static final String HW311_DBINIT = "com.heymonk.homework311.wrotedb";

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    // Fragments & UI flags
    private static MainListFragment mListFrag=null;
    private static RssDetailFragment mDetailFrag=null;
    private ActionBar mActionBar=null;
    private static boolean mDetailsShowing = false;

    // progress dialog
    private long mSpinStart;
    private ProgressDialog mRefreshDialog;
    private static long PROGRESS_DIALOG_MINTIME=2500;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // enable up affordance
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled( mDetailsShowing );

        // create list view fragment
        if (savedInstanceState == null ) {
            if ( mListFrag == null ) {
                mListFrag = new MainListFragment();
                Log.v(TAG,"New Main List Fragment");
            }
            getSupportFragmentManager().beginTransaction().add(R.id.container, mListFrag ).commit();
            Log.v(TAG,"add List Fragment into container");
        }

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override public void onShake(int count) {
                handleShake();   // we don't use count for anything interesting, a single shake suffices
            }
        });

        // Check shared pref to see if we need to load XML into the DB (via the content provider) or not
        SharedPreferences prefs = this.getSharedPreferences(HW311_PREFS, Context.MODE_PRIVATE);
        boolean wrotedb = prefs.getBoolean(HW311_DBINIT, false );
        if ( wrotedb == false ) {
            loadStockXML(); // load XML into DB
            prefs.edit().putBoolean(HW311_DBINIT, true ).commit();  // write that we've done it
        }
    }

    @Override public void onResume() {
        super.onResume();
        // Register the Sensor Manager
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override public void onPause() {
        // unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    /**
     * Load content into the contentProvider.
     * 
     * Parse input XML file (raw/data.xml) and load values into DB via contentProvider interface
     * 
     * @throws XPathExpressionException
     */

    private void loadStockXML( )
    {
        // OK, show shake dialog w spinner
        ShowProgressDialog( true );

        // Start by deleting existing data (wheee!)
        // Defines selection criteria for the rows you want to delete - rowID >=0 for us
        // TODO: figure out communication to bulk delete and then refetch for HW312, and make async :-)
        String mSelectionClause = RssFeed.RssEntry.rowID + " >= ?";
        String[] mSelectionArgs = {"0"};

        // Defines a variable to contain the number of rows deleted
        int mRowsDeleted = getContentResolver().delete(RssFeed.CONTENT_URI, mSelectionClause, mSelectionArgs );
        Log.v(TAG,"loadStockXML started by deleting " + mRowsDeleted +" Rows");

        // create an InputSource object from /res/raw/data.xml
        InputSource inputSrc = new InputSource(getResources().openRawResource(R.raw.data));
        XPath xpath = XPathFactory.newInstance().newXPath();

        // specify the xpath expression to get title and content nodes under item
        String expression = "//item/title|//item/content";

        // Query: get list of nodes matching the above
        NodeList nodes = null;
        try {
            nodes = (NodeList)xpath.evaluate(expression, inputSrc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        if ( nodes != null ) {
            Log.v( TAG, "LoadStockXML: Returned node count: " + String.valueOf(nodes.getLength()));
            // If node(s) found, go through them two by two and store via contentProvider
            // this will be quick, but in a real app with heavier data look at doing this with
            // an async cursorLoader so it's off the UI thread

            int len = nodes.getLength();
            if( len > 0) {
                ContentResolver cr = this.getContentResolver();
                if ( cr == null ) {
                    Log.e(TAG,"Error getting content resolver");
                    return;
                }
                for(int i = 0; i < len;  ) {
                    String title="";
                    String content="";

                    // Code below assumes XML is well formed (both title, content, either order)
                    // rewrite for HW311 when XML could be less well formed

                    for ( int j=0; j<2; j++ ) {
                        Node node = nodes.item(i);
                        //Log.v(TAG,"node ("+i+") "+node.getNodeName()+" value "+ ( node.getTextContent()) );
                        if ( node.getNodeName().contentEquals("title")) {
                            title = node.getTextContent();
                        }
                        if (  node.getNodeName().contentEquals("content")) {
                            content = node.getTextContent();
                        }
                        ++i;
                    }
                    // set up our values and write to the content provider
                    // use current time for Date, then stock values for icon, provider

                    ContentValues cv = new ContentValues();
                    SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.std_date_format), Locale.US );
                    String datenow = dateFormat.format(new Date(System.currentTimeMillis()));

                    cv.put(RssFeed.RssEntry.DATE, datenow  );
                    cv.put(RssFeed.RssEntry.ICON, Integer.toString( R.drawable.ic_rss1 ));
                    cv.put(RssFeed.RssEntry.PROVIDER, getString(R.string.raw_xml_provider) );
                    cv.put( RssFeed.RssEntry.TITLE, title );
                    cv.put( RssFeed.RssEntry.CONTENT, content );

                    // insert item via content provider URI
                    cr.insert(RssFeed.CONTENT_URI, cv);
                }
            }
        }

        if ( Refreshing() ) {
            ShowProgressDialog( false );   // done, shut down progress dialog
        }
    }

    void ShowProgressDialog( boolean state )
    {
        if ( state == true ) {
            if ( mRefreshDialog == null ) {
                // turn on progress dialog and keep track of when we turned it on
                // mRefreshDialog being non null is an indicator we have the dialog up
                Log.v(TAG, "progress dialog on!");
                mSpinStart = System.currentTimeMillis();
                mRefreshDialog = new ProgressDialog(this);
                mRefreshDialog.setMessage(getString(R.string.refreshing_content));
                mRefreshDialog.setIndeterminate(true);
                mRefreshDialog.setCancelable(true);  // leave true for debugging
                mRefreshDialog.show();
            }
        } else {
            // turn off progress dialog - make sure it's been up at least 2.5s total though

            if ( mRefreshDialog != null ) {
                long elapsed = System.currentTimeMillis() - mSpinStart;
                long delay = ( elapsed < PROGRESS_DIALOG_MINTIME ) ? (PROGRESS_DIALOG_MINTIME -elapsed) : 200;     //
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        mRefreshDialog.hide();
                        mRefreshDialog =null;
                    }}, delay);
            }
        }
    }

    boolean Refreshing()
    {
        return (mRefreshDialog != null );
    }

    /**
     *  Inflate the menu; this adds items to the action bar if it is present.
     */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handle action bar item clicks here.
     * Explicitly handle Home/Up button to pop detail view off fragment stack if appropriate
     */
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch( id ) {
            case R.id.action_shake:  {  // util menu item to test shake action without actually shaking
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
     * User shook device or chose "Shake" from menu - refresh content
     */
    private void handleShake() {

        // don't allow shake if we're already processing a shake :-|
        if ( !Refreshing()) {
            // First, let's go back to main list if details is up and we've got a detail view on top (pushed)
            if ( mDetailsShowing ) {
                FragmentManager fm= getSupportFragmentManager();
                if(fm.getBackStackEntryCount()>0){
                    fm.popBackStack();  // pop fragment off (phone view)
                } else {
                    // remove frag (tablet view)
                    getSupportFragmentManager().beginTransaction().remove(mDetailFrag).commit();
                    Log.v(TAG,"Shake - remove details frag - tablet");
                }
            }
            mDetailsShowing = false;
            mDetailFrag = null;
            mActionBar.setDisplayHomeAsUpEnabled(false);

            // Now let's reload the XML.  It handles showing/closing the progress dialog
            loadStockXML();
        }
    }

    /**
     * Called from listview fragment to tell us to show article with ID #"id"
     * 
     * @param id - id to use to get actual article data from contentProvider
     */
    public void ShowDetails(long id) {

        if ( findViewById( R.id.detailcontainer ) != null ) {
            // tablet mode, two fragments onscreen at once:  use detailcontainer and skip backstack
            if ( mDetailFrag != null ) {
                // remove existing details fragment
                getSupportFragmentManager().beginTransaction().remove(mDetailFrag).commit();
                Log.v(TAG,"removed old details fragment in detailcontainer(tablet)");
            }
            mDetailFrag = new RssDetailFragment();
            Log.v(TAG,"new details fragment in detailcontainer(tablet)");
            getSupportFragmentManager().beginTransaction().add(R.id.detailcontainer, mDetailFrag ).commit();
        } else {
            // phone mode - push detail on top of listview and add to back stack
            mDetailFrag = new RssDetailFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, mDetailFrag ).addToBackStack(null).commit();
            Log.v(TAG,"new details fragment in container(phone)");
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        // update content pointer/id in the detail fragment, it'll get the data and draw itself
        mDetailFrag.setContent( id );
        mDetailsShowing = true;
    }
}