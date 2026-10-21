package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.types.Team;
import lombok.AccessLevel;
import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Capture the Flag. Players take, return and capture flags by clicking them; combatants of the round, which addons
 * field, do so by touching them. Both follow the same rules, and only players earn score, statistics and experience.
 */
@Getter
public final class GameTypeCTF extends GameTypeTDM
{
    private static final String FLAG_SUFFIX = " flag";
    private static final String NBT_FLAG_RETURN_TIME = "ctf_flag_time";
    /** Ticks between checks for combatants touching a flag. */
    private static final int TOUCH_INTERVAL = 5;
    /** Blocks around a flag within which a combatant touches it, across and up and down. */
    private static final double TOUCH_REACH = 1.5D;
    private static final double TOUCH_HEIGHT = 2.5D;
    private int flagReturnTimeSeconds = 30;
    @Getter(AccessLevel.NONE)
    private int touchTicks;

    GameTypeCTF()
    {
        super("ctf", "Capture the Flag", TeamLimits.upToSides(TeamsRound.MAX_SIDES));
    }

    @Override
    public void baseClicked(TeamsManager manager, ServerPlayer player, Flagpole base)
    {
        if (base.getFlag() != null)
            flagClicked(manager, player, base.getFlag());
    }

    @Override
    public void flagClicked(TeamsManager manager, ServerPlayer player, Flag flag)
    {
        flagTouched(manager, player, flag);
    }

    /** Lets the combatants of the round touch flags, as players click them. */
    @Override
    public void tick(TeamsManager manager)
    {
        if (++touchTicks < TOUCH_INTERVAL)
            return;
        touchTicks = 0;
        ServerLevel level = manager.getCurrentLevel();
        if (level == null)
            return;
        for (Flag flag : manager.getFlags())
        {
            AABB reach = flag.getBoundingBox().inflate(TOUCH_REACH, TOUCH_HEIGHT, TOUCH_REACH);
            for (LivingEntity fighter : level.getEntitiesOfClass(LivingEntity.class, reach, entity -> entity.isAlive() && TeamsCombatants.getCombatantTeam(manager, entity) != null))
                flagTouched(manager, fighter, flag);
        }
    }

    /** A player or a combatant takes, returns or captures a flag, or drops the one it carries. */
    private void flagTouched(TeamsManager manager, LivingEntity fighter, Flag flag)
    {
        Flagpole base = flag.getBase();
        if (base == null || !manager.isBaseInCurrentMap(base))
            return;
        Team fighterTeam = TeamsCombatants.getTeam(fighter);
        Team flagTeam = manager.getTeamForBase(base);
        if (fighterTeam == null || fighterTeam == Team.SPECTATORS || flagTeam == null)
            return;

        if (manager.areAllied(fighterTeam, flagTeam))
        {
            touchOwnFlag(manager, fighter, fighterTeam, flag, flagTeam);
            return;
        }

        // A player drops the enemy flag by clicking it again; a combatant keeps it while it touches it.
        if (flag.isCarriedBy(fighter))
        {
            if (fighter instanceof ServerPlayer)
                flag.drop(flagReturnTimeSeconds * 20);
            return;
        }
        if (!flag.hasCarrier())
        {
            if (flag.isHome() && fighter instanceof ServerPlayer player)
                PlayerData.getInstance(player).setScore(PlayerData.getInstance(player).getScore() + 3);
            flag.pickUp(fighter);
            manager.broadcast(Component.literal(name(fighter) + " picked up the " + flagTeam.getName() + FLAG_SUFFIX));
        }
    }

    /** Returning a dropped flag of the fighter's own side, or capturing an enemy flag at a flag of its side at home. */
    private static void touchOwnFlag(TeamsManager manager, LivingEntity fighter, Team fighterTeam, Flag flag, Team flagTeam)
    {
        PlayerData data = fighter instanceof ServerPlayer player ? PlayerData.getInstance(player) : null;
        if (!flag.isHome() && !flag.hasCarrier())
        {
            flag.resetToBase();
            if (data != null)
            {
                ServerPlayer player = (ServerPlayer) fighter;
                data.setScore(data.getScore() + 2);
                manager.getStats(player).recordFlagSave();
                manager.awardExperience(player, 10);
            }
            manager.broadcast(Component.literal(name(fighter) + " returned the " + flagTeam.getName() + FLAG_SUFFIX));
            return;
        }

        Flag carried = manager.getFlagCarriedBy(fighter);
        if (flag.isHome() && carried != null && carried != flag)
        {
            Team captured = manager.getTeamForBase(carried.getBase());
            carried.resetToBase();
            manager.addTeamScore(fighterTeam, 1);
            if (data != null)
            {
                ServerPlayer player = (ServerPlayer) fighter;
                data.setScore(data.getScore() + 10);
                manager.getStats(player).recordFlagCapture();
                manager.awardExperience(player, 20);
            }
            manager.broadcast(Component.literal(name(fighter) + " captured the " + (captured == null ? "enemy" : captured.getName()) + FLAG_SUFFIX));
        }
    }

    private static String name(LivingEntity fighter)
    {
        return fighter instanceof ServerPlayer player ? player.getScoreboardName() : fighter.getName().getString();
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        if ("flagtime".equalsIgnoreCase(variable))
        {
            flagReturnTimeSeconds = Math.max(1, Integer.parseInt(value));
            return true;
        }
        return super.setVariable(variable, value);
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        super.loadSettings(tag);
        flagReturnTimeSeconds = Math.max(1, tag.getInt(NBT_FLAG_RETURN_TIME));
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        super.saveSettings(tag);
        tag.putInt(NBT_FLAG_RETURN_TIME, flagReturnTimeSeconds);
    }
}
