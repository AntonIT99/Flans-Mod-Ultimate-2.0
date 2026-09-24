package com.flansmodultimate.platform.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Version boundary for item attribute modifiers. 1.20.1 returns a per-slot multimap of modifiers identified
 * by UUID; 1.21 returns item attribute components identified by a resource location and a slot group.
 */
public final class ItemAttributes
{
    private ItemAttributes() {}

    public enum Operation
    {
        ADD_VALUE,
        ADD_MULTIPLIED_TOTAL
    }

    /** Modifiers an item adds on top of its vanilla ones. */
    public static final class Modifiers
    {
        private final List<Entry> entries = new ArrayList<>();

        private record Entry(Attribute attribute, AttributeModifier modifier) {}

        private Modifiers() {}

        /**
         * @param idPath     1.21 modifier id, in the Flan's Mod Ultimate namespace
         * @param legacyUuid 1.20.1 modifier UUID; only requested on 1.20.1
         * @param legacyName 1.20.1 modifier name
         */
        public void add(Attribute attribute, String idPath, Supplier<UUID> legacyUuid, String legacyName,
                        double amount, Operation operation)
        {
            AttributeModifier.Operation legacyOperation = operation == Operation.ADD_VALUE
                ? AttributeModifier.Operation.ADDITION : AttributeModifier.Operation.MULTIPLY_TOTAL;
            entries.add(new Entry(attribute, new AttributeModifier(legacyUuid.get(), legacyName, amount, legacyOperation)));
        }
    }

    /** Vanilla modifiers plus {@code additions}, applied while the item is held in the main hand. */
    public static Multimap<Attribute, AttributeModifier> mainHand(EquipmentSlot slot, Multimap<Attribute, AttributeModifier> vanilla,
                                                                  Consumer<Modifiers> additions)
    {
        if (slot != EquipmentSlot.MAINHAND)
            return vanilla;

        Modifiers modifiers = new Modifiers();
        additions.accept(modifiers);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(vanilla);
        modifiers.entries.forEach(entry -> builder.put(entry.attribute(), entry.modifier()));
        return builder.build();
    }

    /**
     * Modifiers of an armor piece worn in {@code armorSlot}: {@code additions} replace the vanilla modifiers
     * of the same attributes, and the remaining vanilla modifiers are kept.
     */
    public static Multimap<Attribute, AttributeModifier> armor(EquipmentSlot slot, EquipmentSlot armorSlot,
                                                               Multimap<Attribute, AttributeModifier> vanilla,
                                                               Consumer<Modifiers> additions)
    {
        if (slot != armorSlot)
            return vanilla;

        Modifiers modifiers = new Modifiers();
        additions.accept(modifiers);
        Set<Attribute> replaced = new HashSet<>();
        modifiers.entries.forEach(entry -> replaced.add(entry.attribute()));

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (var entry : vanilla.entries())
        {
            if (entry.getKey() != null && entry.getValue() != null && !replaced.contains(entry.getKey()))
                builder.put(entry.getKey(), entry.getValue());
        }
        modifiers.entries.forEach(entry -> builder.put(entry.attribute(), entry.modifier()));
        return builder.build();
    }
}
