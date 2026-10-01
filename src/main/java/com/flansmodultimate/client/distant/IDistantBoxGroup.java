package com.flansmodultimate.client.distant;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Boxes drawn together by a far-terrain renderer, around an origin that may move every frame. Render thread. */
public interface IDistantBoxGroup
{
    /** Where the group is drawn, asked once per frame. */
    @FunctionalInterface
    interface Origin
    {
        /** World position of the group's origin at this point of the current tick. */
        Vec3 at(float partialTick);
    }

    void setOrigin(Origin origin);

    /** Replaces the boxes, which are relative to the origin. */
    void setBoxes(List<DistantBox> boxes);

    void setActive(boolean active);

    /** Lights the boxes as if they glowed, whatever their style, as in a thermal sight. */
    void setGlowing(boolean glowing);

    /** Whether the renderer still draws this group; it stops after a level change and must be recreated. */
    boolean isValid();

    /** Removes the group from the renderer and frees it. */
    void close();
}
