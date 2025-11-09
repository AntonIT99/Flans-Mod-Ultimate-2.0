package com.wolffsarmormod.client;

import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.common.types.InfoType;
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
            IModelBase model = InfoType.loadModel(type.getModelClassName(), type);
            cache.put(type.getModelClassName(), Optional.ofNullable(model));

            if (type instanceof GunType gunType)
            {
                IModelBase deployableModel = InfoType.loadModel(gunType.getDeployableModelClassName(), type);
                cache.put(gunType.getDeployableModelClassName(), Optional.ofNullable(deployableModel));
            }
        }
    }

    @Nullable
    public static IModelBase getOrLoadModel(String key, InfoType type)
    {
        return cache.computeIfAbsent(key, k -> Optional.ofNullable(InfoType.loadModel(k, type))).orElse(null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(InfoType type)
    {
        return getOrLoadModel(type.getModelClassName(), type);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(String typeShortname)
    {
        InfoType type = InfoType.getInfoType(typeShortname);
        if (type != null)
        {
            return getOrLoadModel(type.getModelClassName(), type);
        }
        return null;
    }

    @Nullable
    public static IModelBase getOrLoadDeployableModel(String typeShortname)
    {
        InfoType type = InfoType.getInfoType(typeShortname);
        if (type instanceof GunType gunType && gunType.isDeployable())
        {
            return getOrLoadModel(gunType.getDeployableModelClassName(), type);
        }
        return null;
    }
}
