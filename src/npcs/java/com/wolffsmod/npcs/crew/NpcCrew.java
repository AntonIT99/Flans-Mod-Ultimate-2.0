package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.equipment.FlansMelee;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.names.NpcDisplayNames;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.sim.NpcParachutes;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.soldier.FlanSoldierLoadout;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.CustomEntities;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Crews of vehicle NPCs: NPCs riding a vehicle, plane or mecha NPC in the seats of its model. Crew members sit where the
 * model's seats are and face along their seat; the vehicle drives and fires by itself, so its driver and gunners hold
 * their own fire, and passengers fight with their weapons only when the vehicle allows it. Crew may be shielded while
 * aboard, in which case attackers aiming at them aim at the vehicle instead. When the vehicle is destroyed its crew get
 * out, die or vanish as it says, and a vehicle that is deleted takes its crew with it.
 *
 * <p>
 * A crew member keeps its seat in synchronized entity data, so both sides seat it alike. A rider without a seat of its
 * own, such as one put on with Custom NPCs' mounter, takes the first free seat, the driver's first. A player on the
 * vehicle's side gets into the empty seat they click, or a free gunner's seat, else a passenger seat, else the driver's seat when using its body, moves to the next free seat with
 * the seat key, mans the gun of a gunner's seat, and gets out when the vehicle is destroyed or they log out. Server
 * thread unless a method says otherwise.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcCrew
{
    public static final int NO_SEAT = -1;
    /** Custom NPCs' respawn setting for an NPC that stays dead. */
    private static final int STAYS_DEAD = 3;
    /** Custom NPCs' moving type (not its navigation type) for an NPC that stands where it is when idle. */
    private static final int STANDING = 0;
    /** Ticks between checks whether a vehicle that crews itself needs a crew. */
    private static final int AUTO_INTERVAL = 10;
    /** Degrees a seated crew member may turn its head away from where its seat faces. */
    private static final float HEAD_TURN = 75F;
    /** Blocks beyond the side of the vehicle at which a crew member bails out. */
    private static final double BAIL_OUT_MARGIN = 1.5D;
    /** Blocks above the ground from which crew bail out by parachute rather than climbing down. */
    private static final double JUMP_HEIGHT = 4D;
    /** Ticks of slow falling a crew member gets when no parachute tool is loaded. */
    private static final int FALL_TICKS = 30 * 20;

    /** A seat of a vehicle's model, with the post it is. */
    public record Seat(WeaponMount mount, CrewPost post)
    {
        public int index()
        {
            return mount.seat();
        }
    }

    /** Makes the NPC that takes a seat, already in the level; empty leaves the seat empty. */
    @FunctionalInterface
    public interface Recruiter
    {
        Optional<EntityNPCInterface> recruit(ServerLevel level, EntityNPCInterface vehicle, Seat seat);
    }

    // Seats, either side

    /** @return the seats of the vehicle's model in seat order; none for an NPC that is no vehicle, plane or mecha */
    public static List<Seat> seats(EntityNPCInterface vehicle)
    {
        return NpcVehicleState.of(vehicle).crewSeats(vehicle).stream().map(mount -> new Seat(mount, CrewPost.of(mount))).toList();
    }

    /** @return the seat a crew member was given, or -1 */
    public static int seat(Entity entity)
    {
        return entity instanceof NpcCrewHolder holder ? holder.wolffsmodnpcsCrewSeat() : NO_SEAT;
    }

    /**
     * @return the seat a rider of the vehicle sits in: the one it was given or moved to, unless an earlier rider holds it,
     *         or else the first seat no rider holds that it may take, the driver's first, in the order the riders got on. A
     *         player takes a gunner's seat before a passenger seat, and the driver's seat last.
     */
    public static Optional<WeaponMount> seatOf(EntityNPCInterface vehicle, Entity rider)
    {
        List<WeaponMount> mounts = NpcVehicleState.of(vehicle).crewSeats(vehicle);
        if (mounts.isEmpty())
            return Optional.empty();
        Map<Integer, Entity> claimed = new HashMap<>();
        for (Entity passenger : vehicle.getPassengers())
            if (mount(mounts, held(vehicle, passenger)) != null)
                claimed.putIfAbsent(held(vehicle, passenger), passenger);
        if (claimed.get(held(vehicle, rider)) == rider)
            return Optional.ofNullable(mount(mounts, held(vehicle, rider)));
        List<WeaponMount> free = new ArrayList<>(mounts.stream().filter(mount -> !claimed.containsKey(mount.seat())).toList());
        for (Entity passenger : vehicle.getPassengers())
        {
            if (claimed.get(held(vehicle, passenger)) == passenger)
                continue;
            WeaponMount next = free.stream().min(Comparator.comparingInt(mount -> rank(passenger, mount))).orElse(null);
            if (next != null)
                free.remove(next);
            if (passenger == rider)
                return Optional.ofNullable(next);
        }
        return Optional.empty();
    }

    /** @return the seat a rider holds: a crew member's own, or the one a rider without one moved to; else -1 */
    private static int held(EntityNPCInterface vehicle, Entity rider)
    {
        int seat = seat(rider);
        if (seat >= 0 || !(vehicle instanceof NpcCrewHolder holder))
            return seat;
        CompoundTag seats = holder.wolffsmodnpcsRiderSeats();
        String id = rider.getStringUUID();
        return id != null && seats.contains(id, Tag.TAG_INT) ? seats.getInt(id) : NO_SEAT;
    }

    /**
     * Moves a player riding the vehicle to its next free seat after theirs, in seat order and wrapping round, as the seat
     * key does in a Flan's driveable. The vehicle still drives itself when the player sits in its driver's seat. Every other rider
     * without a crew seat keeps the seat it sits in. Server side.
     *
     * @return whether the player changed seats
     */
    public static boolean changeSeat(EntityNPCInterface vehicle, Player player)
    {
        if (player.getVehicle() != vehicle || !(vehicle instanceof NpcCrewHolder holder) || !vehicle.isAlive() || vehicle.isKilled())
            return false;
        List<WeaponMount> mounts = NpcVehicleState.of(vehicle).crewSeats(vehicle);
        WeaponMount current = seatOf(vehicle, player).orElse(null);
        if (current == null)
            return false;
        int from = mounts.indexOf(current);
        for (int step = 1; step < mounts.size(); step++)
        {
            WeaponMount next = mounts.get((from + step) % mounts.size());
            if (occupied(vehicle, next.seat()))
                continue;
            // Pin every rider without a crew seat where it sits, so that none slides into the seat the player leaves.
            CompoundTag seats = new CompoundTag();
            pinRiders(vehicle, seats);
            seats.putInt(player.getStringUUID(), next.seat());
            holder.wolffsmodnpcsRiderSeats(seats);
            return true;
        }
        return false;
    }

    /**
     * @return how readily a rider without a seat of its own takes a seat, lowest first: a player takes a gunner's seat
     *         before a passenger seat and the driver's last, anyone else any seat alike
     */
    private static int rank(Entity rider, WeaponMount mount)
    {
        if (!(rider instanceof Player))
            return 0;
        return switch (CrewPost.of(mount))
        {
            case DRIVER -> 2;
            case GUNNER -> 0;
            case PASSENGER -> 1;
        };
    }

    /** @return the seat whose gun a player mans: the gunner's seat they sit in on a vehicle NPC. Either side. */
    public static Optional<WeaponMount> mannedGun(Player player)
    {
        if (!(player.getVehicle() instanceof EntityNPCInterface vehicle))
            return Optional.empty();
        return seatOf(vehicle, player).filter(mount -> CrewPost.of(mount) == CrewPost.GUNNER);
    }

    @Nullable
    private static WeaponMount mount(List<WeaponMount> mounts, int seat)
    {
        for (WeaponMount mount : mounts)
            if (mount.seat() == seat)
                return mount;
        return null;
    }

    /** @return the vehicle an NPC crews, when it rides one in a seat it was given */
    public static Optional<EntityNPCInterface> vehicleOf(Entity entity)
    {
        if (seat(entity) < 0 || !(entity.getVehicle() instanceof EntityNPCInterface vehicle) || NpcVehicleState.of(vehicle).crewSeats(vehicle).isEmpty())
            return Optional.empty();
        return Optional.of(vehicle);
    }

    /** Rendering policy only: hidden crew remain aboard and become visible again as soon as they leave their seat. */
    public static boolean hiddenAboard(Entity entity)
    {
        return vehicleOf(entity).flatMap(vehicle -> seatOf(vehicle, entity).map(mount -> !NpcCrewSettings.of(vehicle).isSeatVisible(mount.seat()))).orElse(false);
    }

    /** @return the living crew member sitting in a seat of the vehicle */
    public static Optional<EntityNPCInterface> occupant(EntityNPCInterface vehicle, int seat)
    {
        return crew(vehicle).stream().filter(member -> member.isAlive() && !member.isKilled() && seatOf(vehicle, member).map(WeaponMount::seat).orElse(NO_SEAT) == seat).findFirst();
    }

    /**
     * @return whether the vehicle has room for another rider, for a player any free seat including the driver's; empty for
     *         an NPC without seats, which takes one rider
     */
    public static Optional<Boolean> canCarry(EntityNPCInterface vehicle, Entity rider)
    {
        List<WeaponMount> mounts = NpcVehicleState.of(vehicle).crewSeats(vehicle);
        if (mounts.isEmpty())
            return Optional.empty();
        if (rider instanceof Player)
            return Optional.of(mounts.stream().anyMatch(mount -> !occupied(vehicle, mount.seat())));
        return Optional.of(vehicle.getPassengers().size() < mounts.size());
    }

    /**
     * Seats a player in the empty seat they click on a vehicle that is on their side, without sneaking. Using the body
     * instead takes a free gunner's, passenger or driver's seat, in that order.
     * Server side.
     *
     * @return whether the player got on; otherwise the vehicle answers them as any NPC would
     */
    public static boolean carry(EntityNPCInterface vehicle, Player player)
    {
        if (player.isSecondaryUseActive() || player.isSpectator() || player.getVehicle() == vehicle || !vehicle.isAlive() || vehicle.isKilled() || !NpcDrivers.ally(vehicle, player)
            || !canCarry(vehicle, player).orElse(false))
            return false;
        NpcSeatInteraction.Hit clicked = NpcSeatInteraction.pick(vehicle, player).orElse(null);
        if (clicked != null && (occupied(vehicle, clicked.mount().seat()) || !(vehicle instanceof NpcCrewHolder)))
            return false;
        player.stopRiding();
        CompoundTag seats = null;
        if (vehicle instanceof NpcCrewHolder holder)
        {
            seats = holder.wolffsmodnpcsRiderSeats().copy();
            // Keep existing riders in place when a later rider chooses a seat ahead of them in the seat order.
            if (clicked != null)
                pinRiders(vehicle, seats);
            seats.remove(player.getStringUUID());
            if (clicked != null)
                seats.putInt(player.getStringUUID(), clicked.mount().seat());
        }
        if (!player.startRiding(vehicle, true))
            return false;
        if (vehicle instanceof NpcCrewHolder holder)
            holder.wolffsmodnpcsRiderSeats(seats);
        return true;
    }

    /** Keeps riders without an assigned crew seat at their current positions before another rider chooses a seat. */
    private static void pinRiders(EntityNPCInterface vehicle, CompoundTag seats)
    {
        for (Entity passenger : vehicle.getPassengers())
            if (seat(passenger) < 0)
                seatOf(vehicle, passenger).ifPresent(mount -> seats.putInt(passenger.getStringUUID(), mount.seat()));
    }

    /**
     * Puts a player leaving the server off the vehicle NPC they ride. A mount whose only player rider logs out would
     * otherwise leave the world with its whole crew, saved with that player, until they come back. Server side, before the
     * player is saved.
     */
    public static void loggingOut(Player player)
    {
        if (player.getVehicle() instanceof EntityNPCInterface vehicle && !NpcVehicleState.of(vehicle).crewSeats(vehicle).isEmpty())
            player.stopRiding();
    }

    /**
     * Puts a rider in its seat and turns a crew member to face along it. Either side.
     *
     * @return whether the rider was placed; otherwise it rides as on any mob
     */
    public static boolean positionRider(EntityNPCInterface vehicle, Entity passenger, Entity.MoveFunction move)
    {
        boolean crew = seat(passenger) >= 0;
        // A player always sits in the seat they got into.
        if (!crew && !(passenger instanceof Player) && !((NpcWeaponSettings) vehicle.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.RIDER_AT_SEAT))
            return false;
        NpcVehicleState state = NpcVehicleState.of(vehicle);
        Optional<WeaponMount> seat = seatOf(vehicle, passenger);
        Optional<Vec3> offset = seat.flatMap(mount -> state.seatOffset(vehicle, mount));
        if (offset.isEmpty())
            return false;
        Vec3 at = offset.get().add(NpcPlatform.riderOffset(passenger, vehicle));
        move.accept(passenger, vehicle.getX() + at.x, vehicle.getY() + at.y, vehicle.getZ() + at.z);
        if (crew && passenger instanceof LivingEntity living)
        {
            if (CrewPost.of(seat.get()) == CrewPost.DRIVER)
            {
                // The driver looks where the vehicle looks, so it appears to steer it
                face(living, state.seatFrameYaw(seat.get()), vehicle.getYHeadRot());
                living.setXRot(vehicle.getXRot());
            }
            else if (seat.get().gun().isPresent())
            {
                // A gunner looks along its gun, as a player manning it does
                face(living, state.seatBodyYaw(seat.get()), state.seatFacing(seat.get()));
                living.setXRot(state.seatAim(seat.get().seat()).pitch());
            }
            else
                face(living, state.seatBodyYaw(seat.get()), living.getYHeadRot());
        }
        return true;
    }

    private static void face(LivingEntity crew, float facing, float look)
    {
        float body = crew.yBodyRot + Mth.wrapDegrees(facing - crew.yBodyRot);
        crew.setYBodyRot(body);
        float head = body + Mth.clamp(Mth.wrapDegrees(look - body), -HEAD_TURN, HEAD_TURN);
        crew.setYHeadRot(head);
        crew.setYRot(head);
    }

    // Conduct aboard

    /** Whether a crew member leaves the fighting to its vehicle: a driver or gunner, or a passenger the vehicle keeps quiet. */
    public static boolean holdsFire(Entity entity)
    {
        Optional<EntityNPCInterface> vehicle = vehicleOf(entity);
        if (vehicle.isEmpty())
            return false;
        boolean passenger = seatOf(vehicle.get(), entity).map(mount -> CrewPost.of(mount) == CrewPost.PASSENGER).orElse(false);
        return !passenger || !NpcCrewSettings.of(vehicle.get()).isPassengersFire();
    }

    /** @return the vehicle a shielded crew member rides, which attackers aim at instead; empty for anyone else */
    public static Optional<EntityNPCInterface> shieldingVehicle(@Nullable Entity entity)
    {
        return entity == null ? Optional.empty() : vehicleOf(entity).filter(vehicle -> NpcCrewSettings.of(vehicle).isProtectedCrew() && vehicle.isAlive() && !vehicle.isKilled());
    }

    /** Whether a crew member takes no harm from a hit: it is shielded and the hit is not one that ignores invulnerability. */
    public static boolean shielded(Entity entity, DamageSource source)
    {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && shieldingVehicle(entity).isPresent();
    }

    /** Protected crew forward melee to their vehicle, whose own threshold and resistances decide the result. */
    public static boolean shielded(Entity entity, DamageSource source, float amount)
    {
        if (!shielded(entity, source))
            return false;
        if (FlansMelee.isMelee(source) && source.getEntity() != entity)
            shieldingVehicle(entity).ifPresent(vehicle -> vehicle.hurt(source, amount));
        return true;
    }

    /**
     * @return whom an NPC targets when asked to target someone: nobody while it holds its fire as crew, and the vehicle of a
     *         shielded crew member rather than the crew member
     */
    @Nullable
    public static LivingEntity target(EntityNPCInterface npc, @Nullable LivingEntity target)
    {
        if (target == null || npc.level().isClientSide)
            return target;
        if (holdsFire(npc))
            return null;
        EntityNPCInterface vehicle = shieldingVehicle(target).orElse(null);
        return vehicle == null || vehicle == npc || vehicle.hasPassenger(npc) ? target : vehicle;
    }

    // Crewing

    /**
     * Fills the empty seats of a vehicle with what the recruiter makes, seat by seat in seat order. A recruit that cannot
     * get on is removed again.
     *
     * @return how many crew members got on
     */
    public static int fill(ServerLevel level, EntityNPCInterface vehicle, Recruiter recruiter)
    {
        if (!vehicle.isAlive() || vehicle.isKilled())
            return 0;
        int boarded = 0;
        for (Seat seat : seats(vehicle))
        {
            EntityNPCInterface recruit = occupied(vehicle, seat.index()) ? null : recruiter.recruit(level, vehicle, seat).orElse(null);
            if (recruit != null && board(vehicle, recruit, seat.index()))
                boarded++;
            else if (recruit != null)
                recruit.delete();
        }
        return boarded;
    }

    /** @return whether any rider, including a player, occupies this model seat; either side */
    public static boolean occupied(EntityNPCInterface vehicle, int seat)
    {
        return vehicle.getPassengers().stream().anyMatch(passenger -> seatOf(vehicle, passenger).map(WeaponMount::seat).orElse(NO_SEAT) == seat);
    }

    /** Seats an NPC that is already in the level as crew in a seat of the vehicle. */
    public static boolean board(EntityNPCInterface vehicle, EntityNPCInterface crew, int seat)
    {
        NpcCrewHolder holder = (NpcCrewHolder) crew;
        holder.wolffsmodnpcsCrewSeat(seat);
        crew.moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), vehicle.getYRot(), 0F);
        if (crew.startRiding(vehicle, true))
            return true;
        holder.wolffsmodnpcsCrewSeat(NO_SEAT);
        return false;
    }

    /**
     * Removes the crew members aboard a vehicle.
     *
     * @return how many were removed
     */
    public static int dismiss(EntityNPCInterface vehicle)
    {
        List<EntityNPCInterface> crew = crew(vehicle);
        crew.forEach(EntityNPCInterface::delete);
        return crew.size();
    }

    /** @return the NPCs aboard the vehicle in seats they were given */
    public static List<EntityNPCInterface> crew(EntityNPCInterface vehicle)
    {
        List<EntityNPCInterface> crew = new ArrayList<>();
        for (Entity passenger : vehicle.getPassengers())
            if (passenger instanceof EntityNPCInterface npc && seat(npc) >= 0)
                crew.add(npc);
        return crew;
    }

    /** Crews a vehicle that crews itself, once each time it spawns or comes back. Runs on the vehicle's server tick. */
    public static void tick(EntityNPCInterface vehicle)
    {
        if (vehicle.tickCount % AUTO_INTERVAL != 0 || !(vehicle.level() instanceof ServerLevel level) || vehicle.isKilled() || !vehicle.isAlive())
            return;
        NpcCrewSettings settings = NpcCrewSettings.of(vehicle);
        NpcCrewHolder holder = (NpcCrewHolder) vehicle;
        if (!settings.isAutoCrew() || !settings.namesAnyone() || vehicle.getUUID().equals(holder.wolffsmodnpcsCrewedAs()) || seats(vehicle).isEmpty())
            return;
        holder.wolffsmodnpcsCrewedAs(vehicle.getUUID());
        fill(level, vehicle, recruiter(settings));
    }

    /** @return the recruiter that makes crew from the vehicle's own settings */
    public static Recruiter recruiter(NpcCrewSettings settings)
    {
        return (level, vehicle, seat) -> recruit(level, vehicle, settings.source(seat.index(), seat.post()));
    }

    /** Makes a crew member from a source beside the vehicle, of the vehicle's faction and staying dead once killed. */
    public static Optional<EntityNPCInterface> recruit(ServerLevel level, EntityNPCInterface vehicle, CrewSource source)
    {
        EntityNPCInterface npc = switch (source.kind())
        {
            case NONE -> null;
            case CLONE -> fromClone(level, vehicle.position(), source);
            case CLASS -> fromClass(level, vehicle.position(), source);
        };
        if (npc == null)
            return Optional.empty();
        npc.setFaction(vehicle.faction.id);
        npc.stats.spawnCycle = STAYS_DEAD;
        npc.ais.returnToStart = false;
        npc.ais.setMovingType(STANDING);
        npc.ais.setStartPos(vehicle.blockPosition());
        npc.updateClient = true;
        npc.updateAI = true;
        return Optional.of(npc);
    }

    @Nullable
    private static EntityNPCInterface fromClone(ServerLevel level, Vec3 position, CrewSource source)
    {
        if (!source.names())
            return null;
        try
        {
            IEntity<?> spawned = NpcAPI.Instance().getClones().spawn(position.x, position.y, position.z, source.cloneTab(), source.cloneName(), NpcAPI.Instance().getIWorld(level));
            if (spawned != null && spawned.getMCEntity() instanceof EntityNPCInterface npc)
                return npc;
            if (spawned != null)
                spawned.getMCEntity().discard();
        }
        // Custom NPCs throws for a clone that does not exist.
        catch (RuntimeException e)
        {
            NpcsMod.log.warn("Cannot spawn clone {} of tab {} as vehicle crew: {}", source.cloneName(), source.cloneTab(), e.getMessage());
        }
        return null;
    }

    @Nullable
    private static EntityNPCInterface fromClass(ServerLevel level, Vec3 position, CrewSource source)
    {
        FlanSoldierSelection selection = FlanSoldierSelection.resolve(source.team(), source.playerClass()).orElse(null);
        if (selection == null)
        {
            NpcsMod.log.warn("Cannot crew a vehicle with unknown class {} of team {}", source.playerClass(), source.team());
            return null;
        }
        selection = new FlanSoldierSelection(selection.team(), selection.playerClass(), source.chooseSkin(selection.playerClass().getSkinOverrideVariants().size(), level.random));
        EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, level);
        npc.moveTo(position.x, position.y, position.z, 0F, 0F);
        FlanSoldierLoadout.apply(npc, selection);
        // Only clients know the class skin and whether it needs the 64x32 model, so each client puts it on.
        if (!selection.playerClass().getSkinOverrideVariants().isEmpty())
        {
            selection.write(npc.modelData.extra);
            NpcSkins.markClassSkin(npc);
        }
        NpcDisplayNames.bindName(npc.display, selection.playerClass());
        level.addFreshEntity(npc);
        npc.setHealth(npc.getMaxHealth());
        return npc;
    }

    // The vehicle's end

    /** Deals with the crew of a vehicle that has just been destroyed, as its settings say. Runs as it dies. */
    public static void destroyed(EntityNPCInterface vehicle)
    {
        ((NpcCrewHolder) vehicle).wolffsmodnpcsCrewedAs(null);
        // Nobody rides on in a wreck, which may stay where it fell.
        vehicle.getPassengers().stream().filter(Player.class::isInstance).toList().forEach(Entity::stopRiding);
        List<EntityNPCInterface> crew = crew(vehicle);
        if (crew.isEmpty())
            return;
        switch (NpcCrewSettings.of(vehicle).getDestroyed())
        {
            case BAIL_OUT -> crew.forEach(member -> bailOut(vehicle, member));
            case DIE -> crew.forEach(LivingEntity::kill);
            case VANISH -> crew.forEach(EntityNPCInterface::delete);
        }
    }

    /** Takes the crew of a vehicle that is deleted, rather than killed, with it. */
    public static void deleted(EntityNPCInterface vehicle)
    {
        if (!vehicle.level().isClientSide)
            crew(vehicle).stream().filter(member -> !member.isRemoved()).forEach(EntityNPCInterface::delete);
    }

    /**
     * Gets a crew member out beside the vehicle, on the side its seat is, or by parachute from an aircraft in the air: one it
     * carries, else the first the content packs load; without any, it floats down.
     */
    static void bailOut(EntityNPCInterface vehicle, EntityNPCInterface crew)
    {
        NpcVehicleState state = NpcVehicleState.of(vehicle);
        Vec3 seat = seatOf(vehicle, crew).flatMap(mount -> state.seatOffset(vehicle, mount)).orElse(Vec3.ZERO);
        crew.stopRiding();
        ((NpcCrewHolder) crew).wolffsmodnpcsCrewSeat(NO_SEAT);
        Vec3 side = new Vec3(seat.x, 0D, seat.z);
        if (side.lengthSqr() < 1.0E-4D)
            side = Vec3.directionFromRotation(0F, state.getHullYaw() + 90F);
        Vec3 spot = vehicle.position().add(side.normalize().scale(state.reach(vehicle) + BAIL_OUT_MARGIN));
        double ground = vehicle.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(spot.x), Mth.floor(spot.z));
        boolean jump = vehicle.getY() - ground > JUMP_HEIGHT;
        crew.teleportTo(spot.x, jump ? vehicle.getY() + seat.y : Math.max(ground, vehicle.getY()), spot.z);
        if (jump && !NpcParachutes.open(crew) && !FlansEquipment.deployParachute(crew))
            crew.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FALL_TICKS));
        crew.ais.setStartPos(BlockPos.containing(spot.x, ground, spot.z));
    }
}
