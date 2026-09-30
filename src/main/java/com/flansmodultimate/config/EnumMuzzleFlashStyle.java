package com.flansmodultimate.config;

import org.jetbrains.annotations.Nullable;

/** Personal preference when a gun supplies both muzzle flash models. */
public enum EnumMuzzleFlashStyle
{
    FMU_1_7_10,
    MC_1_12_2;

    /** Older packs may supply only one style; keep that model visible in either setting. */
    @Nullable
    public <T> T select(@Nullable T flashModel, @Nullable T muzzleFlashModel)
    {
        if (this == MC_1_12_2)
            return muzzleFlashModel != null ? muzzleFlashModel : flashModel;
        return flashModel != null ? flashModel : muzzleFlashModel;
    }
}
