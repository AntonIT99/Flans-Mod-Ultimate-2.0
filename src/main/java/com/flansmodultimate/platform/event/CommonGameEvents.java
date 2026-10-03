package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.event.handler.CommonEventHandler;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * NeoForge game-bus subscribers for events whose type, phase or accessors differ between loaders.
 * They adapt the event and call the shared handlers. The hurt stage runs from {@code LivingEntityDamageMixin}.
 */
@EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class CommonGameEvents
{
    private CommonGameEvents() {}

    @SubscribeEvent
    public static void onMobFinalizeSpawn(FinalizeSpawnEvent event)
    {
        CommonEventHandler.onMobFinalizeSpawn(event.getEntity(), event.getSpawnType());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        CommonEventHandler.onServerTick(event.getServer());
        com.flansmodultimate.apocalyse.event.handler.CommonEventHandler.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        CommonEventHandler.onPlayerTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event)
    {
        if (event.getPlayer() instanceof ServerPlayer player && !CommonEventHandler.canPickUp(player, event.getItemEntity()))
            event.setCanPickup(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event)
    {
        if (event.getEntity() instanceof LivingEntity living)
            CommonEventHandler.onLivingTick(living);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event)
    {
        if (CommonEventHandler.shouldCancelIncomingDamage(event.getEntity(), event.getSource(), event.getAmount()))
            event.setCanceled(true);
    }

    /**
     * A vanilla shield stopped a hit: lets a Flan's melee weapon play its ShieldHitSound. NeoForge posts this
     * event for every hit and decides the block from {@code getBlocked()} once all listeners ran, so this
     * listens last, including after a cancel, where Forge's {@code ShieldBlockEvent} fires only for a block.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onShieldBlock(LivingShieldBlockEvent event)
    {
        if (event.getBlocked() && !event.getEntity().level().isClientSide)
            CommonEventHandler.playMeleeImpactSound(event.getDamageSource(), true);
    }
}
