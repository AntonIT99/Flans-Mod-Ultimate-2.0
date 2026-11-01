/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.api.driveables.IDriveableType;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Set;
import java.util.stream.Collectors;

/** Immutable per-spawner NPC vehicle permission, safe to read on either side. */
@ApiStatus.Experimental
public record SpawnerNpcPolicy(Mode mode, Set<String> selections)
{
    /** The four mutually exclusive ways to select permitted NPC definitions. */
    @ApiStatus.Experimental
    public enum Mode
    {
        NONE, ALL, CATEGORIES, DEFINITIONS
    }

    /** All NPC vehicle definitions are allowed by default. */
    public static final SpawnerNpcPolicy ALL = new SpawnerNpcPolicy(Mode.ALL, Set.of());

    /** Copies the selection so a snapshot cannot be mutated by its caller. */
    public SpawnerNpcPolicy
    {
        selections = Set.copyOf(selections);
    }

    /** Returns the category key, or an empty string for non-vehicle definitions. */
    public static String category(IContentType type)
    {
        if (type instanceof IAAGunType)
            return "aa_guns";
        if (type instanceof IDriveableType driveable)
            return driveable.isShip() ? "ships" : switch (driveable.getDriveableKind())
            {
                case VEHICLE -> "vehicles";
                case PLANE -> "planes";
                case MECHA -> "mechas";
            };
        return "";
    }

    /** Whether this definition may use the spawner; does not grant team ownership or bypass spawn limits. */
    public boolean allows(IContentType type)
    {
        if (type == null || category(type).isEmpty())
            return false;
        return switch (mode)
        {
            case NONE -> false;
            case ALL -> true;
            case CATEGORIES -> selections.contains(category(type));
            case DEFINITIONS -> selections.contains(type.getShortName());
        };
    }

    /** Serializes the permission for world persistence and editor packets. */
    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", mode.name());
        ListTag values = new ListTag();
        selections.stream().sorted().forEach(value -> values.add(StringTag.valueOf(value)));
        tag.put("selections", values);
        return tag;
    }

    /** Reads a saved permission; old spawners with no setting allow all NPC vehicles. */
    public static SpawnerNpcPolicy load(CompoundTag tag)
    {
        Mode mode;
        try
        {
            mode = Mode.valueOf(tag.getString("mode"));
        }
        catch (IllegalArgumentException ignored)
        {
            return ALL;
        }
        return new SpawnerNpcPolicy(mode, tag.getList("selections", Tag.TAG_STRING).stream().limit(4096).map(Tag::getAsString).filter(value -> value.length() <= 256).collect(Collectors.toSet()));
    }
}
