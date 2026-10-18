package com.flansmodultimate.mixin;

import com.flansmodultimate.common.guns.GunArmPoses;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/** Keeps a player's off-hand Flan shield arm steady through vanilla main-hand attacks. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin
{
    @Shadow @Final
    public ModelPart rightArm;
    @Shadow @Final
    public ModelPart leftArm;

    @Unique
    private ModelPart flansmodultimateShieldArm;
    @Unique
    private PartPose flansmodultimateShieldArmPose;

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"))
    private void flansmodultimateSaveShieldArm(LivingEntity entity, float ageInTicks, CallbackInfo callback)
    {
        flansmodultimateShieldArm = null;
        flansmodultimateShieldArmPose = null;
        if (GunArmPoses.keepsOffhandShieldRaised(entity))
        {
            flansmodultimateShieldArm = entity.getMainArm() == HumanoidArm.RIGHT ? leftArm : rightArm;
            flansmodultimateShieldArmPose = flansmodultimateShieldArm.storePose();
        }
    }

    @Inject(method = "setupAttackAnimation", at = @At("RETURN"))
    private void flansmodultimateRestoreShieldArm(LivingEntity entity, float ageInTicks, CallbackInfo callback)
    {
        if (flansmodultimateShieldArm != null && flansmodultimateShieldArmPose != null)
            flansmodultimateShieldArm.loadPose(flansmodultimateShieldArmPose);
        flansmodultimateShieldArm = null;
        flansmodultimateShieldArmPose = null;
    }
}
