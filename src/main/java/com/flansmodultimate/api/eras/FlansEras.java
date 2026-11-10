/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.eras;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.common.eras.EraApiSupport;
import com.flansmodultimate.common.eras.EraRestrictions;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Entry point to era restrictions, which limit equipment to a period of history. Content types may carry an issue and
 * a retirement date; a restriction is a window of months, written in one line such as {@code between WW2 1946} or
 * {@code until 1944-06 match=introduction}, and applies to the subjects of its scope: the server, a dimension, a
 * Teams round's participants or an addon's {@link IEraScope}. An entity may have and use a type only when every
 * restriction covering it allows the type; operators may bypass them.
 *
 * <p>
 * Every method is for the server thread. Off the server, checks allow everything.
 * </p>
 */
@ApiStatus.Experimental
public final class FlansEras
{
    private FlansEras()
    {}

    /**
     * @param subject
     *            a player, bot or entity
     * @param type
     *            any content type
     * @return whether every restriction covering the subject allows the type; true for an operator who bypasses them
     */
    public static boolean isAllowed(Entity subject, IContentType type)
    {
        return subject.level().isClientSide || EraApiSupport.isAllowed(subject, type);
    }

    /**
     * @param subject
     *            a player or bot
     * @param playerClass
     *            a Teams class
     * @return whether the subject may use everything the class grants
     */
    public static boolean isClassAllowed(Entity subject, IPlayerClass playerClass)
    {
        return subject.level().isClientSide || EraApiSupport.isClassAllowed(subject, playerClass);
    }

    /**
     * The restrictions on something about to exist in a level, such as a bot before it spawns: the server's, the
     * level's dimension's and, for a combatant of the running Teams round, the round's. Add a battle's own with
     * {@link IEraContext#with}.
     *
     * @param level
     *            where it will exist
     * @param roundCombatant
     *            whether it will fight in the running Teams round
     * @return an immutable context
     */
    public static IEraContext contextAt(ServerLevel level, boolean roundCombatant)
    {
        return EraApiSupport.contextAt(level, roundCombatant);
    }

    /**
     * Checks a restriction an addon scope will apply.
     *
     * @param restriction
     *            the one-line form; blank for none
     * @return its canonical form, or an empty string for a blank one
     * @throws IllegalArgumentException
     *             with a message fit to show an operator when it cannot be read, names an unknown era, has reversed bounds
     *             or asks to despawn, which only server and dimension restrictions may
     */
    public static String validateRestriction(String restriction)
    {
        return EraApiSupport.validate(restriction);
    }

    /**
     * Adds a scope whose restrictions are checked with the server's, the dimensions' and the rounds'. Register once,
     * during mod setup.
     *
     * @param scope
     *            the scope
     */
    public static void addScope(IEraScope scope)
    {
        EraApiSupport.addScope(scope);
    }

    /**
     * Call after a scope's restrictions changed, such as when a battle starts or ends: clients are told what they may
     * use now, and entities a scope's restriction now blocks are despawned.
     *
     * @param server
     *            the running server
     */
    public static void restrictionsChanged(MinecraftServer server)
    {
        EraRestrictions.restrictionsChanged(server);
    }
}
