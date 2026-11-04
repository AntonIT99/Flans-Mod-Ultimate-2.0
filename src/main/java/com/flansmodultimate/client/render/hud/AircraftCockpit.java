package com.flansmodultimate.client.render.hud;

import com.flansmodultimate.api.driveables.AircraftDamage;
import com.flansmodultimate.api.driveables.AircraftMode;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.damage.AircraftDamageModel;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * What the pilot of a damaged aircraft feels and is told: the warnings on the instrument panel with their alarm, a
 * tremor through the airframe that grows with the damage, and oil thrown across the canopy by a hit engine. Client side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AircraftCockpit
{
    /** A warning on the panel: what it says and whether it is the red kind that sounds the alarm. */
    record Warning(Component text, boolean critical)
    {}

    private static final int AMBER = 0xFFB000;
    private static final int RED = 0xFF3030;
    /** Ticks between two sounds of the alarm while anything critical is lit. */
    private static final int ALARM_INTERVAL = 30;
    /** Below this the tremor is not worth shaking the view for. */
    private static final float MIN_VIBRATION = 0.08F;
    /** Degrees the strongest tremor rolls and pitches the view by. */
    private static final float MAX_TREMOR_ROLL = 1.4F;
    private static final float MAX_TREMOR_PITCH = 0.8F;
    /** Engine health under which oil starts to streak the canopy. */
    private static final float OIL_ENGINE_HEALTH = 0.5F;
    /** Streaks of oil on the canopy at worst, and their darkest. */
    private static final int OIL_STREAKS = 14;
    private static final int OIL_ALPHA = 0x90;

    private static List<Warning> warnings = List.of();
    private static int criticalCount;
    private static int alarmCooldown;
    private static float vibration;
    private static float oil;
    private static int oilSeed;

    /** Reads the damage of the aircraft the player flies, and sounds the alarm. Client tick. */
    public static void tick()
    {
        Plane plane = pilotedPlane();
        if (plane == null || !ModCommonConfig.realisticAircraftDamageEnabled())
        {
            warnings = List.of();
            criticalCount = 0;
            vibration = 0F;
            oil = Math.max(0F, oil - 0.02F);
            return;
        }
        AircraftDamage damage = plane.getAircraftDamage();
        AircraftMode mode = plane.getAircraftMode();
        warnings = warningsFor(plane, damage, mode);
        int critical = (int) warnings.stream().filter(Warning::critical).count();
        // The alarm sounds at once for a new red warning, then keeps on while any is lit.
        if (critical > criticalCount || critical > 0 && --alarmCooldown <= 0)
        {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), 1.6F, 0.5F));
            alarmCooldown = ALARM_INTERVAL;
        }
        criticalCount = critical;
        vibration = plane.isWreck() ? 0F : AircraftDamageModel.vibration(damage, mode);
        float wantedOil = damage.engine() < OIL_ENGINE_HEALTH ? (OIL_ENGINE_HEALTH - damage.engine()) / OIL_ENGINE_HEALTH : 0F;
        if (wantedOil > oil + 0.25F)
            oilSeed = plane.getId() * 31 + plane.tickCount;
        oil = Mth.approach(oil, wantedOil, 0.01F);
    }

    @Nullable
    private static Plane pilotedPlane()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return null;
        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        return driveable instanceof Plane plane && plane.getPlaneType() != null ? plane : null;
    }

    private static List<Warning> warningsFor(Plane plane, AircraftDamage damage, AircraftMode mode)
    {
        List<Warning> list = new ArrayList<>();
        if (isBurning(plane))
            warn(list, "fire", true);
        if (plane.isEngineShotOut())
            warn(list, "engine_failure", true);
        else if (AircraftDamageModel.thrustFraction(damage) < 0.85F)
            warn(list, "engine", false);
        if (AircraftDamageModel.fuelLeakPerSecond(damage) > 0F && plane.getFuel() > 0F)
            warn(list, "fuel_leak", false);
        if (mode == AircraftMode.ROTORCRAFT)
            rotorcraftWarnings(list, plane, damage);
        else
            wingedWarnings(list, damage);
        if (AircraftDamageModel.crashMode(damage, mode).falling())
            warn(list, "mayday", true);
        return List.copyOf(list);
    }

    private static boolean isBurning(Plane plane)
    {
        boolean burning = false;
        if (plane.getDriveableData() != null)
        {
            for (DriveablePart part : plane.getDriveableData().getParts().values())
                burning |= part.isOnFire() && !part.isDestroyed();
        }
        return burning;
    }

    private static void rotorcraftWarnings(List<Warning> list, Plane plane, AircraftDamage damage)
    {
        if (damage.rotor() <= 0F)
            warn(list, "rotor_lost", true);
        else if (damage.rotor() < 0.6F)
            warn(list, "rotor", false);
        if (AircraftDamageModel.tailRotorAuthority(damage) <= 0F)
            warn(list, "tail_rotor_lost", true);
        if (plane.isAutorotating())
            warn(list, "autorotation", true);
    }

    private static void wingedWarnings(List<Warning> list, AircraftDamage damage)
    {
        if (damage.leftWing() <= 0F || damage.rightWing() <= 0F)
            warn(list, "wing_lost", true);
        else if (Math.min(damage.leftWing(), damage.rightWing()) < 0.6F)
            warn(list, "wing", false);
        if (damage.tail() <= 0F)
            warn(list, "tail_lost", true);
        else if (AircraftDamageModel.tailAuthority(damage) < 0.75F)
            warn(list, "controls", false);
    }

    private static void warn(List<Warning> list, String key, boolean critical)
    {
        list.add(new Warning(Component.translatable("hud.flansmodultimate.aircraft.warning." + key), critical));
    }

    /**
     * Draws the panel's warnings under the rest of the aircraft's readout, the red ones blinking.
     *
     * @return the height used
     */
    static int renderWarnings(Driveable driveable, GuiGraphics g, Font font, int x, int y, int lineHeight)
    {
        if (!(driveable instanceof Plane))
            return 0;
        boolean lit = (Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime()) / 5L % 2L == 0L;
        int line = 0;
        for (Warning warning : warnings)
        {
            if (warning.critical() && !lit)
            {
                ++line;
                continue;
            }
            g.drawString(font, warning.text(), x, y + lineHeight * line++, warning.critical() ? RED : AMBER, false);
        }
        return lineHeight * line;
    }

    /** @return degrees the airframe's tremor rolls the view by at this moment */
    public static float tremorRoll(float partialTick)
    {
        if (vibration < MIN_VIBRATION || !firstPerson())
            return 0F;
        float time = tickTime(partialTick);
        return (float) (Math.sin(time * 2.3F) * 0.6F + Math.sin(time * 5.1F) * 0.4F) * vibration * MAX_TREMOR_ROLL;
    }

    /** @return degrees the airframe's tremor pitches the view by at this moment */
    public static float tremorPitch(float partialTick)
    {
        if (vibration < MIN_VIBRATION || !firstPerson())
            return 0F;
        float time = tickTime(partialTick);
        return (float) (Math.sin(time * 3.7F + 1F) * 0.7F + Math.sin(time * 6.3F) * 0.3F) * vibration * MAX_TREMOR_PITCH;
    }

    private static float tickTime(float partialTick)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0F : player.tickCount + partialTick;
    }

    private static boolean firstPerson()
    {
        return Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
    }

    /** Oil thrown back across the canopy by a hit engine: dark streaks running down the view, in first person. */
    public static void renderOil(GuiGraphics g, int sw, int sh)
    {
        if (oil <= 0.01F || !firstPerson() || pilotedPlane() == null)
            return;
        int streaks = Math.max(1, Math.round(OIL_STREAKS * oil));
        int alpha = Math.round(OIL_ALPHA * Math.min(1F, 0.4F + oil));
        // Placed by a fixed seed, so the oil stays where it struck rather than flickering about the screen.
        long seed = oilSeed;
        for (int i = 0; i < streaks; i++)
        {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int x = (int) Math.floorMod(seed >>> 16, (long) sw);
            int width = 2 + (int) Math.floorMod(seed >>> 40, 6L);
            int top = (int) Math.floorMod(seed >>> 24, (long) Math.max(1, sh / 3));
            int length = (int) (sh * (0.2F + 0.6F * oil) * (0.5F + Math.floorMod(seed >>> 32, 50L) / 100F));
            int dark = alpha << 24 | 0x14100C;
            g.fillGradient(x, top, x + width, Math.min(sh, top + length), dark, dark & 0x00FFFFFF);
            // A blot where it struck.
            g.fill(x - 1, top, x + width + 1, top + width + 2, dark);
        }
    }
}
