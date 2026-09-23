package com.flansmodultimate.apocalyse.common.world;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Runs the Apocalypse per-chunk generators as an ordinary biome feature.
 *
 * <p>Placed without placement modifiers, so it runs once per chunk with the chunk's minimum
 * corner as its origin, on a worldgen worker thread and before the chunk is ever sent to a
 * player. One instance decorates the wasteland; the other seeds abandoned portals elsewhere.</p>
 */
public class ApocalypseChunkFeature extends Feature<NoneFeatureConfiguration>
{
    private final boolean wasteland;

    public ApocalypseChunkFeature(boolean wasteland)
    {
        super(NoneFeatureConfiguration.CODEC);
        this.wasteland = wasteland;
    }

    @Override
    public boolean place(@NotNull FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        boolean inApocalypse = level.getLevel().dimension().equals(ApocalypseContent.APOCALYPSE_LEVEL);
        if (wasteland != inApocalypse)
            return false;
        ChunkPos chunk = new ChunkPos(context.origin());
        return wasteland
            ? ApocalypseWorldgen.generateWasteland(level, chunk)
            : ApocalypseWorldgen.generateOverworldPortal(level, chunk);
    }
}
