package com.heymonk.homework312.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeHelper {

    public String convert( String src, String srcfmt, String dstfmt )
    {
        String result="";


        SimpleDateFormat srcsdf = new SimpleDateFormat( srcfmt, Locale.US );
        SimpleDateFormat dstsdf = new SimpleDateFormat( dstfmt , Locale.US);
        try {
            result  = dstsdf.format( srcsdf.parse( src ));
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String from_millis( String dt, String dstfmt )
    {
        String result="";
        Date d;

        try {
            d= new Date( Long.parseLong(dt) );
        } catch ( java.lang.NumberFormatException e ) {
            return "";
        }

        SimpleDateFormat dstsdf = new SimpleDateFormat( dstfmt , Locale.US);
        result  = dstsdf.format( d );
        return result;
    }

    public String from_millis( long dt, String dstfmt )
    {
        String result="";

        Date d= new Date( dt );

        SimpleDateFormat dstsdf = new SimpleDateFormat( dstfmt , Locale.US);
        result  = dstsdf.format( d );
        return result;
    }


    public long to_millis( String src, String srcfmt ) {

        SimpleDateFormat srcsdf = new SimpleDateFormat( srcfmt, Locale.US );

        Date date;
        try {
            date = srcsdf.parse(  src );
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
        return  date.getTime();
    }
}
