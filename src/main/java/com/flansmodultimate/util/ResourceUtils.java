package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils
{
    public static String sanitize(@Nullable String name)
    {
        if (name == null)
            return StringUtils.EMPTY;
        // Lowercase + swap spaces; keep only chars valid in resource paths
        return name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9._\\-]", "_");
    }

    @AllArgsConstructor
    public static class ModelJson
    {
        String parent;
        @SerializedName("gui_light")
        String guiLight;
        Map<String, String> textures;
        List<Override> overrides;

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

        public static ModelJson createBlockModel(BlockType config)
        {
            String parent = "minecraft:block/cube_bottom_top";

            Map<String, String> textures = new LinkedHashMap<>();
            textures.put("top", FlansMod.FLANSMOD_ID + ":block/" + config.getTopTextureName());
            textures.put("bottom", FlansMod.FLANSMOD_ID + ":block/" + config.getBottomTextureName());
            textures.put("side", FlansMod.FLANSMOD_ID + ":block/" + config.getSideTextureName());

            return new ModelJson(parent, null, textures, null);
        }

        public static ModelJson createItemModel(InfoType config)
        {
            String parent = "minecraft:item/generated";
            if (config.getType().isHasBlock())
                parent = FlansMod.FLANSMOD_ID + ":block/" + config.getShortName();
            else if (config.getType().isHandHeldItem())
                parent = "minecraft:item/handheld";

            Map<String, String> textures = null;
            if (!config.getType().isHasBlock())
                textures = Map.of("layer0", FlansMod.FLANSMOD_ID + ":item/" + config.getIcon());

            List<Override> overrides = null;
            if (config instanceof PaintableType paintableType)
            {
                overrides = paintableType.getPaintjobs().values().stream()
                    .filter(p -> !p.equals(paintableType.getDefaultPaintjob()))
                    .map(p -> new Override(FlansMod.paintjob.toString(), p.getId(), FlansMod.FLANSMOD_ID + ":item/" + p.getIcon()))
                    .toList();
            }

            return new ModelJson(parent, null, textures, overrides);
        }

        public static ModelJson createItemModel(InfoType config, Paintjob paintjob)
        {
            String parent = "minecraft:item/generated";
            if (config.getType().isHasBlock())
                parent = FlansMod.FLANSMOD_ID + ":block/" + config.getShortName();
            else if (config.getType().isHandHeldItem())
                parent = "minecraft:item/handheld";

            Map<String, String> textures = null;
            if (!config.getType().isHasBlock())
                textures = Map.of("layer0", FlansMod.FLANSMOD_ID + ":item/" + paintjob.getIcon());

            return new ModelJson(parent, null, textures, null);
        }
    }

    public static class BlockStateJson
    {
        Map<String, Object> variants;

        BlockStateJson()
        {
            variants = new LinkedHashMap<>();
        }

        public static BlockStateJson create(InfoType type)
        {
            return BlockStateJson.single(FlansMod.FLANSMOD_ID + ":block/" + type.getShortName());
        }

        /** Represents one model entry inside a variant */
        @AllArgsConstructor
        public static class Variant
        {
            String model;
            Integer x;
            Integer y;
            @SerializedName("uvlock")
            Boolean uvLock;
            Integer weight;

            public Variant(String model)
            {
                this.model = model;
            }

            public Variant rotX(int x)
            {
                this.x = x; return this;
            }

            public Variant rotY(int y)
            {
                this.y = y; return this;

            }

            public Variant uvlock(boolean uv)
            {
                this.uvLock = uv; return this;
            }

            public Variant weight(int w)
            {
                this.weight = w; return this;
            }
        }

        /** { "variants": { "": { "model": "<model>" } } } */
        public static BlockStateJson single(String model)
        {
            BlockStateJson bs = new BlockStateJson();
            bs.variants.put("", new Variant(model));
            return bs;
        }

        /** { "variants": { "<key>": { "model": "<model>" } } } */
        public static BlockStateJson singleVariant(String key, String model)
        {
            BlockStateJson bs = new BlockStateJson();
            bs.variants.put(key, new Variant(model));
            return bs;
        }

        /**
         * { "variants": { "<key>": [ { ... }, { ... } ] } }
         * Use for random rotation/weights etc.
         */
        public static BlockStateJson multiVariant(String key, List<Variant> choices)
        {
            BlockStateJson bs = new BlockStateJson();
            bs.variants.put(key, new ArrayList<>(choices));
            return bs;
        }

        /**
         * Common case: horizontal facing variants that rotate the same model.
         * Produces keys like "facing=north" ...
         */
        public static BlockStateJson horizontalFacing(String model, boolean uvLock)
        {
            BlockStateJson bs = new BlockStateJson();
            bs.variants.put("facing=north", new Variant(model).rotY(180).uvlock(uvLock));
            bs.variants.put("facing=south", new Variant(model).rotY(0).uvlock(uvLock));
            bs.variants.put("facing=west",  new Variant(model).rotY(90).uvlock(uvLock));
            bs.variants.put("facing=east",  new Variant(model).rotY(270).uvlock(uvLock));
            return bs;
        }
    }
}
