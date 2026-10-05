package com.flansmodultimate.client.distant.dh;

import com.flansmodultimate.client.distant.DistantBox;
import com.flansmodultimate.client.distant.DistantBoxStyle;
import com.flansmodultimate.client.distant.DistantRaycastMath;
import com.flansmodultimate.client.distant.IDistantBoxGroup;
import com.flansmodultimate.client.distant.IDistantTerrain;
import com.mojang.logging.LogUtils;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelUnloadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiRaycastResult;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link IDistantTerrain} backed by the Distant Horizons API. The only class that touches Distant Horizons,
 * and only loaded once the mod is known to be present: {@link #create()} checks the API version first.
 *
 * <p>Boxes are drawn through Distant Horizons' generic object renderer, which depth tests them against its
 * terrain and fogs them with it. Raycasts read its terrain database on a worker thread of their own, with a
 * cache per level, because each read can decompress a whole section.</p>
 */
public final class DhDistantTerrain implements IDistantTerrain
{
    private static final Logger LOGGER = LogUtils.getLogger();
    /** API 5.1 added the level being rendered to render events, and API 5.0 the terrain data cache. */
    private static final int MIN_API_MAJOR = 5;
    private static final int MIN_API_MINOR = 1;
    private static final String NAMESPACE = "FlansModUltimate";

    private final ExecutorService raycastWorker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Flan's Mod Distant Horizons Rangefinder");
        thread.setDaemon(true);
        return thread;
    });
    private final List<Group> groups = new ArrayList<>();
    /** The client level Distant Horizons last rendered, captured on the render thread. */
    private final AtomicReference<@Nullable IDhApiLevelWrapper> renderedLevel = new AtomicReference<>();
    /** Set from any thread when Distant Horizons unloads a level; handled on the next tick. */
    private volatile boolean levelUnloaded;
    /** Increases whenever the level changes, which invalidates every group made before. */
    private long generation;
    /** {@link #drawsBoxes()}, worked out once per tick since every distant shape asks for it. */
    private boolean drawsBoxes;
    @Nullable
    private IDhApiLevelWrapper cacheLevel;
    @Nullable
    private IDhApiTerrainDataCache cache;

    private DhDistantTerrain()
    {
    }

    /** The Distant Horizons backend, or {@link IDistantTerrain#NONE} when its API is too old. */
    public static IDistantTerrain create()
    {
        int major = DhApi.getApiMajorVersion();
        int minor = DhApi.getApiMinorVersion();
        if (major < MIN_API_MAJOR || major == MIN_API_MAJOR && minor < MIN_API_MINOR)
        {
            LOGGER.warn("Distant Horizons {} provides API {}.{}; Flan's Mod needs {}.{} or newer, so its integration stays off",
                DhApi.getModVersion(), major, minor, MIN_API_MAJOR, MIN_API_MINOR);
            return NONE;
        }

        DhDistantTerrain terrain = new DhDistantTerrain();
        DhApiEventRegister.on(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent()
        {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event)
            {
                if (event.value != null)
                    terrain.renderedLevel.set(event.value.clientLevelWrapper);
            }
        });
        DhApiEventRegister.on(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent()
        {
            @Override
            public void onLevelUnload(DhApiEventParam<EventParam> event)
            {
                terrain.levelUnloaded = true;
            }
        });
        LOGGER.info("Distant Horizons {} (API {}.{}) found; enabling Flan's Mod far-terrain integration", DhApi.getModVersion(), major, minor);
        return terrain;
    }

    @Override
    public void tick()
    {
        if (levelUnloaded)
        {
            levelUnloaded = false;
            invalidate();
        }
        drawsBoxes = rendersGenericObjects();
    }

    @Override
    public void reset()
    {
        invalidate();
    }

    /** Drops every group and cache made for the level that was current until now. */
    private void invalidate()
    {
        generation++;
        renderedLevel.set(null);
        for (Group group : List.copyOf(groups))
            group.close();
        groups.clear();
        raycastWorker.execute(() -> {
            cache = null;
            cacheLevel = null;
        });
    }

    /** The Distant Horizons level for the client's current level, or null when it has none yet. */
    @Nullable
    private IDhApiLevelWrapper currentLevel()
    {
        Object clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null)
            return null;

        IDhApiLevelWrapper rendered = renderedLevel.get();
        if (rendered != null && rendered.getWrappedMcObject() == clientLevel)
            return rendered;

        // Before the first frame, or while rendering is off, look the level up instead
        IDhApiWorldProxy world = DhApi.Delayed.worldProxy;
        try
        {
            if (world != null && world.worldLoaded())
            {
                for (IDhApiLevelWrapper level : world.getAllLoadedLevelWrappers())
                {
                    if (level.getLevelType() == EDhApiLevelType.CLIENT_LEVEL && level.getWrappedMcObject() == clientLevel)
                        return level;
                }
            }
        }
        catch (IllegalStateException ignored)
        {
            // The world unloaded between the two calls
        }
        return null;
    }

    @Override
    public boolean drawsBoxes()
    {
        return drawsBoxes;
    }

    /** Whether Distant Horizons renders, renders generic objects such as boxes, and has a renderer for this level. */
    private boolean rendersGenericObjects()
    {
        IDhApiConfig configs = DhApi.Delayed.configs;
        if (configs == null || DhApi.Delayed.customRenderObjectFactory == null)
            return false;

        try
        {
            if (!Boolean.TRUE.equals(configs.graphics().renderingEnabled().getValue())
                || !Boolean.TRUE.equals(configs.graphics().genericRendering().renderingEnabled().getValue()))
                return false;
            IDhApiLevelWrapper level = currentLevel();
            return level != null && level.getRenderRegister() != null;
        }
        catch (RuntimeException | LinkageError exception)
        {
            return false;
        }
    }

    @Override
    public boolean canRaycast()
    {
        return DhApi.Delayed.terrainRepo != null && currentLevel() != null;
    }

    @Override
    public CompletableFuture<OptionalDouble> raycast(Vec3 origin, Vec3 direction, double maxDistance)
    {
        IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
        IDhApiLevelWrapper level = currentLevel();
        if (repo == null || level == null || maxDistance <= 0D || direction.lengthSqr() < 1.0E-12D)
            return CompletableFuture.completedFuture(OptionalDouble.empty());

        Vec3 unit = direction.normalize();
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                return raycastNow(repo, level, origin, unit, maxDistance);
            }
            catch (RuntimeException | LinkageError exception)
            {
                LOGGER.debug("Distant Horizons raycast failed", exception);
                return OptionalDouble.empty();
            }
        }, raycastWorker);
    }

    /** Runs on the raycast worker, which alone touches the cache. */
    private OptionalDouble raycastNow(IDhApiTerrainDataRepo repo, IDhApiLevelWrapper level, Vec3 origin, Vec3 direction, double maxDistance)
    {
        // Distant Horizons stops as soon as the ray is outside the level's height range, as it is when it
        // starts from an aircraft above the build limit, so start where the ray enters that range
        double bottom = level.getMinHeight();
        double top = level.getMaxHeight() - 1D;
        double skipped = 0D;
        if (origin.y > top)
        {
            if (direction.y >= -1.0E-6D)
                return OptionalDouble.empty();
            skipped = (origin.y - top) / -direction.y;
        }
        else if (origin.y < bottom)
        {
            if (direction.y <= 1.0E-6D)
                return OptionalDouble.empty();
            skipped = (bottom - origin.y) / direction.y;
        }
        if (skipped >= maxDistance)
            return OptionalDouble.empty();

        Vec3 start = origin.add(direction.scale(skipped));
        // Its length limit is a Manhattan distance, longer than the straight one by this factor
        double manhattanFactor = Math.abs(direction.x) + Math.abs(direction.y) + Math.abs(direction.z);
        int maxLength = (int) Math.min(Integer.MAX_VALUE, Math.ceil((maxDistance - skipped) * manhattanFactor) + 2D);
        DhApiResult<DhApiRaycastResult> result = repo.raycast(level, start.x, start.y, start.z,
            (float) direction.x, (float) direction.y, (float) direction.z, maxLength, cacheFor(repo, level));
        if (result == null || !result.success || result.payload == null || result.payload.dataPoint == null || result.payload.pos == null)
            return OptionalDouble.empty();

        double distance = skipped + DistantRaycastMath.entryDistance(start, direction, result.payload.pos.x,
            result.payload.dataPoint.bottomYBlockPos, result.payload.dataPoint.topYBlockPos, result.payload.pos.z);
        return distance <= maxDistance ? OptionalDouble.of(distance) : OptionalDouble.empty();
    }

    private IDhApiTerrainDataCache cacheFor(IDhApiTerrainDataRepo repo, IDhApiLevelWrapper level)
    {
        if (cache == null || cacheLevel != level)
        {
            cache = repo.createSoftCache();
            cacheLevel = level;
        }
        return cache;
    }

    @Override
    @Nullable
    public IDistantBoxGroup createGroup(String name, DistantBoxStyle style)
    {
        IDhApiCustomRenderObjectFactory factory = DhApi.Delayed.customRenderObjectFactory;
        IDhApiLevelWrapper level = currentLevel();
        IDhApiCustomRenderRegister register = level == null ? null : level.getRenderRegister();
        if (factory == null || register == null)
            return null;

        try
        {
            IDhApiRenderableBoxGroup boxes = factory.createRelativePositionedGroup(NAMESPACE + ":" + name, new DhApiVec3d(), new ArrayList<>());
            Group group = new Group(boxes, register, style, generation);
            group.setGlowing(false);
            // Ambient occlusion darkens the gaps between boxes, which a few boxes standing for a whole hull do not have
            boxes.setSsaoEnabled(false);
            boxes.setActive(false);
            register.add(boxes);
            groups.add(group);
            return group;
        }
        catch (RuntimeException | LinkageError exception)
        {
            LOGGER.debug("Could not create a Distant Horizons box group", exception);
            return null;
        }
    }

    private final class Group implements IDistantBoxGroup
    {
        private final IDhApiRenderableBoxGroup boxes;
        private final IDhApiCustomRenderRegister register;
        private final DistantBoxStyle style;
        private final long createdIn;
        private final EDhApiBlockMaterial material;
        private final AtomicReference<@Nullable Origin> origin = new AtomicReference<>();
        /** The boxes last handed to the renderer; unchanged ones are not uploaded again. */
        private List<DistantBox> current = List.of();
        @Nullable
        private Boolean glowing;
        private boolean closed;

        private Group(IDhApiRenderableBoxGroup boxes, IDhApiCustomRenderRegister register, DistantBoxStyle style, long createdIn)
        {
            this.boxes = boxes;
            this.register = register;
            this.style = style;
            this.createdIn = createdIn;
            this.material = switch (style)
            {
                case SOLID -> EDhApiBlockMaterial.METAL;
                case GLOW -> EDhApiBlockMaterial.ILLUMINATED;
                case SMOKE -> EDhApiBlockMaterial.UNKNOWN;
            };
            // Moved every frame rather than every tick, so fast aircraft glide instead of stepping
            boxes.setPreRenderFunc(this::beforeRender);
        }

        private void beforeRender(DhApiRenderParam parameters)
        {
            Origin o = origin.get();
            if (o == null)
                return;
            Vec3 position = o.at(parameters.partialTicks);
            boxes.setOriginBlockPos(new DhApiVec3d(position.x, position.y, position.z));
        }

        @Override
        public void setOrigin(Origin origin)
        {
            this.origin.set(origin);
            Vec3 position = origin.at(0F);
            boxes.setOriginBlockPos(new DhApiVec3d(position.x, position.y, position.z));
        }

        @Override
        public void setBoxes(List<DistantBox> source)
        {
            // A parked tank keeps its shape, so most updates change nothing worth uploading
            if (closed || source.equals(current))
                return;
            current = List.copyOf(source);

            // The renderer copies the list from another thread, so reuse the boxes in place whenever their
            // count is unchanged rather than emptying the list it may be copying
            if (boxes.size() != source.size())
            {
                boxes.clear();
                source.forEach(box -> boxes.add(new DhApiRenderableBox(new DhApiVec3d(), new DhApiVec3d(), Color.WHITE, material)));
            }
            for (int i = 0; i < source.size(); i++)
            {
                DistantBox box = source.get(i);
                DhApiRenderableBox target = boxes.get(i);
                target.minPos = new DhApiVec3d(box.minX(), box.minY(), box.minZ());
                target.maxPos = new DhApiVec3d(box.maxX(), box.maxY(), box.maxZ());
                if (target.color == null || target.color.getRGB() != box.argb())
                    target.color = new Color(box.argb(), true);
                target.material = material.index;
            }
            boxes.triggerBoxChange();
        }

        @Override
        public void setActive(boolean active)
        {
            if (!closed && boxes.isActive() != active)
                boxes.setActive(active);
        }

        @Override
        public void setGlowing(boolean glowing)
        {
            if (Boolean.valueOf(glowing).equals(this.glowing))
                return;
            this.glowing = glowing;
            boolean lit = glowing || style == DistantBoxStyle.GLOW;
            boxes.setSkyLight(15);
            boxes.setBlockLight(lit ? 15 : 0);
            boxes.setShading(lit ? DhApiRenderableBoxGroupShading.getUnshaded() : DhApiRenderableBoxGroupShading.getDefaultShaded());
        }

        @Override
        public boolean isValid()
        {
            return !closed && createdIn == generation;
        }

        @Override
        public void close()
        {
            if (closed)
                return;
            closed = true;
            groups.remove(this);
            try
            {
                register.remove(boxes.getId());
                // The implementation frees its buffers on the render thread; the API interface does not say so
                if (boxes instanceof Closeable closeable)
                    closeable.close();
            }
            catch (Exception exception)
            {
                LOGGER.debug("Could not free a Distant Horizons box group", exception);
            }
        }
    }
}
