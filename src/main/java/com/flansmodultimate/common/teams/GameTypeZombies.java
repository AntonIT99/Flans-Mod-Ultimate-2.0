package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Infection mode: team zero starts human and deaths convert players to team one. */
public final class GameTypeZombies extends GameType
{
    private boolean friendlyFire;
    private int humanPrepTimeTicks = 30 * 20;
    private boolean infectionStarted;

    GameTypeZombies()
    {
        super("zom", "Zombies", 2);
    }

    @Override
    public void roundStarted(TeamsManager manager)
    {
        infectionStarted = false;
    }

    @Override
    public void tick(TeamsManager manager)
    {
        int elapsed = manager.getRoundElapsedTicks();
        if (!infectionStarted && elapsed >= humanPrepTimeTicks)
        {
            infectionStarted = true;
            manager.broadcast(Component.literal("The zombie plague is here!"));
            infectRandomHuman(manager);
        }
        else if (infectionStarted && elapsed % 200 == 0 && manager.countPlayersOnRoundTeam(1) == 0)
        {
            infectRandomHuman(manager);
        }
    }

    private void infectRandomHuman(TeamsManager manager)
    {
        List<ServerPlayer> humans = manager.getPlayersOnRoundTeam(0);
        if (humans.isEmpty())
            return;
        ServerPlayer chosen = humans.get(manager.getRandom().nextInt(humans.size()));
        manager.selectTeam(chosen, manager.getCurrentRound().orElseThrow().getTeam(1), true);
        manager.respawnPlayer(chosen, true);
        manager.broadcast(Component.literal(chosen.getScoreboardName() + " was infected!"));
    }

    @Override
    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        super.playerKilled(manager, victim, source);
        if (infectionStarted)
        {
            Team zombies = manager.getCurrentRound().map(round -> round.getTeam(1)).orElse(null);
            if (zombies != null)
                manager.selectTeam(victim, zombies, true);
        }
    }

    @Override public boolean isFriendlyFireEnabled() { return friendlyFire; }
    @Override public boolean showZombieScore() { return true; }
    @Override public boolean canPlayerPickup(TeamsManager manager, ServerPlayer player, ItemStack stack) { return manager.getRoundTeamIndex(manager.getPlayerTeam(player)) != 1; }

    @Override
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        return manager.findSpawnPoint(player, manager.getPlayerTeam(player) == Team.SPECTATORS).orElse(null);
    }

    @Override
    public boolean hasWinner(TeamsManager manager, Team team)
    {
        int teamIndex = manager.getRoundTeamIndex(team);
        if (teamIndex == 0)
            return manager.getRoundTimeLeftTicks() <= 1 && manager.countPlayersOnRoundTeam(0) > 0;
        return teamIndex == 1 && infectionStarted && manager.countPlayersOnRoundTeam(0) == 0;
    }

    @Override
    public boolean setVariable(String variable, String value)
    {
        if ("humanpreptime".equalsIgnoreCase(variable))
        {
            humanPrepTimeTicks = Math.max(0, Integer.parseInt(value) * 20);
            return true;
        }
        if ("friendlyfire".equalsIgnoreCase(variable))
        {
            friendlyFire = Boolean.parseBoolean(value);
            return true;
        }
        return false;
    }

    @Override public void loadSettings(CompoundTag tag) { humanPrepTimeTicks = Math.max(0, tag.getInt("ZombiePrepTime")); friendlyFire = tag.getBoolean("ZombieFriendlyFire"); }
    @Override public void saveSettings(CompoundTag tag) { tag.putInt("ZombiePrepTime", humanPrepTimeTicks); tag.putBoolean("ZombieFriendlyFire", friendlyFire); }
}
