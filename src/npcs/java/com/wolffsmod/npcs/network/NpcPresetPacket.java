package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.presets.NpcPreset;
import com.wolffsmod.npcs.presets.NpcPresetRequests;
import com.wolffsmod.npcs.presets.NpcPresetSetting;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Optional;

/**
 * The presets of the Wolff's Mod tab: a client asks for the settings presets set on the NPC it is editing, or asks the
 * server to apply a preset, and the server answers with those settings as they now are.
 *
 * @param entityId
 *            the NPC being edited, so a request that arrives for another NPC is refused
 * @param preset
 *            name of the preset to apply; empty otherwise
 * @param values
 *            the settings by name, sent by the server; empty otherwise
 */
public record NpcPresetPacket(int entityId, Action action, String preset, CompoundTag values)
{
    /** Longest preset name accepted; the names are far shorter. */
    private static final int MAX_NAME_LENGTH = 64;

    /** What the packet asks for or carries. */
    public enum Action
    {
        /** Client to server: send the settings. */
        GET,
        /** Client to server: apply the preset. */
        APPLY,
        /** Server to client: these are the settings. */
        STATE, UNDO
    }

    public static NpcPresetPacket request(int entityId)
    {
        return new NpcPresetPacket(entityId, Action.GET, "", new CompoundTag());
    }

    public static NpcPresetPacket apply(int entityId, NpcPreset preset)
    {
        return new NpcPresetPacket(entityId, Action.APPLY, preset.name(), new CompoundTag());
    }

    public static NpcPresetPacket undo(int entityId)
    {
        return new NpcPresetPacket(entityId, Action.UNDO, "", new CompoundTag());
    }

    public static NpcPresetPacket state(EntityNPCInterface npc)
    {
        CompoundTag values = new CompoundTag();
        for (NpcPresetSetting setting : NpcPresetSetting.values())
            values.putInt(setting.name(), setting.get(npc));
        values.putBoolean("CanUndo", NpcPresetRequests.canUndo(npc));
        return new NpcPresetPacket(npc.getId(), Action.STATE, "", values);
    }

    /** Gives the client's copy of the NPC the settings the server sent; a setting it does not know or a value out of range is skipped. */
    public void copyTo(EntityNPCInterface npc)
    {
        for (NpcPresetSetting setting : NpcPresetSetting.values())
            if (values.contains(setting.name()) && setting.accepts(values.getInt(setting.name())))
                setting.set(npc, values.getInt(setting.name()));
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(action);
        buffer.writeUtf(preset, MAX_NAME_LENGTH);
        buffer.writeNbt(values);
    }

    public static NpcPresetPacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        Action action = buffer.readEnum(Action.class);
        String preset = buffer.readUtf(MAX_NAME_LENGTH);
        CompoundTag values = buffer.readNbt();
        return new NpcPresetPacket(entityId, action, preset, values == null ? new CompoundTag() : values);
    }

    /** Carries out a client's request; a preset the server does not know is ignored. */
    public void handle(ServerPlayer player)
    {
        switch (action)
        {
            case UNDO -> NpcPresetRequests.undo(player, entityId);
            case GET -> NpcPresetRequests.sendState(player, entityId);
            case APPLY -> named().ifPresent(chosen -> NpcPresetRequests.apply(player, entityId, chosen));
            default -> {
                // The server sends the state; it does not take it.
            }
        }
    }

    /** @return the preset the packet names, if this side knows it */
    public Optional<NpcPreset> named()
    {
        return Arrays.stream(NpcPreset.values()).filter(candidate -> candidate.name().equals(preset)).findFirst();
    }
}
