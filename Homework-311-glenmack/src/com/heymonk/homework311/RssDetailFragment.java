package com.heymonk.homework311;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Fragment showing "detail" (full article) view of an RSS entry
 */
public class RssDetailFragment extends Fragment  {

    private static long contentID=-1;
    private View rootView = null;

    public RssDetailFragment() {

    }

    /**
     * Set the ID we use to get this view's data from the provider in member var contentID
     * Note contentID is *static* - e.g. can survive a rotate teardown/rebuild
     * 
     * @param id - long indicating article we should display (from contentProvider)
     */
    public void setContent(long id ) {
        contentID = id;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        readContent();

        // Detail view consumes clicks - otherwise they pass thru to listview fragment beneath us
        rootView.setClickable(true);
        return rootView;
    }
    /**
     * Populate the title, text, timestamp and icon data in this detail view by
     * querying the contentProvider with the id given in contentID
     */
    private void readContent() {

        if ( contentID != -1 ) {    // we have a proper value
            Cursor c;
            String [] id = { String.valueOf( contentID ) };

            // Get the Content Resolver - synchronous query, *probably* fast enough for this use
            // Could do what we did in MainListFragment with SimpleCursorAdapter ...

            ContentResolver cr = this.getActivity().getContentResolver();
            c = cr.query(RssFeed.CONTENT_URI, RssFeed.RssEntry.PROJECTION, "_ID = ?",  id, null);

            if ( c.getCount() != 0 ) {
                c.moveToNext(); // move to first match and use that results' data

                TextView tv =(TextView) rootView.findViewById(R.id.rss_contentview );
                tv.setText( c.getString( 3 )+"\n");       //id 3= content of RSS article

                // format date (field2) from 'full' 04/19/2014 12:34:56  to friendly form e.g.  Apr 19 12:34pm
                TextView dt =(TextView) rootView.findViewById(R.id.rss_dateview );
                SimpleDateFormat sdf = new SimpleDateFormat(getActivity().getString(R.string.DetailView_Date_format), Locale.US);
                SimpleDateFormat src = new SimpleDateFormat(getActivity().getString(R.string.std_date_format), Locale.US );
                String d="";
                try {
                    d = sdf.format( src.parse(c.getString(2)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                dt.setText( d );

                // set the title value from field #1
                TextView title =(TextView) rootView.findViewById(R.id.rss_titleview );
                title.setText( c.getString( 1 ));

                // hard code RSS icon for this iteration of homework
                // next version, use field 4's value
                ImageView icon = (ImageView) rootView.findViewById( R.id.rss_icon );
                icon.setImageDrawable(getResources().getDrawable( R.drawable.ic_rss1 ));
            }
            c.close();
        }
    }
}