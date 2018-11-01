package com.example.utting.testwearos;


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

    private float mSunRise;
    private float mSunSet;

    /*
     * Calculate sunrise and sunset time, based on lat,lng.
     *
     */
    private void calculateSunRiseSet(double lat, double lng, Calendar cal) {
        double longWest = lng;  // TODO: check what conversion is needed.
        int year = cal.get(Calendar.YEAR);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        // number of days since 1 jan 2000 (Nb. out by 1 day during jan/feb of a leap year).
        int n = year * 365 + (year - 2000) / 4 + dayOfYear;

        // we use local time, not GMT, so can ignore longitude for now.
        // TODO: adjust for position within timezone?  Or use GMT and adjust for timezone?
        double jStar = n; // - longWest / 360.0;

        // solar mean anomaly
        double M = (357.5291 + 0.98560028 * jStar) % 360.0;

        // Equation of the centre value
        double C = 1.9148 * sin(M) + 0.0200 * sin(2 * M) + 0.0003 * sin(3 * M);

        // ecliptic longitude
        // TODO: Wikipedia had M + C + 180 + ..., but should this be times?
        double lambda = (M + C * 180.0 + 102.9372) % 360.0;

        // Solar transit
        double jTransit = 2451545.5 + jStar + 0.0053 * sin(M) - 0.0069 * sin(2 * lambda);

        // delta is the declination of the sun.
        double sinDelta = sin(lambda) * sin(23.44);
        double delta = Math.toDegrees(Math.asin(sinDelta));

        // phi is the north latitude of our location in degrees (negative for south)
        double phi = lat;   // TODO: check what conversion is needed?
        // TODO: use more complex version that handles refraction and sun diameter.
        double tanPhi = tan(phi);
        double tanDelta = tan(delta);
        double cosW0 = tanPhi * tanDelta;
        double W0 = Math.toDegrees(Math.acos(cosW0));

        // sunrise/set are +/- from the zenith (jTransit)
        double jRise = jTransit - W0 / 360.0;
        double jSet = jTransit + W0 / 360.0;
        mSunRise = (float) (jRise - Math.floor(jRise)) * 24.0f;
        mSunSet = (float) (jSet - Math.floor(jSet)) * 24.0f;
    }

    public float getSunrise() {
        return mSunRise;
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
        calc.calculateSunRiseSet(-26.84, 152.96, cal);
        System.out.printf("summer sunrise %.2f, sunset %.2f\n", calc.getSunrise(), calc.getSunset());

        // winter
        cal.set(2018, 6, 22);
        // Sunshine Coast
        calc.calculateSunRiseSet(-26.84, 152.96, cal);
        System.out.printf("winter sunrise %.2f, sunset %.2f\n", calc.getSunrise(), calc.getSunset());
    }
}
