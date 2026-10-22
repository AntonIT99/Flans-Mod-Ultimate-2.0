package com.flansmodultimate.apocalyse.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;

/** Shared legacy biped geometry for the survivor and abandoned self's 64 by 32 skin. */
public class SurvivorModel<T extends LivingEntity> extends HumanoidModel<T>
{
    public SurvivorModel()
    {
        // Vanilla zombie/player layers use 64 by 64 UVs; this skin uses mirrored legacy limbs.
        super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32).bakeRoot());
    }
}
