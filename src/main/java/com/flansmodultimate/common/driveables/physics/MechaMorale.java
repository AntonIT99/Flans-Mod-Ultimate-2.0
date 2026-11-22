package com.flansmodultimate.common.driveables.physics;

import lombok.Getter;

/**
 * The Labjac fork's mecha morale, for its horses, camels and elephants: every point of core damage costs a point of
 * morale, which recovers by one a second. A mount whose morale runs out panics and bolts forward, out of its rider's
 * control, until its panic timer has run down, two a second; damage taken while panicking adds a quarter of itself to
 * the timer. Stepped once a second.
 */
@Getter
public final class MechaMorale
{
    /** What a one-second step did, for the sounds it plays. */
    public enum Step
    {
        NONE,
        /** The mount has just started to panic. */
        PANIC_STARTED,
        /** The mount is still panicking. */
        PANICKING
    }

    private int morale;
    private boolean started;
    private float panicTimer;
    private float lastCoreHealth = Float.NaN;

    /** @return whether the mount is panicking and runs out of its rider's control */
    public boolean isPanicking()
    {
        return panicTimer > 0F;
    }

    /**
     * @param maxMorale the type's {@code morale}
     * @param panicTime the type's {@code panicTime}, the panic timer it starts with
     * @param coreHealth the core's health now
     * @return what this second did
     */
    public Step step(int maxMorale, int panicTime, float coreHealth)
    {
        if (!started)
        {
            started = true;
            morale = maxMorale;
        }
        int damage = Float.isNaN(lastCoreHealth) || !(coreHealth < lastCoreHealth) ? 0 : (int) (lastCoreHealth - coreHealth);
        lastCoreHealth = coreHealth;
        boolean wasPanicking = isPanicking();
        if (wasPanicking)
        {
            panicTimer = Math.max(0F, panicTimer - 2F);
            morale = 1;
        }
        if (morale < maxMorale)
            ++morale;
        Step result = wasPanicking ? Step.PANICKING : Step.NONE;
        if (morale <= 0 && !isPanicking())
        {
            panicTimer = Math.max(1, panicTime);
            result = Step.PANIC_STARTED;
        }
        if (damage > 0)
        {
            if (morale > 0)
                morale -= damage;
            if (isPanicking())
                panicTimer += damage * 0.25F;
        }
        return result;
    }
}
