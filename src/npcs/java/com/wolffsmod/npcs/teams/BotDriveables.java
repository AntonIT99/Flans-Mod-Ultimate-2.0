package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Battle policy for soldiers using real driveables, independent of generated vehicle NPCs. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotDriveables
{
    private static final double UNLOAD_MARGIN = 16D;
    private static final double COMPATIBLE_DESTINATION = 32D;
    private static final double UNLOAD_SPEED = 0.1D;

    /** The assigned destination for a bot piloting a real aircraft, without requiring a ground path. */
    public static Optional<Vec3> destination(EntityNPCInterface npc)
    {
        return BotBattles.of(npc).isPresent() ? TeamsBots.orderFor(npc).map(BotCommander.Order::spot) : Optional.empty();
    }

    /** Whether this battle permits the seat's job; ordinary NPCs retain their own switches. */
    public static boolean mayUse(EntityNPCInterface npc, ISeat seat)
    {
        if (!BotTag.isBot(npc))
            return true;
        return BotBattles.of(npc).map(battle -> allowed(battle.getSettings(), seat.isDriverSeat(), seat.getGunType().isPresent())).orElse(false);
    }

    /** Whether the battle permits this soldier to ride physical passenger seats. */
    public static boolean allowedPassenger(EntityNPCInterface npc)
    {
        return !BotTag.isBot(npc) || BotBattles.of(npc).map(battle -> battle.getSettings().isRideRealVehicles()).orElse(false);
    }

    static boolean allowed(BotSettings settings, boolean driver, boolean gun)
    {
        if (driver)
            return settings.isDriveRealVehicles();
        return gun ? settings.isGunRealVehicles() : settings.isRideRealVehicles();
    }

    /** Whether automatic recruitment serves the squad's current task without taking all its soldiers off foot duties. */
    public static boolean mayRecruit(EntityNPCInterface npc, ISeat seat)
    {
        if (!BotTag.isBot(npc))
            return true;
        BotBattle battle = BotBattles.of(npc).orElse(null);
        BotSquad squad = squad(npc).orElse(null);
        if (battle == null || squad == null || !mayUse(npc, seat) || npc.getTarget() != null || NpcSuppression.isPinned(npc))
            return false;
        Entity driveable = seat.getDriveable();
        if (driveable == null || !travelling(npc, squad.getTask()) && !followingDriver(npc, driveable, squad) || !seat.isDriverSeat() && !compatible(npc, driveable, squad))
            return false;
        int[] counts = squadCrew(npc, driveable, battle, squad);
        if (counts[0] < 0)
            return false;
        if (!seat.isDriverSeat() && seat.getGunType().isEmpty())
            return transport(npc, driveable);
        return counts[0] < crewLimit(counts[1]);
    }

    /** Counts assigned crew and living members, or returns a negative crew count for another chassis. */
    private static int[] squadCrew(EntityNPCInterface npc, Entity driveable, BotBattle battle, BotSquad squad)
    {
        int assigned = 0;
        int living = 0;
        for (int id : squad.getSlots())
        {
            BotSlot slot = battle.getSlots().get(id);
            Entity entity = slot == null || slot.getEntity() == null ? null : ((ServerLevel) npc.level()).getEntity(slot.getEntity());
            if (!(entity instanceof EntityNPCInterface member) || !member.isAlive() || member.isKilled())
                continue;
            living++;
            if (member != npc)
            {
                int memberCrew = assignedCrew(member, driveable);
                if (memberCrew < 0)
                    return new int[]{-1, living};
                assigned += memberCrew;
            }
        }
        return new int[]{assigned, living};
    }

    /** Returns a negative value if the member has joined a different chassis. */
    private static int assignedCrew(EntityNPCInterface member, Entity driveable)
    {
        ISeat held = NpcDrivers.assignedSeat(member).orElse(null);
        if (held == null)
            return 0;
        if (held.getDriveable() != driveable)
            return -1;
        return held.isDriverSeat() || held.getGunType().isPresent() ? 1 : 0;
    }

    static int crewLimit(int living)
    {
        return Math.max(1, living / 2);
    }

    /** Whether a passenger can travel with its own squad driver or the player its squad follows. */
    public static boolean transport(EntityNPCInterface npc, Entity driveable)
    {
        if (SquadTransports.carries(npc, driveable))
            return true;
        return squad(npc).map(squad -> compatible(npc, driveable, squad) && (travelling(npc, squad.getTask()) || followingDriver(npc, driveable, squad)) && sameSquadOrLeader(npc, driveable, squad))
            .orElse(false);
    }

    /** Whether a bot passenger reached its task or lost its ride assignment and can safely get off. */
    public static boolean unload(EntityNPCInterface npc, ISeat seat)
    {
        Entity driveable = seat.getDriveable();
        if (!BotTag.isBot(npc) || seat.getGunType().isPresent() || driveable == null)
            return false;
        boolean needed = transport(npc, driveable);
        return !needed && FlansEntityTypes.isGrounded(driveable) && driveable.getDeltaMovement().lengthSqr() <= UNLOAD_SPEED * UNLOAD_SPEED;
    }

    private static Optional<BotSquad> squad(EntityNPCInterface npc)
    {
        return BotBattles.of(npc)
            .flatMap(battle -> BotBattles.slotOf(battle, npc).filter(slot -> !slot.isVehicle() && slot.getSource() != BotSlot.Source.COMMANDER).map(slot -> battle.getSquads().get(slot.getSquad())))
            .filter(squad -> !squad.isDisbanded());
    }

    private static boolean travelling(EntityNPCInterface npc, SquadTask task)
    {
        return task != null && task.kind() != SquadTask.Kind.HOLD && npc.position().subtract(task.target()).horizontalDistance() > task.radius() + UNLOAD_MARGIN;
    }

    private static boolean compatible(EntityNPCInterface npc, Entity driveable, BotSquad squad)
    {
        if (sameSquadOrLeader(npc, driveable, squad))
            return true;
        Entity driver = driver(driveable);
        return driver instanceof EntityNPCInterface other && squad(other)
            .map(otherSquad -> otherSquad.getTask() != null && squad.getTask() != null && otherSquad.getTask().target().distanceTo(squad.getTask().target()) <= COMPATIBLE_DESTINATION).orElse(false);
    }

    private static boolean sameSquadOrLeader(EntityNPCInterface npc, Entity driveable, BotSquad squad)
    {
        Entity driver = driver(driveable);
        if (driver == null)
            return false;
        if (driver instanceof EntityNPCInterface other && squad(other).filter(candidate -> candidate == squad).isPresent())
            return true;
        return followingDriver(npc, driveable, squad);
    }

    private static boolean followingDriver(EntityNPCInterface npc, Entity driveable, BotSquad squad)
    {
        Entity driver = driver(driveable);
        if (!(driver instanceof net.minecraft.world.entity.player.Player))
            return false;
        PlayerOrder order = squad.getOrder();
        return driver.getUUID().equals(squad.getLeader())
            || order != null && order.kind() == PlayerOrder.Kind.FOLLOW && driver.getUUID().equals(order.player()) && order.expiresAt() > npc.level().getGameTime();
    }

    private static Entity driver(Entity driveable)
    {
        return driveable.level().getEntities(driveable, driveable.getBoundingBox().inflate(8D), entity -> entity instanceof ISeat seat && seat.getDriveable() == driveable && seat.isDriverSeat())
            .stream().map(entity -> ((ISeat) entity).getRiddenByEntity()).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }
}
