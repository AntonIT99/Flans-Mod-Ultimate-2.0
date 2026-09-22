package com.flansmodultimate.network;

import com.flansmodultimate.common.FlanParticles;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleNameCodecTest
{
    @Test
    void everyKnownNameRoundTripsAsASmallId()
    {
        for (String name : FlanParticles.sortedNames())
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            ParticleNameCodec.write(buffer, name);
            assertTrue(buffer.readableBytes() <= 2, name + " should be written as a varint id");
            assertEquals(name, ParticleNameCodec.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void unknownNamesFallBackToText()
    {
        for (String name : new String[] {"blockcrack_minecraft:stone", "FlansMod.FMSmoke", ""})
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            ParticleNameCodec.write(buffer, name);
            assertEquals(name, ParticleNameCodec.read(buffer));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void knownNameIsSmallerThanText()
    {
        FriendlyByteBuf id = new FriendlyByteBuf(Unpooled.buffer());
        ParticleNameCodec.write(id, FlanParticles.FM_SMOKE);
        FriendlyByteBuf text = new FriendlyByteBuf(Unpooled.buffer());
        text.writeUtf(FlanParticles.FM_SMOKE);
        assertTrue(id.readableBytes() < text.readableBytes());
    }
}
