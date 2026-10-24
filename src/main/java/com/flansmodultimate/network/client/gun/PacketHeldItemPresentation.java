package com.flansmodultimate.network.client.gun;

import com.flansmodultimate.api.equipment.HeldItemSettings;
import com.flansmodultimate.common.guns.HeldItemPresentation;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Server-authored held-item settings and target state; no client-to-server mutation path. */
@NoArgsConstructor
public final class PacketHeldItemPresentation implements IClientPacket
{
    private int entityId;
    private HeldItemSettings main;
    private HeldItemSettings off;
    private boolean targeting;

    public PacketHeldItemPresentation(int entityId, HeldItemSettings main, HeldItemSettings off, boolean targeting)
    {
        this.entityId = entityId;
        this.main = main;
        this.off = off;
        this.targeting = targeting;
    }

    @Override
    public void encodeInto(PacketBuffer data)
    {
        data.writeVarInt(entityId);
        data.writeNbt(main.save());
        data.writeNbt(off.save());
        data.writeBoolean(targeting);
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        entityId = data.readVarInt();
        CompoundTag mainTag = data.readNbt();
        CompoundTag offTag = data.readNbt();
        main = mainTag == null ? HeldItemSettings.DEFAULT : HeldItemSettings.load(mainTag);
        off = offTag == null ? HeldItemSettings.DEFAULT : HeldItemSettings.load(offTag);
        targeting = data.readBoolean();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        if (level.getEntity(entityId) instanceof LivingEntity holder)
            HeldItemPresentation.receive(holder, main, off, targeting);
    }
}
