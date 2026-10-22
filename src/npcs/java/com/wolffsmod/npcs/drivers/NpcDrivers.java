package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Claim;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * NPCs that drive Flan's driveables, a switch of each NPC. Such an NPC walks to the free driver seat of a vehicle, mecha
 * or aircraft nearby that no enemy of it rides, gets in, and drives or flies it wherever it would otherwise walk. It
 * fights with the driveable's weapons rather than its own, and gets out again when its switch is turned off or the
 * driveable stays stuck, by parachute from high in the air, as it does from an aircraft about to be lost. Aircraft that
 * fly freely in six degrees and ships built on planes are left to players. An NPC drawn as a Flan model is a vehicle itself
 * and drives no other, and a crew member keeps its seat. Server thread.
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
    /** Blocks above the ground from which an NPC getting out opens a parachute, and ticks it floats down without one. */
    private static final double JUMP_HEIGHT = 4D;
    private static final int FALL_TICKS = 30 * 20;
    /** Share of its hit points under which a driveable is about to be lost: none boards it, and an aircraft's pilot bails out. */
    private static final double BAIL_OUT_HEALTH = 0.2D;

    public static boolean enabled(EntityNPCInterface npc)
    {
        return NpcEquipment.enabled(npc, Feature.DRIVE_DRIVEABLES);
    }

    /** Whether the NPC may board a driveable now: its switch is on, it is alive and on foot, and it is a soldier rather than a vehicle or crew. */
    static boolean mayBoard(EntityNPCInterface npc)
    {
        return !npc.level().isClientSide && npc.isAlive() && !npc.isKilled() && !npc.isPassenger() && enabled(npc) && NpcModels.flan(npc) == null && NpcCrew.seat(npc) < 0;
    }

    /** @return the driver seat of a driveable NPCs drive that the NPC sits in */
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
            stopped(npc, driver);
        else if (!enabled(npc))
            switchedOff(npc, seat, driveable, driver);
        else if (!operate(npc, seat, driveable, driver))
            leave(npc, true);
    }

    /** Forgets driving once the NPC no longer sits at the controls; thrown out of an aircraft that broke up, it opens its parachute. */
    private static void stopped(EntityNPCInterface npc, NpcDriver driver)
    {
        if (!driver.isDriving())
            return;
        if (npc.isAlive() && !npc.isPassenger())
            descend(npc);
        driver.stopDriving();
        driver.setBoardAfter(npc.level().getGameTime() + BOARD_COOLDOWN);
    }

    /** It gets out of a driveable it got into by itself; one it was put in, it only stops driving. */
    private static void switchedOff(EntityNPCInterface npc, ISeat seat, Entity driveable, NpcDriver driver)
    {
        if (driveable.getUUID().equals(driver.getBoarded()))
            leave(npc, false);
        else if (driver.isDriving())
            release(seat, driver);
    }

    /**
     * Drives or flies for one tick.
     *
     * @return whether the NPC keeps the driveable: false once it gives it up, stuck, or bails out of an aircraft about to be
     *         lost while there is height to open a parachute
     */
    private static boolean operate(EntityNPCInterface npc, ISeat seat, Entity driveable, NpcDriver driver)
    {
        Optional<Vec3> heading = seat.getDriveDirection();
        IDriveableType type = seat.getDriveableType().orElse(null);
        if (heading.isEmpty() || type == null)
            return true;
        if (type.getDriveableKind() != DriveableKind.PLANE)
            return driver.drive(npc, seat, driveable, heading.get(), type.getDriveableKind() == DriveableKind.MECHA, type.isTracked());
        boolean bailOut = failing(driveable) && height(npc) > JUMP_HEIGHT;
        return !bailOut && driver.fly(npc, seat, driveable, FlansEntityTypes.getAircraftMode(driveable).orElse(AircraftMode.FIXED_WING));
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
        if (driveable != null)
            note(npc, gaveUp ? "gives up" : "gets out of", driveable);
        npc.stopRiding();
        descend(npc);
        driver.stopDriving();
        driver.setBoarded(null);
        driver.setBoardAfter(now + BOARD_COOLDOWN);
    }

    /** Opens a parachute over an NPC that got out high above the ground; without a parachute tool, it floats down. */
    private static void descend(EntityNPCInterface npc)
    {
        if (height(npc) > JUMP_HEIGHT && !FlansEquipment.deployParachute(npc))
            npc.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FALL_TICKS));
    }

    /** @return blocks the entity is above the ground under it */
    private static double height(Entity entity)
    {
        return entity.getY() - entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.getBlockX(), entity.getBlockZ());
    }

    /** Whether a driveable has so few hit points left that it is about to be lost. */
    private static boolean failing(Entity driveable)
    {
        return FlansEntityTypes.getHealthShare(driveable).orElse(1D) < BAIL_OUT_HEALTH;
    }

    // Boarding

    /** @return the free driver seat, within the NPC's boarding range, of the nearest driveable it may take */
    static Optional<Entity> findSeat(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        double range = NpcTuning.of(npc).get(Value.BOARD_RANGE);
        return npc.level().getEntities(npc, npc.getBoundingBox().inflate(range), entity -> entity instanceof ISeat seat && boardable(npc, seat, now)).stream()
            .min(Comparator.comparingDouble(npc::distanceToSqr));
    }

    /**
     * Whether the NPC may take the seat: the free driver seat of a driveable NPCs drive, that is neither a wreck nor about
     * to be lost, that its choice of driveables allows, that it did not give up on and that no enemy of it rides.
     */
    static boolean boardable(EntityNPCInterface npc, ISeat seat, long now)
    {
        Entity driveable = seat.getDriveable();
        return seat.isDriverSeat() && seat.getRiddenByEntity() == null && driveable != null && driveable.isAlive() && !FlansEntityTypes.isWreck(driveable) && drivable(seat) && !failing(driveable)
            && mayTake(npc, driveable) && !NpcDriver.of(npc).refuses(driveable.getUUID(), now) && noEnemyAboard(npc, driveable);
    }

    /** Whether the NPC may take the driveable by who owns it and whether it is locked, as its choice of driveables says. */
    private static boolean mayTake(EntityNPCInterface npc, Entity driveable)
    {
        Optional<UUID> owner = FlansEntityTypes.getOwner(driveable);
        return mayTake(NpcTuning.of(npc).claim(), FlansEntityTypes.isLocked(driveable), owner.isPresent(), owner.map(id -> alliedOwner(npc, id)).orElse(false));
    }

    /**
     * The rule alone: a driveable locked with a key is never taken; otherwise it is taken as the choice says, from what is
     * known of its owner.
     *
     * @param owned
     *            whether a player owns it
     * @param ownerAllied
     *            whether its owner is on the NPC's side
     */
    static boolean mayTake(Claim claim, boolean locked, boolean owned, boolean ownerAllied)
    {
        if (locked)
            return false;
        return switch (claim)
        {
            case ANY -> true;
            case UNOWNED -> !owned;
            case ALLIED -> !owned || ownerAllied;
        };
    }

    /**
     * Whether the player who owns a driveable is on the NPC's side: the NPC follows them, they play on its Teams team, or
     * its faction is friendly to them. An owner who is not about is not.
     */
    private static boolean alliedOwner(EntityNPCInterface npc, UUID owner)
    {
        Player player = npc.level().getPlayerByUUID(owner);
        if (player == null)
            return false;
        if (npc.getOwner() == player)
            return true;
        Optional<ITeam> team = FlansTeams.getTeam(player);
        return team.isPresent() && team.equals(FlansTeams.getTeam(npc)) || npc.faction.isFriendlyToPlayer(player);
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
        note(npc, "gets into", driveable);
        return true;
    }

    /** Leaves a driveable alone for a while, one the NPC could not reach or get into. */
    static void refuse(EntityNPCInterface npc, @Nullable Entity seat)
    {
        Entity driveable = seat instanceof ISeat driverSeat ? driverSeat.getDriveable() : null;
        if (driveable == null)
            return;
        NpcDriver.of(npc).refuse(driveable.getUUID(), npc.level().getGameTime() + REFUSAL_TICKS);
        note(npc, "cannot reach", driveable);
    }

    /** Tells the server log what an NPC with the movement debug switch did with a driveable. */
    private static void note(EntityNPCInterface npc, String what, Entity driveable)
    {
        if (NpcEquipment.enabled(npc, Feature.PATH_DEBUG) && NpcsMod.log.isInfoEnabled())
            NpcsMod.log.info("NPC {} id={} {} {} at {}", npc.getName().getString(), npc.getId(), what, driveable.getName().getString(), driveable.blockPosition().toShortString());
    }

    /** Whether NPCs drive the seat's driveable: any but an aircraft that flies freely in six degrees and a ship built on a plane. */
    private static boolean drivable(ISeat seat)
    {
        IDriveableType type = seat.getDriveableType().orElse(null);
        Entity driveable = seat.getDriveable();
        if (type == null || driveable == null)
            return false;
        if (type.getDriveableKind() != DriveableKind.PLANE)
            return true;
        return !type.isShip() && FlansEntityTypes.getAircraftMode(driveable).map(mode -> mode != AircraftMode.SIX_DOF).orElse(false);
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
