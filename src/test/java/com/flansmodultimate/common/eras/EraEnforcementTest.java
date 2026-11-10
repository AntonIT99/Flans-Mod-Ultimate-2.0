package com.flansmodultimate.common.eras;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EraEnforcementTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();

    private static String[] window(String restriction)
    {
        return EraEnforcement.describeWindow(new ScopedRestriction(EraScope.server(), EraRestriction.parse(restriction)), ERAS);
    }

    @Test
    void windowsPrintTheirMonthsWithEllipsesForOpenEnds()
    {
        assertArrayEquals(new String[]{"1939-09", "1945-09"}, window("equal WW2"));
        assertArrayEquals(new String[]{"…", "1944-06"}, window("until 1944-06"));
        assertArrayEquals(new String[]{"1950-01", "…"}, window("notbefore 1950"));
        assertArrayEquals(new String[]{"…", "…"}, EraEnforcement.describeWindow(null, ERAS));
    }

    @Test
    void periodsShowAnOpenEndWhenRetirementIsUnknown()
    {
        assertEquals("1942-02 – 1957", EraEnforcement.describePeriod(ServicePeriod.parse("1942-02 1957")));
        assertEquals("~1944 –", EraEnforcement.describePeriod(ServicePeriod.parse("~1944")));
    }
}
