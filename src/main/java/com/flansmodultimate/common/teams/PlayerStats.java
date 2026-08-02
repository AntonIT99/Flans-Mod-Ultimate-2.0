package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.LoadoutPool;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** UUID-keyed, world-persistent Teams statistics. */
public final class PlayerStats
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_KILLS = "kills";
    private static final String NBT_DEATHS = "deaths";
    private static final String NBT_EXPERIENCE = "experience";
    private static final String NBT_TOTAL_EXPERIENCE = "total_experience";
    private static final String NBT_RANK = "rank";
    private static final String NBT_LONGEST_KILL = "longest_kill";
    private static final String NBT_PLAYED_ROUNDS = "played_rounds";
    private static final String NBT_PLAY_TIME = "play_time";
    private static final String NBT_MVP_COUNT = "mvp_count";
    private static final String NBT_CAPTURED_FLAGS = "captured_flags";
    private static final String NBT_SAVED_FLAGS = "saved_flags";
    private static final String NBT_VEHICLES_DESTROYED = "vehicles_destroyed";
    private static final String NBT_SELECTED_TEAM = "selected_team";
    private static final String NBT_SELECTED_CLASS = "selected_class";
    private static final String NBT_SELECTED_LOADOUT = "selected_loadout";
    private static final String NBT_KILLSTREAK = "killstreak";
    private static final String NBT_BEST_KILLSTREAK = "best_killstreak";
    private static final String NBT_LOADOUT_PROFILES = "loadout_profiles";
    private static final String NBT_POOL = "pool";
    private static final String NBT_LOADOUTS = "loadouts";
    private static final String NBT_REWARD_BOXES = "reward_boxes";

    @Getter
    private final UUID playerId;
    @Getter
    private String lastKnownName;
    @Getter
    private int kills;
    @Getter
    private int deaths;
    @Getter
    private int experience;
    @Getter
    private int totalExperience;
    @Getter
    private int rank = 1;
    @Getter
    private double longestKill;
    @Getter
    private int playedRounds;
    @Getter
    private long playTimeTicks;
    @Getter
    private int mvpCount;
    @Getter
    private int capturedFlags;
    @Getter
    private int savedFlags;
    @Getter
    private int vehiclesDestroyed;
    @Getter
    private String selectedTeam = "spectators";
    @Getter
    private String selectedClass = "";
    @Getter
    private int selectedLoadout;
    @Getter
    private int killstreak;
    @Getter
    private int bestKillstreak;
    private final Map<String, List<PlayerLoadout>> loadoutProfiles = new LinkedHashMap<>();
    private final List<RewardBoxInstance> rewardBoxes = new ArrayList<>();

    public PlayerStats(UUID playerId, String lastKnownName)
    {
        this.playerId = playerId;
        this.lastKnownName = StringUtils.defaultString(lastKnownName);
    }

    public void setLastKnownName(String value)
    {
        lastKnownName = StringUtils.defaultString(value);
    }

    public double getAverageKills()
    {
        return playedRounds == 0 ? 0D : (double) kills / playedRounds;
    }

    public void recordKill(double distance)
    {
        kills++;
        killstreak++;
        bestKillstreak = Math.max(bestKillstreak, killstreak);
        longestKill = Math.max(longestKill, distance);
    }

    public void recordDeath()
    {
        deaths++; killstreak = 0;
    }

    public void recordRound()
    {
        playedRounds++;
    }

    public void addPlayTime(long ticks)
    {
        playTimeTicks = Math.max(0, playTimeTicks + ticks);
    }

    public void recordMvp()
    {
        mvpCount++;
    }

    public void recordFlagCapture()
    {
        capturedFlags++;
    }

    public void recordFlagSave()
    {
        savedFlags++;
    }

    public void recordVehicleDestroyed()
    {
        vehiclesDestroyed++;
    }

    public void setSelection(String team, String playerClass)
    {
        selectedTeam = StringUtils.defaultIfBlank(team, "spectators");
        selectedClass = StringUtils.defaultString(playerClass);
    }

    public void setSelectedLoadout(int index)
    {
        selectedLoadout = Math.max(0, Math.min(LoadoutPool.LOADOUT_COUNT - 1, index));
    }

    public List<PlayerLoadout> getLoadouts(LoadoutPool pool)
    {
        return loadoutProfiles.computeIfAbsent(pool.getOriginalShortName(), ignored -> {
            List<PlayerLoadout> created = new ArrayList<>(LoadoutPool.LOADOUT_COUNT);
            for (int i = 0; i < LoadoutPool.LOADOUT_COUNT; i++) created.add(pool.getDefaultLoadout(i));
            for (String boxId : pool.getRewardsForRank(1)) addRewardBox(boxId, RewardBoxInstance.Origin.LEVEL_UP);
            return created;
        });
    }

    public PlayerLoadout getSelectedLoadout(LoadoutPool pool)
    {
        return getLoadouts(pool).get(selectedLoadout);
    }

    public boolean replaceLoadout(LoadoutPool pool, int index, PlayerLoadout loadout)
    {
        if (index < 0 || index >= LoadoutPool.LOADOUT_COUNT || rank < pool.getLoadoutUnlockLevel(index)
            || !pool.validate(loadout, rank, this::ownsReward)) return false;
        getLoadouts(pool).set(index, loadout.copy());
        return true;
    }

    public List<RewardBoxInstance> getRewardBoxes()
    {
        return List.copyOf(rewardBoxes);
    }

    public boolean ownsReward(String key)
    {
        return rewardBoxes.stream().anyMatch(box -> key.equals(box.rewardKey()));
    }

    public long getUnopenedRewardBoxCount()
    {
        return rewardBoxes.stream().filter(box -> !box.isOpened()).count();
    }

    public void addRewardBox(String boxId, RewardBoxInstance.Origin origin)
    {
        rewardBoxes.add(new RewardBoxInstance(boxId, origin));
    }

    public Optional<RewardBoxInstance> getRewardBox(UUID id)
    {
        return rewardBoxes.stream().filter(box -> box.id().equals(id)).findFirst();
    }

    public boolean markRewardBoxOpened(UUID id, String rewardKey)
    {
        for (int i = 0; i < rewardBoxes.size(); i++)
        {
            RewardBoxInstance box = rewardBoxes.get(i);
            if (box.id().equals(id) && !box.isOpened())
            {
                rewardBoxes.set(i, box.openedWith(rewardKey));
                return true;
            }
        }
        return false;
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

    /** Adds XP using a pool's rank curve and returns every newly reached rank. */
    public List<Integer> addExperience(int amount, LoadoutPool pool)
    {
        if (amount <= 0 || rank >= pool.getMaxLevel()) return List.of();
        experience += amount;
        totalExperience += amount;
        List<Integer> reached = new ArrayList<>();
        while (rank < pool.getMaxLevel() && experience >= pool.getExperienceForRank(rank))
        {
            experience -= pool.getExperienceForRank(rank);
            rank++;
            reached.add(rank);
        }
        if (rank >= pool.getMaxLevel()) experience = 0;
        return reached;
    }

    public void resetRankProgress()
    {
        experience = 0;
        totalExperience = 0;
        rank = 1;
        selectedLoadout = 0;
        killstreak = 0;
        bestKillstreak = 0;
        loadoutProfiles.clear();
        rewardBoxes.clear();
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_ID, playerId); tag.putString(NBT_NAME, lastKnownName); tag.putInt(NBT_KILLS, kills); tag.putInt(NBT_DEATHS, deaths);
        tag.putInt(NBT_EXPERIENCE, experience); tag.putInt(NBT_TOTAL_EXPERIENCE, totalExperience); tag.putInt(NBT_RANK, rank); tag.putDouble(NBT_LONGEST_KILL, longestKill);
        tag.putInt(NBT_PLAYED_ROUNDS, playedRounds); tag.putLong(NBT_PLAY_TIME, playTimeTicks); tag.putInt(NBT_MVP_COUNT, mvpCount);
        tag.putInt(NBT_CAPTURED_FLAGS, capturedFlags); tag.putInt(NBT_SAVED_FLAGS, savedFlags); tag.putInt(NBT_VEHICLES_DESTROYED, vehiclesDestroyed);
        tag.putString(NBT_SELECTED_TEAM, selectedTeam); tag.putString(NBT_SELECTED_CLASS, selectedClass); tag.putInt(NBT_SELECTED_LOADOUT, selectedLoadout);
        tag.putInt(NBT_KILLSTREAK, killstreak); tag.putInt(NBT_BEST_KILLSTREAK, bestKillstreak);

        ListTag profiles = new ListTag();
        loadoutProfiles.forEach((pool, loadouts) -> {
            CompoundTag profile = new CompoundTag();
            profile.putString(NBT_POOL, pool);
            ListTag values = new ListTag();
            loadouts.forEach(loadout -> values.add(loadout.save()));
            profile.put(NBT_LOADOUTS, values);
            profiles.add(profile);
        });
        tag.put(NBT_LOADOUT_PROFILES, profiles);

        ListTag boxes = new ListTag();
        rewardBoxes.forEach(box -> boxes.add(box.save()));
        tag.put(NBT_REWARD_BOXES, boxes);
        return tag;
    }

    public static PlayerStats load(CompoundTag tag)
    {
        PlayerStats result = new PlayerStats(tag.getUUID(NBT_ID), tag.getString(NBT_NAME));
        result.kills = tag.getInt(NBT_KILLS); result.deaths = tag.getInt(NBT_DEATHS); result.experience = tag.getInt(NBT_EXPERIENCE);
        result.totalExperience = tag.getInt(NBT_TOTAL_EXPERIENCE); result.rank = Math.max(1, tag.getInt(NBT_RANK)); result.longestKill = tag.getDouble(NBT_LONGEST_KILL);
        result.playedRounds = tag.getInt(NBT_PLAYED_ROUNDS); result.playTimeTicks = tag.getLong(NBT_PLAY_TIME); result.mvpCount = tag.getInt(NBT_MVP_COUNT);
        result.capturedFlags = tag.getInt(NBT_CAPTURED_FLAGS); result.savedFlags = tag.getInt(NBT_SAVED_FLAGS); result.vehiclesDestroyed = tag.getInt(NBT_VEHICLES_DESTROYED);
        result.selectedTeam = tag.getString(NBT_SELECTED_TEAM); result.selectedClass = tag.getString(NBT_SELECTED_CLASS);
        result.selectedLoadout = Math.max(0, Math.min(LoadoutPool.LOADOUT_COUNT - 1, tag.getInt(NBT_SELECTED_LOADOUT)));
        result.killstreak = tag.getInt(NBT_KILLSTREAK); result.bestKillstreak = tag.getInt(NBT_BEST_KILLSTREAK);
        for (Tag rawProfile : tag.getList(NBT_LOADOUT_PROFILES, Tag.TAG_COMPOUND))
        {
            CompoundTag profile = (CompoundTag) rawProfile;
            List<PlayerLoadout> loadouts = new ArrayList<>();
            for (Tag rawLoadout : profile.getList(NBT_LOADOUTS, Tag.TAG_COMPOUND)) loadouts.add(PlayerLoadout.load((CompoundTag) rawLoadout));
            while (loadouts.size() < LoadoutPool.LOADOUT_COUNT) loadouts.add(new PlayerLoadout());
            result.loadoutProfiles.put(profile.getString(NBT_POOL), new ArrayList<>(loadouts.subList(0, LoadoutPool.LOADOUT_COUNT)));
        }
        for (Tag rawBox : tag.getList(NBT_REWARD_BOXES, Tag.TAG_COMPOUND)) result.rewardBoxes.add(RewardBoxInstance.load((CompoundTag) rawBox));
        return result;
    }
}
