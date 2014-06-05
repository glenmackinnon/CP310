package com.heymonk.hw314weather;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Our weather view fragment - uses "weatherID" int ID to identify
 * what Weather object when we call back to MainActivity for data
 */
public class WeatherFragment extends Fragment {

    private static final String TAG = "HW 314- WeatherFragment";

    private Weather mWeather;
    View mRootView;
    private int mWeatherId=-1;

    @Override public void setArguments(Bundle args) {
        super.setArguments(args);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get an ID for our position from passed "weatherId"; default to 1 if not set (it should be)
        mWeatherId = (getArguments() != null) ? getArguments().getInt("weatherId") : 1;
        Log.v(TAG,"WF:OnCreate mwid=" + mWeatherId);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        //Log.v(TAG,"WF:OnResume mwid=" + mWeatherId);
        populateUI( false );    // update UI fields - don't force a data fetch if we have data
        super.onResume();
    }

    /**
     * Create our view, update UI
     */
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Call back to main activity to get weather class/info based on mWeatherId
        //Log.v(TAG,"OnCreateView");
        mWeather = ((MainActivity) getActivity()).getWeather( mWeatherId );
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);
        return mRootView;
    }

    public void populateUI( boolean force ) {
        //Log.v(TAG,"populateUI: called for weatherID " +mWeatherId);
        if (mRootView== null ) {
            Log.v(TAG, "rootview is null??");
        }

        // if we have info and a view, tell the weather object to fetch data (which also updates the UI)
        if ( mWeather != null && mRootView != null ) {
            mWeather.fetchData( mRootView, force );
        }
    }
}