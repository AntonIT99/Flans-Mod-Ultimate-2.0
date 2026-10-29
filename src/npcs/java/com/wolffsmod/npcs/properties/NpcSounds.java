package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.entity.FlansEntitySounds;
import com.flansmodultimate.api.entity.SoundRange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Plays an NPC's own sounds the way Flan's Mod Ultimate plays the same sound for a player or a driveable: the server
 * sends it to everyone within the configured radius of that kind of sound, and the radius sets the volume, so the sound
 * swells as a listener walks up to the NPC and fades out at its edge.
 *
 * <p>
 * Custom NPCs instead sends its own sounds at a fixed volume within a fixed radius, which leaves a gun an NPC fires or
 * an engine it runs much quieter, and audible over a far shorter distance, than the same weapon in a player's hands or
 * the same machine with a player at its controls. Server thread.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSounds
{
    private static final String FLANSMOD = "flansmod";

    /**
     * Plays one of the NPC's own sounds at the radius its role carries.
     *
     * @param npc
     *            the NPC the sound comes from
     * @param sound
     *            sound event name, as Custom NPCs stores it
     * @param role
     *            what the sound is for, which selects the server's radius for it
     */
    public static void play(EntityNPCInterface npc, @Nullable String sound, SoundRange role)
    {
        String id = resolve(sound);
        if (id != null)
            FlansEntitySounds.playClip(npc, id, FlansEntitySounds.getRange(role), false);
    }

    /**
     * @param sound
     *            sound event name, as Custom NPCs stores it
     * @return whether the sound is one of Flan's Mod Ultimate's own, so that playing it the Flan way rather than the
     *         Custom NPCs way is what the content pack it comes from intended
     */
    public static boolean isFlanSound(@Nullable String sound)
    {
        String id = resolve(sound);
        return id != null && id.startsWith(FLANSMOD + ":");
    }

    /**
     * @return the qualified sound event the stored name selects, or null when it names none. Custom NPCs reads an
     *         unqualified name as a vanilla sound, while the mod's content-pack sounds are only ever stored
     *         unqualified, so an unqualified name without a vanilla sound event of its own is a Flan content sound.
     */
    @Nullable
    @SuppressWarnings("deprecation") // The static sound event registry is how Custom NPCs itself resolves a stored sound name.
    private static String resolve(@Nullable String sound)
    {
        if (sound == null || sound.isBlank())
            return null;
        ResourceLocation id = ResourceLocation.tryParse(sound);
        if (id == null)
            return null;
        if (sound.contains(":"))
            return id.toString();
        return BuiltInRegistries.SOUND_EVENT.containsKey(id) ? id.toString() : FLANSMOD + ":" + sound;
    }
}
