/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * An immutable paintjob description, readable on either side after content loading.
 * @param id definition-local paintjob ID; zero denotes the default appearance
 * @param name declared display name, or the texture name when no display name was declared
 * @param texture body texture, available on the client; null on a dedicated server
 */
@ApiStatus.Experimental
public record PaintjobVariant(int id, String name, @Nullable ResourceLocation texture) {}
