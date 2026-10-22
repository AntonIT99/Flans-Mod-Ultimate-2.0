package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansTeams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Runs the skirmishes of the world: starts them, raises and commands their bots as Teams rounds do, moves their fronts,
 * decides them, and stops them. While a Teams round is played, or a skirmish is paused, its bots rest: they stand still,
 * attack nobody and, when the skirmish protects them, cannot be hurt; the managed factions are at rest too. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Skirmishes
{
    private static final int UPKEEP_INTERVAL = 20;
    private static final int COMMAND_INTERVAL = 40;
    /** Ticks a decided skirmish's bots stay before they are removed. */
    private static final long END_LINGER = 600L;
    private static final long TICKS_PER_MINUTE = 1200L;
    /** How far a front moves from the middle toward the weaker side, as a share of the distance between the zones. */
    private static final double PUSH_SHIFT = 0.35D;
    /** Blocks around a skirmish within which players hear how it ended. */
    private static final double ANNOUNCE_RANGE = 256D;
    /** Key of what a resting bot was before it rested, in its persistent data. */
    private static final String REST_KEY = "WolffsModSkirmishRest";
    private static final String NBT_INVULNERABLE = "invulnerable";
    private static final String NBT_NO_AI = "no_ai";
    private static final String PREFIX = "wolffsmodnpcs.skirmish.";

    /** Whether the skirmishes of the running server were taken up again after it started. */
    private static boolean adopted;

    /** What starting a skirmish came to: whether it started, and what the operator should know. */
    public record Outcome(boolean started, List<Component> messages)
    {}

    // --- Starting and stopping ---

    /**
     * Composes the sides' forces from their factions' branches and starts a drafted skirmish. Refused while a Teams round is
     * played, with fewer than two sides, with a side of a faction that is not managed or has no branch, with no two sides
     * at war, or with a team serving two sides.
     */
    public static Outcome start(MinecraftServer server, Skirmish skirmish)
    {
        Optional<Outcome> refusal = refusal(server, skirmish);
        if (refusal.isPresent())
            return refusal.get();
        List<Component> notes = new ArrayList<>();
        Optional<Outcome> clash = compose(server, skirmish, notes);
        if (clash.isPresent())
            return clash.get();
        ServerLevel level = Objects.requireNonNull(server.getLevel(skirmish.getDimension()));
        skirmish.setState(Skirmish.State.RUNNING);
        skirmish.setStartedAt(level.getGameTime());
        skirmish.setFront(null);
        moveFront(level, skirmish);
        BotBattle battle = battle(server, skirmish);
        skirmish.setBattle(battle);
        BotBattles.add(battle);
        SkirmishStore.get(server).setDirty();
        notes.add(0, text("started", skirmish.getName()));
        return new Outcome(true, notes);
    }

    /** @return why a skirmish may not start now, or empty when it may */
    private static Optional<Outcome> refusal(MinecraftServer server, Skirmish skirmish)
    {
        if (FlansTeams.getRunningRound().isPresent())
            return Optional.of(refused("round_running"));
        if (server.getLevel(skirmish.getDimension()) == null || skirmish.getState() != Skirmish.State.DRAFT)
            return Optional.of(refused("not_draft"));
        if (skirmish.getSides().size() < 2)
            return Optional.of(refused("too_few_sides"));
        BattleFactions factions = BattleFactions.get(server);
        for (Skirmish.Side side : skirmish.getSides())
        {
            Optional<BattleFactions.Managed> managed = factions.managed(side.faction());
            if (managed.isEmpty())
                return Optional.of(refused("not_managed", factionName(side.faction())));
            if (managed.get().getBranches().isEmpty())
                return Optional.of(refused("no_branches", factionName(side.faction())));
        }
        return anyWar(skirmish) ? Optional.empty() : Optional.of(refused("no_war"));
    }

    /**
     * Composes the forces of every side into the skirmish's settings and fixes the teams serving each side, noting what the
     * factions could not field.
     *
     * @return the refusal when a team would serve two sides, or empty
     */
    private static Optional<Outcome> compose(MinecraftServer server, Skirmish skirmish, List<Component> notes)
    {
        BattleFactions factions = BattleFactions.get(server);
        ServerLevel level = Objects.requireNonNull(server.getLevel(skirmish.getDimension()));
        BotSettings settings = settings(skirmish);
        Set<String> used = new HashSet<>();
        List<Skirmish.Side> started = new ArrayList<>();
        for (Skirmish.Side side : skirmish.getSides())
        {
            String name = factionName(side.faction());
            BattleFactions.Managed managed = factions.managed(side.faction()).orElseThrow();
            SkirmishComposer.Result result = SkirmishComposer.compose(SkirmishComposer.views(serving(level, managed, side.anchor(), name, notes)), side.request(), SkirmishComposer::roleOfType);
            for (Map.Entry<String, BotSettings.TeamForce> force : result.forces().entrySet())
            {
                if (!used.add(force.getKey()))
                    return Optional.of(refused("team_twice", force.getKey()));
                settings.getTeams().put(force.getKey(), skirmish.isRespawn() ? force.getValue() : once(force.getValue()));
            }
            result.missingRoles().forEach(role -> notes.add(text("missing_role", name, BotCommands.lower(role))));
            result.unknownTypes().forEach(type -> notes.add(text("unknown_type", name, type)));
            started.add(side.withTeams(List.copyOf(result.forces().keySet())));
        }
        skirmish.getSides().clear();
        skirmish.getSides().addAll(started);
        skirmish.setSettings(settings);
        return Optional.empty();
    }

    /** The branches of a faction that serve where a side comes in by their theatres; every branch, noted, when none does. */
    private static List<BattleFactions.Branch> serving(ServerLevel level, BattleFactions.Managed managed, Vec3 at, String name, List<Component> notes)
    {
        List<BattleFactions.Branch> serving = managed.getBranches().stream().filter(branch -> Theatres.serves(branch.getTheatres(), level, at)).toList();
        if (!serving.isEmpty())
            return serving;
        notes.add(text("no_theatre", name));
        return managed.getBranches();
    }

    /** Removes a skirmish and all its bots. */
    public static int stop(MinecraftServer server, Skirmish skirmish)
    {
        int removed = 0;
        BotBattle battle = skirmish.getBattle();
        if (battle != null)
        {
            removed = TeamsBots.removeAll(server, battle);
            BotBattles.remove(battle);
        }
        BotOrders.clearWaypoints(server);
        SkirmishStore.get(server).remove(skirmish);
        return removed;
    }

    /** Pauses or resumes a running skirmish; its bots rest from the next moment on, or stop resting. */
    public static boolean pause(MinecraftServer server, Skirmish skirmish, boolean paused)
    {
        if (!skirmish.isLive())
            return false;
        skirmish.setState(paused ? Skirmish.State.PAUSED : Skirmish.State.RUNNING);
        SkirmishStore.get(server).setDirty();
        return true;
    }

    /** The settings a skirmish's bots fight by; the teams' forces are added as they are composed. */
    private static BotSettings settings(Skirmish skirmish)
    {
        BotSettings settings = new BotSettings();
        settings.setEnabled(true);
        settings.setMode(BotSettings.Mode.FIXED);
        // Name tags only allies see go by Teams teams, which skirmish bots fight without.
        settings.setNameplates(BotSettings.Nameplates.ALWAYS);
        settings.setRespawnSeconds(skirmish.getRespawnSeconds());
        settings.setTickets(skirmish.isRespawn() ? skirmish.getTickets() : 0);
        settings.setDifficulty(skirmish.getDifficulty());
        settings.setPlayerTickets(false);
        settings.setRadio(false);
        // A squad leader's rally point is where the squad's fallen come back; coming back there is for round players only.
        settings.setRallyPoints(true);
        settings.setVehicleCrews(true);
        return settings;
    }

    /** A force each of whose soldiers, squads and vehicles fights once: every entry's budget is what it fields. */
    private static BotSettings.TeamForce once(BotSettings.TeamForce force)
    {
        force.setBudget(force.getCount());
        force.getSquads().replaceAll(squad -> new BotSettings.SquadEntry(squad.template(), squad.count(), squad.count()));
        force.getVehicles()
            .replaceAll(vehicle -> new BotSettings.VehicleEntry(vehicle.type(), vehicle.count(), vehicle.count(), vehicle.respawnSeconds(), vehicle.ticketCost(), vehicle.spawner(), vehicle.carry()));
        return force;
    }

    private static boolean anyWar(Skirmish skirmish)
    {
        List<Skirmish.Side> sides = skirmish.getSides();
        for (int i = 0; i < sides.size(); i++)
            for (int j = i + 1; j < sides.size(); j++)
                if (BattleFactions.attacks(sides.get(i).faction(), sides.get(j).faction()) || BattleFactions.attacks(sides.get(j).faction(), sides.get(i).faction()))
                    return true;
        return false;
    }

    /** The bots of a started skirmish: each team's bots join the faction of the side it serves. */
    private static BotBattle battle(MinecraftServer server, Skirmish skirmish)
    {
        Map<String, Integer> factions = new HashMap<>();
        for (Skirmish.Side side : skirmish.getSides())
            side.teams().forEach(team -> factions.put(team, side.faction()));
        return new BotBattle(skirmish.getId(), new SkirmishBattleground(skirmish, server), skirmish.getSettings(), factions);
    }

    private static Outcome refused(String key, Object... args)
    {
        return new Outcome(false, List.of(text(key, args)));
    }

    // --- Running ---

    /** Runs every skirmish one tick; called every server tick. */
    public static void tick(MinecraftServer server)
    {
        SkirmishStore store = SkirmishStore.get(server);
        if (!adopted)
        {
            adopt(server, store);
            adopted = true;
        }
        int tick = server.getTickCount();
        boolean upkeep = tick % UPKEEP_INTERVAL == 0;
        boolean round = FlansTeams.getRunningRound().isPresent();
        Set<ServerLevel> fighting = new HashSet<>();
        for (Skirmish skirmish : List.copyOf(store.all()))
            tick(server, store, skirmish, tick, round).ifPresent(fighting::add);
        if (upkeep)
            fighting.forEach(BotSuppression::tick);
    }

    /**
     * Runs one skirmish one tick: its bots rest while a round is played or it is paused, fight otherwise, and are removed a
     * while after it was decided.
     *
     * @return the level it is fought in, when its bots fought this tick
     */
    private static Optional<ServerLevel> tick(MinecraftServer server, SkirmishStore store, Skirmish skirmish, int tick, boolean round)
    {
        BotBattle battle = skirmish.getBattle();
        ServerLevel level = server.getLevel(skirmish.getDimension());
        if (battle == null || level == null)
            return Optional.empty();
        long now = level.getGameTime();
        boolean resting = round || skirmish.getState() == Skirmish.State.PAUSED;
        if (tick % UPKEEP_INTERVAL == 0)
        {
            rest(level, skirmish, resting);
            // The world saves the skirmish as it is now, whenever it is saved next.
            store.setDirty();
        }
        if (skirmish.getState() == Skirmish.State.ENDED)
        {
            if (now - skirmish.getEndedAt() >= END_LINGER)
                stop(server, skirmish);
            return Optional.empty();
        }
        if (resting)
            return Optional.empty();
        fight(server, level, skirmish, battle, tick, now);
        return Optional.of(level);
    }

    private static void fight(MinecraftServer server, ServerLevel level, Skirmish skirmish, BotBattle battle, int tick, long now)
    {
        RandomGenerator random = ThreadLocalRandom.current();
        if (tick % UPKEEP_INTERVAL == 0)
        {
            TeamsBots.upkeep(server, level, battle, now, random);
            moveFront(level, skirmish);
            decide(server, level, skirmish, now);
        }
        TeamsBots.spawnQueued(level, battle, now, random);
        BotGunners.tick(level, battle);
        if (tick % COMMAND_INTERVAL == 0)
        {
            BotCommander.plan(server, level, battle);
            BotOrders.sendWaypoints(server, battle);
        }
    }

    /** Takes up the skirmishes that were fighting when the server stopped, with the bots they had. */
    private static void adopt(MinecraftServer server, SkirmishStore store)
    {
        for (Skirmish skirmish : store.all())
        {
            ServerLevel level = server.getLevel(skirmish.getDimension());
            if (skirmish.getState() == Skirmish.State.DRAFT || level == null)
                continue;
            BotBattle battle = battle(server, skirmish);
            CompoundTag pending = skirmish.getPendingBattle();
            if (BotBattleCodec.instance(pending).filter(skirmish.getId()::equals).isPresent())
                BotBattleCodec.restore(battle, pending, level.getGameTime());
            skirmish.setPendingBattle(new CompoundTag());
            skirmish.setBattle(battle);
            BotBattles.add(battle);
        }
    }

    /** Lets go of the skirmishes as the server stops; they are saved with the world and taken up again when it starts. */
    public static void serverStopping(MinecraftServer server)
    {
        for (Skirmish skirmish : SkirmishStore.get(server).all())
            if (skirmish.getBattle() != null)
                BotBattles.remove(skirmish.getBattle());
        SkirmishStore.get(server).setDirty();
        adopted = false;
    }

    // --- Resting ---

    /** Makes the loaded bots of a skirmish rest, or stop resting, crews included. */
    private static void rest(ServerLevel level, Skirmish skirmish, boolean resting)
    {
        BotBattle battle = Objects.requireNonNull(skirmish.getBattle());
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (!(entity instanceof EntityNPCInterface npc))
                continue;
            rest(npc, resting, skirmish.isProtection());
            for (Entity rider : npc.getPassengers())
                if (rider instanceof EntityNPCInterface crew)
                    rest(crew, resting, skirmish.isProtection());
        }
    }

    /** A resting bot thinks nothing, attacks nobody and stands still; what it was before is kept to give back. */
    private static void rest(EntityNPCInterface npc, boolean resting, boolean protection)
    {
        CompoundTag data = npc.getPersistentData();
        boolean rests = data.contains(REST_KEY, Tag.TAG_COMPOUND);
        if (resting && !rests)
        {
            CompoundTag before = new CompoundTag();
            before.putBoolean(NBT_INVULNERABLE, npc.isInvulnerable());
            before.putBoolean(NBT_NO_AI, npc.isNoAi());
            data.put(REST_KEY, before);
            npc.setTarget(null);
            npc.getNavigation().stop();
            npc.setDeltaMovement(Vec3.ZERO);
            npc.setNoAi(true);
            if (protection)
                npc.setInvulnerable(true);
        }
        else if (!resting)
            wake(npc);
    }

    /** Gives a resting bot back what it was before it rested; nothing for one that does not rest. */
    static void wake(EntityNPCInterface npc)
    {
        CompoundTag data = npc.getPersistentData();
        if (!data.contains(REST_KEY, Tag.TAG_COMPOUND))
            return;
        CompoundTag before = data.getCompound(REST_KEY);
        npc.setNoAi(before.getBoolean(NBT_NO_AI));
        npc.setInvulnerable(before.getBoolean(NBT_INVULNERABLE));
        data.remove(REST_KEY);
    }

    // --- Fronts and outcomes ---

    /**
     * Places the front a skirmish's ground squads head for: none for a hunt, the middle between the zones for a meeting,
     * and for a push of two sides a point moving from the middle toward the weaker side.
     */
    static void moveFront(ServerLevel level, Skirmish skirmish)
    {
        List<Skirmish.Side> sides = skirmish.getSides();
        if (skirmish.getIntent() == Skirmish.Intent.HUNT || sides.size() < 2)
        {
            skirmish.setFront(null);
            return;
        }
        double share = 0.5D;
        if (skirmish.getIntent() == Skirmish.Intent.PUSH && sides.size() == 2 && skirmish.getBattle() != null)
            share = pushShare(strength(level, skirmish.getBattle(), 0), strength(level, skirmish.getBattle(), 1));
        Vec3 front = sides.size() == 2 ? sides.get(0).anchor().lerp(sides.get(1).anchor(), share) : sides.stream().map(Skirmish.Side::anchor).reduce(Vec3.ZERO, Vec3::add).scale(1D / sides.size());
        Vec3 previous = skirmish.getFront();
        if (previous == null || previous.distanceToSqr(front.x, previous.y, front.z) > 1D)
            skirmish.setFront(SkirmishBattleground.ground(level, front.x, front.z));
    }

    /** @return how far from the first side's zone toward the second's the front of a push lies, the stronger pushing on */
    static double pushShare(int first, int second)
    {
        int total = first + second;
        return total <= 0 ? 0.5D : 0.5D + PUSH_SHIFT * (first - second) / total;
    }

    /** @return the bots of a side standing now */
    static int strength(ServerLevel level, BotBattle battle, int side)
    {
        int standing = 0;
        for (BotSlot slot : battle.getSlots().values())
        {
            if (slot.getEntity() != null && battle.sideOf(slot.getTeam()) == side && level.getEntity(slot.getEntity()) instanceof LivingEntity living && living.isAlive())
                standing++;
        }
        return standing;
    }

    /** Ends a skirmish whose end has come: by wipeout once only sides at peace with each other stand, or by its time limit. */
    private static void decide(MinecraftServer server, ServerLevel level, Skirmish skirmish, long now)
    {
        BotBattle battle = Objects.requireNonNull(skirmish.getBattle());
        int sides = skirmish.getSides().size();
        int[] strengths = new int[sides];
        for (int side = 0; side < sides; side++)
            strengths[side] = strength(level, battle, side);
        Optional<List<Integer>> winners = switch (skirmish.getEnd())
        {
            case WIPEOUT -> wipedOut(battle, strengths);
            case TIME -> now - skirmish.getStartedAt() >= skirmish.getTimeLimitMinutes() * TICKS_PER_MINUTE ? Optional.of(leading(strengths)) : Optional.empty();
            // Only stopping it ends it.
            case NEVER -> Optional.empty();
        };
        winners.ifPresent(standing -> finish(server, level, skirmish, standing, now));
    }

    /**
     * @return the sides left once only sides at peace with each other stand, every other worn out: fallen to the last bot
     *         with nothing left to bring in; empty while sides at war with each other still stand
     */
    private static Optional<List<Integer>> wipedOut(BotBattle battle, int[] strengths)
    {
        List<Integer> standing = new ArrayList<>();
        for (int side = 0; side < strengths.length; side++)
        {
            boolean out = battle.getSidesThatFought().contains(side) && strengths[side] == 0 && !BotPopulation.canStillField(battle, side);
            if (!out)
                standing.add(side);
        }
        boolean war = standing.stream().anyMatch(side -> standing.stream().anyMatch(other -> battle.getGround().hostile(side, other)));
        return standing.size() < strengths.length && !war ? Optional.of(standing) : Optional.empty();
    }

    /** @return the side with the most bots standing, or none for a draw */
    private static List<Integer> leading(int[] strengths)
    {
        int most = Arrays.stream(strengths).max().orElse(0);
        List<Integer> leading = new ArrayList<>();
        for (int side = 0; side < strengths.length; side++)
            if (strengths[side] == most && most > 0)
                leading.add(side);
        return leading.size() == 1 ? leading : List.of();
    }

    private static void finish(MinecraftServer server, ServerLevel level, Skirmish skirmish, List<Integer> winners, long now)
    {
        skirmish.setState(Skirmish.State.ENDED);
        skirmish.setEndedAt(now);
        Objects.requireNonNull(skirmish.getBattle()).getQueue().clear();
        String names = String.join(", ", winners.stream().map(side -> factionName(skirmish.getSides().get(side).faction())).toList());
        announce(server, level, skirmish, winners.isEmpty() ? text("draw", skirmish.getName()) : text("won", names, skirmish.getName()));
    }

    /** Tells the players near the skirmish and the members of its factions. */
    private static void announce(MinecraftServer server, ServerLevel level, Skirmish skirmish, Component message)
    {
        BattleFactions factions = BattleFactions.get(server);
        List<Vec3> places = new ArrayList<>(skirmish.getSides().stream().map(Skirmish.Side::anchor).toList());
        if (skirmish.getFront() != null)
            places.add(skirmish.getFront());
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            boolean near = player.level() == level && places.stream().anyMatch(place -> place.distanceTo(player.position()) <= ANNOUNCE_RANGE);
            boolean member = factions.factionOf(player.getUUID()).map(faction -> skirmish.sideOf(faction) >= 0).orElse(false);
            if (near || member)
                player.sendSystemMessage(message);
        }
    }

    static String factionName(int id)
    {
        Faction faction = FactionController.instance.getFaction(id);
        return faction == null ? "#" + id : faction.name;
    }

    private static Component text(String key, Object... args)
    {
        return Component.translatable(PREFIX + key, args);
    }
}
