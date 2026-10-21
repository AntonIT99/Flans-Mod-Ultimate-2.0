package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.crew.CrewPost;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    private static final String NBT_MODE = "mode";
    private static final String NBT_TOTAL = "total";
    private static final String NBT_NAMEPLATES = "nameplates";
    private static final String NBT_SCOREBOARD = "scoreboard";
    private static final String NBT_INFANTRY_WEIGHT = "infantry_capture_weight";
    private static final String NBT_VEHICLE_WEIGHT = "vehicle_capture_weight";
    private static final String NBT_RESPAWN = "respawn_seconds";
    private static final String NBT_TICKETS = "tickets";
    private static final String NBT_SQUAD_SIZE = "squad_size";
    private static final String NBT_ABANDON = "abandon_chance";
    private static final String NBT_RUBBER_BAND = "rubber_band";
    private static final String NBT_DIFFICULTY = "difficulty";
    private static final String NBT_PLAYER_TICKETS = "player_tickets";
    private static final String NBT_RADIO = "radio";
    private static final String NBT_RETREAT = "retreat_health";
    private static final String NBT_SUPPRESSION = "suppression";
    private static final String NBT_RALLY = "rally_points";
    private static final String NBT_VEHICLE_CREWS = "vehicle_crews";
    private static final String NBT_TEAMS = "teams";

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
        ANTI_TANK
    }

    /**
     * How well bots fight, by scaling what their loadout or clone gives them: aim, how far they notice enemies, health,
     * and the pause between their shots.
     */
    public enum Difficulty
    {
        /** Bots fight as their loadout or clone makes them. */
        DEFAULT(1F, 1F, 1F, 1F), RECRUIT(0.6F, 0.75F, 0.8F, 1.5F), REGULAR(0.85F, 0.9F, 1F, 1.2F), VETERAN(1.15F, 1.15F, 1.25F, 0.85F), ELITE(1.35F, 1.3F, 1.5F, 0.7F);

        private final float accuracy;
        private final float aggro;
        private final float health;
        private final float delay;

        Difficulty(float accuracy, float aggro, float health, float delay)
        {
            this.accuracy = accuracy;
            this.aggro = aggro;
            this.health = health;
            this.delay = delay;
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

        /** Factor on the most health bots have. */
        public float health()
        {
            return health;
        }

        /** Factor on the ticks between shots; below 1 is faster. */
        public float delay()
        {
            return delay;
        }
    }

    private boolean enabled = true;
    private Mode mode = Mode.FIXED;
    /** Bots shared out between the teams in {@link Mode#SHARE}. */
    private int total = 32;
    private Nameplates nameplates = Nameplates.ALLIES;
    private Scoreboard scoreboard = Scoreboard.AUTO;
    private float infantryCaptureWeight = 1F;
    private float vehicleCaptureWeight = 1F;
    /** Seconds a fallen unit waits before it can come back. */
    private int respawnSeconds = 10;
    /** Respawns each side may make, a vehicle counting its ticket cost; 0 for no limit. */
    private int tickets;
    /** How many loose infantry bots form one squad. */
    private int squadSize = 6;
    /** Chance, in percent, that a destroyed vehicle bot leaves a damaged vehicle players can repair and use. */
    private int abandonChance = 25;
    /** Most extra infantry, in percent of its force, a side gets while it trails on score; 0 switches it off. */
    private int rubberBand;
    private Difficulty difficulty = Difficulty.DEFAULT;
    /** Whether players, too, spend their side's tickets when they respawn, once the side has a ticket limit. */
    private boolean playerTickets = true;
    /** Whether bots call out on the radio what happens at the objectives. */
    private boolean radio = true;
    /** Share of its health, in percent, below which a soldier falls back to a medic or its spawn; 0 never. */
    private int retreatHealth = 30;
    /** Whether rounds passing close by make bots aim worse and stop advancing. */
    private boolean suppression = true;
    /** Whether players leading a squad may place a rally point for themselves and their squad to spawn at. */
    private boolean rallyPoints = true;
    /** Whether vehicle bots spawn with soldiers of their team in their seats. */
    private boolean vehicleCrews;
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
        private static final String NBT_SQUADS = "squads";
        private static final String NBT_VEHICLES = "vehicles";
        private static final String NBT_CREW = "crew";
        /** The crew class that leaves its seats empty. */
        public static final String NO_CREW = "none";

        /** Loose infantry: a count, a share weight or a fill target, depending on the mode. */
        private int count;
        /** Loose infantry spawns the team may make in all; 0 for no limit. */
        private int budget;
        /** The soldier name list bots are named from, such as {@code german_20th}; empty keeps the template's names. */
        private String nameList = "";
        /** The Custom NPCs clone bots are made from; tab 0 or an empty name makes plain NPCs. */
        private int cloneTab;
        private String cloneName = "";
        /** Whether bots made from the clone keep its weapons and armour rather than getting their class's loadout. */
        private boolean keepCloneEquipment;
        /** Weight of each class bots are given; a class left out weighs 1 when no class is listed, 0 otherwise. */
        private final Map<String, Integer> classWeights = new LinkedHashMap<>();
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

        CompoundTag save()
        {
            CompoundTag tag = new CompoundTag();
            tag.putInt(NBT_COUNT, count);
            tag.putInt(NBT_BUDGET, budget);
            tag.putString(NBT_NAME_LIST, nameList);
            tag.putInt(NBT_CLONE_TAB, cloneTab);
            tag.putString(NBT_CLONE_NAME, cloneName);
            tag.putBoolean(NBT_KEEP_EQUIPMENT, keepCloneEquipment);
            CompoundTag classes = new CompoundTag();
            classWeights.forEach(classes::putInt);
            tag.put(NBT_CLASSES, classes);
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
            return tag;
        }

        static TeamForce load(CompoundTag tag)
        {
            TeamForce force = new TeamForce();
            force.count = Math.max(0, tag.getInt(NBT_COUNT));
            force.budget = Math.max(0, tag.getInt(NBT_BUDGET));
            force.nameList = tag.getString(NBT_NAME_LIST);
            force.cloneTab = tag.getInt(NBT_CLONE_TAB);
            force.cloneName = tag.getString(NBT_CLONE_NAME);
            force.keepCloneEquipment = tag.getBoolean(NBT_KEEP_EQUIPMENT);
            CompoundTag classes = tag.getCompound(NBT_CLASSES);
            for (String name : classes.getAllKeys())
                force.classWeights.put(name, Math.max(0, classes.getInt(name)));
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

        /** Whether bots are made from a Custom NPCs clone rather than as plain NPCs. */
        public boolean usesClone()
        {
            return cloneTab > 0 && !cloneName.isBlank();
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
     *            seconds a destroyed one takes to come back; 0 takes the round's respawn time
     * @param ticketCost
     *            tickets one costs to bring back
     * @param spawner
     *            the vehicle spawner it appears at, or null for any the team holds
     * @param carry
     *            the squad template it carries to the front and unloads there, or drops by parachute from the air; empty
     *            for none
     */
    public record VehicleEntry(String type, int count, int budget, int respawnSeconds, int ticketCost, @Nullable UUID spawner, String carry)
    {
        private static final String NBT_TYPE = "type";
        private static final String NBT_COUNT = "count";
        private static final String NBT_BUDGET = "budget";
        private static final String NBT_RESPAWN = "respawn_seconds";
        private static final String NBT_TICKET_COST = "ticket_cost";
        private static final String NBT_SPAWNER = "spawner";
        private static final String NBT_CARRY = "carry";

        public VehicleEntry
        {
            count = Math.max(0, count);
            budget = Math.max(0, budget);
            respawnSeconds = Math.max(0, respawnSeconds);
            ticketCost = Math.max(0, ticketCost);
            carry = carry == null ? "" : carry;
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
            return tag;
        }

        static VehicleEntry load(CompoundTag tag)
        {
            return new VehicleEntry(tag.getString(NBT_TYPE), tag.getInt(NBT_COUNT), tag.getInt(NBT_BUDGET), tag.getInt(NBT_RESPAWN), tag.getInt(NBT_TICKET_COST),
                tag.hasUUID(NBT_SPAWNER) ? tag.getUUID(NBT_SPAWNER) : null, tag.getString(NBT_CARRY));
        }
    }

    /** The force of a team, made empty on first use. */
    public TeamForce team(String team)
    {
        return teams.computeIfAbsent(team, ignored -> new TeamForce());
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_ENABLED, enabled);
        tag.putString(NBT_MODE, mode.name());
        tag.putInt(NBT_TOTAL, total);
        tag.putString(NBT_NAMEPLATES, nameplates.name());
        tag.putString(NBT_SCOREBOARD, scoreboard.name());
        tag.putFloat(NBT_INFANTRY_WEIGHT, infantryCaptureWeight);
        tag.putFloat(NBT_VEHICLE_WEIGHT, vehicleCaptureWeight);
        tag.putInt(NBT_RESPAWN, respawnSeconds);
        tag.putInt(NBT_TICKETS, tickets);
        tag.putInt(NBT_SQUAD_SIZE, squadSize);
        tag.putInt(NBT_ABANDON, abandonChance);
        tag.putInt(NBT_RUBBER_BAND, rubberBand);
        tag.putString(NBT_DIFFICULTY, difficulty.name());
        tag.putBoolean(NBT_PLAYER_TICKETS, playerTickets);
        tag.putBoolean(NBT_RADIO, radio);
        tag.putInt(NBT_RETREAT, retreatHealth);
        tag.putBoolean(NBT_SUPPRESSION, suppression);
        tag.putBoolean(NBT_RALLY, rallyPoints);
        tag.putBoolean(NBT_VEHICLE_CREWS, vehicleCrews);
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
        settings.mode = parse(Mode.class, tag.getString(NBT_MODE), Mode.FIXED);
        settings.total = Math.max(0, tag.getInt(NBT_TOTAL));
        settings.nameplates = parse(Nameplates.class, tag.getString(NBT_NAMEPLATES), Nameplates.ALLIES);
        settings.scoreboard = parse(Scoreboard.class, tag.getString(NBT_SCOREBOARD), Scoreboard.AUTO);
        settings.infantryCaptureWeight = weight(tag, NBT_INFANTRY_WEIGHT);
        settings.vehicleCaptureWeight = weight(tag, NBT_VEHICLE_WEIGHT);
        settings.respawnSeconds = tag.contains(NBT_RESPAWN) ? Math.max(0, tag.getInt(NBT_RESPAWN)) : settings.respawnSeconds;
        settings.tickets = Math.max(0, tag.getInt(NBT_TICKETS));
        settings.squadSize = tag.contains(NBT_SQUAD_SIZE) ? Math.max(1, tag.getInt(NBT_SQUAD_SIZE)) : settings.squadSize;
        settings.abandonChance = tag.contains(NBT_ABANDON) ? Math.max(0, Math.min(100, tag.getInt(NBT_ABANDON))) : settings.abandonChance;
        settings.rubberBand = Math.max(0, tag.getInt(NBT_RUBBER_BAND));
        settings.difficulty = parse(Difficulty.class, tag.getString(NBT_DIFFICULTY), Difficulty.DEFAULT);
        settings.playerTickets = !tag.contains(NBT_PLAYER_TICKETS) || tag.getBoolean(NBT_PLAYER_TICKETS);
        settings.radio = !tag.contains(NBT_RADIO) || tag.getBoolean(NBT_RADIO);
        settings.retreatHealth = tag.contains(NBT_RETREAT) ? Math.max(0, Math.min(100, tag.getInt(NBT_RETREAT))) : settings.retreatHealth;
        settings.suppression = !tag.contains(NBT_SUPPRESSION) || tag.getBoolean(NBT_SUPPRESSION);
        settings.rallyPoints = !tag.contains(NBT_RALLY) || tag.getBoolean(NBT_RALLY);
        settings.vehicleCrews = tag.getBoolean(NBT_VEHICLE_CREWS);
        CompoundTag teamTag = tag.getCompound(NBT_TEAMS);
        for (String team : teamTag.getAllKeys())
            settings.teams.put(team, TeamForce.load(teamTag.getCompound(team)));
        return settings;
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
