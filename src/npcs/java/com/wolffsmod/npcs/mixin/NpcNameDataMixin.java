package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.SoldierNames;
import noppes.npcs.entity.data.DataDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Accepts the appended generator slots and preserves a stable list identity in NPC saves and editor synchronization. */
@Mixin(value = DataDisplay.class, remap = false)
public abstract class NpcNameDataMixin
{
    @Shadow
    private int markovGeneratorId;

    @Inject(method = "setMarkovGeneratorId", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsAcceptSoldierList(int generator, CallbackInfo callback)
    {
        if (SoldierNames.contains(generator))
        {
            markovGeneratorId = generator;
            callback.cancel();
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsSaveNameList(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        SoldierNames.saveSelection(markovGeneratorId, callback.getReturnValue());
    }

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsLoadNameList(CompoundTag tag, CallbackInfo callback)
    {
        markovGeneratorId = SoldierNames.loadSelection(markovGeneratorId, tag);
    }
}
