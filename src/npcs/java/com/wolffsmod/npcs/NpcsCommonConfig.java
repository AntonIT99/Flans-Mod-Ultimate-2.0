package com.wolffsmod.npcs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The module's common configuration, {@code wolffsmodnpcs-common.toml}: the defaults every world starts from, which operators
 * can override per world and per faction with commands, and the performance settings that decide how a large battle gives
 * way under load.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcsCommonConfig
{
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue NPC_FRIENDLY_FIRE;
    private static final ForgeConfigSpec.BooleanValue PLAYER_FRIENDLY_FIRE;
    private static final ForgeConfigSpec.BooleanValue AI_TICK_LOD;
    private static final ForgeConfigSpec.IntValue AI_TICK_LOD_NEAR_DISTANCE;
    private static final ForgeConfigSpec.IntValue AI_TICK_LOD_FAR_DISTANCE;
    private static final ForgeConfigSpec.IntValue AI_TICK_LOD_MID_PERIOD;
    private static final ForgeConfigSpec.IntValue AI_TICK_LOD_FAR_PERIOD;
    private static final ForgeConfigSpec.BooleanValue ASYNC_PATHFINDING;
    private static final ForgeConfigSpec.DoubleValue BUDGET_TARGET_MILLIS_PER_TICK;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Friendly Fire");
        NPC_FRIENDLY_FIRE = builder.comment("Whether NPCs hurt NPCs of their own or an allied faction.",
            "Teams rounds follow their game type's friendlyfire variable instead. Override it per world or per faction with /flansnpcfriendlyfire.").define("npcFriendlyFire", false);
        PLAYER_FRIENDLY_FIRE = builder.comment("Whether NPCs and the players their faction is friendly to hurt each other.",
            "Teams rounds follow their game type's friendlyfire variable instead. Override it per world or per faction with /flansnpcfriendlyfire.").define("playerFriendlyFire", false);
        builder.pop();
        builder.push("Performance");
        AI_TICK_LOD = builder.comment("Whether NPCs far from every player decide less often than every tick.",
            "Their movement, physics, damage and shots keep running every tick; only goal and target decisions are spaced out, which is what a large battle spends its tick time on.",
            "NPCs driving, flying or manning a driveable always decide every tick.").define("aiTickLod", true);
        AI_TICK_LOD_NEAR_DISTANCE = builder.comment("Blocks from the nearest player within which an NPC always decides every tick.").defineInRange("aiTickLodNearDistance", 64, 8, 512);
        AI_TICK_LOD_FAR_DISTANCE = builder.comment("Blocks beyond which an NPC decides on the far period instead of the middle one.").defineInRange("aiTickLodFarDistance", 128, 16, 1024);
        AI_TICK_LOD_MID_PERIOD = builder.comment("Ticks between decisions between the near and the far distance.").defineInRange("aiTickLodMidPeriod", 2, 1, 10);
        AI_TICK_LOD_FAR_PERIOD = builder.comment("Ticks between decisions beyond the far distance.").defineInRange("aiTickLodFarPeriod", 4, 1, 20);
        ASYNC_PATHFINDING = builder.comment("Experimental: whether NPC path searches run on worker threads instead of the server thread.",
            "A search is handed to a worker and its path taken up a tick or two later, so a crowd asking for paths no longer lengthens the tick; the NPC keeps the path it has meanwhile.",
            "Searches read a snapshot of the blocks around them. Turn this off if NPC paths misbehave or the server logs path worker failures.").define("asyncPathfinding", true);
        BUDGET_TARGET_MILLIS_PER_TICK = builder.comment("Server tick time in milliseconds that the NPC target and path search limits aim to keep; 0 leaves the limits as set.",
            "Above the target the searches every NPC together may start per tick is cut back, below it the allowance grows again, so a battle loses search cadence rather than tick rate.",
            "Overrides the SearchBudget and PathBudget settings while it is not 0.").defineInRange("budgetTargetMillisPerTick", 0D, 0D, 50D);
        builder.pop();
        SPEC = builder.build();
    }

    /** @return whether NPCs hurt NPCs of their own or an allied faction, unless a world or faction says otherwise */
    public static boolean npcFriendlyFire()
    {
        return read(NPC_FRIENDLY_FIRE);
    }

    /** @return whether NPCs and the players their faction is friendly to hurt each other, unless a world or faction says otherwise */
    public static boolean playerFriendlyFire()
    {
        return read(PLAYER_FRIENDLY_FIRE);
    }

    /** @return whether NPCs far from every player decide less often than every tick */
    public static boolean aiTickLod()
    {
        return read(AI_TICK_LOD);
    }

    /** @return blocks from the nearest player within which an NPC always decides every tick */
    public static int aiTickLodNearDistance()
    {
        return read(AI_TICK_LOD_NEAR_DISTANCE);
    }

    /** @return blocks beyond which an NPC decides on the far period */
    public static int aiTickLodFarDistance()
    {
        return read(AI_TICK_LOD_FAR_DISTANCE);
    }

    /** @return ticks between decisions between the near and the far distance */
    public static int aiTickLodMidPeriod()
    {
        return read(AI_TICK_LOD_MID_PERIOD);
    }

    /** @return ticks between decisions beyond the far distance */
    public static int aiTickLodFarPeriod()
    {
        return read(AI_TICK_LOD_FAR_PERIOD);
    }

    /** @return whether NPC path searches run on worker threads */
    public static boolean asyncPathfinding()
    {
        return read(ASYNC_PATHFINDING);
    }

    /** @return the server tick time in milliseconds the search limits aim to keep, or 0 to leave the limits as set */
    public static double budgetTargetMillisPerTick()
    {
        return read(BUDGET_TARGET_MILLIS_PER_TICK);
    }

    /** Reads a value, or its default before the configuration is loaded. */
    private static boolean read(ForgeConfigSpec.BooleanValue value)
    {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private static int read(ForgeConfigSpec.IntValue value)
    {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    // One of the three overloads that read a value before the configuration is loaded; only one setting needs this one.
    @SuppressWarnings("SameParameterValue")
    private static double read(ForgeConfigSpec.DoubleValue value)
    {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
