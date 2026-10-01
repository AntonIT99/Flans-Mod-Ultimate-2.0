package com.flansmodultimate.client.distant;

/**
 * One axis-aligned box of a shape drawn on the far terrain, in blocks, relative to the origin of the group
 * that holds it. {@code argb} is the colour with its alpha in the top byte.
 */
public record DistantBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb)
{
}
