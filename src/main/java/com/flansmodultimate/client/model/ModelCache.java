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
    private static final Map<String, Optional<IModelBase>> cache = new ConcurrentHashMap<>();

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
                cache.put(type.getModelClassName(), Optional.ofNullable(model));
            }

            if (type instanceof GunType gunType)
            {
                if (StringUtils.isNotBlank(gunType.getDeployableModelClassName()))
                {
                    IModelBase deployableModel = InfoType.loadModel(gunType.getDeployableModelClassName(), type, null);
                    cache.put(gunType.getDeployableModelClassName(), Optional.ofNullable(deployableModel));
                }
                if (StringUtils.isNotBlank(gunType.getCasingModelClassName()))
                {
                    IModelBase casingModel = InfoType.loadModel(gunType.getCasingModelClassName(), type, null);
                    cache.put(gunType.getCasingModelClassName(), Optional.ofNullable(casingModel));
                }
                if (StringUtils.isNotBlank(gunType.getFlashModelClassName()))
                {
                    IModelBase flashModel = InfoType.loadModel(gunType.getFlashModelClassName(), type, null);
                    cache.put(gunType.getFlashModelClassName(), Optional.ofNullable(flashModel));
                }
                if (StringUtils.isNotBlank(gunType.getMuzzleFlashModelClassName()))
                {
                    IModelBase muzzleFlashModel = InfoType.loadModel(gunType.getMuzzleFlashModelClassName(), type, new ModelDefaultMuzzleFlash());
                    cache.put(gunType.getMuzzleFlashModelClassName(), Optional.ofNullable(muzzleFlashModel));
                }
            }
        }
    }

    @Nullable
    public static IModelBase getOrLoadModel(String className, InfoType type, @Nullable IModelBase defaultModel)
    {
        if (StringUtils.isNotBlank(className))
        {
            return cache.computeIfAbsent(className, key -> Optional.ofNullable(InfoType.loadModel(key, type, defaultModel))).orElse(null);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        if (StringUtils.isNotBlank(type.getModelClassName()))
        {
            return cache.computeIfAbsent(type.getModelClassName(), key -> Optional.ofNullable(InfoType.loadModel(key, type, type.getDefaultModel()))).orElse(null);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadDeployableGunModel(GunType gunType)
    {
        if (gunType.isDeployable())
        {
            return getOrLoadModel(gunType.getDeployableModelClassName(), gunType, null);
        }
        return null;
    }

    @Nullable
    public static ModelCasing getOrLoadCasingModel(GunType gunType)
    {
        if (getOrLoadModel(gunType.getCasingModelClassName(), gunType, null) instanceof ModelCasing modelCasing)
        {
            return modelCasing;
        }
        return null;
    }

    @Nullable
    public static ModelFlash getOrLoadFlashModel(GunType gunType)
    {
        if (getOrLoadModel(gunType.getFlashModelClassName(), gunType, null) instanceof ModelFlash modelFlash)
        {
            return modelFlash;
        }
        return null;
    }

    @Nullable
    public static ModelMuzzleFlash getOrLoadMuzzleFlashModel(GunType gunType)
    {
        if (getOrLoadModel(gunType.getFlashModelClassName(), gunType, new ModelDefaultMuzzleFlash()) instanceof ModelMuzzleFlash modelMuzzleFlash)
        {
            return modelMuzzleFlash;
        }
        return null;
    }
}
