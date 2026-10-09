package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.properties.*;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Applies hull HP after Custom NPCs has loaded the model, then restores the actual saved damage state. */
// Mixin merges this class into the target, so self-casts are valid.
@SuppressWarnings({"UnresolvedMixinReference", "DataFlowIssue"})
@Mixin(value = EntityCustomNpc.class, remap = false)
public abstract class NpcTypeModelLoadMixin
{
    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("TAIL"))
    private void wolffsmodnpcsLoadedType(CompoundTag compound, CallbackInfo callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        if (!npc.level().isClientSide)
        {
            NpcTypeProperties.of(npc).refresh(npc);
            // Old saves use the NPC maximum; inherited saves record the effective maximum separately from stored defaults.
            if (compound.contains("Health") && (compound.contains(NpcTypeHealth.SAVED_MAXIMUM) || NpcTypeProperties.of(npc).values().containsKey(NpcTypeProperty.HEALTH)))
            {
                double bound = Attributes.MAX_HEALTH instanceof RangedAttribute attribute ? attribute.getMaxValue() : Integer.MAX_VALUE;
                float savedMaximum = compound.contains(NpcTypeHealth.SAVED_MAXIMUM) ? compound.getFloat(NpcTypeHealth.SAVED_MAXIMUM) : (float) Math.min(compound.getInt("MaxHealth"), bound);
                npc.setHealth(NpcTypeHealth.restore(compound.getFloat("Health"), savedMaximum, npc.getMaxHealth()));
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
