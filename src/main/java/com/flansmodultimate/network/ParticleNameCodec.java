package com.flansmodultimate.network;

import com.flansmodultimate.common.FlanParticles;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a particle name as a small id when it is one of this mod's known particle names, and as
 * text otherwise.
 *
 * <p>Particle packets are among the most frequent the server sends, and the name was most of
 * each one. The table is built from {@link FlanParticles#sortedNames()}, identical on both ends
 * because the network protocol version only matches between identical mod versions. Anything
 * outside the table, such as a block-crack particle carrying its block id, falls back to text.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleNameCodec
{
    /** Written instead of an id when the name follows as text. */
    private static final int TEXT = 0;

    private static final List<String> NAMES = FlanParticles.sortedNames();
    private static final Map<String, Integer> IDS = buildIds();

    private static Map<String, Integer> buildIds()
    {
        Map<String, Integer> ids = new HashMap<>();
        for (int index = 0; index < NAMES.size(); index++)
            ids.put(NAMES.get(index), index + 1);
        return ids;
    }

    public static void write(FriendlyByteBuf buffer, String name)
    {
        Integer id = IDS.get(name);
        if (id != null)
        {
            buffer.writeVarInt(id);
            return;
        }
        buffer.writeVarInt(TEXT);
        buffer.writeUtf(name);
    }

    public static String read(FriendlyByteBuf buffer)
    {
        int id = buffer.readVarInt();
        if (id == TEXT)
            return buffer.readUtf();
        if (id < 1 || id > NAMES.size())
            throw new DecoderException("Unknown particle id " + id);
        return NAMES.get(id - 1);
    }
}
