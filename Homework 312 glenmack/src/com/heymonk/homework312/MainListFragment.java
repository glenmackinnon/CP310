package com.heymonk.homework312;

import com.heymonk.homework312.R.id;
import com.heymonk.homework312.R.layout;
import com.heymonk.homework312.contentprovider.RssFeed;
import com.heymonk.homework312.contentprovider.RssFeed.RssEntry;
import com.heymonk.homework312.util.DateTimeHelper;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


/**
 * Our list fragment - listview + simpleCursorLoader to show RSS feed items
 * 
 * viewBinder to handle setting appropriate thumbnail/bitmap and formatting date field
 */

public class MainListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    ListView mRssList;
    SimpleCursorAdapter mAdapter;
    private ProgressDialog mRefreshDialog = null;
    private static final int LOADER_ID = 1;
    private static final String TAG = "Homework311:MainListFragment";

    public MainListFragment( ) {
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

        Log.v(TAG,"create cursor");
        // query content provider by URI for all fields in projection (see RSSFeed.java)
        return new CursorLoader( this.getActivity(),  RssFeed.CONTENT_URI, RssEntry.PROJECTION, null, null, RssFeed.RssEntry.DATE + " DESC");
    }

    /**
     * Setup the content provider and connect simpleCursorAdapter to talk to the listview
     */

    void initProvider()  {

        String[] from = { RssEntry.TITLE, RssEntry.ICON, RssEntry.DATE }; // mappings from the cursor result to the display field
        int[] to = { id.rss_lvtitle, id.rss_lvicon, id.rss_lvdate  };

        // Create a simple cursor adapter and connect to our list view
        // set last param "flags" =0 to prevent deprecation call - real cursor is swapped in by onLoadFinished call
        mAdapter = new SimpleCursorAdapter( this.getActivity(), layout.rss_list_item, null, from, to, 0 );

        // View binder to handle icon and date formatting work
        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
            @Override public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

                //Log.v(TAG, "ViewBinder:SetViewValue: " + view +" : " + cursor+ " : " +Integer.toString( columnIndex ));
                if ( columnIndex == cursor.getColumnIndex(RssFeed.RssEntry.DATE )  ) {
                    // Clean up date format to simpler version for listview

                    DateTimeHelper t = new DateTimeHelper();
                    String d = t.from_millis( cursor.getString(columnIndex), getActivity().getString(R.string.ListView_Date_format) );
                    ((TextView) view).setText( d );

                    return true;
                }

                if ( columnIndex == cursor.getColumnIndex(RssFeed.RssEntry.ICON )  ) {

                    String imageId = cursor.getString(columnIndex);
                    ImageView imageVw = (ImageView) view;
                    // use bitmap cache helper to load bitmap async and write to view -
                    // function is smart enough to recognize int IDs and use resources instead
                    MainActivity m = (MainActivity)getActivity();
                    m.getCache().HandleImageId( m.getApplicationContext(), imageId, imageVw );
                    return true;
                }
                return false;
            }
        };
        mAdapter.setViewBinder(viewBinder);

        mRssList.setAdapter(mAdapter);
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this );
    }
}