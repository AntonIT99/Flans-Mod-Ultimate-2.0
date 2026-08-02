package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Server-side ruleset contract for a Teams round. */
public abstract class GameType
{
    private static final Map<String, GameType> TYPES = new LinkedHashMap<>();

    private final String id;
    private final String name;
    private final int requiredTeams;

    protected GameType(String id, String name, int requiredTeams)
    {
        this.id = id.toLowerCase(Locale.ROOT);
        this.name = name;
        this.requiredTeams = requiredTeams;
        if (TYPES.putIfAbsent(this.id, this) != null)
            throw new IllegalStateException("Duplicate game type: " + id);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getRequiredTeams() { return requiredTeams; }

    public void roundStarted(TeamsManager manager) {}
    public void roundEnded(TeamsManager manager) {}
    public void tick(TeamsManager manager) {}

    public boolean canPlayerBeAttacked(ServerPlayer victim, ServerPlayer attacker)
    {
        TeamsManager manager = TeamsManager.getInstance();
        Team victimTeam = manager.getPlayerTeam(victim);
        Team attackerTeam = manager.getPlayerTeam(attacker);
        return victimTeam != Team.SPECTATORS && attackerTeam != null && victimTeam != null
            && (victimTeam != attackerTeam || isFriendlyFireEnabled());
    }

    public boolean playerAttacked(ServerPlayer player, DamageSource source)
    {
        ServerPlayer attacker = getPlayerFromDamageSource(source);
        return attacker == null ? getPlayerTeam(player) != Team.SPECTATORS : canPlayerBeAttacked(player, attacker);
    }

    public void playerKilled(TeamsManager manager, ServerPlayer victim, DamageSource source)
    {
        PlayerData victimData = PlayerData.getInstance(victim);
        victimData.setDeaths(victimData.getDeaths() + 1);
        manager.getStats(victim).recordDeath();

        ServerPlayer attacker = getPlayerFromDamageSource(source);
        if (attacker == null || attacker == victim)
        {
            victimData.setScore(victimData.getScore() - 1);
            return;
        }

        PlayerData attackerData = PlayerData.getInstance(attacker);
        attackerData.setScore(attackerData.getScore() + 1);
        attackerData.setKills(attackerData.getKills() + 1);
        PlayerStats attackerStats = manager.getStats(attacker);
        attackerStats.addExperience(manager.getStats(victim).getRank() * 2);
        attackerStats.recordKill(attacker.distanceTo(victim));
    }

    public void flagClicked(TeamsManager manager, ServerPlayer player, Flag flag) {}
    public void baseClicked(TeamsManager manager, ServerPlayer player, Flagpole base) {}
    public boolean canPlayerPickup(TeamsManager manager, ServerPlayer player, ItemStack stack) { return true; }

    @Nullable
    public Vec3 getSpawnPoint(TeamsManager manager, ServerPlayer player)
    {
        return manager.findSpawnPoint(player, false).orElse(null);
    }

    public boolean hasWinner(TeamsManager manager, Team team)
    {
        return manager.getTeamScore(team) >= manager.getCurrentRound().map(TeamsRound::getScoreLimit).orElse(Integer.MAX_VALUE);
    }

    public boolean isFriendlyFireEnabled() { return false; }
    public boolean isAutoBalanceEnabled() { return true; }
    public boolean isScoreboardSortedByTeam() { return true; }
    public boolean showZombieScore() { return false; }
    public boolean setVariable(String variable, String value) { return false; }
    public void loadSettings(CompoundTag tag) {}
    public void saveSettings(CompoundTag tag) {}

    @Nullable
    protected Team getPlayerTeam(ServerPlayer player)
    {
        return TeamsManager.getInstance().getPlayerTeam(player);
    }

    @Nullable
    protected static ServerPlayer getPlayerFromDamageSource(DamageSource source)
    {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player)
            return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player)
            return player;
        return source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player ? player : null;
    }

    public static void bootstrap()
    {
        GameTypes.bootstrap();
    }

    @Nullable
    public static GameType get(@Nullable String id)
    {
        bootstrap();
        return id == null ? null : TYPES.get(id.toLowerCase(Locale.ROOT));
    }

    public static Collection<GameType> values()
    {
        bootstrap();
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
