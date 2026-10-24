package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.sim.NpcDriving;
import com.wolffsmod.npcs.sim.NpcMobility;
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

/** Explicit modes for the driving numbers whose zero sentinel inherits model handling. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GroundHandlingControls
{
    private static final int MODE_ID = 1900;
    private static final String[] MODES = {"wolffsmodnpcs.combat.mode.inherit", "wolffsmodnpcs.combat.mode.custom"};
    /** Share of its top speed whose time to reach measures an engine response that is not a fixed rate. */
    private static final double MEASURED_SHARE = 0.9D;
    private static final int MEASURE_TICKS = 2000;

    static boolean supported(EntityNPCInterface npc, Value value)
    {
        return switch (value)
        {
            case HULL_TURN_RATE, TURN_RADIUS, DRIVE_REVERSE_SPEED, DRIVE_ACCELERATION, DRIVE_BRAKING, DRIVE_COASTING -> true;
            // Any other NPC keeps the plain field, whose 0 is its walking speed.
            case LAND_SPEED -> NpcMobility.modelSpeed(npc).isPresent();
            default -> false;
        };
    }

    static boolean inherited(EntityNPCInterface npc, Value value)
    {
        return supported(npc, value) && NpcTuning.of(npc).get(value) == 0;
    }

    static boolean render(GuiBasic gui, EntityNPCInterface npc, Value value, int fieldId, int x, int y, boolean loaded)
    {
        if (!supported(npc, value))
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
            case DRIVE_ACCELERATION -> accelerationPerSecond(settings, NpcMobility.topSpeed(npc));
            case DRIVE_BRAKING -> perSecond(settings.brakingAt(NpcMobility.topSpeed(npc), NpcMobility.topSpeed(npc)));
            case DRIVE_COASTING -> perSecond(settings.coastingAt(NpcMobility.topSpeed(npc), NpcMobility.topSpeed(npc)));
            case LAND_SPEED -> NpcMobility.topSpeed(npc) * 200D;
            default -> throw new IllegalArgumentException(value.name());
        };
    }

    /** @return the steady rate, in percent of top speed per second, that reaches the measured share as soon as the engine does */
    private static double accelerationPerSecond(NpcDriving.Settings settings, double topSpeed)
    {
        double share = 0D;
        for (int tick = 1; tick <= MEASURE_TICKS; tick++)
        {
            share += settings.accelerationAt(share * topSpeed, topSpeed);
            if (share >= MEASURED_SHARE)
                return MEASURED_SHARE * 100D * 20D / tick;
        }
        return MEASURED_SHARE * 100D * 20D / MEASURE_TICKS;
    }

    /** @return a throttle change per tick in percent per second */
    private static double perSecond(float perTick)
    {
        return perTick * 2000D;
    }

    static boolean button(EntityNPCInterface npc, GuiButtonNop button)
    {
        int index = button.id - MODE_ID;
        if (index < 0 || index >= Value.values().length || !supported(npc, Value.values()[index]))
            return false;
        Value value = Value.values()[index];
        double effective = effective(npc, value);
        if (value == Value.HULL_TURN_RATE && NpcTuning.of(npc).driveType() == NpcTuning.DriveType.HEAVY_TRACKED)
            effective /= 0.6D;
        int number = button.getValue() == 0 ? 0 : Math.max(1, Math.round((float) effective));
        NpcTuning.of(npc).set(value, number);
        // Land speed is a travel setting rather than one the driving presets fill in.
        if (value != Value.LAND_SPEED)
            NpcTuning.of(npc).set(Value.DRIVE_PRESET, NpcTuning.DrivePreset.CUSTOM.ordinal());
        return true;
    }
}
