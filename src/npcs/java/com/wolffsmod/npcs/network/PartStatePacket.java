package com.wolffsmod.npcs.network;

import com.flansmodultimate.api.IPartBody;
import com.flansmodultimate.api.PartState;

import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * How much of each part of an NPC's model is left, sent to the clients tracking it whenever that changes. Clients need it
 * to let rounds through the parts that are gone, as the server does.
 *
 * <p>
 * Hit points travel as shares of what the part can hold, so that a client whose boxes arrive later still ends up right.
 * </p>
 */
// Packet arrays are written once and read once; nothing else holds on to them.
@SuppressWarnings({"java:S6218", "java:S2384"})
public record PartStatePacket(int entityId, String[] parts, float[] shares)
{
    private static final int MAX_PARTS = 512;
    private static final int MAX_NAME_LENGTH = 64;

    public static PartStatePacket of(int entityId, IPartBody body)
    {
        List<PartState> states = body.getParts();
        int count = Math.min(MAX_PARTS, states.size());
        String[] parts = new String[count];
        float[] shares = new float[count];
        for (int index = 0; index < count; index++)
        {
            PartState state = states.get(index);
            parts[index] = state.name();
            float capacity = state.box().health();
            // A part without hit points cannot be broken and always counts as whole.
            float left = capacity > 0F ? Math.max(Float.MIN_VALUE, state.health() / capacity) : 1F;
            shares[index] = state.destroyed() ? 0F : left;
        }
        return new PartStatePacket(entityId, parts, shares);
    }

    /** Gives a body the state the server sent. */
    public void applyTo(IPartBody body)
    {
        for (int index = 0; index < parts.length && index < shares.length; index++)
        {
            int part = index;
            body.getPart(parts[part]).ifPresent(state -> body.setHealth(state.name(), shares[part] * state.box().health()));
        }
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId);
        int count = Math.min(MAX_PARTS, Math.min(parts.length, shares.length));
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++)
        {
            buffer.writeUtf(parts[index], MAX_NAME_LENGTH);
            buffer.writeFloat(shares[index]);
        }
    }

    public static PartStatePacket read(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        int count = Math.min(MAX_PARTS, Math.max(0, buffer.readVarInt()));
        String[] parts = new String[count];
        float[] shares = new float[count];
        for (int index = 0; index < count; index++)
        {
            parts[index] = buffer.readUtf(MAX_NAME_LENGTH);
            float share = buffer.readFloat();
            shares[index] = Float.isFinite(share) ? Math.max(0F, Math.min(1F, share)) : 1F;
        }
        return new PartStatePacket(entityId, parts, shares);
    }
}
