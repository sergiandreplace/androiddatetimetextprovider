/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sergiandreplace.androiddatetimetextprovider;

import android.os.Build;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.threeten.bp.format.DateTimeTextProvider;
import org.threeten.bp.format.TextStyle;
import org.threeten.bp.temporal.IsoFields;
import org.threeten.bp.temporal.TemporalField;

import static org.threeten.bp.temporal.ChronoField.AMPM_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.ERA;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;

/**
 * The Service Provider Implementation to obtain date-time text for a field.
 * <p>
 * This implementation extracts data from {@link DateFormatSymbols} and {@link SimpleDateFormat}.
 * <p>
 * This implementation is based on {@link org.threeten.bp.format.SimpleDateTimeTextProvider SimpleDateTimeTextProvider},
 * but uses Android-specific features of {@link SimpleDateFormat}
 * to extract texts for STANDALONE and NARROW styles
 * for {@link org.threeten.bp.temporal.ChronoField#MONTH_OF_YEAR MONTH_OF_YEAR}
 * and {@link org.threeten.bp.temporal.ChronoField#DAY_OF_WEEK DAY_OF_WEEK}.
 *
 * Texts for other fields are fetched the same way as in
 * {@link org.threeten.bp.format.SimpleDateTimeTextProvider SimpleDateTimeTextProvider}.
 * <p>
 * Note that texts for NARROW style can be extracted only on devices with Android 4.3 or higher.
 * On pre-Android 4.3 devices NARROW style texts are emulated
 * by returning only first character of FULL style text.
 */
final public class AndroidDateTimeTextProvider extends DateTimeTextProvider {

    /** Comparator. */
    private static final Comparator<Entry<String, Long>> COMPARATOR = new Comparator<Entry<String, Long>>() {
        @Override
        public int compare(Entry<String, Long> obj1, Entry<String, Long> obj2) {
            return obj2.getKey().length() - obj1.getKey().length();  // longest to shortest
        }
    };

    /** Cache. */
    private final ConcurrentMap<Entry<TemporalField, Locale>, Object> cache =
            new ConcurrentHashMap<Entry<TemporalField, Locale>, Object>(16, 0.75f, 2);

    //-----------------------------------------------------------------------
    @Override
    public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getText(value, style);
        }
        return null;
    }

    @Override
    public Iterator<Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale) {
        Object store = findStore(field, locale);
        if (store instanceof LocaleStore) {
            return ((LocaleStore) store).getTextIterator(style);
        }
        return null;
    }

    //-----------------------------------------------------------------------
    private Object findStore(TemporalField field, Locale locale) {
        Entry<TemporalField, Locale> key = createEntry(field, locale);
        Object store = cache.get(key);
        if (store == null) {
            store = createStore(field, locale);
            cache.putIfAbsent(key, store);
            store = cache.get(key);
        }
        return store;
    }

    private Object createStore(TemporalField field, Locale locale) {
        if (field == MONTH_OF_YEAR) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TextStyle, Map<Long, String>> styleMap = new HashMap<TextStyle, Map<Long, String>>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("", locale);

            //Uses the same assumptions about months as SimpleDateTimeTextProvider.

            String[] array = oldSymbols.getMonths();
            Map<Long, String> map = createMonthsMapFromSymbolsArray(array);
            styleMap.put(TextStyle.FULL, map);

            array = oldSymbols.getShortMonths();
            map = createMonthsMapFromSymbolsArray(array);
            styleMap.put(TextStyle.SHORT, map);

            if (Build.VERSION.SDK_INT >= 18) {
                map = createMonthsMapFromPattern(dateFormat, "MMMMM");
                styleMap.put(TextStyle.NARROW, map);
                map = createMonthsMapFromPattern(dateFormat, "LLLLL");
                styleMap.put(TextStyle.NARROW_STANDALONE, map);
            } else {
                map = createNarrowMonthsMapFromPattern(dateFormat, "MMMM");
                styleMap.put(TextStyle.NARROW, map);
                map = createNarrowMonthsMapFromPattern(dateFormat, "LLLL");
                styleMap.put(TextStyle.NARROW_STANDALONE, map);
            }

            map = createMonthsMapFromPattern(dateFormat, "LLLL");
            styleMap.put(TextStyle.FULL_STANDALONE, map);

            map = createMonthsMapFromPattern(dateFormat, "LLL");
            styleMap.put(TextStyle.SHORT_STANDALONE, map);

            return createLocaleStore(styleMap);
        }
        if (field == DAY_OF_WEEK) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TextStyle, Map<Long, String>> styleMap = new HashMap<TextStyle, Map<Long, String>>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("", locale);

            String[] array = oldSymbols.getWeekdays();
            Map<Long, String> map = createDaysMapFromSymbolsArray(array);
            styleMap.put(TextStyle.FULL, map);

            array = oldSymbols.getShortWeekdays();
            map = createDaysMapFromSymbolsArray(array);
            styleMap.put(TextStyle.SHORT, map);

            if (Build.VERSION.SDK_INT >= 18) {
                map = createDaysMapFromPattern(dateFormat, "EEEEE");
                styleMap.put(TextStyle.NARROW, map);
                map = createDaysMapFromPattern(dateFormat, "ccccc");
                styleMap.put(TextStyle.NARROW_STANDALONE, map);
            } else {
                map = createNarrowDaysMapFromPattern(dateFormat, "EEEE");
                styleMap.put(TextStyle.NARROW, map);
                map = createNarrowDaysMapFromPattern(dateFormat, "cccc");
                styleMap.put(TextStyle.NARROW_STANDALONE, map);
            }

            map = createDaysMapFromPattern(dateFormat, "cccc");
            styleMap.put(TextStyle.FULL_STANDALONE, map);

            map = createDaysMapFromPattern(dateFormat, "ccc");
            styleMap.put(TextStyle.SHORT_STANDALONE, map);

            return createLocaleStore(styleMap);
        }
        if (field == AMPM_OF_DAY) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TextStyle, Map<Long, String>> styleMap = new HashMap<TextStyle, Map<Long, String>>();
            String[] array = oldSymbols.getAmPmStrings();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(0L, array[Calendar.AM]);
            map.put(1L, array[Calendar.PM]);
            styleMap.put(TextStyle.FULL, map);
            styleMap.put(TextStyle.SHORT, map);  // re-use, as we don't have different data
            return createLocaleStore(styleMap);
        }
        if (field == ERA) {
            DateFormatSymbols oldSymbols = DateFormatSymbols.getInstance(locale);
            Map<TextStyle, Map<Long, String>> styleMap = new HashMap<TextStyle, Map<Long, String>>();
            String[] array = oldSymbols.getEras();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(0L, array[GregorianCalendar.BC]);
            map.put(1L, array[GregorianCalendar.AD]);
            styleMap.put(TextStyle.SHORT, map);
            if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                map = new HashMap<Long, String>();
                map.put(0L, "Before Christ");
                map.put(1L, "Anno Domini");
                styleMap.put(TextStyle.FULL, map);
            } else {
                // re-use, as we don't have different data
                styleMap.put(TextStyle.FULL, map);
            }
            map = new HashMap<Long, String>();
            map.put(0L, array[GregorianCalendar.BC].substring(0, 1));
            map.put(1L, array[GregorianCalendar.AD].substring(0, 1));
            styleMap.put(TextStyle.NARROW, map);
            return createLocaleStore(styleMap);
        }
        // hard code English quarter text
        if (field == IsoFields.QUARTER_OF_YEAR) {
            Map<TextStyle, Map<Long, String>> styleMap = new HashMap<TextStyle, Map<Long, String>>();
            Map<Long, String> map = new HashMap<Long, String>();
            map.put(1L, "Q1");
            map.put(2L, "Q2");
            map.put(3L, "Q3");
            map.put(4L, "Q4");
            styleMap.put(TextStyle.SHORT, map);
            map = new HashMap<Long, String>();
            map.put(1L, "1st quarter");
            map.put(2L, "2nd quarter");
            map.put(3L, "3rd quarter");
            map.put(4L, "4th quarter");
            styleMap.put(TextStyle.FULL, map);
            return createLocaleStore(styleMap);
        }
        return "";  // null marker for map
    }

    private Long calMonthToThreeTenMonth(int calMonth) {
        //Calendar months are from 0 (JANUARY) to 11 (DECEMBER)
        //ThreeTen months are from 1 (JANUARY) to 12 (DECEMBER)

        //Show boxing explicitly.
        //noinspection UnnecessaryBoxing
        return Long.valueOf(calMonth + 1);
    }

    private Long calDayToThreeTenDay(int calDay) {
        //Calendar days start from SUNDAY
        //ThreeTen days start from MONDAY
        //So 1 -> 7, 2 -> 1, ..., 7 -> 6

        //Show boxing explicitly.
        //noinspection UnnecessaryBoxing
        return Long.valueOf(((calDay + 5) % 7) + 1);
    }

    private Map<Long, String> createMonthsMapFromSymbolsArray(String[] array) {
        Map<Long, String> map = new HashMap<Long, String>();
        for (int calMonth = Calendar.JANUARY; calMonth <= Calendar.DECEMBER; ++calMonth) {
            Long threeTenMonth = calMonthToThreeTenMonth(calMonth);
            map.put(threeTenMonth, array[calMonth]);
        }
        return map;
    }

    private Map<Long, String> createMonthsMapFromPattern(
            SimpleDateFormat dateFormat, String pattern) {
        dateFormat.applyPattern(pattern);

        Map<Long, String> map = new HashMap<Long, String>();
        for (int calMonth = Calendar.JANUARY; calMonth <= Calendar.DECEMBER; ++calMonth) {
            Long threeTenMonth = calMonthToThreeTenMonth(calMonth);
            dateFormat.getCalendar().set(Calendar.MONTH, calMonth);
            String formattedMonth = dateFormat.format(dateFormat.getCalendar().getTime());
            map.put(threeTenMonth, formattedMonth);
        }
        return map;
    }

    private Map<Long, String> createNarrowMonthsMapFromPattern(
            SimpleDateFormat dateFormat, String pattern) {
        dateFormat.applyPattern(pattern);

        Map<Long, String> map = new HashMap<Long, String>();
        for (int calMonth = Calendar.JANUARY; calMonth <= Calendar.DECEMBER; ++calMonth) {
            Long threeTenMonth = calMonthToThreeTenMonth(calMonth);
            dateFormat.getCalendar().set(Calendar.MONTH, calMonth);
            String formattedMonth = dateFormat.format(dateFormat.getCalendar().getTime());
            map.put(threeTenMonth, formattedMonth.substring(0, 1));
        }
        return map;
    }

    private Map<Long, String> createDaysMapFromSymbolsArray(String[] array) {
        Map<Long, String> map = new HashMap<Long, String>();
        for (int calDay = Calendar.SUNDAY; calDay <= Calendar.SATURDAY; ++calDay) {
            Long threeTenDay = calDayToThreeTenDay(calDay);
            map.put(threeTenDay, array[calDay]);
        }
        return map;
    }

    private Map<Long, String> createDaysMapFromPattern(
            SimpleDateFormat dateFormat, String pattern) {
        dateFormat.applyPattern(pattern);

        Map<Long, String> map = new HashMap<Long, String>();
        for (int calDay = Calendar.SUNDAY; calDay <= Calendar.SATURDAY; ++calDay) {
            Long threeTenDay = calDayToThreeTenDay(calDay);
            dateFormat.getCalendar().set(Calendar.DAY_OF_WEEK, calDay);
            String formattedMonth = dateFormat.format(dateFormat.getCalendar().getTime());
            map.put(threeTenDay, formattedMonth);
        }
        return map;
    }

    private Map<Long, String> createNarrowDaysMapFromPattern(
            SimpleDateFormat dateFormat, String pattern) {
        dateFormat.applyPattern(pattern);

        Map<Long, String> map = new HashMap<Long, String>();
        for (int calDay = Calendar.SUNDAY; calDay <= Calendar.SATURDAY; ++calDay) {
            Long threeTenDay = calDayToThreeTenDay(calDay);
            dateFormat.getCalendar().set(Calendar.DAY_OF_WEEK, calDay);
            String formattedMonth = dateFormat.format(dateFormat.getCalendar().getTime());
            map.put(threeTenDay, formattedMonth.substring(0, 1));
        }
        return map;
    }

    //-----------------------------------------------------------------------

    /**
     * Helper method to create an immutable entry.
     *
     * @param text the text, not null
     * @param field the field, not null
     * @return the entry, not null
     */
    private static <A, B> Entry<A, B> createEntry(A text, B field) {
        return new SimpleImmutableEntry<A, B>(text, field);
    }

    //-----------------------------------------------------------------------
    private static LocaleStore createLocaleStore(Map<TextStyle, Map<Long, String>> valueTextMap) {
        if (!valueTextMap.containsKey(TextStyle.FULL_STANDALONE)) {
            valueTextMap.put(TextStyle.FULL_STANDALONE, valueTextMap.get(TextStyle.FULL));
        }

        if (!valueTextMap.containsKey(TextStyle.SHORT_STANDALONE)) {
            valueTextMap.put(TextStyle.SHORT_STANDALONE, valueTextMap.get(TextStyle.SHORT));
        }

        if (valueTextMap.containsKey(TextStyle.NARROW) && valueTextMap.containsKey(TextStyle.NARROW_STANDALONE) == false) {
            valueTextMap.put(TextStyle.NARROW_STANDALONE, valueTextMap.get(TextStyle.NARROW));
        }
        return new LocaleStore(valueTextMap);
    }

    /**
     * Stores the text for a single locale.
     * <p>
     * Some fields have a textual representation, such as day-of-week or month-of-year.
     * These textual representations can be captured in this class for printing
     * and parsing.
     * <p>
     * This class is immutable and thread-safe.
     */
    static final class LocaleStore {
        /**
         * Map of value to text.
         */
        private final Map<TextStyle, Map<Long, String>> valueTextMap;
        /**
         * Parsable data.
         */
        private final Map<TextStyle, List<Entry<String, Long>>> parsable;

        //-----------------------------------------------------------------------

        /**
         * Constructor.
         *
         * @param valueTextMap the map of values to text to store, assigned and not altered, not null
         */
        LocaleStore(Map<TextStyle, Map<Long, String>> valueTextMap) {
            this.valueTextMap = valueTextMap;
            Map<TextStyle, List<Entry<String, Long>>> map = new HashMap<TextStyle, List<Entry<String, Long>>>();
            List<Entry<String, Long>> allList = new ArrayList<Entry<String, Long>>();
            for (TextStyle style : valueTextMap.keySet()) {
                Map<String, Entry<String, Long>> reverse = new HashMap<String, Entry<String, Long>>();
                for (Entry<Long, String> entry : valueTextMap.get(style).entrySet()) {
                    if (reverse.put(entry.getValue(), createEntry(entry.getValue(), entry.getKey())) != null) {
                        continue;  // not parsable, try next style
                    }
                }
                List<Entry<String, Long>> list = new ArrayList<Entry<String, Long>>(reverse.values());
                Collections.sort(list, COMPARATOR);
                map.put(style, list);
                allList.addAll(list);
                map.put(null, allList);
            }
            Collections.sort(allList, COMPARATOR);
            this.parsable = map;
        }

        //-----------------------------------------------------------------------

        /**
         * Gets the text for the specified field value, locale and style
         * for the purpose of printing.
         *
         * @param value the value to get text for, not null
         * @param style the style to get text for, not null
         * @return the text for the field value, null if no text found
         */
        String getText(long value, TextStyle style) {
            Map<Long, String> map = valueTextMap.get(style);
            return map != null ? map.get(value) : null;
        }

        /**
         * Gets an iterator of text to field for the specified style for the purpose of parsing.
         * <p>
         * The iterator must be returned in order from the longest text to the shortest.
         *
         * @param style the style to get text for, null for all parsable text
         * @return the iterator of text to field pairs, in order from longest text to shortest text,
         * null if the style is not parsable
         */
        Iterator<Entry<String, Long>> getTextIterator(TextStyle style) {
            List<Entry<String, Long>> list = parsable.get(style);
            return list != null ? list.iterator() : null;
        }
    }
}