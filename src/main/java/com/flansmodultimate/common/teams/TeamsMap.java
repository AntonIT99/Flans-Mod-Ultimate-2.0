package com.flansmodultimate.common.teams;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Pattern;

public final class TeamsMap
{
    private static final String NBT_ID = "id";
    private static final String NBT_NAME = "name";
    private static final String NBT_DIMENSION = "dimension";
    private static final String NBT_BASES = "bases";
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";
    private static final String NBT_CAPTURE_POINT = "capture_point";
    private static final String NBT_BOUNDS = "bounds";
    private static final String NBT_DEPLOY_IMAGE = "deploy_image";
    private static final String NBT_DEPLOY_UV = "deploy_uv";
    private static final String NBT_DEPLOY_LAYOUT = "deploy_layout";
    private static final String NBT_KEY = "key";
    /** Deployment images are resource names, so anything that could leave the image folder is refused. */
    private static final Pattern IMAGE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    /** The smallest slice of the image the deployment map may be cropped to, on either axis. */
    private static final float MIN_UV_SPAN = 0.02F;
    /** More entries than any real map needs; bounds what one layout save can store. */
    public static final int MAX_DEPLOY_LAYOUT_ENTRIES = 64;

    /** An axis-aligned playing area; players of a round who leave it are warned, then killed. */
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        public static Bounds of(BlockPos first, BlockPos second)
        {
            return new Bounds(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()), Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
        }

        public boolean contains(Vec3 position)
        {
            return position.x >= minX && position.x <= maxX && position.y >= minY && position.y <= maxY && position.z >= minZ && position.z <= maxZ;
        }

        int[] toArray()
        {
            return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
        }
    }

    /** The part of the deployment image shown behind the spawn choices, in texture fractions. */
    public record ImageCrop(float minU, float minV, float maxU, float maxV)
    {
        public static final ImageCrop FULL = new ImageCrop(0F, 0F, 1F, 1F);

        /** Orders the corners and keeps the crop at least {@link TeamsMap#MIN_UV_SPAN} wide and tall. */
        public static ImageCrop normalised(float minU, float minV, float maxU, float maxV)
        {
            float[] u = span(DeployLayoutEntry.clamp(minU), DeployLayoutEntry.clamp(maxU));
            float[] v = span(DeployLayoutEntry.clamp(minV), DeployLayoutEntry.clamp(maxV));
            return new ImageCrop(u[0], v[0], u[1], v[1]);
        }

        private static float[] span(float first, float second)
        {
            float min = Math.min(first, second);
            float max = Math.max(first, second);
            if (max - min < MIN_UV_SPAN)
            {
                min = Math.min(min, 1F - MIN_UV_SPAN);
                max = min + MIN_UV_SPAN;
            }
            return new float[]{min, max};
        }
    }

    @Getter
    private final String shortName;
    @Getter
    private String name;
    @Getter @Setter
    private ResourceKey<Level> dimension;
    private final Set<UUID> bases = new LinkedHashSet<>();
    private final Map<UUID, BlockPos> basePositions = new LinkedHashMap<>();
    /** Bases known to be capture points, kept so a map can be judged without loading its chunks. */
    private final Set<UUID> capturePoints = new LinkedHashSet<>();
    @Getter @Setter @Nullable
    private Bounds bounds;
    /** Name of the image under {@code textures/gui/deploy/}, or blank for the plain background. */
    @Getter
    private String deployImage = StringUtils.EMPTY;
    @Getter
    private ImageCrop deployImageCrop = ImageCrop.FULL;
    private final Map<String, DeployLayoutEntry> deployLayout = new LinkedHashMap<>();

    public TeamsMap(String shortName, String name, ResourceKey<Level> dimension)
    {
        if (StringUtils.isBlank(shortName))
            throw new IllegalArgumentException("Map short name cannot be blank");
        this.shortName = shortName.trim().toLowerCase(java.util.Locale.ROOT);
        this.name = StringUtils.defaultIfBlank(name, shortName).trim();
        this.dimension = dimension;
    }

    public void setName(String name)
    {
        this.name = StringUtils.defaultIfBlank(name, shortName).trim();
    }

    public Set<UUID> getBases()
    {
        return Collections.unmodifiableSet(bases);
    }

    public boolean addBase(UUID id)
    {
        return bases.add(id);
    }

    public boolean addBase(UUID id, BlockPos position)
    {
        basePositions.put(id, position.immutable());
        return bases.add(id);
    }

    public boolean removeBase(UUID id)
    {
        basePositions.remove(id);
        capturePoints.remove(id);
        return bases.remove(id);
    }

    public Map<UUID, BlockPos> getBasePositions()
    {
        return Collections.unmodifiableMap(basePositions);
    }

    /** Records whether a base of this map is a capture point. Returns whether that changed anything. */
    public boolean markCapturePoint(UUID id, boolean capturePoint)
    {
        return capturePoint ? bases.contains(id) && capturePoints.add(id) : capturePoints.remove(id);
    }

    /** Whether the objective game types have anything to fight over here. */
    public boolean hasCapturePoints()
    {
        return !capturePoints.isEmpty();
    }

    /** Sets the deployment image by resource name. Returns false, changing nothing, for an unsafe name. */
    public boolean setDeployImage(@Nullable String image)
    {
        if (StringUtils.isBlank(image) || "none".equalsIgnoreCase(image))
        {
            deployImage = StringUtils.EMPTY;
            return true;
        }
        String cleaned = StringUtils.removeEndIgnoreCase(image.trim(), ".png");
        if (!IMAGE_NAME.matcher(cleaned).matches())
            return false;
        deployImage = cleaned.toLowerCase(java.util.Locale.ROOT);
        return true;
    }

    public void setDeployImageCrop(float minU, float minV, float maxU, float maxV)
    {
        deployImageCrop = ImageCrop.normalised(minU, minV, maxU, maxV);
    }

    public List<DeployLayoutEntry> getDeployLayout()
    {
        return List.copyOf(deployLayout.values());
    }

    /** Replaces the whole deployment layout, dropping entries with unusable keys. */
    public void replaceDeployLayout(Collection<DeployLayoutEntry> entries)
    {
        deployLayout.clear();
        for (DeployLayoutEntry entry : entries)
            if (entry != null && entry.isValid() && deployLayout.size() < MAX_DEPLOY_LAYOUT_ENTRIES)
                deployLayout.put(entry.key(), entry);
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, shortName);
        tag.putString(NBT_NAME, name);
        tag.putString(NBT_DIMENSION, dimension.location().toString());
        ListTag baseList = new ListTag();
        for (UUID id : bases)
        {
            CompoundTag base = new CompoundTag();
            base.putUUID(NBT_ID, id);
            BlockPos position = basePositions.get(id);
            if (position != null)
            {
                base.putInt(NBT_X, position.getX());
                base.putInt(NBT_Y, position.getY());
                base.putInt(NBT_Z, position.getZ());
            }
            if (capturePoints.contains(id))
                base.putBoolean(NBT_CAPTURE_POINT, true);
            baseList.add(base);
        }
        tag.put(NBT_BASES, baseList);
        if (bounds != null)
            tag.putIntArray(NBT_BOUNDS, bounds.toArray());
        tag.putString(NBT_DEPLOY_IMAGE, deployImage);
        CompoundTag crop = new CompoundTag();
        crop.putFloat("min_u", deployImageCrop.minU());
        crop.putFloat("min_v", deployImageCrop.minV());
        crop.putFloat("max_u", deployImageCrop.maxU());
        crop.putFloat("max_v", deployImageCrop.maxV());
        tag.put(NBT_DEPLOY_UV, crop);
        ListTag layout = new ListTag();
        for (DeployLayoutEntry entry : deployLayout.values())
        {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(NBT_KEY, entry.key());
            entryTag.putFloat(NBT_X, entry.x());
            entryTag.putFloat(NBT_Y, entry.y());
            layout.add(entryTag);
        }
        tag.put(NBT_DEPLOY_LAYOUT, layout);
        return tag;
    }

    public static TeamsMap load(CompoundTag tag)
    {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(NBT_DIMENSION));
        if (dimensionId == null)
            dimensionId = Level.OVERWORLD.location();
        TeamsMap map = new TeamsMap(tag.getString(NBT_ID), tag.getString(NBT_NAME), ResourceKey.create(Registries.DIMENSION, dimensionId));
        for (Tag entry : tag.getList(NBT_BASES, Tag.TAG_COMPOUND))
        {
            CompoundTag base = (CompoundTag) entry;
            if (base.hasUUID(NBT_ID))
            {
                UUID id = base.getUUID(NBT_ID);
                map.bases.add(id);
                if (base.contains(NBT_X) && base.contains(NBT_Y) && base.contains(NBT_Z))
                    map.basePositions.put(id, new BlockPos(base.getInt(NBT_X), base.getInt(NBT_Y), base.getInt(NBT_Z)));
                if (base.getBoolean(NBT_CAPTURE_POINT))
                    map.capturePoints.add(id);
            }
        }
        loadObjectiveData(tag, map);
        return map;
    }

    /** Reads what was added with the objective game types; maps saved before them have none of it. */
    private static void loadObjectiveData(CompoundTag tag, TeamsMap map)
    {
        int[] bounds = tag.getIntArray(NBT_BOUNDS);
        if (bounds.length == 6)
            map.bounds = Bounds.of(new BlockPos(bounds[0], bounds[1], bounds[2]), new BlockPos(bounds[3], bounds[4], bounds[5]));
        map.setDeployImage(tag.getString(NBT_DEPLOY_IMAGE));
        if (tag.contains(NBT_DEPLOY_UV, Tag.TAG_COMPOUND))
        {
            CompoundTag crop = tag.getCompound(NBT_DEPLOY_UV);
            map.setDeployImageCrop(crop.getFloat("min_u"), crop.getFloat("min_v"), crop.getFloat("max_u"), crop.getFloat("max_v"));
        }
        for (Tag entry : tag.getList(NBT_DEPLOY_LAYOUT, Tag.TAG_COMPOUND))
        {
            CompoundTag entryTag = (CompoundTag) entry;
            DeployLayoutEntry layoutEntry = new DeployLayoutEntry(entryTag.getString(NBT_KEY), entryTag.getFloat(NBT_X), entryTag.getFloat(NBT_Y));
            if (layoutEntry.isValid())
                map.deployLayout.put(layoutEntry.key(), layoutEntry);
        }
    }
}
