package com.flansmodultimate.common.eras;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The command centre's era section: the server-wide and per-dimension restrictions, and the dimensions an operator may
 * restrict. Eras, dates and the operator's own restrictions already reach the client through the era state.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraSection
{
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "eras");
    public static final String SERVER = "server";
    public static final String DIMENSIONS = "dimensions";
    public static final String LEVELS = "levels";

    public static CompoundTag build(ServerPlayer player)
    {
        EraSavedData data = EraSavedData.get(player.server);
        CompoundTag tag = new CompoundTag();
        tag.putString(SERVER, data.getServer().map(EraRestriction::toString).orElse(""));
        CompoundTag dimensions = new CompoundTag();
        data.getDimensions().forEach((dimension, restriction) -> dimensions.putString(dimension, restriction.toString()));
        tag.put(DIMENSIONS, dimensions);
        ListTag levels = new ListTag();
        player.server.levelKeys().stream().map(key -> key.location().toString()).sorted().forEach(id -> levels.add(StringTag.valueOf(id)));
        tag.put(LEVELS, levels);
        return tag;
    }
}
