package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = FlansMod.MOD_ID)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ApocalypseContent.survivor.get(), SurvivorEntity.createAttributes().build());
        event.put(ApocalypseContent.skullDrone.get(), SkullDroneEntity.createAttributes().build());
        event.put(ApocalypseContent.skullBoss.get(), SkullBossEntity.createAttributes().build());
    }
}
