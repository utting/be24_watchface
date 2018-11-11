package au.edu.usc.utting.be24_watchface;

/**
 * Constants used in companion app.
 *
 *  This code is from: https://www.programcreek.com/java-api-examples/?code=mauimauer/AndroidWearable-Samples/AndroidWearable-Samples-master/AgendaData/Application/src/main/java/com/example/android/wearable/agendadata/CalendarQueryService.java
 */
public final class Constants {
    private Constants() {
    }

    public static final String TAG = "AgendaDataSample";

    public static final String CAL_DATA_ITEM_PATH_PREFIX = "/event";
    // Timeout for making a connection to GoogleApiClient (in milliseconds).
    public static final long CONNECTION_TIME_OUT_MS = 100;

    public static final String EVENT_ID = "event_id";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String BEGIN = "begin";
    public static final String END = "end";
    public static final String DATA_ITEM_URI = "data_item_uri";
    public static final String ALL_DAY = "all_day";
    public static final String COLOR = "color";
}
