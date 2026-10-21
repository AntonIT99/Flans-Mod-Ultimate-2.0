package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The places operators marked on Teams maps for bots: route points paths are strung through, garrison and sniper posts
 * for defenders, positions for AA and patrol points for ships. Saved with the world, per map. Server thread.
 */
public final class BotMarkers extends SavedData
{
    private static final String ID = "wolffsmodnpcs_bot_markers";
    private static final String NBT_MAPS = "maps";
    private static final String NBT_TYPE = "type";
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";

    /** What a marker is for. */
    public enum Type
    {
        /** A point paths across the map may pass through. */
        ROUTE,
        /** A post defenders of a nearby objective take up. */
        GARRISON,
        /** A post with a long view, taken by defenders first. */
        SNIPER,
        /** A position for AA guns. */
        AA,
        /** A patrol point on the water for ships. */
        NAVAL
    }

    public record Marker(Type type, Vec3 position)
    {}

    private final Map<String, List<Marker>> markersByMap = new TreeMap<>();

    public static BotMarkers get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(BotMarkers::load, BotMarkers::new, ID);
    }

    public List<Marker> of(String mapId)
    {
        return List.copyOf(markersByMap.getOrDefault(mapId, List.of()));
    }

    public List<Vec3> positions(String mapId, Type type)
    {
        return markersByMap.getOrDefault(mapId, List.of()).stream().filter(marker -> marker.type() == type).map(Marker::position).toList();
    }

    public void add(String mapId, Marker marker)
    {
        markersByMap.computeIfAbsent(mapId, ignored -> new ArrayList<>()).add(marker);
        setDirty();
    }

    /** Removes the marker of the map nearest to a position, within a distance. */
    public Optional<Marker> removeNearest(String mapId, Vec3 position, double within)
    {
        List<Marker> markers = markersByMap.get(mapId);
        if (markers == null)
            return Optional.empty();
        Optional<Marker> nearest = markers.stream().filter(marker -> marker.position().distanceTo(position) <= within)
            .min(Comparator.comparingDouble(marker -> marker.position().distanceToSqr(position)));
        nearest.ifPresent(marker ->
        {
            markers.remove(marker);
            setDirty();
        });
        return nearest;
    }

    public int clear(String mapId)
    {
        List<Marker> removed = markersByMap.remove(mapId);
        if (removed != null)
            setDirty();
        return removed == null ? 0 : removed.size();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag)
    {
        CompoundTag maps = new CompoundTag();
        markersByMap.forEach((mapId, markers) ->
        {
            ListTag list = new ListTag();
            for (Marker marker : markers)
            {
                CompoundTag entry = new CompoundTag();
                entry.putString(NBT_TYPE, marker.type().name());
                entry.putDouble(NBT_X, marker.position().x);
                entry.putDouble(NBT_Y, marker.position().y);
                entry.putDouble(NBT_Z, marker.position().z);
                list.add(entry);
            }
            maps.put(mapId, list);
        });
        tag.put(NBT_MAPS, maps);
        return tag;
    }

    static BotMarkers load(CompoundTag tag)
    {
        BotMarkers markers = new BotMarkers();
        CompoundTag maps = tag.getCompound(NBT_MAPS);
        for (String mapId : maps.getAllKeys())
        {
            List<Marker> list = new ArrayList<>();
            for (Tag element : maps.getList(mapId, Tag.TAG_COMPOUND))
            {
                CompoundTag entry = (CompoundTag) element;
                list.add(new Marker(BotSettings.parse(Type.class, entry.getString(NBT_TYPE), Type.ROUTE), new Vec3(entry.getDouble(NBT_X), entry.getDouble(NBT_Y), entry.getDouble(NBT_Z))));
            }
            markers.markersByMap.put(mapId, list);
        }
        return markers;
    }
}
