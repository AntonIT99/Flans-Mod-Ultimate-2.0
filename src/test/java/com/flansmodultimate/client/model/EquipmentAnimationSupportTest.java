package com.flansmodultimate.client.model;

import com.flansmodultimate.api.equipment.ArmPoseRule;
import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.guns.GunArmPoses.Arm;
import com.flansmodultimate.common.guns.HeldItemPresentation;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipmentAnimationSupportTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void leavingProneClearsHeadAndHatRollOnAReusedModel()
    {
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(HumanoidModel.createMesh(CubeDeformation.NONE, 0F).getRoot().bake(64, 64));
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getMainArm()).thenReturn(HumanoidArm.RIGHT);
        try (var poses = mockStatic(GunArmPoses.class); var posture = mockStatic(ProneSupport.class))
        {
            poses.when(() -> GunArmPoses.resolve(entity)).thenReturn(new GunArmPoses.Result(Arm.ONE_ARM, Arm.NONE));
            posture.when(() -> ProneSupport.lyingAmount(entity, model.swimAmount)).thenReturn(1F);
            EquipmentAnimationSupport.prone(model, entity, 30F, 15F);
            assertNotEquals(0F, model.head.zRot);
            assertEquals(model.head.zRot, model.hat.zRot);

            // Vanilla's next standing animation refreshes pitch and yaw, but not roll.
            model.head.xRot = model.hat.xRot = 0.2F;
            model.head.yRot = model.hat.yRot = 0.4F;
            posture.when(() -> ProneSupport.lyingAmount(entity, model.swimAmount)).thenReturn(0F);
            EquipmentAnimationSupport.prone(model, entity, 30F, 15F);
            assertEquals(0F, model.head.zRot);
            assertEquals(0F, model.hat.zRot);
            assertEquals(0.2F, model.head.xRot);
            assertEquals(0.4F, model.head.yRot);
            assertEquals(0.2F, model.hat.xRot);
            assertEquals(0.4F, model.hat.yRot);
        }
    }

    @Test
    void aLateAimPoseKeepsTheMeleeStrokeOnOnlyTheStrikingArm()
    {
        HumanoidModel<LivingEntity> model = new HumanoidModel<>(HumanoidModel.createMesh(CubeDeformation.NONE, 0F).getRoot().bake(64, 64));
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getMainArm()).thenReturn(HumanoidArm.RIGHT);
        entity.swingingArm = InteractionHand.MAIN_HAND;
        try (var poses = mockStatic(GunArmPoses.class); var presentation = mockStatic(HeldItemPresentation.class))
        {
            poses.when(() -> GunArmPoses.resolve(entity)).thenReturn(new GunArmPoses.Result(Arm.ONE_ARM, Arm.ONE_ARM));
            presentation.when(() -> HeldItemPresentation.rule(eq(entity), any())).thenReturn(ArmPoseRule.DEFAULT);
            EquipmentAnimationSupport.weapons(model, entity);
            float restingRight = model.rightArm.xRot;
            float restingLeft = model.leftArm.xRot;
            model.attackTime = 0.5F;
            EquipmentAnimationSupport.weapons(model, entity);
            assertNotEquals(restingRight, model.rightArm.xRot);
            assertEquals(restingLeft, model.leftArm.xRot);
            entity.swingingArm = InteractionHand.OFF_HAND;
            EquipmentAnimationSupport.weapons(model, entity);
            assertEquals(restingRight, model.rightArm.xRot);
            assertNotEquals(restingLeft, model.leftArm.xRot);
        }
    }
}
