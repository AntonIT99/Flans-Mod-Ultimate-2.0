/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.commandcentre;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * An addon's named settings presets in the command centre. All methods run on the server thread, after operator
 * permission checks. Tags contain configuration only, never live entities or world state; implementations validate
 * before changing settings and throw on failure. Register during setup with {@link FlansCommandCentre#addPresets}.
 */
@ApiStatus.Experimental
public interface ISettingsPresets
{
    /** @return preset names, including built-in entries; callers do not modify this collection */
    Collection<String> names();

    /** @return a copy of the named preset; throw if it does not exist */
    CompoundTag read(String name);

    /** @return the current configuration of the target, such as a round index */
    CompoundTag capture(String target);

    /** Validates an imported preset without side effects; throw if it is malformed or unsupported. */
    void validate(CompoundTag data);

    /** Saves a validated preset durably; existing names require {@code replace}. */
    void save(String name, CompoundTag data, boolean replace) throws IOException;

    /** Removes a custom preset durably; built-in entries must be protected. */
    void remove(String name) throws IOException;

    /** @return whether the entry is built in, so it cannot be overwritten or deleted */
    default boolean builtIn(String name)
    {
        return false;
    }

    /** @return changes, target mapping and dependency warnings shown before applying; no side effects */
    List<Component> preview(CompoundTag data, String target);

    /** Applies a validated preset to the target; validate all targets before making any changes. */
    void apply(CompoundTag data, String target);
}
