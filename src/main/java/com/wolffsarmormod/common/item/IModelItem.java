package com.wolffsarmormod.common.item;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.IContentProvider;
import com.wolffsarmormod.client.model.IFlanModel;
import com.wolffsarmormod.client.render.CustomBewlr;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsarmormod.util.ClassLoaderUtils;
import com.wolffsarmormod.util.DynamicReference;
import com.wolffsarmormod.util.LogUtils;
import com.wolffsmod.api.client.model.IModelBase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.function.Consumer;

public interface IModelItem<T extends InfoType, M extends IModelBase> extends IFlanItem<T>
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
    default boolean useCustomItemRendering()
    {
        return this instanceof ICustomRendererItem;
    }

    @OnlyIn(Dist.CLIENT)
    default void loadModelAndTexture(@Nullable M defaultModel)
    {
        loadModel(defaultModel);
        loadTexture();
    }

    @SuppressWarnings("unchecked")
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
                    M model = (M) ClassLoaderUtils.loadAndModifyClass(contentPack, className, actualClassName.get()).getConstructor().newInstance();
                    if (model instanceof IFlanModel<?> flanItemModel)
                    {
                        ((IFlanModel<T>) flanItemModel).setType(configType);
                    }
                    setModel(model);
                }
                catch (Exception | NoClassDefFoundError | ClassFormatError e)
                {
                    ArmorMod.log.error("Could not load model class {} for {}", className, configType);
                    if (e instanceof IOException ioException && ioException.getCause() instanceof NoSuchFileException noSuchFileException)
                        ArmorMod.log.error("File not found: {}", noSuchFileException.getFile());
                    else
                        LogUtils.logWithoutStacktrace(e);
                }
            }
        }
        if (getModel() == null && defaultModel != null)
        {
            if (defaultModel instanceof IFlanModel<?> flanItemModel)
            {
                ((IFlanModel<T>) flanItemModel).setType(configType);
            }
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

    @OnlyIn(Dist.CLIENT)
    default void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        if (useCustomItemRendering())
        {
            consumer.accept(new IClientItemExtensions()
            {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer()
                {
                    Minecraft mc = Minecraft.getInstance();
                    return new CustomBewlr(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
            });
        }
    }
}
