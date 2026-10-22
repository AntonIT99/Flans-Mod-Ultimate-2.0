package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.*;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The battle of a skirmish: its sides are managed factions, which fight the sides they are at war with; players fight for
 * the faction they belong to and NPCs for theirs; bots come in within their side's zone; there is no map, objective or
 * flag. Server thread.
 */
final class SkirmishBattleground implements Battleground
{
    /** Spawn points spread over a side's zone. */
    private static final int SPAWN_POINTS = 12;

    private final Skirmish skirmish;
    private final MinecraftServer server;
    private final List<ITeam> teams;
    /** The spawn points of each side, found on the ground when first needed. */
    private final Map<Integer, List<Vec3>> spawns = new HashMap<>();

    SkirmishBattleground(Skirmish skirmish, MinecraftServer server)
    {
        this.skirmish = skirmish;
        this.server = server;
        this.teams = skirmish.getSides().stream().flatMap(side -> side.teams().stream()).map(FlansModApi::getTeam).flatMap(Optional::stream).toList();
    }

    @Override
    public ResourceKey<Level> dimension()
    {
        return skirmish.getDimension();
    }

    @Override
    public List<ITeam> teams()
    {
        return teams;
    }

    @Override
    public int sideCount()
    {
        return skirmish.getSides().size();
    }

    @Override
    public int sideOf(ITeam team)
    {
        return skirmish.sideOfTeam(team.getShortName());
    }

    /** A player fights for the managed faction they belong to, an NPC for its faction. */
    @Override
    public int sideOf(Entity entity)
    {
        if (entity instanceof Player player)
            return BattleFactions.get(server).factionOf(player.getUUID()).map(skirmish::sideOf).orElse(-1);
        if (entity instanceof EntityNPCInterface npc && npc.faction != null)
            return skirmish.sideOf(npc.faction.id);
        return -1;
    }

    @Override
    public List<ITeam> teamsOfSide(int side)
    {
        return teams.stream().filter(team -> sideOf(team) == side).toList();
    }

    /** Sides fight each other while their factions are at war, whichever of the two declared it. */
    @Override
    public boolean hostile(int side, int other)
    {
        if (side == other || side < 0 || other < 0 || side >= sideCount() || other >= sideCount())
            return false;
        int faction = skirmish.getSides().get(side).faction();
        int enemy = skirmish.getSides().get(other).faction();
        return BattleFactions.attacks(faction, enemy) || BattleFactions.attacks(enemy, faction);
    }

    @Override
    public boolean areEnemies(Entity first, Entity second)
    {
        return !BattleFactions.atRest() && hostile(sideOf(first), sideOf(second));
    }

    @Override
    public OptionalInt attackingSide()
    {
        return OptionalInt.empty();
    }

    @Override
    public boolean mayCapture(int side)
    {
        return false;
    }

    @Override
    public List<TeamsCapturePoint> capturePoints()
    {
        return List.of();
    }

    @Override
    public List<TeamsFlag> flags()
    {
        return List.of();
    }

    @Override
    public List<Vec3> spawnPositions(ITeam team)
    {
        int side = sideOf(team);
        if (side < 0)
            return List.of();
        return spawns.computeIfAbsent(side, this::zone);
    }

    @Override
    public List<TeamsSpawner> spawners()
    {
        return List.of();
    }

    @Override
    public Optional<String> mapId()
    {
        return skirmish.getMarkerMap().isEmpty() ? Optional.empty() : Optional.of(skirmish.getMarkerMap());
    }

    @Override
    public Optional<ITeamsLiveRound> round()
    {
        return Optional.empty();
    }

    @Override
    public Optional<Vec3> front()
    {
        return Optional.ofNullable(skirmish.getFront());
    }

    /** The middle of the sides' zones. */
    @Override
    public Optional<Vec3> centre()
    {
        List<Skirmish.Side> sides = skirmish.getSides();
        return sides.isEmpty() ? Optional.empty() : Optional.of(sides.stream().map(Skirmish.Side::anchor).reduce(Vec3.ZERO, Vec3::add).scale(1D / sides.size()));
    }

    /** Forgets where the sides came in, after their zones moved; the spawn points are found again when next needed. */
    void forgetZones()
    {
        spawns.clear();
    }

    /** Points spread evenly over a side's zone, on the ground. */
    private List<Vec3> zone(int side)
    {
        ServerLevel level = server.getLevel(skirmish.getDimension());
        if (level == null)
            return List.of();
        Skirmish.Side zone = skirmish.getSides().get(side);
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < SPAWN_POINTS; i++)
        {
            // A golden-angle spiral covers the disc evenly with any number of points.
            double distance = zone.radius() * Math.sqrt((i + 0.5D) / SPAWN_POINTS);
            double angle = i * 2.399963229728653D;
            points.add(ground(level, zone.anchor().x + distance * Math.cos(angle), zone.anchor().z + distance * Math.sin(angle)));
        }
        return points;
    }

    /** The top of the ground at a column, below leaves. */
    static Vec3 ground(ServerLevel level, double x, double z)
    {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return new Vec3(x, y, z);
    }
}
