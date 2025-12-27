package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfigs;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

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
                IModelBase model = InfoType.loadModel(type.getModelClassName(), type, type.getDefaultModel());
                cache.put(new ModelCacheKey(type.getModelClassName(), type.getShortName()), Optional.ofNullable(model));
            }

            if (type instanceof GunType gunType)
            {
                if (StringUtils.isNotBlank(gunType.getDeployableModelClassName()))
                {
                    IModelBase deployableModel = InfoType.loadModel(gunType.getDeployableModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName()), Optional.ofNullable(deployableModel));
                }
                if (StringUtils.isNotBlank(gunType.getCasingModelClassName()))
                {
                    IModelBase casingModel = InfoType.loadModel(gunType.getCasingModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getCasingModelClassName(), null), Optional.ofNullable(casingModel));
                }
                if (StringUtils.isNotBlank(gunType.getFlashModelClassName()))
                {
                    IModelBase flashModel = InfoType.loadModel(gunType.getFlashModelClassName(), type, null);
                    cache.put(new ModelCacheKey(gunType.getFlashModelClassName(), null), Optional.ofNullable(flashModel));
                }
                if (StringUtils.isNotBlank(gunType.getMuzzleFlashModelClassName()))
                {
                    IModelBase muzzleFlashModel = InfoType.loadModel(gunType.getMuzzleFlashModelClassName(), type, new ModelDefaultMuzzleFlash());
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

        return cache.computeIfAbsent(modelCacheKey, key -> Optional.ofNullable(InfoType.loadModel(key.modelClassName(), type, defaultModel))).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        if (StringUtils.isNotBlank(type.getModelClassName()))
        {
            return cache.computeIfAbsent(new ModelCacheKey(type.getModelClassName(), type.getShortName()), key -> Optional.ofNullable(InfoType.loadModel(key.modelClassName(), type, type.getDefaultModel()))).orElse(null);
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
}
