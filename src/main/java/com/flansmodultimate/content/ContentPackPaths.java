package com.flansmodultimate.content;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Shared directory and alias-file names of legacy and modern content packs. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPackPaths
{
    public static final String FOLDER_BLOCKSTATES = "blockstates";
    public static final String FOLDER_MODELS = "models";
    public static final String FOLDER_MODELS_BLOCK = "block";
    public static final String FOLDER_MODELS_ITEM = "item";
    public static final String FOLDER_LANG = "lang";
    public static final String FOLDER_TEXTURES = "textures";
    public static final String FOLDER_TEXTURES_ARMOR = "armor";
    public static final String FOLDER_TEXTURES_GUI = "gui";
    public static final String FOLDER_TEXTURES_SKINS = "skins";
    public static final String FOLDER_TEXTURES_BLOCK = "block";
    public static final String FOLDER_TEXTURES_BLOCKS = "blocks";
    public static final String FOLDER_TEXTURES_ITEM = "item";
    public static final String FOLDER_TEXTURES_ITEMS = "items";
    public static final String FOLDER_SOUND = "sound";
    public static final String FOLDER_SOUNDS = "sounds";

    static final String ID_ALIAS_FILE = "id_alias.json";
    static final String ARMOR_TEXTURES_ALIAS_FILE = "armor_textures_alias.json";
    static final String GUI_TEXTURES_ALIAS_FILE = "gui_textures_alias.json";
    static final String SKINS_TEXTURES_ALIAS_FILE = "skins_textures_alias.json";
}
