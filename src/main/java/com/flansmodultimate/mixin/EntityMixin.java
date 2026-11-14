package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.SeatRiderEntity;
import com.flansmodultimate.common.driveables.SeatRiders;
import com.flansmodultimate.common.driveables.collision.*;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;

/**
 * Hooks into the vanilla entity base class.
 *
 * <p>
 * Lets ordinary entity movement collide with driveable hulls. Vanilla collides moving entities only with blocks
 * and with other entities' bounding boxes, which cannot describe a rotated, shaped hull. Movement that could reach a
 * hull is resolved by {@link DriveableCollisionWorld} using the same axis order and step-up rules; all other movement
 * runs vanilla unchanged.
 * Entities wearing a solid part body instead move that body's posed geometry against terrain and solid entities,
 * so ground and wall contacts use their actual parts rather than their vanilla bounding box.
 * </p>
 *
 * <p>
 * Keeps the {@link HullCarry} record of an entity a moving hull carries, and moves its stored previous position back
 * to before a carry that came ahead of its own tick, so the client interpolates the carry instead of stepping it.
 * </p>
 *
 * <p>
 * Saves an entity riding a driveable seat as an entity of its own, with the seat to return to. Seats are never saved,
 * and vanilla saves a passenger only inside its vehicle, so the rider would otherwise be dropped from the save.
 * </p>
 *
 * <p>
 * The owning Flan NPC is culled before its detached Flan model renderer is called.
 * </p>
 */
@Mixin(Entity.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class EntityMixin implements HullCarriedEntity, SeatRiderEntity
{
    @Unique
    private HullCarry flansmodultimateHullCarry;
    @Unique
    private SeatRiders.SavedSeat flansmodultimateSavedSeat;

    // Hull collision

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateCollideWithDriveableHulls(Vec3 movement, CallbackInfoReturnable<Vec3> callback)
    {
        Entity entity = (Entity) (Object) this;
        PartBody body = PartBodies.of(entity);
        Vec3 result = body == null || entity.isPassenger() ? null : body.collideMovement(movement);
        if (result == null)
            result = DriveableCollisionWorld.collide(entity, movement);
        if (result != null)
            callback.setReturnValue(result);
    }

    // Carrying by hulls

    @Override
    public HullCarry flansmodultimateHullCarry()
    {
        if (flansmodultimateHullCarry == null)
            flansmodultimateHullCarry = new HullCarry();
        return flansmodultimateHullCarry;
    }

    @Override
    public HullCarry flansmodultimateHullCarryIfPresent()
    {
        return flansmodultimateHullCarry;
    }

    @Inject(method = "setOldPosAndRot", at = @At("TAIL"))
    private void flansmodultimateKeepHullCarryInterpolated(CallbackInfo callback)
    {
        if (flansmodultimateHullCarry != null)
            flansmodultimateHullCarry.startTick((Entity) (Object) this);
    }

    // Riders of driveable seats

    @Override
    public SeatRiders.SavedSeat flansmodultimateSavedSeat()
    {
        return flansmodultimateSavedSeat;
    }

    @Override
    public void flansmodultimateSetSavedSeat(SeatRiders.SavedSeat seat)
    {
        flansmodultimateSavedSeat = seat;
    }

    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateSaveSeatRider(CallbackInfoReturnable<Boolean> callback)
    {
        Entity entity = (Entity) (Object) this;
        Entity.RemovalReason removal = entity.getRemovalReason();
        if ((removal == null || removal.shouldSave()) && SeatRiders.savesAlone(entity))
            callback.setReturnValue(true);
    }

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;)Z", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateSaveSeatRiderAlone(CompoundTag tag, CallbackInfoReturnable<Boolean> callback)
    {
        Entity entity = (Entity) (Object) this;
        if (SeatRiders.savesAlone(entity))
            callback.setReturnValue(SeatRiders.save(entity, tag));
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void flansmodultimateLoadSavedSeat(CompoundTag tag, CallbackInfo callback)
    {
        Entity entity = (Entity) (Object) this;
        flansmodultimateSavedSeat = entity.level().isClientSide ? null : SeatRiders.SavedSeat.load(tag, entity.level().getGameTime());
    }

    // Rendering

    // shouldRenderAtSqrDistance is client-only and stripped from the dedicated server, hence require = 0.
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true, require = 0)
    private void flansmodultimateNpcDistance(double distanceSquared, CallbackInfoReturnable<Boolean> result)
    {
        Entity entity = (Entity) (Object) this;
        if (EntityDistancePolicy.isFlanNpc(entity))
        {
            double distance = EntityDistancePolicy.renderDistance(entity, true);
            result.setReturnValue(distanceSquared < distance * distance);
        }
    }
}
