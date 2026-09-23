package com.wolffsmod.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface IModelRenderer
{
    float getRotateAngleX();

    float getRotateAngleY();

    float getRotateAngleZ();

    float getRotationPointX();

    float getRotationPointY();

    float getRotationPointZ();

    float getOffsetX();

    float getOffsetY();

    float getOffsetZ();

    boolean isMirror();

    boolean isShowModel();

    boolean isHidden();

    String getBoxName();

    List<ModelPart.Cube> getCubeList();

    void setRotateAngleX(float angle);

    void setRotateAngleY(float angle);

    void setRotateAngleZ(float angle);

    void setRotationPointX(float point);

    void setRotationPointY(float point);

    void setRotationPointZ(float point);

    void setOffsetX(float offset);

    void setOffsetY(float offset);

    void setOffsetZ(float offset);

    void setMirror(boolean mirror);

    void setShowModel(boolean showModel);

    void setHidden(boolean hidden);

    default void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn)
    {
        setRotationPointX(rotationPointXIn);
        setRotationPointY(rotationPointYIn);
        setRotationPointZ(rotationPointZIn);
    }

    /**
     * Sets the current box's rotation points and rotation angles to another box.
     */
    void addChild(IModelRenderer renderer);

    IModelRenderer setTextureOffset(int x, int y);

    /**
     * Creates a textured box.
     */
    void addBox(float offX, float offY, float offZ, int width, int height, int depth, float scaleFactor);

    IModelRenderer addBox(String partName, float offX, float offY, float offZ, int width, int height, int depth);

    IModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth);

    IModelRenderer addBox(float offX, float offY, float offZ, int width, int height, int depth, boolean mirrored);

    void render(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale);
}
