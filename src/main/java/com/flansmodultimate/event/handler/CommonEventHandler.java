package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.command.DefaultAmmoCommand;
import com.flansmodultimate.common.command.DigitalAmmoCommand;
import com.flansmodultimate.common.command.FMParticleCommand;
import com.flansmodultimate.common.command.TeamsCommand;
import com.flansmodultimate.common.command.VehiclePhysicsCommand;
import com.flansmodultimate.common.digitalammo.DigitalAmmoSupplyHandler;
import com.flansmodultimate.common.enchantments.EnchantmentModule;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEventHandler
{
    private static final Set<ResourceLocation> FLANS_LOOT_TABLES = Set.of(
        BuiltInLootTables.ABANDONED_MINESHAFT,
        BuiltInLootTables.VILLAGE_WEAPONSMITH,
        BuiltInLootTables.END_CITY_TREASURE,
        BuiltInLootTables.NETHER_BRIDGE,
        BuiltInLootTables.DESERT_PYRAMID,
        ResourceLocation.fromNamespaceAndPath("lostcities", "chests/lostcitychest"),
        ResourceLocation.fromNamespaceAndPath("lostcities", "chests/raildungeonchest")
    );

    @Getter
    private static long ticker;
    @Getter
    private static final Set<UUID> nightVisionPlayers = new HashSet<>();
    private static final Map<UUID, Integer> regenTimers = new HashMap<>();
    private static boolean contentReferencesValidated;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        DigitalAmmoCommand.register(event.getDispatcher());
        DefaultAmmoCommand.register(event.getDispatcher());
        FMParticleCommand.register(event.getDispatcher());
        TeamsCommand.register(event.getDispatcher());
        VehiclePhysicsCommand.register(event.getDispatcher());
        DigitalAmmoSupplyHandler.reloadSupplyBlocks();
    }

    @SubscribeEvent
    public static void registerLoot(LootTableLoadEvent event)
    {
        if (!FLANS_LOOT_TABLES.contains(event.getName()))
            return;

        InfoType.beginLootTableLoad(event);
        try
        {
            for (InfoType type : InfoType.getInfoTypes().values())
                type.addLoot(event);
        }
        finally
        {
            InfoType.finishLootTableLoad(event);
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event)
    {
        if (event.getLevel().isClientSide())
            return;

        FlansMod.teamsManager.setExplosionsBreakBlocks(ModCommonConfig.get().explosionsBreakBlocks());
        FlansMod.teamsManager.setCanBreakGlass(ModCommonConfig.get().shootablesCanBreakGlass());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        FlansMod.teamsManager.attachServer(event.getServer());
        if (contentReferencesValidated || !ModCommonConfig.get().validateContentReferencesOnWorldLoad())
            return;

        ContentManager.validateContentReferences();
        contentReferencesValidated = true;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        FlansMod.teamsManager.detachServer();
        contentReferencesValidated = false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        if (ticker == Long.MAX_VALUE)
            ticker = 0;
        else
            ticker++;

        MinecraftServer server = event.getServer();
        if (server == null)
            return;

        FlansMod.teamsManager.tick();

        Iterator<UUID> it = nightVisionPlayers.iterator();
        while (it.hasNext())
        {
            UUID uuid = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);

            if (player == null || !shouldKeepNightVision(player))
            {
                if (player != null)
                    player.removeEffect(MobEffects.NIGHT_VISION);
                it.remove();
            }
        }
    }

    private static boolean shouldKeepNightVision(ServerPlayer player)
    {
        ItemStack currentItem = player.getMainHandItem();
        if (currentItem.isEmpty() || !(currentItem.getItem() instanceof GunItem itemGun))
            return false;

        AttachmentType scope = itemGun.getConfigType().getScope(currentItem);
        return itemGun.getConfigType().isAllowNightVision() || (scope != null && scope.isHasNightVision());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        Player player = event.player;
        PlayerData.getInstance(player).tick(player);

        if (!player.level().isClientSide)
        {
            int regenTimer = regenTimers.merge(player.getUUID(), 1, Integer::sum);
            if (regenTimer >= ModCommonConfig.get().bonusRegenTickDelay())
            {
                if (player.getFoodData().getFoodLevel() >= ModCommonConfig.get().bonusRegenFoodLimit())
                    player.heal(ModCommonConfig.get().bonusRegenAmount());
                regenTimers.put(player.getUUID(), 0);
            }

            if (FlansMod.teamsManager.isRoundRunning() && FlansMod.teamsManager.isOverrideHunger())
            {
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20F);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e)
    {
        if (e.getEntity() instanceof ServerPlayer sp)
        {
            ModCommonConfigSync.syncClientIfServer(sp);
            FlansMod.teamsManager.playerLoggedIn(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        ModCommonConfig.clearServerOverride();
        ModApocalypseConfig.clearServerOverride();
        regenTimers.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player)
            FlansMod.teamsManager.playerLoggedOut(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            FlansMod.teamsManager.respawnPlayer(player, false);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        FlansMod.teamsManager.getCurrentGameType().ifPresent(type -> {
            if (!type.canPlayerPickup(FlansMod.teamsManager, player, event.getItem().getItem()))
                event.setCanceled(true);
        });
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity().level().isClientSide)
            return;

        if (event.getEntity() instanceof Player || event.getEntity() instanceof Mob)
        {
            CustomArmorItem.handleSpecialEffects(event.getEntity());
            CustomArmorItem.handleMobEffects(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event)
    {
        if (event.getEntity().level().isClientSide)
            return;

        if (event.getEntity() instanceof Player || event.getEntity() instanceof Mob)
            CustomArmorItem.handleJumpModifier(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.getVehicle() instanceof Driveable || entity.getVehicle() instanceof Seat)
            event.setCanceled(true);

        if (!entity.level().isClientSide && entity instanceof ServerPlayer player)
        {
            FlansMod.teamsManager.getCurrentGameType().ifPresent(type -> {
                if (!type.playerAttacked(player, event.getSource()))
                    event.setCanceled(true);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity.level().isClientSide)
            return;

        EnchantmentModule.applyOffHandWeaponDamage(event);
        EnchantmentModule.applyJuggernaut(event);

        if (entity instanceof Player player)
        {
            float absorption = getShieldAbsorption(player);
            if (absorption > 0F && !FlanDamageSources.isShootableDamage(source) && isAttackFromFront(player, source))
            {
                event.setAmount(event.getAmount() * (1F - absorption));
            }
        }

        if (entity instanceof Player || entity instanceof Mob)
        {
            if (FlanDamageSources.isShootableDamage(source))
            {
                if (CustomArmorItem.tryApplyIgnoreArmorShot(event, entity, source))
                    return;
            }

            CustomArmorItem.applyOldArmorRatioSystem(event, entity);

            if (FlanDamageSources.isShootableDamage(source))
                CustomArmorItem.applyArmorBulletDefense(event, entity);
        }
    }

    private static float getShieldAbsorption(Player player)
    {
        float absorption = 0F;
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            {
                if (gunItem.getConfigType().isShield())
                {
                    absorption = Math.max(absorption, gunItem.getConfigType().getShieldDamageAbsorption());
                }
            }
        }
        return absorption;
    }

    private static boolean isAttackFromFront(Player player, DamageSource source)
    {
        Entity attacker = source.getDirectEntity();
        if (attacker == null)
            attacker = source.getEntity();
        if (attacker == null)
            return true;

        Vec3 playerLook = player.getLookAngle();
        Vec3 toAttacker = player.position().vectorTo(attacker.position()).normalize();
        if (toAttacker.lengthSqr() < 0.001)
            return true;

        double dot = playerLook.dot(toAttacker);
        return dot > 0.0;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player)
            FlansMod.teamsManager.playerDied(player, event.getSource());
        if (entity instanceof Player player)
            PlayerData.getInstance(player).playerKilled();
    }
}
