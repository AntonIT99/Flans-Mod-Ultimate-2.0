package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.IFlanNpcDistance;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Custom NPCs owns the live entity; detached model entities do not own tracking or culling. */
@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class NpcDistanceMixin implements IFlanNpcDistance
{
    @Unique private static final String FLANS_DISTANCE_OPT_IN = "FlansDistanceOptIn";
    @Unique private static final EntityDataAccessor<Boolean> FLANS_DISTANCE_DATA =
        SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
    @Unique private boolean wolffsmodnpcs$lastDistanceManaged;

    // The 1.21.1 dependency uses Mojang names and the builder-based data lifecycle.
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void wolffsmodnpcs$defineDistance(SynchedEntityData.Builder builder, CallbackInfo callback)
    {
        builder.define(FLANS_DISTANCE_DATA, false);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcs$readDistance(CompoundTag tag, CallbackInfo callback)
    {
        ((Entity)(Object)this).getEntityData().set(FLANS_DISTANCE_DATA, tag.getBoolean(FLANS_DISTANCE_OPT_IN));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcs$saveDistance(CompoundTag tag, CallbackInfo callback)
    {
        tag.putBoolean(FLANS_DISTANCE_OPT_IN, isFlanNpcDistanceOptIn());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void wolffsmodnpcs$changedModel(CallbackInfo callback)
    {
        Entity npc = (Entity)(Object)this;
        if (npc.level() instanceof ServerLevel level)
        {
            boolean managed = usesFlanNpcDistanceSettings();
            if (managed != wolffsmodnpcs$lastDistanceManaged)
            {
                wolffsmodnpcs$lastDistanceManaged = managed;
                FlansModApi.refreshEntityTracking(level);
            }
        }
    }

    @Override
    public boolean usesFlanNpcDistanceSettings()
    {
        return isFlanNpcDistanceOptIn() || (Object)this instanceof EntityCustomNpc npc
            && npc.modelData.getEntity(npc) instanceof FlanModelEntity;
    }

    @Override
    public boolean isFlanNpcDistanceOptIn()
    {
        return ((Entity)(Object)this).getEntityData().get(FLANS_DISTANCE_DATA);
    }

    @Override
    public void setFlanNpcDistanceOptIn(boolean enabled)
    {
        Entity npc = (Entity)(Object)this;
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            return;
        npc.getEntityData().set(FLANS_DISTANCE_DATA, enabled);
        FlansModApi.refreshEntityTracking(level);
    }
}
