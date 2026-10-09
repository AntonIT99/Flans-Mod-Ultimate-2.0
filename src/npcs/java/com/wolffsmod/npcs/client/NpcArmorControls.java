package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.combat.EquipmentAttributes;
import com.wolffsmod.npcs.combat.NpcArmorPreview;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;

import java.util.ArrayList;
import java.util.Locale;

/** Nonlinear armor protection is previewed at ten incoming damage, never stored as a linear NPC resistance. */
public final class NpcArmorControls
{
    private NpcArmorControls()
    {}

    public static boolean apply(GuiBasic gui)
    {
        EntityNPCInterface npc = NpcRangedControls.findNpc(gui);
        if (npc == null || !NpcEquipment.enabled(npc, Feature.ITEM_ARMOR))
            return false;
        float normal = normalDefense(npc);
        for (int id = 0; id < 4; id++)
            mark(gui, id, protection(npc, id, normal), id != 0 && hasProtectionEffects(npc));
        return true;
    }

    private static float normalDefense(EntityNPCInterface npc)
    {
        float normal = 0F;
        for (ItemStack stack : npc.getArmorSlots())
        {
            var properties = FlansEquipment.getArmorProperties(stack);
            if (properties.isPresent())
            {
                normal = (float) (normal + properties.get().defense());
            }
        }
        return normal;
    }

    private static float protection(EntityNPCInterface npc, int id, float normal)
    {
        if (id == 0)
            return equippedValue(npc, Attributes.KNOCKBACK_RESISTANCE);
        return NpcArmorPreview.protection(10F, normal, equippedValue(npc, Attributes.ARMOR), equippedValue(npc, Attributes.ARMOR_TOUGHNESS), 0);
    }

    /** Conditional 1.21 enchantment protection needs the real server damage context. */
    private static boolean hasProtectionEffects(EntityNPCInterface npc)
    {
        var enchantments = npc.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ItemStack armor : npc.getArmorSlots())
            if (armor.getAllEnchantments(enchantments).keySet().stream().anyMatch(enchantment -> enchantment.value().effects().has(EnchantmentEffectComponents.DAMAGE_PROTECTION)))
                return true;
        return false;
    }

    private static float equippedValue(EntityNPCInterface npc, Holder<Attribute> attribute)
    {
        var modifiers = new ArrayList<AttributeModifier>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                npc.getItemBySlot(slot).forEachModifier(slot, (current, modifier) ->
                {
                    if (current.equals(attribute))
                        modifiers.add(modifier);
                });
        }
        var instance = npc.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }

    private static void mark(GuiBasic gui, int id, float protection, boolean conditionalEffects)
    {
        var slider = gui.getSlider(id);
        if (slider == null)
            return;
        slider.active = false;
        slider.sliderValue = Math.clamp(protection, 0F, 1F);
        slider.startValue = slider.sliderValue;
        String healthHint = id == 0 ? "" : "@ 10 HP";
        slider.setString(conditionalEffects ? I18n.get("wolffsmodnpcs.weapons.item_value") : String.format(Locale.ROOT, "%.1f%% %s", protection * 100F, healthHint));
        slider.setTooltip(Tooltip.create(Component.translatable("wolffsmodnpcs.weapons.read_only.item_armor")));
        var label = gui.getLabel(id);
        if (label != null)
        {
            label.setColor(0xA0A0A0);
            label.setTooltip(Tooltip.create(Component.translatable("wolffsmodnpcs.weapons.read_only.item_armor")));
        }
    }
}
