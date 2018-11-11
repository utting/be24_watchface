package au.edu.usc.utting.be24_watchface;

import static au.edu.usc.utting.be24_watchface.Constants.TAG;
import static au.edu.usc.utting.be24_watchface.Constants.CONNECTION_TIME_OUT_MS;
import static au.edu.usc.utting.be24_watchface.Constants.CAL_DATA_ITEM_PATH_PREFIX;
import static au.edu.usc.utting.be24_watchface.Constants.ALL_DAY;
import static au.edu.usc.utting.be24_watchface.Constants.BEGIN;
import static au.edu.usc.utting.be24_watchface.Constants.DATA_ITEM_URI;
import static au.edu.usc.utting.be24_watchface.Constants.DESCRIPTION;
import static au.edu.usc.utting.be24_watchface.Constants.END;
import static au.edu.usc.utting.be24_watchface.Constants.EVENT_ID;
import static au.edu.usc.utting.be24_watchface.Constants.ID;
import static au.edu.usc.utting.be24_watchface.Constants.TITLE;
import static au.edu.usc.utting.be24_watchface.Constants.COLOR;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.CalendarContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.wearable.provider.WearableCalendarContract;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Queries calendar events using Android WearableCalendarContract API and creates an
 * Appointment item for each calendar event instance.
 *
 * This code is adapted from: https://www.programcreek.com/java-api-examples/?code=mauimauer/AndroidWearable-Samples/AndroidWearable-Samples-master/AgendaData/Application/src/main/java/com/example/android/wearable/agendadata/CalendarQueryService.java
 * And also from: https://www.javatips.net/api/android.support.wearable.provider.wearablecalendarcontract
 *
 * To send data back to UI:
 * * https://developers.google.com/android/reference/com/google/android/gms/wearable/DataClient ?
 *
 */
public class CalendarQueryService extends IntentService
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static final String[] INSTANCE_PROJECTION = {
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.CALENDAR_COLOR,
    };

    private GoogleApiClient mGoogleApiClient;

    public CalendarQueryService() {
        super(CalendarQueryService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver resultReceiver = intent.getParcelableExtra("RESULT_RECEIVER");
        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        // Query calendar events in the next 3 days.
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);

        cal.set(Calendar.DAY_OF_MONTH, 0);  // temp
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        long beginTime = cal.getTimeInMillis();

        String beginStr = cal.getTime().toString();

        cal.set(Calendar.DAY_OF_YEAR, 360); // temp!
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        long endTime = cal.getTimeInMillis();

        String endStr = cal.getTime().toString();

        Log.e(TAG, "Querying events from " + beginStr + " to " + endStr);
        List<Event> events = queryEvents(this, beginTime, endTime);

        // From https://proandroiddev.com/intentservice-and-resultreceiver-70de71e5e40a
        Bundle bundle = new Bundle();
        List<Appointments.Appointment> appts = new ArrayList<>();
        for (Event event : events) {
            float startHour = event.begin;
            float endHour = event.end;
            int color = event.color;
            appts.add(new Appointments.Appointment(startHour, endHour, color));
            /*
            final PutDataMapRequest putDataMapRequest = event.toPutDataMapRequest();
            if (mGoogleApiClient.isConnected()) {
                Wearable.DataApi.putDataItem(
                        mGoogleApiClient, putDataMapRequest.asPutDataRequest()).await();
            } else {
                Log.e(TAG, "Failed to send data item: " + putDataMapRequest
                        + " - Client disconnected from Google Play Services");
            }
            */
        }
        // bundle.putSerializable(Be24WatchFace.CalendarReceiver.APPOINTMENTS_LIST, (Serializable) appts);
        resultReceiver.send(0, bundle);
        mGoogleApiClient.disconnect();
    }

    private static String makeDataItemPath(long eventId, long beginTime) {
        return CAL_DATA_ITEM_PATH_PREFIX + eventId + "/" + beginTime;
    }

    private static List<Event> queryEvents(Context context, long beginTime, long endTime) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, beginTime);
        ContentUris.appendId(builder, endTime);
        Uri uri = builder.build();

        Cursor cursor = contentResolver.query(uri, INSTANCE_PROJECTION,
                null, null, null);

        Log.e(TAG, "Queried events " + uri.toString() + " gives count=" + cursor.getCount());
        try {
            int idIdx = cursor.getColumnIndex(CalendarContract.Instances._ID);
            int eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID);
            int titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE);
            int beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
            int endIdx = cursor.getColumnIndex(CalendarContract.Instances.END);
            int allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY);
            int descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION);
            int color = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR);

            List<Event> events = new ArrayList<Event>(cursor.getCount());
            while (cursor.moveToNext()) {
                Event event = new Event();
                event.id = cursor.getLong(idIdx);
                event.eventId = cursor.getLong(eventIdIdx);
                event.title = cursor.getString(titleIdx);
                event.begin = cursor.getLong(beginIdx);
                event.end = cursor.getLong(endIdx);
                event.allDay = cursor.getInt(allDayIdx) != 0;
                event.description = cursor.getString(descIdx);
                event.color = cursor.getInt(color);
                Log.e(TAG, "  Got event " + event.title);
                events.add(event);
            }
            return events;
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    private static Asset getDefaultProfile(Resources res) {
        Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.sunrise);
        return Asset.createFromBytes(toByteArray(bitmap));
    }

    private static Asset getProfilePicture(ContentResolver contentResolver, Context context,
                                           long contactId) {
        if (contactId != -1) {
            // Try to retrieve the profile picture for the given contact.
            Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            InputStream inputStream = Contacts.openContactPhotoInputStream(contentResolver,
                    contactUri, true /*preferHighres*/);

            if (null != inputStream) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        return Asset.createFromBytes(toByteArray(bitmap));
                    } else {
                        Log.e(TAG, "Cannot decode profile picture for contact " + contactId);
                    }
                } finally {
                    closeQuietly(inputStream);
                }
            }
        }
        // Use a default background image if the user has no profile picture or there was an error.
        return getDefaultProfile(context.getResources());
    }

    private static byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        closeQuietly(stream);
        return byteArray;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException while closing closeable.", e);
        }
    }

    private static class Event {

        public long id;
        public long eventId;
        public String title;
        public long begin;
        public long end;
        public boolean allDay;
        public String description;
        public int color;

        public PutDataMapRequest toPutDataMapRequest(){
            final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(
                    makeDataItemPath(eventId, begin));
            DataMap data = putDataMapRequest.getDataMap();
            data.putString(DATA_ITEM_URI, putDataMapRequest.getUri().toString());
            data.putLong(ID, id);
            data.putLong(EVENT_ID, eventId);
            data.putString(TITLE, title);
            data.putLong(BEGIN, begin);
            data.putLong(END, end);
            data.putBoolean(ALL_DAY, allDay);
            data.putString(DESCRIPTION, description);
            data.putInt(COLOR, color);

            return putDataMapRequest;
        }
    }
}
