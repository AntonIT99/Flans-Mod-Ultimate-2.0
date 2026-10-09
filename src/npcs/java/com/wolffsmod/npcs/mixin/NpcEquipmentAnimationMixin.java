package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.client.FlansEquipmentRender;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ModelData;
import noppes.npcs.client.model.animation.AnimationHandler;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;

/** Applies equipment poses after Custom NPCs' animation pipeline has positioned the humanoid model. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mixin(value = AnimationHandler.class, remap = false)
public abstract class NpcEquipmentAnimationMixin
{
    // Mixin callbacks must match the dependency's complete method signature.
    @SuppressWarnings("java:S107")
    @Inject(method = "animateBipedPost", at = @At("TAIL"))
    private static void wolffsmodnpcsItemPoses(ModelData data, HumanoidModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch,
        CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) entity;
        if (NpcEquipment.enabled(npc, Feature.WEAPON_ANIMATIONS))
            FlansEquipmentRender.applyWeaponPose(model, npc);
        else if (NpcEquipment.weaponAuthority(npc) && (npc.isUsingItem() || npc.currentAnimation == 6))
        {
            model.rightArm.xRot = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * limbSwingAmount;
            model.leftArm.xRot = (float) Math.cos(limbSwing * 0.6662F) * limbSwingAmount;
            model.rightArm.yRot = model.leftArm.yRot = 0F;
            model.rightArm.zRot = model.leftArm.zRot = 0F;
        }
        if (model instanceof PlayerModel<?> player)
        {
            player.rightSleeve.copyFrom(model.rightArm);
            player.leftSleeve.copyFrom(model.leftArm);
        }
    }
}
