package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.types.Team;
import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public final class GameTypeCTF extends GameTypeTDM
{
    private static final String NBT_FLAG_RETURN_TIME = "ctf_flag_time";
    private int flagReturnTimeSeconds = 30;

    GameTypeCTF()
    {
        super("ctf", "Capture the Flag", 2);
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
        Flagpole base = flag.getBase();
        if (base == null || !manager.isBaseInCurrentMap(base))
            return;
        Team playerTeam = manager.getPlayerTeam(player);
        Team flagTeam = manager.getTeamForBase(base);
        if (playerTeam == null || playerTeam == Team.SPECTATORS || flagTeam == null)
            return;

        PlayerData data = PlayerData.getInstance(player);
        if (playerTeam == flagTeam)
        {
            if (!flag.isHome() && !flag.hasCarrier())
            {
                flag.resetToBase();
                data.setScore(data.getScore() + 2);
                manager.getStats(player).recordFlagSave();
                manager.awardExperience(player, 10);
                manager.broadcast(Component.literal(player.getScoreboardName() + " returned the " + flagTeam.getName() + " flag"));
                return;
            }

            Flag carried = manager.getFlagCarriedBy(player);
            if (flag.isHome() && carried != null && carried != flag)
            {
                Team captured = manager.getTeamForBase(carried.getBase());
                carried.resetToBase();
                manager.addTeamScore(playerTeam, 1);
                data.setScore(data.getScore() + 10);
                manager.getStats(player).recordFlagCapture();
                manager.awardExperience(player, 20);
                manager.broadcast(Component.literal(player.getScoreboardName() + " captured the " + (captured == null ? "enemy" : captured.getName()) + " flag"));
            }
            return;
        }

        if (flag.isCarriedBy(player))
        {
            flag.drop(flagReturnTimeSeconds * 20);
            return;
        }
        if (!flag.hasCarrier())
        {
            if (flag.isHome())
                data.setScore(data.getScore() + 3);
            flag.pickUp(player);
            manager.broadcast(Component.literal(player.getScoreboardName() + " picked up the " + flagTeam.getName() + " flag"));
        }
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
