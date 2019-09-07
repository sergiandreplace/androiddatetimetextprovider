package com.sergiandreplace.androiddatetimetextprovider;

import android.os.Build;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.gabrielittner.threetenbp.LazyThreeTen;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeTextProvider;
import org.threeten.bp.temporal.TemporalAdjusters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DayOfWeekFormattingTest {

    private static Object[][] data;

    @BeforeClass
    public static void init() {
        LazyThreeTen.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        DateTimeTextProvider.setInitializer(new AndroidDateTimeTextProvider());

        //Not using Parameterized tests because they are slower,
        //and if for one month test fails then usually others fail as well.
        data = new Object[][] {
                { Calendar.SUNDAY, DayOfWeek.SUNDAY },
                { Calendar.MONDAY, DayOfWeek.MONDAY },
                { Calendar.TUESDAY, DayOfWeek.TUESDAY },
                { Calendar.WEDNESDAY, DayOfWeek.WEDNESDAY },
                { Calendar.THURSDAY, DayOfWeek.THURSDAY },
                { Calendar.FRIDAY, DayOfWeek.FRIDAY },
                { Calendar.SATURDAY, DayOfWeek.SATURDAY },
        };
    }

    private GregorianCalendar getCalendar(int calendarDay) {
        GregorianCalendar calendar = new GregorianCalendar(1970, 0, 1);
        //Force recalculation.
        calendar.getTime();
        calendar.set(Calendar.DAY_OF_WEEK, calendarDay);
        return calendar;
    }

    private LocalDate getLocalDate(DayOfWeek dayOfWeek) {
        return LocalDate.of(1970, 1, 1).with(TemporalAdjusters.nextOrSame(dayOfWeek));
    }

    private void testPattern(String pattern, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);

        Log.d("Month Formatting Test", "Locale: " + locale.toString());

        for (Object[] entry : data) {
            int calendarDay = (Integer) entry[0];
            DayOfWeek dayOfWeek = (DayOfWeek) entry[1];

            String javaText = simpleDateFormat.format(getCalendar(calendarDay).getTime());
            String threeTenText = getLocalDate(dayOfWeek).format(formatter);

            Log.d("Month Formatting Test", " Pattern: " + pattern);
            Log.d("Month Formatting Test", "  DayOfWeek: " + dayOfWeek);
            Log.d("Month Formatting Test", "  Java: " + javaText);
            Log.d("Month Formatting Test", "  ThreeTenBp: " + threeTenText);

            assertEquals(javaText, threeTenText);
        }
    }

    private void assumeNarrowStyleSupported() {
        assumeTrue(Build.VERSION.SDK_INT >= 18);
    }

    public void testFull(Locale locale) {
        testPattern("EEEE", locale);
    }

    public void testShort(Locale locale) {
        testPattern("EEE", locale);
    }

    public void testNarrow(Locale locale) {
        assumeNarrowStyleSupported();
        testPattern("EEEEE", locale);
    }

    public void testFullStandalone(Locale locale) {
        testPattern("cccc", locale);
    }

    public void testShortStandalone(Locale locale) {
        testPattern("ccc", locale);
    }

    public void testNarrowStandalone(Locale locale) {
        assumeNarrowStyleSupported();
        testPattern("ccccc", locale);
    }

    public void testLocale(Locale locale) {
        testFull(locale);
        testShort(locale);
        testNarrow(locale);
        testFullStandalone(locale);
        testShortStandalone(locale);
        testNarrowStandalone(locale);
    }

    @Test
    public void testLocalesWithStandaloneDaysOfWeek() {
        testLocale(new Locale("ru", "RU"));
        testLocale(new Locale("pl", "PL"));
        testLocale(new Locale("fi", "FI"));
    }
}
