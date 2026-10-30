package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.network.BoardSeatPacket;
import io.netty.buffer.Unpooled;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BoardSeatPacketTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void seatPickingAndItsPacketLoadWithoutClientClasses() throws Exception
    {
        URL classes = BoardSeatPacket.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, getClass().getClassLoader())
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("com.mojang.blaze3d.") || name.startsWith("com.wolffsmod.npcs.client."))
                    throw new ClassNotFoundException("Client classes are unavailable on a dedicated server: " + name);
                if (name.startsWith(NpcSeatInteraction.class.getName()) || name.equals(BoardSeatPacket.class.getName()))
                {
                    synchronized (getClassLoadingLock(name))
                    {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null)
                            loaded = findClass(name);
                        if (resolve)
                            resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        })
        {
            for (String name : new String[]{NpcSeatInteraction.class.getName(), BoardSeatPacket.class.getName()})
                assertDoesNotThrow(() -> Class.forName(name, true, loader).getDeclaredMethods());
        }
    }

    @Test
    void packetRoundTripsEntityAndHand()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            BoardSeatPacket packet = new BoardSeatPacket(321, InteractionHand.OFF_HAND);
            packet.write(buffer);
            assertEquals(packet, BoardSeatPacket.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void serverRequiresAnUnoccupiedSeatUnderItsOwnRayBeforeRunningNormalInteraction()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        WeaponMount seat = mock(WeaponMount.class);
        when(seat.seat()).thenReturn(3);
        when(player.level()).thenReturn(level);
        when(player.isAlive()).thenReturn(true);
        when(vehicle.isAlive()).thenReturn(true);
        when(level.getEntity(12)).thenReturn(vehicle);
        BoardSeatPacket packet = new BoardSeatPacket(12, InteractionHand.MAIN_HAND);
        try (var interactions = mockStatic(NpcSeatInteraction.class); var crew = mockStatic(NpcCrew.class))
        {
            // A valid entity id is not proof that a seat is in sight and reach.
            packet.handle(player);
            verify(player, never()).interactOn(any(), any());
            interactions.when(() -> NpcSeatInteraction.pick(vehicle, player)).thenReturn(Optional.of(new NpcSeatInteraction.Hit(seat, Vec3.ZERO)));
            crew.when(() -> NpcCrew.occupied(vehicle, 3)).thenReturn(true);
            packet.handle(player);
            verify(player, never()).interactOn(any(), any());
            crew.when(() -> NpcCrew.occupied(vehicle, 3)).thenReturn(false);
            packet.handle(player);
            verify(player).interactOn(vehicle, InteractionHand.MAIN_HAND);
            when(player.isSpectator()).thenReturn(true);
            packet.handle(player);
            verify(player, times(1)).interactOn(vehicle, InteractionHand.MAIN_HAND);
        }
    }
}
