package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.event.GunFiredEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

/** NeoForge boundaries for the server-owned opt-in Teams rules. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class RoundRulesEventHandler
{
    private static RoundRuleRuntime runtime()
    {
        return TeamsManager.getInstance().getRoundRuleRuntime();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingIncomingDamageEvent event)
    {
        if (!event.getEntity().level().isClientSide && runtime().spares(event.getEntity(), event.getSource()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fired(GunFiredEvent event)
    {
        Entity shooter = event.getShooter();
        if (!shooter.level().isClientSide && (runtime().blocksCombat(shooter)
            || shooter instanceof LivingEntity living && (runtime().blocksItemUse(living, living.getMainHandItem()) || runtime().blocksItemUse(living, living.getOffhandItem()))))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void mounted(EntityMountEvent event)
    {
        if (!event.isMounting() || event.getEntityMounting().level().isClientSide)
            return;
        Entity rider = event.getEntityMounting();
        Entity vehicle = RoundRuleRuntime.vehicle(event.getEntityBeingMounted());
        if (!runtime().mayMount(rider) || vehicle != null && !runtime().getVehicles().prepare(vehicle, TeamsCombatants.getPlayingTeam(TeamsManager.getInstance(), rider)))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event)
    {
        if (!event.getLevel().isClientSide())
            runtime().getVehicles().joined(event.getEntity());
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event)
    {
        if (!event.getLevel().isClientSide())
            runtime().getVehicles().left(event.getEntity());
    }

    /** Runs as the entity starts its tick, where Forge 1.20.1 posts its living tick. */
    @SubscribeEvent
    public static void livingTick(EntityTickEvent.Pre event)
    {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide)
            return;
        if (living instanceof net.minecraft.world.entity.player.Player)
            TeamsHealth.apply(living, TeamsManager.getInstance().getPlayerHealth());
        else
            runtime().enforceHomeEntry(living);
    }

    @SubscribeEvent
    public static void used(LivingEntityUseItemEvent.Start event)
    {
        if (!event.getEntity().level().isClientSide && runtime().blocksItemUse(event.getEntity(), event.getItem()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void attackedWithItem(AttackEntityEvent event)
    {
        if (!event.getEntity().level().isClientSide && (runtime().blocksCombat(event.getEntity()) || runtime().blocksItemUse(event.getEntity(), event.getEntity().getMainHandItem())))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void rightClickedItem(PlayerInteractEvent.RightClickItem event)
    {
        rejectInteraction(event, event, event::setCancellationResult);
    }

    @SubscribeEvent
    public static void rightClickedBlock(PlayerInteractEvent.RightClickBlock event)
    {
        rejectInteraction(event, event, event::setCancellationResult);
    }

    @SubscribeEvent
    public static void rightClickedEntity(PlayerInteractEvent.EntityInteract event)
    {
        rejectInteraction(event, event, event::setCancellationResult);
    }

    // NeoForge declares cancellation and its result on each interaction event rather than on their shared superclass.
    private static void rejectInteraction(PlayerInteractEvent event, ICancellableEvent cancellable, Consumer<InteractionResult> result)
    {
        if (!event.getLevel().isClientSide && runtime().blocksItemUse(event.getEntity(), event.getItemStack()))
        {
            result.accept(InteractionResult.FAIL);
            cancellable.setCanceled(true);
        }
    }
}
