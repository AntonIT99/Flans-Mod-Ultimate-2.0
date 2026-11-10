package com.flansmodultimate.common.eras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.flansmodultimate.common.eras.ServiceDate.monthIndex;
import static org.junit.jupiter.api.Assertions.*;

class ServiceDateTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();

    @Test
    void monthCoversOneMonth()
    {
        ServiceDate date = ServiceDate.parse("1944-06");
        assertEquals(ServiceDate.Precision.MONTH, date.precision());
        assertEquals(monthIndex(1944, 6), date.first(ERAS));
        assertEquals(monthIndex(1944, 6), date.last(ERAS));
    }

    @Test
    void yearIsLenientAtBothEnds()
    {
        ServiceDate date = ServiceDate.parse("1944");
        assertEquals(monthIndex(1944, 1), date.first(ERAS));
        assertEquals(monthIndex(1944, 12), date.last(ERAS));
    }

    @Test
    void approximateYearKeepsItsMarker()
    {
        ServiceDate date = ServiceDate.parse("~1944");
        assertTrue(date.approximate());
        assertEquals(monthIndex(1944, 1), date.first(ERAS));
        assertEquals("~1944", date.toString());
    }

    @Test
    void beforeChristYearsAreAstronomical()
    {
        assertEquals(-479, ServiceDate.parse(" 480 bc ").year());
        assertEquals(0, ServiceDate.parse("1BC").year());
        assertEquals("~480BC", ServiceDate.parse("~480BC").toString());
    }

    @Test
    void centuriesUsePopularReckoning()
    {
        ServiceDate twentieth = ServiceDate.parse("20C");
        assertEquals(monthIndex(1900, 1), twentieth.first(ERAS));
        assertEquals(monthIndex(1999, 12), twentieth.last(ERAS));
        ServiceDate first = ServiceDate.parse("1C");
        assertEquals(monthIndex(1, 1), first.first(ERAS));
        assertEquals(monthIndex(99, 12), first.last(ERAS));
        ServiceDate fifthBc = ServiceDate.parse("5CBC");
        assertEquals(monthIndex(-499, 1), fifthBc.first(ERAS));
        assertEquals(monthIndex(-400, 12), fifthBc.last(ERAS));
        assertEquals("5CBC", fifthBc.toString());
    }

    @Test
    void erasResolveByIdOrAliasWithoutCaseOrSeparators()
    {
        ServiceDate coldWar = ServiceDate.parse("Cold War");
        assertEquals(ServiceDate.parse("cold_war").first(ERAS), coldWar.first(ERAS));
        assertEquals(ServiceDate.parse("COLD-WAR").last(ERAS), coldWar.last(ERAS));
        assertEquals(monthIndex(1914, 7), ServiceDate.parse("GreatWar").first(ERAS));
        assertEquals(ServiceDate.OPEN_HIGH, ServiceDate.parse("DigitalAge").last(ERAS));
    }

    @Test
    void unknownEraParsesButDoesNotResolve()
    {
        ServiceDate atlantis = ServiceDate.parse("Atlantis");
        assertFalse(atlantis.isResolvable(ERAS));
        assertThrows(IllegalArgumentException.class, () -> atlantis.first(ERAS));
    }

    @Test
    void futureIsLaterThanEveryDateAndNeverEnds()
    {
        for (String text : new String[]{"Future", "sci-fi"})
        {
            ServiceDate future = ServiceDate.parse(text);
            assertEquals(ServiceDate.Precision.FUTURE, future.precision());
            assertEquals(ServiceDate.FUTURE, future.first(ERAS));
            assertEquals(ServiceDate.OPEN_HIGH, future.last(ERAS));
        }
        assertTrue(ServiceDate.parse("1000000").last(ERAS) < ServiceDate.FUTURE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "1944-13", "1944-0", "0", "-480", "~1944-06", "0C", "1944.5", "12abc!"})
    void rejectsMalformedDates(String text)
    {
        assertThrows(IllegalArgumentException.class, () -> ServiceDate.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1944-06", "1944", "~1944", "480BC", "~480BC", "20C", "5CBC", "ww2", "Future"})
    void printedFormReadsBackTheSame(String text)
    {
        ServiceDate date = ServiceDate.parse(text);
        assertEquals(date, ServiceDate.parse(date.toString()));
    }
}
