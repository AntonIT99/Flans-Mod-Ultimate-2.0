package com.flansmodultimate.network.client;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.PacketIO;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bounded S2C snapshot of inventory slots that affect a driveable's model. */
public final class PacketDriveableRenderState implements IClientPacket
{
    private int driveableId;
    private int paintjobId;
    private int[] slots = new int[0];
    private ItemStack[] stacks = new ItemStack[0];

    public PacketDriveableRenderState() {}

    public PacketDriveableRenderState(@NotNull Driveable driveable)
    {
        driveableId = driveable.getId();
        DriveableData data = driveable.getDriveableData();
        if (data == null)
            return;
        paintjobId = data.getPaintjobID();
        int count = data.getRenderSlotCount();
        slots = new int[count];
        stacks = new ItemStack[count];
        for (int index = 0; index < count; index++)
        {
            slots[index] = data.getRenderSlotIndex(index);
            stacks[index] = data.getItem(slots[index]).copy();
        }
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeVarInt(driveableId);
        buffer.writeVarInt(paintjobId);
        buffer.writeVarInt(slots.length);
        for (int index = 0; index < slots.length; index++)
        {
            buffer.writeVarInt(slots[index]);
            PacketIO.writeItem(buffer, stacks[index]);
        }
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf buffer)
    {
        driveableId = buffer.readVarInt();
        paintjobId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > DriveableData.MAX_RENDER_SYNC_SLOTS)
            throw new IllegalArgumentException("Invalid driveable render slot count " + count);
        slots = new int[count];
        stacks = new ItemStack[count];
        for (int index = 0; index < count; index++)
        {
            slots[index] = buffer.readVarInt();
            stacks[index] = PacketIO.readItem(buffer);
        }
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        Entity entity = level.getEntity(driveableId);
        if (entity instanceof Driveable driveable)
            driveable.applyRenderInventoryNetworkState(paintjobId, slots, stacks);
    }
}
