package com.flansmodultimate.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Bridges the 26.1 value-based persistence API to the mod's legacy NBT
 * serializers. Keeping the legacy serializers centralized preserves the
 * existing save format and avoids making content-pack data version-specific.
 */
public final class ValueIOUtils
{
    private ValueIOUtils()
    {
    }

    @SuppressWarnings("deprecation")
    public static CompoundTag toCompoundTag(ValueInput input)
    {
        // ValueInput is map-shaped at the entity root. NeoForge's extension
        // uses the same codec internally for keySet().
        return input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
    }

    public static void storeCompoundTag(ValueOutput output, CompoundTag tag)
    {
        output.store(tag);
    }
}
