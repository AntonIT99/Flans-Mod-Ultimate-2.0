package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils
{
    public static String sanitize(String name)
    {
        // Lowercase + swap spaces; keep only chars valid in resource paths
        return name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9._\\-]", "_");
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
                        .map(p -> new Override(FlansMod.paintjob.toString(), p.getId(), FlansMod.FLANSMOD_ID + ":item/" + p.getIcon()))
                        .toList();
            }
            return new ItemModel("item/generated", null, new ItemModel.Textures(FlansMod.FLANSMOD_ID + ":item/" + config.getIcon()), overrides);
        }

        public static ItemModel create(Paintjob paintjob)
        {
            return new ItemModel("item/generated", null, new ItemModel.Textures(FlansMod.FLANSMOD_ID + ":item/" + paintjob.getIcon()), null);
        }
    }
}
