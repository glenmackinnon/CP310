package com.heymonk.homework311;

import com.heymonk.homework311.R.id;
import com.heymonk.homework311.R.layout;
import com.heymonk.homework311.RssFeed.RssEntry;

import android.support.v4.app.LoaderManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * A placeholder fragment containing a simple view.
 */

public class MainListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    ListView mRssList;
    SimpleCursorAdapter mAdapter;
    private ProgressDialog mRefreshDialog = null;
    private static final int LOADER_ID = 1;
    private static final String TAG = "Homework311:MainListFragment";

    public MainListFragment() {
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRssList = (ListView)rootView.findViewById(R.id.RssListView );

        // setup click handler - when clicked, call showDetails() function in main activity with ID
        mRssList.setOnItemClickListener( new ListView.OnItemClickListener(){
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MainActivity)getActivity()).ShowDetails( id );
            } } );

        return rootView;
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        initProvider();
    }

    @Override public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor curs) {
        if ( loader.getId() == LOADER_ID ) {
            // The asynchronous load is complete and the data is now available for use. Now can we associate
            // the queried Cursor with the SimpleCursorAdapter and then the listview will show the data
            mAdapter.swapCursor(curs);
            Log.v(TAG,"load finished - swap cursor in");
            if ( mRefreshDialog != null ) {
                mRefreshDialog.dismiss();
                Log.v(TAG, "progress dialog off!");
                mRefreshDialog=null;
            }
        }
    }

    @Override public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with a null Cursor.
        Log.v(TAG,"loader reset- swap cursor out");
        mAdapter.swapCursor(null);
    }

    @Override public android.support.v4.content.Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        //CursorLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
        Log.v(TAG,"create cursor");
        // query content provider by URI for all fields in projection (see RSSFeed.java)
        return new CursorLoader( this.getActivity(),  RssFeed.CONTENT_URI, RssEntry.PROJECTION, null, null, null);
    }

    /**
     * Setup the content provider and connect simpleCursorAdapter to talk to the listview
     * @param mainActivity TODO
     * 
     */
    void initProvider()  {

        String[] from = { RssEntry.TITLE, RssEntry.ICON}; // mappings from the cursor result to the display field
        int[] to = { id.rss_lvtitle, id.rss_lvicon  };

        // Create a simple cursor adapter and connect to our list view
        // set last param "flags" =0 to prevent deprecation call - real cursor is put in by onLoadFinished call
        mAdapter = new SimpleCursorAdapter( this.getActivity(), layout.rss_list_item, null, from, to, 0 );
        mRssList.setAdapter(mAdapter);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this );
    }

    /**
     * Called when user shakes device to reload content - for now, just poke that the dataset changed
     */
    public void reloadContent() {


        // reconnect and query provider
        //initProvider();
        if ( mAdapter != null ) {
            mAdapter.notifyDataSetChanged();
        }
    }
}