package com.heymonk.homework312;

import com.heymonk.homework312.contentprovider.RssFeed;
import com.heymonk.homework312.util.DateTimeHelper;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
/**
 * Class that reads an XML feed and stores it in our HW312 ContentProvider
 * See HRD for basic details.
 * 
 * @author Glen
 */
public class AsyncXMLReader extends AsyncTask<URL, Integer, Long > {

    private static final String TAG = "Homework312-AsyncXMLreader";
    private MainActivity mActivity;

    public  AsyncXMLReader( MainActivity main  ) {
        mActivity = main;
    }

    /**
     *  Read and parse XML from given URL - upload each RSS story record to our content provider
     */
    private void parseXML( URL url )
    {
        // Sample stream:  https://news.google.com/news/section?topic=w&output=rss

        if ( url == null ) return;
        InputStream input = null;

        try {
            input = url.openStream();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if ( input == null ) return;        // fail the load

        InputSource inputSrc = new InputSource( input );
        XPath xpath = XPathFactory.newInstance().newXPath();

        // Xpath expression to get title,icon  as well as "content" nodes under <item>
        String expression = "//channel/title|//channel/image/url|//item/title|//item/description|//item/pubDate|//item/link";

        // Query: get list of nodes matching the above
        NodeList nodes = null;
        try {
            nodes = (NodeList)xpath.evaluate(expression, inputSrc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        if ( nodes == null ) {
            return;     // fail the load, nothing to do
        }
        // Simplification on read here - not terribly resilient to missing fields or different field orders
        //
        // Assumes XML is ordered channel title, then imageURL, then <items>,
        // where each <item> contains all four fields of interest
        // Ideally I would rewrite this to handle missing item elements and possible re-ordering of the
        // title/feed icon elements but ... it works for now

        int len = nodes.getLength();
        Log.v( TAG, "parseXML for url (" + url + "): Returned node count: " + len );

        if ( len == 0 ) return;     // nothing to do
        int i=0;

        // Header / feed stuff
        // sample XML
        //    <channel>
        //        <generator>NFE/1.0</generator>
        //        <title>World - Google News</title>
        //        <link>http://news.google.com/news?pz=1&ned=us&hl=en&topic=w </link>
        //        <image>
        //            <title>World - Google News</title>
        //            <url>https://ssl.gstatic.com/news-static/img/logo/en_us/news.gif</url>
        //           <link>http://news.google.com/news?pz=1&ned=us&hl=en&topic=w</link>
        //        </image>


        String providerName = "Rss";
        Node node = nodes.item(i);
        if ( node.getNodeName().contentEquals("title")) {     // channel/title
            providerName= node.getTextContent();
            Log.v(TAG, "found feed name/title of "+providerName );
            ++i;
        }

        // see if next node is image/link, if so, we have custom icon for feed
        String providerIcon = Integer.toString( R.drawable.ic_rss1 );
        node = nodes.item(i);
        if ( node.getNodeName().contentEquals("url")) {     // channel/image/url
            providerIcon = node.getTextContent();
            Log.v(TAG, "found default icon of "+providerIcon );
            ++i;
        }

        // Now go through the remaining items and look for data elements
        // Sample:
        //        <item>
        //            <title>Nigeria eyes prisoner swap for girls - USA TODAY</title>
        //            <link>http://news.google.com/news/url?sa=t&fd=R&ct2=us&usg=AFQjCNGmc91nepyDEpYzR3oZuSOZkIJ5VA&clid=c3a7d30bb8a4878e06b80cf16b898331&cid=52778501585576&ei=DE5yU-DIB-uOwQH3wQE&url=http://www.usatoday.com/story/news/world/2014/05/13/nigeria-school-girls-kidnap-drones/9033645/</link>
        //            <guid isPermaLink="false">tag:news.google.com,2005:cluster=52778501585576</guid>
        //            <category>World</category>
        //            <pubDate>Tue, 13 May 2014 16:38:51 GMT</pubDate>
        //            <description>  ---bunch of HTML /w article here --- </description>
        //        </item>


        ContentResolver cr = mActivity.getContentResolver();
        if ( cr == null ) {
            Log.e(TAG,"Error getting content resolver");
            return;
        }
        while( i < len  ) {
            String title="";
            String description="";
            String pubdate="";
            String storyLink="";
            String storyIcon = providerIcon;

            // Code below assumes XML is  well-formed ( title, description, pubdate, link, any order)

            for ( int j=0; j<4; j++ ) { // 4 tags per item
                node = nodes.item(i);
                //Log.v(TAG,":node ("+i+") "+node.getNodeName()+" value "+ ( node.getTextContent()) );
                if ( node.getNodeName().contentEquals("title")) {
                    title = node.getTextContent();
                }
                if (  node.getNodeName().contentEquals("description")) {
                    description = node.getTextContent();
                }
                if (  node.getNodeName().contentEquals("pubDate")) {
                    pubdate= node.getTextContent();
                }
                if (  node.getNodeName().contentEquals("link")) {
                    storyLink= node.getTextContent();
                }
                ++i;
            }
            // Now clean up our values and write to the content provider

            // Look at date time format, we have two apparent formats
            // format: pubdate='Wed, 30 Apr 2014 17:06:06 GMT'     "EEE, dd MMM yyyy HH:mm:ss zzz";
            // format: pubdate='Wed, 30 Apr 2014 11:24:06 -0400'   "EEE, dd MMM yyyy HH:mm:ss Z";

            String dateformat1="EEE, dd MMM yyyy HH:mm:ss zzz";
            String dateformat2="EEE, dd MMM yyyy HH:mm:ss Z";

            // TODO: should verify that timezone --> millis mapping is 100% correct

            DateTimeHelper t = new DateTimeHelper();
            Long dt = t.to_millis( pubdate, dateformat1 );
            if ( dt == -1 ) {
                // that didn't work, try other format
                dt = t.to_millis( pubdate, dateformat2 );
            }
            if ( dt == -1 ) {
                Log.v(TAG,"neither timeformat parsing worked");
            }

            // Do a couple of story fixups here to improve output
            //
            // 1. Fix up bad image src values in google RSS feed... they have lines like
            // Google: <img src="//t2.gstatic.com/images?q=tbn:ANd9GcTtSufaPEEI7UyCp1i7xZ6ZEgCBrhiRCnwRZZVyqePdawuteDYxquJ9xP1lMAfwkOgiVIi1lbY"

            String newstr = description.replace("img src=\"//", "img src=\"http://");
            description = newstr;

            // 2. Fix up whacked EM and single tick character in some Yahoo feeds - display as aE` if not fixed
            newstr = description.replace( "—", "-");
            description = newstr;
            newstr = description.replace( "’", "'");
            description = newstr;

            // Now try to get a better story icon out of the content using regex
            // Not ideal to use regex to read HTML, but should be OK for this limited use
            // Extract the first <img src="(.*)"> we find in description  ()= capture group
            //
            // Yahoo: <img src="http://l.yimg.com/bt/api/res/...IS-LUHANSK-SHOOTING.JPG" width="130" height="86...
            // Google: <img src="//t2.gstatic.com/images?q=tbn:ANd9GcTtSufaPEEI7UyCp1i7xZ6ZEgCBrhiRCnwRZZVyqePdawuteDYxquJ9xP1lMAfwkOgiVIi1lbY"
            // CBC: <img title='Transport Minister...Ottawa. ' height='259' alt='Rail Safety 20140423' width='460' src='http://i.cbc.ca/1.2619633.1399999373!/cpImage/httpImage/image.jpg_gen/derivatives/16x9_460/rail-safety-20140423.jpg' />
            //
            // need Multi line and both types of quotes (because CBC has newlines & use single ticks)

            Pattern p = Pattern.compile( ".*<img.*?src=[\\x22|\\x27](.*?)[\\x22|\\x27'].*>.*?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher( description );
            if ( m.matches() ) {
                // Log.v(TAG,"match icon="+storyIcon );
                storyIcon=m.group(1);
            } else {
                //Log.v(TAG,"no icon" + description );  // use default provider icon
            }

            // now write the values into the provider

            ContentValues cv = new ContentValues();

            cv.put(RssFeed.RssEntry.DATE, dt.toString());   // store time/date as long millis from epoch
            cv.put(RssFeed.RssEntry.ICON, storyIcon );
            cv.put(RssFeed.RssEntry.PROVIDERNAME, providerName );
            cv.put(RssFeed.RssEntry.PROVIDERICON, providerIcon );
            cv.put(RssFeed.RssEntry.LINK, storyLink );
            cv.put( RssFeed.RssEntry.TITLE, title );
            cv.put( RssFeed.RssEntry.CONTENT, description );

            // insert item via content provider URI
            cr.insert(RssFeed.CONTENT_URI, cv);
        }
    }

    /**
     * Load our XML urls in the background, calling parseXML for each
     */
    @Override protected Long doInBackground(URL... urls ) {
        int count = urls.length;
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            parseXML( urls[i] );
            publishProgress((int) ((i / (float) count) * 100));
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return totalSize;
    }

    @Override protected void onPostExecute(Long result) {
        Log.v(TAG,"Done parsing!");
        mActivity.XMLLoaded();         // call back to main activity to tell them we're done loading
        super.onPostExecute(result);
    }
}