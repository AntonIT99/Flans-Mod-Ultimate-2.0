package com.wolffsarmormod.util;

import com.google.gson.annotations.SerializedName;
import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.paintjob.Paintjob;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.common.types.PaintableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils
{
    public static String sanitize(String name)
    {
        // Lowercase + swap spaces; keep only chars valid in resource paths
        String n = name.toLowerCase().replace(' ', '_');
        return n.replaceAll("[^a-z0-9._\\-]", "_");
    }

    public static class ItemModel
    {
        String parent;
        @SerializedName("gui_light")
        String guiLight;
        Textures textures;
        List<Override> overrides;

        ItemModel(String parent, @Nullable String guiLight, Textures textures, List<Override> overrides)
        {
            this.parent = parent;
            this.guiLight = guiLight;
            this.textures = textures;
            this.overrides = overrides;
        }

        static class Textures
        {
            String layer0;
            Textures(String layer0)
            {
                this.layer0 = layer0;
            }
        }

        static class Override
        {
            Map<String, Integer> predicate;
            String model;

            Override(String namespacedKey, int value, String modelPath)
            {
                predicate = new LinkedHashMap<>();
                predicate.put(namespacedKey, value);
                model = modelPath;
            }
        }

        public static ItemModel create(InfoType config)
        {
            List<Override> overrides = null;
            if (config instanceof PaintableType paintableType)
            {
                overrides = paintableType.getPaintjobs().values().stream()
                        .filter(p -> !p.equals(paintableType.getDefaultPaintjob()))
                        .map(p -> new Override(ArmorMod.paintjob.toString(), p.getId(), ArmorMod.FLANSMOD_ID + ":item/" + p.getIcon()))
                        .toList();
            }
            return new ItemModel("item/generated", null, new ItemModel.Textures(ArmorMod.FLANSMOD_ID + ":item/" + config.getIcon()), overrides);
        }

        public static ItemModel create(Paintjob paintjob)
        {
            return new ItemModel("item/generated", null, new ItemModel.Textures(ArmorMod.FLANSMOD_ID + ":item/" + paintjob.getIcon()), null);
        }
    }
}
