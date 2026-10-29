package com.wolffsmod.npcs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** The server's result of a crew action, displayed in the editor that requested it. */
public record CrewResultPacket(int entityId, Result result, int count)
{
    public enum Result
    {
        CREWED, REMOVED, DENIED, NO_SEATS;

        public String key()
        {
            return "wolffsmodnpcs.crew." + name().toLowerCase(Locale.ROOT);
        }
    }

    public Component message()
    {
        return Component.translatable(result == Result.CREWED && count == 0 ? "wolffsmodnpcs.crew.none_boarded" : result.key(), count);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(result);
        buffer.writeVarInt(count);
    }

    public static CrewResultPacket read(FriendlyByteBuf buffer)
    {
        return new CrewResultPacket(buffer.readVarInt(), buffer.readEnum(Result.class), buffer.readVarInt());
    }
}
