package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.FlanParticles;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeltTracerTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "tracers"));

    @Test
    void irregularBeltKeepsTracerAndBallisticsOnTheSameRoundAcrossRepeats()
    {
        BulletType bullet = bullet("AddRound Ball 3 10 0 800 0", "AddRound AP-T 1 12 0 900 20 flansmod.fmtracerred", "AddRound HE 2 15 4 750 0");
        for (int shot = 0; shot < 18; shot++)
        {
            FiredShot fired = new FiredShot(null, bullet, null, null, shot);
            boolean tracer = shot % 6 == 3;
            assertEquals(tracer ? FlanParticles.FM_TRACER_RED : "", fired.getTracerParticle());
            if (tracer)
            {
                assertEquals(12F, fired.getProjectileMass());
                assertEquals(45F, fired.getMuzzleVelocity());
            }
        }
    }

    @Test
    void replacementBeltDoesNotLeakOriginalTracersAndScalarOverridesKeepThem()
    {
        BulletType bullet = bullet("AddRound AP-T 1 12 0 900 20 flansmod.fmtracerred", "AddRound HE 1 15 4 750 0");
        AmmoOverride replacement = override("AddRoundForAmmo belt HE 2 15 4 750 0", "AddRoundForAmmo belt AP-T 1 12 0 900 20 flansmod.fmtracergreen");
        assertEquals("", replacement.resolveTracerParticle(bullet, 0));
        assertEquals("", replacement.resolveTracerParticle(bullet, 1));
        assertEquals(FlanParticles.FM_TRACER_GREEN, replacement.resolveTracerParticle(bullet, 2));
        assertEquals("", replacement.resolveTracerParticle(bullet, 3));
        assertEquals(FlanParticles.FM_TRACER_RED, override("AmmoMass belt 100").resolveTracerParticle(bullet, 0));
    }

    @Test
    void firedShotUsesTheWeaponsReplacementVisual()
    {
        BulletType bullet = bullet("AddRound Ball 1 10 0 800 0");
        GunType gun = new GunType();
        gun.load(new TypeFile("tracerGun", EnumType.GUN, PACK, List.of("AddRoundForAmmo belt AP-T 1 12 0 900 20 flansmod.fmtracergreen")));
        FireableGun fireable = new FireableGun(gun, 1F, 0F, 0F, EnumSpreadPattern.CIRCLE);
        FiredShot shot = new FiredShot(fireable, bullet, null, null, 0);
        assertEquals(FlanParticles.FM_TRACER_GREEN, shot.getTracerParticle());
        assertEquals(12F, shot.getProjectileMass());
    }

    @Test
    void legacyBeltsAndAlternateSettingsStillHaveNoPerEntryTracer()
    {
        BulletType bullet = bullet("AddRound AP 1 12 0 900 20", "HasAlternateModel True", "AlternateBulletLoad 4", "AlternateTrailParticles True");
        assertEquals("", new FiredShot(null, bullet, null, null, 3).getTracerParticle());
        assertTrue(bullet.isAlternateRound(3));
        assertTrue(bullet.isAlternateTrailParticles());
        assertEquals("", AmmoOverride.EMPTY.resolveTracerParticle(new BulletType(), 0));
        assertEquals("", override("AddRoundForAmmo belt Ball 1 9").resolveTracerParticle(bullet, 0));
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        var definition = new java.util.ArrayList<>(List.of("ShortName belt", "RoundsPerItem 100"));
        definition.addAll(List.of(lines));
        type.load(new TypeFile("tracerBelt", EnumType.BULLET, PACK, definition));
        return type;
    }

    private static AmmoOverride override(String... lines)
    {
        AmmoOverrides.Result result = AmmoOverrides.read(new TypeFile("tracerGun", EnumType.GUN, PACK, List.of(lines)));
        assertTrue(result.warnings().isEmpty(), () -> result.warnings().toString());
        return result.overrides().get("belt");
    }
}
