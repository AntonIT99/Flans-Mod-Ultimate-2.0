package com.flansmodultimate.common.raytracing;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunAnimationConfig;
import com.flansmodultimate.common.types.GunType;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShieldAttackPoseTest
{
    @BeforeAll
    static void bootstrapRegistries()
    {
        SharedConstants.tryDetectVersion();
        try
        {
            Bootstrap.bootStrap();
        }
        catch (ExceptionInInitializerError error)
        {
            // Plain JUnit lacks Forge's NetworkEvent transformer; vanilla registries initialize first.
            Throwable cause = error;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (!(cause instanceof NoSuchMethodException) || !cause.getMessage().equals("net.minecraftforge.network.NetworkEvent.<init>()"))
                throw error;
        }
    }

    @Test
    void mainHandAttackKeepsShieldAndItsArmSteadyForEitherHandedness()
    {
        for (HumanoidArm mainArm : HumanoidArm.values())
        {
            Player player = player(mainArm, shield(), InteractionHand.MAIN_HAND);
            try (var poses = mockStatic(GunArmPoses.class, CALLS_REAL_METHODS))
            {
                poses.when(() -> GunArmPoses.resolve(player)).thenReturn(new GunArmPoses.Result(GunArmPoses.Arm.NONE, GunArmPoses.Arm.ONE_ARM));
                PlayerSnapshot idle = new PlayerSnapshot(player);
                when(player.getAttackAnim(1F)).thenReturn(0.4F);
                PlayerSnapshot attacking = new PlayerSnapshot(player);
                EnumHitboxType shieldArm = mainArm == HumanoidArm.RIGHT ? EnumHitboxType.LEFTARM : EnumHitboxType.RIGHTARM;
                EnumHitboxType weaponArm = mainArm == HumanoidArm.RIGHT ? EnumHitboxType.RIGHTARM : EnumHitboxType.LEFTARM;
                assertTrue(transform(idle, shieldArm).equals(transform(attacking, shieldArm), 0.000001F));
                assertTrue(transform(idle, EnumHitboxType.LEFTITEM).equals(transform(attacking, EnumHitboxType.LEFTITEM), 0.000001F));
                assertFalse(transform(idle, weaponArm).equals(transform(attacking, weaponArm), 0.000001F));
                assertFalse(transform(idle, EnumHitboxType.BODY).equals(transform(attacking, EnumHitboxType.BODY), 0.000001F));
            }
        }
    }

    @Test
    void vanillaOffhandItemsAndAttacksWithTheShieldKeepTheirAttackMovement()
    {
        for (HumanoidArm mainArm : HumanoidArm.values())
        {
            assertFalse(GunArmPoses.keepsOffhandShieldRaised(player(mainArm, ItemStack.EMPTY, InteractionHand.MAIN_HAND)));
            assertFalse(GunArmPoses.keepsOffhandShieldRaised(player(mainArm, shield(), InteractionHand.OFF_HAND)));
        }
    }

    private static Player player(HumanoidArm mainArm, ItemStack offhand, InteractionHand swingingHand)
    {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        when(player.level()).thenReturn(level);
        when(player.getMainArm()).thenReturn(mainArm);
        when(player.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(player.getOffhandItem()).thenReturn(offhand);
        when(player.getItemInHand(any())).thenAnswer(call -> call.getArgument(0) == InteractionHand.OFF_HAND ? offhand : ItemStack.EMPTY);
        when(player.getDeltaMovement()).thenReturn(Vec3.ZERO);
        player.swingingArm = swingingHand;
        return player;
    }

    private static ItemStack shield()
    {
        GunType type = mock(GunType.class);
        when(type.isShield()).thenReturn(true);
        when(type.getAnimationConfig()).thenReturn(new GunAnimationConfig());
        when(type.getModelScale()).thenReturn(1F);
        when(type.getShieldOrigin()).thenReturn(new Vector3f());
        when(type.getShieldDimensions()).thenReturn(new Vector3f(1F, 1F, 1F));
        GunItem item = mock(GunItem.class);
        when(item.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        return stack;
    }

    private static Matrix4f transform(PlayerSnapshot snapshot, EnumHitboxType type)
    {
        return snapshot.hitboxes.stream().filter(hitbox -> hitbox.type == type).findFirst().orElseThrow().transform;
    }
}
