package com.flansmodultimate.client.render.thermal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalHotParticleIdsTest
{
    @Test
    void theLabjacListIsHotUnderModernIds()
    {
        assertTrue(ThermalHotParticleIds.isHot("minecraft:explosion_emitter", List.of()));
        assertTrue(ThermalHotParticleIds.isHot("minecraft:flame", List.of()));
        assertTrue(ThermalHotParticleIds.isHot("flansmodultimate:fm_muzzle_flash", List.of()));
        assertTrue(ThermalHotParticleIds.isHot("flansmodultimate:big_smoke", List.of()), "Flan's blast smoke is hot by name");
    }

    @Test
    void keywordsMakeOtherModsParticlesHotButNotSmoke()
    {
        assertTrue(ThermalHotParticleIds.isHot("othermod:napalm_burst", List.of()));
        assertTrue(ThermalHotParticleIds.isHot("othermod:Muzzle_Blast", List.of()));
        assertFalse(ThermalHotParticleIds.isHot("minecraft:campfire_cosy_smoke", List.of()));
        assertFalse(ThermalHotParticleIds.isHot("flansmodultimate:smoke_grenade", List.of()));
        assertFalse(ThermalHotParticleIds.isHot("minecraft:poof", List.of()));
        assertFalse(ThermalHotParticleIds.isHot("minecraft:rain", List.of()));
    }

    @Test
    void configuredExtrasAreHotWithOrWithoutANamespace()
    {
        assertTrue(ThermalHotParticleIds.isHot("othermod:glow", List.of("othermod:glow")));
        assertTrue(ThermalHotParticleIds.isHot("othermod:glow", List.of("GLOW")));
        assertTrue(ThermalHotParticleIds.isHot("othermod:hot_smoke", List.of("othermod:hot_smoke")), "an explicit entry wins over the smoke rule");
        assertFalse(ThermalHotParticleIds.isHot("othermod:glow", List.of("othermod:other")));
    }
}
