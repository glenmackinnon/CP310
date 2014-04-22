package com.heymonk.homework311;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
//
// DB :   HW311RSS   TABLE:  RssTable
// Columns:  int _ID, TEXT Title, DateTime Date, TEXT Content, TEXT Icon, TEXT Provider
//

public class RssSQLiteOpenHelper extends SQLiteOpenHelper {

    private final static String DB_NAME = RssFeed.RssEntry.DB_NAME;         // database name
    private final static String TABLE_NAME = RssFeed.RssEntry.TABLE_NAME;   // table name
    private final static int DB_VERSION = 1;

    // fields

    private final static String TABLE_ROW_ID = RssFeed.RssEntry.rowID;
    private final static String TABLE_ROW_TITLE = RssFeed.RssEntry.TITLE;
    private final static String TABLE_ROW_DATE= RssFeed.RssEntry.DATE;
    private final static String TABLE_ROW_CONTENT = RssFeed.RssEntry.CONTENT;
    private final static String TABLE_ROW_ICON= RssFeed.RssEntry.ICON;
    private final static String TABLE_ROW_PROVIDER= RssFeed.RssEntry.PROVIDER;

    // TAG for logging
    private static final String TAG = "Homework311:SqlOpenHelper";

    public RssSQLiteOpenHelper(Context context) {

        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        Log.v(TAG,"OnCreate");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        String createTableQueryString =
                "CREATE TABLE " +
                        TABLE_NAME + " (" +
                        TABLE_ROW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        TABLE_ROW_TITLE + " TEXT, " +
                        TABLE_ROW_DATE + " DATETIME, " +
                        TABLE_ROW_CONTENT + " TEXT, " +
                        TABLE_ROW_ICON + " TEXT, " +
                        TABLE_ROW_PROVIDER + " TEXT " +

                        ");";

        Log.v(TAG,"SQL Create={"+createTableQueryString+"}");
        db.execSQL(createTableQueryString);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
