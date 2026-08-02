package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class GameTypeTDM extends GameType
{
    private static final String NBT_FRIENDLY_FIRE = "tdm_friendly_fire";
    private static final String NBT_AUTO_BALANCE = "tdm_auto_balance";

    private boolean friendlyFire;
    private boolean autoBalance = true;

    GameTypeTDM()
    {
        this("tdm", "Team Deathmatch", 2);
    }

    protected GameTypeTDM(String id, String name, int requiredTeams)
    {
        super(id, name, requiredTeams);
    }

    @Override
    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        ServerPlayer attacker = getPlayerFromDamageSource(source);
        PlayerData victimData = PlayerData.getInstance(victim);
        victimData.setDeaths(victimData.getDeaths() + 1);
        PlayerStats victimStats = manager.getStats(victim);
        victimStats.recordDeath();
        manager.getCurrentLoadoutPool().ifPresent(pool -> manager.awardExperience(victim, pool.getExperienceForDeath()));
        if (attacker == null || attacker == victim)
        {
            victimData.setScore(victimData.getScore() - 1);
            return;
        }
        Team attackerTeam = manager.getPlayerTeam(attacker);
        Team victimTeam = manager.getPlayerTeam(victim);
        PlayerData attackerData = PlayerData.getInstance(attacker);
        if (attackerTeam == victimTeam)
        {
            attackerData.setScore(attackerData.getScore() - 1);
            return;
        }
        if (attackerTeam == null || attackerTeam == Team.SPECTATORS)
            return;
        attackerData.setScore(attackerData.getScore() + 1);
        attackerData.setKills(attackerData.getKills() + 1);
        PlayerStats attackerStats = manager.getStats(attacker);
        attackerStats.recordKill(attacker.distanceTo(victim));
        int xp = manager.getCurrentLoadoutPool()
            .map(pool -> pool.getExperienceForKill() + Math.max(0, attackerStats.getKillstreak() - 1) * pool.getExperienceForKillstreakBonus())
            .orElse(manager.getStats(victim).getRank() * 2 + Math.max(1, (int) (attacker.distanceTo(victim) / 10D)));
        manager.awardExperience(attacker, xp);
        manager.addTeamScore(attackerTeam, 1);
    }

    @Override
    public boolean isFriendlyFireEnabled()
    {
        return friendlyFire;
    }

    @Override
    public boolean isAutoBalanceEnabled()
    {
        return autoBalance;
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        if ("friendlyfire".equalsIgnoreCase(variable))
        {
            friendlyFire = Boolean.parseBoolean(value);
            return true;
        }
        if ("autobalance".equalsIgnoreCase(variable))
        {
            autoBalance = Boolean.parseBoolean(value);
            return true;
        }
        return false;
    }

    @Override
    public void loadSettings(CompoundTag tag)
    {
        friendlyFire = tag.getBoolean(NBT_FRIENDLY_FIRE);
        autoBalance = !tag.contains(NBT_AUTO_BALANCE) || tag.getBoolean(NBT_AUTO_BALANCE);
    }

    @Override
    public void saveSettings(CompoundTag tag)
    {
        tag.putBoolean(NBT_FRIENDLY_FIRE, friendlyFire);
        tag.putBoolean(NBT_AUTO_BALANCE, autoBalance);
    }
}
