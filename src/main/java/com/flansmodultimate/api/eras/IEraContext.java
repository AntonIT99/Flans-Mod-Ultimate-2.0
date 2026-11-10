/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.eras;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.IPlayerClass;
import org.jetbrains.annotations.ApiStatus;

/**
 * A fixed set of era restrictions, for deciding what an entity may be given before it exists, such as the class and
 * vehicle of a bot about to spawn. Obtained from {@link FlansEras#contextAt}; immutable, server thread.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IEraContext
{
    /**
     * @param type
     *            any content type
     * @return whether every restriction of the context allows the type
     */
    boolean allows(IContentType type);

    /**
     * @param playerClass
     *            a Teams class
     * @return whether every Flan's type the class grants, its starting items, their attachments and its armour, is allowed
     */
    boolean allowsClass(IPlayerClass playerClass);

    /**
     * A context that also applies one more restriction.
     *
     * @param key
     *            what imposes it, such as a battle's name
     * @param restriction
     *            the one-line form; a restriction that cannot be read or names an unknown era is ignored
     * @return a new context; this one is unchanged
     */
    IEraContext with(String key, String restriction);
}
