package com.wolffsmod.npcs.combat;

import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentAttributesTest
{
    @BeforeAll
    static void bootstrapMinecraft()
    {
        // NeoForge loads optional feature flags from its discovery list during vanilla bootstrap.
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void appliesAllThreeOperationsAgainstVanillaBaseRatherThanNpcDamage()
    {
        Holder<Attribute> attribute = Holder.direct(new RangedAttribute("test.attack", 1D, 0D, 1000D));
        var modifiers = List.of(modifier(6D, AttributeModifier.Operation.ADD_VALUE), modifier(0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            modifier(0.2D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        assertEquals(12.6D, EquipmentAttributes.value(attribute, 1D, modifiers), 1E-9D);
    }

    @Test
    void usesAttributeBoundsAndDoesNotApplyTheSameIdTwice()
    {
        Holder<Attribute> attribute = Holder.direct(new RangedAttribute("test.attack", 1D, 0D, 10D));
        var modifier = modifier(20D, AttributeModifier.Operation.ADD_VALUE);
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
        return new AttributeModifier(ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", UUID.randomUUID().toString()), amount, operation);
    }
}
