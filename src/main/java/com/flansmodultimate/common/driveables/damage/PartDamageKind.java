package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * What a part is made of and does, as far as the look of its damage goes.
 *
 * <p>
 * A part's kind decides what it gives off as it is worn down and once it burns: an engine bay smokes black and
 * oily, a boiler room vents white steam, armour plate only throws sparks, a wing leaks a vapour trail and a mecha
 * limb sparks and sprays hydraulic fluid. Every part has a default from its name; a content pack can give a part
 * another kind with {@code PartDamageEffect <part> <kind>}.
 * </p>
 */
public enum PartDamageKind
{
    /** The hull or fuselage: grey smoke turning black, then fire. */
    STRUCTURE,
    /** An engine or engine room: dark oily smoke early, and the hottest fire. */
    ENGINE,
    /** A gun turret or mount: smoke and fire, and the seat of a tank's ammunition cook-off. */
    TURRET,
    /** Armour plate, skirts and bulges: sparks where it is hit, never smoke. */
    ARMOR,
    /** Wheels, tracks, skids and floats: sparks and dust while moving, a burning tyre at worst. */
    RUNNING_GEAR,
    /** Wings, tail and nose: a fuel-vapour trail behind a moving aircraft, then smoke. */
    AIRFOIL,
    /** Rotor blades: sparks and light smoke. */
    ROTOR,
    /** A ship's boiler room: white steam rather than smoke. */
    BOILER,
    /** A ship's hull compartments: bubbles and oil once holed near the water. */
    HULL,
    /** Decks, bridges and superstructure: smoke and fire. */
    SUPERSTRUCTURE,
    /** Radar, directors and protection systems: electrical sparks. */
    ELECTRONICS,
    /** A mecha's arms, legs, hips and head: electrical sparks and a hydraulic mist. */
    LIMB,
    /** An airship's gas cell: a pale leak, and a large fire once alight. */
    GASBAG,
    /** Shows nothing of its own, such as the crew compartment. */
    NONE;

    /** @return the kind a part has when its type file names none */
    public static PartDamageKind defaultFor(@Nullable EnumDriveablePart part)
    {
        if (part == null)
            return STRUCTURE;
        if (EnumDriveablePart.isWheel(part))
            return RUNNING_GEAR;
        if (EnumDriveablePart.getEngineRooms().contains(part))
            return ENGINE;
        if (EnumDriveablePart.getBoilerRooms().contains(part))
            return BOILER;
        String name = part.name();
        if (name.startsWith("TURRET_ARMOR") || name.equals("MORE_TURRET_ARMOR") || name.equals("TURRET_SKIRT") || name.equals("TURRET_SIDE"))
            return ARMOR;
        if (EnumDriveablePart.isTurretMounted(part))
            return TURRET;
        return switch (part)
        {
            case LEFT_TRACK, RIGHT_TRACK, SKIDS, FLOATS, FLOATS_LEFT, FLOATS_RIGHT, TRAILER -> RUNNING_GEAR;
            case LEFT_WING, RIGHT_WING, TOP_WING, TAIL, NOSE -> AIRFOIL;
            case BLADES -> ROTOR;
            case LEFT_ARM, RIGHT_ARM, HEAD, HIPS -> LIMB;
            case FRONTAL_ARMOR, LEFTSIDE_ARMOR, RIGHTSIDE_ARMOR, ADDITIONAL_ARMOR, ERA, ERA_2, ERA_3, COMPOSITE, COMPOSITE_2, COMPOSITE_LEFT, COMPOSITE_RIGHT, SPACED, BIAS, RIGHT_SKIRT, LEFT_SKIRT,
                SHIELD, TURRET_WEAK, TURRET_WEAK_2, BELT, BELT_LEFT, BELT_RIGHT, CITADEL, BULGE, BULGE_LEFT, BULGE_RIGHT, TORPEDO_BULGE, TORPEDO_BULGE_2, TORPEDO_BULGE_3, TORPEDO_BULGE_4, BULKHEAD,
                BULKHEAD_2 -> ARMOR;
            case APS, ADS, RADAR_1, RADAR_2, RADAR_3, RADAR_4, DIRECTOR_1, DIRECTOR_2, DIRECTOR_3, DIRECTOR_4, DIRECTOR_5, DIRECTOR_6, DIRECTOR_7, DIRECTOR_8 -> ELECTRONICS;
            case BOW, STERN, PORT, STARBOARD, MIDSECTION, LEFT, RIGHT, BUOYANCY -> HULL;
            case CONNING_TOWER, CONNING_TOWER_AFT, BRIDGE, SUPERSTRUCTURE, HANGAR, HANGAR_DECK, HANGAR_DECK_2, HANGAR_DECK_3, FLIGHT_DECK, FLIGHT_DECK_2, DECK, DECK_2, DECK_3, STEERING ->
                SUPERSTRUCTURE;
            case GASBAG -> GASBAG;
            case INFANTRY -> NONE;
            default -> STRUCTURE;
        };
    }

    /** @return the kind named in a type file, case-insensitively */
    public static Optional<PartDamageKind> parse(@Nullable String name)
    {
        if (name == null || name.isBlank())
            return Optional.empty();
        try
        {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException ignored)
        {
            return Optional.empty();
        }
    }

    /** @return whether the part burns once it is badly damaged, rather than only smoking or sparking */
    public boolean burns()
    {
        return this == STRUCTURE || this == ENGINE || this == TURRET || this == SUPERSTRUCTURE || this == GASBAG || this == AIRFOIL;
    }

    /** @return whether the part gives off smoke at all */
    public boolean smokes()
    {
        return this != ARMOR && this != ELECTRONICS && this != NONE && this != BOILER && this != RUNNING_GEAR;
    }
}
