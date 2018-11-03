package au.edu.usc.utting.be24_watchface;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarViewer {

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
            // TODO: add color
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;


    private static final String DEBUG_TAG = "MyCalendarViewer";
    public static final String[] INSTANCE_PROJECTION = new String[] {
            CalendarContract.Instances.EVENT_ID,      // 0
            CalendarContract.Instances.BEGIN,         // 1
            CalendarContract.Instances.TITLE          // 2
    };

    // The indices for the projection array above.
    // private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_BEGIN_INDEX = 1;
    private static final int PROJECTION_TITLE_INDEX = 2;


    public void showCalendars(Context context) {
        // Run query
        Cursor cur = null;
        ContentResolver cr = context.getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
                + CalendarContract.Calendars.OWNER_ACCOUNT + " = ?))";
        String[] selectionArgs = new String[]{"bm.utting@gmail.com", "com.gmail", "bm.utting@gmail.com"};
        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            String accountName = null;
            String ownerName = null;

            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

            // Do something with the values...
            Log.i(DEBUG_TAG, "Found calendar: " + displayName);
        }
    }

    public void showDay(Context context) {

        // Specify the date range you want to search for recurring
        // event instances
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(2018, 11, 05, 8, 0);
        long startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(2018, 11, 05, 23, 0);
        long endMillis = endTime.getTimeInMillis();

        ContentResolver cr = context.getContentResolver();

        // The ID of the recurring event whose instances you are searching
        // for in the Instances table
        String selection = CalendarContract.Instances.EVENT_ID + " = ?";
        String[] selectionArgs = new String[]{"207"};

        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        // TODO: catch permission errors and report them nicely!  Or request them?

        // Submit the query
        Cursor cur = cr.query(builder.build(),
                INSTANCE_PROJECTION,
                selection,
                selectionArgs,
                null);
        Log.i(DEBUG_TAG, "INSTANCES query returns: " + cur.getCount());

        while (cur.moveToNext()) {
            String title = null;
            long eventID = 0;
            long beginVal = 0;

            // Get the field values
            eventID = cur.getLong(PROJECTION_ID_INDEX);
            beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
            title = cur.getString(PROJECTION_TITLE_INDEX);

            // Do something with the values.
            Log.i(DEBUG_TAG, "Event:  " + title);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(beginVal);
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            Log.i(DEBUG_TAG, "Date: " + formatter.format(calendar.getTime()));
        }
    }
}
