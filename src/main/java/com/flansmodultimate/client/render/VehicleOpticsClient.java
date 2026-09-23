package com.flansmodultimate.client.render;

import com.flansmodultimate.client.gui.OpticsHudEditorScreen;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ResourceUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Client presentation of the local occupant's synchronized scope. Never changes saved sensitivity. */
public final class VehicleOpticsClient
{
    private static Seat viewedSeat;
    private static CameraType previousCamera;

    private VehicleOpticsClient() {}

    @Nullable
    public static Seat activeSeat()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isAlive() && (mc.screen == null || mc.screen instanceof OpticsHudEditorScreen)
            && mc.player.getVehicle() instanceof Seat seat && seat.isAlive() && seat.isScoped()
            && seat.getDriveable() != null && seat.getDriveable().isAlive() && seat.getOptics() != null ? seat : null;
    }

    public static void tick()
    {
        Minecraft mc = Minecraft.getInstance();
        Seat seat = activeSeat();
        if (viewedSeat != seat)
        {
            reset();
            if (seat != null)
            {
                viewedSeat = seat;
                previousCamera = mc.options.getCameraType();
            }
        }
        if (seat != null)
            mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    public static void reset()
    {
        if (previousCamera != null)
            Minecraft.getInstance().options.setCameraType(previousCamera);
        previousCamera = null;
        viewedSeat = null;
        VehicleOpticsHud.resetSession();
    }

    public static float zoom()
    {
        Seat seat = activeSeat();
        return seat == null ? 1F : seat.getScopeZoom();
    }

    public static boolean nightVision()
    {
        Seat seat = activeSeat();
        return seat != null && (seat.isNightSightActive() || seat.isThermalScoped());
    }

    public static boolean thermal()
    {
        Seat seat = activeSeat();
        return seat != null && seat.isThermalScoped();
    }

    @Nullable
    public static ResourceLocation texture(Seat seat, String name)
    {
        if (name == null || name.isBlank() || name.equalsIgnoreCase("none")) return null;
        return InfoType.loadOverlay(ResourceUtils.sanitize(name), seat.getDriveable().getConfigType()).orElse(null);
    }
}
