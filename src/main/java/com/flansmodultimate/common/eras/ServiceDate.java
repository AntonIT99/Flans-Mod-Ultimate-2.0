package com.flansmodultimate.common.eras;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A point in history as a content author or server owner wrote it: a month, a year (optionally approximate), a century,
 * an era or the open-ended future. It keeps its precision so it can be printed as written, and resolves to a range of
 * months: an introduction uses the {@link #first first} month the value covers, a retirement the {@link #last last}.
 * Years are astronomical internally, so 1 BC is year 0 and 480 BC is year -479.
 */
public record ServiceDate(Precision precision, boolean approximate, int year, int month, @Nullable String era)
{
    /** Lower than every month, for a window with no lower bound. */
    public static final int OPEN_LOW = Integer.MIN_VALUE;
    /** The month every {@link Precision#FUTURE} value starts at, later than any dated month. */
    public static final int FUTURE = Integer.MAX_VALUE - 1;
    /** Higher than every month, for a window with no upper bound or a service with no retirement. */
    public static final int OPEN_HIGH = Integer.MAX_VALUE;
    /** Furthest year from year 0 accepted, which keeps every month index far from the sentinels. */
    public static final int MAX_YEAR = 1_000_000;

    private static final Pattern MONTH = Pattern.compile("(\\d{1,7})-(\\d{1,2})");
    private static final Pattern YEAR = Pattern.compile("(~)?(\\d{1,7})(bc|ad)?");
    private static final Pattern CENTURY = Pattern.compile("(\\d{1,5})c(bc)?");
    private static final Pattern ERA = Pattern.compile("[a-z][a-z0-9]*");

    public enum Precision
    {
        MONTH, YEAR, CENTURY, ERA, FUTURE
    }

    /**
     * Reads a date in the content syntax: {@code 1944-06}, {@code 1944}, {@code ~1944}, {@code 480BC}, {@code 20C},
     * {@code 5CBC}, an era id such as {@code WW2}, or {@code Future}. Case, spaces and underscores are ignored.
     *
     * @throws IllegalArgumentException with a message fit for a content warning when the text is not a date
     */
    public static ServiceDate parse(String text)
    {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("Expected a date such as 1944-06, 1944, ~1944, 480BC, 20C, an era or Future");
        String compact = text.strip().toLowerCase(Locale.ROOT).replaceAll("[\\s_]", "");
        if (Era.isFutureName(compact))
            return new ServiceDate(Precision.FUTURE, false, 0, 0, null);

        Matcher matcher = MONTH.matcher(compact);
        if (matcher.matches())
        {
            int month = Integer.parseInt(matcher.group(2));
            if (month < 1 || month > 12)
                throw new IllegalArgumentException("Month must be from 1 to 12 in '" + text.strip() + "'");
            return new ServiceDate(Precision.MONTH, false, adYear(matcher.group(1), text), month, null);
        }
        matcher = YEAR.matcher(compact);
        if (matcher.matches())
        {
            boolean beforeChrist = "bc".equals(matcher.group(3));
            int year = beforeChrist ? bcYear(matcher.group(2), text) : adYear(matcher.group(2), text);
            return new ServiceDate(Precision.YEAR, matcher.group(1) != null, year, 0, null);
        }
        matcher = CENTURY.matcher(compact);
        if (matcher.matches())
        {
            int century = Integer.parseInt(matcher.group(1));
            if (century < 1 || century > MAX_YEAR / 100)
                throw new IllegalArgumentException("Century out of range in '" + text.strip() + "'");
            // A BC century is stored as a negative number; the sign only selects the years it covers.
            return new ServiceDate(Precision.CENTURY, false, matcher.group(2) == null ? century : -century, 0, null);
        }
        String era = Era.normalize(compact);
        if (ERA.matcher(era).matches())
            return new ServiceDate(Precision.ERA, false, 0, 0, era);
        throw new IllegalArgumentException("'" + text.strip() + "' is not a date; expected a form such as 1944-06, 1944, ~1944, 480BC, 20C, an era or Future");
    }

    /** First month this value covers, or {@link #FUTURE}. */
    public int first(EraCatalog eras)
    {
        return switch (precision)
        {
            case MONTH -> monthIndex(year, month);
            case YEAR -> monthIndex(year, 1);
            case CENTURY -> monthIndex(centuryFirstYear(), 1);
            case ERA -> resolveEra(eras).first();
            case FUTURE -> FUTURE;
        };
    }

    /** Last month this value covers, or {@link #OPEN_HIGH} for the future and eras with no end. */
    public int last(EraCatalog eras)
    {
        return switch (precision)
        {
            case MONTH -> monthIndex(year, month);
            case YEAR -> monthIndex(year, 12);
            case CENTURY -> monthIndex(centuryFirstYear() + (year == 1 ? 98 : 99), 12);
            case ERA -> resolveEra(eras).last();
            case FUTURE -> OPEN_HIGH;
        };
    }

    /** Whether {@link #first} and {@link #last} can resolve this value against those eras. */
    public boolean isResolvable(EraCatalog eras)
    {
        return precision != Precision.ERA || eras.find(era) != null;
    }

    public static int monthIndex(int year, int month)
    {
        return year * 12 + month - 1;
    }

    /**
     * A month index in the content syntax, such as {@code 1939-09}; BC months print as their year, which has no month
     * form. The sentinels print as {@code open} and {@code Future}.
     */
    public static String formatMonth(int index)
    {
        if (index == OPEN_LOW || index == OPEN_HIGH)
            return "open";
        if (index == FUTURE)
            return Era.FUTURE_ID;
        int year = Math.floorDiv(index, 12);
        if (year < 1)
            return new ServiceDate(Precision.YEAR, false, year, 0, null).toString();
        return new ServiceDate(Precision.MONTH, false, year, Math.floorMod(index, 12) + 1, null).toString();
    }

    /** The value in the content syntax, as {@link #parse} reads it back. */
    @Override
    public String toString()
    {
        return switch (precision)
        {
            case MONTH -> String.format(Locale.ROOT, "%d-%02d", year, month);
            case YEAR -> (approximate ? "~" : "") + (year >= 1 ? Integer.toString(year) : (1 - year) + "BC");
            case CENTURY -> year > 0 ? year + "C" : -year + "CBC";
            case ERA -> era;
            case FUTURE -> Era.FUTURE_ID;
        };
    }

    /** Popular reckoning: the 20th century is 1900 to 1999, the 1st is 1 to 99 and the 5th BC is 500 to 401 BC. */
    private int centuryFirstYear()
    {
        if (year > 0)
            return year == 1 ? 1 : (year - 1) * 100;
        return 1 + year * 100;
    }

    private Era resolveEra(EraCatalog eras)
    {
        Era found = eras.find(era);
        if (found == null)
            throw new IllegalArgumentException("Unknown era '" + era + "'");
        return found;
    }

    private static int adYear(String digits, String text)
    {
        int year = Integer.parseInt(digits);
        if (year < 1 || year > MAX_YEAR)
            throw new IllegalArgumentException("Year out of range in '" + text.strip() + "'; there is no year 0, use 1BC");
        return year;
    }

    private static int bcYear(String digits, String text)
    {
        int year = Integer.parseInt(digits);
        if (year < 1 || year > MAX_YEAR)
            throw new IllegalArgumentException("Year out of range in '" + text.strip() + "'");
        return 1 - year;
    }
}
