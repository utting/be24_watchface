package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Path;

/**
 * Draws a triangular hour hand with an outline, filled with a highlight color.
 */
public class HourHandOutlineTriangle extends HourHand {
    /** Determines how wide the base of the triangle is. */
    private static final float TRIANGLE_WIDTH = 12;

    private Path mHandPath;

    public HourHandOutlineTriangle(int mainColor, int highlightColor, int shadowColor) {
        super(mainColor, highlightColor, shadowColor);
    }

    @Override
    public void setGeometry(float width, float height, float handLen) {
        super.setGeometry(width, height, handLen);

        // create hand path
        float startX = mCenterX - TRIANGLE_WIDTH * 3f;
        float stopX = mCenterX + mHourHandLength;
        float offsetY = TRIANGLE_WIDTH / 2f;
        mHandPath = new Path();
        mHandPath.setFillType(Path.FillType.EVEN_ODD);
        mHandPath.moveTo(startX, mCenterY - offsetY);
        // we make the tip as sharp as possible but still visible and symmetrical (so 3 pixels).
        mHandPath.lineTo(stopX, mCenterY - 1);
        mHandPath.lineTo(stopX, mCenterY + 1);
        mHandPath.lineTo(startX, mCenterY + offsetY);
        mHandPath.lineTo(startX, mCenterY - offsetY);
        mHandPath.close();
    }

    @Override
    public void drawHand(Canvas canvas, float hours) {

        canvas.save();
        final float hoursRotation = Be24WatchFace.angle(hours);
        canvas.rotate(hoursRotation, mCenterX, mCenterY);

        // triangular hand with different colored centre
        canvas.drawPath(mHandPath, mHandInnerPaint);
        canvas.drawPath(mHandPath, mHandPaint);

        // and a circle at the centre
        // Add this line if you want a filled centre.
        canvas.drawCircle(mCenterX, mCenterY, TRIANGLE_WIDTH, mHandInnerPaint);
        canvas.drawCircle(mCenterX, mCenterY, TRIANGLE_WIDTH, mHandPaint);

        canvas.restore();
    }

}
