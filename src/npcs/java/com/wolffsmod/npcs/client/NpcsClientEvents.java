package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import noppes.npcs.CustomEntities;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NpcsClientEvents
{
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        for (FlanModelEntityType entityType : FlanModelEntities.getEntityTypes())
            event.registerEntityRenderer(entityType, context -> new FlanModelRenderer(context, entityType));

        // This module loads after Custom NPCs and replaces only its 64x32 NPC renderer.
        event.registerEntityRenderer(CustomEntities.entityNPC64x32,
            context -> {
                NpcsMod.log.info("Creating Custom NPCs 64x32 renderer with legacy UV layout");
                return new LegacyNpc64x32Renderer(context);
            });
    }
}
