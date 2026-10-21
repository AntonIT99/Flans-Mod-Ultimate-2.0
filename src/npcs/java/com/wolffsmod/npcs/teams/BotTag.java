package com.wolffsmod.npcs.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/**
 * What marks an NPC as a bot of a Teams round, kept in its persistent data. The server saves it with the NPC, so a bot
 * left over from an earlier round can be recognised and removed; clients receive the team and the name tag rule with the
 * NPC's spawn data, for the name tags only allies see.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotTag
{
    /** Key of the bot data in the persistent data, and of its client part in the NPC's spawn data. */
    public static final String KEY = "WolffsModTeamsBot";
    private static final String NBT_INSTANCE = "instance";
    private static final String NBT_SLOT = "slot";
    private static final String NBT_TEAM = "team";
    private static final String NBT_VEHICLE = "vehicle";
    private static final String NBT_NAMEPLATES = "nameplates";

    /**
     * @param instance
     *            the playing of the round the bot was made for
     * @param slot
     *            the place in the round's force the bot fills
     */
    public record Data(UUID instance, int slot, String team, boolean vehicle, BotSettings.Nameplates nameplates)
    {}

    /** What clients learn about a bot. */
    public record ClientData(String team, BotSettings.Nameplates nameplates)
    {}

    public static void write(Entity entity, Data data)
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(NBT_INSTANCE, data.instance());
        tag.putInt(NBT_SLOT, data.slot());
        tag.putString(NBT_TEAM, data.team());
        tag.putBoolean(NBT_VEHICLE, data.vehicle());
        tag.putString(NBT_NAMEPLATES, data.nameplates().name());
        entity.getPersistentData().put(KEY, tag);
    }

    public static Optional<Data> read(Entity entity)
    {
        if (!entity.getPersistentData().contains(KEY, Tag.TAG_COMPOUND))
            return Optional.empty();
        CompoundTag tag = entity.getPersistentData().getCompound(KEY);
        if (!tag.hasUUID(NBT_INSTANCE))
            return Optional.empty();
        return Optional.of(new Data(tag.getUUID(NBT_INSTANCE), tag.getInt(NBT_SLOT), tag.getString(NBT_TEAM), tag.getBoolean(NBT_VEHICLE),
            BotSettings.parse(BotSettings.Nameplates.class, tag.getString(NBT_NAMEPLATES), BotSettings.Nameplates.DEFAULT)));
    }

    /** Whether the entity carries bot data at all, from any round. */
    public static boolean isBot(Entity entity)
    {
        return entity.getPersistentData().contains(KEY, Tag.TAG_COMPOUND);
    }

    /** Adds what clients need to an NPC's spawn data; nothing for an NPC that is no bot. Server side. */
    public static void writeClient(Entity entity, CompoundTag spawnData)
    {
        read(entity).ifPresent(data ->
        {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_TEAM, data.team());
            tag.putString(NBT_NAMEPLATES, data.nameplates().name());
            spawnData.put(KEY, tag);
        });
    }

    /** Keeps what the server sent about a bot with the client's copy of the NPC. Client side. */
    public static void readClient(Entity entity, CompoundTag spawnData)
    {
        if (spawnData.contains(KEY, Tag.TAG_COMPOUND))
            entity.getPersistentData().put(KEY, spawnData.getCompound(KEY).copy());
        else
            entity.getPersistentData().remove(KEY);
    }

    /** What the server sent about a bot, on the client. */
    public static Optional<ClientData> readClient(Entity entity)
    {
        if (!entity.getPersistentData().contains(KEY, Tag.TAG_COMPOUND))
            return Optional.empty();
        CompoundTag tag = entity.getPersistentData().getCompound(KEY);
        return Optional.of(new ClientData(tag.getString(NBT_TEAM), BotSettings.parse(BotSettings.Nameplates.class, tag.getString(NBT_NAMEPLATES), BotSettings.Nameplates.DEFAULT)));
    }
}
