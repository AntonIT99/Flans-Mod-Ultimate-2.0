package com.wolffsmod.npcs.combat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/** Resolves the item's attribute operations against vanilla combat bases, never against NPC editor damage. */
public final class EquipmentAttributes
{
    private EquipmentAttributes()
    {}

    public static double value(ItemStack stack, EquipmentSlot slot, Attribute attribute, double base)
    {
        return value(attribute, base, stack.getAttributeModifiers(slot).get(attribute));
    }

    public static double value(Attribute attribute, double base, Iterable<AttributeModifier> modifiers)
    {
        AttributeInstance instance = new AttributeInstance(attribute, ignored ->
        {
        });
        instance.setBaseValue(base);
        for (AttributeModifier modifier : modifiers)
            if (!instance.hasModifier(modifier))
                instance.addTransientModifier(modifier);
        return instance.getValue();
    }

    public static int attackDelay(double attacksPerSecond)
    {
        return Double.isFinite(attacksPerSecond) && attacksPerSecond > 0D ? Math.max(1, (int) Math.ceil(20D / attacksPerSecond)) : 20;
    }
}
