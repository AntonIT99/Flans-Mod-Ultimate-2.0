package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Class Skeleton for DebugEntities which use a color
 */
public abstract class DebugColor
{
    @Getter
    protected final Vec3 position;
    @Getter
    protected final float colorRed;
    @Getter
    protected final float colorGreen;
    @Getter
    protected final float colorBlue;
    @Getter
    protected final float colorAlpha;
    protected int lifeTime;

    protected DebugColor(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        this.position = position;
        this.lifeTime = lifeTime;
        colorRed = red;
        colorGreen = green;
        colorBlue = blue;
        colorAlpha = 1F;
    }

    public void tick()
    {
        lifeTime--;
        if (lifeTime <= 0 || !ModClient.isDebug())
            DebugHelper.getActiveDebugEntities().remove(this);
    }

    public abstract AABB getAABB();

    public abstract void render(@NotNull PoseStack pose, @NotNull MultiBufferSource buffers, @NotNull Camera cam);
}
