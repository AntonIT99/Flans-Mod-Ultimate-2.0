package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.crew.CrewSource;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Read-only clone previews for the crew picker; the request token keeps late replies out of another selection. */
public record CrewClonePreviewPacket(int entityId, int tab, String name, UUID token, boolean request, CompoundTag data)
{
    private static final int MAX_NAME_LENGTH = 256;

    public static CrewClonePreviewPacket request(int entityId, int tab, String name, UUID token)
    {
        return new CrewClonePreviewPacket(entityId, tab, name, token, true, new CompoundTag());
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(tab);
        buffer.writeUtf(name, MAX_NAME_LENGTH);
        buffer.writeUUID(token);
        buffer.writeBoolean(request);
        // Requests carry no entity data. Only the server supplies the preview.
        if (!request)
            buffer.writeNbt(data);
    }

    public static CrewClonePreviewPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        int tab = buffer.readVarInt();
        String name = buffer.readUtf(MAX_NAME_LENGTH);
        UUID token = buffer.readUUID();
        boolean request = buffer.readBoolean();
        CompoundTag data = request ? new CompoundTag() : buffer.readNbt();
        return new CrewClonePreviewPacket(entityId, tab, name, token, request, data == null ? new CompoundTag() : data);
    }

    /** Reads only a listed clone, for a builder holding the wand while editing this NPC. Never spawns an entity. */
    public void handle(ServerPlayer player)
    {
        EntityNPCInterface npc = NoppesUtilServer.getEditingNpc(player);
        if (!request || npc == null || npc.getId() != entityId || !npc.isAlive() || npc.level() != player.level() || player.getInventory().getSelected().getItem() != CustomItems.wand
            || !CustomNpcsPermissions.hasPermission(player, CustomNpcsPermissions.NPC_CLONE))
            return;
        CompoundTag clone = null;
        ServerCloneController clones = ServerCloneController.Instance;
        // Checking membership before the file lookup also prevents arbitrary paths supplied as clone names.
        if (tab >= 1 && tab <= CrewSource.MAX_CLONE_TAB && clones != null && clones.getClones(tab).contains(name))
            clone = clones.getCloneData(null, name, tab);
        NpcNetwork.sendTo(player, new CrewClonePreviewPacket(entityId, tab, name, token, false, clone == null ? new CompoundTag() : clone));
    }
}
