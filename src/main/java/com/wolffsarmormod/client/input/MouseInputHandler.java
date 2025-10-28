package com.wolffsarmormod.client.input;

import com.flansmod.api.IControllable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MouseInputHandler
{
    @OnlyIn(Dist.CLIENT)
    public static void handleMouseMove(double dx, double dy)
    {
        if (Minecraft.getInstance().screen != null)
            return;

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof IControllable controllable)
        {
            controllable.onMouseMoved(dx, dy);
        }
    }
}
