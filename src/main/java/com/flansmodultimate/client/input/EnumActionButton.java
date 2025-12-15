package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

public enum EnumActionButton
{
    LEFT_MOUSE,
    RIGHT_MOUSE,
    MIDDLE_MOUSE,
    MOUSE_4,
    MOUSE_5;

    public int toGlfw()
    {
        return switch (this)
        {
            case LEFT_MOUSE -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT_MOUSE -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE_MOUSE -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case MOUSE_4 -> GLFW.GLFW_MOUSE_BUTTON_4;
            case MOUSE_5 -> GLFW.GLFW_MOUSE_BUTTON_5;
        };
    }
}
