package com.flansmodultimate.network.client.teams;

import com.flansmodultimate.common.teams.PlayerClassSkin;
import com.flansmodultimate.platform.network.PacketBuffer;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketPlayerClassSkinsTest
{
    @Test
    void playersOfTheSameClassKeepTheirIndividualSkinChoicesAcrossTheWire()
    {
        Map<UUID, PlayerClassSkin> choices = Map.of(UUID.randomUUID(), new PlayerClassSkin("rifleman", 0, "flansmod:textures/skins/first.png"), UUID.randomUUID(),
            new PlayerClassSkin("rifleman", 2, "flansmod:textures/skins/old_life.png"));
        assertEquals(choices, roundTrip(choices));
        assertEquals(Map.of(), roundTrip(Map.of()));
    }

    private static Map<UUID, PlayerClassSkin> roundTrip(Map<UUID, PlayerClassSkin> choices)
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            new PacketPlayerClassSkins(choices).encodeInto(new PacketBuffer(buffer));
            PacketPlayerClassSkins decoded = new PacketPlayerClassSkins();
            decoded.decodeInto(new PacketBuffer(buffer));
            decoded.encodeInto(new PacketBuffer(output));
            int count = output.readVarInt();
            Map<UUID, PlayerClassSkin> result = new HashMap<>();
            for (int i = 0; i < count; i++)
                result.put(output.readUUID(), new PlayerClassSkin(output.readUtf(), output.readVarInt(), output.readUtf(256)));
            assertEquals(0, buffer.readableBytes());
            assertEquals(0, output.readableBytes());
            return result;
        }
        finally
        {
            buffer.release();
            output.release();
        }
    }
}
