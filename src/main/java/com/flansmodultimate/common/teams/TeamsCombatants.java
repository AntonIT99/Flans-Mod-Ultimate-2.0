package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;
import java.util.UUID;

/**
 * Who fights in a Teams round: players, through their team selection, and combatants, the non-player living entities
 * an addon assigned to a team.
 *
 * <p>
 * A combatant carries its assignment in its persistent data, stamped with the playing of the round it was made in, so
 * it survives chunk unloading and server restarts and lapses by itself once that playing is over. Server thread.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsCombatants
{
    private static final String NBT_COMBATANT = "flansmodultimate_teams_combatant";
    private static final String NBT_TEAM = "team";
    private static final String NBT_ROUND_INSTANCE = "round_instance";
    private static final String NBT_CAPTURE_WEIGHT = "capture_weight";

    /** Makes the entity fight for the team in the round being played. Players are refused: they choose their team. */
    public static boolean assign(TeamsManager manager, LivingEntity entity, Team team, float captureWeight)
    {
        UUID instance = manager.getRoundInstanceId();
        if (entity instanceof Player || !manager.isRoundRunning() || instance == null || manager.getRoundTeamIndex(team) < 0 || !Float.isFinite(captureWeight) || captureWeight < 0F)
            return false;
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_TEAM, team.getShortName());
        tag.putUUID(NBT_ROUND_INSTANCE, instance);
        tag.putFloat(NBT_CAPTURE_WEIGHT, captureWeight);
        entity.getPersistentData().put(NBT_COMBATANT, tag);
        return true;
    }

    public static void release(Entity entity)
    {
        entity.getPersistentData().remove(NBT_COMBATANT);
    }

    /** The team of a combatant of the round being played, or null for any other entity, players included. */
    @Nullable
    public static Team getCombatantTeam(TeamsManager manager, Entity entity)
    {
        CompoundTag tag = combatantTag(manager, entity);
        return tag == null ? null : Team.getTeam(tag.getString(NBT_TEAM));
    }

    /**
     * The team an entity is on as the game type rules see it: a player's selection, spectators included and whether or
     * not a round runs, or a combatant's team. Null for everyone else.
     */
    @Nullable
    public static Team getTeam(Entity entity)
    {
        TeamsManager manager = TeamsManager.getInstance();
        return entity instanceof Player player ? manager.getPlayerTeam(player) : getCombatantTeam(manager, entity);
    }

    /** The team an entity plays for in the running round, or null for spectators, outsiders and between rounds. */
    @Nullable
    public static Team getPlayingTeam(TeamsManager manager, Entity entity)
    {
        if (!manager.isRoundRunning())
            return null;
        Team team = entity instanceof Player player ? manager.getPlayerTeam(player) : getCombatantTeam(manager, entity);
        return team != null && manager.getRoundTeamIndex(team) >= 0 ? team : null;
    }

    /** Whether the entity is a player or a combatant of the running round, either of which the game type rules judge. */
    public static boolean isParticipant(TeamsManager manager, Entity entity)
    {
        return entity instanceof ServerPlayer || combatantTag(manager, entity) != null;
    }

    /** How much the entity counts on a capture point: 1 for a living player of the round, a combatant's weight, otherwise 0. */
    public static float getCaptureWeight(TeamsManager manager, Entity entity)
    {
        if (!entity.isAlive() || entity.isSpectator())
            return 0F;
        if (entity instanceof Player)
            return getPlayingTeam(manager, entity) == null ? 0F : 1F;
        CompoundTag tag = combatantTag(manager, entity);
        return tag == null ? 0F : Math.max(0F, tag.getFloat(NBT_CAPTURE_WEIGHT));
    }

    /** Whether both entities play the running round on opposite sides. */
    public static boolean areEnemies(TeamsManager manager, Entity first, Entity second)
    {
        Team firstTeam = getPlayingTeam(manager, first);
        Team secondTeam = getPlayingTeam(manager, second);
        return firstTeam != null && secondTeam != null && !manager.areAllied(firstTeam, secondTeam);
    }

    /**
     * Whether the running round lets one fighter hurt another, as its game type rules: allies only with friendly fire on.
     * Empty unless both play the round, players or combatants.
     */
    public static Optional<Boolean> mayHarm(TeamsManager manager, LivingEntity victim, LivingEntity attacker)
    {
        if (getPlayingTeam(manager, victim) == null || getPlayingTeam(manager, attacker) == null)
            return Optional.empty();
        return manager.getCurrentGameType().map(type -> type.canBeAttacked(victim, attacker));
    }

    /**
     * The player or combatant held responsible for some damage: whoever caused it, or the owner of the projectile that
     * dealt it. Null when that is neither.
     */
    @Nullable
    public static LivingEntity getAttacker(DamageSource source)
    {
        LivingEntity cause = getCause(source);
        return cause != null && isParticipant(TeamsManager.getInstance(), cause) ? cause : null;
    }

    /** The living entity held responsible for some damage: whoever caused it, or the owner of the projectile that dealt it. */
    @Nullable
    public static LivingEntity getCause(DamageSource source)
    {
        Entity cause = source.getEntity();
        if (cause instanceof Projectile projectile)
            cause = projectile.getOwner();
        if (cause == null && source.getDirectEntity() instanceof Projectile projectile)
            cause = projectile.getOwner();
        return cause instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private static CompoundTag combatantTag(TeamsManager manager, Entity entity)
    {
        if (entity instanceof Player || !entity.getPersistentData().contains(NBT_COMBATANT, Tag.TAG_COMPOUND))
            return null;
        CompoundTag tag = entity.getPersistentData().getCompound(NBT_COMBATANT);
        UUID instance = manager.getRoundInstanceId();
        if (!manager.isRoundRunning() || instance == null || !tag.hasUUID(NBT_ROUND_INSTANCE) || !instance.equals(tag.getUUID(NBT_ROUND_INSTANCE)))
            return null;
        Team team = Team.getTeam(tag.getString(NBT_TEAM));
        return team != null && manager.getRoundTeamIndex(team) >= 0 ? tag : null;
    }
}
