package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.FlansRepairs;
import com.wolffsmod.npcs.combat.NpcAmmunition;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.sim.TacticKind;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What soldiers do for their allies besides fighting, once a second: medics heal hurt allies, engineers repair damaged
 * allied vehicles, support soldiers hand out ammunition, anti-tank soldiers turn on enemy armour, and badly hurt soldiers
 * fall back to a medic or to safety. A soldier with such a duty walks to it while it has nobody to fight. Teams bots get
 * their duty from their class and their side from their battle ({@link BotDuties}); any other NPC on foot gets its duty and
 * the health it falls back at from the Wolff's Mod tab, takes those its faction is friendly to for allies, and falls back to
 * its home. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcDuties
{
    /** Side of the soldier itself and its allies, and of its enemies, for an NPC that is no bot. */
    private static final int ALLIES = 0;
    private static final int ENEMIES = 1;
    /** Ticks between two passes over one NPC's duties. */
    private static final int INTERVAL = 20;
    /** Blocks around an NPC that is no bot within which it looks for allies and enemies. */
    private static final double SCAN_RANGE = 48D;
    /** Ticks an NPC's equipment is taken to suit the same role before it is looked at again. */
    private static final long ROLE_TICKS = 5L * 20L;
    /** Blocks within which a medic looks for the hurt, an engineer for damage, and a support soldier for the short of ammunition. */
    private static final double MEDIC_RANGE = 16D;
    private static final double ENGINEER_RANGE = 24D;
    private static final double SUPPORT_RANGE = 16D;
    /** Blocks within which a duty is carried out. */
    static final double HEAL_REACH = 3.5D;
    private static final double REPAIR_REACH = 6D;
    private static final double SUPPLY_REACH = 4D;
    /** Blocks within which anti-tank soldiers turn on armour they can see. */
    private static final double ARMOUR_RANGE = 64D;
    /** Blocks a falling back soldier looks for a medic within. */
    private static final double MEDIC_SEARCH = 48D;
    /**
     * Ticks between two repairs by one engineer, before one vehicle is repaired again, between two heals by one medic, before one patient is healed again, and before one player gets ammunition again.
     */
    private static final long REPAIR_TICKS = 5L * 20L;
    private static final long VEHICLE_TICKS = 10L * 20L;
    private static final long HEAL_TICKS = 5L * 20L;
    private static final long PATIENT_TICKS = 10L * 20L;
    private static final long SUPPLY_TICKS = 30L * 20L;
    /** Ticks a soldier falls back at most. */
    private static final long RETREAT_TICKS = 30L * 20L;
    /** Share of its health a soldier needs healing below, the one it fights again from, and the share below which a vehicle needs repairs. */
    private static final double HURT = 0.75D;
    private static final double RECOVERED = 0.7D;
    private static final double DAMAGED = 0.95D;
    /** Spare ammunition items a player holds at least before a support soldier stops handing out more. */
    private static final int SPARE_STOCKS = 2;
    private static final int HANDED_OUT = 2;

    /**
     * Someone who fights: a player, a bot or another NPC, with its side.
     *
     * @param role
     *            what a soldier does besides fighting; null for a player or a vehicle
     * @param healAmount
     *            health a medic gives back with each heal, from the medical tool it carries; 0 for the default
     */
    record Fighter(LivingEntity entity, int side, @Nullable BotSettings.ClassRole role, boolean vehicle, int healAmount)
    {
        boolean medic()
        {
            return !vehicle && role == BotSettings.ClassRole.MEDIC;
        }
    }

    /** What an NPC's equipment suits it for, as last looked at. */
    private record Known(BotRoles.Kit kit, long until)
    {}

    /** Where soldiers with a duty go, by entity; dropped at every pass over the soldier and set again while the duty lasts. */
    private static final Map<UUID, Vec3> DESTINATIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static final Map<UUID, Long> SUPPLIED = new HashMap<>();
    private static final Map<UUID, Long> HEALED = new HashMap<>();
    private static final Map<UUID, Long> REPAIRED = new HashMap<>();
    private static final Map<UUID, Long> RETREATING = new HashMap<>();
    private static final Map<Entity, Known> KITS = new WeakHashMap<>();

    /** @return where a soldier goes for its duty, or empty while it has none */
    public static Optional<Vec3> destination(Entity npc)
    {
        return Optional.ofNullable(DESTINATIONS.get(npc.getUUID()));
    }

    /**
     * @return what the NPC does besides fighting, as its duty says, or as its equipment suggests when its duty names no role;
     *         it decides which enemies an NPC that prefers suited targets looks at first
     */
    public static BotSettings.ClassRole role(EntityNPCInterface npc)
    {
        return switch (NpcTuning.of(npc).duty())
        {
            case MEDIC -> BotSettings.ClassRole.MEDIC;
            case ENGINEER -> BotSettings.ClassRole.ENGINEER;
            case SUPPORT -> BotSettings.ClassRole.SUPPORT;
            case ANTI_TANK -> BotSettings.ClassRole.ANTI_TANK;
            case ASSAULT -> BotSettings.ClassRole.ASSAULT;
            case MELEE -> BotSettings.ClassRole.MELEE;
            case SKIRMISHER -> BotSettings.ClassRole.SKIRMISHER;
            case NONE, FROM_EQUIPMENT -> kit(npc).role();
        };
    }

    /** What the NPC's equipment suits it for, looked at afresh every few seconds. */
    private static BotRoles.Kit kit(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        Known known = KITS.get(npc);
        if (known == null || now >= known.until())
        {
            known = new Known(BotRoles.detect(NpcAmmunition.supplies(npc).stream().filter(stack -> !stack.isEmpty()).toList()), now + ROLE_TICKS);
            KITS.put(npc, known);
        }
        return known.kit();
    }

    /**
     * Carries out the duties of an NPC that is no bot, once a second: falls back when badly hurt, or else does what its duty
     * says. Called every tick of the NPC, server side.
     */
    public static void tick(EntityNPCInterface npc)
    {
        if ((npc.tickCount + npc.getId()) % INTERVAL != 0 || BotTag.isBot(npc))
            return;
        DESTINATIONS.remove(npc.getUUID());
        NpcTuning tuning = NpcTuning.of(npc);
        int threshold = tuning.get(Value.RETREAT_HEALTH);
        boolean duty = tuning.duty() != NpcTuning.Duty.NONE;
        if (!duty && threshold <= 0 || !npc.isAlive() || npc.isKilled() || npc.isPassenger() || !(npc.level() instanceof ServerLevel level) || TacticKind.of(npc) != TacticKind.STANDARD)
        {
            RETREATING.remove(npc.getUUID());
            return;
        }
        long now = level.getGameTime();
        Fighter self = fighter(npc, ALLIES);
        List<Fighter> fighters = around(level, npc, self);
        if (!retreats(npc, self, fighters, now, threshold, () -> new Vec3(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos())) && duty)
            serve(level, npc, self, fighters, now, rider -> NpcDrivers.ally(npc, rider), NpcDuties::armoured);
    }

    /** The NPC as a fighter of a side: its role, and the health its medical tool gives back. */
    private static Fighter fighter(EntityNPCInterface npc, int side)
    {
        boolean vehicle = TacticKind.of(npc) != TacticKind.STANDARD;
        BotSettings.ClassRole role = vehicle ? null : role(npc);
        return new Fighter(npc, side, role, vehicle, vehicle ? 0 : kit(npc).healAmount());
    }

    /** The NPC and everyone alive around it who is its ally or its enemy; anyone else is left out. */
    private static List<Fighter> around(ServerLevel level, EntityNPCInterface npc, Fighter self)
    {
        List<Fighter> fighters = new ArrayList<>();
        fighters.add(self);
        AABB area = npc.getBoundingBox().inflate(SCAN_RANGE);
        NpcCombatants combatants = NpcCombatants.of(level);
        for (int index = 0; index < combatants.size(); index++)
        {
            NpcCombatants.Combatant combatant = combatants.get(index);
            LivingEntity other = combatant.entity();
            if (!area.intersects(combatant.bounds()) || !alive(npc, other))
                continue;
            OptionalInt side = sideOf(npc, other);
            if (side.isPresent())
                fighters.add(combatant.npc() != null ? fighter(combatant.npc(), side.getAsInt()) : new Fighter(other, side.getAsInt(), null, false, 0));
        }
        return fighters;
    }

    /** Whether someone else than the NPC is alive and in play: no spectator, and no killed NPC lying on the ground. */
    private static boolean alive(EntityNPCInterface npc, LivingEntity other)
    {
        return other != npc && other.isAlive() && !other.isSpectator() && !(other instanceof EntityNPCInterface fellow && fellow.isKilled());
    }

    /** @return the side of someone near the NPC: its allies' or its enemies'; empty for anyone who is neither */
    private static OptionalInt sideOf(EntityNPCInterface npc, LivingEntity other)
    {
        if (NpcDrivers.ally(npc, other))
            return OptionalInt.of(ALLIES);
        return NpcDrivers.hostile(npc, other) ? OptionalInt.of(ENEMIES) : OptionalInt.empty();
    }

    /** Forgets the cooldowns that ran out; called every second. */
    public static void serverTick(MinecraftServer server)
    {
        if (server.getTickCount() % INTERVAL != 0)
            return;
        long now = server.overworld().getGameTime();
        LAST_ACTION.values().removeIf(tick -> now - tick > SUPPLY_TICKS);
        SUPPLIED.values().removeIf(tick -> now - tick > SUPPLY_TICKS);
        HEALED.values().removeIf(tick -> now - tick >= PATIENT_TICKS);
        REPAIRED.values().removeIf(tick -> now - tick >= VEHICLE_TICKS);
        RETREATING.values().removeIf(tick -> now - tick > RETREAT_TICKS);
    }

    static void forget()
    {
        DESTINATIONS.clear();
        LAST_ACTION.clear();
        SUPPLIED.clear();
        HEALED.clear();
        REPAIRED.clear();
        RETREATING.clear();
        KITS.clear();
    }

    /** Drops where a soldier was going for its duty, before its duties are looked at again. */
    static void clearDestination(Entity npc)
    {
        DESTINATIONS.remove(npc.getUUID());
    }

    /** Whether falling back, rather than another duty, owns the destination. The combat safety goal may follow it under fire. */
    public static boolean retreating(Entity npc)
    {
        return RETREATING.containsKey(npc.getUUID());
    }

    // --- Falling back ---

    /**
     * Sends a badly hurt soldier back to the nearest medic of its side, or to its refuge when it has none near, until it
     * has healed or has been falling back for a while.
     *
     * @param threshold
     *            percent of its health below which the soldier falls back; 0 never
     * @return whether the soldier falls back, which comes before its duty
     */
    // Callers ask whether a soldier is free for its duty; the method answers the question it is named after.
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean retreats(EntityNPCInterface npc, Fighter self, List<Fighter> fighters, long now, int threshold, Supplier<Vec3> refuge)
    {
        double share = share(npc);
        Long since = RETREATING.get(npc.getUUID());
        if (since == null && threshold > 0 && share * 100D < threshold)
        {
            since = now;
            RETREATING.put(npc.getUUID(), since);
        }
        if (since != null && (share >= RECOVERED || now - since > RETREAT_TICKS || threshold <= 0))
        {
            RETREATING.remove(npc.getUUID());
            since = null;
        }
        if (since == null)
            return false;
        Optional<Fighter> medic = fighters.stream().filter(other -> other != self && other.side() == self.side() && other.medic()).filter(other -> other.entity().distanceTo(npc) <= MEDIC_SEARCH)
            .min(Comparator.comparingDouble(other -> other.entity().distanceToSqr(npc)));
        Vec3 spot = medic.map(other -> other.entity().position()).orElseGet(refuge);
        if (spot != null && spot.distanceTo(npc.position()) > HEAL_REACH)
            DESTINATIONS.put(npc.getUUID(), spot);
        return true;
    }

    // --- Duties ---

    /**
     * Carries out a soldier's duty, as its role says.
     *
     * @param friendlyRider
     *            whether someone riding a driveable is on the soldier's side, for a driveable an engineer may repair
     * @param armoured
     *            whether an entity counts as armour, for an anti-tank soldier
     */
    static void serve(ServerLevel level, EntityNPCInterface npc, Fighter self, List<Fighter> fighters, long now, Predicate<Entity> friendlyRider, Predicate<Entity> armoured)
    {
        if (self.role() == null)
            return;
        switch (self.role())
        {
            case MEDIC -> heal(level, npc, self, fighters, now);
            case ENGINEER -> repair(level, npc, self, fighters, now, friendlyRider);
            case SUPPORT -> supply(npc, self, fighters, now);
            case ANTI_TANK -> engageArmour(npc, self, fighters, armoured);
            case RIFLEMAN, ASSAULT, MELEE, SKIRMISHER -> {
                // Infantry combat movement handles these roles.
            }
        }
    }

    private static void heal(ServerLevel level, EntityNPCInterface medic, Fighter self, List<Fighter> fighters, long now)
    {
        Optional<Fighter> hurt = fighters.stream().filter(other -> other.side() == self.side() && !other.vehicle() && share(other.entity()) < HURT && !HEALED.containsKey(other.entity().getUUID()))
            .filter(other -> other.entity().distanceTo(medic) <= MEDIC_RANGE).min(Comparator.comparingDouble(other -> share(other.entity())));
        if (hurt.isEmpty())
            return;
        LivingEntity patient = hurt.get().entity();
        if (patient.distanceTo(medic) > HEAL_REACH)
        {
            DESTINATIONS.put(medic.getUUID(), patient.position());
            return;
        }
        if (busy(medic, now, HEAL_TICKS))
            return;
        patient.heal(self.healAmount() > 0 ? self.healAmount() : BotRoles.DEFAULT_AMOUNT);
        HEALED.put(patient.getUUID(), now);
        effect(level, ParticleTypes.HEART, patient.position().add(0D, patient.getBbHeight(), 0D));
        medic.swing(InteractionHand.MAIN_HAND);
    }

    private static double share(LivingEntity entity)
    {
        return entity.getHealth() / Math.max(1F, entity.getMaxHealth());
    }

    private static void repair(ServerLevel level, EntityNPCInterface engineer, Fighter self, List<Fighter> fighters, long now, Predicate<Entity> friendlyRider)
    {
        Entity damaged = damagedVehicle(level, engineer, self, fighters, friendlyRider);
        if (damaged == null)
            return;
        if (!FlansRepairs.withinReach(damaged, engineer.getEyePosition(), REPAIR_REACH))
        {
            DESTINATIONS.put(engineer.getUUID(), damaged.position());
            return;
        }
        if (busy(engineer, now, REPAIR_TICKS))
            return;
        boolean repaired = false;
        for (ItemStack tool : NpcAmmunition.supplies(engineer))
        {
            if (FlansRepairs.useTool(engineer, damaged, tool))
            {
                repaired = true;
                engineer.updateClient = true;
                break;
            }
        }
        if (repaired)
        {
            REPAIRED.put(damaged.getUUID(), now);
            effect(level, ParticleTypes.HAPPY_VILLAGER, damaged.position().add(0D, damaged.getBbHeight() / 2D, 0D));
            engineer.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * The damaged vehicle nearest an engineer: a vehicle of its side, or a driveable that is empty or carries only its side,
     * such as a wreck players could take.
     */
    @Nullable
    private static Entity damagedVehicle(ServerLevel level, Entity engineer, Fighter self, List<Fighter> fighters, Predicate<Entity> friendlyRider)
    {
        List<Entity> candidates = new ArrayList<>();
        fighters.stream().filter(other -> other.vehicle() && other.side() == self.side() && other.entity().getHealth() < other.entity().getMaxHealth())
            .filter(other -> other.entity().distanceTo(engineer) <= ENGINEER_RANGE).forEach(other -> candidates.add(other.entity()));
        for (Entity entity : level.getEntities((Entity) null, new AABB(engineer.blockPosition()).inflate(ENGINEER_RANGE), entity -> FlansEntityTypes.getDriveableType(entity).isPresent()))
        {
            boolean damaged = FlansEntityTypes.getHealthShare(entity).orElse(1D) < DAMAGED;
            if (damaged && entity.getPassengers().stream().allMatch(friendlyRider))
                candidates.add(entity);
        }
        return candidates.stream().filter(candidate -> !REPAIRED.containsKey(candidate.getUUID()) && repairable(candidate))
            .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(engineer))).orElse(null);
    }

    private static boolean repairable(Entity entity)
    {
        if (!(entity instanceof EntityNPCInterface npc))
            return entity.isAlive();
        if (!npc.isAlive() || npc.isKilled() || !NpcEquipment.enabled(npc, Feature.PART_HEALTH))
            return false;
        return NpcVehicleState.of(npc).getParts().body()
            .map(body -> body.getParts().stream().anyMatch(part -> (!part.destroyed() || NpcEquipment.enabled(npc, Feature.PART_REPAIR)) && FlansRepairs.canRepair(body, part.name()))).orElse(false);
    }

    private static void supply(EntityNPCInterface support, Fighter self, List<Fighter> fighters, long now)
    {
        Optional<Fighter> needy = fighters.stream().filter(other -> other != self && other.side() == self.side() && !other.vehicle() && !SUPPLIED.containsKey(other.entity().getUUID()))
            .filter(other -> other.entity().distanceTo(support) <= SUPPORT_RANGE && shortOfAmmunition(other.entity())).min(Comparator.comparingDouble(other -> other.entity().distanceToSqr(support)));
        if (needy.isEmpty())
            return;
        LivingEntity ally = needy.get().entity();
        if (ally.distanceTo(support) > SUPPLY_REACH)
        {
            DESTINATIONS.put(support.getUUID(), ally.position());
            return;
        }
        if (handOut(ally))
        {
            SUPPLIED.put(ally.getUUID(), now);
            support.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Whether someone holds a Flan gun with little or nothing to reload it with. */
    private static boolean shortOfAmmunition(LivingEntity ally)
    {
        ItemStack gun = ally.getMainHandItem();
        if (FlansEquipment.createDefaultAmmunition(gun).isEmpty())
            return false;
        if (ally instanceof EntityNPCInterface npc)
            return NpcAmmunition.exhausted(npc, gun);
        if (!(ally instanceof ServerPlayer player))
            return false;
        int spare = 0;
        for (ItemStack stack : player.getInventory().items)
            if (FlansEquipment.acceptsAmmunition(gun, stack))
                spare++;
        return spare < SPARE_STOCKS;
    }

    /** Gives an ally ammunition for the gun in its hand: a player into the inventory, an NPC into its spare slots. */
    private static boolean handOut(LivingEntity ally)
    {
        Optional<ItemStack> ammunition = FlansEquipment.createDefaultAmmunition(ally.getMainHandItem()).map(stack -> stack.copyWithCount(Math.min(stack.getCount(), HANDED_OUT)));
        if (ammunition.isEmpty())
            return false;
        if (ally instanceof ServerPlayer player)
        {
            if (!player.getInventory().add(ammunition.get()))
                return false;
            player.containerMenu.broadcastChanges();
            return true;
        }
        if (ally instanceof EntityNPCInterface npc)
        {
            for (int slot = 0; slot < NpcAmmunition.INVENTORY_SLOTS; slot++)
            {
                if (npc.inventory.drops.get(slot) == null || npc.inventory.drops.get(slot).isEmpty())
                {
                    npc.inventory.drops.put(slot, NpcAPI.Instance().getIItemStack(ammunition.get()));
                    npc.inventory.dropchance.put(slot, 0F);
                    return true;
                }
            }
        }
        return false;
    }

    /** Turns an anti-tank soldier on the nearest enemy armour it can see, unless it already fights armour. */
    private static void engageArmour(EntityNPCInterface soldier, Fighter self, List<Fighter> fighters, Predicate<Entity> armoured)
    {
        LivingEntity target = soldier.getTarget();
        if (target != null && armoured.test(target))
            return;
        fighters.stream().filter(other -> other.side() != self.side() && armoured.test(other.entity())).filter(other -> other.entity().distanceTo(soldier) <= ARMOUR_RANGE)
            .filter(other -> soldier.hasLineOfSight(other.entity())).min(Comparator.comparingDouble(other -> other.entity().distanceToSqr(soldier)))
            .ifPresent(other -> soldier.setTarget(other.entity()));
    }

    /**
     * Whether an entity is armour: an NPC drawn as a vehicle, mecha or AA gun, or someone riding a vehicle or mecha.
     * Aircraft are not.
     */
    static boolean armoured(Entity entity)
    {
        if (entity instanceof EntityNPCInterface npc)
        {
            TacticKind kind = TacticKind.of(npc);
            if (kind == TacticKind.VEHICLE || kind == TacticKind.MECHA || kind == TacticKind.AA_GUN)
                return true;
        }
        return BotTargeting.ridesArmour(entity);
    }

    /** Whether a soldier healed or repaired too recently to do it again; otherwise it counts as doing it now. */
    private static boolean busy(Entity soldier, long now, long cooldown)
    {
        Long last = LAST_ACTION.get(soldier.getUUID());
        if (last != null && now - last < cooldown)
            return true;
        LAST_ACTION.put(soldier.getUUID(), now);
        return false;
    }

    private static void effect(ServerLevel level, ParticleOptions particle, Vec3 at)
    {
        level.sendParticles(particle, at.x, at.y, at.z, 5, 0.3D, 0.3D, 0.3D, 0D);
    }
}
