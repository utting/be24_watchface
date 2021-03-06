package au.edu.usc.utting.be24_watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Draws a triangle hand with two bevels, whose colors are taken from a photo of the
 * two-storey high polished chrome Utinam clock in Besançon Franche-Comté TGV station.
 */
public class HourHand3DTriangle extends HourHand {

    /** Determines how wide the base of the triangle is. */
    private static final float TRIANGLE_WIDTH = 20f;

    private static final int BRIGHT_SIDE = Color.rgb(233, 250, 255);
    private static final int DARK_SIDE = Color.rgb(150, 161, 163);
    private static final int DARK_END = Color.rgb(100, 111, 113);

    private Path mTopPath;
    private Path mBotPath;
    private Path mEndPath;


    /**
     * This class just uses the HAND2 paint (FILL mode) with its own custom colors.
     * @param normal
     * @param ambient
     */
    public HourHand3DTriangle(Paint[] normal, Paint[] ambient) {
    }

    @Override
    public void setGeometry(float width, float height, float handLen) {
        super.setGeometry(width, height, handLen);

        // create hand path
        float startX = mCenterX - TRIANGLE_WIDTH / 2f;
        float stopX = mCenterX + mHourHandLength;
        float offsetY = TRIANGLE_WIDTH / 2f;

        // top path will be filled with brighter color.
        mTopPath = new Path();
        mTopPath.setFillType(Path.FillType.EVEN_ODD);
        mTopPath.moveTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mTopPath.lineTo(startX, mCenterY - offsetY);
        // we make the tip as sharp as possible but still visible and symmetrical (3 pixels total).
        mTopPath.lineTo(stopX, mCenterY - 1);
        mTopPath.lineTo(stopX, mCenterY);
        mTopPath.lineTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mTopPath.close();

        // bottom path will be filled with darker shade.
        mBotPath = new Path();
        mBotPath.setFillType(Path.FillType.EVEN_ODD);
        mBotPath.moveTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mBotPath.lineTo(stopX, mCenterY);
        mBotPath.lineTo(stopX, mCenterY + 1);
        mBotPath.lineTo(startX, mCenterY + offsetY);
        mBotPath.lineTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mBotPath.close();

        // bottom path will be filled with darker shade.
        mEndPath = new Path();
        mEndPath.setFillType(Path.FillType.EVEN_ODD);
        mEndPath.moveTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mEndPath.lineTo(startX, mCenterY + offsetY);
        mEndPath.lineTo(startX, mCenterY - offsetY);
        mEndPath.lineTo(startX + TRIANGLE_WIDTH / 2f, mCenterY);
        mEndPath.close();
    }


    @Override
    public void drawHand(Canvas canvas, float hours, Paint[] paint, boolean ambient) {

        canvas.save();
        final float hoursRotation = Be24WatchFace.angle(hours);
        canvas.rotate(hoursRotation, mCenterX, mCenterY);

        // 3D triangular hand with different colored sides
        Paint p = paint[Be24WatchFace.HAND2];
        int oldColor = p.getColor();
        p.setColor(BRIGHT_SIDE);
        canvas.drawPath(mBotPath, p);
        p.setColor(DARK_SIDE);
        canvas.drawPath(mTopPath, p);
        p.setColor(DARK_END);
        canvas.drawPath(mEndPath, p);
        p.setColor(oldColor);

        // and a little circle in the centre to look like a pin
        // (Disabled, since the join of the three sides is now the centre)
        // canvas.drawCircle(mCenterX, mCenterY, 5, mDarkerPaint);

        // Add this line if you want a filled centre.
        // canvas.drawCircle(mCenterX, mCenterY, TRIANGLE_WIDTH - HOUR_STROKE_WIDTH, mHandInnerPaint);
        // canvas.drawCircle(mCenterX, mCenterY, TRIANGLE_WIDTH, mHandPaint);

        canvas.restore();
    }
}
