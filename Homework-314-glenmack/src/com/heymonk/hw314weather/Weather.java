package com.heymonk.hw314weather;

import com.heymonk.hw314weather.util.AsyncJSONReader;
import com.heymonk.hw314weather.util.AsyncJSONReader.OnJSONCompleteListener;
import com.heymonk.hw314weather.util.ProgressDialogHelper;
import com.heymonk.hw314weather.util.Throttle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;

public class Weather {
    private static final String TAG = "HW314-Weather";

    // API call throttler, allow 9 calls in 60s  (WU allows 10/min)
    private static final int  MAX_THROTTLE_TIME = 60000;
    private static final int  MAX_THROTTLE_DURATION = 9;
    static Throttle mAPIThrottle = new Throttle( MAX_THROTTLE_TIME, MAX_THROTTLE_DURATION );

    // pref keys to store weather settings
    private static final String PREF_LOCATION = "location";
    private static final String PREF_URL = "url";
    private static final String PREF_ZIP= "zip";
    private static final String PREF_SHOWF= "showF";

    // Days of forecasts to fetch, and # of text half day forecasts to use
    private static final int MAX_DAYS = 10;
    private static final int NUM_TEXT_FORECAST = 10;

    // Data fields
    String mZip;                // User specified ZIP code
    String mURL;                // URL of WU data fetch call (generated from user zip+ url )
    String mSrcLocation;        // user-entered friendly string location

    // State fields
    boolean mHaveData=false;    // do we have valid (fetched) data
    boolean mShowF = true;      // true = show temp in F, otherwise C

    // Fetched weather and forecast fields
    private class DataFetched {
        String CurrentTime;
        String CurrentC;
        String CurrentF;
        String CurrentCondition;
        String CurrentIcon;

        private String Wind;
        private String WindMph;
        private String WindKph;
        private String PrecipIn;
        private String PrecipMetric;

        String ForecastDay[] = new String[MAX_DAYS];
        String ForecastIcon[] = new String[MAX_DAYS];
        String ForecastLoF[] = new String[MAX_DAYS];
        String ForecastLoC[] = new String[MAX_DAYS];
        String ForecastHiF[] = new String[MAX_DAYS];
        String ForecastHiC[] = new String[MAX_DAYS];
        String ForecastPOP[] = new String[MAX_DAYS];

        String FiveDayForecastTextF="";
        String FiveDayForecastTextC="";
    };
    DataFetched mData = new DataFetched();

    // Util / instances
    private View mView;         // view associated with this weather object
    private Context mContext;   // context from above view, mostly for lookup up resources
    ProgressDialogHelper   mProgressDialog = null;

    /**
     * Constructor - build given friendly name, zip/postal code, and URL for datafetch
     * 
     * @param location
     * @param zip
     * @param url
     */
    public  Weather( String location, String zip, String url ) {
        mSrcLocation = location;
        mURL = url;
        mZip = zip;
    }

    /**
     * Switch display preference between F & C
     */
    public void toggleUnits() {
        mShowF = !mShowF;
    }

    /**
     * Return friendly name of location (e.g. "Mom's house")
     * @return
     */
    public String getLocation() {
        return mSrcLocation;
    }

    /**
     * Return ZIP or postal code for this weather item
     * 
     * @return
     */
    public String getZip() {
        return mZip;
    }

    /**
     * Parse passed JSON response and fill out our "DataFetched" structure.
     * 
     * See attached JsonResponse.txt in project files for sample JSON file  and
     * view @ http://jsonviewer.stack.hu/ to see the data received
     *
     * Ideally refactor into GSON or something more template based/less hardcoded, but this works
     * 
     * @param src
     */
    public void parseJSON( JSONObject src )
    {
        boolean baddata=false;      // set if we catch any errors reading


        clearmdata();   // clear existing data, at least make it printable to prevent null strings


        if ( src != null) {
            // Approx JSON size ~= 16KB
            // Log.v(TAG,"jsonRes = " + jsonRes.toString().length() + " characters");

            try {

                // TODO : Check Response to make sure we have good data, maybe show error (network down, API key dead?)

                // Assume good JSON response... pull out current settings & icons
                // ---------------------------------------------------------------

                JSONObject rootObj = src.getJSONObject("current_observation" );
                mData.CurrentTime = rootObj.getString("observation_time");      // "last updated...
                mData.CurrentCondition = rootObj.getString( "weather" );        // "Mostly Cloudy"
                mData.CurrentIcon = rootObj.getString("icon");                  // "flurries"

                // get temps as INTs to truncate reading - eg. 66.7F to plain old 66F
                mData.CurrentF = Integer.toString(rootObj.getInt("temp_f"));
                mData.CurrentC = Integer.toString(rootObj.getInt("temp_c"));

                // for indicators let's get wind and precip
                mData.Wind = rootObj.getString("wind_dir" );        // "NW"
                mData.WindMph = rootObj.getString("wind_mph" );     // "0.7"
                mData.WindKph = rootObj.getString("wind_kph" );     // "3.1"
                mData.PrecipIn = rootObj.getString("precip_today_in" );         // "0:00"
                mData.PrecipMetric = rootObj.getString("precip_today_metric" ); // "0"

            } catch (JSONException e) {
                //e.printStackTrace();
                baddata = true;
            }

            // Then forecast data for the next MAXDAYs
            // ---------------------------------------
            JSONObject forecast;

            try {
                forecast = src.getJSONObject("forecast" );
                JSONObject simp = forecast.getJSONObject("simpleforecast");
                JSONArray  forecastday = simp.getJSONArray("forecastday" );

                JSONObject jo;
                for ( int i=0; i< MAX_DAYS; i++ ) {
                    jo = forecastday.getJSONObject(i);

                    JSONObject jx= jo.getJSONObject("date");                // get date string first
                    mData.ForecastDay[i] = jx.getString("weekday_short");   // "THU"

                    JSONObject jh = jo.getJSONObject("high");
                    mData.ForecastHiF[i] = jh.getString("fahrenheit");  // hi, F
                    mData.ForecastHiC[i] = jh.getString("celsius" );    // hi, C

                    JSONObject jl = jo.getJSONObject("low");
                    mData.ForecastLoF[i] = jl.getString("fahrenheit");  // lo, F
                    mData.ForecastLoC[i] = jl.getString("celsius" );    // lo, C

                    mData.ForecastIcon[i] = jo.getString("icon");   // forecast icon string
                    mData.ForecastPOP[i] = jo.getString("pop");     // % of precipitation

                }
            } catch (JSONException e) {
                //e.printStackTrace();
                baddata=true;
            }

            // build up 5d (day+night) "text" forecast too- in HTML, with Bolding and HR breaks
            // ----------------------------------------------------------------------------------

            try {
                forecast = src.getJSONObject("forecast" );

                JSONObject simp = forecast.getJSONObject("txt_forecast");
                JSONArray  fday = simp.getJSONArray("forecastday" );

                StringBuilder sbF = new StringBuilder();    // Fahrenheit
                StringBuilder sbC = new StringBuilder();    // metric

                sbF.append( "<html>" );
                sbC.append( "<html>" );

                for ( int i=0; i<= NUM_TEXT_FORECAST ; i++ ) {

                    JSONObject jo = fday.getJSONObject(i);
                    String label="<b>" + jo.getString( "title") + "</b> &nbsp";
                    sbF.append( label );
                    sbC.append( label );

                    sbF.append( jo.getString( "fcttext" ));         // "A mostly clear sky. Low 52F. Winds NNE at 5 to 10 mph."
                    sbC.append( jo.getString( "fcttext_metric" ));  //  "Clear skies. Low 11C. Winds NNE at 10 to 15 kph."

                    String sep= ((i%2)==1) ? "<hr>" : "<br>";
                    sbF.append(sep);
                    sbC.append(sep);
                }
                String fin="<p><p><p></html>";
                sbF.append( fin );
                sbC.append( fin );

                mData.FiveDayForecastTextF = sbF.toString();
                mData.FiveDayForecastTextC = sbC.toString();

            } catch (JSONException e) {
                //e.printStackTrace();
                baddata=true;
            }
        } else {
            baddata = true; // no src JSON readable
        }
        // TODO: Do something (user message?) if we have "baddata"?  For now, just set flag that means
        // we will refetch next time we refresh
        mHaveData = !baddata;
        if ( baddata ) {
            Log.v(TAG,"error reading some portion of the JSON from WeatherUnderground - ignored");
        }
    }

    private void clearmdata() {

        mData.CurrentTime = "";
        mData.CurrentC = "";
        mData.CurrentF = "";
        mData.CurrentCondition = "unknown";
        mData.CurrentIcon = "unknown";

        mData.Wind ="";
        mData.WindMph ="";
        mData.WindKph ="";
        mData.PrecipIn ="";
        mData.PrecipMetric ="";

        for ( int i=0; i< MAX_DAYS; i++ ) {
            mData.ForecastDay[i] = "";
            mData.ForecastIcon[i] = "";
            mData.ForecastLoF[i] = "";
            mData.ForecastLoC[i] = "";
            mData.ForecastHiF[i] = "";
            mData.ForecastHiC[i] = "";
            mData.ForecastPOP[i] = "";
        }

        mData.FiveDayForecastTextF="";
        mData.FiveDayForecastTextC="";
    }

    /** Do data fetch (if necessary, or forced) then call updateUIFields
     * 
     * @param root
     * @param force
     */

    public void fetchData( View root, boolean force ) {
        URL weatherUrl=null;

        if ( root == null ) return;
        mView = root;
        mContext = mView.getContext();

        if ( ((mHaveData && !force))) {
            // if we have data and not forced, be done
            updateUIFields(); // redraw for kicks with current values
            return;
        }

        // Make sure we're OK timewise for API throttle (max 10 calls per min)
        boolean bOK = mAPIThrottle.check();
        if ( !bOK ) {
            // too soon since last fetch, do nothing
            updateUIFields(); // redraw
            return;
        }

        // Let's do a data fetch!
        try {
            weatherUrl = new URL( mURL  );
        } catch (MalformedURLException e) {
            e.printStackTrace();            // shouldn't happen unless we bork the URLs
            return;
        }

        // Show progress dialog if we're forcing a refresh (e.g. on current-focused UI)
        if ( mView != null && force ) {
            mProgressDialog = new ProgressDialogHelper();
            mProgressDialog.show( mContext, mContext.getString( R.string.refreshing_forecast )  );
        } else {
            Log.v(TAG,"null view or mProg dialog : view= "+mView + "prog=" +mProgressDialog );
        }

        // Load in background using our reader ... it calls back us when done

        // Log.v(TAG,"JSON loading " + weatherUrl );
        AsyncJSONReader reader = new AsyncJSONReader( new OnJSONCompleteListener() {

            @Override public void onJSONComplete(JSONObject result) {
                parseJSON( result );        // parse the result
                if ( mProgressDialog != null ) {
                    mProgressDialog.hide(); // turn off the progress dialog if we had one
                }
                mProgressDialog = null;
                updateUIFields();           // draw the new data
            }
        } );
        reader.execute( weatherUrl );  // load URL up
    }

    /**
     * Save Weather settings (e.g. location, URL and ZIP) to SharedPreferences
     * Prefix+id is used to differentiate different weather items, e.g. KEY1Location KEY1URL, etc
     * 
     * @param string
     */
    void saveSettings( Context c, String key, String prefix )
    {
        SharedPreferences prefs = c.getSharedPreferences(key, Context.MODE_PRIVATE);
        if ( prefs != null ) {
            prefs.edit().putString( prefix+PREF_LOCATION, mSrcLocation ).commit();
            prefs.edit().putString( prefix+PREF_URL, mURL ).commit();
            prefs.edit().putString( prefix+PREF_ZIP, mZip ).commit();
            prefs.edit().putBoolean( prefix+PREF_SHOWF, mShowF ).commit();
        }
    }

    /**
     * Read positions from SharedPreferences
     */
    void restoreSettings( Context c, String key, String prefix )
    {
        // Restore state from shared pref; keep existing values if no value saved
        SharedPreferences prefs = c.getSharedPreferences( key, Context.MODE_PRIVATE);
        if ( prefs != null ) {
            mSrcLocation = prefs.getString( prefix+PREF_LOCATION, mSrcLocation );
            mURL = prefs.getString( prefix+PREF_URL, mURL );
            mZip = prefs.getString( prefix+PREF_ZIP, mZip);
            mShowF = prefs.getBoolean( prefix+PREF_SHOWF, mShowF );
        }
    }

    /**
     * Draw our data values to the UI
     * 
     */
    public void updateUIFields() {
        if ( mView == null ) return;
        Log.v(TAG,"Weather:update UI Fields" );

        setText( R.id.textLocation, mSrcLocation );
        setText( R.id.textCurrentTemp, ((mShowF) ? mData.CurrentF  + mContext.getString( R.string.f_label)
                : mData.CurrentC+ mContext.getString( R.string.c_label) ) );
        setText( R.id.textWeatherText, mData.CurrentCondition );
        setIcon( R.id.imageWeatherIcon, mData.CurrentIcon );

        setText( R.id.textUpdated, mData.CurrentTime );
        setIcon( R.id.icon1Image, mContext.getString( R.string.windicon) );     // hardcoded icon
        setText( R.id.icon1Text, mData.Wind + " " +
                ((mShowF) ? (mData.WindMph + " " + mContext.getString( R.string.mph_label))
                        : (mData.WindKph + " " + mContext.getString( R.string.kmh_label))) );

        setIcon( R.id.icon2Image, mContext.getString( R.string.rainicon) );     // hardcoded icon
        setText( R.id.icon2Text,  mContext.getString( R.string.precip_label) +
                ((mShowF) ? mData.PrecipIn : mData.PrecipMetric) + " " +
                mContext.getString((mShowF) ? R.string.in_label : R.string.mm_label )  );

        // set five days of forecast
        setForecastFields( 0, R.id.f1Day, R.id.f1Lo, R.id.f1Hi, R.id.f1POP, R.id.f1Icon );
        setForecastFields( 1, R.id.f2Day, R.id.f2Lo, R.id.f2Hi, R.id.f2POP, R.id.f2Icon );
        setForecastFields( 2, R.id.f3Day, R.id.f3Lo, R.id.f3Hi, R.id.f3POP, R.id.f3Icon );
        setForecastFields( 3, R.id.f4Day, R.id.f4Lo, R.id.f4Hi, R.id.f4POP, R.id.f4Icon );
        setForecastFields( 4, R.id.f5Day, R.id.f5Lo, R.id.f5Hi, R.id.f5POP, R.id.f5Icon );

        // Set forecast webview with appropriate text

        // work around "known bug in (older) WebView, where if you have any percentages in the supplied data,
        // the data is interpreted as a URL" : http://stackoverflow.com/questions/12792576/android-2-3-webview-loaddata-just-shows-encoded-characters
        // if more time, i probably would see if textview is a better choice since webview is overkill

        String x = (mShowF) ? mData.FiveDayForecastTextF : mData.FiveDayForecastTextC ;
        x = x.replaceAll("%", "&#37" );     // work around bug in older webviews where % in feed
        x = x.replaceAll("&nbsp", " ");     // &nbsp too.. sheesh
        WebView wvText= (WebView) mView.findViewById( R.id.forecastWebView  );
        if ( wvText != null ) {
            wvText.loadData( x, "text/html; charset=UTF-8", null );
            wvText.invalidate();
        }
        mView.invalidate();
    }

    /**
     * Util: Set forecast text/icons for day 'i' into specified UI controls
     * 
     * @param i         - forecast day index
     * @param f1day     - id of day-of-week textview
     * @param f1lo      - id of lo temp textview
     * @param f1hi      - id of hi temp textview
     * @param f1pop     - id of precip textview
     * @param f1icon    - id of forecast icon imageview
     */
    private void setForecastFields(int i, int f1day, int f1lo, int f1hi, int f1pop, int f1icon) {
        setText( f1day, mData.ForecastDay[i]);
        setText( f1lo, (mShowF) ? mData.ForecastLoF[i] : mData.ForecastLoC[i]);
        setText( f1hi, (mShowF) ? mData.ForecastHiF[i] : mData.ForecastHiC[i]);
        setText( f1pop,  mData.ForecastPOP[i]  +"%" );
        setIcon( f1icon, mData.ForecastIcon[i] );
    }


    /**
     * Util, Set imageview specified by id to icon from friendly name specified by 'str'
     * 
     * @param id - id of ImageView control
     * @param str - text name of icon [mapped to resource via lookupIcon() ]
     */
    private void setIcon( int id , String str ) {

        if ( mView == null ) return;
        ImageView iv = (ImageView)mView.findViewById( id  );
        if ( iv != null ) {
            int icon=lookupIcon( str );
            if ( icon != -1 ) {
                iv.setImageResource( icon );
            }
        }
    }

    /**
     * Util, Set text in textview id to string
     * @param id - id of textView control
     * @param str - text to set in control
     */
    private void setText( int id, String str ) {
        if ( mView == null ) return;
        TextView tv = (TextView)mView.findViewById( id  );
        if ( tv != null ) {
            tv.setText( ( str != null ) ? str : "");    // display blanks if one of these is null
        }
    }

    /**
     * Util, lookup string "icon" from WU to a drawable ID
     * 
     * TODO: refactor to hash table lookup?  String resources?  Leave for now?
     * 
     * @param str
     * @return  id of icon resource
     */
    private int lookupIcon( String str ) {
        if ( str== null ) return R.drawable.weath05;    // unknown, default

        // these two are strings we use in our code
        if ( str.equalsIgnoreCase("windicon")) return R.drawable.weath06;
        if ( str.equalsIgnoreCase("rainicon")) return R.drawable.weath43;

        // these are ones that WU API can return
        if ( str.equalsIgnoreCase("chanceflurries")) return R.drawable.weath21;
        if ( str.equalsIgnoreCase("chancerain")) return R.drawable.weath17;
        if ( str.equalsIgnoreCase("chancesleet")) return R.drawable.weath24;
        if ( str.equalsIgnoreCase("chancesnow")) return R.drawable.weath21;
        if ( str.equalsIgnoreCase("chancetstorms")) return R.drawable.weath16;
        if ( str.equalsIgnoreCase("clear")) return R.drawable.weath02;
        if ( str.equalsIgnoreCase("cloudy")) return R.drawable.weath14;
        if ( str.equalsIgnoreCase("flurries")) return R.drawable.weath23;
        if ( str.equalsIgnoreCase("fog")) return R.drawable.weath12;
        if ( str.equalsIgnoreCase("hazy")) return R.drawable.weath12;
        if ( str.equalsIgnoreCase("mostlycloudy")) return R.drawable.weath08;
        if ( str.equalsIgnoreCase("mostlysunny")) return R.drawable.weath02;
        if ( str.equalsIgnoreCase("partlycloudy")) return R.drawable.weath08;
        if ( str.equalsIgnoreCase("partlysunny")) return R.drawable.weath02;
        if ( str.equalsIgnoreCase("rain")) return R.drawable.weath18;
        if ( str.equalsIgnoreCase("sleet")) return R.drawable.weath23;
        if ( str.equalsIgnoreCase("snow")) return R.drawable.weath23;
        if ( str.equalsIgnoreCase("sunny")) return R.drawable.weath02;
        if ( str.equalsIgnoreCase("tstorms")) return R.drawable.weath26;
        if ( str.equalsIgnoreCase("unknown")) return R.drawable.weath05;

        return -1;
    }
}