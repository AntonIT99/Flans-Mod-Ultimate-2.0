package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Announces the player's own reload inventory preferences to the server. The client sends this on login and
 * whenever the preferences change, so that both requested and automatic reloads use them.
 * The server still has the final say on combining ammo.
 */
@NoArgsConstructor
public class PacketReloadPreferences implements IServerPacket
{
    private boolean combineAmmoOnReload;
    private boolean ammoToUpperInventoryOnReload;

    public PacketReloadPreferences(boolean combineAmmoOnReload, boolean ammoToUpperInventoryOnReload)
    {
        this.combineAmmoOnReload = combineAmmoOnReload;
        this.ammoToUpperInventoryOnReload = ammoToUpperInventoryOnReload;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeBoolean(combineAmmoOnReload);
        data.writeBoolean(ammoToUpperInventoryOnReload);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        combineAmmoOnReload = data.readBoolean();
        ammoToUpperInventoryOnReload = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        PlayerData data = PlayerData.getInstance(player);
        data.setCombineAmmoOnReloadPreference(combineAmmoOnReload);
        data.setAmmoToUpperInventoryOnReloadPreference(ammoToUpperInventoryOnReload);
    }
}
