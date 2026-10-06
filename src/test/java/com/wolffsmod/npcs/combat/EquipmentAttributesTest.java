package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentAttributesTest
{
    @Test
    void appliesAllThreeOperationsAgainstVanillaBaseRatherThanNpcDamage()
    {
        Attribute attribute = new RangedAttribute("test.attack", 1D, 0D, 1000D);
        var modifiers = List.of(modifier(6D, AttributeModifier.Operation.ADDITION), modifier(0.5D, AttributeModifier.Operation.MULTIPLY_BASE),
            modifier(0.2D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        assertEquals(12.6D, EquipmentAttributes.value(attribute, 1D, modifiers), 1E-9D);
    }

    @Test
    void usesAttributeBoundsAndDoesNotApplyTheSameUuidTwice()
    {
        Attribute attribute = new RangedAttribute("test.attack", 1D, 0D, 10D);
        var modifier = modifier(20D, AttributeModifier.Operation.ADDITION);
        assertEquals(10D, EquipmentAttributes.value(attribute, 1D, List.of(modifier, modifier)), 1E-9D);
    }

    @Test
    void attackSpeedProducesNativeTickIntervalsAndRejectsInvalidRates()
    {
        assertEquals(13, EquipmentAttributes.attackDelay(1.6D));
        assertEquals(5, EquipmentAttributes.attackDelay(4D));
        assertEquals(1, EquipmentAttributes.attackDelay(40D));
        assertEquals(20, EquipmentAttributes.attackDelay(0D));
        assertEquals(20, EquipmentAttributes.attackDelay(Double.NaN));
    }

    private static AttributeModifier modifier(double amount, AttributeModifier.Operation operation)
    {
        return new AttributeModifier(UUID.randomUUID(), "equipment", amount, operation);
    }
}
