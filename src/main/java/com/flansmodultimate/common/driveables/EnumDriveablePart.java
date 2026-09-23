package com.flansmodultimate.common.driveables;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter
public enum EnumDriveablePart
{
    //Plane parts
    TAIL_WHEEL(new EnumDriveablePart[] { }, "tailWheel", "Wheel (Tail)"),
    TAIL(new EnumDriveablePart[] {TAIL_WHEEL}, "tail", "Tail"),
    BAY(new EnumDriveablePart[] {TAIL}, "bay", "Bay"),
    TOP_WING(new EnumDriveablePart[] {}, "topWing", "Wing (Top)"),
    LEFT_WING_WHEEL(new EnumDriveablePart[] { }, "leftWingWheel", "Wheel (Left Wing)"),
    LEFT_WING(new EnumDriveablePart[] {TOP_WING, LEFT_WING_WHEEL}, "leftWing", "Wing (Left)"),
    RIGHT_WING_WHEEL(new EnumDriveablePart[] { }, "rightWingWheel", "Wheel (Right Wing)"),
    RIGHT_WING(new EnumDriveablePart[] {TOP_WING, RIGHT_WING_WHEEL}, "rightWing", "Wing (Right)"),
    NOSE(new EnumDriveablePart[] { }, "nose", "Nose"),
    CORE_WHEEL(new EnumDriveablePart[] { }, "coreWheel", "Wheel (Core)"),
    AIRFRAME(new EnumDriveablePart[] { },"airframe", "Airframe"),
    FLOATS_RIGHT(new EnumDriveablePart[] { }, "floatsRight", "Floats (Right)"),
    FLOATS_LEFT(new EnumDriveablePart[] { }, "floatsLeft", "Floats (Left)"),
    FLOATS(new EnumDriveablePart[] { }, "floats", "Floats"),

    //Helicopter parts
    SKIDS(new EnumDriveablePart[] { }, "skids", "Skids"),
    BLADES(new EnumDriveablePart[] { }, "blades", "Blades"),

    //Vehicle parts
    TURRET(new EnumDriveablePart[] { }, "turret", "Turret"),
    BACK_WHEEL(new EnumDriveablePart[] { }, "backWheel", "Wheel (Back)"),
    FRONT_WHEEL(new EnumDriveablePart[] { }, "frontWheel", "Wheel (Front)"),
    BACK_LEFT_WHEEL(new EnumDriveablePart[] { }, "backLeftWheel", "Wheel (Back Left)"),
    FRONT_LEFT_WHEEL(new EnumDriveablePart[] { }, "frontLeftWheel", "Wheel (Front Left)"),
    BACK_RIGHT_WHEEL(new EnumDriveablePart[] { }, "backRightWheel", "Wheel (Back Right)"),
    FRONT_RIGHT_WHEEL(new EnumDriveablePart[] { }, "frontRightWheel", "Wheel (Front Right)"),
    LEFT_TRACK(new EnumDriveablePart[] { }, "leftTrack", "Track (Left)"),
    RIGHT_TRACK(new EnumDriveablePart[] { }, "rightTrack", "Track (Right)"),
    TRAILER(new EnumDriveablePart[] { }, "trailer", "Trailer"),
    HARVESTER(new EnumDriveablePart[] { }, "harvester", "Harvester"),	//This is the drill bit, combine blades or excavator for utility vehicles
    //New parts
    FRONTAL_ARMOR(new EnumDriveablePart[] { }, "frontalArmor", "Armor (Frontal)"),
    LEFTSIDE_ARMOR(new EnumDriveablePart[] { }, "leftsideArmor", "Armor (Left Side)"),
    RIGHTSIDE_ARMOR(new EnumDriveablePart[] { }, "rightsideArmor", "Armor (Right Side)"),
    ADDITIONAL_ARMOR(new EnumDriveablePart[] { }, "additionalArmor", "Armor (Additional)"),
    ERA(new EnumDriveablePart[] { }, "ERA", "Armour (ERA)"),
    APS(new EnumDriveablePart[] { }, "APS", "Armour (APS)"),
    ADS(new EnumDriveablePart[] { }, "ADS", "Armour (ADS)"),
    COMPOSITE(new EnumDriveablePart[] { }, "composite", "Armor (Composite)"),
    COMPOSITE_2(new EnumDriveablePart[] { }, "composite2", "Armor (More Composite)"),
    SPACED(new EnumDriveablePart[] { }, "spaced", "Armor (Spaced)"),
    COMPOSITE_LEFT(new EnumDriveablePart[] { }, "compositeL", "Armor (Left Composite)"),
    COMPOSITE_RIGHT(new EnumDriveablePart[] { }, "compositeR", "Armor (Right Composite)"),
    GENERIC_0(new EnumDriveablePart[] { }, "generic0", "Armor (Extra)"),
    GENERIC_1(new EnumDriveablePart[] { }, "generic1", "Armor (Extra 1)"),
    GENERIC_2(new EnumDriveablePart[] { }, "generic2", "Armor (Extra 2)"),
    GENERIC_3(new EnumDriveablePart[] { }, "generic3", "Armor (Extra 3)"),
    GENERIC_4(new EnumDriveablePart[] { }, "generic4", "Armor (Extra 4)"),
    GENERIC_5(new EnumDriveablePart[] { }, "generic5", "Armor (Extra 5)"),
    GENERIC_6(new EnumDriveablePart[] { }, "generic6", "Armor (Extra 6)"),
    GENERIC_7(new EnumDriveablePart[] { }, "generic7", "Armor (Extra 7)"),
    GENERIC_8(new EnumDriveablePart[] { }, "generic8", "Armor (Extra 8)"),
    GENERIC_9(new EnumDriveablePart[] { }, "generic9", "Armor (Extra 9)"),
    BIAS(new EnumDriveablePart[] { }, "russianBias", "Stalinium"),
    ERA_2(new EnumDriveablePart[] { }, "ERA2", "More ERA"),
    ERA_3(new EnumDriveablePart[] { }, "ERA3", "Even More ERA"),
    INFANTRY(new EnumDriveablePart[] { }, "infantry", "Meat Shield"),
    RIGHT_SKIRT(new EnumDriveablePart[] { }, "rightSkirt", "Skirt (Right)"),
    LEFT_SKIRT(new EnumDriveablePart[] { }, "leftSkirt", "Skirt (Left)"),
    SHIELD(new EnumDriveablePart[] { }, "shield", "Energy Shield"),

    // Additional turret-mounted armor and weak spots from the Krishna Mk6C fork.
    TURRET_ARMOR(new EnumDriveablePart[] { }, "turretarmor", "Armor (Turret)"),
    MORE_TURRET_ARMOR(new EnumDriveablePart[] { }, "moreturretarmor", "Armor (Turret Composite)"),
    TURRET_SIDE(new EnumDriveablePart[] { }, "turretside", "Armor (Turret Side)"),
    TURRET_SKIRT(new EnumDriveablePart[] { }, "turretSkirt", "Skirt (Turret)"),
    TURRET_WEAK(new EnumDriveablePart[] { }, "weakTrt", "Turret Weak Spot"),
    TURRET_WEAK_2(new EnumDriveablePart[] { }, "weakTrt2", "Turret Weak Spot 2"),
    WEAK_SPOT(new EnumDriveablePart[] { }, "weakSpot", "Weak Spot"),
    WEAK_SPOT_2(new EnumDriveablePart[] { }, "weakSpot2", "Weak Spot 2"),
    WEAK_SPOT_3(new EnumDriveablePart[] { }, "weakSpot3", "Weak Spot 3"),

    BOW(new EnumDriveablePart[] { }, "bow", "Bow"),
    STERN(new EnumDriveablePart[] { }, "stern", "Stern"),
    CONNING_TOWER(new EnumDriveablePart[] { }, "conningTower", "Conning Tower"),
    CONNING_TOWER_AFT(new EnumDriveablePart[] { }, "aftTower", "Conning Tower (Aft)"),
    BRIDGE(new EnumDriveablePart[] { }, "bridge", "Bridge"),
    RADAR_1(new EnumDriveablePart[] { }, "radar1", "Radar 1"),
    RADAR_2(new EnumDriveablePart[] { }, "radar2", "Radar 2"),
    RADAR_3(new EnumDriveablePart[] { }, "radar3", "Radar 3"),
    RADAR_4(new EnumDriveablePart[] { }, "radar4", "Radar 4"),
    DIRECTOR_1(new EnumDriveablePart[] { }, "director1", "Director 1"),
    DIRECTOR_2(new EnumDriveablePart[] { }, "director2", "Director 2"),
    DIRECTOR_3(new EnumDriveablePart[] { }, "director3", "Director 3"),
    DIRECTOR_4(new EnumDriveablePart[] { }, "director4", "Director 4"),
    DIRECTOR_5(new EnumDriveablePart[] { }, "director5", "Director 5"),
    DIRECTOR_6(new EnumDriveablePart[] { }, "director6", "Director 6"),
    DIRECTOR_7(new EnumDriveablePart[] { }, "director7", "Director 7"),
    DIRECTOR_8(new EnumDriveablePart[] { }, "director8", "Director 8"),
    SUPERSTRUCTURE(new EnumDriveablePart[] { }, "superstructure", "Superstructure"),
    HANGAR(new EnumDriveablePart[] { }, "hangar", "Hangar"),
    HANGAR_DECK(new EnumDriveablePart[] { }, "hangarDeck", "Hangar Deck"),
    HANGAR_DECK_2(new EnumDriveablePart[] { }, "hangarDeck2", "Hangar Deck 2"),
    HANGAR_DECK_3(new EnumDriveablePart[] { }, "hangarDeck3", "Hangar Deck 3"),
    FLIGHT_DECK(new EnumDriveablePart[] { }, "flightDeck", "Flight Deck"),
    FLIGHT_DECK_2(new EnumDriveablePart[] { }, "flightDeck2", "Flight Deck 2"),
    ENGINE_ROOM_1(new EnumDriveablePart[] { }, "engineRoom1", "Engine Room 1"),
    ENGINE_ROOM_2(new EnumDriveablePart[] { }, "engineRoom2", "Engine Room 2"),
    ENGINE_ROOM_3(new EnumDriveablePart[] { }, "engineRoom3", "Engine Room 3"),
    ENGINE_ROOM_4(new EnumDriveablePart[] { }, "engineRoom4", "Engine Room 4"),
    ENGINE_ROOM_5(new EnumDriveablePart[] { }, "engineRoom5", "Engine Room 5"),
    ENGINE_ROOM_6(new EnumDriveablePart[] { }, "engineRoom6", "Engine Room 6"),
    ENGINE_ROOM_7(new EnumDriveablePart[] { }, "engineRoom7", "Engine Room 7"),
    ENGINE_ROOM_8(new EnumDriveablePart[] { }, "engineRoom8", "Engine Room 8"),
    BOILER_ROOM_1(new EnumDriveablePart[] { }, "boilerRoom1", "Boiler Room 1"),
    BOILER_ROOM_2(new EnumDriveablePart[] { }, "boilerRoom2", "Boiler Room 2"),
    BOILER_ROOM_3(new EnumDriveablePart[] { }, "boilerRoom3", "Boiler Room 3"),
    BOILER_ROOM_4(new EnumDriveablePart[] { }, "boilerRoom4", "Boiler Room 4"),
    BOILER_ROOM_5(new EnumDriveablePart[] { }, "boilerRoom5", "Boiler Room 5"),
    BOILER_ROOM_6(new EnumDriveablePart[] { }, "boilerRoom6", "Boiler Room 6"),
    BOILER_ROOM_7(new EnumDriveablePart[] { }, "boilerRoom7", "Boiler Room 7"),
    BOILER_ROOM_8(new EnumDriveablePart[] { }, "boilerRoom8", "Boiler Room 8"),
    STEERING(new EnumDriveablePart[] { }, "steering", "Steering Room"),
    DECK(new EnumDriveablePart[] { }, "deck", "First Deck"),
    DECK_2(new EnumDriveablePart[] { }, "deck2", "Second Deck"),
    DECK_3(new EnumDriveablePart[] { }, "deck3", "Third Deck"),
    CITADEL(new EnumDriveablePart[] { }, "citadel", "Citadel"),
    BELT(new EnumDriveablePart[] { }, "belt", "Armor Belt"),
    TORPEDO_BULGE(new EnumDriveablePart[] { }, "torpedoBulge", "Torpedo Bulge"),
    TORPEDO_BULGE_2(new EnumDriveablePart[] { }, "torpedoBulge2", "Torpedo Bulge 2"),
    TORPEDO_BULGE_3(new EnumDriveablePart[] { }, "torpedoBulge3", "Torpedo Bulge 3"),
    TORPEDO_BULGE_4(new EnumDriveablePart[] { }, "torpedoBulge4", "Torpedo Bulge 4"),
    BULGE(new EnumDriveablePart[] { }, "bulge", "Torpedo Bulge"),
    BULGE_LEFT(new EnumDriveablePart[] { }, "bulgel", "Torpedo Bulge (Left)"),
    BULGE_RIGHT(new EnumDriveablePart[] { }, "bulger", "Torpedo Bulge (Right)"),
    TURRET_1(new EnumDriveablePart[] { }, "turret1", "Turret 1"),
    TURRET_2(new EnumDriveablePart[] { }, "turret2", "Turret 2"),
    TURRET_3(new EnumDriveablePart[] { }, "turret3", "Turret 3"),
    TURRET_4(new EnumDriveablePart[] { }, "turret4", "Turret 4"),
    TURRET_5(new EnumDriveablePart[] { }, "turret5", "Turret 5"),
    TURRET_6(new EnumDriveablePart[] { }, "turret6", "Turret 6"),
    TURRET_7(new EnumDriveablePart[] { }, "turret7", "Turret 7"),
    TURRET_8(new EnumDriveablePart[] { }, "turret8", "Turret 8"),
    TURRET_9(new EnumDriveablePart[] { }, "turret9", "Turret 9"),
    TURRET_10(new EnumDriveablePart[] { }, "turret10", "Turret 10"),
    TURRET_11(new EnumDriveablePart[] { }, "turret11", "Turret 11"),
    TURRET_12(new EnumDriveablePart[] { }, "turret12", "Turret 12"),
    TURRET_13(new EnumDriveablePart[] { }, "turret13", "Turret 13"),
    TURRET_14(new EnumDriveablePart[] { }, "turret14", "Turret 14"),
    TURRET_15(new EnumDriveablePart[] { }, "turret15", "Turret 15"),
    TURRET_16(new EnumDriveablePart[] { }, "turret16", "Turret 16"),
    BULKHEAD(new EnumDriveablePart[] { }, "bulkhead", "Bulkhead"),
    BULKHEAD_2(new EnumDriveablePart[] { }, "bulkhead2", "Bulkhead 2"),
    PORT(new EnumDriveablePart[] { }, "port", "Portside (Left)"),
    STARBOARD(new EnumDriveablePart[] { }, "starboard", "Starboard (Right)"),
    MIDSECTION(new EnumDriveablePart[] { }, "midsection", "Midsection"),
    LEFT(new EnumDriveablePart[] { }, "left", "Center Port"),
    RIGHT(new EnumDriveablePart[] { }, "right", "Center Starboard"),
    BELT_LEFT(new EnumDriveablePart[] { }, "beltl", "Armor Belt (Left)"),
    BELT_RIGHT(new EnumDriveablePart[] { }, "beltr", "Armor Belt (Right)"),
    GASBAG(new EnumDriveablePart[] { }, "gasbag", "Gas Bag"),
    BUOYANCY(new EnumDriveablePart[] { }, "buoyancy", "Buoyancy"),
    ENGINE(new EnumDriveablePart[] { }, "engine", "Engine"),
    ENGINE_2(new EnumDriveablePart[] { }, "engine2", "Engine 2"),
    ENGINE_3(new EnumDriveablePart[] { }, "engine3", "Engine 3"),
    ENGINE_4(new EnumDriveablePart[] { }, "engine4", "Engine 4"),
    ENGINE_5(new EnumDriveablePart[] { }, "engine5", "Engine 5"),
    ENGINE_6(new EnumDriveablePart[] { }, "engine6", "Engine 6"),

    //Mecha parts
    LEFT_ARM(new EnumDriveablePart[] { }, "leftArm", "Arm (Left)"),
    RIGHT_ARM(new EnumDriveablePart[] { }, "rightArm", "Arm (Right)"),
    HEAD(new EnumDriveablePart[] { }, "head", "Head"),
    HIPS(new EnumDriveablePart[] { }, "hips", "Hips"),
    BARREL(new EnumDriveablePart[] { }, "barrel", "Barrel"),

    //Shared part
    CORE(new EnumDriveablePart[]
        {
                BAY,
                LEFT_WING,
                RIGHT_WING,
                NOSE,
                TURRET,
                CORE_WHEEL,
                LEFT_ARM,
                RIGHT_ARM,
                HEAD,
                HIPS,
                BLADES,
                SKIDS,
                BACK_WHEEL,
                FRONT_WHEEL,
                BACK_LEFT_WHEEL,
                FRONT_LEFT_WHEEL,
                BACK_RIGHT_WHEEL,
                FRONT_RIGHT_WHEEL,
                LEFT_TRACK,
                RIGHT_TRACK,
                TRAILER,
                HARVESTER,
                AIRFRAME,
                FRONTAL_ARMOR, LEFTSIDE_ARMOR, RIGHTSIDE_ARMOR, ADDITIONAL_ARMOR,
                ERA, APS, ADS,
                COMPOSITE, COMPOSITE_2, SPACED, COMPOSITE_LEFT, COMPOSITE_RIGHT,
                GENERIC_0, GENERIC_1, GENERIC_2, GENERIC_3, GENERIC_4,
                GENERIC_5, GENERIC_6, GENERIC_7, GENERIC_8, GENERIC_9,
                BIAS, ERA_2, ERA_3, INFANTRY,
                RIGHT_SKIRT, LEFT_SKIRT, SHIELD,
                TURRET_ARMOR, MORE_TURRET_ARMOR, TURRET_SIDE, TURRET_SKIRT,
                TURRET_WEAK, TURRET_WEAK_2,
                WEAK_SPOT, WEAK_SPOT_2, WEAK_SPOT_3,

                BOW,
                STERN,
                CONNING_TOWER,
                CONNING_TOWER_AFT,
                BRIDGE,
                RADAR_1, RADAR_2, RADAR_3, RADAR_4,
                DIRECTOR_1, DIRECTOR_2, DIRECTOR_3, DIRECTOR_4, DIRECTOR_5, DIRECTOR_6, DIRECTOR_7, DIRECTOR_8,
                SUPERSTRUCTURE,
                HANGAR,
                HANGAR_DECK, HANGAR_DECK_2, HANGAR_DECK_3,
                FLIGHT_DECK, FLIGHT_DECK_2,
                ENGINE_ROOM_1, ENGINE_ROOM_2, ENGINE_ROOM_3, ENGINE_ROOM_4, ENGINE_ROOM_5, ENGINE_ROOM_6, ENGINE_ROOM_7, ENGINE_ROOM_8,
                BOILER_ROOM_1, BOILER_ROOM_2, BOILER_ROOM_3, BOILER_ROOM_4, BOILER_ROOM_5, BOILER_ROOM_6, BOILER_ROOM_7, BOILER_ROOM_8,
                STEERING,
                DECK, DECK_2, DECK_3,
                CITADEL,
                BELT,
                TORPEDO_BULGE, TORPEDO_BULGE_2, TORPEDO_BULGE_3, TORPEDO_BULGE_4,
                TURRET_1, TURRET_2, TURRET_3, TURRET_4, TURRET_5, TURRET_6, TURRET_7, TURRET_8, TURRET_9, TURRET_10, TURRET_11, TURRET_12, TURRET_13, TURRET_14, TURRET_15, TURRET_16,
                BULKHEAD, BULKHEAD_2,
                PORT,
                STARBOARD,
                BULGE, BULGE_LEFT, BULGE_RIGHT,
                MIDSECTION, LEFT, RIGHT, BELT_LEFT, BELT_RIGHT,
                GASBAG, BUOYANCY,
                ENGINE, ENGINE_2, ENGINE_3, ENGINE_4, ENGINE_5, ENGINE_6
        }, "core", "Core");

    private final String shortName;
    private final String name;
    private final EnumDriveablePart[] children;

    private static final Map<String, EnumDriveablePart> BY_NAME;
    private static final List<EnumDriveablePart> ENGINE_ROOMS = List.of(
        ENGINE_ROOM_1, ENGINE_ROOM_2, ENGINE_ROOM_3, ENGINE_ROOM_4,
        ENGINE_ROOM_5, ENGINE_ROOM_6, ENGINE_ROOM_7, ENGINE_ROOM_8,
        ENGINE, ENGINE_2, ENGINE_3, ENGINE_4, ENGINE_5, ENGINE_6);
    private static final List<EnumDriveablePart> BOILER_ROOMS = List.of(BOILER_ROOM_1, BOILER_ROOM_2, BOILER_ROOM_3, BOILER_ROOM_4, BOILER_ROOM_5, BOILER_ROOM_6, BOILER_ROOM_7, BOILER_ROOM_8);

    static
    {
        Map<String, EnumDriveablePart> parts = new HashMap<>();
        for (EnumDriveablePart part : values())
        {
            parts.put(part.shortName.toLowerCase(Locale.ROOT), part);
            parts.put(part.name().toLowerCase(Locale.ROOT), part);
        }
        // Common community-pack names and historical misspellings.
        parts.put("deck1", DECK);
        parts.put("superstucture", SUPERSTRUCTURE);
        parts.put("engine1", ENGINE);
        parts.put("engine7", ENGINE_ROOM_7);
        parts.put("engine8", ENGINE_ROOM_8);
        BY_NAME = Collections.unmodifiableMap(parts);
    }

    EnumDriveablePart(EnumDriveablePart[] parts, String s, String s2)
    {
        children = parts;
        shortName = s;
        name = s2;
    }

    /** Used to determine when parts can be stuck back on */
    public EnumDriveablePart[] getParents()
    {
        List<EnumDriveablePart> parents = new ArrayList<>();
        for (EnumDriveablePart part : values())
        {
            for (EnumDriveablePart childPart : part.getChildren())
            {
                if (childPart == this)
                    parents.add(part);
            }
        }
        return parents.toArray(new EnumDriveablePart[0]);
    }

    /** For reading parts from driveable files */
    public static EnumDriveablePart getPart(String s)
    {
        return s == null ? null : BY_NAME.get(s.trim().toLowerCase(Locale.ROOT));
    }

    public static List<EnumDriveablePart> getEngineRooms()
    {
        return ENGINE_ROOMS;
    }

    public static List<EnumDriveablePart> getBoilerRooms()
    {
        return BOILER_ROOMS;
    }

    /** Parts whose authored boxes rotate with the main turret and use turret armour defaults. */
    public static boolean isTurretMounted(EnumDriveablePart part)
    {
        return part == TURRET || part == BARREL || part == MORE_TURRET_ARMOR
            || part != null && part.name().startsWith("TURRET_");
    }

    public static boolean isWheel(EnumDriveablePart part)
    {
        return part == CORE_WHEEL || part == TAIL_WHEEL || part == LEFT_WING_WHEEL || part == RIGHT_WING_WHEEL
            || part == FRONT_WHEEL || part == BACK_WHEEL || part == FRONT_LEFT_WHEEL || part == FRONT_RIGHT_WHEEL
            || part == BACK_LEFT_WHEEL || part == BACK_RIGHT_WHEEL;
    }
}
