package com.flansmodultimate.config;

import lombok.Getter;

/** Largest side length content-pack item icons keep in the item icon atlas; larger icons are halved until they fit. */
public enum EnumItemIconSizeLimit
{
    UNLIMITED(0),
    PX_16(16),
    PX_32(32),
    PX_64(64),
    PX_128(128),
    PX_256(256),
    PX_512(512);

    /** Maximum width and height in pixels, or 0 for no limit. */
    @Getter
    private final int pixels;

    EnumItemIconSizeLimit(int pixels)
    {
        this.pixels = pixels;
    }
}
