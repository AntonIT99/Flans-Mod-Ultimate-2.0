package com.flansmodultimate.platform.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.sync.ContentFingerprint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/** Sends the server's pack manifest before NeoForge's frozen-registry synchronization. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPackLogin
{
    private record Manifest(Map<String, String> fingerprints) implements CustomPacketPayload
    {
        private static final Type<Manifest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "pack_manifest"));
        private static final StreamCodec<FriendlyByteBuf, Manifest> CODEC = StreamCodec.of((buffer, packet) -> ContentPackDiagnostics.encode(buffer, packet.fingerprints()),
            buffer -> new Manifest(ContentPackDiagnostics.decode(buffer)));

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event)
    {
        event.registrar("1").executesOn(HandlerThread.NETWORK).configurationToClient(Manifest.TYPE, Manifest.CODEC,
            (packet, context) -> ContentPackDiagnostics.receive(context.connection(), packet.fingerprints()));
    }

    public static void send(Consumer<CustomPacketPayload> sender)
    {
        sender.accept(new Manifest(ContentFingerprint.get()));
    }
}
