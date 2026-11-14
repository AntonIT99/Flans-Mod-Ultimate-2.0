package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * When each weapon group of the NPC's model may fire next, and how far into its magazine it is. A vehicle NPC can hand a
 * group between its main attack and the guns beside it as its targets change; both read and write the group's state here,
 * so a tank's cannon keeps its firing interval and reload whichever of them fires it. One instance per NPC; server thread.
 */
public final class NpcBankCadence
{
    @Nullable
    private IContentType model;
    private final Group primary = new Group();
    private final Group secondary = new Group();

    /** The firing state of one weapon group. Times are game ticks, possibly fractional. */
    public static final class Group
    {
        private double readyAt = Double.NEGATIVE_INFINITY;
        /** Triggers fired from the current magazine. */
        @Getter
        @Setter
        private int fired;

        /** @return ticks left before the group may fire again, 0 once it may */
        public double remaining(long now)
        {
            return Math.max(0D, readyAt - now);
        }

        /** Adds a delay to what the group already waits, counting from now once it has nothing left to wait. */
        public void delay(long now, double ticks)
        {
            readyAt = Math.max(readyAt, now) + ticks;
        }

        /** Makes the group wait the given ticks from now, at least. */
        public void holdFor(long now, double ticks)
        {
            readyAt = Math.max(readyAt, now + ticks);
        }

        void reset()
        {
            readyAt = Double.NEGATIVE_INFINITY;
            fired = 0;
        }
    }

    /** @return the state of the model's weapon group; a different model starts both groups afresh */
    public Group group(@Nullable IContentType model, boolean secondaryGroup)
    {
        if (model != this.model)
        {
            this.model = model;
            primary.reset();
            secondary.reset();
        }
        return secondaryGroup ? secondary : primary;
    }
}
