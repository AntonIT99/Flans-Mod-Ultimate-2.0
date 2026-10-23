/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Optional;

/**
 * The part hitboxes of a driveable definition, worn by an entity that is not a driveable: one box and one pool of hit
 * points per part, turned with the entity and its turret, exactly as a driveable of that definition carries them.
 *
 * <p>
 * Created by {@link FlansPartBodies#create} and handed out by the entity through {@link IFlanPartTarget}. Flan rounds
 * and explosions then meet these boxes instead of the entity's own bounding box: a round strikes the part its path
 * crosses first, meets the armour of that part and face, and a blast is shared out between the parts it reaches.
 * </p>
 *
 * <p>
 * A body is plain state without a thread of its own. Use each body from the thread of the level its entity lives in.
 * The server decides hit points; a client needs the same boxes, pose and broken parts to follow rounds faithfully, so
 * the owner has to bring {@link #saveState} across.
 * </p>
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IPartBody
{
    /** @return the driveable definition the parts come from */
    IContentType getType();

    /** @return every part the definition gives a box, the core first, including disabled ones */
    List<PartState> getParts();

    /**
     * @param part
     *            part name as in type files, in any letter case
     * @return the part, or empty when the definition has no such part
     */
    Optional<PartState> getPart(String part);

    /**
     * Replaces the box and hit points of a part, or returns it to the definition's. The part keeps the share of its hit
     * points it had left.
     *
     * @param part
     *            part name as in type files
     * @param box
     *            the replacement, or null for the definition's box
     */
    void setBox(String part, @Nullable PartBox box);

    /**
     * Replaces every face's armour for one part, independently of its box, or restores the definition's armour. The
     * replacement has no virtual slope; impact angle still applies. Used by rounds and blasts only while the wearer's
     * {@link IFlanDamageModel#flansmodultimateModelArmour()} is enabled. Call on the server to decide gameplay and on
     * clients to keep their projectile prediction in agreement; the owner must synchronize these settings separately
     * from {@link #saveState()}.
     *
     * @param part
     *            part name as in type files, in any letter case; unknown parts are ignored
     * @param armor
     *            replacement kinetic and HEAT protection in millimetres, or null to restore the model's plates
     */
    default void setArmor(String part, @Nullable PartArmor armor)
    {}

    /**
     * Reads the explicit armour replacement on either side, on the body's level thread.
     *
     * @param part
     *            part name as in type files, in any letter case
     * @return the replacement, or empty when the part uses model armour or does not exist
     */
    default Optional<PartArmor> getArmor(String part)
    {
        return Optional.empty();
    }

    /**
     * Takes a part out of the body or puts it back. A disabled part is not hit, holds no hit points and is never
     * broken.
     */
    void setEnabled(String part, boolean enabled);

    /** @return where the body stands */
    PartBodyPose getPose();

    /** Moves the body. Call once per tick on either side, after the owner has moved. */
    void setPose(PartBodyPose pose);

    /**
     * @param solid
     *            whether a broken part still stops rounds and blasts. Off by default: a broken part has been shot off,
     *            as on a driveable.
     */
    void setBrokenPartsSolid(boolean solid);

    /**
     * @param outlined
     *            whether clients draw the boxes even outside debug mode: yellow while a part can be hit, red once it
     *            is broken
     */
    void setOutlined(boolean outlined);

    /**
     * @param solid
     *            whether other entities collide with the boxes as they do with a driveable's hull: they walk into them,
     *            stand on them and are carried along. The entity wearing the body, its riders and what it rides pass
     *            through. Broken parts are solid only while {@link #setBrokenPartsSolid} says so. Off by default; takes
     *            effect with the next {@link #setPose}, which has to be called on both sides
     */
    void setSolid(boolean solid);

    /** @return the hit points left in every enabled part together */
    float getHealth();

    /** @return the hit points of every enabled part together when none is damaged */
    float getMaxHealth();

    /**
     * Takes hit points from a part. Breaking a part breaks the parts that hang on it, as a wing takes its wheel along
     * and the core takes everything.
     *
     * @param part
     *            part name as in type files
     * @param amount
     *            hit points to take
     * @param fire
     *            whether the hit sets the part alight
     * @return the hit points the body lost in all, which is less than {@code amount} when the part had fewer left and
     *         more when it took others along
     */
    float damage(String part, float amount, boolean fire);

    /**
     * Sets the hit points left in one part. Raising them above zero puts a broken part back.
     *
     * @param part
     *            part name as in type files
     * @param health
     *            hit points left, clamped to what the part can hold
     */
    void setHealth(String part, float health);

    /** Puts every part back at full hit points and puts out every fire. */
    void restore();

    /**
     * Burns the parts that are alight, one tick's worth. Server side.
     *
     * @return the hit points the body lost
     */
    float tickFire();

    /** @return the hit points and broken state of every part, for saving and for sending to clients */
    CompoundTag saveState();

    /**
     * Takes over saved or received part state. Parts the tag does not mention keep what they have.
     *
     * @param state
     *            a tag written by {@link #saveState}
     */
    void loadState(CompoundTag state);

    /** @return a number that changes whenever a box, armour, a switch or the hit points of a part change */
    int signature();
}
