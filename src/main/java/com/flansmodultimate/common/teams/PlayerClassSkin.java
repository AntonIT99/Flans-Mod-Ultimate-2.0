package com.flansmodultimate.common.teams;

/** The server's class and skin-variant choice for one player, retained until the next class application. */
public record PlayerClassSkin(String playerClass, int variant, String texture)
{
    public PlayerClassSkin(String playerClass, int variant)
    {
        this(playerClass, variant, "");
    }
}
