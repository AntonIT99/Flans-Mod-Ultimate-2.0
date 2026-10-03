package com.flansmodultimate.network.server;

import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PacketDriveableInputTest
{
    @Test
    void shootPointPitchPivotsSurviveARoundTripInBankOrder()
    {
        // The Warfare 44 M4A3E8: main gun on the trunnion, then coaxial and two roof guns.
        Vec3[] primary = { new Vec3(0.875D, 1.75D, 0D) };
        Vec3[] secondary = { new Vec3(0.875D, 1.75D, 0D), new Vec3(0.1875D, 2.46875D, -0.125D), null };
        PacketDriveableInput original = new PacketDriveableInput(7, 0, 12F, -4F, 0F, 0F, false,
            new Vec3(0.875D, 1.75D, 0D), 3).withShootPointPitchPivots(primary, secondary);

        byte[] bytes = encode(original);
        PacketDriveableInput decoded = new PacketDriveableInput();
        decoded.decodeInto(new PacketBuffer(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))));

        assertArrayEquals(primary, decoded.getPrimaryPitchPivots());
        assertEquals(3, decoded.getSecondaryPitchPivots().length);
        assertEquals(secondary[1], decoded.getSecondaryPitchPivots()[1]);
        assertNull(decoded.getSecondaryPitchPivots()[2], "a point on no barrel section keeps the main gun's pivot");
        assertArrayEquals(bytes, encode(decoded));
    }

    @Test
    void anInputWithoutPivotsDecodesToEmptyBanks()
    {
        PacketDriveableInput decoded = new PacketDriveableInput();
        decoded.decodeInto(new PacketBuffer(new FriendlyByteBuf(Unpooled.wrappedBuffer(
            encode(new PacketDriveableInput(7, 0, 0F, 0F, 0F, 0F, false, 1))))));

        assertEquals(0, decoded.getPrimaryPitchPivots().length);
        assertEquals(0, decoded.getSecondaryPitchPivots().length);
    }

    @Test
    void anImplausiblePivotCountIsRejected()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        PacketBuffer data = new PacketBuffer(buffer);
        new PacketDriveableInput(7, 0, 0F, 0F, 0F, 0F, false, 1).encodeInto(data);
        // Replace the empty primary bank count with a hostile one.
        buffer.writerIndex(buffer.writerIndex() - 2);
        data.writeVarInt(1_000_000);

        PacketDriveableInput decoded = new PacketDriveableInput();
        assertThrows(DecoderException.class, () -> decoded.decodeInto(new PacketBuffer(buffer)));
    }

    private static byte[] encode(PacketDriveableInput packet)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encodeInto(new PacketBuffer(buffer));
        return ByteBufUtil.getBytes(buffer);
    }
}
