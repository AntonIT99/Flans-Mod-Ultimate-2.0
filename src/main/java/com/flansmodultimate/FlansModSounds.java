package com.flansmodultimate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModSounds
{
    public static final String SOUND_EMPTY_CLICK = "emptyclick";
    public static final String SOUND_DEFAULT_SHELL_INSERT = "defaultshellinsert";
    public static final String SOUND_SWITCH_FIRING_MODE = "switchfiringmode";
    public static final String SOUND_IMPACT_DIRT = "impact_dirt";
    public static final String SOUND_IMPACT_METAL = "impact_metal";
    public static final String SOUND_IMPACT_BRICKS = "impact_bricks";
    public static final String SOUND_IMPACT_GLASS = "impact_glass";
    public static final String SOUND_IMPACT_ROCK = "impact_rock";
    public static final String SOUND_IMPACT_WOOD = "impact_wood";
    public static final String SOUND_IMPACT_WATER = "impact_water";
    public static final String SOUND_BULLET = "bullet";
    public static final String SOUND_BULLETFLYBY = "bulletflyby";
    public static final String SOUND_UNLOCKNOTCH = "unlocknotch";
    public static final String SOUND_SKULLBOSSLAUGH = "skullboss_laugh";
    public static final String SOUND_SKULLBOSSSPAWN = "skullboss_spawn";

    static void registerSounds()
    {
        FlansMod.registerSound(SOUND_EMPTY_CLICK, null);
        FlansMod.registerSound(SOUND_DEFAULT_SHELL_INSERT, null);
        FlansMod.registerSound(SOUND_SWITCH_FIRING_MODE, null);
        FlansMod.registerSound(SOUND_IMPACT_DIRT, null);
        FlansMod.registerSound(SOUND_IMPACT_METAL, null);
        FlansMod.registerSound(SOUND_IMPACT_BRICKS, null);
        FlansMod.registerSound(SOUND_IMPACT_GLASS, null);
        FlansMod.registerSound(SOUND_IMPACT_ROCK, null);
        FlansMod.registerSound(SOUND_IMPACT_WOOD, null);
        FlansMod.registerSound(SOUND_IMPACT_WATER, null);
        FlansMod.registerSound(SOUND_BULLET, null);
        FlansMod.registerSound(SOUND_BULLETFLYBY, null);
        FlansMod.registerSound(SOUND_UNLOCKNOTCH, null);
        FlansMod.registerSound(SOUND_SKULLBOSSLAUGH, null);
        FlansMod.registerSound(SOUND_SKULLBOSSSPAWN, null);
    }
}
