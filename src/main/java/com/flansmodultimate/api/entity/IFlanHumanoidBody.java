/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Implemented by a living entity drawn with the vanilla humanoid model, such as a human NPC, so that Flan rounds hit it
 * limb by limb as they hit players: a round that crosses its box between the limbs misses, and the part struck sets the
 * damage multiplier, headshots included, and the worn armour piece that resists the round.
 *
 * <p>
 * Asked only for an entity whose box a round crosses, wherever Flan resolves a hit: always on the server thread, and on
 * a client that follows a round's path, so the answer must be available on both sides. Keep it cheap.
 * </p>
 */
@ApiStatus.Experimental
public interface IFlanHumanoidBody
{
    /**
     * @return the proportions the entity is drawn with right now, or empty while it is not drawn as a humanoid, for
     *         example with another mob's model or in a pose the player model has not, so that rounds hit its box as one
     */
    Optional<HumanoidBody> flansmodultimateHumanoidBody();
}
