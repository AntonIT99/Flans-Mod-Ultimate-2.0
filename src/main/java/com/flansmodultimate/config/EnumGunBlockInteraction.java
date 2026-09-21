package com.flansmodultimate.config;

/**
 * What right-clicking a block does while a gun is in hand.
 *
 * <p>Aiming is a right-click, so without this a player lining up a shot at a chest opens it instead.
 * This is a personal preference and costs nobody anything: the block interaction is the player's own
 * to give up.
 */
public enum EnumGunBlockInteraction
{
    /** Blocks behave as they do with any other item. */
    ALLOW,
    /** Blocks that open a screen, such as chests and furnaces, are left alone unless the player sneaks. */
    NO_CONTAINERS,
    /** No block is used while armed, doors, levers and buttons included, sneaking or not. */
    NONE
}
