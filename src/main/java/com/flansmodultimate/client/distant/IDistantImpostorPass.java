package com.flansmodultimate.client.distant;

import org.joml.Matrix4f;

/**
 * Draws into a far-terrain renderer's own colour and depth textures while it renders a frame, after its terrain
 * and before its fog, so that what is drawn is hidden by that terrain and fogged with it. Render thread.
 */
public interface IDistantImpostorPass
{
    /** Whether the pass still works; once it fails it stays off and the far terrain stops running it. */
    boolean usable();

    /** Draws this frame's impostors. Leaves the OpenGL state as it found it. */
    void render(Frame frame);

    /**
     * One frame of the far terrain.
     *
     * @param viewProjection
     *            the far terrain's projection and view, from camera-relative block positions to clip space
     * @param colorTexture
     *            OpenGL name of the far terrain's colour texture
     * @param depthTexture
     *            OpenGL name of the far terrain's depth texture
     * @param reverseZ
     *            whether nearer is a greater depth, cleared to 0, rather than a smaller one, cleared to 1
     * @param partialTick
     *            the frame's partial tick
     */
    record Frame(Matrix4f viewProjection, int colorTexture, int depthTexture, boolean reverseZ, float partialTick)
    {}
}
