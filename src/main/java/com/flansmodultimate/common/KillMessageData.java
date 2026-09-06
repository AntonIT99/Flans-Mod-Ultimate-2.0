package com.flansmodultimate.common;

import net.minecraft.ChatFormatting;

/**
 * One entry of the Teams kill feed: who killed whom, with what, and in which team colours.
 * Legacy packs sent the colour glued in front of the name; keeping them apart lets the
 * client look the killer up by name again.
 */
public record KillMessageData(boolean headshot, String weaponShortName,
                              String killerName, ChatFormatting killerColour,
                              String victimName, ChatFormatting victimColour)
{
}
