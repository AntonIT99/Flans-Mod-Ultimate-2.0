package com.wolffsmod.npcs.combat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Resolves the item's attribute operations against vanilla combat bases, never against NPC editor damage. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EquipmentAttributes
{
    public static double value(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute, double base)
    {
        List<AttributeModifier> modifiers = new ArrayList<>();
        stack.forEachModifier(slot, (current, modifier) ->
        {
            if (current.equals(attribute))
                modifiers.add(modifier);
        });
        return value(attribute, base, modifiers);
    }

    public static double value(Holder<Attribute> attribute, double base, Iterable<AttributeModifier> modifiers)
    {
        AttributeInstance instance = new AttributeInstance(attribute, ignored ->
        {
        });
        instance.setBaseValue(base);
        for (AttributeModifier modifier : modifiers)
            if (!instance.hasModifier(modifier.id()))
                instance.addTransientModifier(modifier);
        return instance.getValue();
    }

    public static int attackDelay(double attacksPerSecond)
    {
        return Double.isFinite(attacksPerSecond) && attacksPerSecond > 0D ? Math.max(1, (int) Math.ceil(20D / attacksPerSecond)) : 20;
    }
}
