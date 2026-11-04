package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.explosions.FlanExplosion;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Optional far report for detonation, sent outside the range occupied by near explosion audio. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantDetonationSounds
{
    public static void send(Level level, ShootableType type, Vec3 position, @Nullable Entity explosive)
    {
        if (level.isClientSide || type.getDistantDetonateSound() == null || type.getDistantDetonateSound().isBlank())
            return;
        float nearRange = FlanExplosion.soundRange(type.getExplosionStats(explosive));
        float farRange = type.getDistantDetonateSoundRange();
        if (!Float.isFinite(farRange) || farRange <= nearRange)
            return;
        PacketHandler.sendToDonut(level.dimension(), position, nearRange, farRange,
            new PacketPlaySound(position, farRange, type.getDistantDetonateSound(), true, false, null));
    }
}
