package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.driveables.armor.ResolvedArmorHit;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketArmourImpact;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Cosmetic feedback for the server's armour decision; never changes a round's trajectory or detonation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArmourImpactEffects
{
    /** All inputs describe the resolved contact, including its normal in world space. */
    @SuppressWarnings("java:S107")
    public static void send(Level level, Vec3 position, Vec3 incoming, Vec3 outwardNormal, float massGrams, ResolvedArmorHit armour, boolean blocked)
    {
        if (level.isClientSide || !armour.isArmoured() || incoming.lengthSqr() < 1.0E-12D || outwardNormal.lengthSqr() < 1.0E-12D)
            return;
        PacketHandler.sendToAllAround(new PacketArmourImpact(position, incoming.normalize(), outwardNormal.normalize(), scale(massGrams), blocked, ModCommonConfig.ricochetSparkCount()), position,
            ModCommonConfig.entityHitParticleRange(), level.dimension());
    }

    /** Mass is the available size proxy: cube-root scaling keeps rifle rounds subtle and naval shells bounded. */
    public static float scale(float massGrams)
    {
        return Float.isFinite(massGrams) && massGrams > 0F ? (float) Mth.clamp(Math.cbrt(massGrams / 1000D), 0.2D, 2.5D) : 0.2F;
    }

    /** Reflected sparks remain outside the plate, even when its virtual slope faces away from the incoming ray. */
    public static Vec3 reflectedDirection(Vec3 incoming, Vec3 outwardNormal)
    {
        Vec3 direction = incoming.normalize();
        Vec3 normal = outwardNormal.normalize();
        return direction.add(normal.scale(2D * Math.abs(direction.dot(normal)))).normalize();
    }
}
