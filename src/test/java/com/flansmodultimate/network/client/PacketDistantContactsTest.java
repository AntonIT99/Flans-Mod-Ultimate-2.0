package com.flansmodultimate.network.client;

import com.flansmodultimate.network.IPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketDistantContactsTest
{
    @Test
    void contactsSurviveARoundTrip()
    {
        PacketDistantContacts.Contact plane = new PacketDistantContacts.Contact(42, "spitfire", 3, 1500.25D, 180D, -320.5D,
            2.5F, -0.125F, 0.75F, 91F, -12.5F, 30F, new int[] {4, 9});
        PacketDistantContacts.Contact tank = new PacketDistantContacts.Contact(7, "tiger", 0, -800D, 64D, 2048D,
            0F, 0F, 0F, 180F, 0F, 0F, new int[0]);
        PacketDistantContacts original = new PacketDistantContacts(5, List.of(plane, tank));

        byte[] bytes = encode(original);
        PacketDistantContacts decoded = new PacketDistantContacts();
        decoded.decodeInto(buffer(bytes));

        assertEquals(5, decoded.getUpdateInterval());
        assertEquals(2, decoded.getContacts().size());
        PacketDistantContacts.Contact first = decoded.getContacts().get(0);
        assertEquals(42, first.entityId());
        assertEquals("spitfire", first.shortName());
        assertEquals(3, first.paintjobId());
        assertEquals(1500.25D, first.x());
        assertEquals(-0.125F, first.velocityY());
        assertEquals(30F, first.roll());
        assertArrayEquals(new int[] {4, 9}, first.destroyedParts());
        assertEquals(0, decoded.getContacts().get(1).destroyedParts().length);
        assertArrayEquals(bytes, encode(decoded));
    }

    @Test
    void anEmptyUpdateClearsTheClientsContacts()
    {
        PacketDistantContacts decoded = new PacketDistantContacts();
        decoded.decodeInto(buffer(encode(new PacketDistantContacts(10, List.of()))));
        assertTrue(decoded.getContacts().isEmpty());
        assertEquals(10, decoded.getUpdateInterval());
    }

    @Test
    void anImplausibleContactCountIsRejected()
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeVarInt(5);
        buf.writeVarInt(1_000_000);
        assertThrows(IllegalStateException.class, () -> new PacketDistantContacts().decodeInto(new PacketBuffer(buf)));
    }

    @Test
    void distantExplosionsSurviveARoundTrip()
    {
        PacketDistantExplosion original = new PacketDistantExplosion(new Vec3(1200.5D, 70D, -64D), 6F, 14F, true);
        PacketDistantExplosion decoded = new PacketDistantExplosion();
        decoded.decodeInto(buffer(encode(original)));

        assertEquals(original.getCenter(), decoded.getCenter());
        assertEquals(6F, decoded.getExplosionRadius());
        assertEquals(14F, decoded.getBlastRadius());
        assertTrue(decoded.isFiery());
    }

    private static PacketBuffer buffer(byte[] bytes)
    {
        return new PacketBuffer(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), RegistryAccess.EMPTY));
    }

    private static byte[] encode(IPacket packet)
    {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        packet.encodeInto(new PacketBuffer(buffer));
        return ByteBufUtil.getBytes(buffer);
    }
}
