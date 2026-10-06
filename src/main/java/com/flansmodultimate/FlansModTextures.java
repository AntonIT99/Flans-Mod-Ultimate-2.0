package com.flansmodultimate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModTextures
{
    public static final String DEFAULT_BULLET_TEXTURE = "defaultbullet";
    public static final String DEFAULT_BULLET_TRAIL_TEXTURE = "defaultbullettrail";

    /** Placeholder for callers without a texture. 1.20.1 still accepts the empty resource location. */
    public static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.parse("");
    public static final ResourceLocation TEXTURE_BANNER = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/entity/banner.png");
    public static final ResourceLocation TEXTURE_DEFAULTFLASH = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultflash.png");
    public static final ResourceLocation TEXTURE_DEFAULTMUZZLEFLASH = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultmuzzleflash.png");
    public static final ResourceLocation TEXTURE_FLAGPOLE = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/entity/flagpole.png");
    public static final ResourceLocation TEXTURE_GUI_AMMOGUI = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/ammo_gui.png");
    public static final ResourceLocation TEXTURE_GUI_ARMORBOX = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/armor_box.png");
    public static final ResourceLocation TEXTURE_GUI_BASEEDIT = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/base_edit.png");
    public static final ResourceLocation TEXTURE_GUI_BASICHITMARKER = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/basic_hitmarker.png");
    public static final ResourceLocation TEXTURE_GUI_BLOOD = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/blood.png");
    public static final ResourceLocation TEXTURE_GUI_FLARE = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/flare.png");
    public static final ResourceLocation TEXTURE_GUI_FLASH = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/flash.png");
    public static final ResourceLocation TEXTURE_GUI_DRIVEABLECRAFTING = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/driveable_crafting.png");
    public static final ResourceLocation TEXTURE_GUI_DRIVEABLEFUEL = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/driveable_fuel.png");
    public static final ResourceLocation TEXTURE_GUI_DRIVEABLEINVENTORY = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/driveable_inventory.png");
    public static final ResourceLocation TEXTURE_GUI_DRIVEABLEMENU = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/driveable_menu.png");
    public static final ResourceLocation TEXTURE_GUI_DRIVEABLEREPAIR = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/driveable_repair.png");
    public static final ResourceLocation TEXTURE_GUI_FMUHITMARKER = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/fmu_hitmarker.png");
    public static final ResourceLocation TEXTURE_GUI_FMUHITMARKERHD = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/fmu_hitmarker_hd.png");
    public static final ResourceLocation TEXTURE_GUI_GUNWORKBENCH = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/gun_workbench.png");
    public static final ResourceLocation TEXTURE_GUI_HEADSHOTSYMBOL = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/headshotsymbol.png");
    public static final ResourceLocation TEXTURE_GUI_MECHAINVENTORY = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/mecha_inventory.png");
    public static final ResourceLocation TEXTURE_GUI_PAINTJOBTABLE = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/paintjob_table.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMS = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSLANDINGPAGE = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_landing_page.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSLOADOUTEDITOR = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_loadout_editor.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSMISSIONRESULTS = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_mission_results.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSOPENCREATES = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_open_crates.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSRANKS = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_ranks.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSSCORES = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_scores.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSSCORES2 = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_scores_2.png");
    public static final ResourceLocation TEXTURE_GUI_TEAMSVOTE = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/teams_vote.png");
    public static final ResourceLocation TEXTURE_GUI_WEAPONBOX = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/weaponbox.png");
}
