package com.flansmodultimate.common.driveables.physics;

import lombok.Getter;

/**
 * Thermal lag behind a cockpit's oil and water gauges, as fractions of their full scale. Heat follows the throttle
 * slowly, maximum power pushes it further, and it drains away faster once the engine stops feeding the gauges.
 */
@Getter
public final class EngineGaugeHeat
{
    private static final float RUNNING_THROTTLE = 0.01F;
    private static final float FULL_THROTTLE = 0.98F;
    private static final float CRUISE_HEAT_SHARE = 0.7F;
    private static final float SUSTAINED_LOAD_PER_TICK = 0.00015F;
    private static final float OIL_IDLE_HEAT = 0.25F;
    private static final float OIL_IDLE_MAX_THROTTLE = 0.2F;
    private static final float SHUTDOWN_COOLING_PER_TICK = 0.025F;
    private static final float WATER_MAX_LOAD_SLOWDOWN = 0.25F;

    private float oil;
    private float water;
    private float sustainedLoad;

    public void advance(float throttle, boolean engineRunning, int elapsedTicks)
    {
        advance(throttle, engineRunning, false, false, elapsedTicks);
    }

    /** With an afterburner only burning it builds the extra heat; other engines build it at sustained full throttle. */
    public void advance(float throttle, boolean engineRunning, boolean hasAfterburner, boolean burning, int elapsedTicks)
    {
        float demand = engineRunning ? Math.abs(throttle) : 0F;
        boolean maximumPower = hasAfterburner ? burning : demand >= FULL_THROTTLE;
        for (int tick = 0; tick < elapsedTicks; tick++)
            step(demand, maximumPower);
    }

    private void step(float demand, boolean maximumPower)
    {
        if (demand < RUNNING_THROTTLE)
        {
            oil = Math.max(0F, oil - SHUTDOWN_COOLING_PER_TICK);
            water = Math.max(0F, water - SHUTDOWN_COOLING_PER_TICK);
            sustainedLoad = Math.max(0F, sustainedLoad - SUSTAINED_LOAD_PER_TICK);
            return;
        }

        sustainedLoad = clamp(sustainedLoad + (maximumPower ? SUSTAINED_LOAD_PER_TICK : -SUSTAINED_LOAD_PER_TICK));
        float target = demand * (CRUISE_HEAT_SHARE + (1F - CRUISE_HEAT_SHARE) * sustainedLoad);
        oil = approach(oil, demand <= OIL_IDLE_MAX_THROTTLE ? Math.max(target, OIL_IDLE_HEAT) : target, 1F);
        water = approach(water, target, 1F - WATER_MAX_LOAD_SLOWDOWN * sustainedLoad);
    }

    private static float approach(float heat, float target, float riseScale)
    {
        float delta = target - heat;
        // A hot gauge climbs more reluctantly and also sheds its heat more slowly.
        float inertia = delta > 0F ? (0.0015F * (1F - heat) + 0.0003F) * riseScale : 0.00006F * (1F - heat * heat) + 0.000075F;
        return clamp(heat + delta * inertia);
    }

    private static float clamp(float value)
    {
        return Math.max(0F, Math.min(1F, value));
    }
}
