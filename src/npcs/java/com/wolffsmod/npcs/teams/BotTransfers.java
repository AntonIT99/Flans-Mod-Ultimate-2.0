package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Moves soldiers between battles and hands them out: bots of one skirmish go over to a side of another, NPCs made by hand
 * enlist in a skirmish their faction fights in, and either can be let go. A soldier that changes battle keeps its name,
 * looks and equipment and joins a squad of its own on its new side, which never brings it back once it falls. Vehicles stay
 * where they are. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotTransfers
{
    /**
     * Which soldiers of a battle a command means.
     *
     * @param faction
     *            only those fighting for this faction, or null for every side
     * @param near
     *            only those within a radius of here, or null for anywhere
     */
    record Selection(@Nullable Integer faction, @Nullable Vec3 near, double radius)
    {
        boolean selects(EntityNPCInterface npc)
        {
            boolean ofFaction = faction == null || npc.faction != null && npc.faction.id == faction;
            return ofFaction && (near == null || npc.position().distanceTo(near) <= radius);
        }
    }

    /**
     * Moves the selected living soldiers of one skirmish to a side of another.
     *
     * @return the soldiers moved
     */
    static int transfer(ServerLevel level, Skirmish from, Selection selection, Skirmish to, int side)
    {
        BotBattle source = Objects.requireNonNull(from.getBattle());
        // Each keeps the class it was made as, which decides its duties.
        Map<EntityNPCInterface, String> moved = new LinkedHashMap<>();
        for (EntityNPCInterface npc : soldiers(level, source, selection))
            moved.put(npc, BotBattles.slotOf(source, npc).map(BotSlot::getRole).orElse(""));
        moved.keySet().forEach(npc -> leave(source, npc));
        join(to, side, moved, false);
        return moved.size();
    }

    /**
     * Lets the selected living soldiers of a skirmish go: bots stay where they are as NPCs of their faction, no bots any
     * more; enlisted NPCs are given back as they were.
     *
     * @return the soldiers let go
     */
    static int release(ServerLevel level, Skirmish from, Selection selection)
    {
        BotBattle source = Objects.requireNonNull(from.getBattle());
        List<EntityNPCInterface> released = soldiers(level, source, selection);
        for (EntityNPCInterface npc : released)
        {
            leave(source, npc);
            Skirmishes.wake(npc);
            BotTag.clear(npc);
            npc.updateAI = true;
        }
        return released.size();
    }

    /**
     * Enlists the NPCs made by hand within a radius that belong to the faction of a side of a skirmish. Bots, vehicles and
     * dead NPCs are left out.
     *
     * @return the NPCs enlisted
     */
    static int enlist(ServerLevel level, Skirmish skirmish, int side, Vec3 at, double radius)
    {
        int faction = skirmish.getSides().get(side).faction();
        List<EntityNPCInterface> recruits = level.getEntitiesOfClass(EntityNPCInterface.class, new AABB(at, at).inflate(radius),
            npc -> npc.isAlive() && !npc.isKilled() && !BotTag.isBot(npc) && npc.faction != null && npc.faction.id == faction && !vehicle(npc) && npc.position().distanceTo(at) <= radius);
        Map<EntityNPCInterface, String> enlisted = new LinkedHashMap<>();
        recruits.forEach(npc -> enlisted.put(npc, ""));
        join(skirmish, side, enlisted, true);
        return recruits.size();
    }

    /** The living soldiers of a battle a selection means, vehicles and their crews left out. */
    private static List<EntityNPCInterface> soldiers(ServerLevel level, BotBattle battle, Selection selection)
    {
        List<EntityNPCInterface> soldiers = new ArrayList<>();
        for (BotSlot slot : battle.getSlots().values())
        {
            Entity entity = slot.getEntity() == null ? null : level.getEntity(slot.getEntity());
            if (!slot.isVehicle() && entity instanceof EntityNPCInterface npc && npc.isAlive() && !npc.isKilled() && selection.selects(npc))
                soldiers.add(npc);
        }
        return soldiers;
    }

    /** Takes a soldier off its battle, whose place then is retired and never filled again. */
    private static void leave(BotBattle battle, EntityNPCInterface npc)
    {
        Integer slotId = battle.getSlotOfEntity().remove(npc.getUUID());
        BotSlot slot = slotId == null ? null : battle.getSlots().get(slotId);
        if (slot == null)
            return;
        slot.setEntity(null);
        slot.setRetired(true);
    }

    /** Puts soldiers, each with its class, on a side of a skirmish, in a squad of their own, with the faction of the side. */
    private static void join(Skirmish skirmish, int side, Map<EntityNPCInterface, String> soldiers, boolean enlisted)
    {
        if (soldiers.isEmpty())
            return;
        BotBattle battle = Objects.requireNonNull(skirmish.getBattle());
        Skirmish.Side joined = skirmish.getSides().get(side);
        // The side's first team stands for the volunteers: it puts them on the side, and its force sets their rules.
        String team = joined.teams().isEmpty() ? "" : joined.teams().get(0);
        BotSquad squad = battle.newSquad(team, BotRole.INFANTRY, BotSlot.Source.VOLUNTEER, -1, "");
        for (Map.Entry<EntityNPCInterface, String> soldier : soldiers.entrySet())
        {
            EntityNPCInterface npc = soldier.getKey();
            BotSlot slot = battle.newSlot(team, BotSlot.Source.VOLUNTEER, -1);
            BotTag.Data before = BotTag.read(npc).orElse(null);
            slot.setName(npc.display.getName());
            slot.setRole(soldier.getValue());
            slot.setEntity(npc.getUUID());
            slot.setSpawns(1);
            slot.setLastSeen(npc.level().getGameTime());
            slot.setLastPosition(npc.position());
            slot.setSquad(squad.getId());
            squad.getSlots().add(slot.getId());
            battle.getSlotOfEntity().put(npc.getUUID(), slot.getId());
            npc.setFaction(joined.faction());
            BotTag.write(npc, new BotTag.Data(battle.getInstance(), slot.getId(), team, false, battle.getSettings().getNameplates(), false, enlisted || before != null && before.enlisted()));
            npc.updateAI = true;
        }
        battle.getSidesThatFought().add(side);
    }

    private static boolean vehicle(EntityNPCInterface npc)
    {
        return NpcVehicleState.of(npc).kind() != null;
    }
}
