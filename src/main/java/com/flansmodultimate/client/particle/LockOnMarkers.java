package com.flansmodultimate.client.particle;

import com.flansmodultimate.common.FlanParticles;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

/** Short operator-only lock leases. The marker follows the target without revealing it to other clients. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LockOnMarkers
{
    private record Lock(int target, long expires)
    {}

    private static final Map<InteractionHand, Lock> LOCKS = new EnumMap<>(InteractionHand.class);
    private static ClientLevel currentLevel;

    public static void update(int target, InteractionHand hand)
    {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || target < 0)
            LOCKS.remove(hand);
        else
            LOCKS.put(hand, new Lock(target, level.getGameTime() + 6));
    }

    static void tick()
    {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null)
            return;
        LOCKS.values().removeIf(lock -> lock.expires < level.getGameTime());
        if (level.getGameTime() % 3 != 0)
            return;
        for (Lock lock : LOCKS.values())
        {
            Entity target = level.getEntity(lock.target);
            if (target != null && target.isAlive() && !target.isInvisible())
                ParticleHelper.spawnFromString(FlanParticles.FM_LOCKED, target.getX(), target.getBoundingBox().maxY + 0.4D, target.getZ(), 0D, 0D, 0D, 1F, 0.3F);
        }
    }

    @SuppressWarnings("java:S2696") // Marker leases belong to the active client level.
    private static void useLevel(ClientLevel level)
    {
        if (currentLevel != level)
        {
            LOCKS.clear();
            currentLevel = level;
        }
    }
}
