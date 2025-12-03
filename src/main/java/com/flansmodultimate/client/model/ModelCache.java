package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelCache
{
    //TODO: make this configurable
    public static boolean loadAllModelsInCache = false;

    private static final Map<String, Optional<IModelBase>> cache = new ConcurrentHashMap<>();

    public static void reload()
    {
        cache.clear();
        if (loadAllModelsInCache)
            loadAll();
    }

    public static void loadAll()
    {
        for (InfoType type : InfoType.getInfoTypes().values())
        {
            IModelBase model = InfoType.loadModel(type.getModelClassName(), type, type.getDefaultModel());
            cache.put(type.getModelClassName(), Optional.ofNullable(model));

            if (type instanceof GunType gunType)
            {
                IModelBase deployableModel = InfoType.loadModel(gunType.getDeployableModelClassName(), type, null);
                IModelBase casingModel = InfoType.loadModel(gunType.getDeployableModelClassName(), type, null);
                IModelBase flashModel = InfoType.loadModel(gunType.getCasingModelClassName(), type, null);
                IModelBase muzzleFlashModel = InfoType.loadModel(gunType.getMuzzleFlashModelClassName(), type, new ModelDefaultMuzzleFlash());
                cache.put(gunType.getDeployableModelClassName(), Optional.ofNullable(deployableModel));
                cache.put(gunType.getCasingModelClassName(), Optional.ofNullable(casingModel));
                cache.put(gunType.getFlashModelClassName(), Optional.ofNullable(flashModel));
                cache.put(gunType.getMuzzleFlashModelClassName(), Optional.ofNullable(muzzleFlashModel));
            }
        }
    }

    @Nullable
    public static IModelBase getOrLoadModel(String className, InfoType type, @Nullable IModelBase defaultModel)
    {
        return cache.computeIfAbsent(className, key -> Optional.ofNullable(InfoType.loadModel(key, type, defaultModel))).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        return cache.computeIfAbsent(type.getModelClassName(), key -> Optional.ofNullable(InfoType.loadModel(key, type, type.getDefaultModel()))).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(String shortname)
    {
        InfoType type = InfoType.getInfoType(shortname);
        if (type != null)
        {
            return getOrLoadTypeModel(type);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadDeployableGunModel(String shortname)
    {
        if (InfoType.getInfoType(shortname) instanceof GunType gunType && gunType.isDeployable())
        {
            return getOrLoadModel(gunType.getDeployableModelClassName(), gunType, null);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadCasingModel(GunType gunType)
    {
        return getOrLoadModel(gunType.getCasingModelClassName(), gunType, null);
    }

    @Nullable
    public static IModelBase getOrLoadFlashModel(GunType gunType)
    {
        return getOrLoadModel(gunType.getFlashModelClassName(), gunType, null);
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
