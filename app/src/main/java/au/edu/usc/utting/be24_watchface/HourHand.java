package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


import static au.edu.usc.utting.be24_watchface.Be24WatchFace.angle;

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

    protected float mCenterX;
    protected float mCenterY;

    /**
     *  Length of hour hand, from centre of watch to tip of hand.
     *  The whole hand will be longer than this if it has a back part.
     */
    protected float mHourHandLength;


    public HourHand() {

    }

    public HourHand(Paint[] normal, Paint[] ambient) {
        // customise our paint objects as necessary
        normal[Be24WatchFace.HAND1].setStrokeCap(Paint.Cap.ROUND);
        ambient[Be24WatchFace.HAND1].setStrokeCap(Paint.Cap.ROUND);
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

    /**
     * This is the main drawing method that draws the watch hand.
     *
     * One easy way of doing this is to always draw the hand to the east,
     * but to rotate the canvas by Be24WatchFace.angle(hours) before drawing,
     * (and restore the canvas afterwards of course).
     *
     * @param canvas
     * @param hours current time in hours: 0.00 .. 23.9999.
     * @param paint the set of paints to use (may be normal or ambient)
     * @param ambient true means draw for ambient mode.
     */
    public void drawHand(Canvas canvas, float hours, Paint[] paint, boolean ambient) {

        /* Save the canvas state before we can begin to rotate it. */
        canvas.save();
        final float hoursRotation = angle(hours);
        canvas.rotate(hoursRotation, mCenterX, mCenterY);

        canvas.drawLine(
                mCenterX - CENTER_GAP_AND_CIRCLE_RADIUS * 1.5f,
                mCenterY,
                mCenterX + mHourHandLength,
                mCenterY,
                paint[Be24WatchFace.HAND1]);

        canvas.drawCircle(
                mCenterX,
                mCenterY,
                CENTER_GAP_AND_CIRCLE_RADIUS,
                paint[Be24WatchFace.HAND2]);

        canvas.drawCircle(
                mCenterX,
                mCenterY,
                CENTER_GAP_AND_CIRCLE_RADIUS,
                paint[Be24WatchFace.HAND1]);

        /* Restore the canvas' original orientation. */
        canvas.restore();
    }

}
