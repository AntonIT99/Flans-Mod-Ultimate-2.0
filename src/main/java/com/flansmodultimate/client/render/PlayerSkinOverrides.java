package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the SkinOverride of a team player class to the wearer's player model.
 * <p>
 * A texture is only ever used when it really ships with the loaded content packs and
 * when its size matches the layout the player model expects, so that an old pack cannot
 * force a skin onto players that would render broken.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerSkinOverrides
{
    /** Player model skins are square sheets whose width is a whole number of 64 pixel tiles. */
    private static final int SKIN_TILE_SIZE = 64;

    private static volatile Map<UUID, String> classByPlayer = Map.of();
    private static final Map<String, Optional<ResourceLocation>> VALIDATED = new ConcurrentHashMap<>();

    /** Replaces the known player class assignments with the set just received from the server. */
    public static void setPlayerClasses(Map<UUID, String> classes)
    {
        classByPlayer = classes.isEmpty() ? Map.of() : Collections.unmodifiableMap(new HashMap<>(classes));
    }

    public static void clear()
    {
        classByPlayer = Map.of();
        VALIDATED.clear();
    }

    /** Forgets every validation result so reloaded resource packs are inspected again. */
    public static void clearValidationCache()
    {
        VALIDATED.clear();
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
            if (!isPlayerSkinLayout(image.getWidth(), image.getHeight()))
            {
                FlansMod.log.warn("Ignoring SkinOverride '{}' of player class {}: {}x{} is not a player skin layout"
                        + " (expected a square sheet sized in multiples of {} pixels)",
                    playerClass.getSkinOverride(), playerClassName, image.getWidth(), image.getHeight(), SKIN_TILE_SIZE);
                return Optional.empty();
            }
        }
        catch (IOException | IllegalArgumentException exception)
        {
            FlansMod.log.warn("Ignoring SkinOverride '{}' of player class {}: texture {} could not be read: {}",
                playerClass.getSkinOverride(), playerClassName, texture, exception.getMessage());
            return Optional.empty();
        }
        return Optional.of(texture);
    }

    /**
     * The player model maps its arms and its outer layer onto the square 64x64 sheet.
     * Legacy 64x32 skins are rejected on purpose: they lack both and would render with
     * holes and mirrored limbs.
     */
    private static boolean isPlayerSkinLayout(int width, int height)
    {
        return width == height && width >= SKIN_TILE_SIZE && width % SKIN_TILE_SIZE == 0;
    }
}
