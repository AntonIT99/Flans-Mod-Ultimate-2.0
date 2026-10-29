package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.client.equipment.FlansEquipmentRender;
import com.wolffsmod.npcs.crew.NpcCrew;
import noppes.npcs.entity.EntityCustomNpc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;

/** Gives borrowed humanoid models the live NPC's handedness, swing, crew sitting and equipment poses, including legacy skin models. */
@Mixin(targets = "noppes.npcs.client.renderer.RenderCustomNpc$2", remap = false)
public abstract class NpcBorrowedModelMixin
{
    // The redirect includes the enclosing method's arguments to recover the live owner of the detached model entity.
    @SuppressWarnings({"rawtypes", "unchecked", "java:S107", "DefaultAnnotationParam", "UnresolvedMixinReference"})
    @Redirect(method = "setupAnim", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", remap = true))
    private void wolffsmodnpcsAnimateOwner(EntityModel model, Entity borrowed, float swing, float amount, float age, float yaw, float pitch, Entity owner, float ownerSwing, float ownerAmount,
        float ownerAge, float ownerYaw, float ownerPitch)
    {
        if (model instanceof HumanoidModel<?> humanoid && borrowed instanceof EntityCustomNpc && owner instanceof EntityCustomNpc npc)
        {
            boolean crew = NpcCrew.vehicleOf(npc).isPresent();
            model.riding = crew || npc.isPassenger() && npc.getVehicle() != null && npc.getVehicle().shouldRiderSit();
            model.setupAnim(npc, swing, amount, age, yaw, pitch);
            if (!crew)
                FlansEquipmentRender.applyWeaponPose(humanoid, npc);
            if (humanoid instanceof PlayerModel<?> player)
            {
                player.rightSleeve.copyFrom(humanoid.rightArm);
                player.leftSleeve.copyFrom(humanoid.leftArm);
            }
        }
        else
            model.setupAnim(borrowed, swing, amount, age, yaw, pitch);
    }
}
