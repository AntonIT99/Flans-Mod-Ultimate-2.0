package com.flansmodultimate.hooks.client;

import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.particle.ParticleHelper;
import com.flansmodultimate.client.render.item.CustomBewlr;
import com.flansmodultimate.hooks.IClientRenderHooks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public final class ClientRenderHooksImpl implements IClientRenderHooks
{
    @Override
    public void initCustomBewlr(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                Minecraft mc = Minecraft.getInstance();
                return new CustomBewlr(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        });
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, float scale)
    {
        ParticleHelper.spawnFromString(s, x, y, z, 0, 0, 0, scale);
    }

    @Override
    public void spawnParticle(String s, double x, double y, double z, double vx, double vy, double vz, float scale)
    {
        ParticleHelper.spawnFromString(s, x, y, z, vx, vy, vz, scale);
    }

    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime, float red, float green, float blue)
    {
        DebugHelper.spawnDebugVector(start, end, lifeTime, red, green, blue);
    }

    public void spawnDebugVector(Vec3 start, Vec3 end, int lifeTime)
    {
        DebugHelper.spawnDebugVector(start, end, lifeTime, 1F, 1F, 1F);
    }

    public void spawnDebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        DebugHelper.spawnDebugDot(position, lifeTime, red, green, blue);
    }

    public void spawnDebugDot(Vec3 position, int lifeTime)
    {
        DebugHelper.spawnDebugDot(position, lifeTime, 1F, 1F, 1F);
    }
}
