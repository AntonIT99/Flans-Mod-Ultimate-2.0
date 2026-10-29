package com.wolffsmod.npcs.model;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import org.jetbrains.annotations.Nullable;

/** Browser grouping independent of the model's entity kind and NPC simulation behavior. */
public enum FlanModelFolder
{
    AA_GUN, MECHA, PLANE, VEHICLE, SHIP;

    @Nullable
    public static FlanModelFolder of(@Nullable IContentType type)
    {
        if (type instanceof IDriveableType driveable && driveable.isShip())
            return SHIP;
        FlanModelKind kind = FlanModelKind.of(type);
        return kind == null ? null : switch (kind)
        {
            case AA_GUN -> AA_GUN;
            case MECHA -> MECHA;
            case PLANE -> PLANE;
            case VEHICLE -> VEHICLE;
        };
    }
}
