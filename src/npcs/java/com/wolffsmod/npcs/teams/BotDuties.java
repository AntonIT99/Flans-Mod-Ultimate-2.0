package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.api.FlansEquipment;
import com.flansmodultimate.api.FlansTeams;
import com.flansmodultimate.api.ITeam;
import com.wolffsmod.npcs.combat.NpcAmmunition;
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

/**
 * What soldier bots do for their side besides fighting, once a second: medics heal hurt allies, engineers repair damaged
 * allied vehicles, support soldiers hand out ammunition, anti-tank soldiers turn on enemy armour, and badly hurt soldiers
 * fall back to a medic or their spawn. A bot with such a duty walks to it while it has nobody to fight. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotDuties
{
    /** Blocks within which a medic looks for the hurt, an engineer for damage, and a support soldier for the short of ammunition. */
    private static final double MEDIC_RANGE = 16D;
    private static final double ENGINEER_RANGE = 24D;
    private static final double SUPPORT_RANGE = 16D;
    /** Blocks within which a duty is carried out. */
    private static final double HEAL_REACH = 3.5D;
    private static final double REPAIR_REACH = 6D;
    private static final double SUPPLY_REACH = 4D;
    /** Blocks within which anti-tank soldiers turn on armour they can see. */
    private static final double ARMOUR_RANGE = 64D;
    /** Blocks a falling back soldier looks for a medic within. */
    private static final double MEDIC_SEARCH = 48D;
    /** Ticks between two heals or repairs by one bot, and before one player gets ammunition again. */
    private static final long ACTION_TICKS = 40L;
    private static final long SUPPLY_TICKS = 30L * 20L;
    /** Ticks a soldier falls back at most. */
    private static final long RETREAT_TICKS = 30L * 20L;
    /** Share of its health a soldier falls back below, and the one it fights again from. */
    private static final double HURT = 0.75D;
    private static final double RECOVERED = 0.7D;
    private static final double DAMAGED = 0.95D;
    /** Spare ammunition items a player holds at least before a support soldier stops handing out more. */
    private static final int SPARE_STOCKS = 2;
    private static final int HANDED_OUT = 2;

    /** Someone who fights in the round: a player or a bot, with its side. */
    private record Fighter(LivingEntity entity, int side, @Nullable BotSlot slot)
    {
        boolean vehicle()
        {
            return slot != null && slot.isVehicle();
        }
    }

    /** Where bots with a duty go, by entity; dropped at every pass and set again while the duty lasts. */
    private static final Map<UUID, Vec3> DESTINATIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();
    private static final Map<UUID, Long> SUPPLIED = new HashMap<>();
    private static final Map<UUID, Long> RETREATING = new HashMap<>();

    /** @return where a bot goes for its duty, ahead of its squad's task, or empty while it has none */
    static Optional<Vec3> destination(Entity npc)
    {
        return Optional.ofNullable(DESTINATIONS.get(npc.getUUID()));
    }

    static void forget()
    {
        DESTINATIONS.clear();
        LAST_ACTION.clear();
        SUPPLIED.clear();
        RETREATING.clear();
    }

    static void tick(MinecraftServer server, ServerLevel level, BotRound round)
    {
        DESTINATIONS.clear();
        long now = level.getGameTime();
        List<Fighter> fighters = fighters(server, level, round);
        for (Fighter fighter : fighters)
        {
            if (fighter.slot() != null && !fighter.vehicle() && fighter.entity() instanceof EntityNPCInterface npc && !retreats(round, npc, fighter, fighters, now))
                serve(level, round, npc, fighter, fighters, now);
        }
        LAST_ACTION.values().removeIf(tick -> now - tick > SUPPLY_TICKS);
        SUPPLIED.values().removeIf(tick -> now - tick > SUPPLY_TICKS);
    }

    /** Carries out a soldier's duty, as its class's role says. */
    private static void serve(ServerLevel level, BotRound round, EntityNPCInterface npc, Fighter fighter, List<Fighter> fighters, long now)
    {
        BotSettings.TeamForce force = round.getSettings().team(Objects.requireNonNull(fighter.slot()).getTeam());
        switch (BotRoles.of(force, fighter.slot().getRole()))
        {
            case MEDIC -> heal(level, npc, fighter, fighters, now);
            case ENGINEER -> repair(level, round, npc, fighter, fighters, now);
            case SUPPORT -> supply(npc, fighter, fighters, now);
            case ANTI_TANK -> engageArmour(round, npc, fighter, fighters);
            case RIFLEMAN -> {
                // A rifleman only fights.
            }
        }
    }

    /** Everyone alive who fights in the round in the level: its players and its bots. */
    private static List<Fighter> fighters(MinecraftServer server, ServerLevel level, BotRound round)
    {
        List<Fighter> fighters = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isAlive() && !player.isSpectator() && player.level() == level)
                FlansTeams.getTeam(player).ifPresent(team -> fighters.add(new Fighter(player, round.getLive().getRound().getSideOf(team), null)));
        }
        for (BotSlot slot : round.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof LivingEntity living && living.isAlive() && slot.getDiedAt() == 0)
                fighters.add(new Fighter(living, round.sideOf(slot.getTeam()), slot));
        }
        return fighters;
    }

    // --- Falling back ---

    /**
     * Sends a badly hurt soldier back to the nearest medic of its side, or to its spawn when it has none near, until it
     * has healed or has been falling back for a while.
     *
     * @return whether the soldier falls back, which comes before its duty
     */
    private static boolean retreats(BotRound round, EntityNPCInterface npc, Fighter self, List<Fighter> fighters, long now)
    {
        int threshold = round.getSettings().getRetreatHealth();
        double share = npc.getHealth() / Math.max(1F, npc.getMaxHealth());
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
        Optional<Fighter> medic = fighters.stream().filter(other -> other != self && other.side() == self.side() && isMedic(round, other))
            .filter(other -> other.entity().distanceTo(npc) <= MEDIC_SEARCH).min(Comparator.comparingDouble(other -> other.entity().distanceToSqr(npc)));
        Vec3 refuge = medic.map(other -> other.entity().position()).orElseGet(() -> spawnRefuge(round, npc, self));
        if (refuge != null && refuge.distanceTo(npc.position()) > HEAL_REACH)
            DESTINATIONS.put(npc.getUUID(), refuge);
        return true;
    }

    private static boolean isMedic(BotRound round, Fighter fighter)
    {
        return fighter.slot() != null && !fighter.vehicle() && BotRoles.of(round.getSettings().team(fighter.slot().getTeam()), fighter.slot().getRole()) == BotSettings.ClassRole.MEDIC;
    }

    @Nullable
    private static Vec3 spawnRefuge(BotRound round, Entity npc, Fighter self)
    {
        Optional<ITeam> team = self.slot() == null ? Optional.empty() : round.team(self.slot().getTeam());
        return team.flatMap(found -> round.getLive().getSpawnPositions(found).stream().min(Comparator.comparingDouble(spawn -> spawn.distanceToSqr(npc.position())))).orElse(null);
    }

    // --- Medics ---

    private static void heal(ServerLevel level, EntityNPCInterface medic, Fighter self, List<Fighter> fighters, long now)
    {
        Optional<Fighter> hurt = fighters.stream().filter(other -> other.side() == self.side() && !other.vehicle() && share(other.entity()) < HURT)
            .filter(other -> other.entity().distanceTo(medic) <= MEDIC_RANGE).min(Comparator.comparingDouble(other -> share(other.entity())));
        if (hurt.isEmpty())
            return;
        LivingEntity patient = hurt.get().entity();
        if (patient.distanceTo(medic) > HEAL_REACH)
        {
            DESTINATIONS.put(medic.getUUID(), patient.position());
            return;
        }
        if (busy(medic, now))
            return;
        int kitAmount = BotRoles.kit(Objects.requireNonNull(self.slot()).getRole()).healAmount();
        patient.heal(kitAmount > 0 ? kitAmount : BotRoles.DEFAULT_AMOUNT);
        effect(level, ParticleTypes.HEART, patient.position().add(0D, patient.getBbHeight(), 0D));
        medic.swing(InteractionHand.MAIN_HAND);
    }

    private static double share(LivingEntity entity)
    {
        return entity.getHealth() / Math.max(1F, entity.getMaxHealth());
    }

    // --- Engineers ---

    private static void repair(ServerLevel level, BotRound round, EntityNPCInterface engineer, Fighter self, List<Fighter> fighters, long now)
    {
        Entity damaged = damagedVehicle(level, round, engineer, self, fighters);
        if (damaged == null)
            return;
        if (damaged.distanceTo(engineer) > REPAIR_REACH + damaged.getBbWidth() / 2F)
        {
            DESTINATIONS.put(engineer.getUUID(), damaged.position());
            return;
        }
        if (busy(engineer, now))
            return;
        int kitAmount = BotRoles.kit(Objects.requireNonNull(self.slot()).getRole()).repairAmount();
        int amount = kitAmount > 0 ? kitAmount : BotRoles.DEFAULT_REPAIR;
        boolean repaired = damaged instanceof LivingEntity vehicleBot ? heal(vehicleBot, amount) : FlansEntityTypes.repair(damaged, engineer.getEyePosition(), amount);
        if (repaired)
        {
            effect(level, ParticleTypes.HAPPY_VILLAGER, damaged.position().add(0D, damaged.getBbHeight() / 2D, 0D));
            engineer.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static boolean heal(LivingEntity vehicleBot, int amount)
    {
        if (vehicleBot.getHealth() >= vehicleBot.getMaxHealth())
            return false;
        vehicleBot.heal(amount);
        return true;
    }

    /**
     * The damaged vehicle nearest an engineer: a vehicle bot of its side, or a driveable that is empty or carries only
     * its side, such as a wreck players could take.
     */
    @Nullable
    private static Entity damagedVehicle(ServerLevel level, BotRound round, Entity engineer, Fighter self, List<Fighter> fighters)
    {
        List<Entity> candidates = new ArrayList<>();
        fighters.stream().filter(other -> other.vehicle() && other.side() == self.side() && other.entity().getHealth() < other.entity().getMaxHealth())
            .filter(other -> other.entity().distanceTo(engineer) <= ENGINEER_RANGE).forEach(other -> candidates.add(other.entity()));
        for (Entity entity : level.getEntities((Entity) null, new AABB(engineer.blockPosition()).inflate(ENGINEER_RANGE), entity -> FlansEntityTypes.getDriveableType(entity).isPresent()))
        {
            boolean damaged = FlansEntityTypes.getHealthShare(entity).orElse(1D) < DAMAGED;
            boolean friendly = entity.getPassengers().stream().allMatch(rider -> FlansTeams.getTeam(rider).map(team -> round.getLive().getRound().getSideOf(team) == self.side()).orElse(true));
            if (damaged && friendly)
                candidates.add(entity);
        }
        return candidates.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(engineer))).orElse(null);
    }

    // --- Support ---

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

    /** Gives an ally ammunition for the gun in its hand: a player into the inventory, a bot into its spare slots. */
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

    // --- Anti-tank soldiers ---

    /** Turns an anti-tank soldier on the nearest enemy armour it can see, unless it already fights armour. */
    private static void engageArmour(BotRound round, EntityNPCInterface soldier, Fighter self, List<Fighter> fighters)
    {
        LivingEntity target = soldier.getTarget();
        if (target != null && BotTargeting.armoured(round, target))
            return;
        fighters.stream().filter(other -> other.side() != self.side() && BotTargeting.armoured(round, other.entity())).filter(other -> other.entity().distanceTo(soldier) <= ARMOUR_RANGE)
            .filter(other -> soldier.hasLineOfSight(other.entity())).min(Comparator.comparingDouble(other -> other.entity().distanceToSqr(soldier)))
            .ifPresent(other -> soldier.setTarget(other.entity()));
    }

    /** Whether a bot healed or repaired too recently to do it again; otherwise it counts as doing it now. */
    private static boolean busy(Entity bot, long now)
    {
        Long last = LAST_ACTION.get(bot.getUUID());
        if (last != null && now - last < ACTION_TICKS)
            return true;
        LAST_ACTION.put(bot.getUUID(), now);
        return false;
    }

    private static void effect(ServerLevel level, ParticleOptions particle, Vec3 at)
    {
        level.sendParticles(particle, at.x, at.y, at.z, 5, 0.3D, 0.3D, 0.3D, 0D);
    }
}
