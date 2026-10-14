package com.wolffsmod.npcs.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ModelPartConfig;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Makes Flan armour follow the size and offset an NPC gives each of its body parts. Custom NPCs scales the parts of
 * its own models and of the vanilla armour layer only, so armour drawn by Flan's Mod kept the default proportions on
 * an NPC with, say, a larger head or shorter legs. Poses already carry over with the part rotations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcArmorParts
{
    /** Model units per block: part offsets are stored in sixteenths. */
    private static final float UNITS = 16F;

    public static void apply(LivingEntity wearer, HumanoidModel<?> armor)
    {
        if (!(wearer instanceof EntityCustomNpc npc) || npc.modelData == null)
            return;
        apply(armor.head, npc.modelData.getPartConfig(EnumParts.HEAD));
        apply(armor.hat, npc.modelData.getPartConfig(EnumParts.HEAD));
        apply(armor.body, npc.modelData.getPartConfig(EnumParts.BODY));
        apply(armor.leftArm, npc.modelData.getPartConfig(EnumParts.ARM_LEFT));
        apply(armor.rightArm, npc.modelData.getPartConfig(EnumParts.ARM_RIGHT));
        apply(armor.leftLeg, npc.modelData.getPartConfig(EnumParts.LEG_LEFT));
        apply(armor.rightLeg, npc.modelData.getPartConfig(EnumParts.LEG_RIGHT));
    }

    private static void apply(ModelPart part, ModelPartConfig config)
    {
        if (config == null)
            return;
        part.x += config.transX * UNITS;
        part.y += config.transY * UNITS;
        part.z += config.transZ * UNITS;
        part.xScale *= config.scaleX;
        part.yScale *= config.scaleY;
        part.zScale *= config.scaleZ;
    }
}
