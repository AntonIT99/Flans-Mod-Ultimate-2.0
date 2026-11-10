package com.flansmodultimate.common.eras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EraRestrictionTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();

    private static ServicePeriod period(String introduction, String retirement)
    {
        return new ServicePeriod(ServiceDate.parse(introduction), retirement == null ? null : ServiceDate.parse(retirement));
    }

    private static boolean allows(String restriction, String introduction, String retirement)
    {
        return EraRestriction.parse(restriction).allows(period(introduction, retirement), ERAS);
    }

    @Test
    void untilMeansIntroducedByTheUpperBound()
    {
        assertTrue(allows("until 1944-06", "1944-06", null));
        assertFalse(allows("until 1944-06", "1944-07", null));
        assertTrue(allows("until 1944-06 match=introduction", "1890", "1950"));
    }

    @Test
    void notBeforeUnderWholeServiceMeansStillInService()
    {
        assertTrue(allows("notbefore 1950", "1936", "1950"));
        assertFalse(allows("notbefore 1950", "1936", "1949"));
        assertFalse(allows("notbefore 1950 match=introduction", "1949", "1960"));
        assertTrue(allows("notbefore 1950 match=introduction", "1950", null));
    }

    @Test
    void oldItemsStillInServiceFitAnEraOnlyUnderWholeService()
    {
        assertTrue(allows("equal WW2", "1891", "1965"));
        assertFalse(allows("equal WW2 match=introduction", "1891", "1965"));
        assertTrue(allows("between 1939 1945", "1907", "1957"));
    }

    @Test
    void missingRetirementFollowsThePolicy()
    {
        // A 1937 design with no retirement leaves service with the Interwar era under the default policy.
        assertFalse(allows("equal WW2", "1937", null));
        assertTrue(allows("equal WW2 retirement=open", "1937", null));
        assertTrue(allows("equal WW2", "1942", null));
        assertTrue(allows("equal DigitalAge", "1995", null));
    }

    @Test
    void impreciseDatesAreLenient()
    {
        assertTrue(allows("until 1944-06", "1944", null));
        assertTrue(allows("until 1944-06", "~1944", null));
        assertTrue(allows("notbefore 1945-09", "1930", "WW2"));
        assertFalse(allows("notbefore 1946", "1930", "WW2"), "a WW2 retirement lasts to September 1945 only");
    }

    @Test
    void retirementBeforeIntroductionCountsAsTheIntroduction()
    {
        assertTrue(allows("equal 1944", "1944-06", "1940"));
    }

    @Test
    void futureItemsNeedAWindowThatReachesTheFuture()
    {
        assertFalse(allows("until 2000", "Future", null));
        assertFalse(allows("between 1900 3000", "Future", null));
        assertTrue(allows("notbefore 2000", "Future", null));
        assertTrue(allows("equal Future", "Future", null));
        assertFalse(allows("equal Future", "2020", null));
    }

    @Test
    void undatedAndUnresolvableItemsFollowTheUndatedPolicy()
    {
        EraRestriction lenient = EraRestriction.parse("equal WW2");
        EraRestriction strict = EraRestriction.parse("equal WW2 undated=block");
        assertTrue(lenient.allows(null, ERAS));
        assertFalse(strict.allows(null, ERAS));
        assertTrue(lenient.allows(period("Atlantis", null), ERAS));
        assertFalse(strict.allows(period("Atlantis", null), ERAS));
    }

    @Test
    void printedFormKeepsOnlyChangedPolicies()
    {
        assertEquals("between ww2 1946", EraRestriction.parse(" BETWEEN  WW2 1946 match=service ").toString());
        String full = "notbefore 20C match=introduction retirement=open undated=block ammo=on despawn=on";
        EraRestriction restriction = EraRestriction.parse(full);
        assertEquals(full, restriction.toString());
        assertEquals(restriction, EraRestriction.parse(restriction.toString()));
        assertEquals("until 1944-06", EraRestriction.parse("until 1944-06 ammo=off").toString());
    }

    @Test
    void validateReportsUnknownErasAndReversedBounds()
    {
        assertTrue(EraRestriction.parse("between 1939 1945").validate(ERAS).isEmpty());
        assertTrue(EraRestriction.parse("equal Atlantis").validate(ERAS).isPresent());
        assertTrue(EraRestriction.parse("between 1946 1939").validate(ERAS).isPresent());
        assertTrue(EraRestriction.parse("between 1944-06 1944").validate(ERAS).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sometimes 1944", "between 1944", "equal", "until 1944 foo", "until 1944 match=maybe", "until 1944 ammo=yes", "until 1944 undated=allow undated=block",
        "until 1944 colour=red", "between 1944 match=service", "until =1944"})
    void rejectsMalformedRestrictions(String text)
    {
        assertThrows(IllegalArgumentException.class, () -> EraRestriction.parse(text));
    }

    @Test
    void constructorEnforcesTheBoundsOfEachMode()
    {
        ServiceDate date = ServiceDate.parse("1944");
        assertThrows(IllegalArgumentException.class, () -> EraRestriction.of(EraRestriction.BoundaryMode.UNTIL, date, date));
        assertThrows(IllegalArgumentException.class, () -> EraRestriction.of(EraRestriction.BoundaryMode.BETWEEN, date, null));
        assertThrows(IllegalArgumentException.class, () -> EraRestriction.of(EraRestriction.BoundaryMode.NOT_BEFORE, null, date));
        assertEquals(EraRestriction.Policies.DEFAULT, EraRestriction.of(EraRestriction.BoundaryMode.EQUAL, date, date).policies());
    }
}
