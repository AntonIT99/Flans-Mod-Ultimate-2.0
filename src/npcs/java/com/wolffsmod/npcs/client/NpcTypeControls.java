package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import com.wolffsmod.npcs.properties.NpcTypeReadouts;
import noppes.npcs.client.gui.SubGuiNpcMeleeProperties;
import noppes.npcs.client.gui.SubGuiNpcMovement;
import noppes.npcs.client.gui.SubGuiNpcProjectiles;
import noppes.npcs.client.gui.SubGuiNpcRangeProperties;
import noppes.npcs.client.gui.advanced.GuiNPCSoundsMenu;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/** Displays converted type values, while the runtime overlay retains the original editable defaults for saves. */
public final class NpcTypeControls
{
    private static final RangedControlLocks EMPTY = new RangedControlLocks(Map.of(), Map.of(), Map.of());
    private static final Map<Integer, NpcTypeProperty> RANGED_FIELDS = Map.ofEntries(Map.entry(1, NpcTypeProperty.ACCURACY), Map.entry(2, NpcTypeProperty.RANGE),
        Map.entry(3, NpcTypeProperty.DELAY_MIN), Map.entry(4, NpcTypeProperty.DELAY_MAX), Map.entry(5, NpcTypeProperty.BURST_DELAY), Map.entry(6, NpcTypeProperty.BURST),
        Map.entry(7, NpcTypeProperty.FIRING_SOUND), Map.entry(8, NpcTypeProperty.SHOT_COUNT));

    private NpcTypeControls()
    {}

    public static RangedControlLocks apply(GuiBasic gui)
    {
        EntityNPCInterface npc = NpcRangedControls.findNpc(gui);
        return npc == null ? EMPTY : apply(gui, npc);
    }

    static RangedControlLocks apply(GuiBasic gui, EntityNPCInterface npc)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        properties.refresh(npc);
        Map<NpcTypeProperty, Tag> values = properties.values();
        Map<NpcTypeProperty, String> readouts = properties.type()
            .map(type -> NpcTypeReadouts.create(type, values, ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions(), properties.usesNativeProjectile())).orElse(Map.of());
        Map<Integer, Reason> fields = new HashMap<>();
        Map<Integer, Reason> buttons = new HashMap<>();
        Map<Integer, Reason> labels = new HashMap<>();
        fields(gui).forEach((id, property) ->
        {
            String value = readouts.get(property);
            var field = gui.getTextField(id);
            if (value != null && field != null)
            {
                field.setMaxLength(256);
                field.numbersOnly = false;
                field.floatsOnly = false;
                field.setValue(value);
                fields.put(id, Reason.TYPE_PROPERTIES);
                labels.put(id, Reason.TYPE_PROPERTIES);
            }
        });
        buttons(gui).forEach((id, property) ->
        {
            Tag value = values.get(property);
            var button = gui.getButton(id);
            if (value != null && button != null)
            {
                button.setDisplay(((ByteTag) value).getAsByte() != 0 ? 1 : 0);
                buttons.put(id, Reason.TYPE_PROPERTIES);
                labels.put(id == 5 ? 11 : 13, Reason.TYPE_PROPERTIES);
            }
        });
        for (int id : soundButtons(gui))
            if (fields.containsKey(id))
                buttons.put(id, Reason.TYPE_PROPERTIES);
        RangedControlLocks locks = new RangedControlLocks(fields, buttons, labels);
        NpcRangedControls.markControls(gui, locks);
        return RangedControlLocks.merge(locks, NpcEquipmentControls.apply(gui, npc));
    }

    private static Map<Integer, NpcTypeProperty> fields(GuiBasic gui)
    {
        if (gui instanceof GuiNpcStats)
            return Map.of(0, NpcTypeProperty.HEALTH, 1, NpcTypeProperty.AGGRO_RANGE);
        if (gui instanceof SubGuiNpcRangeProperties)
            return RANGED_FIELDS;
        if (gui instanceof SubGuiNpcProjectiles)
            return Map.of(1, NpcTypeProperty.DAMAGE, 4, NpcTypeProperty.SPEED);
        if (gui instanceof SubGuiNpcMeleeProperties)
            return Map.of(2, NpcTypeProperty.MELEE_REACH);
        if (gui instanceof SubGuiNpcMovement)
            return Map.of(14, NpcTypeProperty.WALKING_SPEED);
        if (gui instanceof GuiNPCSoundsMenu)
            return Map.of(0, NpcTypeProperty.IDLE_SOUND, 5, NpcTypeProperty.STEP_SOUND);
        return Map.of();
    }

    private static Map<Integer, NpcTypeProperty> buttons(GuiBasic gui)
    {
        return gui instanceof GuiNpcStats ? Map.of(5, NpcTypeProperty.CAN_DROWN, 7, NpcTypeProperty.NO_FALL_DAMAGE) : Map.of();
    }

    private static int[] soundButtons(GuiBasic gui)
    {
        if (gui instanceof SubGuiNpcRangeProperties)
            return new int[]{7};
        return gui instanceof GuiNPCSoundsMenu ? new int[]{0, 5} : new int[0];
    }
}
