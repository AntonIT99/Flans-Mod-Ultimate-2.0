package com.flansmodultimate.network;

import com.flansmodultimate.FlansMod;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Writes one of this mod's sound names as its sound event registry id, and anything else as text.
 *
 * <p>Every content-pack sound is registered as a sound event under the {@code flansmod} namespace,
 * and Forge makes the client's sound event ids match the server's while connecting, the same ids
 * vanilla's own sound packets rely on. So the id is safe whatever packs each side has loaded: a
 * sound the client lacks would already have failed the connection's registry check.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundNameCodec
{
    /** Written instead of an id when the name follows as text. */
    private static final int TEXT = 0;

    public static void write(FriendlyByteBuf buffer, String name)
    {
        int id = registryId(name);
        if (id >= 0)
        {
            buffer.writeVarInt(id + 1);
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
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.byId(id - 1);
        ResourceLocation key = soundEvent == null ? null : BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
        if (key == null || !FlansMod.FLANSMOD_ID.equals(key.getNamespace()))
            throw new DecoderException("Unknown sound event id " + (id - 1));
        return key.getPath();
    }

    /** The registry id of this mod's sound of that name, or -1 when it is not one. */
    private static int registryId(String name)
    {
        if (name.isEmpty() || !ResourceLocation.isValidPath(name))
            return -1;
        DeferredHolder<SoundEvent, SoundEvent> soundEvent = FlansMod.getSoundEvent(name).orElse(null);
        if (soundEvent == null || !soundEvent.isBound())
            return -1;
        return BuiltInRegistries.SOUND_EVENT.getId(soundEvent.get());
    }
}
