package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ISeat;
import com.wolffsmod.npcs.crew.NpcCrew;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Bots riding in a driveable a player drives: they board its free gunner seats on the player's order, man the seat guns
 * against the enemy, and get off again on order. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotGunners
{
    /** Blocks around the player within which bots answer the order to board. */
    private static final double BOARD_RANGE = 16D;
    /** Blocks within which a gunner looks for a target of its own when its NPC has none. */
    private static final double SEARCH_RANGE = 96D;
    /** Blocks beyond which a gunner gives a target up. */
    private static final double FIRE_RANGE = 160D;
    /** Degrees the gun may point off its target and still fire. */
    private static final double FIRE_CONE = 3D;
    private static final int SEARCH_INTERVAL = 10;

    /** The target each gunner picked for itself, by gunner. */
    private static final Map<UUID, UUID> TARGETS = new HashMap<>();

    /** Forgets the targets gunners picked, once their round is over. */
    static void forget()
    {
        TARGETS.clear();
    }

    /** Aims and fires the seat guns manned by bots of the round. */
    static void tick(ServerLevel level, BotBattle round)
    {
        for (BotSlot slot : round.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof EntityNPCInterface npc && npc.isAlive() && npc.getVehicle() instanceof ISeat seat && seat.getGunType().isPresent())
                man(level, npc, seat);
            else if (entity != null)
                TARGETS.remove(entity.getUUID());
        }
    }

    private static void man(ServerLevel level, EntityNPCInterface npc, ISeat seat)
    {
        Optional<Vec3> origin = seat.getGunOrigin();
        LivingEntity target = origin.map(from -> target(level, npc, from)).orElse(null);
        if (target == null)
        {
            seat.setAutomatedTriggers(false, false);
            return;
        }
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        seat.aimAt(aimPoint);
        Vec3 wanted = aimPoint.subtract(origin.get()).normalize();
        boolean aligned = seat.getGunDirection().map(direction -> direction.dot(wanted) >= Math.cos(Math.toRadians(FIRE_CONE))).orElse(false);
        seat.setAutomatedTriggers(aligned, false);
    }

    /**
     * The NPC's own target when the gun can see it in range, otherwise the nearest enemy in sight it found on its last
     * search. A target behind cover is given up rather than shot through the terrain.
     */
    @Nullable
    private static LivingEntity target(ServerLevel level, EntityNPCInterface npc, Vec3 gun)
    {
        LivingEntity own = npc.getTarget();
        if (own != null && engageable(level, npc, own, gun))
            return own;
        UUID kept = TARGETS.get(npc.getUUID());
        Entity remembered = kept == null ? null : level.getEntity(kept);
        if (npc.tickCount % SEARCH_INTERVAL != 0 && remembered instanceof LivingEntity living && engageable(level, npc, living, gun))
            return living;
        AABB area = npc.getBoundingBox().inflate(SEARCH_RANGE);
        Optional<LivingEntity> nearest = level
            .getEntitiesOfClass(LivingEntity.class, area, candidate -> candidate.isAlive() && BotRelations.areEnemies(npc, candidate) && NpcCrew.shieldingVehicle(candidate).isEmpty()).stream()
            .sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(npc))).filter(candidate -> visible(level, npc, candidate, gun)).findFirst();
        nearest.ifPresentOrElse(found -> TARGETS.put(npc.getUUID(), found.getUUID()), () -> TARGETS.remove(npc.getUUID()));
        return nearest.orElse(null);
    }

    private static boolean engageable(ServerLevel level, EntityNPCInterface npc, LivingEntity target, Vec3 gun)
    {
        return target.isAlive() && target.distanceTo(npc) <= FIRE_RANGE && visible(level, npc, target, gun);
    }

    /** Whether nothing solid stands between the gun and the middle of the target. */
    private static boolean visible(ServerLevel level, EntityNPCInterface npc, LivingEntity target, Vec3 gun)
    {
        ClipContext sight = new ClipContext(gun, target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc);
        return level.clip(sight).getType() == HitResult.Type.MISS;
    }

    /**
     * Orders the player's allied bots nearby into the free gunner seats of the driveable the player drives.
     *
     * @return how many bots boarded, or -1 when the player drives no driveable
     */
    public static int board(ServerPlayer player)
    {
        if (!(player.getVehicle() instanceof ISeat driverSeat) || !driverSeat.isDriverSeat() || driverSeat.getDriveable() == null)
            return -1;
        Entity driveable = driverSeat.getDriveable();
        List<Entity> seats = new ArrayList<>(player.level().getEntities(driveable, driveable.getBoundingBox().inflate(8D),
            entity -> entity instanceof ISeat seat && seat.getDriveable() == driveable && !seat.isDriverSeat() && seat.getRiddenByEntity() == null && seat.getGunType().isPresent()));
        if (seats.isEmpty())
            return 0;
        List<EntityNPCInterface> bots = player.level().getEntitiesOfClass(EntityNPCInterface.class, player.getBoundingBox().inflate(BOARD_RANGE),
            npc -> npc.isAlive() && !npc.isPassenger() && BotTag.read(npc).map(data -> !data.vehicle()).orElse(false) && allied(player, npc));
        List<EntityNPCInterface> nearest = new ArrayList<>(bots);
        nearest.sort(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)));
        int boarded = 0;
        for (int i = 0; i < Math.min(seats.size(), nearest.size()); i++)
        {
            if (nearest.get(i).startRiding(seats.get(i), true))
                boarded++;
        }
        return boarded;
    }

    /**
     * Orders the bots in the driveable the player rides out of it.
     *
     * @return how many bots got off, or -1 when the player rides no driveable
     */
    public static int dismount(ServerPlayer player)
    {
        if (!(player.getVehicle() instanceof ISeat ownSeat) || ownSeat.getDriveable() == null)
            return -1;
        Entity driveable = ownSeat.getDriveable();
        int dismounted = 0;
        for (Entity entity : player.level().getEntities(driveable, driveable.getBoundingBox().inflate(8D), candidate -> candidate instanceof ISeat seat && seat.getDriveable() == driveable))
        {
            Entity rider = ((ISeat) entity).getRiddenByEntity();
            if (rider instanceof EntityNPCInterface npc && BotTag.isBot(npc))
            {
                ((ISeat) entity).setAutomatedTriggers(false, false);
                npc.stopRiding();
                dismounted++;
            }
        }
        return dismounted;
    }

    private static boolean allied(ServerPlayer player, EntityNPCInterface npc)
    {
        return FlansTeams.getTeam(player)
            .flatMap(playerTeam -> FlansTeams.getTeam(npc).flatMap(npcTeam -> FlansTeams.getRunningRound().map(round -> round.getRound().areTeamsAllied(playerTeam, npcTeam)))).orElse(false);
    }
}
