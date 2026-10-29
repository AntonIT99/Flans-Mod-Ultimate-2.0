package com.flansmodultimate.common.driveables.collision;

import net.minecraft.world.entity.Entity;

/**
 * What being carried by a moving hull did to one entity during the current tick, kept so the client draws the carry
 * as smooth motion rather than a step every tick.
 *
 * <p>
 * A hull carries what stands on it during its own tick, and the client then interpolates the entity between its
 * previous and its current position. That only covers the carry when the entity ticked first: a hull that ticks
 * before it moves the entity before vanilla stores its previous position, so the carry is folded into that position
 * and drawn as a jump at the next tick. {@link #startTick} undoes that for whatever the hull moved earlier in the tick.
 * </p>
 *
 * <p>
 * The camera of a player on foot does not interpolate its yaw at all, so a turning hull or turret would swing the
 * view round in steps. {@link #viewYawLag} instead eases the turn carried in the last tick in over the frames up to
 * the next one.
 * </p>
 *
 * <p>
 * Client only; only the client level's thread touches it.
 * </p>
 */
public final class HullCarry
{
    private long tickTime = Long.MIN_VALUE;
    private boolean applied;
    private double carriedX;
    private double carriedY;
    private double carriedZ;
    private float carriedYaw;
    private int viewTick = Integer.MIN_VALUE;

    /** Records that a hull moved and turned an entity, after the move. */
    public static void noteCarry(Entity entity, double x, double y, double z, float yaw)
    {
        if (entity.level().isClientSide && entity instanceof HullCarriedEntity carried)
            carried.flansmodultimateHullCarry().carried(entity.level().getGameTime(), entity.tickCount, x, y, z, yaw);
    }

    void carried(long gameTime, int tickCount, double x, double y, double z, float yaw)
    {
        if (tickTime != gameTime)
        {
            tickTime = gameTime;
            applied = false;
            carriedX = carriedY = carriedZ = 0D;
            carriedYaw = 0F;
        }
        carriedX += x;
        carriedY += y;
        carriedZ += z;
        carriedYaw += yaw;
        viewTick = tickCount;
    }

    /**
     * Called right after vanilla stored the entity's previous position and rotation, which on the client happens just
     * before the entity's own tick. Moves them back to where the entity was before a hull carried it earlier this tick.
     */
    public void startTick(Entity entity)
    {
        if (applied || tickTime != entity.level().getGameTime())
            return;
        applied = true;
        entity.xo -= carriedX;
        entity.yo -= carriedY;
        entity.zo -= carriedZ;
        entity.yRotO -= carriedYaw;
        // The tick count goes up right after, so the carry belongs to the tick the entity is starting.
        viewTick = entity.tickCount + 1;
    }

    /**
     * @param tickCount
     *            the entity's tick count now
     * @return degrees of the turn carried in the entity's last tick that the view has not caught up with yet at
     *         {@code partialTick}
     */
    public float viewYawLag(int tickCount, float partialTick)
    {
        return tickCount == viewTick ? carriedYaw * (1F - partialTick) : 0F;
    }
}
