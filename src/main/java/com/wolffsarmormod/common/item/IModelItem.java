package com.wolffsarmormod.common.item;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.IContentProvider;
import com.wolffsarmormod.client.model.IFlanModel;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.util.ClassLoaderUtils;
import com.wolffsarmormod.util.DynamicReference;
import com.wolffsarmormod.util.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface IModelItem<T extends InfoType, M extends IFlanModel<T>> extends IFlanItem<T>
{
    T getConfigType();

    @OnlyIn(Dist.CLIENT)
    void clientSideInit();

    @OnlyIn(Dist.CLIENT)
    ResourceLocation getTexture();

    @OnlyIn(Dist.CLIENT)
    void setTexture(ResourceLocation texture);

    @Nullable
    @OnlyIn(Dist.CLIENT)
    M getModel();

    @OnlyIn(Dist.CLIENT)
    void setModel(M model);

    @OnlyIn(Dist.CLIENT)
    boolean useCustomItemRendering();

    @OnlyIn(Dist.CLIENT)
    default void loadModelAndTexture(@Nullable M defaultModel)
    {
        loadModel(defaultModel);
        loadTexture();
    }

    @OnlyIn(Dist.CLIENT)
    default void loadModel(@Nullable M defaultModel)
    {
        T configType = getConfigType();
        String className = configType.getModelClassName();
        if (!className.isBlank())
        {
            IContentProvider contentPack = configType.getContentPack();
            DynamicReference actualClassName = configType.getActualModelClassName();
            if (actualClassName != null)
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    M model = (M) ClassLoaderUtils.loadAndModifyClass(contentPack, className, actualClassName.get()).getConstructor().newInstance();
                    model.setType(configType);
                    setModel(model);
                }
                catch (Exception | NoClassDefFoundError | ClassFormatError e)
                {
                    ArmorMod.log.error("Could not load model class {} for {}", className, configType);
                    LogUtils.logWithoutStacktrace(e);
                }
            }
        }
        if (getModel() == null && defaultModel != null)
        {
            defaultModel.setType(configType);
            setModel(defaultModel);
        }
    }

    @OnlyIn(Dist.CLIENT)
    default void loadTexture()
    {
        T configType = getConfigType();
        ResourceLocation texture = configType.getTexture();
        setTexture(texture);
        if (getModel() != null)
            getModel().setTexture(texture);
    }
}
