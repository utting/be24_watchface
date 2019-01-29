package au.edu.usc.utting.be24_watchface;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Analog watch face with a single 24-hour hand.
 *
 * In ambient mode, the display is simplified to avoid burn-in.
 * On devices with low-bit ambient mode, the hands are drawn
 * without anti-aliasing in ambient mode.
 * The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class Be24WatchFace extends CanvasWatchFaceService {
    public static final String LOGO = "Be24";
    static final float LOGO_POS_Y = 0.65f;  // relative to mCenterY.
    static final int LOGO_FONT_SIZE = 24;
    static final int NUMBERS_FONT_SIZE = 36;
    static final float MAJOR_TICK_LENGTH = 0.10f;  // percentage of watch radius.
    static final float MINOR_TICK_LENGTH = 0.05f;  // percentage of watch radius.

    // The included photo to use as the background.
    // If this is set to zero, then BACKGROUND_COLOR will be used instead.
    private static final int BACKGROUND_RESOURCE = 0;
    // private static final int BACKGROUND_RESOURCE = R.drawable.bg;
    // private static final int BACKGROUND_RESOURCE = R.drawable.sunrise;

    // some more saturated colours from my sunrise photo.
    private static final int COLOR_DAWN_RED = Color.rgb(255, 120, 60);
    private static final int COLOR_DAWN_BLUE = Color.rgb(90, 145,255);
    // two related colours that are used, together with white and black.
    private static final int COLOR_DEEP_BLUE = Color.rgb(10, 40,100);
    private static final int COLOR_BRIGHT_ORANGE = Color.rgb(230, 130,30);

    // Colors from the 'Blue' Material Design palette:
    // See: https://designguidelines.withgoogle.com/wearos/style/color.html
    static final int BLUE20 = Color.parseColor("#071F33");
    static final int BLUE30 = Color.parseColor("#002A4D");
    static final int BLUE40 = Color.parseColor("#0E3F66");
    static final int BLUE55 = Color.parseColor("#14568C");
    static final int BLUE65 = Color.parseColor("#1766A6");
    static final int BLUE72 = Color.parseColor("#2196F3");
    static final int BLUE100 = Color.parseColor("#80C6FF");

    // amber is roughly complementary to the blue above.
    static final int AMBER20 = Color.parseColor("#331B10");
    static final int AMBER30 = Color.parseColor("#4D342A");
    static final int AMBER40 = Color.parseColor("#664D03");
    static final int AMBER55 = Color.parseColor("#8C6A04");
    static final int AMBER65 = Color.parseColor("#A67E05");
    static final int AMBER72 = Color.parseColor("#FFC107");
    static final int AMBER100 = Color.parseColor("#FFDF80");
       // or Color.parseColor("#fcc900"); // yellow sun

    // These names are used to index the mPaint array inside the engine.
    static final int BGND1 = 0; // main background color
    static final int BGND2 = 1; // 'night-time' background color
    static final int BGND3 = 2; // for fine lines on the background
    static final int LOGO1 = 3; // the logo (e.g. "Be24")
    static final int TICK1 = 4; // main tick marks
    static final int TICK2 = 5; // secondary tick marks
    static final int HAND1 = 6; // primary color for the outline of the hand
    static final int HAND2 = 7; // alt color for interior of the hand
    static final int HOURS = 8; // the hour numbers
    static final int APPTS = 9; // default paint for the appointments (color may vary)

    /*
     * Update rate in milliseconds for interactive mode.
     * We update once a minute, which is plenty for a 24-hour watch,
     * since the tip of the hand moves only about 1 pixel/minute on a 400x400 screen.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(60);

    private static final String TAG = Be24WatchFace.class.getSimpleName();


    private static final String[] INSTANCE_PROJECTION = {
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.CALENDAR_COLOR, // or use EVENT_COLOR?
    };

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    /**
     * Converts a given number of hours (0..24) into the corresponding angle.
     * Note: 00 hours is at the bottom (which is 90 degrees).
     *
     * @param time24 0..24.0 hours.
     * @return angle in degrees (always in the range 0..360.0)
     */
    public static float angle(float time24) {
        return (time24 * 360f / 24f + 90.0f) % 360f;
    }

    /**
     * Calculates the angle between two times (in hours).
     *
     * @param start24 start time 0..24.0 hours.
     * @param end24 end time 0..24.0 hours.
     * @return sweep angle in degrees (always in the range 0..360.0)
     */
    public static float sweep(float start24, float end24) {
        return ((24.0f + end24 - start24) * (360f / 24f)) % 360f;
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Be24WatchFace.Engine> mWeakReference;

        public EngineHandler(Be24WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Be24WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

//    public class CalendarReceiver extends ResultReceiver {
//        public static final String APPOINTMENTS_LIST = "result";
//
//        CalendarReceiver(Handler handler) {
//            super(handler);
//        }
//
//        //...
//        @Override
//        protected void onReceiveResult(int resultCode, Bundle resultData) {
//
//            if(resultCode == 0){
//                List<Appointments.Appointment> appts = (List<Appointments.Appointment>) resultData.getSerializable(APPOINTMENTS_LIST);
//                Log.e(TAG, "GOT Appointments: " + appts.size());
//                mAppointments = new Appointments(appts);
//            } else {
//                Log.e(TAG, "BAD RESULT FROM CalendarQueryService");
//                // TODO: mReceiver.onError((Exception) resultData.getSerializable(PARAM_EXCEPTION));
//            }
//        }
//    }

    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        // TODO: delete this, since once/minute is plenty?
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                updateAppointments(); // since timezone changes will move appointments.
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mLogoOffset;
        private float mMajorTickLength;  // length of major ticks around edge
        private float mMinorTickLength;  // length of minor ticks around edge

        private HourHand mHourHand;
        private float mHandLength;

        /** Each tap on the centre of the watch changes this hand style. */
        private int mHourHandStyle = 0;  // 0 = simple hour hand.

        /**
         * mPaint points to the current paints/colours we are using.
         * This will be aliased to either mPaintNormal to mPaintAmbient,
         * depending upon what mode we are in.
         */
        private Paint[] mPaint = null;
        private Paint[] mPaintNormal = new Paint[APPTS + 1];
        private Paint[] mPaintAmbient = new Paint[APPTS + 1];

        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        /** Angle in degrees (0=east) where sunset happens. */
        private float mSunsetAngle;
        /** Sweep angle of night duration. */
        private float mNightAngle;

        private Appointments mAppointments = null;
        /** the hour (0..23) that appointments were last updated. */
        private int mUpdateHour = -1;

        private double latitude = -26.84;
        private double longitude = 152.96;
        private String locMsg = "default";
        private FusedLocationProviderClient mFusedLocationClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            /* get location.
             * See https://developer.android.com/training/location/retrieve-current
             */
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

            // We ask for last location just once, when watch face starts.
            // TODO: repeat it each week or so.
            OnSuccessListener listen = new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        locMsg = String.format("%.4f,%.4f", latitude, longitude);
                        Log.d(TAG, "got lat,long=" + locMsg);

                        // update sunrise/set calculations
                        long now = System.currentTimeMillis();
                        mCalendar.setTimeInMillis(now);
                        SunCalculator sun = new SunCalculator();
                        sun.calculateSunRiseSet(latitude, longitude, mCalendar);
                        float sunriseHour = sun.getSunrise();
                        float sunsetHour = sun.getSunset();
                        mSunsetAngle = angle(sunsetHour);
                        mNightAngle = sweep(sunsetHour, sunriseHour);
                        Log.d(TAG, "new sunrise=" + sunriseHour + " sunset=" + sunsetHour);
                    }
                }
            };
            try {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(listen);
            } catch (SecurityException ex) {
                Log.d(TAG, "onCreate() location permissions denied.");
            }

            setWatchFaceStyle(new WatchFaceStyle.Builder(Be24WatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            SunCalculator sun = new SunCalculator();
            sun.calculateSunRiseSet(latitude, longitude, mCalendar);
            float sunriseHour = sun.getSunrise();
            float sunsetHour = sun.getSunset();
            mSunsetAngle = angle(sunsetHour);
            mNightAngle = sweep(sunsetHour, sunriseHour);
            Log.d(TAG, "onCreate() sunrise=" + sunriseHour + " sunset=" + sunsetHour);

            initializeBackground();
            initializeColors();
            initializeWatchFace();
            updateAppointments();
        }

        private void initializeColors() {
            // set up most of our paints and colors.
            mPaint = mPaintNormal;
            for (int i = 0; i < mPaint.length; i++) {
                mPaintNormal[i] = new Paint();
                mPaintNormal[i].setAntiAlias(true);
            }
            mPaintNormal[BGND1].setColor(BLUE30);
            mPaintNormal[BGND2].setColor(BLUE20);
            mPaintNormal[BGND3].setColor(AMBER55);
            mPaintNormal[LOGO1].setColor(AMBER72);
            mPaintNormal[TICK1].setColor(BLUE100);
            mPaintNormal[TICK2].setColor(AMBER72);
            mPaintNormal[HAND1].setColor(BLUE100);
            mPaintNormal[HAND2].setColor(AMBER65);
            mPaintNormal[HOURS].setColor(AMBER72);
            mPaintNormal[APPTS].setColor(BLUE65);  // just a default

            // Customise some paints/fonts.
            // these paints are used for drawing lines (the others default to FILL)
            mPaintNormal[BGND3].setStyle(Paint.Style.STROKE);
            mPaintNormal[BGND3].setStrokeWidth(1);

            mPaintNormal[TICK1].setStyle(Paint.Style.STROKE);
            mPaintNormal[TICK1].setStrokeWidth(5);

            mPaintNormal[TICK2].setStyle(Paint.Style.STROKE);
            mPaintNormal[TICK2].setStrokeWidth(2);

            mPaintNormal[HAND1].setStyle(Paint.Style.STROKE);
            mPaintNormal[HAND1].setStrokeWidth(4f);

            // font sizes
            mPaintNormal[HOURS].setTextSize(NUMBERS_FONT_SIZE);
            mPaintNormal[LOGO1].setTextSize(LOGO_FONT_SIZE);

            // copy those paint settings into the paints for ambient mode.
            for (int i = 0; i < mPaint.length; i++) {
                mPaintAmbient[i] = new Paint(mPaintNormal[i]);
                mPaintAmbient[i].setAntiAlias(false);
            }
            // then override the colors to shades of gray.
            mPaintAmbient[BGND1].setColor(Color.BLACK);
            mPaintAmbient[BGND2].setColor(Color.BLACK);
            mPaintAmbient[BGND3].setColor(Color.GRAY); // not shown in ambient mode
            mPaintAmbient[LOGO1].setColor(Color.GRAY);
            mPaintAmbient[TICK1].setColor(Color.WHITE);
            mPaintAmbient[TICK2].setColor(Color.GRAY); // not shown in ambient mode
            mPaintAmbient[HAND1].setColor(Color.WHITE);
            mPaintAmbient[HAND2].setColor(Color.DKGRAY);
            mPaintAmbient[HOURS].setColor(Color.LTGRAY);
            mPaintAmbient[APPTS].setColor(Color.LTGRAY);  // just a default
            mPaintAmbient[HAND1].setAntiAlias(true); // even in ambient mode, this needs anti-aliasing.

            // Background shadow can be useful if we use a background image.
            // mPaintNormal[HAND1].setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }

        private void initializeBackground() {

            if (BACKGROUND_RESOURCE != 0) {
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), BACKGROUND_RESOURCE);

                /* Extracts colors from background image to improve watchface style. */
                Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        if (palette != null) {
                            mPaint[HAND1].setColor(palette.getLightVibrantColor(Color.WHITE));
                            mPaint[HAND2].setColor(palette.getVibrantColor(Color.BLUE));
                            Log.d(TAG, palette.toString());
                            updateWatchStyle();
                        }
                    }
                });
            }
        }

        /**
         * Sets up the basic data structures (paints etc) for the watchface.
         * Note that the color of each paint is set initially here, but also dynamically later.
         */
        private void initializeWatchFace() {
            mHourHand = new HourHand(mPaintNormal, mPaintAmbient);
            // just in case the size is already known...
            mHourHand.setGeometry(mCenterX, mCenterY, mHandLength);
            mLogoOffset = mPaint[LOGO1].measureText(LOGO) / 2f;
        }

        /** Set the hour hand implementation that we want. */
        public void setHourHand(HourHand hand) {
            mHourHand = hand;
            hand.setGeometry(mCenterX, mCenterY, mHandLength);
        }

        /**
         * Set the hour hand style.
         * @param style any positive integer.
         *              This is mapped modulo onto the number of known styles.
         */
        private void setHourHandStyle(int style) {
            switch (style % 3) {
                case 0:
                    setHourHand(new HourHand(mPaintNormal, mPaintAmbient));
                    break;

                case 1:
                    setHourHand(new HourHandOutlineTriangle(mPaintNormal, mPaintAmbient));
                    break;

                case 2:
                    setHourHand(new HourHand3DTriangle(mPaintNormal, mPaintAmbient));
                    break;
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            Log.d(TAG, "set lowBitAmbient=" + mLowBitAmbient
                    + " burnInProtection=" + mBurnInProtection);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchStyle() {
            if (mAmbient) {
                mPaint = mPaintAmbient;
            } else {
                mPaint = mPaintNormal;
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                Log.d(TAG, "setting muteMode = " + mMuteMode);
                for (int i = 0; i < mPaintNormal.length; i++) {
                    mPaintNormal[i].setAlpha(inMuteMode ? 100 : 255);
                    mPaintAmbient[i].setAlpha(inMuteMode ? 100 : 255);
                }
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of hands based on watch screen size.
             */
            mMajorTickLength = mCenterX * MAJOR_TICK_LENGTH;
            mMinorTickLength = mCenterX * MINOR_TICK_LENGTH;

            // leave a couple of pixels clearance, to avoid any friction.
            mHandLength = mCenterX - mCenterX * Be24WatchFace.MAJOR_TICK_LENGTH - 2f;
            mHourHand.setGeometry(mCenterX, mCenterY, mHandLength);

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            if (mBackgroundBitmap != null) {
                float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

                mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        (int) (mBackgroundBitmap.getWidth() * scale),
                        (int) (mBackgroundBitmap.getHeight() * scale), true);

                /*
                 * Create a gray version of the image only if it will look nice on the device in
                 * ambient mode. That means we don't want devices that support burn-in
                 * protection (slight movements in pixels, not great for images going all the way to
                 * edges) and low ambient mode (degrades image quality).
                 *
                 * Also, if your watch face will know about all images ahead of time (users aren't
                 * selecting their own photos for the watch face), it will be more
                 * efficient to create a black/white version (png, etc.) and load that when you need it.
                 */
                if (!mBurnInProtection && !mLowBitAmbient) {
                    initGrayBackgroundBitmap();
                }
            }
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    Log.i(TAG, "TAP_TYPE_TOUCH");
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    Log.i(TAG, "TAP_TYPE_CANCEL");
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    Log.i(TAG, "TAP_TYPE_TAP");
                    float xdiff = x - mCenterX;
                    float ydiff = y - mCenterY;
                    if (xdiff * xdiff + ydiff * ydiff < 20 * 20) {
                        // they tapped the centre, so we change the style of the hour hand.
                        mHourHandStyle++;
                        setHourHandStyle(mHourHandStyle);
                    } else if (y < mCenterY) {
                        // they tapped the top, so we update appointments.
                        updateAppointments();
                        // Intent intent = new Intent(getApplicationContext(), CalendarQueryService.class);
                        // TODO: pass CalendarReceiver listener to intent (or CalendarQueryService instance!)
                        // intent.putExtra("resultreceiver", new CalendarReceiver(intent));
                        // startService(intent);
                        // new CalendarViewer().showCalendars(getApplicationContext());
                    } else {
                        // old stuff.
                        new CalendarViewer().showDay(getApplicationContext());
                    }
                    break;
            }
            invalidate();
        }

        /**
         * Updates the appointments by reading the calendar.
         *
         * If READ_CALENDAR permissions are not turned on, no appointments are shown.
         *
         * TODO: call this automatically, when day or timezone changes?
         *
         * NOTE: WearableCalendarContract does not sync all data now.
         * See issue: https://issuetracker.google.com/issues/38476499
         */
        private void updateAppointments() {
            mUpdateHour = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "No READ_CALENDAR permission.");
                mAppointments = null;
            } else {
                // String[] perms = {Manifest.permission.READ_CALENDAR};
                // TODO: ?? ActivityCompat.requestPermissions(this, perms, 0);
                Calendar cal = Calendar.getInstance();
                long now = System.currentTimeMillis();
                cal.setTimeInMillis(now);

                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 1); // so we do not include all-day events from yesterday?
                cal.set(Calendar.SECOND, 0);
                long beginTime = cal.getTimeInMillis();

                String beginStr = cal.getTime().toString();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                long endTime = cal.getTimeInMillis();

                String endStr = cal.getTime().toString();

                List<Appointments.Appointment> appts = new ArrayList<>();

                ContentResolver contentResolver = getContentResolver();
                Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, beginTime);
                ContentUris.appendId(builder, endTime);
                Uri uri = builder.build();

                try (Cursor cursor = contentResolver.query(uri, INSTANCE_PROJECTION,
                        null, null, null)) {
                    if (cursor == null) {
                        mAppointments = null;
                        return;
                    }
                    long then = System.currentTimeMillis();

                    Log.e(TAG, "Query calendar events " + beginStr + ".." + endStr +
                            " timezone=" + cal.getTimeZone() +
                            " gives count=" + cursor.getCount() + " in " + (then - now) + "ms.");
                    int idIdx = cursor.getColumnIndex(CalendarContract.Instances._ID);
                    int eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID);
                    int titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE);
                    int beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
                    int endIdx = cursor.getColumnIndex(CalendarContract.Instances.END);
                    int allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY);
                    int descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION);
                    int colorIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR);

                    float ONE_HOUR = 1000f * 60f * 60f; // milliseconds in one hour
                    while (cursor.moveToNext()) {
                        // appt.id = cursor.getLong(idIdx);
                        // appt.eventId = cursor.getLong(eventIdIdx);
                        String title = cursor.getString(titleIdx);
                        float startHour = (cursor.getLong(beginIdx) - beginTime) / ONE_HOUR;
                        float endHour = (cursor.getLong(endIdx) - beginTime) / ONE_HOUR;
                        boolean allDay = cursor.getInt(allDayIdx) != 0;
                        // appt.description = cursor.getString(descIdx);
                        int color = cursor.getInt(colorIdx);
                        Log.e(TAG, "  Got event allDay=" + allDay + " " + startHour + " .. " + endHour + ": " + title);
                        // put all-day appointments at the beginning, so they are underneath the others.
                        appts.add(allDay ? 0 : appts.size(),
                                new Appointments.Appointment(startHour, endHour, allDay, color));
                    }
                    mAppointments = new Appointments(appts);
                }
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d(TAG, "onDraw with mAmbient=" + mAmbient);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawWatchFace(canvas);

            final float hours = mCalendar.get(Calendar.HOUR_OF_DAY) + mCalendar.get(Calendar.MINUTE) / 60f;
            mHourHand.drawHand(canvas, hours, mPaint, mAmbient);

            if (mUpdateHour != mCalendar.get(Calendar.HOUR_OF_DAY)) {
                // once an hour (or when user taps) we try to update appointments.
                updateAppointments();
            }
            if (mAppointments != null) {
                mAppointments.drawAppointments(canvas, mAmbient);
            }
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection || mGrayBackgroundBitmap == null)) {
                canvas.drawColor(Color.BLACK);
            } else if (mBackgroundBitmap != null) {
                if (mAmbient) {
                    canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mPaint[BGND1]);
                } else {
                    canvas.drawBitmap(mBackgroundBitmap, 0, 0, mPaint[BGND1]);
                }
            } else {
                canvas.drawPaint(mPaint[BGND1]);
            }

            // the sunrise-sunset pie
            if (!mAmbient) {
                float width = mCenterX * 2f;
                float height = mCenterY * 2f;
                canvas.drawArc(0f, 0f, width, height, mSunsetAngle, mNightAngle, true, mPaint[BGND2]);
                canvas.drawArc(0f, 0f, width, height, mSunsetAngle, mNightAngle, true, mPaint[BGND3]);
            }
            canvas.drawText(LOGO, mCenterX - mLogoOffset, mCenterY * LOGO_POS_Y, mPaint[LOGO1]);

            // DEBUG
            // canvas.drawText(locMsg, mCenterX - 100, mCenterY + 100, mPaint[LOGO1]);
        }

        private void drawWatchFace(Canvas canvas) {

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */

            /*
            // Draw the hourly tick marks around the outside.
            // Each mark is drawn down the bottom of the watchface, starting from 0.
            float bottom = mCenterY * 2f;  // Y position of bottom of whole watchface
            float top = bottom - mMajorTickLength; // top of all the ticks
            for (int hh = 0; hh < 24; hh++) {
                final float len = (hh % 1 == 0) ? mMajorTickLength : mMinorTickLength;
                final Paint paint = (!mAmbient && hh % 1 == 0) ? mMajorPaint : mMinorPaint;
                canvas.drawLine(mCenterX, bottom, mCenterX, top, paint);
                // now draw the hour number
                if (hh % 3 == 0) {
                    String numStr = String.valueOf(hh);
                    float textBottom = top - 4;
                    float textLeft = mNumbersPaint.measureText(numStr) / 2f;
                    canvas.drawText(numStr, mCenterX - textLeft, textBottom, mNumbersPaint);
                }
                canvas.rotate(15.0f, mCenterX, mCenterY);
            }
            */

            /* Code that does not rotate canvas, so all numbers are upright. */
            float innerTickRadius = mCenterX - mMajorTickLength;
            float textRadius = innerTickRadius - NUMBERS_FONT_SIZE * 0.7f;
            float outerTickRadius = mCenterX;
            double tickAngle = Math.PI * 2 / (24 * 4);
            for (int tickIndex = 0; tickIndex < 24 * 4; tickIndex++) {
                float tickRot = (float) (tickIndex * tickAngle);
                boolean draw = false;
                float outer = 0f;
                Paint paint = null;
                if (tickIndex % 4 == 0) {
                    // draw a major tick (for each hour)
                    draw = true;
                    outer = outerTickRadius;
                    paint = mPaint[TICK1];
                } else if (!mAmbient) {
                    // draw a minor tick (for each 1/4 hour)
                    draw = true;
                    outer = innerTickRadius + mMinorTickLength;
                    paint = mPaint[TICK2];
                }
                if (draw) {
                    float innerX = (float) -Math.sin(tickRot) * innerTickRadius;
                    float innerY = (float) Math.cos(tickRot) * innerTickRadius;
                    float outerX = (float) -Math.sin(tickRot) * outer;
                    float outerY = (float) Math.cos(tickRot) * outer;
                    canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                            mCenterX + outerX, mCenterY + outerY, paint);
                    // now draw the hour number
                    if (tickIndex % (3 * 4) == 0) {
                        int hour = tickIndex / 4;
                        String numStr = String.valueOf(hour);
                        // calculate the centre of the text position
                        float textX = (float) -Math.sin(tickRot) * textRadius;
                        float textY = (float) Math.cos(tickRot) * textRadius;
                        System.out.printf("%d (%.1f,%.1f) ", tickIndex, textX, textY);
                        float textLeft = mCenterX + textX - mPaint[HOURS].measureText(numStr) / 2f;
                        float textBottom = mCenterY + textY + NUMBERS_FONT_SIZE / 2f;
                        canvas.drawText(numStr, textLeft, textBottom, mPaint[HOURS]);
                    }
                }
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            Be24WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            Be24WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
