package com.flansmodultimate.common.driveables.parts;

import com.flansmodultimate.api.*;
import com.flansmodultimate.common.types.DriveableType;
import com.google.common.collect.MapMaker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * The entities that wear a {@link PartBody}.
 *
 * <p>
 * Part boxes reach well past the compact bounding box of the entity wearing them, as a ship's do, so hit detection
 * cannot find these entities with a box query around a round. They are few, so it walks all of them instead. The
 * entity itself decides whether it is still hit by the part: a body it no longer hands out counts for nothing.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PartBodies
{
    /** Client and server levels register from their own threads; entities are forgotten once nothing else holds them. */
    private static final Map<Entity, Boolean> OWNERS = new MapMaker().weakKeys().makeMap();

    public static Optional<IPartBody> create(Entity owner, IContentType type)
    {
        if (!(type instanceof DriveableType driveable))
            return Optional.empty();
        PartBody body = PartBody.of(driveable);
        if (body.isEmpty())
            return Optional.empty();
        body.setOwner(owner);
        OWNERS.put(owner, Boolean.TRUE);
        return Optional.of(body);
    }

    /** @return the body Flan rounds and blasts hit the entity by, or null when it is hit as an ordinary entity */
    @Nullable
    public static PartBody of(@Nullable Entity entity)
    {
        return entity instanceof IFlanPartTarget target && target.flansmodultimatePartBody().orElse(null) instanceof PartBody body ? body : null;
    }

    /** @return the living entities of the level that currently wear a body */
    public static List<Entity> wearers(Level level)
    {
        if (OWNERS.isEmpty())
            return List.of();
        List<Entity> wearers = new ArrayList<>();
        for (Entity owner : OWNERS.keySet())
        {
            if (owner.isRemoved())
                OWNERS.remove(owner);
            else if (owner.level() == level && owner.isAlive() && of(owner) != null)
                wearers.add(owner);
        }
        return wearers;
    }
}
