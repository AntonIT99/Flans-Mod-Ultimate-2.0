package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.sim.NpcDriving;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Explicit modes for the three driving numbers whose existing zero sentinel now inherits model handling. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GroundHandlingControls
{
    private static final int MODE_ID = 1900;
    private static final String[] MODES = {"wolffsmodnpcs.combat.mode.inherit", "wolffsmodnpcs.combat.mode.custom"};

    static boolean supported(Value value)
    {
        return value == Value.HULL_TURN_RATE || value == Value.TURN_RADIUS || value == Value.DRIVE_REVERSE_SPEED;
    }

    static boolean inherited(EntityNPCInterface npc, Value value)
    {
        return supported(value) && NpcTuning.of(npc).get(value) == 0;
    }

    static boolean render(GuiBasic gui, EntityNPCInterface npc, Value value, int fieldId, int x, int y, boolean loaded)
    {
        if (!supported(value))
            return false;
        int stored = NpcTuning.of(npc).get(value);
        boolean inherit = stored == 0;
        double shown = inherit ? effective(npc, value) : stored;
        GuiTextFieldNop field = new GuiTextFieldNop(fieldId, gui, x, y, 60, 20, BigDecimal.valueOf(shown).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
        field.numbersOnly = !inherit;
        field.setMinMaxDefault(1, value.maximum(), 1);
        field.setEditable(loaded && !inherit);
        gui.addTextField(field);
        GuiButtonNop mode = new GuiButtonNop(gui, MODE_ID + value.ordinal(), x + 64, y, 80, 20, MODES, inherit ? 0 : 1);
        mode.setEnabled(loaded);
        mode.setTooltip(Tooltip.create(Component.translatable("wolffsmodnpcs.tuning.handling_mode.help")));
        gui.addButton(mode);
        return true;
    }

    private static double effective(EntityNPCInterface npc, Value value)
    {
        NpcDriving.Settings settings = NpcDriving.settings(npc);
        return switch (value)
        {
            case HULL_TURN_RATE -> settings.turnStep() * 20D;
            case TURN_RADIUS -> settings.radius() * 10D;
            case DRIVE_REVERSE_SPEED -> settings.reverseShare() * 100D;
            default -> throw new IllegalArgumentException(value.name());
        };
    }

    static boolean button(EntityNPCInterface npc, GuiButtonNop button)
    {
        int index = button.id - MODE_ID;
        if (index < 0 || index >= Value.values().length || !supported(Value.values()[index]))
            return false;
        Value value = Value.values()[index];
        double effective = effective(npc, value);
        if (value == Value.HULL_TURN_RATE && NpcTuning.of(npc).driveType() == NpcTuning.DriveType.HEAVY_TRACKED)
            effective /= 0.6D;
        int number = button.getValue() == 0 ? 0 : Math.max(1, Math.round((float) effective));
        NpcTuning.of(npc).set(value, number);
        NpcTuning.of(npc).set(Value.DRIVE_PRESET, NpcTuning.DrivePreset.CUSTOM.ordinal());
        return true;
    }
}
