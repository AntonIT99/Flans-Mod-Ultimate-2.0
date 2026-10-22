package com.flansmodultimate.platform.network;

import com.flansmodultimate.common.sync.EnumContentMismatch;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentPackDiagnosticsTest
{
    @Test
    void hugeRegistryReasonIsReplacedEvenWithoutAManifest()
    {
        var channel = new EmbeddedChannel();
        try
        {
            Connection connection = mock(Connection.class);
            when(connection.channel()).thenReturn(channel);
            Component raw = Component.translatable("neoforge.network.registries.sync.server-with-unknown-keys", "missing:item ".repeat(10000));
            Component reason = ContentPackDiagnostics.disconnectReason(connection, raw);
            assertEquals("disconnect.flansmodultimate.content_mismatch", ((TranslatableContents) reason.getContents()).getKey());
            assertFalse(reason.getString().contains("missing:item"));
            assertTrue(reason.getSiblings().isEmpty());
        }
        finally
        {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void manifestNamesAreBoundedAndConnectionSpecific()
    {
        var channel = new EmbeddedChannel();
        var otherChannel = new EmbeddedChannel();
        try
        {
            Connection connection = mock(Connection.class);
            Connection other = mock(Connection.class);
            when(connection.channel()).thenReturn(channel);
            when(other.channel()).thenReturn(otherChannel);
            Map<String, String> manifest = new TreeMap<>();
            for (int i = 0; i < 500; i++)
                manifest.put("Pack " + i + " very long name ".repeat(20), "a".repeat(64));
            ContentPackDiagnostics.receive(connection, manifest);
            Component raw = Component.literal("Failed to synchronize registry data from server, closing connection");
            Component reason = ContentPackDiagnostics.disconnectReason(connection, raw);
            var summary = (TranslatableContents) reason.getSiblings().get(reason.getSiblings().size() - 1).getContents();
            assertEquals(500, summary.getArgs()[0]);
            String names = (String) summary.getArgs()[1];
            assertTrue(names.length() <= 105, names);
            assertTrue(names.startsWith("Pack 0"));
            assertTrue(names.endsWith("…"));
            assertTrue(ContentPackDiagnostics.disconnectReason(other, raw).getSiblings().isEmpty());
        }
        finally
        {
            channel.finishAndReleaseAll();
            otherChannel.finishAndReleaseAll();
        }
    }

    @Test
    void otherDisconnectReasonsArePreserved()
    {
        Connection connection = mock(Connection.class);
        for (Component reason : new Component[]{Component.literal("Banned"), Component.translatable("disconnect.timeout"), Component.translatable("disconnect.loginFailedInfo.invalidSession")})
            assertSame(reason, ContentPackDiagnostics.disconnectReason(connection, reason));
        verifyNoInteractions(connection);
    }

    @Test
    void namesDoNotInjectExtraLinesOrFormatting()
    {
        Component reason = ContentPackDiagnostics.message(Map.of("Pack\n\r§aInjected\tName", EnumContentMismatch.DIFFERENT));
        var summary = (TranslatableContents) reason.getSiblings().get(reason.getSiblings().size() - 1).getContents();
        String names = (String) summary.getArgs()[1];
        assertFalse(names.contains("\n"));
        assertFalse(names.contains("\r"));
        assertFalse(names.contains("§"));
    }

    @Test
    void manifestCodecRoundTripsAndRejectsInvalidCounts()
    {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            Map<String, String> packs = Map.of("Modern Warfare", "a".repeat(64), "WW2", "b".repeat(64));
            ContentPackDiagnostics.encode(buffer, packs);
            assertEquals(packs, ContentPackDiagnostics.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(-1);
            assertThrows(DecoderException.class, () -> ContentPackDiagnostics.decode(buffer));
            buffer.clear();
            buffer.writeVarInt(1025);
            assertThrows(DecoderException.class, () -> ContentPackDiagnostics.decode(buffer));
        }
        finally
        {
            buffer.release();
        }
    }
}
