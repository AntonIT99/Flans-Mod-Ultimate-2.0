package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.NpcTypeHealth;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Applies hull HP after Custom NPCs has loaded the model, then restores the actual saved damage state. */
@Mixin(value = EntityCustomNpc.class, remap = false)
public abstract class NpcTypeModelLoadMixin
{
    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("TAIL"))
    private void wolffsmodnpcsLoadedType(CompoundTag tag, CallbackInfo callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        if (!npc.level().isClientSide)
        {
            NpcTypeProperties.of(npc).refresh(npc);
            // Old saves use the NPC maximum; inherited saves record the effective maximum separately from stored defaults.
            if (tag.contains("Health") && (tag.contains(NpcTypeHealth.SAVED_MAXIMUM) || NpcTypeProperties.of(npc).values().containsKey(NpcTypeProperty.HEALTH)))
            {
                double bound = Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
                float savedMaximum = tag.contains(NpcTypeHealth.SAVED_MAXIMUM) ? tag.getFloat(NpcTypeHealth.SAVED_MAXIMUM) : (float) Math.min(tag.getInt("MaxHealth"), bound);
                npc.setHealth(NpcTypeHealth.restore(tag.getFloat("Health"), savedMaximum, npc.getMaxHealth()));
            }
        }
    }

    @Inject(method = {"addAdditionalSaveData", "m_7380_"}, at = @At("TAIL"))
    private void wolffsmodnpcsSaveEffectiveHealth(CompoundTag tag, CallbackInfo callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        if (NpcTypeProperties.of(npc).values().containsKey(NpcTypeProperty.HEALTH))
            tag.putFloat(NpcTypeHealth.SAVED_MAXIMUM, npc.getMaxHealth());
        else
            tag.remove(NpcTypeHealth.SAVED_MAXIMUM);
    }
}
