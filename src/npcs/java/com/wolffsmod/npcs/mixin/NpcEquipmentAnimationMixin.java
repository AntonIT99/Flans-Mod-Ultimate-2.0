package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.client.equipment.FlansEquipmentRender;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ModelData;
import noppes.npcs.client.model.animation.AnimationHandler;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;

/** Applies equipment poses after Custom NPCs' humanoid animation pipeline and gives mounted crew the sitting animation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mixin(value = AnimationHandler.class, remap = false)
public abstract class NpcEquipmentAnimationMixin
{
    @Redirect(method = {"animateBipedPre", "animateBipedPost"}, at = @At(value = "FIELD", target = "Lnoppes/npcs/entity/EntityNPCInterface;currentAnimation:I"))
    private static int wolffsmodnpcsCrewSitting(EntityNPCInterface npc)
    {
        // Override only the rendered animation; dismounting immediately reveals the NPC's normal animation again.
        return NpcCrew.vehicleOf(npc).isPresent() ? 1 : npc.currentAnimation;
    }

    // Mixin callbacks must match the dependency's complete method signature.
    @SuppressWarnings("java:S107")
    @Inject(method = "animateBipedPost", at = @At("TAIL"))
    private static void wolffsmodnpcsItemPoses(ModelData data, HumanoidModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch,
        CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) entity;
        if (NpcCrew.vehicleOf(npc).isPresent())
            return;
        if (!NpcEquipment.enabled(npc, Feature.WEAPON_ANIMATIONS) && NpcEquipment.weaponAuthority(npc) && (npc.isUsingItem() || npc.currentAnimation == 6))
        {
            model.rightArm.xRot = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * limbSwingAmount;
            model.leftArm.xRot = (float) Math.cos(limbSwing * 0.6662F) * limbSwingAmount;
            model.rightArm.yRot = model.leftArm.yRot = 0F;
            model.rightArm.zRot = model.leftArm.zRot = 0F;
        }
        // Always runs: shield poses survive disabled animation, and explicit custom axes run last.
        FlansEquipmentRender.applyWeaponPose(model, npc);
        if (model instanceof PlayerModel<?> player)
        {
            player.rightSleeve.copyFrom(model.rightArm);
            player.leftSleeve.copyFrom(model.leftArm);
        }
    }
}
