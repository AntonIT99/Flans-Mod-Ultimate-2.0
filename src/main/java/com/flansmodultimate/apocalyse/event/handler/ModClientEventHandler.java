package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.client.render.ItemEntityRenderer;
import com.flansmodultimate.apocalyse.client.render.PowerCubeRenderer;
import com.flansmodultimate.apocalyse.client.render.SurvivorRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModClientEventHandler
{
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ApocalypseContent.survivor.get(), SurvivorRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.teleporter.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.BLOCK_POWER_CUBE_ITEM.get()), 2.0F));
        event.registerEntityRenderer(ApocalypseContent.nukeDrop.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.SULPHURIC_ACID_BUCKET.get()), 1.5F));
        event.registerEntityRenderer(ApocalypseContent.skullDrone.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.SKELETON_SKULL), 1.8F));
        event.registerEntityRenderer(ApocalypseContent.skullBoss.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.WITHER_SKELETON_SKULL), 8.0F));
        event.registerBlockEntityRenderer(ApocalypseContent.powerCubeBlockEntity.get(), PowerCubeRenderer::new);
    }
}
