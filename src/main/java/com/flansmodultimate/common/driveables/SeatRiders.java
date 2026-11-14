package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Keeps the riders of a driveable's seats with the world. Seats are proxies that are never saved, and vanilla saves a
 * passenger only inside its vehicle, so a rider would be dropped from the save with its chunk. A rider other than a
 * player is therefore saved by itself, with the seat it sat in, and its driveable calls it back once both are loaded.
 * Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeatRiders
{
    /** How long after loading a driveable looks for its riders, and a loaded rider answers, in ticks. */
    public static final int RECALL_TICKS = 200;
    /** Ticks between two looks for riders. */
    public static final int RECALL_INTERVAL = 10;

    static final String NBT_SEAT = "flansmodultimate:seat";
    private static final String NBT_DRIVEABLE = "Driveable";
    private static final String NBT_INDEX = "Index";
    /** How far around its seats a driveable looks for riders, which are saved at their seat, in blocks. */
    private static final double RECALL_REACH = 8D;

    /**
     * The seat an entity rode when it was saved.
     *
     * @param loadedAt
     *            the game time the entity was loaded at; the seat is only taken again shortly afterwards, so a rider
     *            that has since gone its own way is not pulled back when its driveable is loaded again later
     */
    public record SavedSeat(UUID driveable, int index, long loadedAt)
    {
        /** @return the seat saved in an entity's data, or null when it was not saved riding one */
        @Nullable
        public static SavedSeat load(CompoundTag entityTag, long gameTime)
        {
            if (!entityTag.contains(NBT_SEAT, Tag.TAG_COMPOUND))
                return null;
            CompoundTag tag = entityTag.getCompound(NBT_SEAT);
            return tag.hasUUID(NBT_DRIVEABLE) && tag.contains(NBT_INDEX, Tag.TAG_INT) ? new SavedSeat(tag.getUUID(NBT_DRIVEABLE), tag.getInt(NBT_INDEX), gameTime) : null;
        }

        void save(CompoundTag entityTag)
        {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(NBT_DRIVEABLE, driveable);
            tag.putInt(NBT_INDEX, index);
            entityTag.put(NBT_SEAT, tag);
        }

        /** Whether this is a seat of the driveable, taken recently enough to be taken again. */
        boolean answers(UUID driveableId, long gameTime)
        {
            return driveable.equals(driveableId) && gameTime >= loadedAt && gameTime - loadedAt <= RECALL_TICKS;
        }
    }

    /** Whether vanilla would leave the entity out of the save because it rides a seat. Players are saved by the server. */
    public static boolean savesAlone(Entity entity)
    {
        return !(entity instanceof Player) && entity.getVehicle() instanceof Seat;
    }

    /**
     * Saves a rider for which {@link #savesAlone} holds as an entity of its own, with the seat to come back to.
     *
     * @return whether it was saved, as {@link Entity#save} reports
     */
    public static boolean save(Entity rider, CompoundTag tag)
    {
        if (!rider.saveAsPassenger(tag))
            return false;
        if (rider.getVehicle() instanceof Seat seat && seat.getDriveable() != null)
            new SavedSeat(seat.getDriveable().getUUID(), seat.getSeatIndex(), 0L).save(tag);
        return true;
    }

    /** Puts the riders saved in these seats of a just loaded driveable, and loaded themselves since, back in them. */
    public static void recall(Driveable driveable, Seat[] seats)
    {
        AABB reach = null;
        for (Seat seat : seats)
        {
            if (seat != null && seat.isAlive())
                reach = reach == null ? seat.getBoundingBox() : reach.minmax(seat.getBoundingBox());
        }
        if (reach == null)
            return;
        long gameTime = driveable.level().getGameTime();
        UUID id = driveable.getUUID();
        for (Entity entity : driveable.level().getEntities(driveable, reach.inflate(RECALL_REACH), candidate -> awaits(candidate, id, gameTime)))
        {
            SeatRiderEntity rider = (SeatRiderEntity) entity;
            int index = rider.flansmodultimateSavedSeat().index();
            rider.flansmodultimateSetSavedSeat(null);
            Seat seat = index >= 0 && index < seats.length ? seats[index] : null;
            // The seat decides as for anyone getting in: a wreck or someone already there keeps the rider out.
            if (seat != null && seat.isAlive())
                entity.startRiding(seat);
        }
    }

    private static boolean awaits(Entity candidate, UUID driveableId, long gameTime)
    {
        if (!(candidate instanceof SeatRiderEntity rider) || !candidate.isAlive() || candidate.isPassenger())
            return false;
        SavedSeat saved = rider.flansmodultimateSavedSeat();
        return saved != null && saved.answers(driveableId, gameTime);
    }
}
