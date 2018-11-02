package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Calendar;

/**
 * Superclass for drawing various kinds of hour hands.
 *
 * This default implementation draws a very simple narrow line for the hand.
 * It also has a little circle at the rotation point, filled with the highlight color.
 *
 * Note that setGeometry(...) must be called before the hand is drawn.
 */
class HourHand {

    static final float HOUR_STROKE_WIDTH = 4f;
    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = HOUR_STROKE_WIDTH * 2;

    protected final int mWatchHandColor;
    protected final int mWatchHandHighlightColor;
    protected final int mWatchHandShadowColor;

    protected Paint mHandPaint;
    protected Paint mHandInnerPaint;

    protected float mCenterX;
    protected float mCenterY;

    /**
     *  Length of hour hand, from centre of watch to tip of hand.
     *  The whole hand will be longer than this if it has a back part.
     */
    protected float mHourHandLength;


    public HourHand(int mainColor, int highlightColor, int shadowColor) {
        mWatchHandColor = mainColor;
        mWatchHandHighlightColor = highlightColor;
        mWatchHandShadowColor = shadowColor;

        mHandPaint = new Paint();
        mHandPaint.setColor(mWatchHandColor);
        mHandPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
        mHandPaint.setStrokeCap(Paint.Cap.ROUND);
        mHandPaint.setAntiAlias(true);

        mHandInnerPaint = new Paint();
        mHandInnerPaint.setColor(mWatchHandHighlightColor);
        mHandInnerPaint.setStrokeWidth(0);
        mHandInnerPaint.setStyle(Paint.Style.FILL);
        mHandInnerPaint.setStrokeCap(Paint.Cap.ROUND);
        mHandInnerPaint.setAntiAlias(true);
    }

    public int getColor() {
        return mWatchHandColor;
    }

    public int getHighlightColor() {
        return mWatchHandHighlightColor;
    }

    public int getShadowColor() {
        return mWatchHandShadowColor;
    }

    /**
     * Sets the size and rotation position of the hand.
     *
     * The tip of the hand should proscribe a circle of the given radius around (centerX, centerY).
     *
     * @param centerX
     * @param centerY
     * @param radius requested length of the hand.
     */
    public void setGeometry(float centerX, float centerY, float radius) {
        mCenterX = centerX;
        mCenterY = centerY;
        mHourHandLength = radius;
    }

    public void updateWatchHandStyle(boolean mAmbient) {
        if (mAmbient) {
            mHandPaint.setColor(Color.WHITE);
            mHandInnerPaint.setColor(Color.WHITE);

            mHandPaint.setAntiAlias(false);
            mHandInnerPaint.setAntiAlias(false);

            mHandPaint.clearShadowLayer();
            mHandInnerPaint.clearShadowLayer();

        } else {
            mHandPaint.setColor(mWatchHandColor);
            mHandInnerPaint.setColor(mWatchHandHighlightColor);

            mHandPaint.setAntiAlias(true);
            mHandInnerPaint.setAntiAlias(true);

            mHandPaint.setShadowLayer(Be24WatchFace.SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            mHandInnerPaint.setShadowLayer(Be24WatchFace.SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }
    }

    /**
     * This is typically just used during interruption filters.
     * @param alpha 0..255.
     */
    public void setAlpha(int alpha) {
        mHandPaint.setAlpha(alpha);
        mHandInnerPaint.setAlpha(alpha);
    }

    /**
     * This is the main drawing method that draws the watch hand.
     *
     * One easy way of doing this is to always draw the hand to the east,
     * but to rotate the canvas by Be24WatchFace.angle(hours) before drawing,
     * (and restore the canvas afterwards of course).
     *
     * @param canvas
     * @param hours  current time in hours: 0.00 .. 23.9999.
     */
    public void drawHand(Canvas canvas, float hours) {

        /* Save the canvas state before we can begin to rotate it. */
        canvas.save();
        final float hoursRotation = Be24WatchFace.angle(hours);
        canvas.rotate(hoursRotation, mCenterX, mCenterY);

        canvas.drawLine(
                mCenterX - CENTER_GAP_AND_CIRCLE_RADIUS * 1.5f,
                mCenterY,
                mCenterX + mHourHandLength,
                mCenterY,
                mHandPaint);

        canvas.drawCircle(
                mCenterX,
                mCenterY,
                CENTER_GAP_AND_CIRCLE_RADIUS,
                mHandPaint);

        canvas.drawCircle(
                mCenterX,
                mCenterY,
                CENTER_GAP_AND_CIRCLE_RADIUS / 2f,
                mHandInnerPaint);

        /* Restore the canvas' original orientation. */
        canvas.restore();
    }

}
