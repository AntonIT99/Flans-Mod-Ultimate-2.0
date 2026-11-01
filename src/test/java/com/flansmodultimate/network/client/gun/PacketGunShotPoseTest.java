package com.flansmodultimate.network.client.gun;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.guns.GunRecoil;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.hooks.client.ClientGunHooksImpl;
import com.flansmodultimate.platform.client.ArmPosePlatform;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class PacketGunShotPoseTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @ParameterizedTest
    @CsvSource({"MAIN_HAND, true", "OFF_HAND, true", "MAIN_HAND, false", "OFF_HAND, false"})
    @SuppressWarnings("ResultOfMethodCallIgnored") // Mockito records the getter invocation when stubbing the static recoil state.
    void everyConfirmedShotKicksEvenWhenStartAndStopArriveBeforeTheNextTick(InteractionHand hand, boolean fancy)
    {
        Player player = mock(Player.class);
        Level level = mock(Level.class);
        UUID id = UUID.randomUUID();
        when(player.getUUID()).thenReturn(id);
        when(player.getEyePosition(0F)).thenReturn(Vec3.ZERO);
        when(level.getEntity(42)).thenReturn(player);
        when(level.getPlayerByUUID(id)).thenReturn(player);
        GunItem gun = mock(GunItem.class);
        GunType type = mock(GunType.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(gun);
        when(player.getItemInHand(hand)).thenReturn(stack);
        when(gun.getConfigType()).thenReturn(type);
        when(type.isUseFancyRecoil()).thenReturn(fancy);
        when(type.getRecoilPitch(stack, null)).thenReturn(4F);
        when(type.getRecoilYaw(stack, null)).thenReturn(2F);
        GunRecoil shotRecoil = new GunRecoil(4F, 0F, 1F, 1F, 1F, 0F, 1F, 0F);
        when(type.getRecoil(stack)).thenReturn(shotRecoil);
        GunRecoil recoil = mock(GunRecoil.class);
        GunAnimations animations = mock(GunAnimations.class);
        ClientGunHooksImpl hooks = new ClientGunHooksImpl();

        // Plain JUnit does not apply NeoForge's enum extensions, which ModClient's arm poses read through ArmPosePlatform.
        try (var armPoses = mockStatic(ArmPosePlatform.class);
            var client = mockStatic(ModClient.class);
            var models = mockStatic(ModelCache.class);
            var poses = mockStatic(GunArmPoses.class);
            var utils = mockStatic(ModUtils.class))
        {
            client.when(ModClient::getPlayerRecoil).thenReturn(recoil);
            client.when(() -> ModClient.getGunAnimations(player, hand)).thenReturn(animations);
            PlayerData data = PlayerData.getClientInstance(id);
            data.setShootTime(hand, 20F);
            new PacketGunShootClient(id, hand, true).handleClientSide(player, level);
            new PacketGunShotPose(42, hand).handleShot(player, level, hooks);
            new PacketGunShootClient(id, hand, false).handleClientSide(player, level);
            assertFalse(data.isShooting(hand));
            if (fancy)
                verify(recoil).addRecoil(shotRecoil);
            else
            {
                client.verify(() -> ModClient.setPlayerRecoilPitch(4F));
                client.verify(() -> ModClient.setPlayerRecoilYaw(2F));
            }

            // Multiple confirmed rounds in one client tick must each kick, even during a local cooldown.
            new PacketGunShotPose(42, hand).handleShot(player, level, hooks);
            new PacketGunShotPose(42, hand).handleShot(player, level, hooks);
            if (fancy)
                verify(recoil, times(3)).addRecoil(shotRecoil);
            else
            {
                client.verify(() -> ModClient.setPlayerRecoilPitch(4F), times(3));
                client.verify(() -> ModClient.setPlayerRecoilYaw(2F), times(3));
            }
            verify(animations, times(3)).doShoot(0, 1, 0, 0F, 0F, 0);

            // Pose-only updates are not additional shots.
            new PacketGunShotPose(42, hand, false).handleShot(player, level, hooks);
            verifyNoMoreInteractions(recoil);
        }
    }
}
