package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

public enum EnumMouseButton
{
    MOUSE_LEFT,
    MOUSE_RIGHT,
    MOUSE_MIDDLE,
    MOUSE_4,
    MOUSE_5;

    public int toGlfw()
    {
        return switch (this)
        {
            case MOUSE_LEFT -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case MOUSE_RIGHT -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case MOUSE_MIDDLE -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case MOUSE_4 -> GLFW.GLFW_MOUSE_BUTTON_4;
            case MOUSE_5 -> GLFW.GLFW_MOUSE_BUTTON_5;
        };
    }
}
