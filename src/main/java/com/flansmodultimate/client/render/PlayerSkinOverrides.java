package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the SkinOverride of a team player class to the wearer's player model.
 * <p>
 * A texture is only ever used when it really ships with the loaded content packs and when its
 * size matches one of the two skin layouts, so that an old pack cannot force a skin onto players
 * that would render broken. Modern square sheets are handed to the player model untouched; legacy
 * 2:1 sheets are rebuilt into the modern layout first, see {@link #toModernLayout}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerSkinOverrides
{
    /** Width of a player skin sheet, in the units both layouts are authored in. */
    private static final int SKIN_WIDTH = 64;
    /** Height of a legacy skin sheet, which is also the offset of the limbs mirrored below it. */
    private static final int LEGACY_SKIN_HEIGHT = 32;
    /** Namespace-local folder the rebuilt legacy sheets are registered under. */
    private static final String CONVERTED_PATH_PREFIX = "skin_override/legacy/";
    /** Below this alpha a pixel counts as translucent for {@link #stripOpaqueHatLayer}. */
    private static final int OPAQUE_ALPHA_THRESHOLD = 128;
    private static final int ALPHA_MASK = 0xFF000000;

    private static volatile Map<UUID, String> classByPlayer = Map.of();
    private static final Map<String, Optional<ResourceLocation>> VALIDATED = new ConcurrentHashMap<>();
    /** Locations of the sheets this class generated, so they can be handed back to the texture manager. */
    private static final Set<ResourceLocation> CONVERTED = ConcurrentHashMap.newKeySet();

    /** Replaces the known player class assignments with the set just received from the server. */
    public static void setPlayerClasses(Map<UUID, String> classes)
    {
        classByPlayer = classes.isEmpty() ? Map.of() : Collections.unmodifiableMap(new HashMap<>(classes));
    }

    public static void clear()
    {
        classByPlayer = Map.of();
        clearValidationCache();
    }

    /** Forgets every validation result so reloaded resource packs are inspected again. */
    public static void clearValidationCache()
    {
        // Forgetting a result and freeing the texture it points at has to happen in one go on the
        // thread that draws players, or a conversion running in between would lose its texture.
        Runnable forget = () -> {
            VALIDATED.clear();
            if (CONVERTED.isEmpty())
                return;

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            List<ResourceLocation> converted = List.copyOf(CONVERTED);
            CONVERTED.clear();
            converted.forEach(textureManager::release);
        };
        if (RenderSystem.isOnRenderThread())
            forget.run();
        else
            RenderSystem.recordRenderCall(forget::run);
    }

    /**
     * The skin this player's class enforces, or null when there is none, when the texture is
     * unusable, or when this player is not drawn with the vanilla player renderer and model.
     */
    @Nullable
    public static ResourceLocation getSkin(AbstractClientPlayer player, EntityRenderer<?> renderer)
    {
        if (!ModClientConfig.get().enablePlayerClassSkinOverrides)
            return null;

        String playerClass = classByPlayer.get(player.getUUID());
        if (playerClass == null || !usesStandardPlayerModel(renderer))
            return null;
        return VALIDATED.computeIfAbsent(playerClass, PlayerSkinOverrides::validate).orElse(null);
    }

    /**
     * Another mod may render players with its own renderer or model. Overriding the texture of
     * a model we do not know the layout of would produce garbage, so leave those players alone.
     */
    private static boolean usesStandardPlayerModel(EntityRenderer<?> renderer)
    {
        return renderer != null
            && renderer.getClass() == PlayerRenderer.class
            && ((PlayerRenderer)renderer).getModel().getClass() == PlayerModel.class;
    }

    private static Optional<ResourceLocation> validate(String playerClassName)
    {
        PlayerClass playerClass = PlayerClass.getPlayerClass(playerClassName);
        if (playerClass == null)
            return Optional.empty();

        ResourceLocation texture = playerClass.getSkinOverrideTexture();
        if (texture == null || texture.getPath().isEmpty())
            return Optional.empty();

        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty())
        {
            FlansMod.log.warn("Ignoring SkinOverride '{}' of player class {}: texture {} was not found",
                playerClass.getSkinOverride(), playerClassName, texture);
            return Optional.empty();
        }

        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream))
        {
            int width = image.getWidth();
            int height = image.getHeight();
            int scale = width / SKIN_WIDTH;

            if (scale >= 1 && width % SKIN_WIDTH == 0)
            {
                // A square sheet is already what the modern player model samples.
                if (height == width)
                    return Optional.of(texture);
                if (height * 2 == width)
                    return Optional.of(convertLegacySkin(texture, image, scale));
            }
            FlansMod.log.warn("Ignoring SkinOverride '{}' of player class {}: {}x{} is not a player skin layout"
                    + " (expected a square sheet, or a 2:1 sheet for the old skin format,"
                    + " sized in multiples of {} pixels)",
                playerClass.getSkinOverride(), playerClassName, width, height, SKIN_WIDTH);
            return Optional.empty();
        }
        catch (IOException | IllegalArgumentException exception)
        {
            FlansMod.log.warn("Ignoring SkinOverride '{}' of player class {}: texture {} could not be read: {}",
                playerClass.getSkinOverride(), playerClassName, texture, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Registers the modern sheet built from a legacy one under its own location and returns it.
     * Two player classes may name the same texture, so an already converted sheet is reused
     * instead of being registered a second time, which would close the one still in use.
     */
    private static ResourceLocation convertLegacySkin(ResourceLocation source, NativeImage legacy, int scale)
    {
        ResourceLocation converted = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID,
            CONVERTED_PATH_PREFIX + source.getNamespace() + '/' + source.getPath());
        if (CONVERTED.add(converted))
        {
            Minecraft.getInstance().getTextureManager()
                .register(converted, new DynamicTexture(toModernLayout(legacy, scale)));
            FlansMod.log.debug("Rebuilt the legacy {}x{} SkinOverride texture {} into the modern skin layout",
                legacy.getWidth(), legacy.getHeight(), source);
        }
        return converted;
    }

    /**
     * Builds the square sheet the modern player model samples from a legacy 2:1 one.
     * <p>
     * The old format gave only the head a second layer and stored a single arm and a single leg,
     * which the model drew on both sides. The lower half of the modern sheet, which holds the left
     * arm and the left leg, is therefore filled with horizontally mirrored copies of the right ones
     * and its second layers are left empty. This is the same rebuild vanilla performs on downloaded
     * skins, scaled up so that HD skins keeping the 2:1 ratio are converted as well.
     *
     * @param scale the sheet's width in multiples of the {@value #SKIN_WIDTH} pixel layout
     */
    static NativeImage toModernLayout(NativeImage legacy, int scale)
    {
        NativeImage modern = new NativeImage(SKIN_WIDTH * scale, SKIN_WIDTH * scale, true);
        modern.copyFrom(legacy);
        modern.fillRect(0, LEGACY_SKIN_HEIGHT * scale, SKIN_WIDTH * scale, LEGACY_SKIN_HEIGHT * scale, 0);

        // Right leg to left leg: the top and bottom faces, then the four sides.
        mirrorBelow(modern, scale, 4, 16, 16, 4, 4);
        mirrorBelow(modern, scale, 8, 16, 16, 4, 4);
        mirrorBelow(modern, scale, 0, 20, 24, 4, 12);
        mirrorBelow(modern, scale, 4, 20, 16, 4, 12);
        mirrorBelow(modern, scale, 8, 20, 8, 4, 12);
        mirrorBelow(modern, scale, 12, 20, 16, 4, 12);
        // Right arm to left arm, likewise.
        mirrorBelow(modern, scale, 44, 16, -8, 4, 4);
        mirrorBelow(modern, scale, 48, 16, -8, 4, 4);
        mirrorBelow(modern, scale, 40, 20, 0, 4, 12);
        mirrorBelow(modern, scale, 44, 20, -8, 4, 12);
        mirrorBelow(modern, scale, 48, 20, -16, 4, 12);
        mirrorBelow(modern, scale, 52, 20, -8, 4, 12);

        // Base layers must be opaque, or the model would be see-through where the skin is not.
        setOpaque(modern, scale, 0, 0, 32, 16);
        stripOpaqueHatLayer(modern, scale, 32, 0, 64, 32);
        setOpaque(modern, scale, 0, 16, 64, 32);
        setOpaque(modern, scale, 16, 48, 48, 64);
        return modern;
    }

    /**
     * Copies a region onto the lower half of the sheet, flipped horizontally, which is what turns
     * the single arm and leg of the old format into the mirrored pair the modern layout stores.
     * All coordinates are in the {@value #SKIN_WIDTH} pixel layout and scaled up for HD skins.
     */
    private static void mirrorBelow(NativeImage image, int scale, int x, int y, int xOffset, int width, int height)
    {
        image.copyRect(x * scale, y * scale, xOffset * scale, LEGACY_SKIN_HEIGHT * scale,
            width * scale, height * scale, true, false);
    }

    /** Forces every pixel of a region to full alpha. Coordinates are scaled as in {@link #mirrorBelow}. */
    private static void setOpaque(NativeImage image, int scale, int x0, int y0, int x1, int y1)
    {
        for (int x = x0 * scale; x < x1 * scale; x++)
            for (int y = y0 * scale; y < y1 * scale; y++)
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | ALPHA_MASK);
    }

    /**
     * Old skins had no way to mark the hat layer as absent, so painting it fully opaque was how
     * editors said "no hat". A region without a single translucent pixel is therefore cleared,
     * because it would otherwise be drawn as a solid block around the head.
     */
    private static void stripOpaqueHatLayer(NativeImage image, int scale, int x0, int y0, int x1, int y1)
    {
        for (int x = x0 * scale; x < x1 * scale; x++)
            for (int y = y0 * scale; y < y1 * scale; y++)
                if ((image.getPixelRGBA(x, y) >> 24 & 0xFF) < OPAQUE_ALPHA_THRESHOLD)
                    return;

        for (int x = x0 * scale; x < x1 * scale; x++)
            for (int y = y0 * scale; y < y1 * scale; y++)
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) & ~ALPHA_MASK);
    }
}
