package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.client.ReadOnlyTooltips.Widget;
import com.wolffsmod.npcs.combat.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * Nonlinear armor protection is previewed at ten incoming damage, never stored as a linear NPC resistance. Only the
 * resistances the NPC inherits from its armor are previewed; the others stay the NPC's own and editable.
 */
public final class NpcArmorControls
{
    /** The armor statistic behind each resistance slider, in the order Custom NPCs numbers them. */
    private static final EquipmentProperty[] SLIDERS = {EquipmentProperty.ARMOR_KNOCKBACK, EquipmentProperty.ARMOR_PROJECTILE, EquipmentProperty.ARMOR_MELEE, EquipmentProperty.ARMOR_EXPLOSION};

    private NpcArmorControls()
    {}

    /** @return the IDs of the sliders that show the armor's protection and must not change the saved resistance */
    public static Set<Integer> apply(GuiBasic gui)
    {
        EntityNPCInterface npc = NpcRangedControls.findNpc(gui);
        Set<Integer> locked = new HashSet<>();
        if (npc == null)
            return locked;
        // Minecraft 1.21 protection enchantments depend on the damage source, so the armor decides those values.
        boolean conditionalProtection = NpcEquipmentStats.conditionalProtection(npc);
        for (int id = 0; id < SLIDERS.length; id++)
        {
            if (!NpcEquipment.inherits(npc, SLIDERS[id]))
                continue;
            mark(gui, id, NpcEquipmentStats.protection(npc, NpcEquipmentStats.Resistance.values()[id]), id != 0 && conditionalProtection);
            locked.add(id);
        }
        return locked;
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
