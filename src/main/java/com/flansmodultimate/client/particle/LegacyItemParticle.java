package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 equivalent of 1.7.10's EntityBreakingFX used by iconcrack.
 */
public final class LegacyItemParticle extends SingleQuadParticle
{
    private final float uo;
    private final float vo;

    public LegacyItemParticle(ClientLevel level, ItemStack stack,
                              double x, double y, double z, double vx, double vy, double vz)
    {
        // EntityBreakingFX first constructed an EntityFX with zero requested
        // velocity, retained ten percent of its randomized velocity, and then
        // added the velocity supplied by the caller.
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, selectSprite(level, stack));

        gravity = 1.0F;
        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        quadSize /= 2.0F;
        uo = random.nextFloat() * 3.0F;
        vo = random.nextFloat() * 3.0F;

        xd = xd * 0.1D + vx;
        yd = yd * 0.1D + vy;
        zd = zd * 0.1D + vz;
    }

    @Override
    @NotNull
    protected Layer getLayer()
    {
        return LegacyParticleRenderTypes.TERRAIN;
    }

    @Override
    protected float getU0()
    {
        return sprite.getU((uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1()
    {
        return sprite.getU(uo / 4.0F);
    }

    @Override
    protected float getV0()
    {
        return sprite.getV(vo / 4.0F);
    }

    @Override
    protected float getV1()
    {
        return sprite.getV((vo + 1.0F) / 4.0F);
    }

    private static net.minecraft.client.renderer.texture.TextureAtlasSprite selectSprite(ClientLevel level, ItemStack stack)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.GROUND, level, null, 0);
        Material.Baked material = renderState.pickParticleMaterial(level.getRandom());
        return material != null ? material.sprite()
            : minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
    }
}
