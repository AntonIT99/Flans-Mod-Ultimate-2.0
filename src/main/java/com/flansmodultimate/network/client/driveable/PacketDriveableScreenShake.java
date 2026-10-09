package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A driveable near this player fired a gun that kicks the camera, as its {@code FancyScreenShake}
 * settings describe. The server picks who is close enough; the kick itself is purely visual.
 */
@NoArgsConstructor
public class PacketDriveableScreenShake implements IClientPacket
{
    private float intensity;
    private float durationSeconds;

    public PacketDriveableScreenShake(DriveableType.ScreenShake shake)
    {
        intensity = shake.intensity();
        durationSeconds = shake.durationSeconds();
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeFloat(intensity);
        data.writeFloat(durationSeconds);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        intensity = data.readFloat();
        durationSeconds = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.addVehicleScreenShake(intensity, durationSeconds);
    }
}
