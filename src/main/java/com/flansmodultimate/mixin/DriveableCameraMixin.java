package com.flansmodultimate.mixin;

import com.flansmodultimate.client.input.KeyInputHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Changes only Camera.setup's requested third-person distance. Camera still
 * passes this value through its vanilla getMaxZoom collision probes.
 */
@Mixin(Camera.class)
public abstract class DriveableCameraMixin
{
    @ModifyConstant(method = "setup", constant = @Constant(doubleValue = 4.0D))
    private double flansmodultimate$driveableCameraDistance(double vanillaDistance)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return vanillaDistance;

        var controllable = KeyInputHandler.resolveControllable(player);
        if (controllable == null)
            return vanillaDistance;

        float requestedDistance = controllable.getCameraDistance();
        return Float.isFinite(requestedDistance)
            ? Mth.clamp(requestedDistance, 1F, 64F)
            : vanillaDistance;
    }
}
