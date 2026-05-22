package com.flansmodultimate.common.teams;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@NoArgsConstructor
public class TeamsManager
{
    public enum EnumWeaponDrop
    {
        NONE, DROPS, SMART_DROPS
    }


    @Getter @Setter
    private boolean explosionsBreakBlocks = true;
    @Getter @Setter
    private boolean canBreakGlass = true;
    @Getter
    private boolean canBreakGuns = true;

    private boolean voting = false;
    private boolean roundsGenerator;
    private boolean driveablesBreakBlocks = true;
    private boolean bombsEnabled = true;
    private boolean shellsEnabled = true;
    private boolean bulletsEnabled = true;
    private boolean forceAdventureMode = true;
    private boolean armourDrops = true;
    private boolean vehiclesNeedFuel = true;
    private boolean overrideHunger = true;
    private boolean survivalCanBreakVehicles = true;
    private boolean survivalCanPlaceVehicles = true;
    @Getter
    private EnumWeaponDrop weaponDrops = EnumWeaponDrop.DROPS;
    /** Life in seconds of certain entity types. 0 is eternal */
    @Getter
    private int mgLife;
    private int planeLife;
    private int vehicleLife;
    private int mechaLove;
    private int aaLife;
    @Getter
    private int bulletSnapshotMin;
    @Getter
    private int bulletSnapshotDivisor = 50;

    /** The current round in play. This class replaces the old set of 3 fields "currentGametype", "currentMap" and "teams" */
    @Nullable
    private TeamsRound currentRound;

    public Optional<TeamsRound> getCurrentRound()
    {
        return Optional.ofNullable(currentRound);
    }
}
