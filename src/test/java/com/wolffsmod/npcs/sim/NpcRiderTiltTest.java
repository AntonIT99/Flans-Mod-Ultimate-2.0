package com.wolffsmod.npcs.sim;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcRiderTiltTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void levelModelLeavesRidersAlone()
    {
        assertNull(NpcRiderTilt.of(0F, 0F));
    }

    @Test
    void tiltMatchesTheDrawnModel()
    {
        // FlanModelEntityModel draws X(-pitch) then Z(-bank) in the NPC body frame.
        Vector3f banked = NpcRiderTilt.of(0F, 90F).transform(new Vector3f(0F, 1F, 0F));
        assertEquals(1F, banked.x, EPSILON);
        assertEquals(0F, banked.y, EPSILON);
        Vector3f pitched = NpcRiderTilt.of(90F, 0F).transform(new Vector3f(0F, 1F, 0F));
        assertEquals(0F, pitched.y, EPSILON);
        assertEquals(-1F, pitched.z, EPSILON);
    }
}
