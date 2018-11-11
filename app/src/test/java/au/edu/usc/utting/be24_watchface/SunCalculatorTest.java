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

class SunCalculatorTest {
    final static String ISO8601DATEFORMAT = "yyyy-MM-dd"; // 'T'HH:mm:ss.SSZ";

    private Calendar dec22;
    private Calendar jun22;

    @BeforeEach
    void setup() {
        dec22 = getCalendarFromISO("2018-12-22");
        jun22 = getCalendarFromISO("2018-06-22");
    }

    @Test
    void testDates() {
        assertEquals(5, jun22.get(Calendar.MONTH)); // months start from 0
        assertEquals(22, jun22.get(Calendar.DAY_OF_MONTH));
        assertEquals(11, dec22.get(Calendar.MONTH)); // months start from 0
        assertEquals(22, dec22.get(Calendar.DAY_OF_MONTH));
    }

    private void runTest(Calendar date, double lat, double lon, float rise, float set) {
        SunCalculator sun = new SunCalculator();
        sun.calculateSunRiseSet(lat, lon, date);
        assertEquals(rise, sun.getSunrise(), 0.05);
        assertEquals(set, sun.getSunset(), 0.05);
    }

    @Test
    void testUscDec() {
        // solar noon: 11:46:07
        runTest(dec22, -26.71683, 153.057333, 4 + 51f/60f, 18 + 41f/60f);
    }

    @Test
    void testUscJun() {
        // solar noon: 11:49:41
        runTest(jun22, -26.71683, 153.057333, 6 + 36f/60f, 17 + 03f/60f);
    }

    @Test
    void testBesanconDec() {
        // solar noon: 12:34:36
        runTest(dec22, 47.246, 5.9876, 8 + 20f/60f, 16 + 49f/60f);
    }

    @Test
    void testBesanconJun() {
        // solar noon: 12:38:03
        runTest(jun22, 47.246, 5.9876, 4 + 40f/60f, 20 + 36f/60f);
    }

    /**
     * Convert an ISO date to a Java Calendar.
     *
     * This code was adapted from: http://www.java2s.com/Code/Android/Date-Type/GenerateaCalendarfromISO8601date.htm
     *
     * @param datestring YYYY-MM-DD
     * @return
     */
    public static Calendar getCalendarFromISO(String datestring) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()) ;
        SimpleDateFormat dateformat = new SimpleDateFormat(ISO8601DATEFORMAT, Locale.getDefault());
        try {
            Date date = dateformat.parse(datestring);
            // TODO (if we use hours): date.setHours(date.getHours()-1);
            calendar.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }
}