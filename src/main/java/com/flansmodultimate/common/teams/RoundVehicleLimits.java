package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.*;

/** Side-wide vehicle slots. Unloaded vehicles retain their slot; destroyed vehicles retain it only for the cooldown. */
public final class RoundVehicleLimits
{
    private static final String ROUND = "round";
    private static final String STAMP = "flansmodultimate_round_vehicle";
    private static final String SPAWNER = "teams_spawner";

    private record Slot(int side, RoundRule category, int releasedAt)
    {}

    private final TeamsManager manager;
    private final Map<UUID, Slot> slots = new HashMap<>();
    private final Map<UUID, Entity> loaded = new HashMap<>();

    public RoundVehicleLimits(TeamsManager manager)
    {
        this.manager = manager;
    }

    public void reset()
    {
        slots.clear();
        loaded.clear();
    }

    private RoundRules rules()
    {
        return manager.getRoundRuleRuntime().rules();
    }

    /** Reserves a slot before insertion, so simultaneous spawners cannot exceed a side's cap. */
    public boolean prepare(Entity entity, Team team)
    {
        if (!manager.isRoundRunning() || team == null || manager.getRoundTeamIndex(team) < 0 || entity.level() != manager.getCurrentLevel())
            return true;
        UUID instance = manager.getRoundInstanceId();
        if (instance == null)
            return true;
        RoundRule category = category(entity);
        if (category == null)
            return true;
        CompoundTag stamp = entity.getPersistentData().getCompound(STAMP);
        if (stamp.hasUUID(ROUND) && stamp.getUUID(ROUND).equals(manager.getRoundInstanceId()))
            return true;
        int side = manager.getCurrentRound().orElseThrow().getSideId(team);
        int limit = rules().number(category);
        int delay = rules().number(RoundRule.VEHICLE_REPLACEMENT_SECONDS) * 20;
        int now = manager.getRoundElapsedTicks();
        long occupied = slots.values().stream().filter(slot -> slot.side == side && slot.category == category).filter(slot -> slot.releasedAt < 0 || now - slot.releasedAt < delay).count();
        boolean cooling = slots.values().stream().anyMatch(slot -> slot.side == side && slot.category == category && slot.releasedAt >= 0 && now - slot.releasedAt < delay);
        if (limit >= 0 && occupied >= limit || limit < 0 && cooling)
            return false;
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ROUND, instance);
        tag.putInt("side", side);
        entity.getPersistentData().put(STAMP, tag);
        slots.put(entity.getUUID(), new Slot(side, category, -1));
        loaded.put(entity.getUUID(), entity);
        return true;
    }

    /** Releases a reservation when placement failed before the entity ever entered the world. */
    public void placementFailed(Entity entity)
    {
        slots.remove(entity.getUUID());
        loaded.remove(entity.getUUID());
        entity.getPersistentData().remove(STAMP);
    }

    public void joined(Entity entity)
    {
        RoundRule category = category(entity);
        if (!manager.isRoundRunning() || category == null || entity.level() != manager.getCurrentLevel())
            return;
        CompoundTag tag = entity.getPersistentData().getCompound(STAMP);
        if (tag.hasUUID(ROUND) && tag.getUUID(ROUND).equals(manager.getRoundInstanceId()))
        {
            slots.putIfAbsent(entity.getUUID(), new Slot(tag.getInt("side"), category, -1));
            loaded.put(entity.getUUID(), entity);
        }
    }

    /** A newly started round adopts its map's existing vehicles when their spawner identifies a side. */
    public void adopt(Entity entity)
    {
        if (category(entity) == null || entity instanceof Driveable driveable && driveable.isWreck())
            return;
        UUID instance = manager.getRoundInstanceId();
        if (instance == null)
            return;
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(SPAWNER))
            return;
        manager.getObject(tag.getUUID(SPAWNER)).filter(TeamSpawnerBlockEntity.class::isInstance).map(TeamSpawnerBlockEntity.class::cast).ifPresent(spawner ->
        {
            // Existing vehicles are retained even if an operator configured a lower cap for this round.
            Team team = spawner.getOwningTeam();
            if (team == null || manager.getRoundTeamIndex(team) < 0)
                return;
            CompoundTag stamp = new CompoundTag();
            int side = manager.getCurrentRound().orElseThrow().getSideId(team);
            stamp.putUUID(ROUND, instance);
            stamp.putInt("side", side);
            entity.getPersistentData().put(STAMP, stamp);
            slots.put(entity.getUUID(), new Slot(side, category(entity), -1));
            loaded.put(entity.getUUID(), entity);
        });
    }

    public void left(Entity entity)
    {
        loaded.remove(entity.getUUID());
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.shouldDestroy())
            release(entity.getUUID());
    }

    public void tick()
    {
        for (Entity entity : List.copyOf(loaded.values()))
        {
            if (entity instanceof Driveable driveable && driveable.isWreck() || entity.isRemoved() && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy())
            {
                release(entity.getUUID());
                loaded.remove(entity.getUUID());
            }
        }
        int delay = rules().number(RoundRule.VEHICLE_REPLACEMENT_SECONDS) * 20;
        slots.values().removeIf(slot -> slot.releasedAt >= 0 && manager.getRoundElapsedTicks() - slot.releasedAt >= delay);
    }

    private void release(UUID id)
    {
        slots.computeIfPresent(id, (ignored, slot) -> slot.releasedAt < 0 ? new Slot(slot.side, slot.category, manager.getRoundElapsedTicks()) : slot);
    }

    private static RoundRule category(Entity entity)
    {
        if (entity instanceof AAGun)
            return RoundRule.AA_LIMIT;
        if (!(entity instanceof Driveable driveable) || driveable.getConfigType() == null)
            return null;
        DriveableType type = driveable.getConfigType();
        if (type.isShip())
            return RoundRule.SHIP_LIMIT;
        if (type instanceof PlaneType)
            return RoundRule.AIRCRAFT_LIMIT;
        if (type instanceof MechaType)
            return RoundRule.MECHA_LIMIT;
        return type instanceof VehicleType vehicle && vehicle.isTank() ? RoundRule.TANK_LIMIT : RoundRule.VEHICLE_LIMIT;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        slots.forEach((id, slot) ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt("side", slot.side);
            entry.putString("category", slot.category.getKey());
            entry.putInt("released_at", slot.releasedAt);
            tag.put(id.toString(), entry);
        });
        return tag;
    }

    public void load(CompoundTag tag)
    {
        reset();
        for (String key : tag.getAllKeys())
        {
            if (!tag.contains(key, Tag.TAG_COMPOUND))
                continue;
            try
            {
                CompoundTag entry = tag.getCompound(key);
                UUID id = UUID.fromString(key);
                RoundRule.find(entry.getString("category")).filter(rule -> rule.name().endsWith("_LIMIT"))
                    .ifPresent(rule -> slots.put(id, new Slot(entry.getInt("side"), rule, entry.getInt("released_at"))));
            }
            catch (IllegalArgumentException ignored)
            {
                // Invalid saved reservations do not create permanent phantom slots.
            }
        }
    }
}
