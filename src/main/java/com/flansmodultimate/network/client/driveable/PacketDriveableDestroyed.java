package com.flansmodultimate.network.client.driveable;

import com.flansmodultimate.common.driveables.damage.WreckLoad;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A driveable about to be removed for good, sent just before its removal so that those watching still see it go.
 *
 * <p>
 * A driveable that becomes a wreck announces its death through its synchronised wreck age. One removed at once,
 * because wrecks are off or its type leaves none, and a wreck blown apart by explosives, are gone before that could
 * reach anyone, so they are announced with this instead.
 * </p>
 */
@NoArgsConstructor
public class PacketDriveableDestroyed implements IClientPacket
{
    private int driveableId;
    /** Whether a wreck was blown apart, rather than a working driveable destroyed. */
    private boolean brokenUp;
    private int load;

    public PacketDriveableDestroyed(int driveableId, boolean brokenUp, int load)
    {
        this.driveableId = driveableId;
        this.brokenUp = brokenUp;
        this.load = load;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(driveableId);
        data.writeBoolean(brokenUp);
        data.writeVarInt(load);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        driveableId = data.readVarInt();
        brokenUp = data.readBoolean();
        load = data.readVarInt();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (!(level.getEntity(driveableId) instanceof Driveable driveable))
            return;
        if (brokenUp)
            ClientHooks.RENDER.onDriveableBrokenUp(driveable);
        else
        {
            driveable.announceDeathLoad(WreckLoad.unpack(load));
            ClientHooks.RENDER.onDriveableWrecked(driveable);
        }
    }
}
