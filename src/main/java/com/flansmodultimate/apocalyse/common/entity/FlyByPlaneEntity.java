package com.flansmodultimate.apocalyse.common.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.PlaneType;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An aircraft that crosses the apocalypse sky under its own power.
 *
 * <p>1.7.10 kept these flying by overriding the fuel checks and pinning the throttle open
 * every tick. The same is done here through the ordinary control path: the autopilot commands
 * the aircraft, holds the throttle-up input and keeps the tank topped up, then normal plane
 * physics fly it — so a flyby that is shot down breaks up like any other plane.</p>
 */
public class FlyByPlaneEntity extends Plane
{
    /** Marks the skeleton the flyby brought with it, so a despawn takes it along. */
    public static final String NBT_FLYBY_CREW = "flansmodapocalypse:flyby_crew";

    private static final String NBT_LIFETIME = "flyby_lifetime";
    private static final String NBT_CREW_SPAWNED = "flyby_crew_spawned";
    /** Removed after this long even if nobody ever saw it, so flyovers cannot accumulate. */
    private static final int MAX_LIFETIME_TICKS = 20 * 60 * 5;
    /** Beyond this distance from every player the flyover has served its purpose. */
    private static final double DESPAWN_DISTANCE = 384.0D;

    private int lifetimeTicks;
    private boolean crewSpawned;

    public FlyByPlaneEntity(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public FlyByPlaneEntity(Level level, PlaneType type, double x, double y, double z, float yaw)
    {
        super(ApocalypseContent.flyByPlane.get(), level, type, x, y, z, yaw, null, ItemStack.EMPTY);
    }

    /** The autopilot flies the aircraft, whether or not the pilot is still alive. */
    @Override
    protected boolean isUnderCommand()
    {
        return true;
    }

    @Override
    protected void tickDriveable()
    {
        PlaneType type = getPlaneType();
        if (type != null)
        {
            setFuel(type.getFuelTankSize());
            setInputMask(DriveableInput.FORWARD);
            // The base tick drops stale control input after a timeout; this input is never stale.
            inputTimeout = 0;
        }

        super.tickDriveable();

        if (type != null)
            setThrottle(1F);
        seatCrew();
        if (++lifetimeTicks > MAX_LIFETIME_TICKS || level().getNearestPlayer(this, DESPAWN_DISTANCE) == null)
            despawn();
    }

    /**
     * Puts a skeleton at the controls once there is a seat to put it in.
     *
     * <p>Seat entities are created by the driveable's own first tick, so the crew cannot be
     * placed at spawn time and is boarded here instead.</p>
     */
    private void seatCrew()
    {
        if (crewSpawned || !(level() instanceof ServerLevel serverLevel))
            return;
        Seat driverSeat = getSeat(0);
        if (driverSeat == null)
            return;

        crewSpawned = true;
        if (driverSeat.getRiddenByEntity() != null)
            return;
        Skeleton pilot = EntityType.SKELETON.create(serverLevel);
        if (pilot == null)
            return;
        pilot.moveTo(getX(), getY(), getZ(), getYRot(), 0F);
        pilot.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.EVENT, null);
        pilot.setPersistenceRequired();
        pilot.getPersistentData().putBoolean(NBT_FLYBY_CREW, true);
        if (serverLevel.addFreshEntity(pilot))
            pilot.startRiding(driverSeat, true);
    }

    /** Removes the aircraft together with the crew it was generated with. */
    private void despawn()
    {
        for (Seat seat : seats)
        {
            if (seat == null)
                continue;
            Entity rider = seat.getRiddenByEntity();
            if (rider != null && !(rider instanceof Player) && rider.getPersistentData().getBoolean(NBT_FLYBY_CREW))
                rider.discard();
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        lifetimeTicks = tag.getInt(NBT_LIFETIME);
        crewSpawned = tag.getBoolean(NBT_CREW_SPAWNED);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_LIFETIME, lifetimeTicks);
        tag.putBoolean(NBT_CREW_SPAWNED, crewSpawned);
    }
}
