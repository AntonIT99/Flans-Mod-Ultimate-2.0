package com.flansmodultimate.common.teams;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@NoArgsConstructor
public class TeamsManager
{
    private boolean voting = false;
    @Getter
    private boolean explosionsBreakBlocks = true; //TODO: default value through config?
    private boolean roundsGenerator;
    private boolean driveablesBreakBlocks = true;
    private boolean bombsEnabled = true;
    private boolean shellsEnabled = true;
    private boolean bulletsEnabled = true;
    private boolean forceAdventureMode = true;
    private boolean canBreakGuns = true;
    @Getter
    private boolean canBreakGlass = true; //TODO: default value through config?
    private boolean armourDrops = true;
    private boolean vehiclesNeedFuel = true;
    private boolean overrideHunger = true;
    private boolean survivalCanBreakVehicles = true;
    private boolean survivalCanPlaceVehicles = true;

    /** The current round in play. This class replaces the old set of 3 fields "currentGametype", "currentMap" and "teams" */
    @Nullable
    private TeamsRound currentRound;

    public Optional<TeamsRound> getCurrentRound()
    {
        return Optional.ofNullable(currentRound);
    }
}
