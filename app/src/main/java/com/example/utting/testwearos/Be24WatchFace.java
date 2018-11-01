package com.example.utting.testwearos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
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

    /*
     * Updates rate in milliseconds for interactive mode.
     * We update once a minute, which is plenty for a 24-hour watch.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(60);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

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

    private class Engine extends CanvasWatchFaceService.Engine {
        public static final String LOGO = "Be24";
        private static final float LOGO_POS_Y = 0.75f;  // relative to mCenterY.
        private static final float LOGO_FONT_SIZE = 24;
        private static final float NUMBERS_FONT_SIZE = 18;
        private static final float HOUR_STROKE_WIDTH = 4f;
        private static final float LINE_STROKE_WIDTH = 2f;
        private static final float MAJOR_TICK_STROKE_WIDTH = 5f;
        private static final float MINOR_TICK_STROKE_WIDTH = 2f;
        private static final float MAJOR_TICK_LENGTH = 0.10f;  // percentage of watch radius.
        private static final float MINOR_TICK_LENGTH = 0.05f;  // percentage of watch radius.

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 4;
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
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
        /**
         *  Length of hour hand, from centre of watch to tip of hand.
         *  The whole hand will be longer than this if it has a back part.
         */
        private float mHourHandLength;

        /* Colors for the hour hand based on photo loaded. */
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;

        private Paint mHandPaint;
        private Paint mHandInnerPaint;
        private Paint mLinePaint;    // for subtle lines
        private Paint mMajorPaint;   // major tick marks
        private Paint mMinorPaint;   // minor tick marks
        private Paint mNumbersPaint; // hour numbers
        private Paint mLogoPaint;    // text logos.
        private Paint mNightPaint;   // for the night area of the background.

        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private Path mHandPath;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(Be24WatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            initializeBackground();
            initializeWatchFace();
        }

        private void initializeBackground() {
            mNightPaint = new Paint();
            mNightPaint.setColor(Color.BLACK);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sunrise);

            /* Extracts colors from background image to improve watchface style. */
            Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette != null) {
                        mWatchHandHighlightColor = palette.getVibrantColor(Color.LTGRAY);
                        mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                        mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                        // Log.d("onGenerated", palette.toString());
                        updateWatchHandStyle();
                    }
                }
            });
        }

        /**
         * Sets up the basic data structures (paints etc) for the watchface.
         * Note that the color of each paint is set dynamically later, so not here.
         */
        private void initializeWatchFace() {
            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE;
            mWatchHandHighlightColor = Color.RED;
            mWatchHandShadowColor = Color.BLACK;

            mHandPaint = new Paint();
            mHandPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mHandInnerPaint = new Paint();
            mHandInnerPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHandInnerPaint.setStrokeCap(Paint.Cap.ROUND);

            mLinePaint = new Paint();
            mLinePaint.setStrokeWidth(LINE_STROKE_WIDTH);
            mLinePaint.setStrokeCap(Paint.Cap.ROUND);

            mMajorPaint = new Paint();
            mMajorPaint.setStrokeWidth(MAJOR_TICK_STROKE_WIDTH);
            mMajorPaint.setStyle(Paint.Style.STROKE);

            mMinorPaint = new Paint();
            mMinorPaint.setStrokeWidth(MINOR_TICK_STROKE_WIDTH);
            mMinorPaint.setStyle(Paint.Style.STROKE);

            mNumbersPaint = new Paint();
            mNumbersPaint.setStrokeWidth(LINE_STROKE_WIDTH);
            mNumbersPaint.setAntiAlias(true);
            mNumbersPaint.setStrokeCap(Paint.Cap.BUTT);
            mNumbersPaint.setStyle(Paint.Style.FILL);
            mNumbersPaint.setTypeface(Typeface.SANS_SERIF);
            mNumbersPaint.setTextSize(NUMBERS_FONT_SIZE);

            mLogoPaint = new Paint();
            mLogoPaint.setStrokeWidth(LINE_STROKE_WIDTH);
            mLogoPaint.setAntiAlias(true);
            mLogoPaint.setStrokeCap(Paint.Cap.BUTT);
            mLogoPaint.setStyle(Paint.Style.FILL);
            mLogoPaint.setTypeface(Typeface.SANS_SERIF);
            mLogoPaint.setTextSize(LOGO_FONT_SIZE);

            mLogoOffset = mLogoPaint.measureText(LOGO) / 2f;
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

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHandPaint.setColor(Color.WHITE);
                mHandInnerPaint.setColor(Color.WHITE);
                mLinePaint.setColor(Color.WHITE);
                mMajorPaint.setColor(Color.WHITE);
                mMinorPaint.setColor(Color.WHITE);
                mNumbersPaint.setColor(Color.WHITE);
                mLogoPaint.setColor(Color.WHITE);

                mHandPaint.setAntiAlias(false);
                mHandInnerPaint.setAntiAlias(false);
                mLinePaint.setAntiAlias(false);
                mMajorPaint.setAntiAlias(false);
                mMinorPaint.setAntiAlias(false);
                mNumbersPaint.setAntiAlias(false);
                mLogoPaint.setAntiAlias(false);

                mHandPaint.clearShadowLayer();
                mHandInnerPaint.clearShadowLayer();
                mLinePaint.clearShadowLayer();
                mMajorPaint.clearShadowLayer();
                mMinorPaint.clearShadowLayer();
                mNumbersPaint.clearShadowLayer();
                mLogoPaint.clearShadowLayer();
            } else {
                mHandPaint.setColor(mWatchHandColor);
                mHandInnerPaint.setColor(mWatchHandHighlightColor);
                mLinePaint.setColor(mWatchHandHighlightColor);
                mMajorPaint.setColor(mWatchHandColor);
                mMinorPaint.setColor(mWatchHandHighlightColor);
                mNumbersPaint.setColor(mWatchHandHighlightColor);
                mLogoPaint.setColor(mWatchHandHighlightColor);

                mHandPaint.setAntiAlias(true);
                mHandInnerPaint.setAntiAlias(true);
                mLinePaint.setAntiAlias(true);
                mMajorPaint.setAntiAlias(true);
                mMinorPaint.setAntiAlias(true);
                mNumbersPaint.setAntiAlias(true);
                mLogoPaint.setAntiAlias(true);

                mHandPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mHandInnerPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mLinePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMajorPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinorPaint.setShadowLayer(SHADOW_RADIUS / 2f, 0, 0, mWatchHandShadowColor);
                mNumbersPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mLogoPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHandPaint.setAlpha(inMuteMode ? 100 : 255);
                mHandInnerPaint.setAlpha(inMuteMode ? 100 : 255);
                mLinePaint.setAlpha(inMuteMode ? 80 : 255);
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
            // leave one pixel clearance, to avoid any friction.
            mHourHandLength = mCenterX - mMajorTickLength - 2f;

            /* Scale loaded background image (more efficient) if surface dimensions change. */
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
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    String time = mCalendar.getTime().toString();
                    Toast.makeText(getApplicationContext(), R.string.message + time, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawWatchFace(canvas);

            /*
             * These calculations reflect that one hour = 360/24 = 15 degrees.
             */
            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 15) + hourHandOffset;

            /* Save the canvas state before we can begin to rotate it. */
            canvas.save();
            canvas.rotate(hoursRotation, mCenterX, mCenterY);

            drawSimpleHand(canvas, hoursRotation);

            /* Restore the canvas' original orientation. */
            canvas.restore();
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mNightPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mNightPaint);
            }


            // TODO: integrate this sunrise/set code:
            /*
            // the sunrise-sunset pie
            float nightAngle = 360f - (mSunSet - mSunRise);
            if (!mAmbient) {
                canvas.drawArc(0f, 0f, mWidth, mHeight, mSunSet, nightAngle, true, mNightPaint);
                canvas.drawArc(0f, 0f, mWidth, mHeight, mSunSet, nightAngle, true, mLinePaint);
            }
            */
            canvas.drawText(LOGO, mCenterX - mLogoOffset, mCenterY * LOGO_POS_Y, mLogoPaint);

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
            float textRadius = mCenterX - mMajorTickLength - NUMBERS_FONT_SIZE;
            float outerTickRadius = mCenterX;
            double tickAngle = Math.PI * 2 / (24 * 4);
            for (int tickIndex = 0; tickIndex < 24 * 4; tickIndex++) {
                float tickRot = (float) (tickIndex * tickAngle);
                final float outer;
                final Paint paint;
                if (tickIndex % 4 == 0) {
                    // draw a major tick (for each hour)
                    outer = outerTickRadius;
                    paint = mMajorPaint;
                } else {
                    // draw a minor tick (for each 1/4 hour)
                    if (mAmbient) {
                        continue;
                    }
                    outer = innerTickRadius + mMinorTickLength;
                    paint = mMinorPaint;
                }
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
                    float textBottom = textY + NUMBERS_FONT_SIZE / 2f;
                    float textLeft = textX - mNumbersPaint.measureText(numStr) / 2f;
                    canvas.drawText(numStr, mCenterX + textLeft, mCenterY + textBottom, mNumbersPaint);
                }
            }
        }

        private void drawSimpleHand(Canvas canvas, float hoursRotation) {
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHandPaint);

            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mMajorPaint);
        }

        private void initTriangleHand() {
            // create hand path
            float startX = mCenterX - 45;
            float stopX = mCenterX + mHourHandLength;
            float offsetY = 8;
            mHandPath = new Path();
            mHandPath.setFillType(Path.FillType.EVEN_ODD);
            mHandPath.moveTo(startX, mCenterY - offsetY);
            mHandPath.lineTo(stopX, mCenterY + 0);
            mHandPath.lineTo(startX, mCenterY + offsetY);
            mHandPath.lineTo(startX, mCenterY - offsetY);
            mHandPath.close();
        }

        private void drawTriangleHand(Canvas canvas, float hoursRotation) {
            // triangular hand with darker centre
            canvas.drawPath(mHandPath, mHandInnerPaint);
            canvas.drawPath(mHandPath, mHandPaint);
            // and a circle around the centre
            canvas.drawCircle(mCenterX, mCenterY, 15, mHandInnerPaint);
            canvas.drawCircle(mCenterX, mCenterY, 15, mHandPaint);
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
