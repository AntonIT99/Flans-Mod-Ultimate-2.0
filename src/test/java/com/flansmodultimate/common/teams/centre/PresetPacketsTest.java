package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.network.IPacket;
import com.flansmodultimate.network.client.teams.PacketPresetResult;
import com.flansmodultimate.network.server.teams.PacketPresetAction;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.platform.network.TestPacketBuffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetPacketsTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void requestsAndRepliesPreserveUploadedSettingsAndBuiltInMetadata()
    {
        roundTrip(new PacketPresetAction(42, SettingsPresets.TEAMS, "import", "example", "all", "{\"name\":\"日本語\"}", true), new PacketPresetAction());
        roundTrip(new PacketPresetResult(43, false, "{}", List.of("arena", "example"), List.of("arena"), List.of(Component.literal("before → after")), Component.literal("done")),
            new PacketPresetResult());
    }

    private static void roundTrip(IPacket original, IPacket decoded)
    {
        PacketBuffer first = new PacketBuffer(TestPacketBuffers.buffer());
        PacketBuffer second = new PacketBuffer(TestPacketBuffers.buffer());
        try
        {
            original.encodeInto(first);
            byte[] expected = new byte[first.readableBytes()];
            first.getBytes(0, expected);
            decoded.decodeInto(first);
            assertFalse(first.isReadable());
            decoded.encodeInto(second);
            byte[] actual = new byte[second.readableBytes()];
            second.readBytes(actual);
            assertArrayEquals(expected, actual);
        }
        finally
        {
            first.release();
            second.release();
        }
    }
}
