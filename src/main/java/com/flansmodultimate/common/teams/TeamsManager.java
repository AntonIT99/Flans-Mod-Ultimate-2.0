package com.flansmodultimate.common.teams;

import lombok.Getter;

public class TeamsManager
{
    private static boolean voting = false;
    @Getter
    private static boolean explosions = true;
    private static boolean roundsGenerator = false;
    private static boolean driveablesBreakBlocks = true;
    private static boolean bombsEnabled = true;
    private static boolean shellsEnabled = true;
    private static boolean bulletsEnabled = true;
    private static boolean forceAdventureMode = true;
    private static boolean canBreakGuns = true;
    @Getter
    private static boolean canBreakGlass = true;
    private static boolean armourDrops = true;
    private static boolean vehiclesNeedFuel = true;
    private static boolean overrideHunger = true;
    private static boolean survivalCanBreakVehicles = true;
    private static boolean survivalCanPlaceVehicles = true;
}
