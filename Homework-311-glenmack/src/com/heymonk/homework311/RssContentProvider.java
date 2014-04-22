package com.heymonk.homework311;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class RssContentProvider extends ContentProvider {

    public static final String TAG = RssContentProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.heymonk.homework311.RssContentProvider";
    private static final int RSSElement = 1;
    private static final int RSSElement_ID = 2;
    RssSQLiteOpenHelper mSQLHelper;

    private static final UriMatcher mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(AUTHORITY, "articles", RSSElement);
        mURIMatcher.addURI(AUTHORITY, "articles/#", RSSElement_ID);
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {

        SQLiteDatabase db = mSQLHelper.getWritableDatabase();
        int count;
        switch (mURIMatcher.match(uri)) {
            case RSSElement:
                count = db.delete(RssFeed.RssEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override public String getType(Uri uri) {

        switch (mURIMatcher.match(uri)) {
            case RSSElement:
                return RssFeed.DIR_CONTENT_TYPE;

            case RSSElement_ID:
                return RssFeed.ITEM_CONTENT_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues cv) {

        //Log.v(TAG,"INSERT!" + cv.toString() );

        if (mURIMatcher.match(uri) != RSSElement) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mSQLHelper.getWritableDatabase();
        long rowID = db.insert(RssFeed.RssEntry.TABLE_NAME, null, cv);
        if (rowID > 0) {

            Uri noteUri = ContentUris.withAppendedId(RssFeed.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        else {
            Log.e(TAG, "insert() Error inserting rss entry");
        }

        return null;
    }

    @Override public boolean onCreate() {

        mSQLHelper = new RssSQLiteOpenHelper(getContext());
        return true;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (mURIMatcher.match(uri)) {
            case RSSElement: {
                qb.setTables(RssFeed.RssEntry.TABLE_NAME);
                break;
            }
            case RSSElement_ID: {
                qb.setTables(RssFeed.RssEntry.TABLE_NAME);
                selection = "_ID = ?";
                String id = uri.getLastPathSegment();
                selectionArgs = new String[] {id};
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        SQLiteDatabase db = mSQLHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        SQLiteDatabase db = mSQLHelper.getWritableDatabase();

        int count;
        switch (mURIMatcher.match(uri)) {
            case RSSElement: {
                count = db.update(RssFeed.RssEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}