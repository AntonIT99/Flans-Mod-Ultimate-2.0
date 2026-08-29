package com.flansmodultimate.common.command;

import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.RealWorldSpecReader;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import com.flansmodultimate.common.driveables.physics.VehicleGeometry;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Developer introspection for the real-world physics system.
 *
 * <p>Answers the question the mode enum alone cannot: why is this vehicle on
 * {@code LEGACY} rather than {@code REAL_WORLD_PROFILE}. It prints the authored
 * source values, what was missing, the resolved values, the active mode and the
 * effective speed scale, all read from the same resolver the simulation uses.
 *
 * <p>Deliberately a command rather than logging, so normal play stays quiet.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class VehiclePhysicsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("vehiclephysics")
            .requires(source -> source.hasPermission(2))
            .executes(VehiclePhysicsCommand::report));
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        DriveableType type = findType(player);
        if (type == null)
        {
            context.getSource().sendFailure(Component.literal(
                "Ride a driveable or hold a driveable item to inspect its physics"));
            return 0;
        }

        double speedScale = ModCommonConfig.realisticVehicleSpeedScale();
        ResolvedVehiclePhysics resolved = type.getResolvedPhysics();
        RealWorldVehicleSpec source = type.getRealWorldSpec();

        send(context, ChatFormatting.GOLD, "=== " + type.getShortName() + " physics ===");
        send(context, ChatFormatting.WHITE, "mode: " + resolved.mode()
            + "  category: " + resolved.category()
            + "  speedScale: " + format(speedScale));

        send(context, ChatFormatting.AQUA, "-- authored source --");
        if (source.isEmpty())
            send(context, ChatFormatting.GRAY, "  (none; this definition declares no Real* key)");
        else
        {
            line(context, RealWorldSpecReader.KEY_MASS, source.massKg(), "kg");
            line(context, RealWorldSpecReader.KEY_MAX_SPEED, source.maxSpeedKmh(), "km/h");
            line(context, RealWorldSpecReader.KEY_ENGINE_POWER, source.enginePowerKw(), "kW");
            line(context, RealWorldSpecReader.KEY_ENGINE_THRUST, source.engineThrustKn(), "kN");
            line(context, RealWorldSpecReader.KEY_WING_SPAN, source.aircraft().wingSpanM(), "m");
            line(context, RealWorldSpecReader.KEY_WING_AREA, source.aircraft().wingAreaM2(), "m2");
            line(context, RealWorldSpecReader.KEY_CLIMB_RATE, source.aircraft().climbRateMs(), "m/s");
            if (source.ground().driveType() != null)
                send(context, ChatFormatting.GRAY, "  " + RealWorldSpecReader.KEY_DRIVE_TYPE
                    + " = " + source.ground().driveType());
            line(context, RealWorldSpecReader.KEY_MAX_REVERSE_SPEED, source.ground().maxReverseSpeedKmh(), "km/h");
            line(context, RealWorldSpecReader.KEY_MAX_SLOPE, source.ground().maxSlopeDeg(), "deg");
            line(context, RealWorldSpecReader.KEY_DRAFT, source.marine().draftM(), "m");
        }

        send(context, ChatFormatting.AQUA, "-- completeness --");
        send(context, ChatFormatting.GRAY, "  ground profile: " + resolved.groundProfileComplete()
            + explainGround(source, resolved));
        send(context, ChatFormatting.GRAY, "  aircraft profile: " + resolved.aircraftProfileComplete()
            + explainAircraft(source, resolved));

        send(context, ChatFormatting.AQUA, "-- resolved --");
        if (resolved.hasGroundPropulsion() || resolved.hasAircraftProfile())
        {
            send(context, ChatFormatting.GRAY, "  mass = " + format(resolved.massKg()) + " kg");
            send(context, ChatFormatting.GRAY, "  maxSpeed = " + format(resolved.maxSpeedKmh()) + " km/h -> "
                + format(resolved.maxSpeedBlocksPerTick(speedScale)) + " blocks/tick");
            send(context, ChatFormatting.GRAY, "  powerToWeight = " + format(resolved.powerToWeightKwPerKg()) + " kW/kg");
            send(context, ChatFormatting.GRAY, "  thrustToWeight = " + format(resolved.thrustToWeight()));
            if (resolved.hasAircraftProfile())
            {
                send(context, ChatFormatting.GRAY, "  wingLoading = " + format(resolved.wingLoadingKgPerM2()) + " kg/m2");
                send(context, ChatFormatting.GRAY, "  referenceSpeed = " + format(resolved.referenceSpeedMs(speedScale)) + " m/s");
                send(context, ChatFormatting.GRAY, "  rollInertiaFactor = " + format(resolved.rollInertiaFactor()));
            }
        }
        else
        {
            send(context, ChatFormatting.GRAY, "  propulsion falls back to legacy: "
                + legacyPropulsionSource(type));
        }
        send(context, ChatFormatting.GRAY, "  driveType = " + resolved.driveType()
            + (resolved.driveTypeExplicit() ? " (explicit)" : " (inferred from Tank/FourWheelDrive)"));
        send(context, ChatFormatting.GRAY, "  reverseOverride = " + (resolved.hasReverseSpeedOverride()
            ? format(resolved.reverseSpeedBlocksPerTick(speedScale)) + " blocks/tick"
            : "none (legacy MaxNegativeThrottle " + format(type.getMaxNegativeThrottle()) + ")"));
        send(context, ChatFormatting.GRAY, "  slopeLimit = "
            + (resolved.hasSlopeLimit() ? format(resolved.maxSlopeDeg()) + " deg" : "none"));
        send(context, ChatFormatting.GRAY, "  draft = "
            + (resolved.hasDraft() ? format(resolved.draftM()) + " m" : "none (legacy Buoyancy "
                + format(type.getBuoyancy()) + ")"));
        send(context, ChatFormatting.GRAY, "  movementClamp = " + format(resolved.movementClampBlocksPerTick())
            + " blocks/tick, wheelProbe = " + format(resolved.wheelPredictionBlocks(speedScale)) + " blocks");

        VehicleGeometry geometry = resolved.geometry();
        send(context, ChatFormatting.AQUA, "-- derived geometry --");
        send(context, ChatFormatting.GRAY, "  length = " + format(geometry.lengthM())
            + " m, width = " + format(geometry.widthM())
            + " m, height = " + format(geometry.heightM()) + " m");
        send(context, ChatFormatting.GRAY, "  wheelbase = " + format(geometry.wheelbaseM())
            + " m, track = " + format(geometry.trackWidthM()) + " m");
        return 1;
    }

    private static String explainGround(RealWorldVehicleSpec source, ResolvedVehiclePhysics resolved)
    {
        if (resolved.groundProfileComplete())
            return "";
        if (resolved.category() != EnumVehicleCategory.GROUND)
            return " (not a ground vehicle)";
        StringBuilder missing = new StringBuilder();
        appendMissing(missing, source.massKg(), RealWorldSpecReader.KEY_MASS);
        appendMissing(missing, source.enginePowerKw(), RealWorldSpecReader.KEY_ENGINE_POWER);
        appendMissing(missing, source.maxSpeedKmh(), RealWorldSpecReader.KEY_MAX_SPEED);
        return missing.isEmpty() ? "" : " missing: " + missing;
    }

    private static String explainAircraft(RealWorldVehicleSpec source, ResolvedVehiclePhysics resolved)
    {
        if (resolved.aircraftProfileComplete())
            return "";
        if (resolved.category() != EnumVehicleCategory.AIRCRAFT)
            return " (not an aircraft)";
        StringBuilder missing = new StringBuilder();
        appendMissing(missing, source.massKg(), RealWorldSpecReader.KEY_MASS);
        appendMissing(missing, source.maxSpeedKmh(), RealWorldSpecReader.KEY_MAX_SPEED);
        appendMissing(missing, source.aircraft().wingSpanM(), RealWorldSpecReader.KEY_WING_SPAN);
        appendMissing(missing, source.aircraft().wingAreaM2(), RealWorldSpecReader.KEY_WING_AREA);
        if (source.enginePowerKw() == null && source.engineThrustKn() == null)
            append(missing, RealWorldSpecReader.KEY_ENGINE_POWER + " or " + RealWorldSpecReader.KEY_ENGINE_THRUST);
        return missing.isEmpty() ? "" : " missing: " + missing;
    }

    private static String legacyPropulsionSource(DriveableType type)
    {
        if (type instanceof VehicleType vehicle)
            return vehicle.isUseRealisticAcceleration()
                ? "UseRealisticAcceleration (legacy Mass " + format(vehicle.getMass()) + ")"
                : "fixed 0.01 acceleration, MaxThrottle " + format(type.getMaxThrottle());
        if (type instanceof PlaneType plane)
            return plane.isNewFlightControl()
                ? "NewFlightControl (legacy Mass " + format(plane.getMass())
                    + ", MaxThrust " + format(plane.getMaxThrust()) + ")"
                : "legacy flight model, MaxThrottle " + format(type.getMaxThrottle());
        return "legacy";
    }

    private static void appendMissing(StringBuilder builder, @Nullable Float value, String key)
    {
        if (value == null)
            append(builder, key);
    }

    private static void append(StringBuilder builder, String key)
    {
        if (!builder.isEmpty())
            builder.append(", ");
        builder.append(key);
    }

    private static void line(CommandContext<CommandSourceStack> context, String key, @Nullable Float value, String unit)
    {
        if (value != null)
            send(context, ChatFormatting.GRAY, "  " + key + " = " + format(value) + " " + unit);
    }

    private static void send(CommandContext<CommandSourceStack> context, ChatFormatting color, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    private static String format(@Nullable Number value)
    {
        if (value == null)
            return "n/a";
        double number = value.doubleValue();
        if (!Double.isFinite(number))
            return "n/a";
        return String.format(Locale.ROOT, "%.4g", number);
    }

    @Nullable
    private static DriveableType findType(ServerPlayer player)
    {
        Entity vehicle = player.getVehicle();
        if (vehicle != null)
        {
            Driveable driveable = vehicle instanceof Driveable direct ? direct
                : vehicle.getVehicle() instanceof Driveable parent ? parent : null;
            if (driveable != null && driveable.getConfigType() != null)
                return driveable.getConfigType();
        }
        DriveableType held = typeOf(player.getItemInHand(InteractionHand.MAIN_HAND));
        return held != null ? held : typeOf(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    @Nullable
    private static DriveableType typeOf(ItemStack stack)
    {
        return stack.getItem() instanceof DriveableItem<?, ?> item ? item.getConfigType() : null;
    }
}
