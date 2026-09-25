package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.platform.forge.ForgeDamageContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerPlayer;

/**
 * Forge game-bus subscribers for events whose type, phase or accessors differ between loaders.
 * They adapt the event and call the shared handlers.
 */
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonGameEvents
{
    private CommonGameEvents() {}

    @SubscribeEvent
    public static void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event)
    {
        CommonEventHandler.onMobFinalizeSpawn(event.getEntity(), event.getSpawnType());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        CommonEventHandler.onServerTick(event.getServer());
        com.flansmodultimate.apocalyse.event.handler.CommonEventHandler.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            CommonEventHandler.onPlayerTick(event.player);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && !CommonEventHandler.canPickUp(player, event.getItem().getItem()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        CommonEventHandler.onLivingTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
        if (CommonEventHandler.shouldCancelIncomingDamage(event.getEntity(), event.getSource(), event.getAmount()))
            event.setCanceled(true);
    }

    /** Forge fires this after shields and attack cooldown, before armor; 1.21.1 uses a mixin at that point. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        CommonEventHandler.applyLivingHurt(new ForgeDamageContext(event));
    }
}
