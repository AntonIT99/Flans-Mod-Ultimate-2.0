package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.JobGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;

/**
 * Custom NPCs' Guard job. Lets a guard attack NPCs wearing a content-pack model chosen on its target list, which Custom
 * NPCs otherwise never treats as guard targets because they are NPCs. NPCs of the guard's own faction are left alone.
 */
// (Object) this is the JobGuard being mixed into.
@SuppressWarnings("DataFlowIssue")
@Mixin(value = JobGuard.class, remap = false)
public abstract class JobGuardMixin
{
    @Inject(method = "isEntityApplicable", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsGuardFlanModels(Entity entity, CallbackInfoReturnable<Boolean> callback)
    {
        FlanModelEntity model = NpcModels.flan(entity);
        FlanModelEntityType type = model == null ? null : model.getModelType();
        if (type == null)
            return;
        JobGuard job = (JobGuard) (Object) this;
        boolean sameFaction = job.npc != null && ((EntityNPCInterface) entity).faction.id == job.npc.faction.id;
        callback.setReturnValue(!sameFaction && job.targets.contains(type.getDescriptionId()));
    }
}
