package com.heymonk.homework312.util;

import com.heymonk.homework312.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class ProgressDialogHelper {
    private static final String TAG = "Homework312:ProgressDialog";
    // progress dialog
    private long mSpinStart;
    private ProgressDialog mRefreshDialog;
    private static long PROGRESS_DIALOG_MINTIME=2500;


    public void show( Context c, boolean state )
    {
        if ( state == true ) {
            if ( mRefreshDialog == null ) {
                // turn on progress dialog and keep track of when we turned it on
                // mRefreshDialog being non null is an indicator we have the dialog up
                Log.v(TAG, "progress dialog on!");
                mSpinStart = System.currentTimeMillis();
                mRefreshDialog = new ProgressDialog( c );
                mRefreshDialog.setMessage( c.getString(R.string.refreshing_content) );
                mRefreshDialog.setIndeterminate(true);
                mRefreshDialog.setCancelable(true);  // leave true for debugging
                mRefreshDialog.show();
            }
        } else {
            // turn off progress dialog - make sure it's been up at least 2.5s total though so it shows

            if ( mRefreshDialog != null ) {
                long elapsed = System.currentTimeMillis() - mSpinStart;
                long delay = ( elapsed < PROGRESS_DIALOG_MINTIME ) ? (PROGRESS_DIALOG_MINTIME -elapsed) : 200;     //
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        if ( mRefreshDialog != null ) {
                            mRefreshDialog.hide();
                        }
                        mRefreshDialog =null;
                    }}, delay);
            }
        }
    }


}
