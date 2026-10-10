package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansEquipment;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.client.ReadOnlyTooltips.Widget;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

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
            mark(gui, id, protection(npc, id, normal));
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
        DamageSource source = switch (id)
        {
            case 1 -> new DamageSource(npc.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW), npc, npc);
            case 3 -> npc.damageSources().explosion(npc, npc);
            default -> npc.damageSources().mobAttack(npc);
        };
        return NpcArmorPreview.protection(10F, normal, equippedValue(npc, Attributes.ARMOR), equippedValue(npc, Attributes.ARMOR_TOUGHNESS),
            EnchantmentHelper.getDamageProtection(npc.getArmorSlots(), source));
    }

    private static float equippedValue(EntityNPCInterface npc, Attribute attribute)
    {
        var modifiers = new ArrayList<AttributeModifier>();
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.ARMOR)
                modifiers.addAll(npc.getItemBySlot(slot).getAttributeModifiers(slot).get(attribute));
        }
        var instance = npc.getAttribute(attribute);
        return (float) EquipmentAttributes.value(attribute, instance == null ? 0D : instance.getBaseValue(), modifiers);
    }

    private static void mark(GuiBasic gui, int id, float protection)
    {
        var slider = gui.getSlider(id);
        if (slider == null)
            return;
        slider.active = false;
        slider.sliderValue = Math.max(0F, Math.min(1F, protection));
        slider.startValue = slider.sliderValue;
        slider.setString(String.format(Locale.ROOT, "%.1f%% %s", protection * 100F, id == 0 ? "" : "@ 10 HP"));
        slider.setTooltip(ReadOnlyTooltips.create(gui, Widget.SLIDER, id, Reason.ITEM_ARMOR));
        var label = gui.getLabel(id);
        if (label != null)
        {
            label.setColor(0xA0A0A0);
            label.setTooltip(ReadOnlyTooltips.create(gui, Widget.LABEL, id, Reason.ITEM_ARMOR));
        }
    }
}
