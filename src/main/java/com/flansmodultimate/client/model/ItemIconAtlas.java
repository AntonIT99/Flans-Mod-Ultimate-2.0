package com.flansmodultimate.client.model;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.mixin.SpriteSourcesAccessor;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * Content-pack item icons live in their own atlas instead of the block atlas, so they get the graphics card's full
 * texture size limit to themselves and cannot push the block atlas past it. Flan item models are baked against it and
 * drawn with render types bound to it; the block atlas keeps 16 pixel copies for particles and blocks.
 */
public final class ItemIconAtlas
{
    public static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/atlas/item_icons.png");
    static final int FALLBACK_ICON_SIZE = 16;
    private static final ResourceLocation ATLAS_INFO = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "item_icons");
    private static final String SPRITE_SOURCE_TYPE_ID = FlansMod.MOD_ID + ":item_icons";
    private static final String INVENTORY_VARIANT = "inventory";
    private static final long QUAD_SEED = 42L;
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final AtomicReference<SpriteSourceType> spriteSourceType = new AtomicReference<>();
    private static final AtomicReference<Set<ResourceLocation>> listedIcons = new AtomicReference<>(Set.of());
    private static final AtomicReference<Map<ResourceLocation, TextureAtlasSprite>> fallbackSprites = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<BakedModel, BakedModel>> wrappedModels = new AtomicReference<>(new ConcurrentHashMap<>());
    private static final Map<List<RenderType>, List<RenderType>> iconRenderTypes = new ConcurrentHashMap<>();

    private ItemIconAtlas()
    {}

    /** Called from the {@code ModelManager} constructor, before the first resource reload reads any atlas definition. */
    public static Map<ResourceLocation, ResourceLocation> withIconAtlas(Map<ResourceLocation, ResourceLocation> atlases)
    {
        spriteSourceType();
        Map<ResourceLocation, ResourceLocation> withIcons = new HashMap<>(atlases);
        withIcons.put(ATLAS, ATLAS_INFO);
        return withIcons;
    }

    /** Registers the sprite source type once; vanilla rejects a second registration of the same id. */
    static synchronized SpriteSourceType spriteSourceType()
    {
        SpriteSourceType type = spriteSourceType.get();
        if (type == null)
        {
            type = SpriteSourcesAccessor.flansmodultimateRegister(SPRITE_SOURCE_TYPE_ID, ItemIconSpriteSource.CODEC);
            spriteSourceType.set(type);
        }
        return type;
    }

    /** The configured largest icon side in pixels, or 0 when icons keep their size. */
    static int configuredIconSizeLimit()
    {
        ModClientConfig config = ModClientConfig.get();
        return config == null ? 0 : config.itemIconSizeLimit.getPixels();
    }

    static void setListedIcons(Set<ResourceLocation> icons)
    {
        listedIcons.set(Set.copyOf(icons));
    }

    /**
     * Wraps the sprite lookup of one model baking pass: Flan item models get their icons from the icon atlas, everything
     * else keeps the block atlas. Runs after both atlases are stitched, so the listed icons are current.
     */
    public static BiFunction<ResourceLocation, Material, TextureAtlasSprite> iconSpriteGetter(BiFunction<ResourceLocation, Material, TextureAtlasSprite> sprites)
    {
        Set<ResourceLocation> icons = listedIcons.get();
        Map<ResourceLocation, TextureAtlasSprite> fallbacks = new ConcurrentHashMap<>();
        fallbackSprites.set(fallbacks);
        wrappedModels.set(new ConcurrentHashMap<>());
        return (model, material) ->
        {
            if (!TextureAtlas.LOCATION_BLOCKS.equals(material.atlasLocation()) || !icons.contains(material.texture()) || !isFlanItemModel(model))
                return sprites.apply(model, material);
            fallbacks.computeIfAbsent(material.texture(), texture -> sprites.apply(model, material));
            return sprites.apply(model, new Material(ATLAS, material.texture()));
        };
    }

    /** Lets a baked Flan item model draw its icon-atlas quads with the icon atlas bound. */
    public static BakedModel wrap(ResourceLocation location, BakedModel model)
    {
        return isFlanItemModel(location) ? wrap(model) : model;
    }

    /** Override and transform results are wrapped once per reload, as they are resolved every frame. */
    static BakedModel wrapResolved(BakedModel model)
    {
        if (model instanceof ItemIconAtlasModel)
            return model;
        return wrappedModels.get().computeIfAbsent(model, ItemIconAtlas::wrap);
    }

    /** The width in pixels of the icon a flat item model draws; its particle icon is the small block atlas copy. */
    public static int iconWidth(BakedModel model)
    {
        List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create(QUAD_SEED));
        TextureAtlasSprite sprite = quads.isEmpty() ? model.getParticleIcon() : quads.get(0).getSprite();
        return sprite.contents().width();
    }

    static boolean isIconQuad(BakedQuad quad)
    {
        return ATLAS.equals(quad.getSprite().atlasLocation());
    }

    static RandomSource quadRandom()
    {
        return RandomSource.create(QUAD_SEED);
    }

    /** The same render types bound to the icon atlas. Item render types are memoized per texture, so lists are cached. */
    static List<RenderType> iconRenderTypes(List<RenderType> types)
    {
        List<RenderType> mapped = iconRenderTypes.get(types);
        if (mapped == null)
        {
            mapped = types.stream().map(ItemIconAtlas::iconRenderType).toList();
            iconRenderTypes.putIfAbsent(List.copyOf(types), mapped);
        }
        return mapped;
    }

    private static RenderType iconRenderType(RenderType type)
    {
        if (type == Sheets.solidBlockSheet())
            return RenderType.entitySolid(ATLAS);
        if (type == Sheets.cutoutBlockSheet())
            return RenderType.entityCutout(ATLAS);
        if (type == Sheets.translucentItemSheet())
            return RenderType.itemEntityTranslucentCull(ATLAS);
        return RenderType.entityTranslucentCull(ATLAS);
    }

    /** Particles render from the block atlas, so icon-atlas models report the block atlas copy of their icon. */
    static TextureAtlasSprite particleIcon(TextureAtlasSprite sprite)
    {
        if (!ATLAS.equals(sprite.atlasLocation()))
            return sprite;
        TextureAtlasSprite fallback = fallbackSprites.get().get(sprite.contents().name());
        return fallback != null ? fallback : sprite;
    }

    private static BakedModel wrap(BakedModel model)
    {
        if (model instanceof ItemIconAtlasModel)
            return model;
        boolean iconQuads = false;
        boolean otherQuads = false;
        for (Direction face : FACES)
        {
            for (BakedQuad quad : model.getQuads(null, face, quadRandom()))
            {
                if (isIconQuad(quad))
                    iconQuads = true;
                else
                    otherQuads = true;
            }
        }
        if (!iconQuads && model.getOverrides() == ItemOverrides.EMPTY)
            return model;
        return new ItemIconAtlasModel(model, iconQuads, otherQuads);
    }

    private static boolean isFlanItemModel(ResourceLocation location)
    {
        return location instanceof ModelResourceLocation modelLocation && INVENTORY_VARIANT.equals(modelLocation.getVariant())
            && FlansMod.FLANSMOD_ID.equals(modelLocation.getNamespace());
    }
}
