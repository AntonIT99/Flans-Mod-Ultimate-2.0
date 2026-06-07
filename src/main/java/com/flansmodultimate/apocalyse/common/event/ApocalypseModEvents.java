package com.flansmodultimate.apocalyse.common.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApocalypseModEvents
{
    private ApocalypseModEvents()
    {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ApocalypseContent.SURVIVOR.get(), SurvivorEntity.createAttributes().build());
        event.put(ApocalypseContent.SKULL_DRONE.get(), SkullDroneEntity.createAttributes().build());
        event.put(ApocalypseContent.SKULL_BOSS.get(), SkullBossEntity.createAttributes().build());
    }
}
