package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.world.ApocalypseWorldgen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

import java.util.Locale;

/**
 * Stands in for work that worldgen cannot do on its worker threads.
 *
 * <p>Driveables, AI mechas, survivors and teleporter portals touch server state that only the
 * server thread may change. Worldgen therefore leaves one of these in the chunk, where it is
 * saved with the chunk like any other entity, and on its first tick on the server thread it
 * performs the spawn and removes itself.</p>
 */
public class WorldgenSpawnMarker extends Entity
{
    private static final String NBT_KIND = "kind";
    private static final String NBT_SEED = "seed";

    public enum Kind
    {
        SURVIVOR,
        ABANDONED_VEHICLE,
        LAB_MECHA,
        PARKED_PLANE,
        APOCALYPSE_PORTAL,
        OVERWORLD_PORTAL
    }

    @Nullable
    private Kind kind = Kind.SURVIVOR;
    private long seed;

    public WorldgenSpawnMarker(EntityType<? extends WorldgenSpawnMarker> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Leaves a marker at the given position; the random only seeds what the spawn picks later. */
    public static void place(WorldGenLevel level, Kind kind, double x, double y, double z, RandomSource random)
    {
        WorldgenSpawnMarker marker = new WorldgenSpawnMarker(ApocalypseContent.worldgenSpawnMarker.get(), level.getLevel());
        marker.kind = kind;
        marker.seed = random.nextLong();
        marker.setPos(x, y, z);
        level.addFreshEntity(marker);
    }

    @Override
    public void tick()
    {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        // Removed first so that a spawn which throws can never run again on every tick.
        discard();
        if (kind == null)
            return;
        try
        {
            ApocalypseWorldgen.runDeferredSpawn(serverLevel, kind, position(), RandomSource.create(seed));
        }
        catch (RuntimeException exception)
        {
            FlansMod.log.error("Apocalypse worldgen {} spawn at {} failed", kind, blockPosition(), exception);
        }
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        try
        {
            kind = Kind.valueOf(tag.getString(NBT_KIND).toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception)
        {
            // An unknown kind came from a newer or edited save: do nothing rather than guess.
            FlansMod.log.warn("Discarding apocalypse worldgen marker with unknown kind '{}'", tag.getString(NBT_KIND));
            kind = null;
        }
        seed = tag.getLong(NBT_SEED);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        tag.putString(NBT_KIND, kind.name().toLowerCase(Locale.ROOT));
        tag.putLong(NBT_SEED, seed);
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean isAttackable()
    {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z)
    {
        return false;
    }
}
