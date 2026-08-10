package com.flansmodultimate.client.particle;

import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 1.20.1 equivalent of 1.7.10's EntityDiggingFX and EntityBlockDustFX.
 */
public final class LegacyBlockParticle extends TextureSheetParticle
{
    public enum Variant
    {
        CRACK,
        DUST
    }

    private final float uo;
    private final float vo;

    private LegacyBlockParticle(ClientLevel level, BlockState state, BlockPos sourcePos, Variant variant,
                                double x, double y, double z, double vx, double vy, double vz)
    {
        // EntityDiggingFX retained EntityFX's randomized and normalized speed.
        super(level, x, y, z, vx, vy, vz);

        setSprite(selectLegacySprite(state, sourcePos));
        gravity = 1.0F;
        rCol = 0.6F;
        gCol = 0.6F;
        bCol = 0.6F;
        applyLegacyRenderColor(state, sourcePos);
        quadSize /= 2.0F;
        uo = random.nextFloat() * 3.0F;
        vo = random.nextFloat() * 3.0F;

        // EntityBlockDustFX immediately restored the caller's exact velocity.
        if (variant == Variant.DUST)
            setParticleSpeed(vx, vy, vz);
    }

    public static LegacyBlockParticle create(ClientLevel level, BlockState state, BlockPos sourcePos, Variant variant,
                                             double x, double y, double z, double vx, double vy, double vz)
    {
        if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE)
            return null;
        return new LegacyBlockParticle(level, state, sourcePos, variant, x, y, z, vx, vy, vz);
    }

    private TextureAtlasSprite selectLegacySprite(BlockState state, BlockPos sourcePos)
    {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        ModelData modelData = level.getModelDataManager().getAt(sourcePos);
        if (modelData == null)
            modelData = ModelData.EMPTY;
        modelData = model.getModelData(level, sourcePos, state, modelData);

        // EntityDiggingFX chose one of the six block faces at random. Modern
        // models expose face quads rather than Block.getIcon(side, metadata).
        Direction side = Direction.from3DDataValue(random.nextInt(6));
        List<BakedQuad> quads = model.getQuads(state, side, random, modelData, (RenderType)null);
        if (quads.isEmpty())
            quads = model.getQuads(state, null, random, modelData, (RenderType)null);

        return quads.isEmpty()
            ? model.getParticleIcon(modelData)
            : quads.get(random.nextInt(quads.size())).getSprite();
    }

    private void applyLegacyRenderColor(BlockState state, BlockPos sourcePos)
    {
        // EntityDiggingFX.applyRenderColor deliberately left grass untinted.
        if (state.is(Blocks.GRASS_BLOCK))
            return;

        int color = Minecraft.getInstance().getBlockColors().getColor(state, level, sourcePos);
        if (color != -1)
        {
            rCol *= (float)(color >> 16 & 255) / 255.0F;
            gCol *= (float)(color >> 8 & 255) / 255.0F;
            bCol *= (float)(color & 255) / 255.0F;
        }
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType()
    {
        return LegacyParticleRenderTypes.TERRAIN;
    }

    @Override
    protected float getU0()
    {
        return sprite.getU((uo + 1.0F) / 4.0F * 16.0F);
    }

    @Override
    protected float getU1()
    {
        return sprite.getU(uo / 4.0F * 16.0F);
    }

    @Override
    protected float getV0()
    {
        return sprite.getV(vo / 4.0F * 16.0F);
    }

    @Override
    protected float getV1()
    {
        return sprite.getV((vo + 1.0F) / 4.0F * 16.0F);
    }
}
