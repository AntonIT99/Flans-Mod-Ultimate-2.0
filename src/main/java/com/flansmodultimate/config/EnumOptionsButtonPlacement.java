package com.flansmodultimate.config;

/** Where the button opening the mod's options screen is added to the vanilla menus. */
public enum EnumOptionsButtonPlacement
{
    /** In the vanilla options screen, above its Done button. */
    OPTIONS_SCREEN,
    /** In the pause menu, above its disconnect button. */
    PAUSE_MENU,
    /** In both menus. */
    BOTH,
    /** Nowhere: the screen stays reachable from the Config button of the mod list. */
    NONE;

    public boolean inOptionsScreen()
    {
        return this == OPTIONS_SCREEN || this == BOTH;
    }

    public boolean inPauseMenu()
    {
        return this == PAUSE_MENU || this == BOTH;
    }
}
