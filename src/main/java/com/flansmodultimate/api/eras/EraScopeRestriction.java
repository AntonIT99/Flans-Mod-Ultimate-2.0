/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.eras;

import org.jetbrains.annotations.ApiStatus;

/**
 * One era restriction an {@link IEraScope} places on a subject.
 *
 * @param key
 *            what imposes it, shown to operators, such as the name of a battle
 * @param restriction
 *            the restriction in its one-line form, as {@link FlansEras#validateRestriction} accepts it, for example
 *            {@code between WW2 1946 match=introduction}
 */
@ApiStatus.Experimental
public record EraScopeRestriction(String key, String restriction)
{}
