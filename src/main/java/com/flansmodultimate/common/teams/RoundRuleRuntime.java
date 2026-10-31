package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.teams.ITeamsRespawnRule;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.Team;
import lombok.Getter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Enforcement of the opt-in rules for one playing of a round. Called only on the logical server. */
public final class RoundRuleRuntime
{
    private final TeamsManager manager;
    @Getter
    private final RoundVehicleLimits vehicles;
    private RoundRuleState state = new RoundRuleState(new RoundRules());
    private final Map<UUID, Vec3> stagingPositions = new HashMap<>();
    private boolean releasingRespawn;

    public RoundRuleRuntime(TeamsManager manager)
    {
        this.manager = manager;
        vehicles = new RoundVehicleLimits(manager);
    }

    public RoundRules rules()
    {
        return state.getRules();
    }

    public void start(TeamsRound round, RoundRules defaults)
    {
        state = new RoundRuleState(round.getRules().resolve(defaults));
        stagingPositions.clear();
        vehicles.reset();
        ServerLevel level = manager.getCurrentLevel();
        if (level != null)
            level.getAllEntities().forEach(vehicles::adopt);
    }

    public void clear()
    {
        state = new RoundRuleState(new RoundRules());
        stagingPositions.clear();
        vehicles.reset();
    }

    public boolean isStaging()
    {
        return manager.isRoundRunning() && state.isStaging();
    }

    public boolean isOvertime()
    {
        return manager.isRoundRunning() && state.isOvertime();
    }

    /** Advances the pre-match countdown only while its player-count and ready requirements are met. */
    public boolean tickWarmup()
    {
        if (!isStaging())
            return false;
        List<ServerPlayer> players = participants();
        boolean enough = players.size() >= rules().number(RoundRule.MINIMUM_PLAYERS);
        boolean ready = !rules().enabled(RoundRule.REQUIRE_READY)
            || manager.getCurrentRound().orElseThrow().getSideIds().stream().allMatch(side -> players.stream().anyMatch(player -> sideOf(player) == side && state.player(player.getUUID()).isReady()));
        if (!enough || !ready)
            state.setWarmupTicks(rules().number(RoundRule.WARMUP_SECONDS) * 20);
        else if (state.getWarmupTicks() > 0)
            state.setWarmupTicks(state.getWarmupTicks() - 1);
        else
        {
            state.setStaging(false);
            stagingPositions.clear();
            manager.broadcast(Component.translatable("message.flansmodultimate.teams.round_live"));
            return false;
        }
        String message = !enough ? "message.flansmodultimate.teams.waiting_players" : !ready ? "message.flansmodultimate.teams.waiting_ready" : "message.flansmodultimate.teams.warmup";
        players.forEach(player -> holdWarmupPlayer(player, message));
        return true;
    }

    private void holdWarmupPlayer(ServerPlayer player, String message)
    {
        if (!player.isSpectator())
        {
            Vec3 anchor = stagingPositions.computeIfAbsent(player.getUUID(), ignored -> player.position());
            if (player.level() == manager.getCurrentLevel() && anchor.distanceToSqr(player.position()) > 0.01D)
                player.teleportTo(anchor.x, anchor.y, anchor.z);
            player.setDeltaMovement(Vec3.ZERO);
        }
        if (manager.getServer().getTickCount() % 20 == 0)
            player.displayClientMessage(Component.translatable(message, (state.getWarmupTicks() + 19) / 20), true);
    }

    public boolean ready(ServerPlayer player, boolean ready)
    {
        if (!isStaging() || !rules().enabled(RoundRule.REQUIRE_READY) || sideOf(player) < TeamsRound.FIRST_TEAM_ID)
            return false;
        state.player(player.getUUID()).setReady(ready);
        manager.broadcast(Component.translatable(ready ? "message.flansmodultimate.teams.player_ready" : "message.flansmodultimate.teams.player_unready", player.getScoreboardName()));
        manager.saveSettings();
        return true;
    }

    public boolean isReady(ServerPlayer player)
    {
        return state.player(player.getUUID()).isReady();
    }

    public void died(ServerPlayer player)
    {
        if (TeamsCombatants.getPlayingTeam(manager, player) == null || player.isSpectator() || isStaging())
            return;
        RoundRuleState.Participant participant = state.player(player.getUUID());
        participant.setDeaths(participant.getDeaths() + 1);
        participant.setAwaitingRespawn(true);
        participant.setDeathTick(manager.getRoundElapsedTicks());
        participant.setPendingTick(-1);
        manager.saveSettings();
    }

    public boolean exhausted(ServerPlayer player)
    {
        return state.exhausted(player.getUUID());
    }

    /** Returns true when the spawn must be held. Every entry path, including team selection, obeys remaining lives. */
    public boolean holdRespawn(ServerPlayer player, ITeamsRespawnRule.Reason reason)
    {
        if (!manager.isRoundRunning() || manager.getRoundTeamIndex(manager.getPlayerTeam(player)) < 0)
            return false;
        RoundRuleState.Participant participant = state.player(player.getUUID());
        if (exhausted(player))
        {
            participant.setPendingTick(-1);
            hold(player);
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.teams.no_lives"));
            return true;
        }
        if (releasingRespawn)
            return false;
        if (participant.getPendingTick() >= 0)
        {
            hold(player);
            return true;
        }
        if ((reason == ITeamsRespawnRule.Reason.DIED || participant.isAwaitingRespawn()) && !isStaging() && rules().number(RoundRule.RESPAWN_WAVE_SECONDS) > 0)
        {
            int delay = manager.getCurrentGameType().map(type -> type.getDeployDelaySeconds(manager, teamIdOf(player))).orElse(0) * 20;
            int earliest = Math.max(manager.getRoundElapsedTicks() + 1, participant.getDeathTick() + delay);
            participant.setPendingTick(RoundRuleState.nextWaveTick(earliest, rules().number(RoundRule.RESPAWN_WAVE_SECONDS)));
            participant.setPendingReason(ITeamsRespawnRule.Reason.DIED);
            hold(player);
            manager.saveSettings();
            return true;
        }
        return false;
    }

    private static void hold(ServerPlayer player)
    {
        player.stopRiding();
        player.setCamera(player);
        player.setGameMode(GameType.SPECTATOR);
    }

    public void spawned(ServerPlayer player)
    {
        if (sideOf(player) < TeamsRound.FIRST_TEAM_ID || player.isSpectator())
            return;
        RoundRuleState.Participant participant = state.player(player.getUUID());
        if (participant.getSide() != sideOf(player))
        {
            participant.setSide(sideOf(player));
            participant.setSwitchTick(manager.getRoundElapsedTicks());
        }
        participant.setAwaitingRespawn(false);
        stagingPositions.remove(player.getUUID());
    }

    public void loggedIn(ServerPlayer player)
    {
        if (!manager.isRoundRunning())
            return;
        if (manager.getRoundTeamIndex(manager.getPlayerTeam(player)) >= 0
            && (exhausted(player) || state.player(player.getUUID()).getPendingTick() >= 0 || state.player(player.getUUID()).isAwaitingRespawn()))
            holdRespawn(player, ITeamsRespawnRule.Reason.DIED);
    }

    public void loggedOut(ServerPlayer player)
    {
        state.player(player.getUUID()).setReady(false);
        stagingPositions.remove(player.getUUID());
    }

    public boolean mayChooseTeam(ServerPlayer player, Team requested)
    {
        if (!manager.isRoundRunning() || requested == Team.SPECTATORS)
            return true;
        TeamsRound round = manager.getCurrentRound().orElseThrow();
        int side = round.getSideId(requested);
        RoundRuleState.Participant participant = state.player(player.getUUID());
        if (side == participant.getSide())
            return true;
        if (participant.getSide() >= TeamsRound.FIRST_TEAM_ID && manager.getRoundElapsedTicks() - participant.getSwitchTick() < rules().number(RoundRule.TEAM_SWITCH_SECONDS) * 20)
            return false;
        if (!rules().enabled(RoundRule.PREVENT_LEADING_SIDE_JOIN))
            return true;
        int score = manager.getTeamScore(requested);
        int lowest = round.getSideIds().stream().map(round::getSideLeader).filter(Objects::nonNull).mapToInt(manager::getTeamScore).min().orElse(score);
        if (score <= lowest)
            return true;
        long requestedCount = participants().stream().filter(other -> other != player && sideOf(other) == side).count();
        long smallest = round.getSideIds().stream().mapToLong(otherSide -> participants().stream().filter(other -> other != player && sideOf(other) == otherSide).count()).min().orElse(0);
        return requestedCount < smallest;
    }

    public void teamChosen(ServerPlayer player, Team team)
    {
        if (manager.getCurrentRound().map(round -> round.getSideId(team)).orElse(0) != state.player(player.getUUID()).getSide())
            state.player(player.getUUID()).setReady(false);
    }

    /** Processes shared respawn waves and the restrictions that need continuous server enforcement. */
    public void tick()
    {
        vehicles.tick();
        for (ServerPlayer player : manager.getServer().getPlayerList().getPlayers())
        {
            RoundRuleState.Participant participant = state.player(player.getUUID());
            if (participant.getPendingTick() >= 0 && participant.getPendingTick() <= manager.getRoundElapsedTicks() && player.isAlive())
            {
                participant.setPendingTick(-1);
                releasingRespawn = true;
                try
                {
                    manager.respawnPlayer(player, true, participant.getPendingReason());
                }
                finally
                {
                    releasingRespawn = false;
                }
                manager.syncPlayer(player, com.flansmodultimate.network.client.teams.PacketTeamsState.OpenScreen.NONE);
            }
            if (manager.getRoundTeamIndex(manager.getPlayerTeam(player)) >= 0 && !player.isSpectator())
            {
                enforceHomeEntry(player);
                enforceFlagCarrier(player);
            }
            enforceSpectator(player);
            if (participant.getPendingTick() >= 0 && manager.getServer().getTickCount() % 20 == 0)
                player.displayClientMessage(Component.translatable("message.flansmodultimate.teams.next_wave", (participant.getPendingTick() - manager.getRoundElapsedTicks() + 19) / 20), true);
        }
        // Addon combatants obey flag-carrier restrictions as well.
        ServerLevel level = manager.getCurrentLevel();
        if (level != null)
            manager.getFlags().stream().map(flag -> flag.getCarrierId() == null ? null : level.getEntity(flag.getCarrierId())).filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(carrier -> !(carrier instanceof Player)).forEach(this::enforceFlagCarrier);
    }

    /** A newly captured Assault stage restarts normal time without replenishing the round's overtime budget. */
    public void resumeClock()
    {
        state.setOvertime(false);
    }

    /** Overtime is continuous: it ends as soon as the live objective threat ends, or the cap expires. */
    public boolean extendAtTimeLimit()
    {
        int cap = rules().number(RoundRule.OVERTIME_SECONDS) * 20;
        if (cap <= 0 || state.getOvertimeTicks() >= cap || !objectiveThreat())
            return false;
        if (!state.isOvertime())
        {
            state.setOvertime(true);
            manager.broadcast(Component.translatable("message.flansmodultimate.teams.overtime"));
        }
        state.setOvertimeTicks(state.getOvertimeTicks() + 1);
        return true;
    }

    private boolean objectiveThreat()
    {
        if (manager.getCurrentGameType().orElse(null) instanceof GameTypeCTF)
            return manager.getFlags().stream().anyMatch(com.flansmodultimate.common.entity.Flag::hasCarrier);
        ServerLevel level = manager.getCurrentLevel();
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (level == null || round == null)
            return false;
        com.flansmodultimate.common.teams.GameType type = manager.getCurrentGameType().orElse(null);
        if (type == null)
            return false;
        List<ObjectiveSnapshot> objectives = type.createObjectives(manager);
        if (type instanceof GameTypeAssault)
        {
            int active = GameTypeAssault.activeObjectiveIndex(objectives.stream().map(ObjectiveSnapshot::state).toList(), type.getAttackingTeamId(round));
            return active < objectives.size() && objectiveThreat(objectives.get(active), level, round);
        }
        return objectives.stream().anyMatch(point -> objectiveThreat(point, level, round));
    }

    private boolean objectiveThreat(ObjectiveSnapshot point, ServerLevel level, TeamsRound round)
    {
        CaptureState capture = point.state();
        ITeamCapturePoint live = manager.getCapturePoints().stream().filter(candidate -> candidate.getObjectId().equals(point.id())).findFirst().orElse(null);
        if (live == null)
            return false;
        CaptureScan scan = CaptureScan.of(live, level, round);
        return scan.isContested() || capture.capturingTeamId() >= TeamsRound.FIRST_TEAM_ID && capture.progress() > 0F && capture.progress() < 1F && scan.count(capture.capturingTeamId()) > 0F;
    }

    /** Limited-life rounds can end early once all but one participating side are exhausted. */
    public Optional<Team> eliminationWinner()
    {
        if (rules().number(RoundRule.PLAYER_LIVES) <= 0 || state.isStaging() || manager.getCurrentGameType().map(type -> !type.isScoreboardSortedByTeam()).orElse(true))
            return Optional.empty();
        long fieldedSides = state.getParticipants().values().stream().map(RoundRuleState.Participant::getSide).filter(side -> side >= TeamsRound.FIRST_TEAM_ID).distinct().count();
        if (fieldedSides < 2)
            return Optional.empty();
        List<Integer> surviving = participants().stream().filter(player -> !exhausted(player)).map(this::sideOf).distinct().toList();
        return surviving.size() == 1 ? Optional.ofNullable(manager.getCurrentRound().orElseThrow().getSideLeader(surviving.get(0))) : Optional.empty();
    }

    public boolean blocksCombat(Entity entity)
    {
        return isStaging() && (TeamsCombatants.getPlayingTeam(manager, entity) != null || entity instanceof Driveable && entity.level() == manager.getCurrentLevel());
    }

    public boolean spares(LivingEntity victim, DamageSource source)
    {
        if (!manager.isRoundRunning() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return false;
        LivingEntity attacker = TeamsCombatants.getCause(source);
        if (blocksCombat(victim) || attacker != null && blocksCombat(attacker))
            return true;
        if (!rules().enabled(RoundRule.HOME_BASE_NO_DAMAGE))
            return false;
        Team team = TeamsCombatants.getPlayingTeam(manager, victim);
        if (team == null)
            return false;
        if (homeBases().stream().anyMatch(base -> alliedHome(base, team) && insideHome(base, victim)))
            return true;
        Team attackerTeam = attacker == null ? null : TeamsCombatants.getPlayingTeam(manager, attacker);
        return attackerTeam != null && homeBases().stream().anyMatch(base -> alliedHome(base, attackerTeam) && insideHome(base, attacker));
    }

    private List<ITeamBase> homeBases()
    {
        TeamsRound round = manager.getCurrentRound().orElse(null);
        if (round == null)
            return List.of();
        return manager.getMap(round.getMapId()).stream().flatMap(map -> map.getBases().stream()).map(manager::getBase).flatMap(Optional::stream)
            .filter(base -> !(base instanceof ITeamCapturePoint) && base.getDefaultOwnerId() >= TeamsRound.FIRST_TEAM_ID).toList();
    }

    private boolean alliedHome(ITeamBase base, Team team)
    {
        TeamsRound round = manager.getCurrentRound().orElseThrow();
        return round.getSideId(base.getDefaultOwnerId()) == round.getSideId(team);
    }

    private boolean insideHome(ITeamBase base, Entity entity)
    {
        int radius = rules().number(RoundRule.HOME_BASE_RADIUS);
        Vec3 centre = base.getTeamObjectPosition();
        double dx = centre.x - entity.getX();
        double dz = centre.z - entity.getZ();
        return base.getDimension().equals(entity.level().dimension()) && dx * dx + dz * dz < (double) radius * radius && Math.abs(centre.y - entity.getY()) < radius;
    }

    public void enforceHomeEntry(LivingEntity fighter)
    {
        if (!manager.isRoundRunning() || !rules().enabled(RoundRule.HOME_BASE_NO_ENTRY) || fighter.isSpectator())
            return;
        Team team = TeamsCombatants.getPlayingTeam(manager, fighter);
        if (team == null)
            return;
        ITeamBase enemyHome = homeBases().stream().filter(base -> !alliedHome(base, team) && insideHome(base, fighter)).findFirst().orElse(null);
        if (enemyHome == null)
            return;
        int teamId = manager.getCurrentRound().orElseThrow().getTeamId(team);
        Vec3 centre = enemyHome.getTeamObjectPosition();
        Vec3 outward = new Vec3(fighter.getX() - centre.x, 0D, fighter.getZ() - centre.z);
        if (outward.lengthSqr() < 0.01D)
            outward = new Vec3(1D, 0D, 0D);
        Vec3 outside = centre.add(outward.normalize().scale(rules().number(RoundRule.HOME_BASE_RADIUS) + 1D));
        Vec3 destination = manager.findHeadquartersSpawn(teamId).orElse(new Vec3(outside.x, fighter.getY(), outside.z));
        fighter.stopRiding();
        fighter.teleportTo(destination.x, destination.y, destination.z);
        if (fighter instanceof ServerPlayer player)
            player.displayClientMessage(Component.translatable("message.flansmodultimate.teams.enemy_home"), true);
    }

    public boolean mayMount(Entity rider)
    {
        return !blocksCombat(rider) && (!manager.isRoundRunning() || !rules().enabled(RoundRule.FLAG_NO_VEHICLES) || manager.getFlagCarriedBy(rider) == null);
    }

    @SuppressWarnings("deprecation") // Forge 1.20.1 retains the vanilla built-in item registry for item-id lookup.
    public boolean blocksItemUse(LivingEntity fighter, ItemStack stack)
    {
        if (!manager.isRoundRunning() || manager.getFlagCarriedBy(fighter) == null || stack.isEmpty())
            return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return Arrays.asList(rules().get(RoundRule.FLAG_BANNED_ITEMS).split(",")).contains(id);
    }

    public boolean mayCarryFlag(LivingEntity fighter)
    {
        return !blocksCombat(fighter) && !fighter.isSpectator() && (!rules().enabled(RoundRule.FLAG_NO_VEHICLES) || !fighter.isPassenger())
            && (!rules().enabled(RoundRule.FLAG_NO_FLIGHT) || !fighter.isFallFlying() && !(fighter instanceof Player player && player.getAbilities().flying));
    }

    private void enforceFlagCarrier(LivingEntity fighter)
    {
        var carried = manager.getFlagCarriedBy(fighter);
        if (carried == null)
            return;
        if (rules().enabled(RoundRule.FLAG_NO_VEHICLES) && fighter.isPassenger())
            fighter.stopRiding();
        if (rules().enabled(RoundRule.FLAG_NO_FLIGHT))
        {
            if (fighter instanceof Player player && player.isFallFlying())
                player.stopFallFlying();
            else if (fighter.isFallFlying())
                carried.drop(30 * 20);
            if (fighter instanceof ServerPlayer player && player.getAbilities().flying)
            {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        if (blocksItemUse(fighter, fighter.getUseItem()))
            fighter.stopUsingItem();
    }

    /** Restricted spectators may follow a living ally; otherwise they remain at their headquarters. Operators are exempt. */
    public boolean restrictedSpectator(ServerPlayer player)
    {
        return manager.isRoundRunning() && rules().enabled(RoundRule.RESTRICT_SPECTATORS) && player.isSpectator() && !player.hasPermissions(2);
    }

    public boolean maySpectate(ServerPlayer player, Entity target)
    {
        if (!restrictedSpectator(player) || target == player)
            return true;
        Team team = TeamsCombatants.getPlayingTeam(manager, target);
        return target.isAlive() && !target.isSpectator() && target.level() == manager.getCurrentLevel() && team != null && manager.areAllied(manager.getPlayerTeam(player), team);
    }

    private void enforceSpectator(ServerPlayer player)
    {
        if (!restrictedSpectator(player))
            return;
        Entity camera = player.getCamera();
        if (camera != player && maySpectate(player, camera))
            return;
        Entity ally = participants().stream().filter(other -> other != player && !other.isSpectator() && maySpectate(player, other)).findFirst().orElse(null);
        player.setCamera(ally == null ? player : ally);
        if (ally == null)
        {
            ServerLevel level = manager.getCurrentLevel();
            Vec3 anchor = manager.findHeadquartersSpawn(teamIdOf(player)).orElseGet(() -> level == null ? player.position() : Vec3.atCenterOf(level.getSharedSpawnPos()));
            if (level != null && (player.level() != level || player.position().distanceToSqr(anchor) > 0.01D))
                player.teleportTo(level, anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        }
    }

    private int teamIdOf(ServerPlayer player)
    {
        return manager.getCurrentRound().map(round -> round.getTeamId(manager.getPlayerTeam(player))).orElse(0);
    }

    private int sideOf(ServerPlayer player)
    {
        return manager.getCurrentRound().map(round -> round.getSideId(PlayerData.getInstance(player).getNewTeam())).orElse(0);
    }

    private List<ServerPlayer> participants()
    {
        return manager.getServer().getPlayerList().getPlayers().stream().filter(player -> sideOf(player) >= TeamsRound.FIRST_TEAM_ID).toList();
    }

    /** Resolves a seat to its parent so all seats of a driveable share one vehicle slot. */
    public static Entity vehicle(Entity mounted)
    {
        return mounted instanceof Seat seat ? seat.getDriveable() : mounted;
    }

    public CompoundTag save()
    {
        CompoundTag tag = state.save();
        tag.put("vehicles", vehicles.save());
        return tag;
    }

    public void load(CompoundTag tag)
    {
        clear();
        if (!tag.isEmpty())
        {
            state = RoundRuleState.load(tag);
            vehicles.load(tag.getCompound("vehicles"));
        }
    }
}
