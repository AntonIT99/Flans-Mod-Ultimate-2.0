package com.flansmodultimate.client.teams;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.teams.TeamFlagUrl;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.FlansLog;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The flag image of a team, wherever it is drawn: a texture of a resource pack, or a PNG the client
 * downloads once per session from the web address the Teams editor set. A flag still downloading,
 * or one that failed, has no image, so the caller draws its plain flag in the team colour meanwhile.
 */
public final class TeamFlagTextures
{
    /** A flag image and its size in pixels, so that it can be drawn without distortion. */
    public record FlagTexture(ResourceLocation location, int width, int height)
    {
        public float aspect()
        {
            return width / (float) height;
        }
    }

    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_SIZE = 1024;
    /** Downloads per session; a server cannot make a client fetch more images than this. */
    private static final int MAX_DOWNLOADS = 64;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String TEXTURE_PATH = "team_flags/";

    /** Web addresses requested this session, including those still downloading and those that failed. */
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Map<String, FlagTexture> DOWNLOADED = new ConcurrentHashMap<>();
    /** Pack textures by location: empty when the texture is missing or unreadable. */
    private static final Map<ResourceLocation, Optional<FlagTexture>> LOCAL = new HashMap<>();
    /** Session the downloads in flight belong to, so that one finishing after a disconnect is dropped. */
    private static final AtomicInteger SESSION = new AtomicInteger();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();

    private TeamFlagTextures()
    {}

    /** The flag of {@code team} to draw now, or null for its plain flag in the team colour. */
    @Nullable
    public static FlagTexture get(@Nullable Team team)
    {
        if (team == null)
            return null;
        String url = team.getVictoryFlagUrl();
        if (url != null)
            return remote(url);
        ResourceLocation location = team.getVictoryFlagTextureLocation();
        return location == null ? null : local(location);
    }

    /** The flag an editor value names, a texture location or a web address, or null when it names none yet. */
    @Nullable
    public static FlagTexture preview(@Nullable String value)
    {
        if (value == null || value.isBlank())
            return null;
        if (TeamFlagUrl.isUrl(value))
            return remote(value.trim());
        ResourceLocation location = ResourceLocation.tryParse(value);
        return location == null ? null : local(location);
    }

    /** Draws {@code image} as large as fits the box at {@code x}, {@code y} without distortion, centred in it. */
    public static void draw(GuiGraphics graphics, FlagTexture image, int x, int y, int boxWidth, int boxHeight)
    {
        int width = Math.max(1, Math.min(boxWidth, Math.round(boxHeight * image.aspect())));
        int height = Math.max(1, Math.min(boxHeight, Math.round(width / image.aspect())));
        graphics.blit(image.location(), x + (boxWidth - width) / 2, y + (boxHeight - height) / 2, width, height, 0F, 0F, image.width(), image.height(), image.width(), image.height());
    }

    @Nullable
    private static FlagTexture local(ResourceLocation location)
    {
        return LOCAL.computeIfAbsent(location, TeamFlagTextures::readLocal).orElse(null);
    }

    @Nullable
    private static FlagTexture remote(String url)
    {
        if (!ModClientConfig.get().loadTeamFlagsFromUrls)
            return null;
        FlagTexture downloaded = DOWNLOADED.get(url);
        if (downloaded != null || REQUESTED.contains(url))
            return downloaded;
        URI uri = TeamFlagUrl.parse(url);
        if (uri != null && REQUESTED.size() < MAX_DOWNLOADS && REQUESTED.add(url))
            startDownload(url, uri, SESSION.get());
        return null;
    }

    private static void startDownload(String url, URI uri, int session)
    {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> download(uri), Util.backgroundExecutor()).whenCompleteAsync((image, failure) ->
        {
            if (failure != null)
            {
                Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
                FlansLog.log.warn("Could not load the team flag {}: {}", url, cause.getMessage());
                return;
            }
            if (session != SESSION.get())
            {
                image.close();
                return;
            }
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, TEXTURE_PATH + Hashing.sha256().hashString(url, StandardCharsets.UTF_8));
            FlagTexture texture = new FlagTexture(location, image.getWidth(), image.getHeight());
            minecraft.getTextureManager().register(location, new DynamicTexture(image));
            DOWNLOADED.put(url, texture);
        }, minecraft);
    }

    private static NativeImage download(URI uri)
    {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("Accept", "image/png").header("User-Agent", FlansMod.MOD_ID).GET().build();
        try
        {
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body())
            {
                if (response.statusCode() != 200)
                    throw new IOException("the server answered HTTP " + response.statusCode());
                byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES)
                    throw new IOException("the image is larger than " + MAX_BYTES / 1024 / 1024 + " MiB");
                return checkedImage(new ByteArrayInputStream(bytes));
            }
        }
        catch (IOException exception)
        {
            throw new CompletionException(exception);
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new CompletionException(exception);
        }
    }

    /** Reads a PNG no larger than {@link #MAX_SIZE} pixels a side. */
    private static NativeImage checkedImage(InputStream stream) throws IOException
    {
        NativeImage image = NativeImage.read(stream);
        if (image.getWidth() > MAX_SIZE || image.getHeight() > MAX_SIZE)
        {
            int width = image.getWidth();
            int height = image.getHeight();
            image.close();
            throw new IOException("the image is " + width + "x" + height + " pixels, more than " + MAX_SIZE + " a side");
        }
        return image;
    }

    private static Optional<FlagTexture> readLocal(ResourceLocation location)
    {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty())
        {
            FlansLog.log.warn("Team flag texture {} was not found", location);
            return Optional.empty();
        }
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream))
        {
            return Optional.of(new FlagTexture(location, image.getWidth(), image.getHeight()));
        }
        catch (IOException | IllegalArgumentException exception)
        {
            FlansLog.log.warn("Team flag texture {} could not be read: {}", location, exception.getMessage());
            return Optional.empty();
        }
    }

    /** Forgets every flag and releases the downloaded images, on leaving a world or server. */
    public static void clear()
    {
        SESSION.incrementAndGet();
        LOCAL.clear();
        List<ResourceLocation> downloaded = DOWNLOADED.values().stream().map(FlagTexture::location).toList();
        DOWNLOADED.clear();
        REQUESTED.clear();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> downloaded.forEach(minecraft.getTextureManager()::release));
    }
}
