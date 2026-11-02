package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.teams.NpcDuties.Fighter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * What soldier bots do for their side besides fighting, once a second, as {@link NpcDuties} carries it out: the duty of a
 * bot is its class's role, its allies and enemies are the players and bots of the battle's sides, and a badly hurt bot falls
 * back to a medic or its spawn below the health its battle's settings name. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotDuties
{
    /** @return where a bot goes for its duty, ahead of its squad's task, or empty while it has none */
    static Optional<Vec3> destination(Entity npc)
    {
        return NpcDuties.destination(npc);
    }

    static void forget()
    {
        NpcDuties.forget();
    }

    static void tick(MinecraftServer server, ServerLevel level, BotBattle battle)
    {
        long now = level.getGameTime();
        List<Fighter> fighters = new ArrayList<>();
        Map<Fighter, BotSlot> soldiers = new LinkedHashMap<>();
        fighters(server, level, battle, fighters, soldiers);
        int threshold = battle.getSettings().getRetreatHealth();
        soldiers.forEach((fighter, slot) ->
        {
            EntityNPCInterface npc = (EntityNPCInterface) fighter.entity();
            NpcDuties.clearDestination(npc);
            if (!NpcDuties.retreats(npc, fighter, fighters, now, threshold, () -> spawnRefuge(battle, npc, slot)) && slot.getSource() != BotSlot.Source.COMMANDER)
                NpcDuties.serve(level, npc, fighter, fighters, now, rider -> friendly(battle, fighter, rider), entity -> BotTargeting.armoured(battle, entity));
        });
    }

    /** Everyone alive who fights in the battle in the level, its players and its bots, and among them the soldier bots with their slots. */
    private static void fighters(MinecraftServer server, ServerLevel level, BotBattle battle, List<Fighter> fighters, Map<Fighter, BotSlot> soldiers)
    {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (player.isAlive() && !player.isSpectator() && player.level() == level)
                battle.sideOfPlayer(player).ifPresent(side -> fighters.add(new Fighter(player, side, null, false, 0)));
        }
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (entity instanceof LivingEntity living && living.isAlive() && slot.getDiedAt() == 0)
                add(battle, slot, living, fighters, soldiers);
        }
    }

    private static void add(BotBattle battle, BotSlot slot, LivingEntity living, List<Fighter> fighters, Map<Fighter, BotSlot> soldiers)
    {
        int side = battle.sideOf(slot.getTeam());
        if (slot.isVehicle())
        {
            fighters.add(new Fighter(living, side, null, true, 0));
            return;
        }
        Fighter fighter = new Fighter(living, side, BotRoles.of(battle.getSettings().team(slot.getTeam()), slot.getRole()), false, BotRoles.kit(slot.getRole()).healAmount());
        fighters.add(fighter);
        if (living instanceof EntityNPCInterface)
            soldiers.put(fighter, slot);
    }

    /** Whether someone riding a driveable is on the bot's side, or on no side of the battle at all. */
    private static boolean friendly(BotBattle battle, Fighter self, Entity rider)
    {
        return FlansTeams.getTeam(rider).map(team -> battle.getGround().sideOf(team) == self.side()).orElse(true);
    }

    @Nullable
    private static Vec3 spawnRefuge(BotBattle battle, Entity npc, BotSlot slot)
    {
        Optional<ITeam> team = battle.team(slot.getTeam());
        return team.flatMap(found -> battle.getGround().spawnPositions(found).stream().min(Comparator.comparingDouble(spawn -> spawn.distanceToSqr(npc.position())))).orElse(null);
    }
}
