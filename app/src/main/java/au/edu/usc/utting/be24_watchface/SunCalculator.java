package au.edu.usc.utting.be24_watchface;


import android.util.Log;

import java.util.Calendar;

/**
 * Calculates sunrise and sunset times.
 *
 *
 * TODO: improve.  There is not enough summer/winter variation yet.
 * delta seems to be only 12 degrees for summer, rather than the full 23.44 degrees?
 *
 * These calculations are taken from Wikipedia: Sunrise equation.
 */
public class SunCalculator {
    private static final String TAG = "SunCalculator";

    private float mSunRise;
    private float mSolarNoon;
    private float mSunSet;
    private float mTimezone = 10.0f;

    /*
     * Calculate sunrise and sunset time, based on lat,lng.
     *
     */
    public void calculateSunRiseSet(double lat, double lng, Calendar cal) {
        mSunRise = (float) getLocalTime(lat, lng, cal, true);
        mSunSet = (float) getLocalTime(lat, lng, cal, false);
        Log.i(TAG, String.format("Alg 2: sunrise=%.2f              sunset=%.2f\n", mSunRise, mSunSet));
/*
        double longWest = lng;
        int year = cal.get(Calendar.YEAR);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        // number of days since 1 jan 2000 (Nb. out by 1 day during jan/feb of a leap year).
        double n = year * 365 + (year - 2000) / 4 + dayOfYear + 0.0008;

        // we use local time, not GMT, so can ignore longitude for now.
        double jStar = n - longWest / 360.0;

        // solar mean anomaly
        double M = (357.5291 + 0.98560028 * jStar) % 360.0;

        // Equation of the centre value
        double C = 1.9148 * sin(M) + 0.0200 * sin(2 * M) + 0.0003 * sin(3 * M);

        // ecliptic longitude
        double lambda = (M + C + 180.0 + 102.9372) % 360.0;

        // Solar transit
        double jTransit = 2451545.5 + jStar + 0.0053 * sin(M) - 0.0069 * sin(2 * lambda);
        mSolarNoon = (float) (jTransit - (n + 2451545)) * 24f;

        // delta is the declination of the sun.
        double sinDelta = sin(lambda) * sin(23.44);
        double delta = Math.toDegrees(Math.asin(sinDelta));

        // phi is the north latitude of our location in degrees (negative for south)
        double phi = lat;
        // Simple sunrise eqn:
        // double tanPhi = tan(phi);
        // double tanDelta = tan(delta);
        // double cosW0 = tanPhi * tanDelta;

        // Here is the more complex equation that includes corrections for
        // astronomical refraction and solar disc diameter.
        double cosW0 = (sin(-0.83) - sin(phi) * sin(delta))
            / (cos(phi) * cos(delta));
        double W0 = Math.toDegrees(Math.acos(cosW0));

        // sunrise/set are +/- from the zenith (jTransit)
        double jRise = jTransit - W0 / 360.0;
        double jSet = jTransit + W0 / 360.0;
        mSunRise = (float) (jRise - Math.floor(jRise)) * 24f;
        mSunSet = (float) (jSet - Math.floor(jSet)) * 24f;
        Log.i(TAG, String.format("Alg 1: sunrise=%.2f              sunset=%.2f\n", mSunRise, mSunSet));
        */
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
        int offset = (int)(lng / 15); // estimate utc correction
        double localT = UT + offset; // -5 for baltimore
        if (localT > 24) {
            localT = localT - 24;
        }
        else if (localT < 0) {
            localT = localT + 24;
        }
        return localT;
    }

    public float getSunrise() {
        return mSunRise;
    }

    public float getSolarNoon() {
        return mSolarNoon;
    }

    public float getSunset() {
        return mSunSet;
    }

    /**
     * A convenience version of Math.sin that takes degrees as input.
     *
     * @param degrees
     * @return sin of that angle.
     */
    private static double sin(double degrees) {
        return Math.sin(Math.toRadians(degrees));
    }

    /**
     * A convenience version of Math.tan that takes degrees as input.
     *
     * @param degrees
     * @return tan of that angle.
     */
    private static double tan(double degrees) {
        return Math.tan(Math.toRadians(degrees));
    }

    /**
     * A convenience version of Math.cos that takes degrees as input.
     *
     * @param degrees
     * @return cos of that angle.
     */
    private static double cos(double degrees) {
        return Math.cos(Math.toRadians(degrees));
    }

    /**
     * For testing.
     *
     * @param args
     */
    public static void main(String[] args) {
        SunCalculator calc = new SunCalculator();
        Calendar cal = Calendar.getInstance();

        // summer
        cal.set(2018, 12, 22);
        // Sunshine Coast
        calc.calculateSunRiseSet(-26.71683, 153.057333, cal);
        System.out.printf("summer sunrise %.2f noon=%.4f sunset %.2f\n",
                calc.getSunrise(), calc.getSolarNoon(), calc.getSunset());
        System.out.printf("EXPECT sunrise %.2f noon=%.4f sunset %.2f\n\n",
                4 + 51f/60f, 11+46.1f/60f, 18 + 41f/60f);

        // winter
        cal.set(2018, 6, 22);
        // Sunshine Coast
        calc.calculateSunRiseSet(-26.71683, 153.057333, cal);
        System.out.printf("winter sunrise %.2f noon=%.4f sunset %.2f\n",
                calc.getSunrise(), calc.getSolarNoon(), calc.getSunset());
        System.out.printf("EXPECT sunrise %.2f noon=%.4f sunset %.2f\n\n",
                6 + 36f/60f, 11+49.7f/60f,17 + 03f/60f);

        System.out.println("BESANCON");

        // December
        cal.set(2018, 12, 22);
        calc.calculateSunRiseSet(47.246, 5.9876, cal);
        System.out.printf("winter sunrise %.2f noon=%.4f sunset %.2f\n",
                calc.getSunrise(), calc.getSolarNoon(), calc.getSunset());
        System.out.printf("EXPECT sunrise %.2f noon=%.4f sunset %.2f\n\n",
                8 + 20f/60f, 12 + 34.6f/60f, 16 + 49f/60f);

        // winter
        cal.set(2018, 6, 22);
        calc.calculateSunRiseSet(47.246, 5.9876, cal);
        System.out.printf("summer sunrise %.2f noon=%.4f sunset %.2f\n",
                calc.getSunrise(), calc.getSolarNoon(), calc.getSunset());
        System.out.printf("EXPECT sunrise %.2f noon=%.4f sunset %.2f\n\n",
                4 + 40f/60f, 12 + 38f/60f, 20 + 36f/60f);
    }
}
