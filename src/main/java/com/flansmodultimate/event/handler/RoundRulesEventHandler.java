package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.event.GunFiredEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Forge boundaries for the server-owned opt-in Teams rules. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoundRulesEventHandler
{
    private static RoundRuleRuntime runtime()
    {
        return TeamsManager.getInstance().getRoundRuleRuntime();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event)
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

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event)
    {
        if (!event.getEntity().level().isClientSide && !(event.getEntity() instanceof net.minecraft.world.entity.player.Player))
            runtime().enforceHomeEntry(event.getEntity());
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
        rejectInteraction(event);
    }

    @SubscribeEvent
    public static void rightClickedBlock(PlayerInteractEvent.RightClickBlock event)
    {
        rejectInteraction(event);
    }

    @SubscribeEvent
    public static void rightClickedEntity(PlayerInteractEvent.EntityInteract event)
    {
        rejectInteraction(event);
    }

    private static void rejectInteraction(PlayerInteractEvent event)
    {
        if (!event.getLevel().isClientSide && runtime().blocksItemUse(event.getEntity(), event.getItemStack()))
        {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
}
