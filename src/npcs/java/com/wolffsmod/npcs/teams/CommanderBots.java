package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.names.NpcDisplayNames;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Optional physical commanders, rear-area orders and bot leadership titles. Server thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CommanderBots
{
    private static final String TITLE_BACKUP = "WolffsModLeadershipTitle";
    private static final double REAR_DISTANCE = 48D;

    static boolean configured(BotBattle battle, BotSlot slot)
    {
        return battle.getSettings().isEnabled() && battle.getSettings().isCommanderBot()
            && battle.team(slot.getTeam()).map(team -> team.getClasses().stream().anyMatch(playerClass -> playerClass.getShortName().equals(slot.getRole()))).orElse(false)
            && battle.getSettings().team(slot.getTeam()).getCommanderClass().equals(slot.getRole());
    }

    static boolean canSpawn(BotBattle battle, BotSlot slot)
    {
        return configured(battle, slot) && BotPopulation.canAfford(battle, slot);
    }

    static void fallen(BotBattle battle, BotSlot slot, long now)
    {
        if (slot.getSource() == BotSlot.Source.COMMANDER && !slot.isRetired() && battle.getSettings().isCommanderBot())
            battle.getDisruptedUntil().merge(battle.sideOf(slot.getTeam()), now + battle.getSettings().getCommanderDisruptionSeconds() * 20L, Math::max);
    }

    static boolean disrupted(BotBattle battle, int side, long now)
    {
        return battle.getSettings().isCommanderBot() && now < battle.getDisruptedUntil().getOrDefault(side, 0L);
    }

    /** During disruption, unordered squads wait where their first living member stands. */
    static void hold(ServerLevel level, BotBattle battle, int side)
    {
        for (BotSquad squad : battle.getSquads().values())
            if (battle.sideOf(squad.getTeam()) == side && squad.getOrder() == null)
                BotCommander.livingMembers(level, battle, squad).stream().findFirst().ifPresent(member -> squad.setTask(new SquadTask(SquadTask.Kind.HOLD, member.position(), 8D)));
    }

    /** One extra commander per side, from its first team with a valid configured class. */
    static void keep(BotBattle battle)
    {
        Set<Integer> kept = new HashSet<>();
        for (BotSlot slot : battle.getSlots().values())
        {
            if (slot.getSource() != BotSlot.Source.COMMANDER || slot.isRetired())
                continue;
            if (!configured(battle, slot) || !kept.add(battle.sideOf(slot.getTeam())))
                slot.setRetired(true);
        }
        if (!battle.getSettings().isCommanderBot())
        {
            battle.getDisruptedUntil().clear();
            return;
        }
        for (ITeam team : battle.getGround().teams())
        {
            int side = battle.getGround().sideOf(team);
            String playerClass = battle.getSettings().team(team.getShortName()).getCommanderClass();
            if (kept.contains(side) || team.getClasses().stream().noneMatch(candidate -> candidate.getShortName().equals(playerClass)))
                continue;
            BotSlot slot = battle.newSlot(team.getShortName(), BotSlot.Source.COMMANDER, -1);
            slot.setRole(playerClass);
            kept.add(side);
        }
    }

    /** Stay behind the nearest friendly squad objective, towards home; never receive an assault task. */
    static BotCommander.Order order(BotBattle battle, BotSlot slot, EntityNPCInterface npc)
    {
        List<Vec3> spawns = battle.team(slot.getTeam()).map(battle.getGround()::spawnPositions).orElse(List.of());
        Vec3 home = spawns.stream().min(Comparator.comparingDouble(point -> point.distanceToSqr(npc.position()))).orElse(npc.position());
        int side = battle.sideOf(slot.getTeam());
        Vec3 front = battle.getGround().front()
            .orElseGet(() -> battle.getSquads().values().stream().filter(squad -> !squad.isDisbanded() && battle.sideOf(squad.getTeam()) == side && squad.getTask() != null)
                .map(squad -> squad.getTask().target()).min(Comparator.comparingDouble(point -> point.distanceToSqr(home))).orElse(home));
        Vec3 rear = rear(home, front);
        if (npc.getTarget() != null && npc.distanceToSqr(npc.getTarget()) < 24D * 24D)
            rear = home;
        Vec3 destination = BotSpawnPlacement.soldier((ServerLevel) npc.level(), rear).orElse(home);
        return new BotCommander.Order(destination, 5D, BotRole.INFANTRY, SquadTask.Kind.HOLD, destination, 10D);
    }

    static Vec3 rear(Vec3 home, Vec3 front)
    {
        Vec3 direction = front.subtract(home).multiply(1D, 0D, 1D);
        double distance = direction.length();
        return home.add(direction.normalize().scale(Math.max(0D, distance - REAR_DISTANCE)));
    }

    /** First living infantry member leads until a player takes over; restore clone titles when leadership changes. */
    static void titles(ServerLevel level, BotBattle battle)
    {
        Set<UUID> leaders = new HashSet<>();
        for (BotSquad squad : battle.getSquads().values())
            if (!squad.isDisbanded() && squad.getLeader() == null && squad.getRole() == BotRole.INFANTRY)
                BotCommander.livingMembers(level, battle, squad).stream().findFirst().ifPresent(member -> leaders.add(member.getUUID()));
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (!(entity instanceof EntityNPCInterface npc) || !npc.isAlive() || npc.isKilled())
                continue;
            String title = leaders.contains(npc.getUUID()) ? "Squad Leader" : "";
            if (slot.getSource() == BotSlot.Source.COMMANDER)
                title = "Commander";
            title(npc, title);
        }
    }

    private static void title(EntityNPCInterface npc, String title)
    {
        var data = npc.getPersistentData();
        if (!title.isEmpty())
        {
            if (!data.contains(TITLE_BACKUP))
            {
                CompoundTag backup = new CompoundTag();
                backup.putString("title", npc.display.getTitle());
                NpcDisplayNames.of(npc.display).save(backup, npc.display.getName(), npc.display.getTitle());
                data.put(TITLE_BACKUP, backup);
            }
            if (!title.equals(npc.display.getTitle()))
            {
                NpcDisplayNames.literalTitle(npc.display, title);
                npc.updateClient = true;
            }
        }
        else if (data.contains(TITLE_BACKUP))
        {
            CompoundTag backup = data.getCompound(TITLE_BACKUP);
            npc.display.setTitle(backup.getString("title"));
            NpcDisplayNames.of(npc.display).load(backup, npc.display.getName(), npc.display.getTitle());
            data.remove(TITLE_BACKUP);
            npc.updateClient = true;
        }
    }
}
