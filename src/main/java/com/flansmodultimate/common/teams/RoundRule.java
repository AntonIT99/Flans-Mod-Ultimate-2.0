package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.IssueDates;
import lombok.Getter;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** The configurable round rules, shared by validation, commands and the command centre. */
@Getter
public enum RoundRule
{
    DESTROYABLE_DEPLOYED_GUNS("destroyableDeployedGuns"), DISABLE_CLASS_LIMITS("disableClassLimits"), RESPAWN_WAVE_SECONDS("respawnWaveSeconds", 0, 0, 300), PLAYER_LIVES("playerLives", 0, 0,
        100), OVERTIME_SECONDS("overtimeSeconds", 0, 0, 600), TANK_LIMIT("tankLimit", -1, -1, 100), VEHICLE_LIMIT("vehicleLimit", -1, -1, 100), AIRCRAFT_LIMIT("aircraftLimit", -1, -1,
            100), SHIP_LIMIT("shipLimit", -1, -1, 100), MECHA_LIMIT("mechaLimit", -1, -1, 100), AA_LIMIT("aaLimit", -1, -1, 100), VEHICLE_REPLACEMENT_SECONDS("vehicleReplacementSeconds", 0, 0,
                600), CAPTURE_ON_FOOT_ONLY("captureOnFootOnly"), MAX_CAPTURE_WEIGHT("maxCaptureWeight", 0, 0, 100), HOME_BASE_RADIUS("homeBaseRadius", 16, 1,
                    128), HOME_BASE_NO_ENTRY("homeBaseNoEntry"), HOME_BASE_NO_DAMAGE("homeBaseNoDamage"), WARMUP_SECONDS("warmupSeconds", 0, 0, 300), MINIMUM_PLAYERS("minimumPlayers", 0, 0,
                        100), REQUIRE_READY("requireReady"), TEAM_SWITCH_SECONDS("teamSwitchSeconds", 0, 0, 3600), PREVENT_LEADING_SIDE_JOIN("preventLeadingSideJoin"), FLAG_NO_VEHICLES(
                            "flagNoVehicles"), FLAG_NO_FLIGHT("flagNoFlight"), FLAG_BANNED_ITEMS("flagBannedItems", Kind.ITEMS, ""), SHOW_ENEMY_NAMEPLATES(
                                "showEnemyNameplates"), HIDE_ALL_NAMEPLATES("hideAllNameplates"), HIDE_KILL_MESSAGES("hideKillMessages"), RESTRICT_SPECTATORS("restrictSpectators"), ERA_WINDOW("eraWindow", Kind.ERA, "");

    public enum Kind
    {
        BOOLEAN, INTEGER, ITEMS,
        /** An {@link EraRestriction} in its one-line form, or empty for none. */
        ERA
    }

    private final String key;
    private final Kind kind;
    private final String defaultValue;
    private final int minimum;
    private final int maximum;

    RoundRule(String key)
    {
        this(key, Kind.BOOLEAN, "false");
    }

    RoundRule(String key, Kind kind, String defaultValue)
    {
        this.key = key;
        this.kind = kind;
        this.defaultValue = defaultValue;
        minimum = 0;
        maximum = 1;
    }

    RoundRule(String key, int defaultValue, int minimum, int maximum)
    {
        this.key = key;
        kind = Kind.INTEGER;
        this.defaultValue = Integer.toString(defaultValue);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /** Rejects malformed input instead of quietly enabling a different rule or clipping a value. */
    public String validate(String value)
    {
        String clean = value.strip();
        if (kind == Kind.BOOLEAN)
        {
            if (!"true".equalsIgnoreCase(clean) && !"false".equalsIgnoreCase(clean))
                throw new IllegalArgumentException("Expected true or false");
            return clean.toLowerCase(Locale.ROOT);
        }
        if (kind == Kind.ITEMS)
        {
            if (clean.length() > 1024)
                throw new IllegalArgumentException("Item list is too long (maximum 1024 characters)");
            if (clean.isEmpty())
                return clean;
            return Arrays.stream(clean.split(",", -1)).map(String::strip).map(item ->
            {
                ResourceLocation id = ResourceLocation.tryParse(item);
                if (item.isEmpty() || id == null)
                    throw new IllegalArgumentException("Expected comma-separated item registry ids");
                return id.toString();
            }).distinct().sorted().collect(Collectors.joining(","));
        }
        if (kind == Kind.ERA)
            return validateEra(clean);
        int number = Integer.parseInt(clean);
        if (number < minimum || number > maximum)
            throw new IllegalArgumentException("Expected a number from " + minimum + " to " + maximum);
        return Integer.toString(number);
    }

    /** A round restriction covers its participants only, so it may not despawn entities as a world restriction may. */
    private static String validateEra(String clean)
    {
        if (clean.isEmpty())
            return clean;
        EraRestriction restriction = EraRestriction.parse(clean);
        restriction.validate(IssueDates.eras()).ifPresent(problem ->
        {
            throw new IllegalArgumentException(problem);
        });
        if (restriction.policies().despawn())
            throw new IllegalArgumentException("despawn=on is only for server and dimension restrictions");
        return restriction.toString();
    }

    public static Optional<RoundRule> find(String key)
    {
        return Arrays.stream(values()).filter(rule -> rule.key.equalsIgnoreCase(key)).findFirst();
    }
}
