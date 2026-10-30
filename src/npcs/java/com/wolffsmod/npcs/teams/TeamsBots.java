package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsListener;
import com.flansmodultimate.api.teams.ITeamsLiveRound;
import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import com.flansmodultimate.api.teams.ITeamsSpawnProvider;
import com.flansmodultimate.api.teams.TeamsCapturePoint;
import com.flansmodultimate.api.teams.TeamsSpawner;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.sim.NpcTargetSearch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Runs the bots of Teams rounds. When a round with bots starts, each team's force is raised from the round's settings:
 * loose infantry, squads and vehicles, spawned at the team's spawn points and vehicle spawners and assigned to the team.
 * Fallen bots come back while the settings, budgets and tickets allow; every side's commander gives its squads tasks;
 * the bots appear on the scoreboard; and when the round ends, they are removed. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsBots implements ITeamsListener, ITeamsRespawnRule, ITeamsSpawnProvider
{
    private static final TeamsBots INSTANCE = new TeamsBots();
    /** Bots added to the level each tick at most, so a large force arrives over a few seconds instead of in one tick. */
    private static final int SPAWNS_PER_TICK = 2;
    private static final int UPKEEP_INTERVAL = 20;
    private static final int COMMAND_INTERVAL = 40;
    private static final int SWEEP_INTERVAL = 200;
    /** Ticks a fallen bot's body stays before it is removed. */
    private static final int BODY_TICKS = 60;
    /** Ticks a bot may be missing from a loaded chunk before it counts as lost. */
    private static final int LOST_TICKS = 100;
    /** Ticks before a spawn that found no spawn point is tried again. */
    private static final int RETRY_TICKS = 200;
    /** Spawn points nearest its squad's task a soldier may appear at. */
    private static final int NEAREST_SPAWNS = 3;
    /** Blocks from where its duty sends it within which a bot counts as there. */
    private static final double DUTY_ARRIVAL = 2D;

    @Nullable
    private BotBattle round;

    /** Hooks the bots into Teams rounds; called once while the module is constructed. */
    public static void register()
    {
        FlansTeams.addListener(INSTANCE);
        FlansTeams.addRespawnRule(INSTANCE);
        FlansTeams.addSpawnProvider(INSTANCE);
        FlansProjectiles.addSuppressionListener(BotSuppression.INSTANCE);
        NpcTargetSearch.setPriority(BotTargeting::rank);
    }

    public static void serverTick(MinecraftServer server)
    {
        BotMarkerDisplay.tick(server);
        // Skirmishes are taken back after a restart before the sweep could take their bots for leftovers.
        Skirmishes.tick(server);
        Wars.tick(server);
        INSTANCE.tick(server);
    }

    /**
     * Players share their side's tickets with its bots, once the round limits them and lets players spend them: each
     * respawn costs one, and so does joining again after having fought. A side out of tickets keeps its players watching.
     */
    @Override
    public boolean mayRespawn(ITeamsLiveRound live, ServerPlayer player, ITeam team, Reason reason)
    {
        BotBattle current = round;
        if (current == null || !current.getInstance().equals(live.getInstanceId()))
            return true;
        boolean first = current.getFieldedPlayers().add(player.getUUID());
        BotSettings settings = current.getSettings();
        if (!settings.isEnabled() || !settings.isPlayerTickets() || settings.getTickets() <= 0 || first && reason == Reason.JOINED)
            return true;
        int side = live.getRound().getSideOf(team);
        int left = BotPopulation.ticketsLeft(current, side);
        if (left <= 0)
            return false;
        current.getTickets().put(side, left - 1);
        current.getSidesThatFought().add(side);
        return true;
    }

    /**
     * Lets go of the round as the server stops. Its bots stay in the world, saved with their chunks and with the round
     * store, to be taken back when the same playing resumes.
     */
    public static void serverStopping(MinecraftServer server)
    {
        if (INSTANCE.round != null)
        {
            BotRoundStore.get(server).setDirty();
            BotBattles.remove(INSTANCE.round);
        }
        INSTANCE.round = null;
        BotGunners.forget();
        BotFactions.tearDown();
        forgetDuties(server);
        Skirmishes.serverStopping(server);
        Wars.serverStopping();
        BotMarkerDisplay.clear();
    }

    private static void forgetDuties(MinecraftServer server)
    {
        BotRadio.forget();
        BotDuties.forget();
        BotSuppression.forget(server);
        BotRoles.forget();
    }

    /** A player leading a squad may come back on it, or at its rally point, as they chose. */
    @Override
    public Optional<Vec3> spawnPosition(ITeamsLiveRound live, ServerPlayer player, ITeam team, Reason reason)
    {
        BotBattle current = round;
        if (current == null || !current.getInstance().equals(live.getInstanceId()) || player.getServer() == null)
            return Optional.empty();
        return BotLeaders.spawnFor(player.getServer(), current, player, ThreadLocalRandom.current());
    }

    /** @return where a bot should go for its squad's task, or empty for an NPC that is no bot of a battle or has no task */
    static Optional<BotCommander.Order> orderFor(EntityNPCInterface npc)
    {
        MinecraftServer server = npc.getServer();
        if (BotBattles.isEmpty() || server == null)
            return Optional.empty();
        // A duty, such as healing an ally or falling back, comes before the squad's task.
        Optional<BotCommander.Order> duty = BotDuties.destination(npc).map(spot -> new BotCommander.Order(spot, DUTY_ARRIVAL, BotRole.INFANTRY, SquadTask.Kind.HOLD));
        return duty.isPresent() ? duty : BotBattles.of(npc).flatMap(battle -> BotCommander.order(server, battle, npc));
    }

    /** @return the bots of the round being played, for commands; empty between rounds */
    static Optional<BotBattle> current()
    {
        return Optional.ofNullable(INSTANCE.round);
    }

    /** Removes every bot of the round now; the force is raised again by the next upkeep while bots are enabled. */
    static int clear(MinecraftServer server)
    {
        BotBattle current = INSTANCE.round;
        if (current == null)
            return 0;
        int removed = removeAll(server, current);
        current.getSlots().clear();
        current.getSquads().clear();
        current.getQueue().clear();
        current.getSlotOfEntity().clear();
        return removed;
    }

    private void tick(MinecraftServer server)
    {
        Optional<ITeamsLiveRound> live = FlansTeams.getRunningRound();
        int tick = server.getTickCount();
        if (live.isEmpty())
        {
            if (round != null)
                end(server);
            else if (tick % SWEEP_INTERVAL == 0)
                sweep(server);
            return;
        }
        if (round == null || !round.getInstance().equals(live.get().getInstanceId()))
            begin(server, live.get());
        BotBattle current = Objects.requireNonNull(round);
        ServerLevel level = server.getLevel(live.get().getDimension());
        if (level == null)
            return;
        long now = level.getGameTime();
        RandomGenerator random = ThreadLocalRandom.current();
        if (tick % UPKEEP_INTERVAL == 0)
        {
            current.setSettings(BotSettings.load(live.get().getRound().getExtensionData(BotSettings.KEY)));
            if (current.getSettings().isEnabled() && current.getFactions().isEmpty())
                current.getFactions().putAll(BotFactions.setUp(live.get().getRound()));
            upkeep(server, level, current, now, random);
            BotSuppression.tick(level);
            eliminate(server, level, current);
            // The world saves the round as it is now, whenever it is saved next.
            BotRoundStore.get(server).setDirty();
        }
        spawnQueued(level, current, now, random);
        BotGunners.tick(level, current);
        if (tick % COMMAND_INTERVAL == 0)
        {
            BotCommander.plan(server, level, current);
            BotRadio.tick(server, level, current);
            BotOrders.sendWaypoints(server, current);
            BotCommander.scoreboard(current);
        }
        if (tick % SWEEP_INTERVAL == 0)
            sweep(server);
    }

    /**
     * Keeps the force of a battle going: notes who fell or went missing, brings the force in line with the settings, puts
     * down the squads transports carried to the front, and gives soldiers their duties.
     */
    static void upkeep(MinecraftServer server, ServerLevel level, BotBattle battle, long now, RandomGenerator random)
    {
        track(level, battle, now);
        BotPopulation.upkeep(server, battle, now, random);
        BotTransports.unload(level, battle, now, random);
        BotDuties.tick(server, level, battle);
    }

    private void begin(MinecraftServer server, ITeamsLiveRound live)
    {
        if (round != null)
            end(server);
        BotSettings settings = BotSettings.load(live.getRound().getExtensionData(BotSettings.KEY));
        BotBattle started = new BotBattle(live.getInstanceId(), new RoundBattleground(live), settings, settings.isEnabled() ? BotFactions.setUp(live.getRound()) : Map.of());
        round = started;
        BotBattles.add(started);
        // Players placed when the round started took the field for free; coming back later costs them a ticket.
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (FlansTeams.getTeam(player).isPresent())
                started.getFieldedPlayers().add(player.getUUID());
        // The same playing resumed after a restart takes back the bots it had; bots of any other playing are removed.
        ServerLevel level = server.getLevel(live.getDimension());
        BotRoundStore.get(server).resume(started, level == null ? 0L : level.getGameTime());
        sweep(server);
    }

    private void end(MinecraftServer server)
    {
        BotBattle ended = round;
        round = null;
        if (ended != null)
        {
            BotBattles.remove(ended);
            removeAll(server, ended);
        }
        BotFactions.tearDown();
        BotGunners.forget();
        forgetDuties(server);
        BotRoundStore.get(server).forget();
        BotOrders.clearWaypoints(server);
        sweep(server);
    }

    // --- Following the bots ---

    /** Notes which bots died, went missing or left the force, and removes the bodies of the fallen. */
    private static void track(ServerLevel level, BotBattle current, long now)
    {
        for (BotSlot slot : current.getSlots().values())
        {
            if (slot.getEntity() != null)
                track(level, current, slot, now);
        }
        current.getSlots().values().removeIf(slot -> slot.isRetired() && slot.getEntity() == null && slot.getKills() == 0 && slot.getDeaths() == 0);
        // A squad goes once none of its slots is left, including a vehicle whose entry was removed.
        current.getSquads().values().removeIf(squad -> squad.getSlots().stream().noneMatch(current.getSlots()::containsKey));
    }

    private static void track(ServerLevel level, BotBattle current, BotSlot slot, long now)
    {
        Entity entity = level.getEntity(Objects.requireNonNull(slot.getEntity()));
        if (entity instanceof EntityNPCInterface npc)
        {
            slot.setLastSeen(now);
            slot.setLastPosition(npc.position());
            boolean dead = !npc.isAlive() || npc.isKilled();
            if (dead && slot.getDiedAt() == 0)
                fallenUnreported(current, slot, now);
            if (dead && now - slot.getDiedAt() > BODY_TICKS || !dead && slot.isRetired())
                removeBody(level, current, slot, npc, dead);
        }
        else if (slot.getDiedAt() > 0)
            forget(current, slot);
        else if (slot.getLastPosition() != null && level.isLoaded(BlockPos.containing(slot.getLastPosition())) && now - slot.getLastSeen() > LOST_TICKS)
        {
            // The NPC was removed without dying, by a command or another mod.
            fallen(current, slot, now);
            forget(current, slot);
        }
    }

    /** Removes a fallen bot's body, or a retired bot, leaving the wreck a destroyed vehicle left. */
    private static void removeBody(ServerLevel level, BotBattle current, BotSlot slot, EntityNPCInterface npc, boolean dead)
    {
        remove(npc);
        if (dead && slot.isWreckPending())
            BotTransports.leaveWreck(level, slot, npc.position(), npc.getYRot());
        forget(current, slot);
    }

    private static void fallen(BotBattle current, BotSlot slot, long now)
    {
        slot.setDiedAt(now);
        int seconds = current.getSettings().getRespawnSeconds();
        if (slot.isVehicle())
        {
            List<BotSettings.VehicleEntry> vehicles = current.getSettings().team(slot.getTeam()).getVehicles();
            if (slot.getEntry() < vehicles.size() && vehicles.get(slot.getEntry()).respawnSeconds() > 0)
                seconds = vehicles.get(slot.getEntry()).respawnSeconds();
        }
        slot.setRespawnAt(now + seconds * 20L);
        // A soldier a transport brought never comes back; a new transport brings others.
        if (slot.getSource() == BotSlot.Source.CARGO || slot.getSource() == BotSlot.Source.VOLUNTEER)
            slot.setRetired(true);
    }

    /**
     * A bot found dead that no kill reported. A round counts the deaths and leaves the wrecks of its bots as their kills are
     * reported; other battles do it here.
     */
    private static void fallenUnreported(BotBattle current, BotSlot slot, long now)
    {
        fallen(current, slot, now);
        if (current.getGround().round().isEmpty())
            died(current, slot);
    }

    /** Counts a bot's death; some destroyed vehicles leave a wreck players can repair, put in place once the bot's body is gone. */
    private static void died(BotBattle current, BotSlot slot)
    {
        slot.setDeaths(slot.getDeaths() + 1);
        if (slot.isVehicle() && ThreadLocalRandom.current().nextInt(100) < current.getSettings().getAbandonChance())
            slot.setWreckPending(true);
    }

    private static void forget(BotBattle current, BotSlot slot)
    {
        if (slot.getEntity() != null)
            current.getSlotOfEntity().remove(slot.getEntity());
        slot.setEntity(null);
    }

    // --- Spawning ---

    /** Brings in the next bots waiting in a battle's spawn queue, a few a tick. */
    static void spawnQueued(ServerLevel level, BotBattle current, long now, RandomGenerator random)
    {
        int spawned = 0;
        while (spawned < SPAWNS_PER_TICK && !current.getQueue().isEmpty())
        {
            BotSlot slot = current.getSlots().get(current.getQueue().poll());
            if (slot != null)
                slot.setQueued(false);
            // Settings or spending may have changed since the slot was queued.
            if (slot != null && slot.getEntity() == null && BotPopulation.canSpawn(current, slot))
            {
                spawn(level, current, slot, now, random);
                spawned++;
            }
        }
    }

    private static void spawn(ServerLevel level, BotBattle current, BotSlot slot, long now, RandomGenerator random)
    {
        spawnAt(level, current, slot, now, random, null);
    }

    /**
     * Makes the NPC of a slot, at a spawn point of its team or at the place given, and records the spawn against the
     * budgets and tickets.
     *
     * @return the new NPC, or empty when it could not be made; the slot then tries again later
     */
    static Optional<EntityNPCInterface> spawnAt(ServerLevel level, BotBattle current, BotSlot slot, long now, RandomGenerator random, @Nullable Vec3 at)
    {
        ITeam team = current.team(slot.getTeam()).orElse(null);
        Vec3 position = team == null || at != null ? at : spawnPoint(current, slot, team, random);
        Integer faction = current.getFactions().get(slot.getTeam());
        if (team == null || position == null || faction == null)
        {
            slot.setRespawnAt(now + RETRY_TICKS);
            return Optional.empty();
        }
        BotSettings.TeamForce force = current.getSettings().team(slot.getTeam());
        BotSquad squad = current.getSquads().get(slot.getSquad());
        String nameList = squad == null || squad.getTemplate().isEmpty()
            ? force.getNameList()
            : SquadTemplates.get().find(squad.getTemplate()).map(SquadTemplate::nameList).filter(list -> !list.isEmpty()).orElse(force.getNameList());
        BotFactory.Order order = new BotFactory.Order(slot, team, faction, current.getSettings(), force, current.getInstance(), position, nameList, current.getGround().round().isPresent());
        Optional<EntityNPCInterface> npc = slot.isVehicle() ? BotFactory.vehicle(level, order, random.nextFloat() * 360F, random) : BotFactory.soldier(level, order, random);
        if (npc.isEmpty())
        {
            NpcsMod.log.warn("Teams bot of team {} could not be made as {}", slot.getTeam(), slot.getRole());
            slot.setRespawnAt(now + RETRY_TICKS);
            return npc;
        }
        BotPopulation.charge(current, slot);
        slot.setSpawns(slot.getSpawns() + 1);
        slot.setEntity(npc.get().getUUID());
        slot.setDiedAt(0);
        slot.setLastSeen(now);
        slot.setLastPosition(npc.get().position());
        current.getSlotOfEntity().put(npc.get().getUUID(), slot.getId());
        if (slot.isVehicle() && squad != null)
            BotTransports.load(current, slot, squad);
        if (slot.isVehicle())
            BotCrews.crew(level, current, order, npc.get(), random);
        return npc;
    }

    /**
     * Where a slot's bot appears: a vehicle at a vehicle spawner its team holds, the one its settings name if any; a
     * soldier at the team's spawn point nearest its squad's task. Null when the team has no spawn point.
     */
    @Nullable
    private static Vec3 spawnPoint(BotBattle current, BotSlot slot, ITeam team, RandomGenerator random)
    {
        if (slot.isVehicle())
        {
            List<BotSettings.VehicleEntry> vehicles = current.getSettings().team(slot.getTeam()).getVehicles();
            UUID wanted = slot.getEntry() < vehicles.size() ? vehicles.get(slot.getEntry()).spawner() : null;
            List<TeamsSpawner> spawners = current.getGround().spawners().stream()
                .filter(spawner -> spawner.kind() == TeamsSpawner.Kind.VEHICLE && spawner.owner() != null && spawner.owner().getShortName().equals(slot.getTeam()))
                .filter(spawner -> wanted == null || wanted.equals(spawner.id())).toList();
            if (!spawners.isEmpty())
                return Vec3.atBottomCenterOf(spawners.get(random.nextInt(spawners.size())).position().above());
        }
        List<Vec3> positions = current.getGround().spawnPositions(team);
        if (positions.isEmpty())
            return null;
        BotSquad squad = current.getSquads().get(slot.getSquad());
        // A squad's fallen come back at its rally point while it has one.
        if (squad != null && squad.getRally() != null)
            return squad.getRally();
        SquadTask task = squad == null ? null : squad.getTask();
        if (task != null)
        {
            // Spread over the few spawn points nearest the task, so a whole squad does not appear on one block.
            List<Vec3> nearest = positions.stream().sorted(Comparator.comparingDouble(position -> position.distanceToSqr(task.target()))).limit(NEAREST_SPAWNS).toList();
            return nearest.get(random.nextInt(nearest.size()));
        }
        return positions.get(random.nextInt(positions.size()));
    }

    // --- Winning by attrition ---

    /**
     * Ends the round once only one side can still fight, when tickets or budgets are set: a side is worn out when it has
     * fought, has no player in the round, no bot alive, and nothing left to bring in.
     */
    private static void eliminate(MinecraftServer server, ServerLevel level, BotBattle current)
    {
        BotSettings settings = current.getSettings();
        if (!settings.isEnabled() || !limited(current))
            return;
        Map<Integer, Integer> players = new HashMap<>();
        // A player held back as a spectator, out of tickets, no longer fights for the side.
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (!player.isSpectator())
                FlansTeams.getTeam(player).ifPresent(team -> players.merge(current.getGround().sideOf(team), 1, Integer::sum));
        players.keySet().forEach(current.getSidesThatFought()::add);
        List<Integer> standing = new ArrayList<>();
        boolean anyOut = false;
        for (int side = 0; side < current.getGround().sideCount(); side++)
        {
            int checked = side;
            boolean alive = current.getSlots().values().stream()
                .anyMatch(slot -> current.sideOf(slot.getTeam()) == checked && slot.getEntity() != null && level.getEntity(slot.getEntity()) instanceof LivingEntity living && living.isAlive());
            boolean out = current.getSidesThatFought().contains(side) && !players.containsKey(side) && !alive && !BotPopulation.canStillField(current, side);
            if (out)
                anyOut = true;
            else
                standing.add(side);
        }
        if (anyOut && standing.size() == 1)
            current.getGround().teamsOfSide(standing.get(0)).stream().findFirst().ifPresent(FlansTeams::declareWinner);
    }

    /** Whether the round limits respawns at all, by tickets or by a budget. */
    private static boolean limited(BotBattle current)
    {
        BotSettings settings = current.getSettings();
        if (settings.getTickets() > 0)
            return true;
        return settings.getTeams().values().stream()
            .anyMatch(force -> force.getBudget() > 0 || force.getSquads().stream().anyMatch(squad -> squad.budget() > 0) || force.getVehicles().stream().anyMatch(vehicle -> vehicle.budget() > 0));
    }

    // --- Removing bots ---

    /** Removes every bot of a battle in its level now. */
    static int removeAll(MinecraftServer server, BotBattle current)
    {
        ServerLevel level = server.getLevel(current.getGround().dimension());
        int removed = 0;
        for (BotSlot slot : current.getSlots().values())
        {
            Entity entity = level == null || slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof EntityNPCInterface npc)
            {
                remove(npc);
                removed++;
            }
        }
        return removed;
    }

    /** Removes every bot in the loaded levels that is not the living bot of a slot of a battle being fought. */
    private static void sweep(MinecraftServer server)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            List<EntityNPCInterface> stale = new ArrayList<>();
            for (Entity entity : level.getAllEntities())
            {
                if (entity instanceof EntityNPCInterface npc && BotTag.read(npc).filter(data -> belongs(npc, data)).isEmpty() && BotTag.isBot(npc))
                    stale.add(npc);
            }
            stale.forEach(TeamsBots::remove);
        }
    }

    /**
     * Whether a bot is the living bot of a slot of the battle it was made for, or crews the vehicle of a slot of it, aboard
     * or on foot since the vehicle was lost.
     */
    private static boolean belongs(EntityNPCInterface npc, BotTag.Data data)
    {
        return BotBattles.get(data.instance()).filter(battle -> data.crew() ? battle.getSlots().containsKey(data.slot()) : battle.getSlotOfEntity().containsKey(npc.getUUID())).isPresent();
    }

    /** Removes a bot; an NPC that was enlisted as it was is given back instead, awake and no bot any more. */
    private static void remove(EntityNPCInterface npc)
    {
        FlansTeams.releaseCombatant(npc);
        if (BotTag.read(npc).map(BotTag.Data::enlisted).orElse(false))
        {
            Skirmishes.wake(npc);
            BotTag.clear(npc);
            return;
        }
        npc.delete();
    }

    /**
     * A bot that dies drops what the Teams {@code weaponDrops} and {@code armourDrops} rules let a dying player drop, in a
     * round or a battle outside one; other NPCs keep their own drops.
     *
     * @param drops
     *            the drops of the bot's drop event, changed in place
     */
    public static void entityDrops(LivingEntity victim, Collection<ItemEntity> drops)
    {
        if (!victim.level().isClientSide && BotTag.isBot(victim))
            FlansTeams.applyDeathDropRules(drops);
    }

    /**
     * Credits the bot that killed an enemy in a battle fought outside rounds, a crew member scoring for its vehicle. Rounds
     * credit their bots as the round reports its kills.
     */
    public static void entityDied(LivingEntity victim, DamageSource source)
    {
        if (victim.level().isClientSide || !(source.getEntity() instanceof EntityNPCInterface killer))
            return;
        for (BotBattle battle : BotBattles.all())
        {
            if (battle.getGround().round().isPresent())
                continue;
            Optional<BotSlot> scorer = BotBattles.slotOf(battle, killer).or(() -> BotCrews.slotOf(battle, killer));
            if (scorer.isPresent())
            {
                if (battle.getGround().areEnemies(killer, victim))
                    scorer.get().setKills(scorer.get().getKills() + 1);
                return;
            }
        }
    }

    // --- Round events ---

    @Override
    public void roundEnded(ITeamsLiveRound ended, @Nullable ITeam winner)
    {
        // The next tick removes the bots, once the round's end has been handled everywhere.
        if (round != null && round.getInstance().equals(ended.getInstanceId()))
            round.getQueue().clear();
    }

    @Override
    public void capturePointChanged(ITeamsLiveRound live, TeamsCapturePoint point, int previousOwnerSide)
    {
        if (round != null && round.getInstance().equals(live.getInstanceId()))
            BotRadio.pointChanged(point, previousOwnerSide);
    }

    @Override
    public void combatantKilled(ITeamsLiveRound live, LivingEntity victim, @Nullable LivingEntity killer, DamageSource source)
    {
        BotBattle current = round;
        if (current == null || !current.getInstance().equals(live.getInstanceId()))
            return;
        Integer victimSlot = current.getSlotOfEntity().get(victim.getUUID());
        BotSlot fallen = victimSlot == null ? null : current.getSlots().get(victimSlot);
        if (fallen != null)
        {
            if (fallen.getDiedAt() == 0)
                fallen(current, fallen, victim.level().getGameTime());
            died(current, fallen);
        }
        Integer killerSlot = killer == null ? null : current.getSlotOfEntity().get(killer.getUUID());
        BotSlot scorer = killerSlot == null ? null : current.getSlots().get(killerSlot);
        // A crew member scores for its vehicle.
        if (scorer == null && killer instanceof EntityNPCInterface crew)
            scorer = BotCrews.slotOf(current, crew).orElse(null);
        if (scorer != null && FlansTeams.areEnemies(killer, victim))
            scorer.setKills(scorer.getKills() + 1);
    }
}
