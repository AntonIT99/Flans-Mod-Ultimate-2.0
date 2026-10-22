package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.*;
import com.flansmodultimate.client.model.EquipmentAnimationSupport;
import com.flansmodultimate.client.model.HeldItemTransforms;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.EnumEntityAimPose;
import com.flansmodultimate.config.EnumPlayerAimPose;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.gun.PacketHeldItemPresentation;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.joml.Vector3f;
import org.junit.jupiter.api.*;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeldItemPresentationTest
{
    private Mob holder;
    private ServerLevel level;
    private final AtomicReference<HeldItemSettings> main = new AtomicReference<>(HeldItemSettings.DEFAULT);
    private final AtomicReference<HeldItemSettings> off = new AtomicReference<>(HeldItemSettings.DEFAULT);

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @BeforeEach
    void holder()
    {
        holder = mock(Mob.class, withSettings().extraInterfaces(IHeldItemPresentation.class, IEquipmentPolicy.class));
        level = mock(ServerLevel.class);
        IHeldItemPresentation storage = (IHeldItemPresentation) holder;
        when(holder.level()).thenReturn(level);
        MinecraftServer server = mock(MinecraftServer.class);
        when(level.getServer()).thenReturn(server);
        when(server.isSameThread()).thenReturn(true);
        when(holder.getMainArm()).thenReturn(HumanoidArm.RIGHT);
        when(holder.getDeltaMovement()).thenReturn(Vec3.ZERO);
        when(holder.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(holder.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(holder.getItemInHand(any())).thenAnswer(call -> call.getArgument(0) == InteractionHand.MAIN_HAND ? holder.getMainHandItem() : holder.getOffhandItem());
        when(((IEquipmentPolicy) holder).flansWeaponAnimations()).thenReturn(true);
        when(storage.flansmodultimateHeldItemSettings(any())).thenAnswer(call -> call.getArgument(0) == InteractionHand.MAIN_HAND ? main.get() : off.get());
        doAnswer(call ->
        {
            (call.getArgument(0) == InteractionHand.MAIN_HAND ? main : off).set(call.getArgument(1));
            return null;
        }).when(storage).flansmodultimateHeldItemSettings(any(), any());
    }

    @Test
    void absentAndMalformedSavedSettingsKeepDefaultsAndFiniteTransforms()
    {
        assertEquals(HeldItemSettings.DEFAULT, HeldItemSettings.load(new CompoundTag()));
        CompoundTag tag = new CompoundTag();
        tag.putFloat("X", Float.NaN);
        tag.putFloat("Y", Float.POSITIVE_INFINITY);
        tag.putFloat("Z", 1000);
        tag.putInt("ShotHoldTicks", -5);
        CompoundTag pose = new CompoundTag();
        pose.putString("Pose", "future_unknown_pose");
        tag.put("IDLE", pose);
        HeldItemSettings loaded = HeldItemSettings.load(tag);
        assertEquals(new HeldItemTransform(0, 0, 64, 0, 0, 0), loaded.transform());
        assertEquals(ArmPoseRule.Pose.DEFAULT, loaded.poses().idle().pose());
        assertEquals(0, loaded.poses().shotHoldTicks());
        assertThrows(IllegalArgumentException.class, () -> new HeldItemTransform(Float.NaN, 0, 0, 0, 0, 0));
    }

    @Test
    void settingsRoundTripAndPacketPreserveIndependentHandsAndCustomAxes()
    {
        HeldItemSettings customized = new HeldItemSettings(new HeldItemTransform(2, -3, 4, 30, 20, -10),
            WeaponPoseRules.DEFAULT.with(WeaponPoseRules.State.USING, new ArmPoseRule(ArmPoseRule.Pose.CUSTOM, 5, -80, 10, 25)));
        assertEquals(customized, HeldItemSettings.load(customized.save()));
        PacketBuffer buffer = new PacketBuffer(new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE));
        PacketBuffer copy = new PacketBuffer(new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE));
        try
        {
            new PacketHeldItemPresentation(42, customized, HeldItemSettings.DEFAULT, true).encodeInto(buffer);
            PacketHeldItemPresentation decoded = new PacketHeldItemPresentation();
            decoded.decodeInto(buffer);
            decoded.encodeInto(copy);
            buffer.readerIndex(0);
            assertEquals(buffer, copy);
        }
        finally
        {
            buffer.release();
            copy.release();
        }
    }

    @Test
    void statePriorityFollowsUsingFiringTargetingMovementAndIdle()
    {
        WeaponPoseRules rules = WeaponPoseRules.DEFAULT;
        for (WeaponPoseRules.State state : WeaponPoseRules.State.values())
            rules = rules.with(state, new ArmPoseRule(ArmPoseRule.Pose.CUSTOM, 1, state.ordinal() * 10, 0, 0));
        main.set(new HeldItemSettings(HeldItemTransform.IDENTITY, rules));
        assertEquals(0, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        when(holder.getDeltaMovement()).thenReturn(new Vec3(0.1, 0, 0));
        assertEquals(10, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        Mob target = mock(Mob.class);
        when(target.isAlive()).thenReturn(true);
        when(holder.getTarget()).thenReturn(target);
        assertEquals(20, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        when(level.getGameTime()).thenReturn(100L);
        GunArmPoses.recordShot(holder, InteractionHand.MAIN_HAND);
        assertEquals(30, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        when(holder.isUsingItem()).thenReturn(true);
        when(holder.getUsedItemHand()).thenReturn(InteractionHand.MAIN_HAND);
        assertEquals(40, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        when(holder.isUsingItem()).thenReturn(false);
        when(level.getGameTime()).thenReturn(120L);
        assertEquals(20, HeldItemPresentation.rule(holder, InteractionHand.MAIN_HAND).x());
        assertEquals(ArmPoseRule.DEFAULT, HeldItemPresentation.rule(holder, InteractionHand.OFF_HAND));
    }

    @Test
    void onlyExplicitCustomAxesMayOverrideAShieldEvenWhenAnimationsAreDisabled()
    {
        GunType type = mock(GunType.class);
        when(type.isShield()).thenReturn(true);
        GunItem shield = mock(GunItem.class);
        when(shield.getConfigType()).thenReturn(type);
        when(shield.useAimingAnimation()).thenReturn(true);
        ItemStack shieldStack = mock(ItemStack.class);
        when(shieldStack.getItem()).thenReturn(shield);
        when(holder.getOffhandItem()).thenReturn(shieldStack);
        when(((IEquipmentPolicy) holder).flansWeaponAnimations()).thenReturn(false);
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::entityAimPose).thenReturn(EnumEntityAimPose.DYNAMIC);
            for (ArmPoseRule.Pose pose : ArmPoseRule.Pose.values())
            {
                off.set(new HeldItemSettings(HeldItemTransform.IDENTITY, WeaponPoseRules.DEFAULT.with(WeaponPoseRules.State.IDLE, ArmPoseRule.of(pose))));
                assertEquals(GunArmPoses.Arm.ONE_ARM, GunArmPoses.resolve(holder).offHand());
            }
            off.set(new HeldItemSettings(HeldItemTransform.IDENTITY, WeaponPoseRules.DEFAULT.with(WeaponPoseRules.State.IDLE, new ArmPoseRule(ArmPoseRule.Pose.CUSTOM, 1, 35, 80, 90))));
            HumanoidModel<Mob> model = new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0), 64, 64).bakeRoot());
            model.head.yRot = 0.25F;
            EquipmentAnimationSupport.weapons(model, holder);
            assertEquals(35F * Mth.DEG_TO_RAD, model.leftArm.xRot, 1.0E-6F);
            assertEquals(0.25F, model.leftArm.yRot, 1.0E-6F);
            assertEquals(0F, model.leftArm.zRot, 1.0E-6F);
        }
    }

    @Test
    void apiUpdatesAddonStorageAndNotifiesClientsOnlyOnChanges()
    {
        HeldItemSettings customized = new HeldItemSettings(new HeldItemTransform(1, 2, 3, 0, 0, 0), WeaponPoseRules.DEFAULT);
        try (var packets = mockStatic(PacketHandler.class))
        {
            FlansItemAnimations.set(holder, InteractionHand.OFF_HAND, customized);
            assertEquals(customized, FlansItemAnimations.get(holder, InteractionHand.OFF_HAND));
            assertEquals(HeldItemSettings.DEFAULT, FlansItemAnimations.get(holder, InteractionHand.MAIN_HAND));
            FlansItemAnimations.set(holder, InteractionHand.OFF_HAND, customized);
            packets.verify(() -> PacketHandler.sendToTracking(any(PacketHeldItemPresentation.class), eq(holder)), times(1));
            FlansItemAnimations.reset(holder, InteractionHand.OFF_HAND);
            assertEquals(HeldItemSettings.DEFAULT, off.get());
            packets.verify(() -> PacketHandler.sendToTracking(any(PacketHeldItemPresentation.class), eq(holder)), times(2));
        }
    }

    @Test
    void npcPolicyFollowsEnforcedPlayerAimBeforeTheEntityFallback()
    {
        when(((IEquipmentPolicy) holder).flansmodultimateUsePlayerAimRules()).thenReturn(true);
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::playerAimPose).thenReturn(EnumPlayerAimPose.ENFORCED);
            config.when(ModCommonConfig::entityAimPose).thenReturn(EnumEntityAimPose.DYNAMIC);
            assertFalse(GunArmPoses.isDynamic(holder));
            config.when(ModCommonConfig::playerAimPose).thenReturn(EnumPlayerAimPose.DYNAMIC);
            config.when(ModCommonConfig::entityAimPose).thenReturn(EnumEntityAimPose.ENFORCED);
            assertTrue(GunArmPoses.isDynamic(holder));
            config.when(ModCommonConfig::playerAimPose).thenReturn(EnumPlayerAimPose.FREE_CHOICE);
            assertFalse(GunArmPoses.isDynamic(holder));
        }
    }

    @Test
    void customRenderedGrenadesUseTheSelectedHandTransformInFirstAndThirdPerson()
    {
        GrenadeItem grenade = mock(GrenadeItem.class);
        when(grenade.useCustomRenderer(any())).thenReturn(true);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(grenade);
        off.set(new HeldItemSettings(new HeldItemTransform(16, -8, 4, 0, 0, 90), WeaponPoseRules.DEFAULT));
        for (ItemDisplayContext context : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND})
        {
            PoseStack pose = new PoseStack();
            HeldItemTransforms.apply(pose, holder, stack, InteractionHand.OFF_HAND, context);
            Vector3f origin = pose.last().pose().transformPosition(new Vector3f());
            assertEquals(1F, origin.x, 1.0E-6F);
            assertEquals(-0.5F, origin.y, 1.0E-6F);
            assertEquals(0.25F, origin.z, 1.0E-6F);
            Vector3f direction = pose.last().pose().transformDirection(new Vector3f(1, 0, 0));
            assertEquals(0F, direction.x, 1.0E-6F);
            assertEquals(1F, direction.y, 1.0E-6F);
            PoseStack untouched = new PoseStack();
            HeldItemTransforms.apply(untouched, holder, stack, InteractionHand.MAIN_HAND, context);
            assertTrue(untouched.last().pose().equals(new PoseStack().last().pose(), 0F));
        }
    }
}
