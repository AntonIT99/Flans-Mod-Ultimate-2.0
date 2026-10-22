package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Optional;

/**
 * Where a branch serves: in biomes named by id, such as {@code minecraft:desert}, or by tag, such as
 * {@code #minecraft:is_badlands}. A branch with no theatre serves anywhere. Server thread for lookups in a level; the text
 * forms on any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Theatres
{
    private static final String TAG = "#";

    /** @return a theatre as it is kept, {@code minecraft} being the namespace left out, or empty for one that is no id */
    static Optional<String> normalize(String text)
    {
        boolean tag = text.startsWith(TAG);
        ResourceLocation id = ResourceLocation.tryParse(tag ? text.substring(TAG.length()) : text);
        if (id == null)
            return Optional.empty();
        return Optional.of(tag ? TAG + id : id.toString());
    }

    /** Whether a branch with these theatres serves where the biome is. */
    static boolean serves(Collection<String> theatres, Holder<Biome> biome)
    {
        if (theatres.isEmpty())
            return true;
        for (String theatre : theatres)
        {
            boolean tag = theatre.startsWith(TAG);
            ResourceLocation id = ResourceLocation.tryParse(tag ? theatre.substring(TAG.length()) : theatre);
            if (id != null && (tag ? biome.is(TagKey.create(Registries.BIOME, id)) : biome.is(id)))
                return true;
        }
        return false;
    }

    /** Whether a branch with these theatres serves at a place in a level. */
    static boolean serves(Collection<String> theatres, ServerLevel level, Vec3 at)
    {
        return theatres.isEmpty() || serves(theatres, level.getBiome(BlockPos.containing(at)));
    }
}
