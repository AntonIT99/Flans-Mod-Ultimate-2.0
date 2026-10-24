package com.flansmodultimate.common.guns;

import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** World-wide override of the config default, shared by every dimension. */
public class ThrownGunLifetime extends FlanSavedData
{
    private static final String NBT_SECONDS = "seconds";
    private int overrideSeconds = -1;

    public static ThrownGunLifetime get(ServerLevel level)
    {
        return SavedDataPlatform.computeIfAbsent(level.getServer().overworld(), "flansmodultimate_thrown_gun_lifetime", ThrownGunLifetime::new, (tag, registries) -> load(tag));
    }

    public static ThrownGunLifetime load(CompoundTag tag)
    {
        ThrownGunLifetime data = new ThrownGunLifetime();
        if (tag.contains(NBT_SECONDS))
            data.overrideSeconds = Math.max(-1, Math.min(tag.getInt(NBT_SECONDS), Integer.MAX_VALUE / 20));
        return data;
    }

    public int seconds()
    {
        return overrideSeconds < 0 ? ModCommonConfig.THROWN_GUN_DESPAWN_SECONDS.get() : overrideSeconds;
    }

    /** A negative value restores the config default. Existing projectile ages are preserved. */
    public void setSeconds(int seconds)
    {
        overrideSeconds = Math.max(-1, Math.min(seconds, Integer.MAX_VALUE / 20));
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        tag.putInt(NBT_SECONDS, overrideSeconds);
        return tag;
    }
}
