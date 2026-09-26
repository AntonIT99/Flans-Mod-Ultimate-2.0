/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** A content-pack mecha definition, which walks on legs rather than resting on wheels. */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IMechaType extends IDriveableType
{
    /**
     * @return the declared width, in blocks
     */
    float getWidth();

    /**
     * @return the declared height from the feet, in blocks
     */
    float getHeight();
}
