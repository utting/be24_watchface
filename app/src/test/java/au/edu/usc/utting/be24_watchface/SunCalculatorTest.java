package au.edu.usc.utting.be24_watchface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test SunCalculator with values from: https://www.timeanddate.com
 *
 */
class SunCalculatorTest {

    @Test
    void testDates() {
        Locale qld = new Locale("au", "au");
        TimeZone timeZone = TimeZone.getTimeZone("AEST");
        Calendar jun22 = Calendar.getInstance(timeZone, qld);
        Calendar dec22 = Calendar.getInstance(timeZone, qld);
        jun22.set(2018, 05, 22, 0, 0, 0);
        dec22.set(2018, 11, 22, 0, 0, 0);
        assertEquals(5, jun22.get(Calendar.MONTH)); // months start from 0
        assertEquals(22, jun22.get(Calendar.DAY_OF_MONTH));
        assertEquals(11, dec22.get(Calendar.MONTH)); // months start from 0
        assertEquals(22, dec22.get(Calendar.DAY_OF_MONTH));
    }

    private void runTest(Calendar date, double lat, double lon, float rise, float set) {
        SunCalculator sun = new SunCalculator();
        sun.calculateSunRiseSet(lat, lon, date);
        assertEquals(rise, sun.getSunrise(), 0.02);
        assertEquals(set, sun.getSunset(), 0.02);
    }

    @Test
    void testUscDec() {
        Locale qld = new Locale("au", "au");
        Calendar dec22 = Calendar.getInstance(TimeZone.getTimeZone("Australia/Brisbane"), qld);
        dec22.set(2018, 11, 22, 0, 0, 0);
        // solar noon: 11:46:07
        runTest(dec22, -26.71683, 153.057333, 4 + 51f/60f, 18 + 41f/60f);
    }

    @Test
    void testUscJun() {
        Locale qld = new Locale("au", "au");
        Calendar jun22 = Calendar.getInstance(TimeZone.getTimeZone("Australia/Brisbane"), qld);
        jun22.set(2018, 5, 22, 0, 0, 0);
        // solar noon: 11:49:41
        runTest(jun22, -26.71683, 153.057333, 6 + 36f/60f, 17 + 03f/60f);
    }

    @Test
    void testBesanconDec() {
        Locale france = new Locale("fr", "fr");
        Calendar dec22 = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"), france);
        dec22.set(2018, 11, 22, 0, 0, 0);
        // solar noon: 12:34:36
        runTest(dec22, 47.246, 5.9876, 8 + 20f/60f, 16 + 49f/60f);
    }

    @Test
    void testBesanconJun() {
        Locale france = new Locale("fr", "fr");
        Calendar jun22 = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"), france);
        jun22.set(2018, 5, 22, 0, 0, 0);
        // solar noon: 12:38:03 + 1 hour daylight saving
        runTest(jun22, 47.246, 5.9876, 5 + 39f/60f, 21 + 35f/60f);
    }
}