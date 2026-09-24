package com.flansmodultimate.platform.item;

import com.flansmodultimate.FlansMod;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

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

        private record Entry(Holder<Attribute> attribute, AttributeModifier modifier) {}

        private Modifiers() {}

        /**
         * @param idPath     1.21 modifier id, in the Flan's Mod Ultimate namespace
         * @param legacyUuid 1.20.1 modifier UUID; only requested on 1.20.1
         * @param legacyName 1.20.1 modifier name
         */
        public void add(Holder<Attribute> attribute, String idPath, Supplier<UUID> legacyUuid, String legacyName,
                        double amount, Operation operation)
        {
            AttributeModifier.Operation modernOperation = operation == Operation.ADD_VALUE
                ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, idPath);
            entries.add(new Entry(attribute, new AttributeModifier(id, amount, modernOperation)));
        }
    }

    /** Vanilla modifiers plus {@code additions}, applied while the item is held in the main hand. */
    public static ItemAttributeModifiers mainHand(ItemAttributeModifiers vanilla, Consumer<Modifiers> additions)
    {
        Modifiers modifiers = new Modifiers();
        additions.accept(modifiers);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : vanilla.modifiers())
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        modifiers.entries.forEach(entry -> builder.add(entry.attribute(), entry.modifier(), EquipmentSlotGroup.MAINHAND));
        return builder.build();
    }

    /**
     * Modifiers of an armor piece worn in {@code armorSlot}: {@code additions} replace the vanilla modifiers
     * of the same attributes, and the remaining vanilla modifiers are kept.
     */
    public static ItemAttributeModifiers armor(EquipmentSlot armorSlot, ItemAttributeModifiers vanilla, Consumer<Modifiers> additions)
    {
        Modifiers modifiers = new Modifiers();
        additions.accept(modifiers);
        Set<Holder<Attribute>> replaced = new HashSet<>();
        modifiers.entries.forEach(entry -> replaced.add(entry.attribute()));

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : vanilla.modifiers())
        {
            if (!replaced.contains(entry.attribute()))
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(armorSlot);
        modifiers.entries.forEach(entry -> builder.add(entry.attribute(), entry.modifier(), group));
        return builder.build();
    }
}
