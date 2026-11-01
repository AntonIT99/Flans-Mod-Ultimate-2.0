package com.flansmodultimate.common.teams;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Persistent last-known vehicle positions, retained across chunk unloading and server restarts. */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class SpawnerVehicles extends FlanSavedData
{
    public static final String SPAWNER = "teams_spawner";
    private final Map<UUID, CompoundTag> vehicles = new HashMap<>();

    public static SpawnerVehicles get(ServerLevel level)
    {
        return SavedDataPlatform.computeIfAbsent(level.getServer().overworld(), "flansmodultimate_spawner_vehicles", SpawnerVehicles::new, (tag, registries) ->
        {
            SpawnerVehicles result = new SpawnerVehicles();
            for (Tag entry : tag.getList("vehicles", Tag.TAG_COMPOUND))
            {
                CompoundTag vehicle = (CompoundTag) entry;
                if (vehicle.hasUUID("id"))
                    result.vehicles.put(vehicle.getUUID("id"), vehicle.copy());
            }
            return result;
        });
    }

    public static boolean operational(Entity entity)
    {
        return (entity.isAlive() || entity.getRemovalReason() != null && !entity.getRemovalReason().shouldDestroy()) && !(entity instanceof Driveable driveable && driveable.isWreck());
    }

    public void observe(Entity entity)
    {
        if (!(entity instanceof Driveable || entity instanceof AAGun) || !entity.getPersistentData().hasUUID(SPAWNER))
            return;
        if (!operational(entity))
            vehicles.remove(entity.getUUID());
        else
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", entity.getUUID());
            entry.putUUID(SPAWNER, entity.getPersistentData().getUUID(SPAWNER));
            entry.putString("dimension", entity.level().dimension().location().toString());
            entry.putDouble("x", entity.getX());
            entry.putDouble("y", entity.getY());
            entry.putDouble("z", entity.getZ());
            vehicles.put(entity.getUUID(), entry);
        }
        setDirty();
    }

    @SubscribeEvent
    public static void joining(EntityJoinLevelEvent event)
    {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity().getPersistentData().hasUUID(SPAWNER))
            get(level).observe(event.getEntity());
    }

    @SubscribeEvent
    public static void leaving(EntityLeaveLevelEvent event)
    {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !(entity instanceof Driveable || entity instanceof AAGun) || !entity.getPersistentData().hasUUID(SPAWNER))
            return;
        SpawnerVehicles data = get(level);
        // Chunk unload keeps the last position. Killed/discarded vehicles no longer block replacements.
        if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy())
        {
            data.vehicles.remove(entity.getUUID());
            data.setDirty();
        }
        else
            data.observe(entity);
    }

    public boolean exists(ServerLevel level, UUID spawner, TeamsMap map)
    {
        // Refresh loaded vehicles, including legacy entities that predate this registry.
        for (ServerLevel dimension : level.getServer().getAllLevels())
            for (Entity entity : dimension.getAllEntities())
                if (entity.getPersistentData().hasUUID(SPAWNER) && spawner.equals(entity.getPersistentData().getUUID(SPAWNER)))
                    observe(entity);
        return vehicles.values().stream().anyMatch(entry -> spawner.equals(entry.getUUID(SPAWNER)) && entry.getString("dimension").equals(level.dimension().location().toString())
            && (map == null || map.getBounds() == null || map.getBounds().contains(new Vec3(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z")))));
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        ListTag entries = new ListTag();
        vehicles.values().forEach(entry -> entries.add(entry.copy()));
        tag.put("vehicles", entries);
        return tag;
    }
}
