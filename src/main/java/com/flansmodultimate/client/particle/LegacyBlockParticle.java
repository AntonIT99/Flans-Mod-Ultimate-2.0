package com.flansmodultimate.client.particle;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 1.20.1 equivalent of 1.7.10's EntityDiggingFX and EntityBlockDustFX.
 */
public final class LegacyBlockParticle extends SingleQuadParticle
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
        super(level, x, y, z, vx, vy, vz, selectLegacySprite(level, state, sourcePos));
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

    private static TextureAtlasSprite selectLegacySprite(ClientLevel level, BlockState state, BlockPos sourcePos)
    {
        Minecraft minecraft = Minecraft.getInstance();
        BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new java.util.ArrayList<>();
        model.collectParts(level, sourcePos, state, level.getRandom(), parts);

        // EntityDiggingFX chose one of the six block faces at random. Modern
        // models expose face quads rather than Block.getIcon(side, metadata).
        Direction side = Direction.from3DDataValue(level.getRandom().nextInt(6));
        List<BakedQuad> quads = parts.stream().flatMap(part -> part.getQuads(side).stream()).toList();
        if (quads.isEmpty())
            quads = parts.stream().flatMap(part -> part.getQuads(null).stream()).toList();

        return quads.isEmpty()
            ? model.particleMaterial(level, sourcePos, state).sprite()
            : quads.get(level.getRandom().nextInt(quads.size())).materialInfo().sprite();
    }

    private void applyLegacyRenderColor(BlockState state, BlockPos sourcePos)
    {
        // EntityDiggingFX.applyRenderColor deliberately left grass untinted.
        if (state.is(Blocks.GRASS_BLOCK))
            return;

        var tintSource = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
        if (tintSource != null)
        {
            int color = tintSource.colorAsTerrainParticle(state, level, sourcePos);
            rCol *= (float)(color >> 16 & 255) / 255.0F;
            gCol *= (float)(color >> 8 & 255) / 255.0F;
            bCol *= (float)(color & 255) / 255.0F;
        }
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
}
