package com.flansmodultimate.client.model;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.model.ModelDefaultMuzzleFlash;
import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelMG;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.LogUtils;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    private record ModelClassLocation(IContentProvider contentPack, String fileClassName, String actualClassName) {}

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
        return getOrLoadModel(new ModelCacheKey(type.getModelClassName(), type.getShortName()), type, null);
    }

    @Nullable
    public static IModelBase getOrLoadTypeModel(ArmorType type)
    {
        return getOrLoadModel(new ModelCacheKey(type.getModelClassName(), type.getShortName()), type, new ModelDefaultArmor(type.getArmorItemType()));
    }

    @Nullable
    public static ModelMG getOrLoadDeployableGunModel(GunType gunType)
    {
        if (getOrLoadModel(new ModelCacheKey(gunType.getDeployableModelClassName(), gunType.getShortName()), gunType, null) instanceof ModelMG modelMG)
        {
            return modelMG;
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

    @Nullable
    public static IModelBase getOrLoadModel(ModelCacheKey modelCacheKey, InfoType type, @Nullable IModelBase defaultModel)
    {
        if (StringUtils.isBlank(modelCacheKey.modelClassName()))
        {
            if (defaultModel != null)
                modelCacheKey = new ModelCacheKey(defaultModel.getClass().getName(), modelCacheKey.typeShortName());
            else
                return null;
        }

        return cache.computeIfAbsent(modelCacheKey, key -> Optional.ofNullable(loadModel(key.modelClassName(), type, defaultModel))).orElse(null);
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
                ModelClassLocation modelLocation = findModelClass(type.getContentPack(), modelClassName);
                if (modelLocation != null)
                {
                    try
                    {
                        model = (IModelBase) ClassLoaderUtils.loadAndModifyClass(modelLocation.contentPack(), modelLocation.fileClassName(), modelLocation.actualClassName()).getConstructor().newInstance();
                        if (!modelLocation.contentPack().equals(type.getContentPack()))
                            FlansMod.log.debug("Loaded model class {} for {} from fallback content pack [{}].", modelClassName, type, modelLocation.contentPack().getName());
                    }
                    catch (Exception | NoClassDefFoundError | ClassFormatError e)
                    {
                        FlansMod.log.error("Could not load model class {} for {}", modelClassName, type);
                        if (e instanceof IOException ioException && ioException.getCause() instanceof NoSuchFileException noSuchFileException)
                            FlansMod.log.error("File not found: {}", noSuchFileException.getFile());
                        else
                            LogUtils.logErrorWithoutStacktrace(e);
                    }
                }
            }

        }

        if (model == null)
            model = defaultModel;

        if (model instanceof IFlanTypeModel<?> flanItemModel && flanItemModel.typeClass().isInstance(type))
            ((IFlanTypeModel<InfoType>) flanItemModel).setType(type);

        if (model != null && type.getRenderOptions().additiveBlending())
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

    @Nullable
    private static ModelClassLocation findModelClass(IContentProvider preferredContentPack, String modelClassName)
    {
        Map<String, DynamicReference> preferredReferences = ContentManager.getModelReferences().get(preferredContentPack);
        DynamicReference preferredActualClassName = preferredReferences == null ? null : preferredReferences.get(modelClassName);
        if (preferredActualClassName == null)
            return null;

        if (ClassLoaderUtils.hasClassFile(preferredContentPack, modelClassName))
            return new ModelClassLocation(preferredContentPack, modelClassName, preferredActualClassName.get());

        if (!ModClientConfig.get().searchModelsInOtherContentPacks)
            return new ModelClassLocation(preferredContentPack, modelClassName, preferredActualClassName.get());

        String legacyClassName = getLegacyClassName(modelClassName);

        return ContentManager.getModelReferences().entrySet().stream()
            .filter(entry -> !entry.getKey().equals(preferredContentPack))
            .filter(entry -> ClassLoaderUtils.hasClassFile(entry.getKey(), modelClassName)
                || legacyClassName != null && ClassLoaderUtils.hasClassFile(entry.getKey(), legacyClassName))
            .sorted(Map.Entry.comparingByKey((left, right) -> left.getName().compareToIgnoreCase(right.getName())))
            .map(entry -> new ModelClassLocation(entry.getKey(),
                ClassLoaderUtils.hasClassFile(entry.getKey(), modelClassName) ? modelClassName : legacyClassName,
                preferredActualClassName.get()))
            .findFirst()
            .orElse(new ModelClassLocation(preferredContentPack, modelClassName, preferredActualClassName.get()));
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
