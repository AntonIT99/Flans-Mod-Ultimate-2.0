package com.flansmodultimate.network.client;

import com.flansmodultimate.client.CommonConfigMirror;
import com.flansmodultimate.client.gui.options.FlansOptionsScreen;
import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * The server's answer to {@code PacketRequestCommonConfig}: the common config entries the options screen
 * can edit, and whether this player is allowed to change them. It is also sent after every change, so an
 * open screen always shows the values that are really in force.
 */
@NoArgsConstructor
public class PacketCommonConfigValues implements IClientPacket
{
    private Map<String, Object> values;
    private boolean mayEdit;

    public PacketCommonConfigValues(Map<String, Object> values, boolean mayEdit)
    {
        this.values = values;
        this.mayEdit = mayEdit;
    }

    @Override
    public void encodeInto(FriendlyByteBuf buf)
    {
        ConfigSpecValues.write(buf, values);
        buf.writeBoolean(mayEdit);
    }

    @Override
    public void decodeInto(FriendlyByteBuf buf)
    {
        values = ConfigSpecValues.read(buf);
        mayEdit = buf.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        CommonConfigMirror.accept(values, mayEdit);
        FlansOptionsScreen.onServerConfigSynced();
    }
}
