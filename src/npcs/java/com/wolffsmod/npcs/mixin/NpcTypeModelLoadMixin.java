package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelRendering;
import com.wolffsmod.npcs.platform.NpcPlatform;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.soldier.SoldierCloneNames;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

/**
 * Migrates legacy Flan rendering, applies inherited HP, restores saved damage state and regenerates opted-in clone names. Also
 * raises the eye height of an NPC wearing a model to the model muzzle: Minecraft 1.21 reads it from the dimensions this class returns.
 */
// Mixin merges this class into the target, so self-casts are valid.
@SuppressWarnings({"UnresolvedMixinReference", "DataFlowIssue"})
@Mixin(value = EntityCustomNpc.class, remap = false)
public abstract class NpcTypeModelLoadMixin
{
    @ModifyVariable(method = "readAdditionalSaveData", at = @At("HEAD"), argsOnly = true)
    private CompoundTag wolffsmodnpcsMigrateRenderer(CompoundTag tag)
    {
        return FlanModelRendering.migrateNpc(tag, "NpcModelData");
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsLoadedType(CompoundTag compound, CallbackInfo callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        if (!npc.level().isClientSide)
        {
            SoldierCloneNames.loaded(npc, compound);
            NpcTypeProperties.of(npc).refresh(npc);
            // Old saves use the NPC maximum; inherited saves record the effective maximum separately from stored defaults.
            if (compound.contains("Health") && (compound.contains(NpcTypeHealth.SAVED_MAXIMUM) || NpcTypeProperties.of(npc).values().containsKey(NpcTypeProperty.HEALTH)))
            {
                double bound = NpcPlatform.maxHealthLimit();
                float savedMaximum = compound.contains(NpcTypeHealth.SAVED_MAXIMUM) ? compound.getFloat(NpcTypeHealth.SAVED_MAXIMUM) : (float) Math.min(compound.getInt("MaxHealth"), bound);
                npc.setHealth(NpcTypeHealth.restore(compound.getFloat("Health"), savedMaximum, npc.getMaxHealth()));
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsSaveEffectiveHealth(CompoundTag tag, CallbackInfo callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        SoldierCloneNames.save(npc, tag);
        if (NpcTypeProperties.of(npc).values().containsKey(NpcTypeProperty.HEALTH))
            tag.putFloat(NpcTypeHealth.SAVED_MAXIMUM, npc.getMaxHealth());
        else
            tag.remove(NpcTypeHealth.SAVED_MAXIMUM);
    }

    // A worn model replaces the dimensions without calling EntityNPCInterface.getDimensions, which the other hook extends.
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsModelEyeHeight(Pose pose, CallbackInfoReturnable<EntityDimensions> callback)
    {
        EntityCustomNpc npc = (EntityCustomNpc) (Object) this;
        EntityDimensions dimensions = callback.getReturnValue();
        NpcVehicleState.of(npc).eyeHeight(npc, dimensions.height()).ifPresent(height -> callback.setReturnValue(dimensions.withEyeHeight(height)));
    }
}
