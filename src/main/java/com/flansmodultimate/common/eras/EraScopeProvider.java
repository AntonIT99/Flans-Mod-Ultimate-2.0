package com.flansmodultimate.common.eras;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

/**
 * Supplies the restrictions one kind of scope places: the world's server and dimension restrictions, a Teams round's for
 * its participants, a battle's for its factions. Called on the server thread.
 */
public interface EraScopeProvider
{
    /** The restrictions on a player, bot or entity. */
    void collect(Entity subject, Consumer<ScopedRestriction> restrictions);

    /**
     * The restrictions on something placed in the level that is no entity yet, such as a vehicle a spawner is about to
     * create. Scopes tied to participants add none.
     */
    default void collectAt(ServerLevel level, Consumer<ScopedRestriction> restrictions)
    {}
}
