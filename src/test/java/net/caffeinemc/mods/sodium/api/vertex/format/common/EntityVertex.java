package net.caffeinemc.mods.sodium.api.vertex.format.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

/** Test stand-in for Sodium's entity vertex format, of which Flan's Mod uses only the format token. */
public final class EntityVertex
{
    public static final VertexFormat FORMAT = DefaultVertexFormat.NEW_ENTITY;

    private EntityVertex()
    {}
}
