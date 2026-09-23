package com.flansmodultimate.client.model;

import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.IModelRenderer;
import com.wolffsmod.api.client.model.TextureOffset;
import lombok.Getter;
import lombok.Setter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "UnusedReturnValue", "BooleanMethodIsAlwaysInverted", "java:S1104"})
public class ModelRenderer implements IModelRenderer
{
    /** The size of the texture file's width in pixels. */
    public float textureWidth;
    /** The size of the texture file's height in pixels. */
    public float textureHeight;
    @Getter @Setter
    public float rotationPointX;
    @Getter @Setter
    public float rotationPointY;
    @Getter @Setter
    public float rotationPointZ;
    @Getter @Setter
    public float rotateAngleX;
    @Getter @Setter
    public float rotateAngleY;
    @Getter @Setter
    public float rotateAngleZ;
    @Getter @Setter
    public float offsetX;
    @Getter @Setter
    public float offsetY;
    @Getter @Setter
    public float offsetZ;
    @Getter @Setter
    public boolean mirror;
    @Getter @Setter
    public boolean showModel;
    /** Hides the model. */
    @Getter @Setter
    public boolean isHidden;
    @Getter
    public final String boxName;
    @Getter
    public final List<ModelPart.Cube> cubeList = new ArrayList<>();
    public final List<ModelRenderer> childModels = new ArrayList<>();

    protected final IModelBase baseModel;

    /** The X offset into the texture used for displaying this model */
    private int textureOffsetX;
    /** The Y offset into the texture used for displaying this model */
    private int textureOffsetY;

    public ModelRenderer(IModelBase model, String boxNameIn)
    {
        textureWidth = 64.0F;
        textureHeight = 32.0F;
        showModel = true;
        baseModel = model;
        model.addModelBox(this);
        boxName = boxNameIn;
        setTextureSize(model.getTextureWidth(), model.getTextureHeight());
    }

    public ModelRenderer(IModelBase model)
    {
        this(model, "");
    }

    public ModelRenderer(IModelBase model, int texOffX, int texOffY)
    {
        this(model);
        setTextureOffset(texOffX, texOffY);
    }

    public ModelRenderer(ModelBase model)
    {
        this((IModelBase)model);
    }

    public ModelRenderer(ModelBase model, String boxNameIn)
    {
        this((IModelBase)model, boxNameIn);
    }

    public ModelRenderer(ModelBase model, int texOffX, int texOffY)
    {
        this((IModelBase)model, texOffX, texOffY);
    }

    public void addChild(ModelRenderer renderer)
    {
        childModels.add(renderer);
    }

    @Override
    public ModelRenderer setTextureOffset(int x, int y)
    {
        textureOffsetX = x;
        textureOffsetY = y;
        return this;
    }

    @Override
    public ModelRenderer addBox(String partName, float offX, float offY, float offZ, int width, int height, int depth)
    {
        partName = boxName + "." + partName;
        TextureOffset textureoffset = baseModel.getTextureOffset(partName);
        setTextureOffset(textureoffset.textureOffsetX(), textureoffset.textureOffsetY());
        addBox(offX, offY, offZ, width, height, depth, 0.0F, mirror);
        return this;
    }

    @Override
    public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth)
    {
        addBox(offX, offY, offZ, width, height, depth, 0.0F, mirror);
        return this;
    }

    @Override
    public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth, boolean mirrored)
    {
        addBox(offX, offY, offZ, width, height, depth, 0.0F, mirrored);
        return this;
    }

    /**
     * Creates a textured box.
     */
    public void addBox(float offX, float offY, float offZ, int width, int height, int depth, float scaleFactor)
    {
        addBox(offX, offY, offZ, width, height, depth, scaleFactor, mirror);
    }

    protected void addBox(float offX, float offY, float offZ, int width, int height, int depth, float scaleFactor, boolean mirrored)
    {
        cubeList.add(new ModelPart.Cube(textureOffsetX, textureOffsetY, offX, offY, offZ, width, height, depth, scaleFactor, scaleFactor, scaleFactor, mirrored, 1.0F, 1.0F, Set.of(Direction.values())));
    }

    @Override
    public void addChild(IModelRenderer renderer)
    {
        if (!(renderer instanceof ModelRenderer modelRenderer))
            throw new IllegalArgumentException("Unsupported child renderer implementation: " + renderer);
        childModels.add(modelRenderer);
    }

    public void render(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale)
    {
        if (!isVisible() || (cubeList.isEmpty() && childModels.isEmpty()))
            return;

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);
        translateAndRotate(poseStack, scale);
        compile(poseStack.last(), vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);

        for (ModelRenderer childModel : childModels)
        {
            childModel.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale);
        }

        poseStack.translate(-offsetX, -offsetY, -offsetZ);
        poseStack.popPose();
    }

    public void translateAndRotate(PoseStack poseStack, float scale)
    {
        poseStack.translate(rotationPointX * 0.0625F, rotationPointY * 0.0625F, rotationPointZ * 0.0625F);

        if (rotateAngleX != 0.0F || rotateAngleY != 0.0F || rotateAngleZ != 0.0F)
        {
            poseStack.mulPose((new Quaternionf()).rotationZYX(rotateAngleZ, rotateAngleY, rotateAngleX));
        }

        if (scale != 1.0F)
        {
            poseStack.scale(scale, scale, scale);
        }
    }

    protected void compile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        for (ModelPart.Cube cube : cubeList)
        {
            int packedColor = ((int)(alpha * 255F) & 0xFF) << 24
                | ((int)(red * 255F) & 0xFF) << 16
                | ((int)(green * 255F) & 0xFF) << 8
                | ((int)(blue * 255F) & 0xFF);
            cube.compile(pose, vertexConsumer, packedLight, packedOverlay, packedColor);
        }
    }

    public ModelRenderer setTextureSize(int textureWidthIn, int textureHeightIn)
    {
        textureWidth = textureWidthIn;
        textureHeight = textureHeightIn;
        return this;
    }
    
    public boolean isVisible()
    {
        return !isHidden && showModel;
    }
}
