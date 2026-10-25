package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.NameTagRanges;
import com.flansmodultimate.api.client.equipment.FlansEquipmentRender;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.client.centre.CommandCentrePages;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.NpcSkins;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityNPCInterface;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = NpcsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NpcsClientEvents
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            FlansEquipmentRender.addArmorPoseStep(NpcArmorParts::apply);
            NpcSkins.setClientResolver(NpcSkinResolver::apply);
            CommandCentrePages.register();
            NameTagRanges.setResolver((entity, configured) -> entity instanceof EntityNPCInterface ? ClientServerSettings.get(Setting.NAME_TAG_RANGE) : configured);
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
