package com.flansmodultimate.client.model;

import com.flansmod.client.model.*;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.platform.render.ShaderPlatform;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.LogUtils;
import com.flansmodultimate.util.ModelClassResolver;
import com.flansmodultimate.util.ModelClassResolver.ModelClassLocation;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.NoSuchFileException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelCache
{
    /**
     * @param contentPackName
     *            the content pack the model is loaded for, because the same model class name may
     *            resolve to a different class file in every content pack
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
    private static final Map<IModelBase, List<EnumRenderPass>> shadowPassCache = new ConcurrentHashMap<>();

    public static void reload()
    {
        GpuModelCache.clear();
        DriveableImpostorCache.clear();
        com.flansmodultimate.client.render.WorldModelPreview.clear();
        com.flansmodultimate.client.render.TypeModelPreview.clearBounds();
        ModelTextureFitter.clear();
        cache.clear();
        renderPassCache.clear();
        shadowPassCache.clear();
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
     * <p>
     * For callers that are not on the render thread. Loading a model fits it to
     * its texture, which reads the atlas, so off-thread callers must take what is
     * already there and do without when there is nothing.
     * </p>
     */
    @Nullable
    public static IModelBase getLoadedTypeModel(InfoType type)
    {
        ModelCacheKey key = new ModelCacheKey(type.getModelClassName(), type.getShortName(), type.getContentPack().getName());
        return cache.getOrDefault(key, Optional.empty()).orElse(null);
    }

    /**
     * The model of a mixed belt's alternate rounds, from the ammunition's {@code AlternateModel},
     * or null when it names none.
     */
    @Nullable
    public static IModelBase getOrLoadAlternateBulletModel(BulletType type)
    {
        if (StringUtils.isBlank(type.getAlternateModelClassName()))
            return null;
        ResourceLocation texture = type.getAlternateTexture() != null ? type.getAlternateTexture() : type.getTexture();
        return getOrLoadModel(new ModelCacheKey(type.getAlternateModelClassName(), type.getShortName(), type.getContentPack().getName()), type, null, texture);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(ArmorType type)
    {
        return getOrLoadModel(new ModelCacheKey(type.getModelClassName(), type.getShortName(), type.getContentPack().getName()), type,
            new ModelDefaultArmor(type.getArmorItemType()), type.getTexture());
    }

    @Nullable
    public static ModelMG getOrLoadDeployableGunModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName(), gunType.getContentPack().getName()), gunType, null,
            gunType.getDeployableTexture()) instanceof ModelMG modelMG)
        {
            return modelMG;
        }
        return null;
    }

    @Nullable
    public static ModelCasing getOrLoadCasingModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getCasingModelClassName(), null, gunType.getContentPack().getName()), gunType, null,
            gunType.getCasingTexture()) instanceof ModelCasing modelCasing)
        {
            return modelCasing;
        }
        return null;
    }

    @Nullable
    public static ModelFlash getOrLoadFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getFlashModelClassName(), null, gunType.getContentPack().getName()), gunType, null,
            gunType.getFlashTexture()) instanceof ModelFlash modelFlash)
        {
            return modelFlash;
        }
        return null;
    }

    @Nullable
    public static ModelMuzzleFlash getOrLoadMuzzleFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getMuzzleFlashModelClassName(), null, gunType.getContentPack().getName()), gunType, new ModelDefaultMuzzleFlash(),
            null) instanceof ModelMuzzleFlash modelMuzzleFlash)
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
     * @param texture
     *            the texture this model is rendered with, used to correct models declaring a
     *            texture size that does not match it. Pass {@code null} to skip that correction.
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

        return cache.computeIfAbsent(modelCacheKey, key ->
        {
            IModelBase model = loadModel(key.modelClassName(), type, defaultModel);
            ModelTextureFitter.fitToTexture(model, texture);
            return Optional.ofNullable(model);
        }).orElse(null);
    }

    /**
     * Returns only the render passes represented by this model's immutable part flags.
     * Legacy renderers previously traversed the complete model four times even when it
     * contained no glow geometry. A shader pack's shadow pass gets only the passes that
     * {@linkplain EnumRenderPass#castsShadow() cast a shadow}.
     */
    public static List<EnumRenderPass> getRenderPasses(IModelBase model)
    {
        List<EnumRenderPass> passes = renderPassCache.computeIfAbsent(model, ModelCache::findRenderPasses);
        if (!ShaderPlatform.isRenderingShadowPass())
            return passes;
        return shadowPassCache.computeIfAbsent(model, ModelCache::findShadowRenderPasses);
    }

    private static List<EnumRenderPass> findShadowRenderPasses(IModelBase model)
    {
        return renderPassCache.computeIfAbsent(model, ModelCache::findRenderPasses).stream().filter(EnumRenderPass::castsShadow).toList();
    }

    private static List<EnumRenderPass> findRenderPasses(IModelBase model)
    {
        EnumSet<EnumRenderPass> passes = EnumSet.noneOf(EnumRenderPass.class);

        // Gun bullet-counter parts are marked as glowing only while they are drawn,
        // so their required pass cannot be discovered from the model's initial flags.
        if (model instanceof ModelGun gun && (gun.isBulletCounterActive() || gun.isAdvBulletCounterActive()))
            passes.add(EnumRenderPass.GLOW_ALPHA);

        model.forEachModelBox(modelRenderer ->
        {
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
            else if (modelClassName.equalsIgnoreCase(ModelDefaultFlash.class.getName()))
                model = new ModelDefaultFlash();
            else if (modelClassName.equalsIgnoreCase(ModelDefaultArmor.class.getName()) && type instanceof ArmorType armorType)
                model = new ModelDefaultArmor(armorType.getArmorItemType());
            else
            {
                ModelClassLocation modelLocation = ModelClassResolver.find(type.getContentPack(), modelClassName, ModClientConfig.get().searchModelsInOtherContentPacks);
                try
                {
                    // A model class file shipped by the type's own content pack overrides a model class of the
                    // same name compiled into the mod, unless that override is disabled in the client config.
                    model = (IModelBase) ModelClassResolver.instantiate(modelLocation, ModClientConfig.get().preferBuiltInModelClasses);
                    if (!modelLocation.contentPack().equals(type.getContentPack()))
                        FlansLog.log.debug("Loaded model class {} for {} from fallback content pack [{}].", modelLocation.className(), type, modelLocation.contentPack().getName());
                }
                catch (Exception | LinkageError e)
                {
                    FlansLog.log.error("Could not load model class {} for {}", modelClassName, type);
                    NoSuchFileException missingFile = findMissingFile(e);
                    if (missingFile != null)
                        FlansLog.log.error("File not found: {}", missingFile.getFile());
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
            model.forEachModelBox(modelRenderer ->
            {
                if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo && modelRendererTurbo.glow)
                {
                    modelRendererTurbo.glowAdditive = true;
                    modelRendererTurbo.glow = false;
                }
            });
        }

        return model;
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
}
