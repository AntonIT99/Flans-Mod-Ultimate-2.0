package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class GameTypeTDM extends GameType
{
    private static final String NBT_FRIENDLY_FIRE = "tdm_friendly_fire";
    private static final String NBT_AUTO_BALANCE = "tdm_auto_balance";

    private boolean friendlyFire;
    private boolean autoBalance = true;

    GameTypeTDM()
    {
        this("tdm", "Team Deathmatch", TeamLimits.upToSides(TeamsRound.MAX_SIDES));
    }

    protected GameTypeTDM(String id, String name, TeamLimits teamLimits)
    {
        super(id, name, teamLimits, true);
    }

    @Override
    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        LivingEntity attacker = TeamsCombatants.getAttacker(source);
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
        scoreTeamKill(manager, attacker, victim, manager.getPlayerTeam(victim));
    }

    @Override
    public void combatantKilled(TeamsManager manager, LivingEntity victim, Team victimTeam, DamageSource source)
    {
        LivingEntity attacker = TeamsCombatants.getAttacker(source);
        if (attacker != null && attacker != victim)
            scoreTeamKill(manager, attacker, victim, victimTeam);
    }

    /**
     * Scores a kill by a player or a combatant: an ally killed costs a player a point, an enemy killed earns a player
     * the kill and the killer's team its worth.
     */
    private void scoreTeamKill(TeamsManager manager, LivingEntity attacker, LivingEntity victim, @Nullable Team victimTeam)
    {
        Team attackerTeam = TeamsCombatants.getTeam(attacker);
        if (attackerTeam == victimTeam || manager.areAllied(attackerTeam, victimTeam))
        {
            if (attacker instanceof ServerPlayer player)
            {
                PlayerData attackerData = PlayerData.getInstance(player);
                attackerData.setScore(attackerData.getScore() - 1);
            }
            return;
        }
        if (attackerTeam == null || attackerTeam == Team.SPECTATORS)
            return;
        if (attacker instanceof ServerPlayer player)
            creditPlayerKill(manager, player, victim);
        scoreKill(manager, attackerTeam);
    }

    /** What killing an enemy is worth to the killer's team: one point here, tickets in the objective modes. */
    protected void scoreKill(TeamsManager manager, Team attackerTeam)
    {
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
