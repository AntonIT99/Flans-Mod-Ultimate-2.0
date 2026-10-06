package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeProperties;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

/** Piggybacks stored defaults and switches on Custom NPCs' existing server-to-client spawn/update data. */
@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class NpcTypeSyncMixin
{
    @Unique
    private static final String WOLFFS_TYPE_DEFAULTS = "WolffsModTypeDefaults";

    @Inject(method = "writeSpawnData()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void wolffsmodnpcsSyncTypeDefaults(CallbackInfoReturnable<CompoundTag> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        callback.getReturnValue().put(WOLFFS_TYPE_DEFAULTS, NpcTypeProperties.of(npc).syncDefaults(npc));
    }

    @Inject(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void wolffsmodnpcsReadTypeDefaults(CompoundTag tag, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (npc.level().isClientSide && tag.contains(WOLFFS_TYPE_DEFAULTS))
            NpcTypeProperties.of(npc).readSyncedDefaults(npc, tag.getCompound(WOLFFS_TYPE_DEFAULTS));
    }
}
