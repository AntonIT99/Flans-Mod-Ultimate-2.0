package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.client.ReadOnlyTooltips.Widget;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentStats;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

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
        for (int id = 0; id < 4; id++)
            mark(gui, id, NpcEquipmentStats.protection(npc, NpcEquipmentStats.Resistance.values()[id]));
        return true;
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
