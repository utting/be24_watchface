package au.edu.usc.utting.be24_watchface;


import android.util.Log;

import java.util.Calendar;

/**
 * Calculates sunrise and sunset times.
 *
 */
public class SunCalculator {
    private static final String TAG = "SunCalculator";

    private float mSunRise;
    private float mSunSet;

    /*
     * Calculate sunrise and sunset time, based on lat,lng.
     *
     */
    public void calculateSunRiseSet(double lat, double lng, Calendar cal) {
        // System.out.println("date=" + cal.getTime().toString() + " timezone=" + cal.getTimeZone());
        mSunRise = (float) getLocalTime(lat, lng, cal, true);
        mSunSet = (float) getLocalTime(lat, lng, cal, false);
        Log.i(TAG, String.format("Alg 2: sunrise=%.2f              sunset=%.2f\n", mSunRise, mSunSet));
    }

    // Version 2 from https://math.stackexchange.com/questions/2186683/how-to-calculate-sunrise-and-sunset-times
    private double getLocalTime(double lat, double lng, Calendar cal, boolean sunrise) {
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double zenith = 90.83333333333333;
        double D2R = Math.PI / 180;
        double R2D = 180 / Math.PI;

        // convert the longitude to hour value and calculate an approximate time
        double lnHour = lng / 15;
        double t;
        if (sunrise) {
            t = dayOfYear + ((6 - lnHour) / 24);
        } else {
            t = dayOfYear + ((18 - lnHour) / 24);
        };

        //calculate the Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;

        //calculate the Sun's true longitude
        double L = M + (1.916 * Math.sin(M * D2R)) + (0.020 * Math.sin(2 * M * D2R)) + 282.634;
        if (L > 360) {
            L = L - 360;
        }
        else if (L < 0) {
            L = L + 360;
        }

        //calculate the Sun's right ascension
        double RA = R2D * Math.atan(0.91764 * Math.tan(L * D2R));
        if (RA > 360) {
            RA = RA - 360;
        }
        else if (RA < 0) {
            RA = RA + 360;
        }

        //right ascension value needs to be in the same qua
        double Lquadrant = (Math.floor(L / (90))) * 90;
        double RAquadrant = (Math.floor(RA / 90)) * 90;
        RA = RA + (Lquadrant - RAquadrant);

        //right ascension value needs to be converted into hours
        RA = RA / 15;

        //calculate the Sun's declination
        double sinDec = 0.39782 * Math.sin(L * D2R);
        double cosDec = Math.cos(Math.asin(sinDec));

        //calculate the Sun's local hour angle
        double cosH = (Math.cos(zenith * D2R) - (sinDec * Math.sin(lat * D2R)))
                / (cosDec * Math.cos(lat * D2R));
        double H;
        if (sunrise) {
            H = 360 - R2D * Math.acos(cosH);
        } else {
            H = R2D * Math.acos(cosH);
        }
        H = H / 15;

        //calculate local mean time of rising/setting
        double T = H + RA - (0.06571 * t) - 6.622;

        //adjust back to UTC
        double UT = T - lnHour;
        if (UT > 24) {
            UT = UT - 24;
        }
        else if (UT < 0) {
            UT = UT + 24;
        }

        //convert UT value to local time zone of latitude/longitude
        // WAS: int offset0 = (int)(lng / 15); // estimate utc correction
        double offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / (3600.0 * 1000.0);
        double localT = UT + offset;
        if (localT > 24) {
            localT = localT - 24;
        }
        else if (localT < 0) {
            localT = localT + 24;
        }
        // System.out.println("offset=" + offset + " raw=" + offset0 + " localT=" + localT);
        return localT;
    }

    public float getSunrise() {
        return mSunRise;
    }

    public float getSunset() {
        return mSunSet;
    }
}
