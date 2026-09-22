package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.model.ModelMG;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.LogUtils;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelCache
{
    /**
     * @param className     name of the model class inside {@code contentPack}, which is the declared model
     *                      class name unless a legacy pack ships it below its own package
     * @param ownClassFile  true when the class file is shipped by the content pack the type belongs to, which
     *                      makes it take precedence over a model class of the same name compiled into the mod
     */
    private record ModelClassLocation(IContentProvider contentPack, String className, boolean ownClassFile) {}

    /**
     * @param contentPackName the content pack the model is loaded for, because the same model class name may
     *                        resolve to a different class file in every content pack
     */
    private record ModelCacheKey(String modelClassName, @Nullable String typeShortName, @Nullable String contentPackName)
    {
        public ModelCacheKey
        {
            typeShortName = StringUtils.isBlank(typeShortName) ? null : typeShortName;
        }
    }

    private static final Map<ModelCacheKey, Optional<IModelBase>> cache = new ConcurrentHashMap<>();
    private static final Map<IModelBase, List<EnumRenderPass>> renderPassCache = new ConcurrentHashMap<>();

    public static void reload()
    {
        GpuModelCache.clear();
        DriveableImpostorCache.clear();
        ModelTextureFitter.clear();
        cache.clear();
        renderPassCache.clear();
        if (ModClientConfig.get().loadAllModelsInCache)
            loadAll();
    }

    public static void loadAll()
    {
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            getOrLoadTypeModel(type);

            if (type instanceof GunType gunType)
            {
                if (StringUtils.isNotBlank(gunType.getDeployableModelClassName()))
                    getOrLoadDeployableGunModel(gunType);
                if (StringUtils.isNotBlank(gunType.getCasingModelClassName()))
                    getOrLoadCasingModel(gunType);
                if (StringUtils.isNotBlank(gunType.getFlashModelClassName()))
                    getOrLoadFlashModel(gunType);
                if (StringUtils.isNotBlank(gunType.getMuzzleFlashModelClassName()))
                    getOrLoadMuzzleFlashModel(gunType);
            }
        }
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        return getOrLoadModel(new ModelCacheKey(type.getModelClassName(), type.getShortName(), type.getContentPack().getName()), type, null, type.getTexture());
    }

    /**
     * The model for this type if it is already cached, without loading it.
     *
     * <p>For callers that are not on the render thread. Loading a model fits it to
     * its texture, which reads the atlas, so off-thread callers must take what is
     * already there and do without when there is nothing.</p>
     */
    @Nullable
    public static IModelBase getLoadedTypeModel(InfoType type)
    {
        ModelCacheKey key = new ModelCacheKey(type.getModelClassName(), type.getShortName(),
            type.getContentPack().getName());
        return cache.getOrDefault(key, Optional.empty()).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(ArmorType type)
    {
        return getOrLoadModel(new ModelCacheKey(type.getModelClassName(), type.getShortName(), type.getContentPack().getName()), type, new ModelDefaultArmor(type.getArmorItemType()), type.getTexture());
    }

    @Nullable
    public static ModelMG getOrLoadDeployableGunModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName(), gunType.getContentPack().getName()), gunType, null, gunType.getDeployableTexture()) instanceof ModelMG modelMG)
        {
            return modelMG;
        }
        return null;
    }

    @Nullable
    public static ModelCasing getOrLoadCasingModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getCasingModelClassName(), null, gunType.getContentPack().getName()), gunType, null, gunType.getCasingTexture()) instanceof ModelCasing modelCasing)
        {
            return modelCasing;
        }
        return null;
    }

    @Nullable
    public static ModelFlash getOrLoadFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getFlashModelClassName(), null, gunType.getContentPack().getName()), gunType, null, gunType.getFlashTexture()) instanceof ModelFlash modelFlash)
        {
            return modelFlash;
        }
        return null;
    }

    @Nullable
    public static ModelMuzzleFlash getOrLoadMuzzleFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getMuzzleFlashModelClassName(), null, gunType.getContentPack().getName()), gunType, new ModelDefaultMuzzleFlash(), null) instanceof ModelMuzzleFlash modelMuzzleFlash)
        {
            return modelMuzzleFlash;
        }
        return null;
    }

    @Nullable
    private static IModelBase getOrLoadModel(ModelCacheKey modelCacheKey, InfoType type, @Nullable IModelBase defaultModel)
    {
        return getOrLoadModel(modelCacheKey, type, defaultModel, type.getTexture());
    }

    /**
     * @param texture the texture this model is rendered with, used to correct models declaring a
     *                texture size that does not match it. Pass {@code null} to skip that correction.
     */
    @Nullable
    private static IModelBase getOrLoadModel(ModelCacheKey modelCacheKey, InfoType type, @Nullable IModelBase defaultModel, @Nullable ResourceLocation texture)
    {
        if (StringUtils.isBlank(modelCacheKey.modelClassName()))
        {
            if (defaultModel != null)
                modelCacheKey = new ModelCacheKey(defaultModel.getClass().getName(), modelCacheKey.typeShortName(), modelCacheKey.contentPackName());
            else
                return null;
        }

        return cache.computeIfAbsent(modelCacheKey, key -> {
            IModelBase model = loadModel(key.modelClassName(), type, defaultModel);
            ModelTextureFitter.fitToTexture(model, texture);
            return Optional.ofNullable(model);
        }).orElse(null);
    }

    /**
     * Returns only the render passes represented by this model's immutable part flags.
     * Legacy renderers previously traversed the complete model four times even when it
     * contained no glow geometry.
     */
    public static List<EnumRenderPass> getRenderPasses(IModelBase model)
    {
        return renderPassCache.computeIfAbsent(model, ModelCache::findRenderPasses);
    }

    private static List<EnumRenderPass> findRenderPasses(IModelBase model)
    {
        EnumSet<EnumRenderPass> passes = EnumSet.noneOf(EnumRenderPass.class);

        // Gun bullet-counter parts are marked as glowing only while they are drawn,
        // so their required pass cannot be discovered from the model's initial flags.
        if (model instanceof ModelGun gun && (gun.isBulletCounterActive() || gun.isAdvBulletCounterActive()))
            passes.add(EnumRenderPass.GLOW_ALPHA);

        model.forEachModelBox(modelRenderer -> {
            if (modelRenderer instanceof ModelRendererTurbo turbo)
            {
                if (turbo.glowNoDepthWrite)
                    passes.add(EnumRenderPass.GLOW_ALPHA_NO_DEPTH_WRITE);
                if (turbo.glow)
                    passes.add(EnumRenderPass.GLOW_ALPHA);
                if (turbo.glowAdditive)
                    passes.add(EnumRenderPass.GLOW_ADDITIVE);
                if (!turbo.glow && !turbo.glowAdditive && !turbo.glowNoDepthWrite)
                    passes.add(EnumRenderPass.DEFAULT);
            }
            else
            {
                passes.add(EnumRenderPass.DEFAULT);
            }
        });

        if (passes.isEmpty())
            passes.add(EnumRenderPass.DEFAULT);
        return EnumRenderPass.ORDER.stream().filter(passes::contains).toList();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static IModelBase loadModel(String modelClassName, InfoType type, @Nullable IModelBase defaultModel)
    {
        IModelBase model = null;
        if (StringUtils.isNotBlank(modelClassName))
        {
            if (modelClassName.equalsIgnoreCase(ModelBullet.class.getName()))
                model = new ModelBullet();
            else if (modelClassName.equalsIgnoreCase(ModelBomb.class.getName()))
                model = new ModelBomb();
            else if (modelClassName.equalsIgnoreCase(ModelDefaultMuzzleFlash.class.getName()))
                model = new ModelDefaultMuzzleFlash();
            else if (modelClassName.equalsIgnoreCase(ModelDefaultArmor.class.getName()) && type instanceof ArmorType armorType)
                model = new ModelDefaultArmor(armorType.getArmorItemType());
            else
            {
                ModelClassLocation modelLocation = findModelClass(type.getContentPack(), modelClassName);
                // A model class file shipped by the type's own content pack overrides a model class of the
                // same name compiled into the mod, unless that override is disabled in the client config.
                boolean preferContentPackClass = modelLocation.ownClassFile() && !ModClientConfig.get().preferBuiltInModelClasses;
                try
                {
                    model = (IModelBase) ClassLoaderUtils.loadModelClass(modelLocation.contentPack(), modelLocation.className(), preferContentPackClass)
                        .getConstructor().newInstance();
                    if (!modelLocation.contentPack().equals(type.getContentPack()))
                        FlansMod.log.debug("Loaded model class {} for {} from fallback content pack [{}].", modelLocation.className(), type, modelLocation.contentPack().getName());
                }
                catch (Exception | LinkageError e)
                {
                    FlansMod.log.error("Could not load model class {} for {}", modelClassName, type);
                    NoSuchFileException missingFile = findMissingFile(e);
                    if (missingFile != null)
                        FlansMod.log.error("File not found: {}", missingFile.getFile());
                    else
                        LogUtils.logErrorWithoutStacktrace(e);
                }
            }

        }

        if (model == null)
            model = defaultModel;

        if (model instanceof IFlanTypeModel<?> flanItemModel && flanItemModel.typeClass().isInstance(type))
            ((IFlanTypeModel<InfoType>) flanItemModel).setType(type);

        if (model != null && type.getRenderOptions().additiveBlending())
        {
            model.forEachModelBox(modelRenderer -> {
                if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo && modelRendererTurbo.glow)
                {
                    modelRendererTurbo.glowAdditive = true;
                    modelRendererTurbo.glow = false;
                }
            });
        }

        return model;
    }

    private static ModelClassLocation findModelClass(IContentProvider preferredContentPack, String modelClassName)
    {
        if (ClassLoaderUtils.hasClassFile(preferredContentPack, modelClassName))
            return new ModelClassLocation(preferredContentPack, modelClassName, true);

        if (!ModClientConfig.get().searchModelsInOtherContentPacks)
            return new ModelClassLocation(preferredContentPack, modelClassName, false);

        String legacyClassName = getLegacyClassName(modelClassName);

        return ContentManager.getContentPacks().stream()
            .filter(contentPack -> !contentPack.equals(preferredContentPack))
            .sorted(Comparator.comparing(IContentProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .map(contentPack -> findClassFile(contentPack, modelClassName, legacyClassName))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(new ModelClassLocation(preferredContentPack, modelClassName, false));
    }

    /** Legacy content packs may ship the same model below their own package instead of the common one. */
    @Nullable
    private static ModelClassLocation findClassFile(IContentProvider contentPack, String modelClassName, @Nullable String legacyClassName)
    {
        if (ClassLoaderUtils.hasClassFile(contentPack, modelClassName))
            return new ModelClassLocation(contentPack, modelClassName, false);

        if (legacyClassName != null && ClassLoaderUtils.hasClassFile(contentPack, legacyClassName))
            return new ModelClassLocation(contentPack, legacyClassName, false);

        return null;
    }

    @Nullable
    private static NoSuchFileException findMissingFile(Throwable throwable)
    {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause())
        {
            if (cause instanceof NoSuchFileException noSuchFileException)
                return noSuchFileException;
        }
        return null;
    }

    @Nullable
    private static String getLegacyClassName(String modelClassName)
    {
        String prefix = "com.flansmod.client.model.";
        if (!modelClassName.startsWith(prefix))
            return null;

        int packageEnd = modelClassName.indexOf('.', prefix.length());
        if (packageEnd < 0)
            return null;

        String packPackage = modelClassName.substring(prefix.length(), packageEnd);
        String simpleClassName = modelClassName.substring(packageEnd + 1);
        return "com.flansmod." + packPackage + ".client.model." + simpleClassName;
    }
}
