package com.flansmodultimate.dev;

import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;

/** Exports the running test server's payload registrations for headless observers; loaded only by the optional development run. */
@Mod("flansmineflayerdev")
public final class MineflayerDevelopment
{
    @SuppressWarnings("java:S1118") // NeoForge instantiates this mod entry point.
    public MineflayerDevelopment()
    {
        NeoForge.EVENT_BUS.addListener(MineflayerDevelopment::serverStarted);
    }

    @SuppressWarnings("java:S3011") // NeoForge exposes no public enumeration API; reflection is confined to this optional development module.
    private static void serverStarted(ServerStartedEvent event)
    {
        String destination = System.getProperty("fmu.mineflayer.runtime");
        if (destination == null)
            return;

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            // NeoForge has no public registration enumeration API. Keep this version-specific reflection in the development module.
            Field field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked") // The private field's declared type matches this pinned NeoForge version.
            Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> registrations = (Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>>) field.get(null);
            ModdedNetworkQueryPayload.STREAM_CODEC.encode(buffer, ModdedNetworkQueryPayload.fromRegistry(registrations));
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);

            JsonObject runtime = new JsonObject();
            runtime.addProperty("minecraftVersion", event.getServer().getServerVersion());
            runtime.addProperty("loader", "neoforge");
            runtime.addProperty("port", event.getServer().getPort());
            runtime.addProperty("pid", ProcessHandle.current().pid());
            runtime.addProperty("payloadQuery", Base64.getEncoder().encodeToString(bytes));
            Path output = Path.of(destination);
            Files.createDirectories(output.getParent());
            Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
            Files.writeString(temporary, runtime.toString());
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (ReflectiveOperationException | IOException e)
        {
            throw new IllegalStateException("Cannot export the Mineflayer test server's payload registrations", e);
        }
        finally
        {
            buffer.release();
        }
    }
}
