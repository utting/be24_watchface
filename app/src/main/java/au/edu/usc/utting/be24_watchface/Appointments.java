package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static au.edu.usc.utting.be24_watchface.Be24WatchFace.angle;

/**
 * Holds basic information about instances of calendar appointments for the current day.
 *
 * This is used to draw the times of the appointments (not the details), on the watchface.
 *
 * @author Mark Utting
 */
public class Appointments {

    private List<Appointment> mAppts = new ArrayList<>();
    private Paint mPaint;


    /** Basic information for today's appointments. */
    protected class Appointment {
        private float startHour;  // e.g. 13.5f for 1:30pm.
        private float endHour;
        private int color;  // shows which calendar it is from


        public Appointment(float startHour, float endHour, int color) {
            this.startHour = startHour;
            this.endHour = endHour;
            this.color = color;
        }
    }


    public Appointments() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        addDummyAppointments();
    }

    public void addDummyAppointments() {
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);
        mAppts.add(new Appointment(1.0f, 3.0f, Color.YELLOW));
        mAppts.add(new Appointment(9.0f, 10.0f, Color.YELLOW));
        mAppts.add(new Appointment(10.0f, 11.5f, Color.GREEN));
        mAppts.add(new Appointment(18.0f, 20.5f, Color.BLUE));
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
            float start = angle(ap.startHour);
            float sweep = angle(ap.endHour) - start;
            mPaint.setColor(ambient ? Color.DKGRAY : ap.color);
            canvas.drawArc(left, top, width, height, start, sweep, false, mPaint);
        }
    }
}
