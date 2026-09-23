package com.flansmodultimate.common.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The ways one side's content packs can fail to be the other side's. */
@AllArgsConstructor
public enum EnumContentMismatch
{
    /** Both sides loaded the pack, but not the same definitions. */
    DIFFERENT("message.flansmodultimate.content_mismatch.different"),
    /** The other side loaded a pack this side did not load at all. */
    MISSING("message.flansmodultimate.content_mismatch.missing"),
    /** This side loaded a pack the other side does not have. */
    UNKNOWN_TO_THEM("message.flansmodultimate.content_mismatch.extra");

    @Getter
    private final String translationKey;
}
