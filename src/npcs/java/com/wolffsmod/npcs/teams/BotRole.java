package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.wolffsmod.npcs.model.FlanModelKind;
import org.jetbrains.annotations.Nullable;

/** What a squad does on the battlefield, which decides the tasks a commander gives it. */
public enum BotRole
{
    /** Soldiers on foot, who take and hold objectives. */
    INFANTRY,
    /** Tanks, other ground vehicles and mechas, which lead attacks. */
    ARMOR,
    /** Planes, which patrol over the front and attack in passes. */
    PLANE,
    /** Helicopters, which hover over the front and support attacks. */
    HELICOPTER,
    /** AA guns, which stay where they were placed. */
    AA,
    /** Ships, which patrol the water. */
    NAVAL;

    /** @return the role of a vehicle NPC wearing the model of a definition */
    public static BotRole of(FlanModelKind kind, @Nullable IContentType type)
    {
        boolean ship = type instanceof IDriveableType driveable && driveable.isShip();
        if (ship)
            return NAVAL;
        return switch (kind)
        {
            case AA_GUN -> AA;
            case PLANE -> type instanceof IDriveableType driveable && driveable.isHelicopter() ? HELICOPTER : PLANE;
            case VEHICLE, MECHA -> ARMOR;
        };
    }

    /** Whether the role flies. */
    public boolean flies()
    {
        return this == PLANE || this == HELICOPTER;
    }
}
