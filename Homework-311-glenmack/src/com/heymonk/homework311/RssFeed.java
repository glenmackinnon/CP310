package com.heymonk.homework311;


import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class contains data structures and URIs for RSS data provider
 * 
 * @author Glen
 *
 */
public class RssFeed implements BaseColumns {

    public static final Uri CONTENT_URI = Uri.parse("content://" + RssContentProvider.AUTHORITY + "/articles");
    public static final String DIR_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.heymonk.homework311.provider.articles";
    public static final String ITEM_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.heymonk.homework311.provider.articles";

    public static final class RssEntry {

        public static final String DB_NAME = "HW311RSS"; // must match in RssSqlLiteOpenHelper
        public static final String TABLE_NAME = "RssTable"; // must match in RssSqlLiteOpenHelper

        public static final String rowID = BaseColumns._ID;
        public static final String TITLE = "title";
        public static final String DATE = "date";
        public static final String CONTENT = "content";
        public static final String ICON = "icon";
        public static final String PROVIDER = "provider";

        public static final String[] PROJECTION = new String[] {
            /* 0 */ RssFeed.RssEntry.rowID,
            /* 1 */ RssFeed.RssEntry.TITLE,
            /* 2 */ RssFeed.RssEntry.DATE,
            /* 3 */ RssFeed.RssEntry.CONTENT,
            /* 4 */ RssFeed.RssEntry.ICON,
            /* 5 */ RssFeed.RssEntry.PROVIDER

        };
    }
}