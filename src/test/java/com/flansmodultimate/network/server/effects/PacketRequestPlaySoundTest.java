package com.flansmodultimate.network.server.effects;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.GunSoundPermissions.Kind;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PacketRequestPlaySoundTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void registeredAnimationIntentRoundTripsAndBroadcastsOnlyAfterAnAcceptedShot()
    {
        assertTrue(PacketHandler.serverPacketTypes().contains(PacketRequestPlaySound.class));
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.isAlive()).thenReturn(true);
        when(player.position()).thenReturn(new Vec3(12, 64, 8));
        GunType type = mock(GunType.class);
        when(type.getActionSound()).thenReturn("bolt");
        GunItem gun = mock(GunItem.class);
        when(gun.getConfigType()).thenReturn(type);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(gun);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);
        PacketRequestPlaySound decoded = new PacketRequestPlaySound();
        var buffer = TestPacketBuffers.buffer();
        try
        {
            new PacketRequestPlaySound(InteractionHand.MAIN_HAND, Kind.ACTION).encodeInto(new PacketBuffer(buffer));
            decoded.decodeInto(new PacketBuffer(buffer));
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
        var dimension = level.dimension();
        Vec3 position = player.position();
        CommonConfigSnapshot snapshot = mock(CommonConfigSnapshot.class);
        when(snapshot.gunActionSoundRange()).thenReturn(24F);
        try (var packets = mockStatic(PacketHandler.class); var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::get).thenReturn(snapshot);
            decoded.handleServerSide(player, level);
            packets.verifyNoInteractions();
            PlayerData.getInstance(player).getGunSoundPermissions().shot(InteractionHand.MAIN_HAND, stack, 0);
            decoded.handleServerSide(player, level);
            decoded.handleServerSide(player, level);
            packets.verify(() -> PacketHandler.sendToAllExcept(eq(dimension), eq(position), eq(24D), eq(player), any(PacketPlaySound.class)), times(1));
        }
    }
}
