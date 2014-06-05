
package com.heymonk.hw314weather;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.List;

/**
 * Our weather pager adapter - using fragments
 * 
 * Ref: http://developer.android.com/reference/android/support/v13/app/FragmentStatePagerAdapter.html
 * 
 * @author Glen
 */
public class WeatherPagerAdapter extends FragmentStatePagerAdapter {
    //private static final String TAG = "WeatherPagerAdapter";

    private static final String TAG = "HW314: WeatherPagerAdapter";

    // list to keep track of our fragments
    private SparseArray<WeatherFragment> mPageReferenceMap=null;

    // our copy of the weather items list (for count, mostly)
    private static List<Weather> mW;


    public WeatherPagerAdapter(FragmentManager fm) {
        super(fm);
        if ( mPageReferenceMap == null ) {
            mPageReferenceMap = new SparseArray<WeatherFragment>();
        }
    }

    public void setWeather(List<Weather> weather) {
        mW = weather;
    }

    @Override public int getCount() {
        if (mW == null) {
            return 0;
        } else {
            //Log.v(TAG,"Getcount=" + mW.size() );
            return mW.size();
        }
    }

    /**
     *  Use location friendly name field as page title
     */
    @Override public CharSequence getPageTitle(int position) {
        return mW.get(position).getLocation();
    }

    /**
     *  Ugly, but simplifying for now... force fragment re-creation when dataset changed
     */
    @Override public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /**
     * Create and return an new WeatherFragment for item i and add to our list
     */
    @Override public Fragment getItem(int i) {
        Log.v(TAG, "New weatherfragment for slot # " + i );
        WeatherFragment fragment = new WeatherFragment();

        Bundle args = new Bundle();
        args.putInt("weatherId", i);            // weatherId is the key into what weather we're getting
        fragment.setArguments(args);            // fragment calls back to MainActivity for its weather data

        mPageReferenceMap.put( i, fragment );   // keep track of ID:fragment in our list
        return fragment;
    }

    /**
     * Destroy a fragment and take out of our list
     */
    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(Integer.valueOf(position));    // remove ID:fragment from our list
    }

    /**
     * Return fragment at location i
     * 
     * @param i - fragment position
     * @return WeatherFragment at position i
     */
    public WeatherFragment getItemFragment( int i ) {
        return mPageReferenceMap.get( i );
    }

    /**
     * Refresh fragment/item cur, force a data fetch if 'force' is true
     * 
     * @param cur
     * @param force
     */
    public void Refresh(int cur, boolean force) {
        WeatherFragment f= mPageReferenceMap.get( cur );
        if ( f != null ) {
            f.populateUI( force );
        }
    }
}
