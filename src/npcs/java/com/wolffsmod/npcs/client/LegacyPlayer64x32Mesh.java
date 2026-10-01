package com.wolffsmod.npcs.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The original 64x32 player UV layout, including mirrored left limbs. */
public final class LegacyPlayer64x32Mesh
{
    private LegacyPlayer64x32Mesh() {}

    public static ModelPart bake()
    {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition root = mesh.getRoot();

        // PlayerModel.createMesh uses the lower half of a 64x64 skin for the left limbs.
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
            .addBox(-1F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(5F, 2F, 0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
            .addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(1.9F, 12F, 0F));

        // These extra player layers also live in the lower half of modern skins.
        for (String part : new String[] {"left_sleeve", "right_sleeve", "left_pants", "right_pants", "jacket"})
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
