package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.sim.NpcDriving;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Explicit modes for the driving and aiming numbers whose zero sentinel inherits model handling, shared by the Travel and
 * Aiming pages and the model properties page.
 */
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
            // Only a content-pack model has a gun mount to take an aim speed from.
            case AIM_SPEED_YAW, AIM_SPEED_PITCH -> NpcModels.flan(npc) != null;
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
        GuiTextFieldNop field = new GuiTextFieldNop(fieldId, gui, x, y, 60, 20, format(shown));
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

    /** @return a value as the editor shows it, with up to three decimals */
    static String format(double value)
    {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** @return the value the NPC uses while it inherits: the model's, or the documented fallback */
    static double effective(EntityNPCInterface npc, Value value)
    {
        if (value == Value.AIM_SPEED_YAW || value == Value.AIM_SPEED_PITCH)
            return NpcVehicleState.modelAimStep(npc, value == Value.AIM_SPEED_PITCH) * 20D;
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
        choose(npc, Value.values()[index], button.getValue() == 1);
        return true;
    }

    /** Switches a value to Custom, starting from the effective value, or back to Inherit, which clears it. */
    static void choose(EntityNPCInterface npc, Value value, boolean custom)
    {
        if (!custom)
        {
            store(npc, value, 0);
            return;
        }
        double effective = effective(npc, value);
        if (value == Value.HULL_TURN_RATE && NpcTuning.of(npc).driveType() == NpcTuning.DriveType.HEAVY_TRACKED)
            effective /= 0.6D;
        store(npc, value, Math.max(1, Math.round((float) effective)));
    }

    /** Stores a value; a driving value the presets fill in turns the preset to Custom, as the Driving page does. */
    static void store(EntityNPCInterface npc, Value value, int number)
    {
        NpcTuning.of(npc).set(value, number);
        // Land speed is a travel setting and aim speeds belong to the turret: no driving preset fills them in.
        if (value != Value.LAND_SPEED && value != Value.AIM_SPEED_YAW && value != Value.AIM_SPEED_PITCH)
            NpcTuning.of(npc).set(Value.DRIVE_PRESET, NpcTuning.DrivePreset.CUSTOM.ordinal());
    }
}
