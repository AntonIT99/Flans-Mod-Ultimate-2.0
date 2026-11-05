package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.penetration.PenetrationResult;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Pack-authored armor feedback selected from the server's resolved penetration; never changes damage. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArmourImpactSounds
{
    public enum Outcome
    {
        RICOCHET, MINOR, PENETRATION, OVERPENETRATION
    }

    public static Outcome outcome(PenetrationResult penetration)
    {
        if (penetration.penetrated())
            return penetration.overmatch() > 3F ? Outcome.OVERPENETRATION : Outcome.PENETRATION;
        return penetration.overmatch() > 0.9F ? Outcome.MINOR : Outcome.RICOCHET;
    }

    public static void send(Level level, Vec3 position, BulletType type, PenetrationResult penetration)
    {
        if (level.isClientSide || !penetration.armourGateRequired())
            return;
        Outcome outcome = outcome(penetration);
        String sound = switch (outcome)
        {
            case RICOCHET -> type.getRicochetSound();
            case MINOR -> type.getMinorPenSound();
            case PENETRATION -> type.getPenetrateSound();
            case OVERPENETRATION -> type.getOverPenSound();
        };
        float range = switch (outcome)
        {
            case RICOCHET -> type.getBounceSoundRange();
            case MINOR -> 35F;
            default -> 50F;
        };
        if (sound != null && !sound.isBlank() && range > 0F)
            PacketPlaySound.sendSoundPacket(position, range, level.dimension(), sound, true, null);
    }
}
