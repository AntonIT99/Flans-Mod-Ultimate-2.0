package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.armor.VehicleArmorSpec;
import com.flansmodultimate.common.driveables.armor.VehicleHealthScaler;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehicleGeometry;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the physics properties a driveable is <em>actually</em> using.
 *
 * <p>Every number here comes from the same {@link ResolvedVehiclePhysics} the
 * runtime physics reads, and speed conversions go through the same
 * {@link VehiclePhysicsUnits} helpers with the same config scale. No derivation
 * is repeated locally, so the tooltip cannot drift from the simulation.
 *
 * <p>The three tiers are kept visually distinct: authored real-world source
 * values sit at the top level, derived and Minecraft-effective values are
 * indented beneath them, and a vehicle still on legacy physics is labelled as
 * such and shown the legacy fields that are genuinely driving it.
 */
public final class DriveablePhysicsTooltip
{
    private static final String PREFIX = "tooltip.flansmodultimate.physics.";

    private DriveablePhysicsTooltip() {}

    public static void append(DriveableType type, List<Component> tooltip)
    {
        if (type == null)
            return;
        ResolvedVehiclePhysics resolved = type.getResolvedPhysics();
        if (resolved == null)
            return;

        double speedScale = ModCommonConfig.realisticVehicleSpeedScale();

        tooltip.add(Component.empty());
        tooltip.add(IFlanItem.statLine(label("header"),
            Component.translatable(PREFIX + "mode." + resolved.mode().translationSuffix()).getString()));

        if (resolved.hasGroundPropulsion() || resolved.hasAircraftProfile())
            appendProfile(type, resolved, speedScale, tooltip);
        else
            appendLegacyPropulsion(type, tooltip);

        appendIndependentOverrides(resolved, speedScale, tooltip);
        appendGeometry(resolved.geometry(), tooltip);
        appendSpeedScale(speedScale, resolved, tooltip);
        appendArmorAndHealth(type, tooltip);
    }

    private static void appendArmorAndHealth(DriveableType type, List<Component> tooltip)
    {
        VehicleArmorSpec armor = type.getArmorSpec();
        VehicleHealthScaler.Result health = type.getResolvedHealth();
        if ((armor == null || armor.isEmpty()) && health != null && !health.requested())
            return;

        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Vehicle armour and health").withStyle(ChatFormatting.GOLD));
        if (armor != null)
        {
            appendPlateGroup("Hull", armor.hull(), tooltip);
            appendPlateGroup("Turret", armor.turret(), tooltip);
            for (Map.Entry<EnumDriveablePart, ArmorPlate> entry : armor.partOverrides().entrySet())
                tooltip.add(IFlanItem.statLine(Component.literal("Part " + entry.getKey().getName()),
                    formatPlate(entry.getValue())));
        }

        if (type.getRealWorldSpec().massKg() != null)
            tooltip.add(IFlanItem.statLine(Component.literal("RealMassKg"),
                IFlanItem.formatFloat(type.getRealWorldSpec().massKg(), 0) + " kg"));
        if (health != null)
        {
            tooltip.add(IFlanItem.statLine(Component.literal("Normalized health"),
                health.enabled() ? "enabled" : health.requested() ? "requested; legacy fallback" : "disabled"));
            if (health.enabled())
            {
                tooltip.add(IFlanItem.indentedStatLine(Component.literal("Derived total HP"),
                    IFlanItem.formatFloat(health.totalHp(), 1)));
                for (Map.Entry<EnumDriveablePart, Float> entry : health.allocations().entrySet())
                {
                    if (entry.getValue() > 0F)
                        tooltip.add(IFlanItem.indentedStatLine(Component.literal(entry.getKey().getName()),
                            IFlanItem.formatFloat(entry.getValue(), 1) + " HP"));
                }
            }
        }
    }

    private static void appendPlateGroup(String prefix, Map<EnumArmorFacing, ArmorPlate> plates,
                                         List<Component> tooltip)
    {
        for (EnumArmorFacing facing : EnumArmorFacing.values())
        {
            ArmorPlate plate = plates.get(facing);
            if (plate != null)
                tooltip.add(IFlanItem.statLine(Component.literal(prefix + " "
                    + facing.name().toLowerCase(Locale.ROOT)), formatPlate(plate)));
        }
    }

    private static String formatPlate(ArmorPlate plate)
    {
        String value = IFlanItem.formatFloat(plate.thicknessMm(), 1) + " mm";
        return plate.slopeDeg() == 0F ? value
            : value + " @ " + IFlanItem.formatFloat(plate.slopeDeg(), 1) + " deg";
    }

    // ------------------------------------------------------- real-world path

    private static void appendProfile(DriveableType type, ResolvedVehiclePhysics resolved,
                                      double speedScale, List<Component> tooltip)
    {
        RealWorldVehicleSpec source = resolved.source();

        // Tier 1: authored real-world source values, in real-world units.
        tooltip.add(IFlanItem.statLine(label("mass"), IFlanItem.formatFloat(resolved.massKg(), 0)));
        if (resolved.baselineThrustKn() > 0F)
            tooltip.add(IFlanItem.statLine(label("engineThrust"), IFlanItem.formatFloat(resolved.baselineThrustKn(), 1)));
        if (resolved.baselinePowerKw() > 0F)
            tooltip.add(IFlanItem.statLine(label("enginePower"), IFlanItem.formatFloat(resolved.baselinePowerKw(), 0)));
        tooltip.add(IFlanItem.statLine(label("maxSpeed"), IFlanItem.formatFloat(resolved.maxSpeedKmh(), 0)));

        // Tier 2: values derived from those, and tier 3: the Minecraft speeds.
        tooltip.add(IFlanItem.indentedStatLine(label("effectiveSpeed"),
            IFlanItem.formatDouble(resolved.maxSpeedBlocksPerTick(speedScale), 2)));
        if (resolved.powerToWeightKwPerKg() > 0F)
            tooltip.add(IFlanItem.indentedStatLine(label("powerToWeight"),
                IFlanItem.formatFloat(resolved.powerToWeightKwPerKg(), 3)));
        if (resolved.thrustToWeight() > 0F)
            tooltip.add(IFlanItem.indentedStatLine(label("thrustToWeight"),
                IFlanItem.formatFloat(resolved.thrustToWeight(), 3)));

        if (resolved.hasAircraftProfile())
        {
            RealWorldVehicleSpec.Aircraft aircraft = source.aircraft();
            if (aircraft.wingSpanM() != null)
                tooltip.add(IFlanItem.statLine(label("wingSpan"), IFlanItem.formatFloat(aircraft.wingSpanM(), 2)));
            if (aircraft.wingAreaM2() != null)
                tooltip.add(IFlanItem.statLine(label("wingArea"), IFlanItem.formatFloat(aircraft.wingAreaM2(), 2)));
            if (aircraft.climbRateMs() != null)
                tooltip.add(IFlanItem.statLine(label("climbRate"), IFlanItem.formatFloat(aircraft.climbRateMs(), 1)));
            tooltip.add(IFlanItem.indentedStatLine(label("wingLoading"),
                IFlanItem.formatFloat(resolved.wingLoadingKgPerM2(), 1)));
            tooltip.add(IFlanItem.indentedStatLine(label("referenceSpeed"),
                IFlanItem.formatDouble(resolved.referenceSpeedMs(speedScale), 1)));
        }

        // Legacy fields that remain in force as deliberate gameplay trims.
        appendRetainedTrims(type, tooltip);
    }

    /**
     * The handling modifiers a pack authored are kept even under the real-world
     * profile, because they express intent the source data cannot: asymmetric
     * steering, a deliberately twitchy or sluggish airframe. Naming them here
     * makes it obvious they are still doing something.
     */
    private static void appendRetainedTrims(DriveableType type, List<Component> tooltip)
    {
        if (type instanceof VehicleType vehicle)
        {
            if (vehicle.getTurnLeftModifier() != 1F || vehicle.getTurnRightModifier() != 1F)
                tooltip.add(IFlanItem.indentedStatLine(label("legacy.steeringTrim"),
                    IFlanItem.formatFloat(vehicle.getTurnLeftModifier(), 2) + " / "
                        + IFlanItem.formatFloat(vehicle.getTurnRightModifier(), 2)));
        }
        else if (type instanceof PlaneType plane)
        {
            tooltip.add(IFlanItem.indentedStatLine(label("legacy.controlTrim"),
                IFlanItem.formatFloat(plane.getTurnRightModifier(), 2) + " / "
                    + IFlanItem.formatFloat(plane.getLookUpModifier(), 2) + " / "
                    + IFlanItem.formatFloat(plane.getRollRightModifier(), 2)));
        }
    }

    // ------------------------------------------------------------ legacy path

    /**
     * A vehicle without a complete profile is running legacy propulsion, so the
     * tooltip shows the legacy fields that are genuinely in force rather than
     * pretending a real-world value applies.
     */
    private static void appendLegacyPropulsion(DriveableType type, List<Component> tooltip)
    {
        tooltip.add(IFlanItem.statLine(label("legacy.maxThrottle"),
            IFlanItem.formatFloat(type.getMaxThrottle(), 2)));
        if (type.getMaxNegativeThrottle() > 0F)
            tooltip.add(IFlanItem.statLine(label("legacy.maxReverseThrottle"),
                IFlanItem.formatFloat(type.getMaxNegativeThrottle(), 2)));
        tooltip.add(IFlanItem.statLine(label("legacy.drag"), IFlanItem.formatFloat(type.getDrag(), 2)));

        if (type instanceof VehicleType vehicle)
            tooltip.add(IFlanItem.statLine(label("legacy.acceleration"),
                translate("legacy.acceleration." + (vehicle.isUseRealisticAcceleration() ? "realistic" : "fixed"))));
        else if (type instanceof PlaneType plane)
            tooltip.add(IFlanItem.statLine(label("legacy.flightModel"),
                translate("legacy.flightModel." + (plane.isNewFlightControl() ? "newFlightControl" : "legacy"))));
    }

    // -------------------------------------------------- independent overrides

    private static void appendIndependentOverrides(ResolvedVehiclePhysics resolved, double speedScale,
                                                   List<Component> tooltip)
    {
        if (resolved.category() == EnumVehicleCategory.GROUND)
        {
            tooltip.add(IFlanItem.statLine(label("driveType"),
                translate("drivetype." + resolved.driveType().name().toLowerCase(Locale.ROOT))
                    + (resolved.driveTypeExplicit() ? "" : " " + translate("inferred"))));
        }
        if (resolved.hasReverseSpeedOverride())
        {
            tooltip.add(IFlanItem.statLine(label("reverseSpeed"),
                IFlanItem.formatFloat(resolved.maxReverseSpeedKmh(), 0)));
            tooltip.add(IFlanItem.indentedStatLine(label("effectiveReverseSpeed"),
                IFlanItem.formatDouble(resolved.reverseSpeedBlocksPerTick(speedScale), 2)));
        }
        if (resolved.hasSlopeLimit())
            tooltip.add(IFlanItem.statLine(label("maxSlope"), IFlanItem.formatFloat(resolved.maxSlopeDeg(), 0)));
        if (resolved.hasDraft())
            tooltip.add(IFlanItem.statLine(label("draft"), IFlanItem.formatFloat(resolved.draftM(), 2)));
    }

    /** Dimensions are always derived, never authored, so they are always indented. */
    private static void appendGeometry(@Nullable VehicleGeometry geometry, List<Component> tooltip)
    {
        if (geometry == null)
            return;
        if (geometry.lengthM() != null)
            tooltip.add(IFlanItem.indentedStatLine(label("length"), IFlanItem.formatFloat(geometry.lengthM(), 2)));
        if (geometry.widthM() != null)
            tooltip.add(IFlanItem.indentedStatLine(label("width"), IFlanItem.formatFloat(geometry.widthM(), 2)));
        if (geometry.wheelbaseM() != null)
            tooltip.add(IFlanItem.indentedStatLine(label("wheelbase"), IFlanItem.formatFloat(geometry.wheelbaseM(), 2)));
    }

    /**
     * The scale is only worth a line when it is not the default, but then it is
     * essential: it is the reason a stated km/h does not match the speed in game.
     */
    private static void appendSpeedScale(double speedScale, ResolvedVehiclePhysics resolved, List<Component> tooltip)
    {
        if (Math.abs(speedScale - 1D) < 1.0E-6D)
            return;
        if (!resolved.hasGroundPropulsion() && !resolved.hasAircraftProfile() && !resolved.hasReverseSpeedOverride())
            return;
        tooltip.add(Component.translatable(PREFIX + "speedScale",
            IFlanItem.formatDouble(speedScale, 2)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component label(String key)
    {
        return Component.translatable(PREFIX + key);
    }

    private static String translate(String key)
    {
        return Component.translatable(PREFIX + key).getString();
    }
}
