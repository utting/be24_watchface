package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static au.edu.usc.utting.be24_watchface.Be24WatchFace.angle;
import static au.edu.usc.utting.be24_watchface.Be24WatchFace.sweep;

/**
 * Holds basic information about instances of calendar appointments for the current day.
 *
 * This is used to draw the times of the appointments (not the details), on the watchface.
 *
 * @author Mark Utting
 */
public class Appointments {
    /** The start time and length (in hours) of 'all day' appointments. */
    private static final float ALL_DAY_START = 7.0f;
    private static final float ALL_DAY_LENGTH = 10.0f;

    private final List<Appointment> mAppts;
    private final Paint mPaint;


    /** Basic information for today's appointments. */
    protected static class Appointment implements Serializable {
        private float startHour;  // e.g. 13.5f for 1:30pm.
        private float endHour;
        private boolean allDay;
        private int color;  // shows which calendar it is from


        public Appointment(float startHour, float endHour, boolean allDay, int color) {
            this.startHour = startHour;
            this.endHour = endHour;
            this.allDay = allDay;
            this.color = adjustColor(color);
        }

        /** Adjusts color to look more compatible with the watchface color scheme. */
        private int adjustColor(int color) {
            // display the calendar colour, but overlaid on background colour so it is not too jarring.
//        int r = Color.red(color);
//        int g = Color.green(color);
//        int b = Color.blue(color);
//        return Color.argb(127, r, g, b);


//        float[] hsv = new float[3];
//        Color.colorToHSV(color, hsv);
//        // hsv[1] = 0.5f;
//        hsv[2] = 0.5f;
//        return Color.HSVToColor(hsv);

//            return Be24WatchFace.BLUE72;  // same color for all calendars
            return color;
        }
    }

    public Appointments(List<Appointment> appts) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mAppts = appts;
    }

    /**
     * Draw all the appointments around the edge of the watchface.
     *
     * @param canvas
     * @param ambient if true, appointments are drawn as gray curves, else color.
     */
    public void drawAppointments(Canvas canvas, boolean ambient) {
        float penWidth = 10;
        mPaint.setAntiAlias(!ambient);
        mPaint.setStrokeWidth(penWidth);
        float left = penWidth / 2f;
        float top = penWidth / 2f;
        float width = canvas.getWidth() - penWidth / 2f;
        float height = canvas.getHeight() - penWidth / 2f;
        for (Appointment ap : mAppts) {
            float start;
            float sweep;
            if (ap.allDay) {
                // we default to the working day
                start = angle(ALL_DAY_START);
                sweep = ALL_DAY_LENGTH * 360f / 24f;
                mPaint.setColor(Color.DKGRAY);
            } else {
                start = angle(ap.startHour);
                sweep = sweep(ap.startHour, ap.endHour);
                if (ambient) {
                    mPaint.setColor(Color.GRAY);
                } else {
                    mPaint.setColor(ap.color);
                }
            }
            canvas.drawArc(left, top, width, height, start, sweep, false, mPaint);
        }
    }
}
