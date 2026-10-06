package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeProperties;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resolves model changes and switch changes before the server runs NPC combat and movement. */
@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class NpcTypePropertiesMixin
{
    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void wolffsmodnpcsInheritType(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (!npc.level().isClientSide)
            NpcTypeProperties.of(npc).refresh(npc);
    }

}
