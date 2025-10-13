package com.wolffsarmormod.client;

import com.wolffsarmormod.common.item.GunItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AimPoseHandler {

    @SubscribeEvent
    public static void onPre(RenderLivingEvent.Pre<?, ?> event)
    {
        var model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> humanoid)) return;

        ItemStack main = event.getEntity().getMainHandItem();
        ItemStack off  = event.getEntity().getOffhandItem();
        boolean force = isGunItem(main) || isGunItem(off);
        if (!force) return;

        // Force the bow-aiming arm pose on both arms
        humanoid.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        humanoid.leftArmPose  = HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    private static boolean isGunItem(ItemStack s)
    {
        return !s.isEmpty() && s.getItem() instanceof GunItem;
    }
}

