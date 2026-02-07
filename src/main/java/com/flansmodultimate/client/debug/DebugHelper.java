package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugHelper
{
    @Getter
    private static final List<DebugColor> activeDebugEntities = new CopyOnWriteArrayList<>();

    public static void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue)
    {
        if (!ModClient.isDebug())
            return;

        activeDebugEntities.add(new DebugVector(start, end, lifeTime, red, green, blue));
    }

    public static void spawnDebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        if (!ModClient.isDebug())
            return;

        activeDebugEntities.add(new DebugDot(position, lifeTime, red, green, blue));
    }
}
