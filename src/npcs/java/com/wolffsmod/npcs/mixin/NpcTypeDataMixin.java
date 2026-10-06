package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeProperties;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAdvanced;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Keeps inherited movement and audio out of normal persistent/menu defaults. */
@Mixin(value = {DataAI.class, DataAdvanced.class}, remap = false)
public abstract class NpcTypeDataMixin
{
    @Shadow(remap = false)
    private EntityNPCInterface npc;

    @Inject(method = "readToNBT", at = @At("TAIL"))
    private void wolffsmodnpcsLoadedDefaults(CompoundTag tag, CallbackInfo callback)
    {
        NpcTypeProperties.of(npc).loaded(NpcTypeProperties.component(this));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void wolffsmodnpcsStoredDefaults(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        NpcTypeProperties.of(npc).saveDefaults(NpcTypeProperties.component(this), callback.getReturnValue());
    }
}
