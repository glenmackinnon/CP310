package com.heymonk.homework312;


import com.heymonk.homework312.contentprovider.RssFeed;
import com.heymonk.homework312.util.DateTimeHelper;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;




/**
 * Fragment showing "detail" (full article) view of an RSS entry
 */
public class RssDetailFragment extends Fragment implements OnClickListener {

    private static final String TAG = "Homework312:DetailFragment";
    private static long contentID=-1;
    private View rootView = null;
    private String mStoryUrl;

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

        // catch clicks on the titlebox and use them to launch web browser with the story's link
        RelativeLayout titlebox =(RelativeLayout ) rootView.findViewById(R.id.rss_titlebox );
        titlebox.setOnClickListener( this );

        return rootView;
    }


    /**
     * Added feature - users click in "TitleBox" area takes you to full story in web browser
     */
    @Override public void onClick(View v) {
        if ( mStoryUrl != null ) {
            Log.v(TAG,"title clicked, launching URL " + mStoryUrl );
            Intent i = new Intent(Intent.ACTION_VIEW);  // launch intent to start web browser for this story
            i.setData(Uri.parse(mStoryUrl));
            startActivity(i);
        }
    }

    /**
     * Populate the title, text, timestamp and icon data in this detail view by
     * querying the contentProvider with the id given in contentID
     */
    private void readContent() {

        if ( contentID != -1 ) {    // we have a proper value
            Cursor c;
            String [] id = { String.valueOf( contentID ) };

            ContentResolver cr = this.getActivity().getContentResolver();
            c = cr.query(RssFeed.CONTENT_URI, RssFeed.RssEntry.PROJECTION, "_ID = ?",  id, null);

            if ( c.getCount() != 0 ) {
                c.moveToNext(); // move to first match and use that results' data

                // Webview to hold description  (add <html> and </html> tags to make it html)
                WebView tv =(WebView) rootView.findViewById(R.id.rss_contentviewWeb );
                tv.loadData( "<html>"+c.getString( RssFeed.RssEntry.INDEX_CONTENT )+"</html>", "text/html", null );
                tv.invalidate();

                // convert date from standard to nice detailview format and display
                TextView dt =(TextView) rootView.findViewById(R.id.rss_dateview );
                DateTimeHelper t = new DateTimeHelper();
                String d = t.from_millis( c.getString( RssFeed.RssEntry.INDEX_DATE),getActivity().getString(R.string.DetailView_Date_format) );
                dt.setText( d );

                // set the title value
                TextView title =(TextView) rootView.findViewById(R.id.rss_titleview );
                title.setText( c.getString( RssFeed.RssEntry.INDEX_TITLE ));

                // Load provider icon for top bar in this view (since story has real picture)
                // using our uber "resource setter or bitmap +cache" resolver class

                ImageView icon = (ImageView) rootView.findViewById( R.id.rss_icon );
                String iconstr = c.getString( RssFeed.RssEntry.INDEX_PROVIDERICON );
                MainActivity m = (MainActivity)getActivity();
                m.getCache().HandleImageId( m.getApplicationContext(), iconstr, icon );

                // set Provider title value from field
                TextView prov =(TextView) rootView.findViewById(R.id.rss_providername );
                prov.setText( c.getString( RssFeed.RssEntry.INDEX_PROVIDERNAME ));

                // set our URL to full story value.  if title clicked, launch that HTML link
                mStoryUrl = c.getString( RssFeed.RssEntry.INDEX_LINK );
            }
            c.close();
        }
    }
}