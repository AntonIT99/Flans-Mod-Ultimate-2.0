package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.client.ReadOnlyTooltips.Widget;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentStats;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

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
        // Minecraft 1.21 protection enchantments depend on the damage source, so the armor decides those values.
        boolean conditionalProtection = NpcEquipmentStats.conditionalProtection(npc);
        for (int id = 0; id < 4; id++)
            mark(gui, id, NpcEquipmentStats.protection(npc, NpcEquipmentStats.Resistance.values()[id]), id != 0 && conditionalProtection);
        return true;
    }

    private static void mark(GuiBasic gui, int id, float protection, boolean conditionalEffects)
    {
        var slider = gui.getSlider(id);
        if (slider == null)
            return;
        slider.active = false;
        slider.sliderValue = Mth.clamp(protection, 0F, 1F);
        slider.startValue = slider.sliderValue;
        String healthHint = id == 0 ? "" : "@ 10 HP";
        slider.setString(conditionalEffects ? I18n.get("wolffsmodnpcs.weapons.item_value") : String.format(Locale.ROOT, "%.1f%% %s", protection * 100F, healthHint));
        slider.setTooltip(ReadOnlyTooltips.create(gui, Widget.SLIDER, id, Reason.ITEM_ARMOR));
        var label = gui.getLabel(id);
        if (label != null)
        {
            label.setColor(0xA0A0A0);
            label.setTooltip(ReadOnlyTooltips.create(gui, Widget.LABEL, id, Reason.ITEM_ARMOR));
        }
    }
}
