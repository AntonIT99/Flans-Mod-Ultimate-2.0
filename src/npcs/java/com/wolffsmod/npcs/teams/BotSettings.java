package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcTuning.AircraftRole;
import com.wolffsmod.npcs.tactics.NpcTacticalProfile;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * How bots fight in one round of the Teams rotation: how many of them each team fields, what they are, how they look, and
 * what they may spend. Kept with the round as its extension data, so it travels with the rotation. Server thread.
 */
@Getter
@Setter
public final class BotSettings
{
    /** The key the settings are saved under in a round's extension data. */
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "bots");

    private static final String NBT_ENABLED = "enabled";
    private static final String NBT_VEHICLES_ENABLED = "vehicles_enabled";
    private static final String NBT_MODE = "mode";
    private static final String NBT_TOTAL = "total";
    private static final String NBT_NAMEPLATES = "nameplates";
    private static final String NBT_SCOREBOARD = "scoreboard";
    private static final String NBT_INFANTRY_WEIGHT = "infantry_capture_weight";
    private static final String NBT_VEHICLE_WEIGHT = "vehicle_capture_weight";
    private static final String NBT_RESPAWN = "respawn_seconds";
    private static final String NBT_VEHICLE_RESPAWN = "vehicle_respawn_seconds";
    private static final String NBT_TICKETS = "tickets";
    private static final String NBT_SQUAD_SIZE = "squad_size";
    private static final String NBT_ABANDON = "abandon_chance";
    private static final String NBT_RUBBER_BAND = "rubber_band";
    private static final String NBT_DIFFICULTY = "difficulty";
    static final String NBT_CUSTOM_DIFFICULTY = "custom_difficulty";
    private static final String NBT_RADIO = "radio";
    private static final String NBT_COMMANDER_DISRUPTION = "commander_disruption";
    private static final String NBT_RETREAT = "retreat_health";
    private static final String NBT_SUPPRESSION = "suppression";
    private static final String NBT_RALLY = "rally_points";
    private static final String NBT_VEHICLE_CREWS = "vehicle_crews";
    private static final String NBT_EMPLACEMENTS = "emplacements";
    private static final String NBT_PLAYER_SEAT_DEPLOYMENT = "player_seat_deployment";
    private static final String NBT_DRIVE_REAL_VEHICLES = "drive_real_vehicles";
    private static final String NBT_GUN_REAL_VEHICLES = "gun_real_vehicles";
    private static final String NBT_RIDE_REAL_VEHICLES = "ride_real_vehicles";
    private static final String NBT_TEAMS = "teams";
    /** The custom values a round, skirmish or war starts with: those of Regular. */
    public static final BotTraining DEFAULT_CUSTOM = Difficulty.REGULAR.training(null);

    /** How the number of loose infantry bots per team is set. */
    public enum Mode
    {
        /** Each team fields exactly its count. */
        FIXED,
        /** A total is shared out between the teams by their counts, used as weights. */
        SHARE,
        /** Bots keep each team at its count of fighters, players included, and leave as players join. */
        FILL
    }

    /** Who sees the name tags of bots. */
    public enum Nameplates
    {
        /** As each bot's own Custom NPCs setting says. */
        DEFAULT, ALWAYS,
        /** Only players on the bot's side. */
        ALLIES, NEVER
    }

    /** How bots appear on the Teams scoreboard. */
    public enum Scoreboard
    {
        INDIVIDUAL, GROUPED,
        /** Individually up to a dozen bots a team, grouped above that. */
        AUTO
    }

    /** What a soldier of a class does for its squad besides fighting, decided from its equipment unless set. */
    public enum ClassRole
    {
        /** Fights and nothing else. */
        RIFLEMAN,
        /** Heals hurt allies near it, with the heal amount of its class's medical tool. */
        MEDIC,
        /** Repairs damaged allied vehicles near it, with the repair amount of its class's repair tool. */
        ENGINEER,
        /** Hands out ammunition to allies near it. */
        SUPPORT,
        /** Goes after enemy vehicles before anything else. */
        ANTI_TANK,
        /** Leads close-range advances and enters assigned capture areas. */
        ASSAULT,
        /** Closes for melee, allowing an opening throw on the approach. */
        MELEE,
        /** Keeps distance to fight with bows, javelins or other ranged weapons. */
        SKIRMISHER
    }

    /**
     * How well bots fight, from the easiest up: how much of what their loadout or clone gives them they make use of (aim
     * and how far they notice enemies), and the human limits on their fighting. Every level keeps the health and rate of
     * fire bots are given, and none makes a bot better than its equipment.
     */
    public enum Difficulty
    {
        // spotless:off
        /** Shoots a lot and rarely hits, and deals a quarter of its damage to players. */
        TRAINEE(0.45F, 0.65F, NpcSkill.Level.TRAINEE),
        /** Below Regular, hits on players deal less damage: half of it for a recruit. */
        RECRUIT(0.55F, 0.75F, NpcSkill.Level.RECRUIT),
        /** The default. */
        REGULAR(0.7F, 0.85F, NpcSkill.Level.REGULAR),
        VETERAN(0.85F, 0.95F, NpcSkill.Level.VETERAN),
        /** Nearly its equipment's full potential, with a human's reaction: about as hard as a skilled player. */
        ELITE(0.95F, 1F, NpcSkill.Level.ELITE),
        /** Fights to the full potential of its equipment, without any human limit: it fires the moment it sees a target. */
        ACE(1F, 1F, NpcSkill.Level.ACE),
        /** Fights as the custom values of its round, skirmish or war say; the values here stand for none of them. */
        CUSTOM(1F, 1F, NpcSkill.Level.CUSTOM);
        // spotless:on

        private final float accuracy;
        private final float aggro;
        /** Reaction, aim settling, lead and damage to players. */
        private final NpcSkill.Level level;

        Difficulty(float accuracy, float aggro, NpcSkill.Level level)
        {
            this.accuracy = accuracy;
            this.aggro = aggro;
            this.level = level;
        }

        /** Whether the level changes the stats of a bot's loadout or clone at all. */
        public boolean scalesStats()
        {
            return Float.compare(accuracy, 1F) != 0 || Float.compare(aggro, 1F) != 0;
        }

        /** The human limits on how bots of this level fight. */
        public NpcSkill skill()
        {
            return level.getSkill();
        }

        /** Factor on the ranged accuracy, which stays within 0 to 100. */
        public float accuracy()
        {
            return accuracy;
        }

        /** Factor on the range within which bots notice enemies. */
        public float aggro()
        {
            return aggro;
        }

        /** @return what the level trains bots to; for {@link #CUSTOM}, the custom values given */
        public BotTraining training(BotTraining custom)
        {
            return this == CUSTOM ? custom : new BotTraining(accuracy, aggro, skill());
        }

        /** @return the preset levels, from the easiest up, without {@link #CUSTOM} */
        public static List<Difficulty> presets()
        {
            return Arrays.stream(values()).filter(level -> level != CUSTOM).toList();
        }
    }

    private boolean enabled = true;
    /** Whether the saved vehicle allocations are fielded. */
    private boolean vehiclesEnabled = true;
    private Mode mode = Mode.FIXED;
    /** Bots shared out between the teams in {@link Mode#SHARE}. */
    private int total = 32;
    private Nameplates nameplates = Nameplates.ALLIES;
    private Scoreboard scoreboard = Scoreboard.AUTO;
    private float infantryCaptureWeight = 1F;
    private float vehicleCaptureWeight = 1F;
    /** Seconds a fallen unit waits before it can come back. */
    private int respawnSeconds = 10;
    /** Seconds each destroyed vehicle waits before replacement, unless its entry overrides this. */
    private int vehicleRespawnSeconds = 10;
    /** Bot respawn budget per side; ignored in Teams objective modes. Standalone battles also use it for attrition. 0 for no limit. */
    private int tickets;
    /** How many loose infantry bots form one squad. */
    private int squadSize = 6;
    /** Chance, in percent, that a destroyed vehicle bot leaves a damaged vehicle players can repair and use. */
    private int abandonChance = 25;
    /** Most extra infantry, in percent of its force, a side gets while it trails on score; 0 switches it off. */
    private int rubberBand;
    private Difficulty difficulty = Difficulty.REGULAR;
    /** What bots are trained to at {@link Difficulty#CUSTOM}; it starts as Regular. */
    private BotTraining customTraining = DEFAULT_CUSTOM;
    /** Whether bots call out on the radio what happens at the objectives. */
    private boolean radio = true;
    /** Spawn a vulnerable commander for each side. */
    private boolean commanderBot;
    private int commanderDisruptionSeconds = 20;
    /** Share of its health, in percent, below which a soldier falls back to a medic or its spawn; 0 never. */
    private int retreatHealth = 30;
    /** Whether rounds passing close by make bots aim worse and stop advancing. */
    private boolean suppression = true;
    /** Whether players leading a squad may place a rally point for themselves and their squad to spawn at. */
    private boolean rallyPoints = true;
    /** Whether vehicle bots spawn with soldiers of their team in their seats. */
    private boolean vehicleCrews = true;
    /** Whether soldiers place and man deployable and AA guns. */
    private boolean emplacements = true;
    /** Whether allied players may deploy into NPC-occupied seats in real vehicles and AA guns. */
    private boolean playerSeatDeployment = true;
    /** Whether soldiers drive, gun or travel in real Flan driveables, independently of generated vehicle NPCs. */
    private boolean driveRealVehicles = true;
    private boolean gunRealVehicles = true;
    private boolean rideRealVehicles = true;
    private NpcTacticalProfile tacticalProfile = NpcTacticalProfile.INHERIT;
    private final Map<String, TeamForce> teams = new LinkedHashMap<>();

    /** What one team fields. */
    @Getter
    @Setter
    public static final class TeamForce
    {
        private static final String NBT_COUNT = "count";
        private static final String NBT_BUDGET = "budget";
        private static final String NBT_NAME_LIST = "name_list";
        private static final String NBT_CLONE_TAB = "clone_tab";
        private static final String NBT_CLONE_NAME = "clone_name";
        private static final String NBT_KEEP_EQUIPMENT = "keep_clone_equipment";
        private static final String NBT_ROLES = "class_roles";
        private static final String NBT_CLASSES = "classes";
        private static final String NBT_CLASS_LIMITS = "class_limits";
        private static final String NBT_SQUADS = "squads";
        private static final String NBT_VEHICLES = "vehicles";
        private static final String NBT_CREW = "crew";
        private static final String NBT_CREW_OVERRIDES = "crew_overrides";
        /** The crew class that leaves its seats empty. */
        public static final String NO_CREW = "none";

        /** Loose infantry: a count, a share weight or a fill target, depending on the mode. */
        private int count;
        private String commanderClass = "";
        /** Loose infantry spawns the team may make in all; 0 for no limit. */
        private int budget;
        /** Loose infantry squad size, or zero to inherit the round's legacy default. */
        private int squadSize;
        /** The soldier name list bots are named from, such as {@code german_20th}; empty keeps the template's names. */
        private String nameList = "";
        /** The Custom NPCs clone bots are made from; tab 0 or an empty name makes plain NPCs. */
        private int cloneTab;
        private String cloneName = "";
        /** Whether bots made from the clone keep its weapons and armour rather than getting their class's loadout. */
        private boolean keepCloneEquipment;
        /** Weight of each class bots are given; a class left out weighs 1 when no class is listed, 0 otherwise. */
        private final Map<String, Integer> classWeights = new LinkedHashMap<>();
        /** Optional per-class caps for automatically assigned infantry; independent of player caps and explicit rosters. */
        private final Map<String, ClassLimit> classLimits = new LinkedHashMap<>();
        /** The role of each class set by hand; a class left out gets the role its equipment suggests. */
        private final Map<String, ClassRole> classRoles = new LinkedHashMap<>();
        private final List<SquadEntry> squads = new ArrayList<>();
        private final List<VehicleEntry> vehicles = new ArrayList<>();
        /**
         * The class that crews each kind of seat of the team's vehicles: a class of the team, {@link #NO_CREW} for none, or,
         * left out, a class picked by the class weights.
         */
        @Getter(AccessLevel.NONE)
        private final Map<CrewPost, String> crewClasses = new EnumMap<>(CrewPost.class);
        /** Sparse seat overrides by kind or definition. Empty values explicitly select a class by weight. */
        @Getter(AccessLevel.NONE)
        private final Map<String, Map<CrewPost, String>> crewOverrides = new LinkedHashMap<>();

        CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt(NBT_COUNT, count);
            tag.putString("commander_class", commanderClass);
            tag.putInt(NBT_BUDGET, budget);
            if (squadSize > 0)
                tag.putInt(NBT_SQUAD_SIZE, squadSize);
            tag.putString(NBT_NAME_LIST, nameList);
            tag.putInt(NBT_CLONE_TAB, cloneTab);
            tag.putString(NBT_CLONE_NAME, cloneName);
            tag.putBoolean(NBT_KEEP_EQUIPMENT, keepCloneEquipment);
            CompoundTag classes = new CompoundTag();
            classWeights.forEach(classes::putInt);
            tag.put(NBT_CLASSES, classes);
            CompoundTag limits = new CompoundTag();
            classLimits.forEach((playerClass, limit) -> limits.put(playerClass, limit.save()));
            tag.put(NBT_CLASS_LIMITS, limits);
            CompoundTag roles = new CompoundTag();
            classRoles.forEach((playerClass, role) -> roles.putString(playerClass, role.name()));
            tag.put(NBT_ROLES, roles);
            ListTag squadList = new ListTag();
            squads.forEach(squad -> squadList.add(squad.save()));
            tag.put(NBT_SQUADS, squadList);
            ListTag vehicleList = new ListTag();
            vehicles.forEach(vehicle -> vehicleList.add(vehicle.save()));
            tag.put(NBT_VEHICLES, vehicleList);
            CompoundTag crew = new CompoundTag();
            crewClasses.forEach((post, playerClass) -> crew.putString(post.id(), playerClass));
            tag.put(NBT_CREW, crew);
            CompoundTag overrides = new CompoundTag();
            crewOverrides.forEach((scope, seats) ->
            {
                CompoundTag values = new CompoundTag();
                seats.forEach((post, playerClass) -> values.putString(post.id(), playerClass));
                overrides.put(scope, values);
            });
            tag.put(NBT_CREW_OVERRIDES, overrides);
            return tag;
        }

        static TeamForce load(CompoundTag tag)
        {
            TeamForce force = new TeamForce();
            force.count = Math.max(0, tag.getInt(NBT_COUNT));
            force.commanderClass = tag.getString("commander_class");
            force.budget = Math.max(0, tag.getInt(NBT_BUDGET));
            force.squadSize = Math.max(0, tag.getInt(NBT_SQUAD_SIZE));
            force.nameList = tag.getString(NBT_NAME_LIST);
            force.cloneTab = tag.getInt(NBT_CLONE_TAB);
            force.cloneName = tag.getString(NBT_CLONE_NAME);
            force.keepCloneEquipment = tag.getBoolean(NBT_KEEP_EQUIPMENT);
            CompoundTag classes = tag.getCompound(NBT_CLASSES);
            for (String name : classes.getAllKeys())
                force.classWeights.put(name, Math.max(0, classes.getInt(name)));
            CompoundTag limits = tag.getCompound(NBT_CLASS_LIMITS);
            for (String name : limits.getAllKeys())
                force.classLimits.put(name, ClassLimit.load(limits.getCompound(name)));
            CompoundTag roles = tag.getCompound(NBT_ROLES);
            for (String playerClass : roles.getAllKeys())
                force.classRoles.put(playerClass, parse(ClassRole.class, roles.getString(playerClass), ClassRole.RIFLEMAN));
            for (Tag entry : tag.getList(NBT_SQUADS, Tag.TAG_COMPOUND))
                force.squads.add(SquadEntry.load((CompoundTag) entry));
            for (Tag entry : tag.getList(NBT_VEHICLES, Tag.TAG_COMPOUND))
                force.vehicles.add(VehicleEntry.load((CompoundTag) entry));
            CompoundTag crew = tag.getCompound(NBT_CREW);
            for (CrewPost post : CrewPost.values())
                force.setCrewClass(post, crew.getString(post.id()));
            CompoundTag overrides = tag.getCompound(NBT_CREW_OVERRIDES);
            for (String scope : overrides.getAllKeys())
            {
                CompoundTag values = overrides.getCompound(scope);
                for (CrewPost post : CrewPost.values())
                    if (values.contains(post.id(), Tag.TAG_STRING))
                        force.setCrewClass(scope, post, values.getString(post.id()));
            }
            return force;
        }

        /** @return the class that crews a kind of seat: a short name, {@link #NO_CREW}, or empty for one picked by weight */
        public String crewClass(CrewPost post)
        {
            return crewClasses.getOrDefault(post, "");
        }

        public void setCrewClass(CrewPost post, String playerClass)
        {
            String value = playerClass == null ? "" : playerClass.strip();
            if (value.isEmpty())
                crewClasses.remove(post);
            else
                crewClasses.put(post, value);
        }

        /** Stable scopes distinguish a definition from a kind even when their names coincide. */
        public static String crewScope(FlanModelKind kind)
        {
            return "kind:" + kind.name().toLowerCase(Locale.ROOT);
        }

        public static String crewDefinitionScope(String definition)
        {
            return "definition:" + definition;
        }

        public Set<String> crewScopes()
        {
            return Collections.unmodifiableSet(crewOverrides.keySet());
        }

        /** Empty scope edits shared defaults; null at other scopes means inherit. */
        @Nullable
        public String crewClass(String scope, CrewPost post)
        {
            return scope.isEmpty() ? crewClass(post) : crewOverrides.getOrDefault(scope, Map.of()).get(post);
        }

        /** Null removes an override; empty explicitly chooses a class by weight. */
        public void setCrewClass(String scope, CrewPost post, @Nullable String playerClass)
        {
            if (scope.isEmpty())
            {
                setCrewClass(post, playerClass);
                return;
            }
            if (playerClass != null)
                crewOverrides.computeIfAbsent(scope, ignored -> new EnumMap<>(CrewPost.class)).put(post, playerClass.strip());
            else if (crewOverrides.containsKey(scope))
            {
                Map<CrewPost, String> seats = crewOverrides.get(scope);
                seats.remove(post);
                if (seats.isEmpty())
                    crewOverrides.remove(scope);
            }
        }

        /** Definition overrides take precedence per seat over kind overrides and shared defaults. */
        public String effectiveCrewClass(String definition, @Nullable FlanModelKind kind, CrewPost post)
        {
            String value = crewClass(crewDefinitionScope(definition), post);
            if (value == null && kind != null)
                value = crewClass(crewScope(kind), post);
            return value == null ? crewClass(post) : value;
        }

        /** Copies only crew settings, keeping drafts independent of the force being edited. */
        public void copyCrewFrom(TeamForce source)
        {
            if (source == this)
                return;
            crewClasses.clear();
            crewClasses.putAll(source.crewClasses);
            crewOverrides.clear();
            source.crewOverrides.forEach((scope, seats) -> crewOverrides.put(scope, new EnumMap<>(seats)));
        }

        /**
         * @param classes
         *            the short names of the team's classes
         * @return the weight each of the classes is given by: its listed weight, 0 for one left out, or 1 for each while
         *         none of them has a weight
         */
        public Map<String, Integer> effectiveClassWeights(List<String> classes)
        {
            boolean weighted = classes.stream().anyMatch(playerClass -> classWeights.getOrDefault(playerClass, 0) > 0);
            Map<String, Integer> weights = new LinkedHashMap<>();
            for (String playerClass : classes)
                weights.put(playerClass, weighted ? Math.max(0, classWeights.getOrDefault(playerClass, 0)) : 1);
            return weights;
        }

        /** Slots of one class within this team's automatically assigned infantry target. */
        public int classCapacity(String playerClass, int infantryTarget)
        {
            return classLimits.getOrDefault(playerClass, ClassLimit.UNLIMITED).capacity(infantryTarget);
        }

        /** Whether bots are made from a Custom NPCs clone rather than as plain NPCs. */
        public boolean usesClone()
        {
            return cloneTab > 0 && !cloneName.isBlank();
        }
    }

    /** Bot-only infantry caps; negative values mean unlimited, and percentages round up. */
    public record ClassLimit(int count, float percent)
    {
        private static final String NBT_COUNT = "count";
        private static final String NBT_PERCENT = "percent";
        public static final ClassLimit UNLIMITED = new ClassLimit(-1, -1F);

        public ClassLimit
        {
            count = Math.max(-1, count);
            percent = Float.isFinite(percent) && percent >= 0F ? Math.min(100F, percent) : -1F;
        }

        public int capacity(int infantryTarget)
        {
            int capacity = count < 0 ? Integer.MAX_VALUE : count;
            return percent < 0F ? capacity : Math.min(capacity, (int) Math.ceil(Math.max(0, infantryTarget) * (double) percent / 100D));
        }

        private CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt(NBT_COUNT, count);
            tag.putFloat(NBT_PERCENT, percent);
            return tag;
        }

        private static ClassLimit load(CompoundTag tag)
        {
            return new ClassLimit(tag.contains(NBT_COUNT, Tag.TAG_ANY_NUMERIC) ? tag.getInt(NBT_COUNT) : -1, tag.contains(NBT_PERCENT, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(NBT_PERCENT) : -1F);
        }
    }

    /**
     * Squads of one template the team fields at once.
     *
     * @param budget
     *            squads of it the team may field in all, the first included; 0 for no limit
     */
    public record SquadEntry(String template, int count, int budget)
    {
        private static final String NBT_TEMPLATE = "template";
        private static final String NBT_COUNT = "count";
        private static final String NBT_BUDGET = "budget";

        public SquadEntry
        {
            count = Math.max(0, count);
            budget = Math.max(0, budget);
        }

        CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_TEMPLATE, template);
            tag.putInt(NBT_COUNT, count);
            tag.putInt(NBT_BUDGET, budget);
            return tag;
        }

        static SquadEntry load(CompoundTag tag)
        {
            return new SquadEntry(tag.getString(NBT_TEMPLATE), tag.getInt(NBT_COUNT), tag.getInt(NBT_BUDGET));
        }
    }

    /**
     * Vehicles of one type the team fields at once.
     *
     * @param type
     *            short name of the vehicle, plane, mecha or AA gun definition
     * @param budget
     *            vehicles of it the team may field in all, the first included; 0 for no limit
     * @param respawnSeconds
     *            seconds a destroyed one takes to come back; 0 takes the round's vehicle respawn time
     * @param ticketCost
     *            tickets one costs to bring back
     * @param spawner
     *            the vehicle spawner it appears at, or null for any the team holds
     * @param carry
     *            the squad template it carries to the front and unloads there, or drops by parachute from the air; empty
     *            for none
     * @param paintjobs
     *            allowed definition-local paintjob IDs; empty permits every available paintjob
     */
    public record VehicleEntry(String type, int count, int budget, int respawnSeconds, int ticketCost, @Nullable UUID spawner, String carry, List<Integer> paintjobs, AircraftRole aircraftRole)
    {
        private static final String NBT_TYPE = "type";
        private static final String NBT_COUNT = "count";
        private static final String NBT_BUDGET = "budget";
        private static final String NBT_RESPAWN = "respawn_seconds";
        private static final String NBT_TICKET_COST = "ticket_cost";
        private static final String NBT_SPAWNER = "spawner";
        private static final String NBT_CARRY = "carry";
        private static final String NBT_PAINTJOBS = "paintjobs";

        /** New allocations allow every available paintjob by default. */
        public VehicleEntry(String type, int count, int budget, int respawnSeconds, int ticketCost, @Nullable UUID spawner, String carry)
        {
            this(type, count, budget, respawnSeconds, ticketCost, spawner, carry, List.of());
        }

        public VehicleEntry(String type, int count, int budget, int respawnSeconds, int ticketCost, @Nullable UUID spawner, String carry, List<Integer> paintjobs)
        {
            this(type, count, budget, respawnSeconds, ticketCost, spawner, carry, paintjobs, AircraftRole.AUTO);
        }

        public VehicleEntry withAircraftRole(AircraftRole role)
        {
            return new VehicleEntry(type, count, budget, respawnSeconds, ticketCost, spawner, carry, paintjobs, role);
        }

        public VehicleEntry
        {
            count = Math.max(0, count);
            budget = Math.max(0, budget);
            respawnSeconds = Math.max(0, respawnSeconds);
            ticketCost = Math.max(0, ticketCost);
            carry = carry == null ? "" : carry;
            paintjobs = paintjobs.stream().filter(id -> id >= 0).distinct().toList();
            aircraftRole = aircraftRole == null ? AircraftRole.AUTO : aircraftRole;
        }

        CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_TYPE, type);
            tag.putInt(NBT_COUNT, count);
            tag.putInt(NBT_BUDGET, budget);
            tag.putInt(NBT_RESPAWN, respawnSeconds);
            tag.putInt(NBT_TICKET_COST, ticketCost);
            if (spawner != null)
                tag.putUUID(NBT_SPAWNER, spawner);
            tag.putString(NBT_CARRY, carry);
            tag.putIntArray(NBT_PAINTJOBS, paintjobs);
            tag.putString("aircraft_role", aircraftRole.name());
            return tag;
        }

        static VehicleEntry load(CompoundTag tag)
        {
            return new VehicleEntry(tag.getString(NBT_TYPE), tag.getInt(NBT_COUNT), tag.getInt(NBT_BUDGET), tag.getInt(NBT_RESPAWN), tag.getInt(NBT_TICKET_COST),
                tag.hasUUID(NBT_SPAWNER) ? tag.getUUID(NBT_SPAWNER) : null, tag.getString(NBT_CARRY),
                tag.contains(NBT_PAINTJOBS) ? Arrays.stream(tag.getIntArray(NBT_PAINTJOBS)).boxed().toList() : List.of(0),
                parse(AircraftRole.class, tag.getString("aircraft_role"), AircraftRole.AUTO));
        }
    }

    /** The force of a team, made empty on first use. */
    public TeamForce team(String team)
    {
        return teams.computeIfAbsent(team, ignored -> new TeamForce());
    }

    /** The team's loose infantry squad size, falling back to the round-wide value for older settings. */
    public int squadSize(String team)
    {
        int size = team(team).getSquadSize();
        return size > 0 ? size : squadSize;
    }

    /** @return what the round's bots are trained to: the values of its difficulty, or its custom ones */
    public BotTraining training()
    {
        return difficulty.training(customTraining);
    }

    /** @return the name a bot keeps of what it was trained to: the level, or for custom training, its values */
    public String trainingId()
    {
        return difficulty == Difficulty.CUSTOM ? customTraining.id() : difficulty.name();
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_ENABLED, enabled);
        tag.putBoolean(NBT_VEHICLES_ENABLED, vehiclesEnabled);
        tag.putString(NBT_MODE, mode.name());
        tag.putInt(NBT_TOTAL, total);
        tag.putString(NBT_NAMEPLATES, nameplates.name());
        tag.putString(NBT_SCOREBOARD, scoreboard.name());
        tag.putFloat(NBT_INFANTRY_WEIGHT, infantryCaptureWeight);
        tag.putFloat(NBT_VEHICLE_WEIGHT, vehicleCaptureWeight);
        tag.putInt(NBT_RESPAWN, respawnSeconds);
        tag.putInt(NBT_VEHICLE_RESPAWN, vehicleRespawnSeconds);
        tag.putInt(NBT_TICKETS, tickets);
        tag.putInt(NBT_SQUAD_SIZE, squadSize);
        tag.putInt(NBT_ABANDON, abandonChance);
        tag.putInt(NBT_RUBBER_BAND, rubberBand);
        tag.putString(NBT_DIFFICULTY, difficulty.name());
        tag.put(NBT_CUSTOM_DIFFICULTY, customTraining.save());
        tag.putBoolean(NBT_RADIO, radio);
        tag.putBoolean("commander_bot", commanderBot);
        tag.putInt(NBT_COMMANDER_DISRUPTION, commanderDisruptionSeconds);
        tag.putInt(NBT_RETREAT, retreatHealth);
        tag.putBoolean(NBT_SUPPRESSION, suppression);
        tag.putBoolean(NBT_RALLY, rallyPoints);
        tag.putBoolean(NBT_VEHICLE_CREWS, vehicleCrews);
        tag.putBoolean(NBT_EMPLACEMENTS, emplacements);
        tag.putBoolean(NBT_PLAYER_SEAT_DEPLOYMENT, playerSeatDeployment);
        tag.putBoolean(NBT_DRIVE_REAL_VEHICLES, driveRealVehicles);
        tag.putBoolean(NBT_GUN_REAL_VEHICLES, gunRealVehicles);
        tag.putBoolean(NBT_RIDE_REAL_VEHICLES, rideRealVehicles);
        tag.putString("tactical_profile", tacticalProfile.name());
        CompoundTag teamTag = new CompoundTag();
        teams.forEach((team, force) -> teamTag.put(team, force.save()));
        tag.put(NBT_TEAMS, teamTag);
        return tag;
    }

    /** Reads saved settings; a round without any has bots switched off, so the rotation plays as before. */
    public static BotSettings load(CompoundTag tag)
    {
        BotSettings settings = new BotSettings();
        if (tag.isEmpty())
        {
            settings.enabled = false;
            return settings;
        }
        settings.enabled = tag.getBoolean(NBT_ENABLED);
        settings.vehiclesEnabled = !tag.contains(NBT_VEHICLES_ENABLED) || tag.getBoolean(NBT_VEHICLES_ENABLED);
        settings.mode = parse(Mode.class, tag.getString(NBT_MODE), Mode.FIXED);
        settings.total = Math.max(0, tag.getInt(NBT_TOTAL));
        settings.nameplates = parse(Nameplates.class, tag.getString(NBT_NAMEPLATES), Nameplates.ALLIES);
        settings.scoreboard = parse(Scoreboard.class, tag.getString(NBT_SCOREBOARD), Scoreboard.AUTO);
        settings.infantryCaptureWeight = weight(tag, NBT_INFANTRY_WEIGHT);
        settings.vehicleCaptureWeight = weight(tag, NBT_VEHICLE_WEIGHT);
        settings.respawnSeconds = tag.contains(NBT_RESPAWN) ? Math.max(0, tag.getInt(NBT_RESPAWN)) : settings.respawnSeconds;
        settings.vehicleRespawnSeconds = tag.contains(NBT_VEHICLE_RESPAWN) ? Math.max(0, tag.getInt(NBT_VEHICLE_RESPAWN)) : settings.respawnSeconds;
        settings.tickets = Math.max(0, tag.getInt(NBT_TICKETS));
        settings.squadSize = tag.contains(NBT_SQUAD_SIZE) ? Math.max(1, tag.getInt(NBT_SQUAD_SIZE)) : settings.squadSize;
        settings.abandonChance = tag.contains(NBT_ABANDON) ? Math.max(0, Math.min(100, tag.getInt(NBT_ABANDON))) : settings.abandonChance;
        settings.rubberBand = Math.max(0, tag.getInt(NBT_RUBBER_BAND));
        settings.difficulty = parse(Difficulty.class, tag.getString(NBT_DIFFICULTY), Difficulty.REGULAR);
        settings.customTraining = BotTraining.load(tag.getCompound(NBT_CUSTOM_DIFFICULTY), DEFAULT_CUSTOM);
        settings.radio = !tag.contains(NBT_RADIO) || tag.getBoolean(NBT_RADIO);
        settings.commanderBot = tag.getBoolean("commander_bot");
        settings.commanderDisruptionSeconds = tag.contains(NBT_COMMANDER_DISRUPTION) ? Math.max(0, tag.getInt(NBT_COMMANDER_DISRUPTION)) : 20;
        settings.retreatHealth = tag.contains(NBT_RETREAT) ? Math.max(0, Math.min(100, tag.getInt(NBT_RETREAT))) : settings.retreatHealth;
        settings.suppression = !tag.contains(NBT_SUPPRESSION) || tag.getBoolean(NBT_SUPPRESSION);
        settings.rallyPoints = !tag.contains(NBT_RALLY) || tag.getBoolean(NBT_RALLY);
        settings.vehicleCrews = !tag.contains(NBT_VEHICLE_CREWS) || tag.getBoolean(NBT_VEHICLE_CREWS);
        settings.emplacements = !tag.contains(NBT_EMPLACEMENTS) || tag.getBoolean(NBT_EMPLACEMENTS);
        settings.playerSeatDeployment = !tag.contains(NBT_PLAYER_SEAT_DEPLOYMENT) || tag.getBoolean(NBT_PLAYER_SEAT_DEPLOYMENT);
        settings.driveRealVehicles = enabledByDefault(tag, NBT_DRIVE_REAL_VEHICLES);
        settings.gunRealVehicles = enabledByDefault(tag, NBT_GUN_REAL_VEHICLES);
        settings.rideRealVehicles = enabledByDefault(tag, NBT_RIDE_REAL_VEHICLES);
        settings.tacticalProfile = parse(NpcTacticalProfile.class, tag.getString("tactical_profile"), NpcTacticalProfile.INHERIT);
        CompoundTag teamTag = tag.getCompound(NBT_TEAMS);
        for (String team : teamTag.getAllKeys())
            settings.teams.put(team, TeamForce.load(teamTag.getCompound(team)));
        return settings;
    }

    private static boolean enabledByDefault(CompoundTag tag, String key)
    {
        return !tag.contains(key) || tag.getBoolean(key);
    }

    private static float weight(CompoundTag tag, String key)
    {
        if (!tag.contains(key))
            return 1F;
        float weight = tag.getFloat(key);
        return Float.isFinite(weight) ? Math.max(0F, weight) : 1F;
    }

    static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback)
    {
        for (E constant : type.getEnumConstants())
            if (constant.name().equalsIgnoreCase(name))
                return constant;
        return fallback;
    }
}
