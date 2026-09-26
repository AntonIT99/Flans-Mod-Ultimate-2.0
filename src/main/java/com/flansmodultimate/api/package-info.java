/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
/**
 * Public API of Flan's Mod Ultimate, the supported way for other mods to interact with it.
 * Everything outside this package, and outside {@code com.flansmodultimate.api.client}, is internal
 * and may change without notice.
 *
 * <p>Start from {@link com.flansmodultimate.api.FlansModApi} to look up content-pack definitions.
 * Types marked {@link org.jetbrains.annotations.ApiStatus.NonExtendable} are implemented by Flan's
 * Mod Ultimate only: use them, do not implement them. Types marked
 * {@link org.jetbrains.annotations.ApiStatus.Experimental} may still change between versions.</p>
 *
 * <p>Flan's Mod Ultimate is usually an optional dependency: only load classes that reference this
 * package after checking that the mod is present.</p>
 */
package com.flansmodultimate.api;
