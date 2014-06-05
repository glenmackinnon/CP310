
package com.heymonk.hw314weather;

import com.heymonk.hw314weather.R;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.view.ViewPager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;
/**
 * Readme.MD  -- homework-314-glenmack
 * 
 * -- Wanted to implement "pull to refresh" but didn't get through it - refresh icon is poor man's compromise, although
 *    it does push Delete off screen in many cases
 * -- interpreted "both C&F" from HRD as a toggle allowing switching between, not both sets of data onscreen at once
 * -- have three default locations, with Seattle being the first (to satisfy HRD) then Mercer Island (home) + Kona (ideal home)
 * -- WU supports Canadian postal codes, so regex on input allows ZIP or postal code
 * -- icons are from "meteocons" - http://www.alessioatzeni.com/meteocons - I converted them from SVG to PNG
 *    after issues with SVG rendering code :-)
 * -- FragmentStatePagerAdapter is interesting (allows for paging through) but makes managing the fragments kind of
 *    tricky - resorted to brute force recreation of fragments instead of saving/restoring state in a few cases, but at
 *    least these frags are pretty lightweight and the weather data itself isnt refetched
 * -- layouts are good but not perfect for all devices
 * -- drawing is done by Weather class into layout from WeatherFragment - longer term i may have refactored the drawing
 *    code back into the fragment and left Weather "data & fetch only"
 * -- JSON parsing code feels "hard coded" and frail - looked briefly at GSON but decided against it for simplicity
 * -- JSON "frail" - if elements missing, or JSON ever changes, it'll catch, and UI will show whatever fields it did
 *    manage to read, as well as marking the data as 'bad' so it will be refetched onthe next try instead of caching
 * -- minimal error handling or display around JSON reading... go "refresh" button!
 * -- no error handling if ZIP is mis-entered (only those passing regex are kept)
 * 
 * -- For weather feed, this uses WeatherUnderground "free" API tier - JSON based; Caveats:
 *      -- Max 10 calls per minute  (so I've added a "Throttle" class to make sure I dont go over 10 calls
 *         per minute - if it does, access key is disabled for the rest of the day (booo!)
 *      -- WU URL requires an account specific key right in the URL - the " " part
 *         For this exercise, I didnt attempt to hide it but in real world I imagine i'd be storing it in
 *         an Account and maybe even obscuring the web call itself
 *      -- Sometimes the "Celsius" version of the text forecast is blank in their data (not sure why)
 *      -- WU requires attribution logo - I put one  in the bottom right and linked it to their WunderMap radar map
 *      -- didnt use syncadapter for data - simple JSON fetch seemed quicker and easier for this simple case and small
 *         (16KB-ish) data stream
 * 
 * 
 * @author Glen
 *
 */
public class MainActivity extends ActionBarActivity  {

    private static final String TAG = "HW314Weather";
    private static final String PREFSKEY = "com.heymonk.weather314";
    private static final String PREFCOUNTKEY = "count";

    static WeatherPagerAdapter  mWeatherPager;
    static ViewPager mViewPager;

    // Weather items we are tracking - master copy
    static List<Weather> mWeather = new ArrayList<Weather>();       // weathers - int index
    static int mCount=0;                                            // number of weather locations


    /**
     * Create
     * - Make pager adapter, viewpager and connect
     * - register clickhandler for WeatherUnderground logo
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ( mWeather.isEmpty() ) {         // load up our weather objects if first time in
            loadSavedWeatherSettings();
        }

        // pretty up our actionbar (esp important on non-holo dark themed devices)
        // cough... hack to work in API10 thru 19 --- 4.0 and higher setTitleColor() doesnt work?

        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView abTitle = (TextView) findViewById(titleId);
        if ( abTitle != null ) {
            abTitle.setTextColor(getResources().getColor( R.color.colorActionBarText));
        }
        setTitle(R.string.apptitle );
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable( getResources().getColor( R.color.colorActionBarBackground )));

        // set up our viewpager and adapter and weather

        mWeatherPager =new WeatherPagerAdapter( getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mWeatherPager.setWeather( mWeather );
        Log.v(TAG,"oncreate, setWeather - count = " + mWeather.size() );
        mViewPager.setAdapter(mWeatherPager);
        mWeatherPager.notifyDataSetChanged();

        // Code to handle click on wulogo
        ImageView v= (ImageView) findViewById( R.id.wulogo );
        if ( v != null ) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    launchWunderMapURL();
                }
            });
        }
    }

    /**
     * Launch  web page with "WunderMap" for selected ZIP (my favorite feature of their site)
     */
    protected void launchWunderMapURL() {

        // build URL from string resource + zip

        String zip = mWeather.get( mViewPager.getCurrentItem() ).getZip();
        String url = String.format( getString(R.string.WuMapUrl ), zip);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }


    @Override public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handle action bar or menu item selection
     */
    @Override public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh ) {               // reload current
            int cur = mViewPager.getCurrentItem();
            mWeatherPager.Refresh ( cur, true );        // force data fetch
            return true;
        }

        if ( id == R.id.action_toggleCF ) {             // toggle Celsius and Fahrenheit views
            int i = mViewPager.getCurrentItem();
            mWeather.get( i ).toggleUnits();
            saveSettings();                             // save settings, overkill to write all weather data but...
            mWeatherPager.notifyDataSetChanged();       // HACK: rebuild frags
            //mWeatherPager.Refresh( i, false );        // refresh weather tile 'i' - doesnt work after rotate?
            return true;
        }

        if ( id == R.id.action_add ) {          // add a location
            addNewZIP();
            return true;
        }

        if ( id == R.id.action_delete ) {       // delete current location
            deleteCurrentWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Delete current weather item and update UI
     */
    private void deleteCurrentWeather() {

        int cur = mViewPager.getCurrentItem();
        mWeather.remove( cur );
        --mCount;
        if ( mCount == 0 ) {
            // Oops, too much ... add our default of Seattle back in
            mWeather.add(new Weather( getString( R.string.default_city), getString( R.string.default_zip),
                    getFetchURL( getString( R.string.default_zip)) ));
            mCount = 1;
        }
        // save settings and refresh all of the fragments
        saveSettings();
        mWeatherPager.setWeather( mWeather );
        mWeatherPager.notifyDataSetChanged();                           // force update/recreate of frags
        mViewPager.setCurrentItem( (cur < mCount) ? cur : mCount );     // handle delete last item case
    }

    /**
     * Save count, then have all weather items save their settings
     */
    private void saveSettings( )
    {
        // Write the count so we know how many to load next time
        SharedPreferences prefs = getSharedPreferences(PREFSKEY, Context.MODE_PRIVATE);
        if ( prefs != null ) {
            prefs.edit().putInt( PREFCOUNTKEY, mCount ).commit();
        }

        // loop thru each mWeather item and have it save self to shared prefs
        for (int i = 0; i < mCount; ++i) {
            mWeather.get(i).saveSettings(this, PREFSKEY, getString(R.string.KEYPREFIX)+Integer.toString(i));
        }
    }

    /**
     * Have all weather items load their settings (and load count)
     */
    private void loadSavedWeatherSettings()
    {
        SharedPreferences prefs = getSharedPreferences(PREFSKEY, Context.MODE_PRIVATE);
        int newCount = 0;
        mWeather.clear();   // clear existing list of Weathers

        if ( prefs != null ) {
            newCount = prefs.getInt(PREFCOUNTKEY , 0 );
        }
        if ( newCount != 0 ) {
            for ( int i=0; i<newCount; i++) {
                // create default & then restore
                mWeather.add(new Weather( getString(R.string.default_city), getString( R.string.default_zip), getFetchURL( getString( R.string.default_zip))));
                mWeather.get(i).restoreSettings(this, PREFSKEY, getString(R.string.KEYPREFIX)+Integer.toString(i) );
            }

        } else {
            // no saves yet (or some error), so let's start with our three default values
            mWeather.add( new Weather( getString(R.string.default_city), getString( R.string.default_zip), getFetchURL( getString( R.string.default_zip))));
            mWeather.add( new Weather( getString(R.string.default_city2), getString( R.string.default_zip2), getFetchURL( getString( R.string.default_zip2))));
            mWeather.add( new Weather( getString(R.string.default_city3), getString( R.string.default_zip3), getFetchURL( getString( R.string.default_zip3))));
            mCount =3;
            saveSettings();
        }
        mCount = mWeather.size();
    }


    /**
     * Pop dialog to ask user for ZIP and name (per HRD, at least the ZIP part)
     */
    void addNewZIP()
    {
        // Build in code for the practice :-)

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.Zip_Dialog_title));
        alert.setMessage(getString(R.string.Zip_Dialog_Help));

        // Set  EditText views to get user input

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText inputZip = new EditText(this);
        inputZip.setHint(getString(R.string.Zip_dialog_zip_hint));
        layout.addView(inputZip);

        final EditText inputName = new EditText(this);
        inputName.setHint( getString(R.string.Zip_dialog_name_hint));
        inputName.setMaxLines(1);
        inputName.setSingleLine(true);
        layout.addView(inputName);

        alert.setView(layout );

        alert.setPositiveButton(getString(R.string.Zip_dialog_OK), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int whichButton) {

                String zip = inputZip.getText().toString();
                String name = inputName.getText().toString();
                zip.replaceAll("\\s+","");      // nuke spaces tabs etc

                if ( zip.matches("\\d\\d\\d\\d\\d") || zip.matches("[a-zA-Z]\\d[a-zA-Z]\\d[a-zA-Z]\\d")) {
                    // looks like a 5 digit ZIP or a Canada postal code (N4N2E8) to me
                    Log.v(TAG,"adding weather for "+zip +" (" + name + ")");
                    mWeather.add(new Weather( name, zip, getFetchURL( zip ) ));
                    mCount++;
                    saveSettings(); // write all of our weather items to prefs
                    mWeatherPager.setWeather( mWeather );
                    mWeatherPager.notifyDataSetChanged();
                    mViewPager.setCurrentItem( mCount-1 );  //set to newly created item
                }
            }
        });

        alert.setNegativeButton(getString(R.string.Zip_dialog_cancel), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }


    /**
     * Build up query URL for WeatherUnderground service given ZIP code
     * @param zip
     * @return
     */
    private String getFetchURL( String zip )
    {
        return String.format( getString( R.string.base_url_printf ), zip );
    }

    /**
     * return weather object # id   (typically called by fragment #n to get its weather)
     * 
     * @param id
     * @return
     */
    public Weather getWeather(int id ) {
        if ( mWeather !=null ) {
            return mWeather.get( id );
        } else {
            return null;
        }
    }

}