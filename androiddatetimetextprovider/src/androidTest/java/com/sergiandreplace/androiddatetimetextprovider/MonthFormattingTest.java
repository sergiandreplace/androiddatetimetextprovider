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
import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeTextProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class MonthFormattingTest {

    private static Object[][] data;

    @BeforeClass
    public static void init() {
        LazyThreeTen.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        DateTimeTextProvider.setInitializer(new AndroidDateTimeTextProvider());

        //Not using Parameterized tests because they are slower,
        //and if for one month test fails then usually others fail as well.
        data = new Object[][] {
                { Calendar.JANUARY, Month.JANUARY },
                { Calendar.FEBRUARY, Month.FEBRUARY },
                { Calendar.MARCH, Month.MARCH },
                { Calendar.APRIL, Month.APRIL },
                { Calendar.MAY, Month.MAY },
                { Calendar.JUNE, Month.JUNE },
                { Calendar.JULY, Month.JULY },
                { Calendar.AUGUST, Month.AUGUST },
                { Calendar.SEPTEMBER, Month.SEPTEMBER },
                { Calendar.OCTOBER, Month.OCTOBER },
                { Calendar.NOVEMBER, Month.NOVEMBER },
                { Calendar.DECEMBER, Month.DECEMBER }
        };
    }

    private GregorianCalendar getCalendar(int calendarMonth) {
        GregorianCalendar calendar = new GregorianCalendar(1970, 0, 1);
        calendar.set(Calendar.MONTH, calendarMonth);
        return calendar;
    }

    private LocalDate getLocalDate(Month month) {
        return LocalDate.of(1970, month, 1);
    }

    private void testPattern(String pattern, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, locale);

        Log.d("Month Formatting Test", "Locale: " + locale.toString());

        for (Object[] entry : data) {
            int calendarMonth = (Integer) entry[0];
            Month month = (Month) entry[1];

            String javaText = simpleDateFormat.format(getCalendar(calendarMonth).getTime());
            String threeTenText = getLocalDate(month).format(formatter);

            Log.d("Month Formatting Test", " Pattern: " + pattern);
            Log.d("Month Formatting Test", "  Month: " + month);
            Log.d("Month Formatting Test", "  Java: " + javaText);
            Log.d("Month Formatting Test", "  ThreeTenBp: " + threeTenText);

            assertEquals(javaText, threeTenText);
        }
    }

    private void assumeNarrowStyleSupported() {
        assumeTrue(Build.VERSION.SDK_INT >= 18);
    }

    public void testFull(Locale locale) {
        testPattern("MMMM", locale);
    }

    public void testShort(Locale locale) {
        testPattern("MMM", locale);
    }

    public void testNarrow(Locale locale) {
        assumeNarrowStyleSupported();
        testPattern("MMMMM", locale);
    }

    public void testFullStandalone(Locale locale) {
        testPattern("LLLL", locale);
    }

    public void testShortStandalone(Locale locale) {
        testPattern("LLL", locale);
    }

    public void testNarrowStandalone(Locale locale) {
        assumeNarrowStyleSupported();
        testPattern("LLLLL", locale);
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
    public void testLocalesWithStandaloneMonths() {
        testLocale(new Locale("ru", "RU"));
        testLocale(new Locale("ca", "ES"));
        testLocale(new Locale("pl", "PL"));
        testLocale(new Locale("fi", "FI"));
    }
} 