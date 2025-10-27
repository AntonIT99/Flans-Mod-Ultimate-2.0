package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModelRenderer
{
    public static final float PI = (float) Math.PI;
    
    /** The size of the texture file's width in pixels. */
    public float textureWidth;

    /** The size of the texture file's height in pixels. */
    public float textureHeight;

    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public boolean mirror;
    public boolean showModel;

    /** Hides the model. */
    public boolean isHidden;

    public final List<ModelPart.Cube> cubeList = new ArrayList<>();
    public final List<ModelRenderer> childModels = new ArrayList<>();
    public final String boxName;

    /** The X offset into the texture used for displaying this model */
    private int textureOffsetX;

    /** The Y offset into the texture used for displaying this model */
    private int textureOffsetY;

    protected final IModelBase baseModel;

    public ModelRenderer(IModelBase model, String boxNameIn)
    {
        textureWidth = 64.0F;
        textureHeight = 32.0F;
        showModel = true;
        baseModel = model;
        model.getBoxList().add(this);
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

    /**
     * Sets the current box's rotation points and rotation angles to another box.
     */
    public void addChild(ModelRenderer renderer)
    {
        childModels.add(renderer);
    }

    public ModelRenderer setTextureOffset(int x, int y)
    {
        textureOffsetX = x;
        textureOffsetY = y;
        return this;
    }

    public ModelRenderer addBox(String partName, float offX, float offY, float offZ, int width, int height, int depth)
    {
        partName = boxName + "." + partName;
        TextureOffset textureoffset = baseModel.getTextureOffset(partName);
        setTextureOffset(textureoffset.textureOffsetX(), textureoffset.textureOffsetY());
        addBox(offX, offY, offZ, width, height, depth, 0.0F, mirror);
        return this;
    }

    public ModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth)
    {
        addBox(offX, offY, offZ, width, height, depth, 0.0F, mirror);
        return this;
    }

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

    public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn)
    {
        rotationPointX = rotationPointXIn;
        rotationPointY = rotationPointYIn;
        rotationPointZ = rotationPointZIn;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        if (!isVisible() || (cubeList.isEmpty() && childModels.isEmpty())) return;

        pPoseStack.pushPose();
        pPoseStack.translate(offsetX, offsetY, offsetZ);
        translateAndRotate(pPoseStack, scale);
        compile(pPoseStack.last(), pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);

        for (ModelRenderer childModel : childModels)
        {
            childModel.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
        }

        pPoseStack.translate(-offsetX, -offsetY, -offsetZ);
        pPoseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public void render(float scale) {}

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

    protected void compile(PoseStack.Pose pPose, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {
        for (ModelPart.Cube cube : cubeList)
        {
            cube.compile(pPose, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
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
