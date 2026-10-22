package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.DriveControl;
import com.flansmodultimate.api.DriveableKind;
import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.IDriveableType;
import com.flansmodultimate.api.ISeat;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * NPCs that drive Flan's driveables, a switch of each NPC. Such an NPC walks to the free driver seat of a vehicle or mecha
 * nearby that no enemy of it rides, gets in, and drives it wherever it would otherwise walk. It fights with the
 * driveable's weapons rather than its own, and gets out again when its switch is turned off or the driveable stays stuck.
 * Planes are left to players. An NPC drawn as a Flan model is a vehicle itself and drives no other, and a crew member
 * keeps its seat. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcDrivers
{
    /** Ticks between searches for a driveable to board. */
    static final int SEARCH_INTERVAL = 20;
    /** Ticks an NPC that got out waits before it boards again. */
    private static final int BOARD_COOLDOWN = 100;
    /** Ticks an NPC leaves a driveable alone that it could not reach or gave up on. */
    private static final int REFUSAL_TICKS = 1200;
    /** Blocks around a driveable within which its seats are looked for. */
    private static final double SEAT_SEARCH = 8D;
    /** Blocks from a driveable's body, or from its driver seat, within which an NPC gets in. */
    private static final double BODY_REACH = 1.5D;
    private static final double SEAT_REACH = 2.5D;

    public static boolean enabled(EntityNPCInterface npc)
    {
        return NpcEquipment.enabled(npc, Feature.DRIVE_DRIVEABLES);
    }

    /** Whether the NPC may board a driveable now: its switch is on, it is alive and on foot, and it is a soldier rather than a vehicle or crew. */
    static boolean mayBoard(EntityNPCInterface npc)
    {
        return !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && !npc.isPassenger() && enabled(npc) && NpcModels.flan(npc) == null && NpcCrew.seat(npc) < 0;
    }

    /** @return the driver seat of a vehicle or mecha the NPC sits in */
    static Optional<ISeat> drivenSeat(EntityNPCInterface npc)
    {
        return npc.getVehicle() instanceof ISeat seat && seat.isDriverSeat() && seat.getDriveable() != null && drivable(seat) ? Optional.of(seat) : Optional.empty();
    }

    /** Whether the NPC leaves its own weapons be because it drives: the driveable's weapons are what it fights with. */
    public static boolean holdsFire(Entity entity)
    {
        return entity instanceof EntityNPCInterface npc && !npc.level().isClientSide && enabled(npc) && drivenSeat(npc).isPresent();
    }

    /** Drives for an NPC at the wheel, or gets it out once its switch is turned off. Runs on the NPC's server tick. */
    public static void tick(EntityNPCInterface npc)
    {
        NpcDriver driver = NpcDriver.of(npc);
        ISeat seat = drivenSeat(npc).orElse(null);
        Entity driveable = seat == null ? null : seat.getDriveable();
        if (seat == null || driveable == null || !npc.isAlive() || npc.isKilled())
        {
            if (driver.isDriving())
            {
                driver.stopDriving();
                driver.setBoardAfter(npc.level().getGameTime() + BOARD_COOLDOWN);
            }
            return;
        }
        if (!enabled(npc))
        {
            // It gets out of a driveable it got into by itself; one it was put in, it only stops driving.
            if (driveable.getUUID().equals(driver.getBoarded()))
                leave(npc, false);
            else if (driver.isDriving())
                release(seat, driver);
            return;
        }
        Optional<Vec3> heading = seat.getDriveDirection();
        IDriveableType type = seat.getDriveableType().orElse(null);
        if (heading.isEmpty() || type == null)
            return;
        if (!driver.drive(npc, seat, driveable, heading.get(), type.getDriveableKind() == DriveableKind.MECHA, type.isTracked()))
            leave(npc, true);
    }

    private static void release(ISeat seat, NpcDriver driver)
    {
        seat.setAutomatedDriving(EnumSet.noneOf(DriveControl.class));
        seat.setAutomatedTriggers(false, false);
        driver.stopDriving();
    }

    /**
     * Gets the NPC out of the driveable it drives; the driveable lets go of the controls once its driver is gone.
     *
     * @param gaveUp
     *            whether it gives the driveable up, which it then leaves alone for a while
     */
    private static void leave(EntityNPCInterface npc, boolean gaveUp)
    {
        NpcDriver driver = NpcDriver.of(npc);
        long now = npc.level().getGameTime();
        Entity driveable = npc.getVehicle() instanceof ISeat seat ? seat.getDriveable() : null;
        if (gaveUp && driveable != null)
            driver.refuse(driveable.getUUID(), now + REFUSAL_TICKS);
        npc.stopRiding();
        driver.stopDriving();
        driver.setBoarded(null);
        driver.setBoardAfter(now + BOARD_COOLDOWN);
    }

    // Boarding

    /** @return the free driver seat, within the NPC's boarding range, of the nearest vehicle or mecha it may take */
    static Optional<Entity> findSeat(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        double range = NpcTuning.of(npc).get(Value.BOARD_RANGE);
        return npc.level().getEntities(npc, npc.getBoundingBox().inflate(range), entity -> entity instanceof ISeat seat && boardable(npc, seat, now)).stream()
            .min(Comparator.comparingDouble(npc::distanceToSqr));
    }

    /** Whether the NPC may take the seat: the free driver seat of a vehicle or mecha that is no wreck, that it did not give up on and that no enemy of it rides. */
    static boolean boardable(EntityNPCInterface npc, ISeat seat, long now)
    {
        Entity driveable = seat.getDriveable();
        return seat.isDriverSeat() && seat.getRiddenByEntity() == null && driveable != null && driveable.isAlive() && !FlansEntityTypes.isWreck(driveable) && drivable(seat)
            && !NpcDriver.of(npc).refuses(driveable.getUUID(), now) && noEnemyAboard(npc, driveable);
    }

    /** Whether the NPC is close enough to the seat's driveable to get in. */
    static boolean withinReach(EntityNPCInterface npc, Entity seat)
    {
        Entity driveable = ((ISeat) seat).getDriveable();
        return npc.distanceTo(seat) <= SEAT_REACH || driveable != null && npc.getBoundingBox().inflate(BODY_REACH).intersects(driveable.getBoundingBox());
    }

    /** Gets the NPC in at the wheel. @return whether it got in */
    static boolean board(EntityNPCInterface npc, Entity seat)
    {
        Entity driveable = ((ISeat) seat).getDriveable();
        if (driveable == null || !npc.startRiding(seat))
            return false;
        NpcDriver.of(npc).setBoarded(driveable.getUUID());
        return true;
    }

    /** Leaves a driveable alone for a while, one the NPC could not reach or get into. */
    static void refuse(EntityNPCInterface npc, @Nullable Entity seat)
    {
        Entity driveable = seat instanceof ISeat driverSeat ? driverSeat.getDriveable() : null;
        if (driveable != null)
            NpcDriver.of(npc).refuse(driveable.getUUID(), npc.level().getGameTime() + REFUSAL_TICKS);
    }

    private static boolean drivable(ISeat seat)
    {
        return seat.getDriveableType().map(type -> type.getDriveableKind() != DriveableKind.PLANE).orElse(false);
    }

    private static boolean noEnemyAboard(EntityNPCInterface npc, Entity driveable)
    {
        for (Entity entity : driveable.level().getEntities(driveable, driveable.getBoundingBox().inflate(SEAT_SEARCH),
            candidate -> candidate instanceof ISeat seat && seat.getDriveable() == driveable))
        {
            Entity rider = ((ISeat) entity).getRiddenByEntity();
            if (rider != null && hostile(npc, rider))
                return false;
        }
        return true;
    }

    /** Whether the NPC and another entity are enemies, by the NPC's faction or the other NPC's, or because it is a monster. */
    private static boolean hostile(EntityNPCInterface npc, Entity other)
    {
        if (other == npc.getTarget())
            return true;
        if (other instanceof Player player)
            return npc.faction.isAggressiveToPlayer(player);
        if (other instanceof EntityNPCInterface rival)
            return npc.faction.isAggressiveToNpc(rival) || rival.faction.isAggressiveToNpc(npc);
        return other instanceof Enemy;
    }
}
