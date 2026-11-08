package com.wolffsmod.npcs.sim;

/**
 * How long an NPC has been in contact with a target it can fight, in the NPC's own ticks. A short break, such as a
 * target slipping out of sight for a moment, leaves the count running rather than starting it again. Not saved: contact
 * begins afresh with the NPC's state. One instance per NPC, server side.
 */
public final class NpcContact
{
    /** Ticks without a target within which contact still counts as unbroken. */
    static final int GRACE_TICKS = 40;
    /** The tick value of contact that never began, or that has been given up. */
    private static final int NEVER = Integer.MIN_VALUE;

    /** The tick contact began. */
    private int since = NEVER;
    /** The tick contact was last seen, which a break is measured from. */
    private int seen = NEVER;

    /**
     * Records whether the NPC is in contact as of this tick.
     *
     * @param now
     *            the NPC's tick count
     * @param contact
     *            whether it holds a target it can fight
     */
    public void tick(int now, boolean contact)
    {
        if (contact)
        {
            if (since == NEVER)
                since = now;
            seen = now;
        }
        else if (since != NEVER && now - seen > GRACE_TICKS)
            since = NEVER;
    }

    /**
     * @param now
     *            the NPC's tick count
     * @return ticks the NPC has held contact, 0 while it holds none
     */
    public int heldFor(int now)
    {
        return since == NEVER ? 0 : Math.max(0, now - since);
    }
}
