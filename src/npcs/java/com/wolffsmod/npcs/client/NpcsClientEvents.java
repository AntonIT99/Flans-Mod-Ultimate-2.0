package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.FlansEquipmentRender;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.NpcSkins;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import noppes.npcs.CustomEntities;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = NpcsMod.MOD_ID, value = Dist.CLIENT)
public final class NpcsClientEvents
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            FlansEquipmentRender.addArmorPoseStep(NpcArmorParts::apply);
            NpcSkins.setClientResolver(NpcSkinResolver::apply);
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(BotKeys.SQUAD_ORDERS);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        for (FlanModelEntityType entityType : FlanModelEntities.getEntityTypes())
            event.registerEntityRenderer(entityType, context -> new FlanModelRenderer(context, entityType));

        // This module loads after Custom NPCs and replaces only its 64x32 NPC renderer.
        event.registerEntityRenderer(CustomEntities.entityNPC64x32, context ->
        {
            NpcsMod.log.info("Creating Custom NPCs 64x32 renderer with legacy UV layout");
            return new LegacyNpc64x32Renderer(context);
        });
    }
}
