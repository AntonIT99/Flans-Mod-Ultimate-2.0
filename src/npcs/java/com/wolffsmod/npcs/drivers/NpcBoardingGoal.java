package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.ISeat;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Walks an NPC that drives Flan's driveables to the free driver seat of a vehicle, mecha or aircraft nearby and gets it
 * in. It comes before every other goal that moves the NPC, those of a fight included, and gives a driveable it cannot
 * reach in time up for a while. Server thread.
 */
@SuppressWarnings("unused") // Made only by the Custom NPCs entity mixin, which IntelliJ does not follow.
public final class NpcBoardingGoal extends Goal
{
    /** Ticks the NPC has to reach the driveable. */
    private static final int REACH_TICKS = 400;
    /** Ticks between path searches toward a driveable, which may be moving. */
    private static final int PATH_INTERVAL = 20;

    private final EntityNPCInterface npc;
    @Nullable
    private Entity seat;
    private int ticks;
    private boolean boarded;

    public NpcBoardingGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        if (!NpcDrivers.mayBoard(npc))
            return false;
        NpcDriver driver = NpcDriver.of(npc);
        long now = npc.level().getGameTime();
        if (now < driver.getNextSearch() || now < driver.getBoardAfter())
            return false;
        driver.setNextSearch(now + NpcDrivers.SEARCH_INTERVAL);
        seat = NpcDrivers.findSeat(npc).orElse(null);
        return seat != null;
    }

    @Override
    public boolean canContinueToUse()
    {
        return seat != null && !boarded && ticks < REACH_TICKS && NpcDrivers.mayBoard(npc) && seat.isAlive() && NpcDrivers.boardable(npc, (ISeat) seat, npc.level().getGameTime());
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void start()
    {
        ticks = 0;
        boarded = false;
        walk();
    }

    @Override
    public void tick()
    {
        if (seat == null)
            return;
        ticks++;
        npc.getLookControl().setLookAt(seat, 30F, 30F);
        if (NpcDrivers.withinReach(npc, seat))
        {
            boarded = NpcDrivers.board(npc, seat);
            if (!boarded)
                NpcDrivers.refuse(npc, seat);
            seat = boarded ? seat : null;
            return;
        }
        if (ticks % PATH_INTERVAL == 0)
            walk();
    }

    /** Heads for the driveable; one there is no path to is given up straight away. */
    private void walk()
    {
        if (seat != null && !NpcDrivers.withinReach(npc, seat) && !npc.getNavigation().moveTo(seat, 1D))
            ticks = REACH_TICKS;
    }

    @Override
    public void stop()
    {
        // Once aboard, the path to the driveable would only hold it where it stands.
        npc.getNavigation().stop();
        if (!boarded && ticks >= REACH_TICKS)
            NpcDrivers.refuse(npc, seat);
        seat = null;
        ticks = 0;
        boarded = false;
    }
}
