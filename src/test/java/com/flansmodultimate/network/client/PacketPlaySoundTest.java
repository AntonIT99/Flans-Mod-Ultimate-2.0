package com.flansmodultimate.network.client;

import com.flansmodultimate.network.PacketBuffer;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketPlaySoundTest
{
    /** Not a valid resource path, so it takes the text fallback without touching the sound registry. */
    private static final String TEXT_SOUND = "Not A Registered Sound";

    @Test
    void cancellableAndPlainSoundsSurviveARoundTrip()
    {
        UUID instance = UUID.randomUUID();
        for (boolean cancellable : new boolean[] {false, true})
        {
            for (boolean distort : new boolean[] {false, true})
            {
                PacketPlaySound original = new PacketPlaySound(new Vec3(1.5D, 64D, -3.25D), 48D, TEXT_SOUND,
                    distort, !distort, cancellable, instance, null);
                byte[] first = encode(original);
                PacketPlaySound decoded = new PacketPlaySound();
                decoded.decodeInto(new PacketBuffer(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(first), RegistryAccess.EMPTY)));
                // A plain sound keeps only the pitch seed; encoding it again must give the same bytes.
                assertArrayEquals(first, encode(decoded));
            }
        }
    }

    @Test
    void plainSoundsNoLongerCarryAFullInstanceId()
    {
        UUID instance = UUID.randomUUID();
        int plain = encode(new PacketPlaySound(Vec3.ZERO, 16D, TEXT_SOUND, true, false, false, instance, null)).length;
        int cancellable = encode(new PacketPlaySound(Vec3.ZERO, 16D, TEXT_SOUND, true, false, true, instance, null)).length;
        assertTrue(plain < cancellable);
    }

    private static byte[] encode(PacketPlaySound packet)
    {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        packet.encodeInto(new PacketBuffer(buffer));
        return ByteBufUtil.getBytes(buffer);
    }
}
