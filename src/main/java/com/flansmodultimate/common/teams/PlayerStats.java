package com.flansmodultimate.common.teams;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/** UUID-keyed, world-persistent Teams statistics. */
public final class PlayerStats
{
    private final UUID playerId;
    private String lastKnownName;
    private int kills;
    private int deaths;
    private int experience;
    private int totalExperience;
    private int rank = 1;
    private double longestKill;
    private int playedRounds;
    private long playTimeTicks;
    private int mvpCount;
    private int capturedFlags;
    private int savedFlags;
    private int vehiclesDestroyed;
    private String selectedTeam = "spectators";
    private String selectedClass = "";

    public PlayerStats(UUID playerId, String lastKnownName)
    {
        this.playerId = playerId;
        this.lastKnownName = StringUtils.defaultString(lastKnownName);
    }

    public UUID getPlayerId() { return playerId; }
    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String value) { lastKnownName = StringUtils.defaultString(value); }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getExperience() { return experience; }
    public int getTotalExperience() { return totalExperience; }
    public int getRank() { return rank; }
    public double getAverageKills() { return playedRounds == 0 ? 0D : (double) kills / playedRounds; }
    public double getLongestKill() { return longestKill; }
    public int getPlayedRounds() { return playedRounds; }
    public long getPlayTimeTicks() { return playTimeTicks; }
    public int getMvpCount() { return mvpCount; }
    public int getCapturedFlags() { return capturedFlags; }
    public int getSavedFlags() { return savedFlags; }
    public int getVehiclesDestroyed() { return vehiclesDestroyed; }
    public String getSelectedTeam() { return selectedTeam; }
    public String getSelectedClass() { return selectedClass; }

    public void recordKill(double distance)
    {
        kills++;
        longestKill = Math.max(longestKill, distance);
        addExperience(Math.max(1, (int) Math.floor(distance / 10D)));
    }

    public void recordDeath() { deaths++; }
    public void recordRound() { playedRounds++; }
    public void addPlayTime(long ticks) { playTimeTicks = Math.max(0, playTimeTicks + ticks); }
    public void recordMvp() { mvpCount++; addExperience(250); }
    public void recordFlagCapture() { capturedFlags++; addExperience(20); }
    public void recordFlagSave() { savedFlags++; addExperience(10); }
    public void recordVehicleDestroyed() { vehiclesDestroyed++; }
    public void setSelection(String team, String playerClass)
    {
        selectedTeam = StringUtils.defaultIfBlank(team, "spectators");
        selectedClass = StringUtils.defaultString(playerClass);
    }

    public void addExperience(int amount)
    {
        if (amount <= 0)
            return;
        experience += amount;
        totalExperience += amount;
        while (experience >= 1000)
        {
            experience -= 1000;
            rank++;
        }
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", playerId);
        tag.putString("Name", lastKnownName);
        tag.putInt("Kills", kills);
        tag.putInt("Deaths", deaths);
        tag.putInt("Experience", experience);
        tag.putInt("TotalExperience", totalExperience);
        tag.putInt("Rank", rank);
        tag.putDouble("LongestKill", longestKill);
        tag.putInt("PlayedRounds", playedRounds);
        tag.putLong("PlayTime", playTimeTicks);
        tag.putInt("MvpCount", mvpCount);
        tag.putInt("CapturedFlags", capturedFlags);
        tag.putInt("SavedFlags", savedFlags);
        tag.putInt("VehiclesDestroyed", vehiclesDestroyed);
        tag.putString("SelectedTeam", selectedTeam);
        tag.putString("SelectedClass", selectedClass);
        return tag;
    }

    public static PlayerStats load(CompoundTag tag)
    {
        PlayerStats result = new PlayerStats(tag.getUUID("Id"), tag.getString("Name"));
        result.kills = tag.getInt("Kills");
        result.deaths = tag.getInt("Deaths");
        result.experience = tag.getInt("Experience");
        result.totalExperience = tag.getInt("TotalExperience");
        result.rank = Math.max(1, tag.getInt("Rank"));
        result.longestKill = tag.getDouble("LongestKill");
        result.playedRounds = tag.getInt("PlayedRounds");
        result.playTimeTicks = tag.getLong("PlayTime");
        result.mvpCount = tag.getInt("MvpCount");
        result.capturedFlags = tag.getInt("CapturedFlags");
        result.savedFlags = tag.getInt("SavedFlags");
        result.vehiclesDestroyed = tag.getInt("VehiclesDestroyed");
        result.selectedTeam = tag.getString("SelectedTeam");
        result.selectedClass = tag.getString("SelectedClass");
        return result;
    }
}
