package com.heymonk.hw314weather.util;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class Throttle {

    private static final String TAG = "Util:Throttle";
    // default values
    static private final int    DEFAULT_MAX_CALLS =9;       // 9 calls max in
    static private final int    DEFAULT_MAX_TIME =600000;    // 60 seconds

    List<Long> mTSList = new ArrayList<Long>();       // weathers - int index
    private int mMaxCalls = DEFAULT_MAX_CALLS;
    private int mMaxTime = DEFAULT_MAX_TIME;

    /**
     * Constructor that allows the App to set throttling rate
     * 
     * @param max
     * @param calls
     */
    public  Throttle( int max, int calls ) {
        mMaxCalls = calls;
        mMaxTime = max;
    }

    /** Check if we can make API call and still fall within throttling spec
     *  If call is OK'd, then we add its timestamp to our list
     * 
     * @return  true if OK to make call, false if it should be throttled
     */
    public boolean check() {
        Long  tsNow = System.currentTimeMillis();

        // remove the entries out of time range of interest
        for ( int i = mTSList.size()-1; i >= 0; i-- ) {
            if ( (tsNow - mTSList.get(i)) > mMaxTime ) {
                mTSList.remove( i );  // remove entries more than nMaxTime ago
            }
        }

        // If (list + this new item) is at max (or greater!!) then return throttled and assume that the call
        // won't happen (e.g.don't add this attempt to the list)

        if ( (mTSList.size()+1) >= mMaxCalls ) {
            Log.v(TAG, "Throttled!");
            return false;               // false= not allowed to call
        } else {
            mTSList.add( tsNow );
        }
        return true;                // True, OK to make the call
    }
}