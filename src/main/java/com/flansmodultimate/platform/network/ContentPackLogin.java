package com.flansmodultimate.platform.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.sync.ContentFingerprint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Sends the pack manifest before Forge's registry snapshots; it is diagnostic and requires no response. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPackLogin
{
    public static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "pack_manifest");
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(CHANNEL_ID).networkProtocolVersion(() -> "1").clientAcceptedVersions("1"::equals)
        .serverAcceptedVersions("1"::equals).simpleChannel();

    private record Manifest(Map<String, String> fingerprints)
    {}

    public static void register()
    {
        CHANNEL.messageBuilder(Manifest.class, 0, NetworkDirection.LOGIN_TO_CLIENT).encoder((packet, buffer) -> ContentPackDiagnostics.encode(buffer, packet.fingerprints()))
            .decoder(buffer -> new Manifest(ContentPackDiagnostics.decode(buffer)))
            .buildLoginPacketList(local -> local ? List.of() : List.of(Pair.of("Flan's content pack manifest", new Manifest(ContentFingerprint.get())))).noResponse()
            .consumerNetworkThread((packet, context) ->
            {
                ContentPackDiagnostics.receive(context.get().getNetworkManager(), packet.fingerprints());
                context.get().setPacketHandled(true);
            }).add();
    }
}
