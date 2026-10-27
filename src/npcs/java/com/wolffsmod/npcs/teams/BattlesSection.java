package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsLiveRound;
import com.flansmodultimate.api.teams.ITeamsRound;
import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.function.Function;

/**
 * The module's section of the Teams command centre: the managed factions, the skirmishes and their presets, the wars
 * with the territory around the operator, and the bots of each round of the rotation, with the choices the screen
 * offers for them. The keys are shared with the screen's pages, which read the section. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BattlesSection
{
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "battles");
    /** Chunks shown on each side of the operator's chunk on a war's territory map. */
    public static final int MAP_RADIUS = 8;

    public static final String FACTIONS = "factions";
    public static final String OTHER_FACTIONS = "other_factions";
    public static final String TEAMS = "teams";
    public static final String SKIRMISHES = "skirmishes";
    public static final String PRESETS = "presets";
    public static final String WARS = "wars";
    public static final String ROUNDS = "rounds";
    public static final String BOT_PRESETS = "bot_presets";
    public static final String LIVE = "live";
    public static final String MARKERS = "markers";
    public static final String DIMENSIONS = "dimensions";
    public static final String HERE = "here";
    public static final String CHOICES = "choices";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String COLOUR = "colour";
    public static final String OUTSIDERS = "outsiders";
    public static final String MEMBERS = "members";
    public static final String MEMBER = "member";
    public static final String ENEMIES = "enemies";
    public static final String BRANCHES = "branches";
    public static final String TEAM = "team";
    public static final String WEIGHT = "weight";
    public static final String COUNT = "count";
    public static final String ROLES = "roles";
    public static final String THEATRES = "theatres";
    public static final String STATE = "state";
    public static final String INTENT = "intent";
    public static final String END = "end";
    public static final String MINUTES = "minutes";
    public static final String RESPAWN = "respawn";
    public static final String RESPAWN_SECONDS = "respawn_seconds";
    public static final String TICKETS = "tickets";
    public static final String DIFFICULTY = "difficulty";
    public static final String PROTECTION = "protection";
    public static final String MARKER_MAP = "marker_map";
    public static final String DIMENSION = "dimension";
    public static final String WAR = "war";
    public static final String SIDES = "sides";
    public static final String FACTION = "faction";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    public static final String RADIUS = "radius";
    public static final String INFANTRY = "infantry";
    public static final String SQUADS = "squads";
    public static final String VEHICLES = "vehicles";
    public static final String STANDING = "standing";
    public static final String DEATHS = "deaths";
    public static final String KILLS = "kills";
    public static final String RUNNING = "running";
    public static final String HOLDINGS = "holdings";
    public static final String SCOPE = "scope";
    public static final String RANGE = "range";
    public static final String ENGAGEMENTS = "engagements";
    public static final String MOST_ENGAGEMENTS = "most_engagements";
    public static final String FLIP = "flip";
    public static final String FIRST = "first";
    public static final String SECOND = "second";
    public static final String PROGRESS = "progress";
    public static final String MAP = "map";
    public static final String OWNERS = "owners";
    public static final String CONTESTED = "contested";
    public static final String CENTRE_X = "centre_x";
    public static final String CENTRE_Z = "centre_z";
    public static final String INDEX = "index";
    public static final String GAME_TYPE = "game_type";
    public static final String ENABLED = "enabled";
    public static final String CONFIGURED = "configured";
    public static final String TOTAL = "total";
    public static final String MODE = "mode";
    public static final String CURRENT = "current";
    public static final String ALIVE = "alive";
    public static final String SLOTS = "slots";

    /** The values each setting may take, by setting, as the commands spell them. */
    public static final String INTENTS = "intents";
    public static final String ENDS = "ends";
    public static final String DIFFICULTIES = "difficulties";
    public static final String OUTSIDER_RULES = "outsider_rules";
    public static final String MARKER_TYPES = "marker_types";
    public static final String BRANCH_ROLES = "branch_roles";

    public static CompoundTag build(ServerPlayer player)
    {
        MinecraftServer server = player.server;
        CompoundTag tag = new CompoundTag();
        tag.put(FACTIONS, factions(server, player));
        tag.put(OTHER_FACTIONS, otherFactions(server));
        tag.put(TEAMS, list(FlansModApi.getTeams(), team ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putString(ID, team.getShortName());
            entry.putString(NAME, team.getName());
            entry.putString("displayName", net.minecraft.network.chat.Component.Serializer.toJson(team.getNameComponent()));
            entry.putInt(COLOUR, team.getTeamColour());
            return entry;
        }));
        SkirmishStore skirmishes = SkirmishStore.get(server);
        tag.put(SKIRMISHES, list(skirmishes.all(), skirmish -> skirmish(server, skirmish)));
        tag.put(PRESETS, strings(new TreeSet<>(skirmishes.presets().keySet())));
        ChunkPos here = player.chunkPosition();
        tag.put(WARS, list(WarStore.get(server).all(), war -> war(server, war, player.serverLevel(), here)));
        tag.put(DIMENSIONS, strings(server.levelKeys().stream().map(key -> key.location().toString()).sorted().toList()));
        tag.putString(HERE, player.level().dimension().location().toString());
        tag.put(ROUNDS, rounds(server));
        tag.put(BOT_PRESETS, strings(new TreeSet<>(BotPresets.get().names())));
        TeamsBots.current().ifPresent(battle -> tag.put(LIVE, live(battle)));
        tag.put(CHOICES, choices());
        return tag;
    }

    private static <T> ListTag list(Collection<T> values, Function<T, CompoundTag> entry)
    {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(entry.apply(value)));
        return list;
    }

    private static ListTag strings(Collection<String> values)
    {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static <E extends Enum<E>> ListTag names(E[] values)
    {
        return strings(Arrays.stream(values).map(BotCommands::lower).toList());
    }

    private static CompoundTag choices()
    {
        CompoundTag tag = new CompoundTag();
        tag.put(INTENTS, names(Skirmish.Intent.values()));
        tag.put(ENDS, names(Skirmish.End.values()));
        tag.put(DIFFICULTIES, names(BotSettings.Difficulty.values()));
        tag.put(OUTSIDER_RULES, names(BattleFactions.Outsiders.values()));
        tag.put(MARKER_TYPES, names(BotMarkers.Type.values()));
        tag.put(BRANCH_ROLES, names(BotRole.values()));
        return tag;
    }

    // --- Factions ---

    private static ListTag factions(MinecraftServer server, ServerPlayer player)
    {
        BattleFactions data = BattleFactions.get(server);
        Optional<Integer> own = data.factionOf(player.getUUID());
        ListTag list = new ListTag();
        for (BattleFactions.Managed managed : data.all())
        {
            Faction faction = FactionController.instance.getFaction(managed.getId());
            if (faction == null)
                continue;
            CompoundTag entry = faction(faction);
            entry.putString(OUTSIDERS, BotCommands.lower(managed.getOutsiders()));
            entry.putInt(MEMBERS, (int) data.memberCount(managed.getId()));
            entry.putBoolean(MEMBER, own.map(id -> id == managed.getId()).orElse(false));
            entry.putIntArray(ENEMIES, faction.attackFactions.stream().mapToInt(Integer::intValue).sorted().toArray());
            entry.put(BRANCHES, list(managed.getBranches(), branch ->
            {
                CompoundTag branchTag = new CompoundTag();
                branchTag.putString(TEAM, branch.getTeam());
                branchTag.putInt(WEIGHT, branch.getWeight());
                branchTag.putInt(COUNT, branch.getForce().getCount());
                branchTag.putString(ROLES, String.join(" ", branch.getRoles().stream().map(BotCommands::lower).toList()));
                branchTag.putString(THEATRES, String.join(" ", branch.getTheatres()));
                branchTag.putString(SQUADS, BotSettingsText.formatSquads(branch.getForce().getSquads()));
                branchTag.putString(VEHICLES, BotSettingsText.formatVehicles(branch.getForce().getVehicles()));
                return branchTag;
            }));
            list.add(entry);
        }
        return list;
    }

    /** The Custom NPCs factions that are not managed, which managed factions may be at war or at peace with. */
    private static ListTag otherFactions(MinecraftServer server)
    {
        BattleFactions data = BattleFactions.get(server);
        ListTag list = new ListTag();
        FactionController.instance.factions.values().stream().filter(faction -> !BotFactions.isRoundFaction(faction.name) && data.managed(faction.id).isEmpty())
            .sorted(Comparator.comparing(faction -> faction.name)).forEach(faction -> list.add(faction(faction)));
        return list;
    }

    private static CompoundTag faction(Faction faction)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ID, faction.id);
        tag.putString(NAME, faction.name);
        tag.putString("displayName", net.minecraft.network.chat.Component.Serializer.toJson(com.wolffsmod.npcs.names.FactionDisplayName.of(faction)));
        tag.putInt(COLOUR, faction.color & 0xFFFFFF);
        return tag;
    }

    // --- Skirmishes ---

    private static CompoundTag skirmish(MinecraftServer server, Skirmish skirmish)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, skirmish.getName());
        tag.putString(STATE, BotCommands.lower(skirmish.getState()));
        tag.putString(INTENT, BotCommands.lower(skirmish.getIntent()));
        tag.putString(END, BotCommands.lower(skirmish.getEnd()));
        tag.putInt(MINUTES, skirmish.getTimeLimitMinutes());
        tag.putBoolean(RESPAWN, skirmish.isRespawn());
        tag.putInt(RESPAWN_SECONDS, skirmish.getRespawnSeconds());
        tag.putInt(TICKETS, skirmish.getTickets());
        tag.putString(DIFFICULTY, BotCommands.lower(skirmish.getDifficulty()));
        tag.putBoolean(PROTECTION, skirmish.isProtection());
        tag.putString(MARKER_MAP, skirmish.getMarkerMap());
        tag.putString(DIMENSION, skirmish.getDimension().location().toString());
        tag.putBoolean(WAR, skirmish.getWar() != null);
        BotBattle battle = skirmish.getBattle();
        ServerLevel level = server.getLevel(skirmish.getDimension());
        ListTag sides = new ListTag();
        for (int i = 0; i < skirmish.getSides().size(); i++)
        {
            Skirmish.Side side = skirmish.getSides().get(i);
            CompoundTag entry = new CompoundTag();
            entry.putInt(FACTION, side.faction());
            entry.putString(NAME, Skirmishes.factionName(side.faction()));
            Faction faction = FactionController.instance.getFaction(side.faction());
            entry.putInt(COLOUR, faction == null ? 0xFFFFFF : faction.color & 0xFFFFFF);
            entry.putInt(X, (int) Math.floor(side.anchor().x));
            entry.putInt(Y, (int) Math.floor(side.anchor().y));
            entry.putInt(Z, (int) Math.floor(side.anchor().z));
            entry.putDouble(RADIUS, side.radius());
            entry.putInt(INFANTRY, side.request().infantry());
            entry.putInt(SQUADS, side.request().squads().values().stream().mapToInt(Integer::intValue).sum());
            entry.putInt(VEHICLES,
                side.request().vehiclesByRole().values().stream().mapToInt(Integer::intValue).sum() + side.request().vehiclesByType().values().stream().mapToInt(Integer::intValue).sum());
            if (battle != null)
            {
                int index = i;
                entry.putInt(STANDING, level == null ? 0 : Skirmishes.strength(level, battle, i));
                entry.putInt(DEATHS, battle.getSlots().values().stream().filter(slot -> battle.sideOf(slot.getTeam()) == index).mapToInt(BotSlot::getDeaths).sum());
                entry.putInt(KILLS, battle.getSlots().values().stream().filter(slot -> battle.sideOf(slot.getTeam()) == index).mapToInt(BotSlot::getKills).sum());
                entry.putInt(TICKETS, skirmish.isRespawn() && skirmish.getTickets() > 0 ? BotPopulation.ticketsLeft(battle, i) : -1);
            }
            sides.add(entry);
        }
        tag.put(SIDES, sides);
        return tag;
    }

    // --- Wars ---

    private static CompoundTag war(MinecraftServer server, War war, ServerLevel level, ChunkPos here)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, war.getName());
        tag.putBoolean(RUNNING, war.isRunning());
        tag.putIntArray(FACTIONS, war.getFactions().stream().mapToInt(Integer::intValue).toArray());
        Map<Integer, Integer> holdings = war.holdings();
        tag.putIntArray(HOLDINGS, war.getFactions().stream().mapToInt(faction -> holdings.getOrDefault(faction, 0)).toArray());
        tag.put(SCOPE, strings(war.getDimensions().stream().map(dimension -> dimension.location().toString()).toList()));
        tag.putInt(INFANTRY, war.getInfantry());
        tag.putInt(RESPAWN_SECONDS, war.getRespawnSeconds());
        tag.putInt(TICKETS, war.getTickets());
        tag.putInt(RANGE, war.getRange());
        tag.putInt(MOST_ENGAGEMENTS, war.getMostEngagements());
        tag.putInt(FLIP, war.getFlipSeconds());
        tag.putString(DIFFICULTY, BotCommands.lower(war.getDifficulty()));
        SkirmishStore skirmishes = SkirmishStore.get(server);
        ListTag engagements = new ListTag();
        Set<Long> contested = new HashSet<>();
        for (War.Engagement engagement : war.getEngagements())
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt(FIRST, engagement.getBorder().firstFaction());
            entry.putInt(SECOND, engagement.getBorder().secondFaction());
            ChunkPos chunk = new ChunkPos(engagement.getBorder().second());
            entry.putInt(X, chunk.getMiddleBlockX());
            entry.putInt(Z, chunk.getMiddleBlockZ());
            entry.putInt(PROGRESS, engagement.getProgress());
            Optional<Skirmish> skirmish = skirmishes.byId(engagement.getSkirmish());
            entry.putString(NAME, skirmish.map(Skirmish::getName).orElse("?"));
            if (skirmish.map(found -> found.getDimension().equals(level.dimension())).orElse(false))
            {
                contested.add(engagement.getBorder().first());
                contested.add(engagement.getBorder().second());
            }
            engagements.add(entry);
        }
        tag.put(ENGAGEMENTS, engagements);
        tag.put(MAP, territory(war, level, here, contested));
        return tag;
    }

    /**
     * Who holds each chunk around the operator's, row by row from the north-west: the faction's place in the war's list,
     * or -1 for no one.
     */
    private static CompoundTag territory(War war, ServerLevel level, ChunkPos here, Set<Long> contested)
    {
        Map<Long, Integer> owners = war.getTerritory().getOrDefault(level.dimension(), Map.of());
        int size = 2 * MAP_RADIUS + 1;
        int[] cells = new int[size * size];
        byte[] fought = new byte[size * size];
        for (int z = 0; z < size; z++)
        {
            for (int x = 0; x < size; x++)
            {
                long chunk = ChunkPos.asLong(here.x - MAP_RADIUS + x, here.z - MAP_RADIUS + z);
                Integer owner = owners.get(chunk);
                cells[z * size + x] = owner == null ? -1 : war.getFactions().indexOf(owner);
                fought[z * size + x] = (byte) (contested.contains(chunk) ? 1 : 0);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt(CENTRE_X, here.x);
        tag.putInt(CENTRE_Z, here.z);
        tag.putInt(RADIUS, MAP_RADIUS);
        tag.putIntArray(OWNERS, cells);
        tag.putByteArray(CONTESTED, fought);
        tag.putBoolean(DIMENSION, war.covers(level.dimension()));
        return tag;
    }

    // --- Bots of the rotation ---

    private static ListTag rounds(MinecraftServer server)
    {
        List<ITeamsRound> rounds = FlansTeams.getRounds();
        ITeamsRound current = FlansTeams.getRunningRound().map(ITeamsLiveRound::getRound).orElse(null);
        BotMarkers markers = BotMarkers.get(server);
        ListTag list = new ListTag();
        for (int i = 0; i < rounds.size(); i++)
        {
            ITeamsRound round = rounds.get(i);
            CompoundTag settingsTag = round.getExtensionData(BotSettings.KEY);
            BotSettings settings = BotSettings.load(settingsTag);
            CompoundTag entry = new CompoundTag();
            entry.putInt(INDEX, i);
            entry.putString(ID, round.getId().toString());
            entry.putString(MAP, round.getMapId());
            entry.putString(GAME_TYPE, round.getGameTypeId());
            entry.put(TEAMS, strings(round.getTeams().stream().map(ITeam::getShortName).toList()));
            entry.putBoolean(CONFIGURED, !settingsTag.isEmpty());
            entry.putBoolean(ENABLED, settings.isEnabled());
            entry.putInt(TOTAL, settings.getTotal());
            entry.putString(MODE, BotCommands.lower(settings.getMode()));
            entry.putString(DIFFICULTY, BotCommands.lower(settings.getDifficulty()));
            entry.putInt(MARKERS, markers.of(round.getMapId()).size());
            entry.putBoolean(CURRENT, round == current);
            list.add(entry);
        }
        return list;
    }

    private static CompoundTag live(BotBattle battle)
    {
        CompoundTag tag = new CompoundTag();
        ListTag teams = new ListTag();
        for (ITeam team : battle.getGround().teams())
        {
            CompoundTag entry = new CompoundTag();
            entry.putString(TEAM, team.getShortName());
            entry.putInt(COLOUR, team.getTeamColour());
            entry.putInt(ALIVE, (int) battle.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && slot.getEntity() != null && slot.getDiedAt() == 0).count());
            entry.putInt(SLOTS, (int) battle.getSlots().values().stream().filter(slot -> slot.getTeam().equals(team.getShortName()) && !slot.isRetired()).count());
            entry.putInt(TICKETS, battle.getSettings().getTickets() > 0 ? BotPopulation.ticketsLeft(battle, battle.getGround().sideOf(team)) : -1);
            teams.add(entry);
        }
        tag.put(TEAMS, teams);
        return tag;
    }
}
