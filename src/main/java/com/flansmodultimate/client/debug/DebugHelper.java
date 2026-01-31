package com.flansmodultimate.client.debug;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.ModClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugHelper
{
    @Getter
    private static final List<DebugColor> activeDebugEntities = new CopyOnWriteArrayList<>();

    public static void spawnDebugVector(Level level, Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue)
    {
        if (!level.isClientSide)
            return;
        if (!ModClient.isDebug())
            return;

        activeDebugEntities.add(new DebugVector(start, end, lifeTime, red, green, blue));
    }

    public static void spawnDebugVector(Level level, Vec3 start, Vec3 end, int lifeTime)
    {
        spawnDebugVector(level, start, end, lifeTime, 1F, 1F, 1F);
    }

    public static void spawnDebugVector(Level level, Vector3f start, Vector3f end, int lifeTime, float red, float green, float blue)
    {
        spawnDebugVector(level, start.toVec3(), end.toVec3(), lifeTime, red, green, blue);
    }

    public static void spawnDebugVector(Level level, Vector3f start, Vector3f end, int lifeTime)
    {
        spawnDebugVector(level, start.toVec3(), end.toVec3(), lifeTime, 1F, 1F, 1F);
    }

    public static void spawnDebugDot(Level level, Vec3 position, int lifeTime, float red, float green, float blue)
    {
        if (!level.isClientSide)
            return;
        if (!ModClient.isDebug())
            return;

        activeDebugEntities.add(new DebugDot(position, lifeTime, red, green, blue));
    }

    public static void spawnDebugDot(Level level, Vec3 position, int lifeTime)
    {
        spawnDebugDot(level, position, lifeTime, 1F, 1F, 1F);
    }

    public static void spawnDebugDot(Level level, Vector3f position, int lifeTime, float red, float green, float blue)
    {
        spawnDebugDot(level, position.toVec3(), lifeTime, red, green, blue);
    }

    public static void spawnDebugDot(Level level, Vector3f position, int lifeTime)
    {
        spawnDebugDot(level, position.toVec3(), lifeTime, 1F, 1F, 1F);
    }
}
