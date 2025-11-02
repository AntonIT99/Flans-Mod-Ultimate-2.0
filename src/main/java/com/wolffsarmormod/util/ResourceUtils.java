package com.wolffsarmormod.util;

import com.google.gson.annotations.SerializedName;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.types.InfoType;

import javax.annotation.Nullable;

public class ResourceUtils
{
    private ResourceUtils() {}

    public static class ItemModel
    {
        String parent;
        @SerializedName("gui_light")
        String guiLight;
        Textures textures;

        protected ItemModel (String parent, @Nullable String guiLight, Textures textures)
        {
            this.parent = parent;
            this.guiLight = guiLight;
            this.textures = textures;
        }

        public static ItemModel create(InfoType config)
        {
            return new ItemModel("item/generated", null, new ResourceUtils.Textures(ArmorMod.FLANSMOD_ID + ":item/" + config.getIcon()));
        }
    }

    public static class Textures
    {
        String layer0;

        protected Textures(String layer0)
        {
            this.layer0 = layer0;
        }
    }
}
