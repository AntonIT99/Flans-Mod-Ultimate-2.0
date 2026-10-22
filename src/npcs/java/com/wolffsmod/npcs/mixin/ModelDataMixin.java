package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelRendering;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** Enforces normal rendering for Flan models on load, use and save, including presets and direct flag writes. */
@Mixin(value = ModelData.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into ModelData, so the self-cast is valid.
public abstract class ModelDataMixin
{
    @Inject(method = "load", at = @At("TAIL"))
    private void wolffsmodnpcsLoadRenderer(CompoundTag tag, CallbackInfo callback)
    {
        FlanModelRendering.loaded((ModelData) (Object) this, tag);
    }

    @Inject(method = "getEntity", at = @At("HEAD"))
    private void wolffsmodnpcsUseRenderer(EntityNPCInterface npc, CallbackInfoReturnable<LivingEntity> callback)
    {
        FlanModelRendering.enforce((ModelData) (Object) this);
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void wolffsmodnpcsSaveRenderer(CallbackInfoReturnable<CompoundTag> callback)
    {
        FlanModelRendering.enforce((ModelData) (Object) this);
    }
}
