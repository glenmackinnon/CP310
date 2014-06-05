package com.heymonk.hw314weather.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class ProgressDialogHelper {
    private static final String TAG = "HW314:ProgressDialog";

    private long mSpinStart;                            // when we showed dialog
    private ProgressDialog mRefreshDialog;              // dialog itself
    private static long PROGRESS_DIALOG_MINTIME=700;    // minimum display time for dialog

    public void show( Context c, String msg  )
    {
        if ( mRefreshDialog == null ) {
            // turn on progress dialog and keep track of when we turned it on
            // mRefreshDialog being non null is an indicator we have the dialog up
            Log.v(TAG, "progress dialog on!");
            mSpinStart = System.currentTimeMillis();
            mRefreshDialog = new ProgressDialog( c );
            mRefreshDialog.setMessage( msg );
            mRefreshDialog.setIndeterminate(true);  // spinner
            mRefreshDialog.setCancelable(true);     // leave true for debugging, user can click outside =cancel
            mRefreshDialog.show();
        }
    }

    public void hide (  )
    {
        // turn off progress dialog - make sure it's been up at least 2.5s total though so it shows

        if ( mRefreshDialog != null ) {
            // minor bit of logic to make sure dialog has been up at least MINTIME ms

            long elapsed = System.currentTimeMillis() - mSpinStart;
            long delay = ( elapsed < PROGRESS_DIALOG_MINTIME ) ? (PROGRESS_DIALOG_MINTIME -elapsed) : 100;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    if ( mRefreshDialog != null && mRefreshDialog.isShowing() ) {
                        mRefreshDialog.dismiss();
                    }
                    mRefreshDialog =null;
                }}, delay);
        }

    }
}