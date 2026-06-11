package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.GloveType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ItemHolderType;
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

    @AllArgsConstructor
    public static class ModelJson
    {
        String parent;
        String credit;
        @SerializedName("gui_light")
        String guiLight;
        Map<String, String> textures;
        List<Element> elements;
        Map<String, DisplayTransform> display;
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

        @AllArgsConstructor
        static class Face
        {
            List<Number> uv;
            String texture;
        }

        @AllArgsConstructor
        static class Rotation
        {
            Number angle;
            String axis;
            List<Number> origin;
        }

        @AllArgsConstructor
        static class Element
        {
            String name;
            List<Number> from;
            List<Number> to;
            Rotation rotation;
            Map<String, Face> faces;
        }

        @AllArgsConstructor
        static class DisplayTransform
        {
            List<Number> rotation;
            List<Number> translation;
            List<Number> scale;
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

        public static ModelJson createItemHolderBlockModel(ItemHolderType config)
        {
            String iconTexture = FlansMod.FLANSMOD_ID + ":item/" + config.getIcon();

            Map<String, String> textures = new LinkedHashMap<>();
            textures.put("particle", iconTexture);
            textures.put("top", iconTexture);
            textures.put("bottom", iconTexture);
            textures.put("side", iconTexture);

            return new ModelJson("minecraft:block/cube_bottom_top", null, textures, null);
        }

        public static ModelJson createItemModel(InfoType config)
        {
            if (config instanceof GloveType)
                return createGloveItemModel(config);

            if (config instanceof ItemHolderType)
            {
                return new ModelJson("minecraft:item/generated", null,
                    Map.of("layer0", FlansMod.FLANSMOD_ID + ":item/" + config.getIcon()), null);
            }

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

        private static ModelJson createGloveItemModel(InfoType config)
        {
            Map<String, String> textures = new LinkedHashMap<>();
            String texture = FlansMod.FLANSMOD_ID + ":item/" + config.getIcon();
            textures.put("0", texture);
            textures.put("particle", texture);

            ModelJson model = new ModelJson(null, null, textures, null);
            model.credit = "Made with Blockbench";
            model.elements = List.of(
                element(null, vec(5, 5, 5), vec(11, 11, 11), null, faces(
                    face(0, 0, 6, 6), face(0, 0, 6, 6), face(0, 0, 6, 6),
                    face(0, 0, 6, 6), face(0, 0, 6, 6), face(0, 0, 6, 6))),
                element(null, vec(6, 6, 4), vec(10, 10, 5), rotation(0, "z", vec(8, 8, 4)), faces(
                    face(0, 8, 4, 12), face(4, 8, 5, 12), face(0, 12, 4, 16),
                    face(4, 12, 5, 16), face(0, 6, 4, 7), face(0, 7, 4, 8))),
                element(null, vec(5.5, 5.5, 9), vec(10.5, 10.5, 13), null, faces(
                    face(0, 0, 5, 5), face(0, 0, 4, 5), face(0, 0, 5, 5),
                    face(0, 0, 4, 5), face(0, 0, 5, 4), face(0, 0, 5, 4))),
                element("spike", vec(9.25, 9.75, 6), vec(10.25, 10.75, 10), rotation(-22.5, "x", vec(9, 10, 6)), faces(
                    face(10, 0, 11, 1), face(12, 1, 16, 2), face(11, 1, 12, 2),
                    face(12, 0, 16, 1), face(10, 0, 11, 4), face(9, 0, 10, 4))),
                element("spike", vec(5.75, 9.5, 6), vec(6.75, 10.5, 10), rotation(-22.5, "x", vec(6, 10, 6)), faces(
                    face(10, 0, 11, 1), face(12, 1, 16, 2), face(11, 1, 12, 2),
                    face(12, 0, 16, 1), face(10, 0, 11, 4), face(9, 0, 10, 4))),
                element("spike", vec(7.5, 9.625, 6), vec(8.5, 10.625, 10), rotation(-22.5, "x", vec(8, 10, 6)), faces(
                    face(10, 0, 11, 1), face(12, 1, 16, 2), face(11, 1, 12, 2),
                    face(12, 0, 16, 1), face(10, 0, 11, 4), face(9, 0, 10, 4)))
            );

            Map<String, DisplayTransform> display = new LinkedHashMap<>();
            display.put("thirdperson_righthand", display(null, vec(0, -2, 0), vec(1, 1, 1)));
            display.put("thirdperson_lefthand", display(null, vec(0, -2, 0), null));
            display.put("firstperson_righthand", display(null, null, vec(1, 1, 1)));
            display.put("gui", display(vec(45, 135, 0), null, vec(1.2, 1.2, 1.2)));
            display.put("fixed", display(vec(-90, 0, 0), null, null));
            model.display = display;
            return model;
        }

        private static Element element(String name, List<Number> from, List<Number> to, Rotation rotation, Map<String, Face> faces)
        {
            return new Element(name, from, to, rotation, faces);
        }

        private static Face face(Number minU, Number minV, Number maxU, Number maxV)
        {
            return new Face(vec(minU, minV, maxU, maxV), "#0");
        }

        private static Rotation rotation(Number angle, String axis, List<Number> origin)
        {
            return new Rotation(angle, axis, origin);
        }

        private static DisplayTransform display(List<Number> rotation, List<Number> translation, List<Number> scale)
        {
            return new DisplayTransform(rotation, translation, scale);
        }

        private static Map<String, Face> faces(Face north, Face east, Face south, Face west, Face up, Face down)
        {
            Map<String, Face> faces = new LinkedHashMap<>();
            faces.put("north", north);
            faces.put("east", east);
            faces.put("south", south);
            faces.put("west", west);
            faces.put("up", up);
            faces.put("down", down);
            return faces;
        }

        private static List<Number> vec(Number... values)
        {
            return List.of(values);
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
            if (type instanceof ItemHolderType)
                return BlockStateJson.horizontalFacing(FlansMod.FLANSMOD_ID + ":block/" + type.getShortName(), true);
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
