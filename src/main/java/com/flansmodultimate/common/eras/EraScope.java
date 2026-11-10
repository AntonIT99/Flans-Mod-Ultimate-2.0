package com.flansmodultimate.common.eras;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

/**
 * What a restriction covers: the whole server, one dimension, a Teams round or a battle. {@code key} names the
 * dimension, round or battle and is empty for the server.
 */
public record EraScope(Kind kind, String key)
{
    private static final EraScope SERVER = new EraScope(Kind.SERVER, "");

    @Getter
    @RequiredArgsConstructor
    public enum Kind
    {
        SERVER("server", true), DIMENSION("dimension", true), ROUND("round", false), BATTLE("battle", false);

        private final String id;
        /** Whether a restriction of this scope may despawn entities it blocks. */
        private final boolean despawnAllowed;

        public static Kind byId(String id)
        {
            for (Kind kind : values())
                if (kind.id.equals(id.toLowerCase(Locale.ROOT)))
                    return kind;
            throw new IllegalArgumentException("Unknown era scope '" + id + "'");
        }
    }

    public static EraScope server()
    {
        return SERVER;
    }

    public static EraScope dimension(String dimension)
    {
        return new EraScope(Kind.DIMENSION, dimension);
    }

    /** Readable form, such as {@code dimension minecraft:the_nether}. */
    @Override
    public String toString()
    {
        return key.isEmpty() ? kind.id : kind.id + " " + key;
    }
}
