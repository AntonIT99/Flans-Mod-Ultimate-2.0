/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.teams;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * One player-class skin, readable on either side after content loading. Its index in
 * {@link IPlayerClass#getSkinOverrideVariants()} identifies it within that class.
 *
 * @param name
 *            sanitized texture name declared by the content pack
 * @param texture
 *            unchecked client texture location; null on a dedicated server
 */
@ApiStatus.Experimental
public record SkinOverrideVariant(String name, @Nullable ResourceLocation texture)
{}
