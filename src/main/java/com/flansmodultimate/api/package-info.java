/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
/**
 * Public API of Flan's Mod Ultimate, the supported way for other mods to interact with it.
 * The supported surface includes this package and its subpackages; all other mod packages are internal
 * and may change without notice.
 *
 * <p>
 * Definitions live in {@code content}, vehicle controls in {@code driveables}, held items in
 * {@code equipment}, entity hooks in {@code entity}, damageable bodies in {@code parts}, firing in
 * {@code projectiles}, match extensions in {@code teams}, and command-centre registration in
 * {@code commandcentre}. Rendering and UI extensions remain under {@code client}. API 1.0 moves
 * the former flat API types into these packages; addons must update imports and recompile.
 * </p>
 *
 * <p>
 * Start from {@link com.flansmodultimate.api.FlansModApi} to look up content-pack definitions.
 * Types marked {@link org.jetbrains.annotations.ApiStatus.NonExtendable} are implemented by Flan's
 * Mod Ultimate only: use them, do not implement them. Types marked
 * {@link org.jetbrains.annotations.ApiStatus.Experimental} may still change between versions.
 * </p>
 *
 * <p>
 * Flan's Mod Ultimate is usually an optional dependency: only load classes that reference this
 * package after checking that the mod is present.
 * </p>
 */
package com.flansmodultimate.api;
