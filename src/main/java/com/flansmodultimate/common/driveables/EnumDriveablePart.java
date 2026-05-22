package com.flansmodultimate.common.driveables;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

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
                STARBOARD
        }, "core", "Core");

    private final String shortName;
    private final String name;
    private final EnumDriveablePart[] children;

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
        for(EnumDriveablePart part : values())
            if(part.getShortName().equals(s))
                return part;
        return null;
    }

    public static List<EnumDriveablePart> getEngineRooms()
    {
        List<EnumDriveablePart> engineRooms = new ArrayList<>();

        engineRooms.add(ENGINE_ROOM_1);
        engineRooms.add(ENGINE_ROOM_2);
        engineRooms.add(ENGINE_ROOM_3);
        engineRooms.add(ENGINE_ROOM_4);
        engineRooms.add(ENGINE_ROOM_5);
        engineRooms.add(ENGINE_ROOM_6);
        engineRooms.add(ENGINE_ROOM_7);
        engineRooms.add(ENGINE_ROOM_8);

        return engineRooms;
    }

    public static List<EnumDriveablePart> getBoilerRooms()
    {
        List<EnumDriveablePart> boilerRooms = new ArrayList<>();

        boilerRooms.add(BOILER_ROOM_1);
        boilerRooms.add(BOILER_ROOM_2);
        boilerRooms.add(BOILER_ROOM_3);
        boilerRooms.add(BOILER_ROOM_4);
        boilerRooms.add(BOILER_ROOM_5);
        boilerRooms.add(BOILER_ROOM_6);
        boilerRooms.add(BOILER_ROOM_7);
        boilerRooms.add(BOILER_ROOM_8);

        return boilerRooms;
    }

    public static boolean isWheel(EnumDriveablePart part)
    {
        return part == CORE_WHEEL || part == TAIL_WHEEL || part == LEFT_WING_WHEEL || part == RIGHT_WING_WHEEL;
    }
}
