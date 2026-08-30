package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.armor.VehicleArmorSpec;
import com.flansmodultimate.common.driveables.armor.VehicleHealthScaler;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the physics properties a driveable is <em>actually</em> using.
 *
 * <p>Every number here comes from the same {@link ResolvedVehiclePhysics} the
 * runtime physics reads, so the tooltip cannot drift from the simulation.
 *
 * <p>Authored real-world source values, independently-usable overrides, and the
 * minimum flight speed pilots need during play are shown here; a vehicle still
 * on legacy physics is labelled as such and shown the legacy fields that are
 * genuinely driving it. Other derived and Minecraft-effective values are left
 * out to keep the tooltip short — they remain available through
 * {@code /vehiclephysics}, which reads the same
 * {@link ResolvedVehiclePhysics}.
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

        double speedScale = ModCommonConfig.realisticSpeedScale(resolved.category());
        double aircraftReferenceSpeedScale = ModCommonConfig.realisticAircraftReferenceSpeedScale();

        tooltip.add(IFlanItem.statLine(label("header"),
            Component.translatable(PREFIX + "mode." + resolved.mode().translationSuffix()).getString()));

        if (resolved.hasGroundPropulsion() || resolved.hasAircraftProfile())
            appendProfile(type, resolved, speedScale, aircraftReferenceSpeedScale, tooltip);
        else
            appendLegacyPropulsion(type, tooltip);

        appendIndependentOverrides(resolved, tooltip);
        appendSpeedScale(speedScale, resolved, tooltip);
        appendArmorAndHealth(type, tooltip);
    }

    private static void appendArmorAndHealth(DriveableType type, List<Component> tooltip)
    {
        VehicleArmorSpec armor = type.getArmorSpec();
        VehicleHealthScaler.Result health = type.getResolvedHealth();
        if ((armor == null || armor.isEmpty()) && (health == null || !health.enabled()))
            return;

        if (armor != null && !armor.isEmpty())
            appendArmorSummary(armor, tooltip);
        if (health != null && health.enabled())
            tooltip.add(IFlanItem.statLine(Component.literal("Total HP"),
                IFlanItem.formatFloat(health.totalHp(), 1)));
    }

    private static void appendArmorSummary(VehicleArmorSpec armor, List<Component> tooltip)
    {
        tooltip.add(IFlanItem.statLine(label("armor.header").copy(), label("armor.facings")));
        if (!armor.hull().isEmpty())
            tooltip.add(IFlanItem.indentedStatLine(label("armor.hull"), formatArmorSummary(armor.hull())));
        if (!armor.turret().isEmpty())
            tooltip.add(IFlanItem.indentedStatLine(label("armor.turret"), formatArmorSummary(armor.turret())));
    }

    static String formatArmorSummary(Map<EnumArmorFacing, ArmorPlate> plates)
    {
        ArmorPlate side = plates.get(EnumArmorFacing.LEFT);
        if (side == null)
            side = plates.get(EnumArmorFacing.RIGHT);
        return formatSummaryPlate(plates.get(EnumArmorFacing.FRONT)) + " / "
            + formatSummaryPlate(side) + " / "
            + formatSummaryPlate(plates.get(EnumArmorFacing.REAR)) + "\u00a0mm";
    }

    private static String formatSummaryPlate(ArmorPlate plate)
    {
        if (plate == null)
            return "—";
        String thickness = IFlanItem.formatFloat(plate.thicknessMm(), 1);
        return plate.slopeDeg() == 0F ? thickness
            : thickness + " (" + IFlanItem.formatFloat(plate.slopeDeg(), 1) + "°)";
    }

    // ------------------------------------------------------- real-world path

    private static void appendProfile(DriveableType type, ResolvedVehiclePhysics resolved, double speedScale,
                                      double aircraftReferenceSpeedScale, List<Component> tooltip)
    {
        RealWorldVehicleSpec source = resolved.source();

        // Most lines are authored real-world source values. Minimum flight speed
        // is the deliberate exception because pilots need the effective,
        // server-configured threshold during ordinary play.
        tooltip.add(IFlanItem.statLine(label("mass"), formatMass(resolved.massKg())));
        if (resolved.baselineThrustKn() > 0F)
            tooltip.add(IFlanItem.statLine(label("engineThrust"), IFlanItem.formatFloat(resolved.baselineThrustKn(), 1) + " kN"));
        if (resolved.baselinePowerKw() > 0F)
            tooltip.add(IFlanItem.statLine(label("enginePower"), IFlanItem.formatFloat(resolved.baselinePowerKw(), 0) + " kW"));
        tooltip.add(IFlanItem.statLine(label("maxSpeed"), IFlanItem.formatFloat(resolved.maxSpeedKmh(), 0) + " km/h"));

        if (resolved.hasAircraftProfile())
        {
            RealWorldVehicleSpec.Aircraft aircraft = source.aircraft();
            if (aircraft.wingSpanM() != null)
                tooltip.add(IFlanItem.statLine(label("wingSpan"), IFlanItem.formatFloat(aircraft.wingSpanM(), 2) + " m"));
            if (aircraft.wingAreaM2() != null)
                tooltip.add(IFlanItem.statLine(label("wingArea"), IFlanItem.formatFloat(aircraft.wingAreaM2(), 2) + " m²"));
            if (aircraft.climbRateMs() != null)
                tooltip.add(IFlanItem.statLine(label("climbRate"), IFlanItem.formatFloat(aircraft.climbRateMs(), 1) + " m/s"));
            double minimumFlightSpeedKmh = resolved.referenceSpeedMs(
                speedScale, aircraftReferenceSpeedScale) * VehiclePhysicsUnits.KMH_PER_METRE_PER_SECOND;
            tooltip.add(IFlanItem.statLine(label("referenceSpeed"),
                IFlanItem.formatDouble(minimumFlightSpeedKmh, 0) + " km/h"));
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
                tooltip.add(IFlanItem.statLine(label("legacy.steeringTrim"),
                    IFlanItem.formatFloat(vehicle.getTurnLeftModifier(), 2) + " / "
                        + IFlanItem.formatFloat(vehicle.getTurnRightModifier(), 2)));
        }
        else if (type instanceof PlaneType plane)
        {
            tooltip.add(IFlanItem.statLine(label("legacy.controlTrim"),
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

    private static void appendIndependentOverrides(ResolvedVehiclePhysics resolved, List<Component> tooltip)
    {
        if (resolved.category() == EnumVehicleCategory.GROUND)
        {
            tooltip.add(IFlanItem.statLine(label("driveType"),
                translate("drivetype." + resolved.driveType().name().toLowerCase(Locale.ROOT))
                    + (resolved.driveTypeExplicit() ? "" : " " + translate("inferred"))));
        }
        if (resolved.hasReverseSpeedOverride())
            tooltip.add(IFlanItem.statLine(label("reverseSpeed"),
                IFlanItem.formatFloat(resolved.maxReverseSpeedKmh(), 0) + " km/h"));
        if (resolved.hasDraft())
            tooltip.add(IFlanItem.statLine(label("draft"), IFlanItem.formatFloat(resolved.draftM(), 2) + " m"));
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

    /**
     * Masses above 1 t read easier in tons; lighter driveables stay in kg.
     */
    private static String formatMass(float massKg)
    {
        return massKg > 1000F
            ? IFlanItem.formatFloat(massKg / 1000F, 1) + " t"
            : IFlanItem.formatFloat(massKg, 0) + " kg";
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
