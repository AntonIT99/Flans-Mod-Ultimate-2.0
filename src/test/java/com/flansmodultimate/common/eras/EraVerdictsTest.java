package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.types.EnumType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EraVerdictsTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();
    private static final ServicePeriod MODERN = ServicePeriod.parse("1990");

    private static boolean decide(String restriction, EnumType kind, ServicePeriod period)
    {
        return EraVerdicts.decide(EraRestriction.parse(restriction), kind, period, ERAS);
    }

    @Test
    void datedEquipmentFollowsTheWindow()
    {
        assertFalse(decide("equal WW2", EnumType.GUN, MODERN));
        assertTrue(decide("equal WW2", EnumType.GUN, ServicePeriod.parse("1942")));
        assertFalse(decide("equal WW2", EnumType.TOOL, MODERN));
        assertFalse(decide("equal WW2", EnumType.PART, MODERN));
    }

    @Test
    void ammunitionIsExemptUnlessTheRestrictionCoversIt()
    {
        assertTrue(decide("equal WW2", EnumType.BULLET, MODERN));
        assertFalse(decide("equal WW2 ammo=on", EnumType.BULLET, MODERN));
        assertTrue(decide("equal WW2 ammo=on undated=block", EnumType.BULLET, ServicePeriod.parse("1942")));
        assertFalse(decide("equal WW2 ammo=on undated=block", EnumType.BULLET, null));
        assertTrue(decide("equal WW2 undated=block", EnumType.BULLET, null));
    }

    @Test
    void undatedBlockingCoversEquipmentOnly()
    {
        for (EnumType equipment : new EnumType[]{EnumType.GUN, EnumType.GRENADE, EnumType.ATTACHMENT, EnumType.ARMOR, EnumType.VEHICLE, EnumType.PLANE, EnumType.MECHA,
            EnumType.AA_GUN})
            assertFalse(decide("equal WW2 undated=block", equipment, null), equipment::toString);
        for (EnumType other : new EnumType[]{EnumType.PART, EnumType.TOOL, EnumType.TEAM, EnumType.PLAYER_CLASS})
            assertTrue(decide("equal WW2 undated=block", other, null), other::toString);
        assertTrue(decide("equal WW2", EnumType.GUN, null));
    }

    @Test
    void unknownEraRestrictionIsNotUsable()
    {
        EraVerdicts verdicts = new EraVerdicts(ERAS, type -> null);
        assertFalse(verdicts.isUsable(EraRestriction.parse("equal Atlantis")));
        assertTrue(verdicts.isUsable(EraRestriction.parse("equal WW2")));
        assertSame(EraVerdicts.Verdict.ALLOWED, verdicts.check(List.of(), null));
    }
}
