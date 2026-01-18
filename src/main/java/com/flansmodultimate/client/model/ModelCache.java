package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfigs;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.LogUtils;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelCache
{
    public record ModelCacheKey(String modelClassName, @Nullable String typeShortName)
    {
        public ModelCacheKey
        {
            typeShortName = StringUtils.isBlank(typeShortName) ? null : typeShortName;
        }
    }

    private static final Map<ModelCacheKey, Optional<IModelBase>> cache = new ConcurrentHashMap<>();

    public static void reload()
    {
        cache.clear();
        if (BooleanUtils.isTrue(ModClientConfigs.loadAllModelsInCache.get()))
            loadAll();
    }

    public static void loadAll()
    {
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            if (StringUtils.isNotBlank(type.getModelClassName()))
            {
                IModelBase model = loadModel(type.getModelClassName(), type, type.getDefaultModel());
                cache.put(new ModelCacheKey(type.getModelClassName(), type.getShortName()), Optional.ofNullable(model));
            }

            if (type instanceof GunType gunType)
            {
                if (StringUtils.isNotBlank(gunType.getDeployableModelClassName()))
                {
                    IModelBase deployableModel = loadModel(gunType.getDeployableModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName()), Optional.ofNullable(deployableModel));
                }
                if (StringUtils.isNotBlank(gunType.getCasingModelClassName()))
                {
                    IModelBase casingModel = loadModel(gunType.getCasingModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getCasingModelClassName(), null), Optional.ofNullable(casingModel));
                }
                if (StringUtils.isNotBlank(gunType.getFlashModelClassName()))
                {
                    IModelBase flashModel = loadModel(gunType.getFlashModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getFlashModelClassName(), null), Optional.ofNullable(flashModel));
                }
                if (StringUtils.isNotBlank(gunType.getMuzzleFlashModelClassName()))
                {
                    IModelBase muzzleFlashModel = loadModel(gunType.getMuzzleFlashModelClassName(), type, new ModelDefaultMuzzleFlash());
                    cache.put(new ModelCacheKey(gunType.getMuzzleFlashModelClassName(), null), Optional.ofNullable(muzzleFlashModel));
                }
            }
        }
    }

    @Nullable
    public static IModelBase getOrLoadModel(ModelCacheKey modelCacheKey, InfoType type, @Nullable IModelBase defaultModel)
    {
        if (StringUtils.isBlank(modelCacheKey.modelClassName()) && defaultModel != null)
            modelCacheKey = new ModelCacheKey(defaultModel.getClass().getName(), modelCacheKey.typeShortName());

        return cache.computeIfAbsent(modelCacheKey, key -> Optional.ofNullable(loadModel(key.modelClassName(), type, defaultModel))).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        if (StringUtils.isNotBlank(type.getModelClassName()))
        {
            return cache.computeIfAbsent(new ModelCacheKey(type.getModelClassName(), type.getShortName()), key -> Optional.ofNullable(loadModel(key.modelClassName(), type, type.getDefaultModel()))).orElse(null);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadDeployableGunModel(GunType gunType)
    {
        if (gunType.isDeployable())
        {
            return getOrLoadModel(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName()), gunType, null);
        }
        return null;
    }

    @Nullable
    public static ModelCasing getOrLoadCasingModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getCasingModelClassName(), null), gunType, null) instanceof ModelCasing modelCasing)
        {
            return modelCasing;
        }
        return null;
    }

    @Nullable
    public static ModelFlash getOrLoadFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getFlashModelClassName(), null), gunType, null) instanceof ModelFlash modelFlash)
        {
            return modelFlash;
        }
        return null;
    }

    @Nullable
    public static ModelMuzzleFlash getOrLoadMuzzleFlashModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getMuzzleFlashModelClassName(), null), gunType, new ModelDefaultMuzzleFlash()) instanceof ModelMuzzleFlash modelMuzzleFlash)
        {
            return modelMuzzleFlash;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @OnlyIn(Dist.CLIENT)
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
                DynamicReference actualClassName = ContentManager.getModelReferences().get(type.getContentPack()).get(modelClassName);
                if (actualClassName != null)
                {
                    try
                    {
                        model = (IModelBase) ClassLoaderUtils.loadAndModifyClass(type.getContentPack(), modelClassName, actualClassName.get()).getConstructor().newInstance();
                    }
                    catch (Exception | NoClassDefFoundError | ClassFormatError e)
                    {
                        FlansMod.log.error("Could not load model class {} for {}", modelClassName, type);
                        if (e instanceof IOException ioException && ioException.getCause() instanceof NoSuchFileException noSuchFileException)
                            FlansMod.log.error("File not found: {}", noSuchFileException.getFile());
                        else
                            LogUtils.logWithoutStacktrace(e);
                    }
                }
            }

        }

        if (model == null)
            model = defaultModel;

        if (model instanceof IFlanTypeModel<?> flanItemModel && flanItemModel.typeClass().isInstance(type))
            ((IFlanTypeModel<InfoType>) flanItemModel).setType(type);

        if (model != null && type.isAdditiveBlending())
        {
            for (ModelRenderer modelRenderer : model.getBoxList())
            {
                if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo && modelRendererTurbo.glow)
                {
                    modelRendererTurbo.glowAdditive = true;
                    modelRendererTurbo.glow = false;
                }
            }
        }

        return model;
    }
}
