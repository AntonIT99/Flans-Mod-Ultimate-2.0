package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.*;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceUtils
{
    private static final String KEY_PARENT = "parent";
    private static final String MODEL_CUBE_BOTTOM_TOP = "minecraft:block/cube_bottom_top";
    private static final String KEY_TEXTURES = "textures";
    private static final String KEY_BOTTOM = "bottom";
    private static final String BLOCK_INFIX = ":block/";
    private static final String ITEM_INFIX = ":item/";
    private static final String MODEL_ITEM_GENERATED = "minecraft:item/generated";
    private static final String KEY_LAYER0 = "layer0";

    /** Recognize only the basic cube shape emitted by this mod, so authored geometry survives regeneration. */
    public static boolean isGeneratedBlockModel(JsonObject model)
    {
        if (model.size() != 2 || !model.has(KEY_PARENT) || !model.get(KEY_PARENT).isJsonPrimitive() || !model.get(KEY_PARENT).getAsString().equals(MODEL_CUBE_BOTTOM_TOP) || !model.has(KEY_TEXTURES)
            || !model.get(KEY_TEXTURES).isJsonObject())
            return false;
        Set<String> slots = model.getAsJsonObject(KEY_TEXTURES).keySet();
        return slots.equals(Set.of("top", KEY_BOTTOM, "side")) || slots.equals(Set.of("top", KEY_BOTTOM, "side", "particle"));
    }

    public static String sanitize(@Nullable String name)
    {
        if (name == null)
            return StringUtils.EMPTY;
        // Lowercase + swap spaces; keep only chars valid in resource paths
        return name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9._\\-]", "_");
    }

    public static String sanitizeFileNameStem(@Nullable String name)
    {
        if (name == null)
            return StringUtils.EMPTY;

        String normalizedName = name.trim().replace('\\', '/');
        int lastSeparator = normalizedName.lastIndexOf('/');
        if (lastSeparator >= 0)
            normalizedName = normalizedName.substring(lastSeparator + 1);

        if (normalizedName.toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
            normalizedName = normalizedName.substring(0, normalizedName.length() - 4);

        return sanitize(normalizedName);
    }

    public static class ModelJson
    {
        String parent;
        @SerializedName("gui_light")
        String guiLight;
        Map<String, String> textures;
        List<Override> overrides;

        public ModelJson(String parent, String guiLight, Map<String, String> textures, List<Override> overrides)
        {
            this.parent = parent;
            this.guiLight = guiLight;
            this.textures = textures;
            this.overrides = overrides;
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

        public static ModelJson createBlockModel(BlockType config)
        {
            String parent = MODEL_CUBE_BOTTOM_TOP;

            Map<String, String> textures = new LinkedHashMap<>();
            textures.put("top", FlansMod.FLANSMOD_ID + BLOCK_INFIX + config.getTopTextureName());
            textures.put(KEY_BOTTOM, FlansMod.FLANSMOD_ID + BLOCK_INFIX + config.getBottomTextureName());
            textures.put("side", FlansMod.FLANSMOD_ID + BLOCK_INFIX + config.getSideTextureName());

            return new ModelJson(parent, null, textures, null);
        }

        public static ModelJson createItemHolderBlockModel(ItemHolderType config)
        {
            String iconTexture = FlansMod.FLANSMOD_ID + ITEM_INFIX + config.getIcon();

            Map<String, String> textures = new LinkedHashMap<>();
            textures.put("particle", iconTexture);
            textures.put("top", iconTexture);
            textures.put(KEY_BOTTOM, iconTexture);
            textures.put("side", iconTexture);

            return new ModelJson(MODEL_CUBE_BOTTOM_TOP, null, textures, null);
        }

        public static ModelJson createItemModel(InfoType config)
        {
            if (config instanceof ItemHolderType)
            {
                return new ModelJson(MODEL_ITEM_GENERATED, null, Map.of(KEY_LAYER0, FlansMod.FLANSMOD_ID + ITEM_INFIX + config.getIcon()), null);
            }

            String parent = MODEL_ITEM_GENERATED;
            if (config.getType().isHasBlock())
                parent = FlansMod.FLANSMOD_ID + BLOCK_INFIX + config.getShortName();
            else if (config.getType().isHandHeldItem())
                parent = "minecraft:item/handheld";

            Map<String, String> textures = null;
            if (!config.getType().isHasBlock())
                textures = Map.of(KEY_LAYER0, FlansMod.FLANSMOD_ID + ITEM_INFIX + config.getIcon());

            List<Override> overrides = null;
            if (config instanceof PaintableType paintableType)
            {
                overrides = paintableType.getPaintjobs().values().stream().filter(p -> !p.equals(paintableType.getDefaultPaintjob()))
                    .map(p -> new Override(FlansMod.PAINTJOB.toString(), p.getId(), FlansMod.FLANSMOD_ID + ITEM_INFIX + p.getIcon())).toList();
            }

            return new ModelJson(parent, null, textures, overrides);
        }

        public static ModelJson createItemModel(InfoType config, Paintjob paintjob)
        {
            return createItemModel(config, paintjob.getIcon());
        }

        public static ModelJson createItemModel(InfoType config, String icon)
        {
            String parent = MODEL_ITEM_GENERATED;
            if (config.getType().isHasBlock())
                parent = FlansMod.FLANSMOD_ID + BLOCK_INFIX + config.getShortName();
            else if (config.getType().isHandHeldItem())
                parent = "minecraft:item/handheld";

            Map<String, String> textures = null;
            if (!config.getType().isHasBlock())
                textures = Map.of(KEY_LAYER0, FlansMod.FLANSMOD_ID + ITEM_INFIX + icon);

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
            if (type instanceof ItemHolderType)
                return BlockStateJson.horizontalFacing(FlansMod.FLANSMOD_ID + BLOCK_INFIX + type.getShortName(), true);
            return BlockStateJson.single(FlansMod.FLANSMOD_ID + BLOCK_INFIX + type.getShortName());
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
                this.x = x;
                return this;
            }

            public Variant rotY(int y)
            {
                this.y = y;
                return this;

            }

            public Variant uvlock(boolean uv)
            {
                this.uvLock = uv;
                return this;
            }

            public Variant weight(int w)
            {
                this.weight = w;
                return this;
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
            bs.variants.put("facing=west", new Variant(model).rotY(90).uvlock(uvLock));
            bs.variants.put("facing=east", new Variant(model).rotY(270).uvlock(uvLock));
            return bs;
        }
    }
}
