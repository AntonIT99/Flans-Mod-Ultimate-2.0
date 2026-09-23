package com.flansmodultimate.event.handler;
import org.jetbrains.annotations.Nullable;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.KillMessageData;
import com.flansmodultimate.common.AmbientMobArmor;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.command.DefaultAmmoCommand;
import com.flansmodultimate.common.command.DigitalAmmoCommand;
import com.flansmodultimate.common.command.FMParticleCommand;
import com.flansmodultimate.common.command.FlanEntityCommand;
import com.flansmodultimate.common.command.GunAttachmentsCommand;
import com.flansmodultimate.common.command.RearmCommand;
import com.flansmodultimate.common.command.ShootPointDebugCommand;
import com.flansmodultimate.common.command.TeamsCommand;
import com.flansmodultimate.common.command.TryClassCommand;
import com.flansmodultimate.common.command.TryTeamCommand;
import com.flansmodultimate.common.command.VehiclePhysicsCommand;
import com.flansmodultimate.common.digitalammo.DigitalAmmoSupplyHandler;
import com.flansmodultimate.common.enchantments.EnchantmentModule;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.explosions.CraterCarver;
import com.flansmodultimate.common.explosions.ExplosionKillAudit;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.sync.ContentFingerprint;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketContentFingerprint;
import com.flansmodultimate.network.client.PacketKillMessage;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import com.flansmodultimate.platform.damage.MutableDamageContext;
import com.flansmodultimate.platform.neoforge.NeoForgeDamageContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class CommonEventHandler
{
    private static final Set<ResourceLocation> FLANS_LOOT_TABLES = Set.of(
        BuiltInLootTables.ABANDONED_MINESHAFT.location(),
        BuiltInLootTables.VILLAGE_WEAPONSMITH.location(),
        BuiltInLootTables.END_CITY_TREASURE.location(),
        BuiltInLootTables.NETHER_BRIDGE.location(),
        BuiltInLootTables.DESERT_PYRAMID.location(),
        ResourceLocation.fromNamespaceAndPath("lostcities", "chests/lostcitychest"),
        ResourceLocation.fromNamespaceAndPath("lostcities", "chests/raildungeonchest")
    );

    @Getter
    private static long ticker;
    @Getter
    private static final Set<UUID> nightVisionPlayers = new HashSet<>();
    private static final Map<UUID, Integer> regenTimers = new HashMap<>();
    private static final Set<Mob> AMBIENT_ARMOR_SPAWNS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean contentReferencesValidated;

    @SubscribeEvent
    public static void onMobFinalizeSpawn(FinalizeSpawnEvent event)
    {
        Mob mob = event.getEntity();
        MobSpawnType spawnType = event.getSpawnType();
        if (!(mob instanceof Zombie) && !(mob instanceof AbstractSkeleton)
            || spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION)
            return;

        int spawnRate = ModCommonConfig.get().ambientMobArmorSpawnRate();
        if (spawnRate > 0 && mob.getRandom().nextInt(100) < spawnRate)
            AMBIENT_ARMOR_SPAWNS.add(mob);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob && AMBIENT_ARMOR_SPAWNS.remove(mob))
            AmbientMobArmor.equip(mob);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        DigitalAmmoCommand.register(event.getDispatcher());
        DefaultAmmoCommand.register(event.getDispatcher());
        FMParticleCommand.register(event.getDispatcher());
        FlanEntityCommand.register(event.getDispatcher());
        GunAttachmentsCommand.register(event.getDispatcher());
        RearmCommand.register(event.getDispatcher());
        ShootPointDebugCommand.register(event.getDispatcher());
        TeamsCommand.register(event.getDispatcher());
        TryClassCommand.register(event.getDispatcher());
        TryTeamCommand.register(event.getDispatcher());
        VehiclePhysicsCommand.register(event.getDispatcher());
        DigitalAmmoSupplyHandler.reloadSupplyBlocks();
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event)
    {
        ItemStack left = event.getLeft();
        if (left.getItem() instanceof CustomArmorItem armor
            && armor.getEnchantmentValue() == 0
            && !event.getRight().isEmpty())
            event.setCanceled(true);
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
        CraterCarver.clear();
        contentReferencesValidated = false;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (ticker == Long.MAX_VALUE)
            ticker = 0;
        else
            ticker++;

        MinecraftServer server = event.getServer();
        if (server == null)
            return;

        FlansMod.teamsManager.tick();
        CraterCarver.tick();

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
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
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
            // Player data outlives the connection, so a player who disconnected while
            // aiming would come back still aiming until they next raised the sights.
            PlayerData.getInstance(sp).setScoped(false);
            PacketHandler.sendTo(new PacketContentFingerprint(ContentFingerprint.get()), sp);
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
    public static void onItemPickup(ItemEntityPickupEvent.Pre event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;
        FlansMod.teamsManager.getCurrentGameType().ifPresent(type -> {
            if (!type.canPlayerPickup(FlansMod.teamsManager, player, event.getItemEntity().getItem()))
                event.setCanPickup(TriState.FALSE);
        });
    }

    /** Items whose type declares {@code CanDrop False} cannot be tossed out of the inventory. */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event)
    {
        if (!canDrop(event.getEntity().getItem()))
            event.setCanceled(true);
    }

    /** Items whose type declares {@code CanDrop False} are removed from the player's death drops. */
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event)
    {
        if (event.getEntity() instanceof Player)
            event.getDrops().removeIf(item -> !canDrop(item.getItem()));
    }

    private static boolean canDrop(ItemStack stack)
    {
        return !(stack.getItem() instanceof IFlanItem<?> flanItem) || flanItem.getConfigType().isCanDrop();
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide)
            return;

        if (living instanceof Player || living instanceof Mob)
        {
            CustomArmorItem.handleSpecialEffects(living);
            CustomArmorItem.handleMobEffects(living);
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
    public static void onLivingDamage(LivingIncomingDamageEvent event)
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

        if (event.isCanceled())
            return;

        MutableDamageContext damage = new NeoForgeDamageContext(event);
        DamageSource source = damage.source();

        if (entity.level().isClientSide)
            return;

        EnchantmentModule.applyOffHandWeaponDamage(damage);
        EnchantmentModule.applyJuggernaut(damage);

        if (entity instanceof Player player)
        {
            float absorption = getShieldAbsorption(player);
            if (absorption > 0F && !FlanDamageSources.isShootableDamage(source) && isAttackFromFront(player, source))
            {
                damage.setAmount(damage.amount() * (1F - absorption));
            }
        }

        if (entity instanceof Player || entity instanceof Mob)
        {
            if (FlanDamageSources.isShootableDamage(source))
            {
                if (CustomArmorItem.tryApplyIgnoreArmorShot(damage, entity, source))
                    return;
            }

            CustomArmorItem.applyOldArmorRatioSystem(damage, entity);

            if (FlanDamageSources.isShootableDamage(source))
                CustomArmorItem.applyArmorBulletDefense(damage, entity);
        }
    }

    private static float getShieldAbsorption(Player player)
    {
        float absorption = 0F;
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem && gunItem.getConfigType().isShield())
            {
                absorption = Math.max(absorption, gunItem.getConfigType().getShieldDamageAbsorption());
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
        {
            FlansMod.teamsManager.playerDied(player, event.getSource());
            sendKillMessage(player, event.getSource());
            ExplosionKillAudit.logIfApplicable(player, event.getSource());
        }
        else if (!entity.level().isClientSide)
        {
            FlansMod.teamsManager.entityDied(entity, event.getSource());
        }
        if (entity instanceof Player player)
            PlayerData.getInstance(player).playerKilled();
    }

    /** Announces a player killed by another player's Flan's weapon to the kill feed. */
    private static void sendKillMessage(ServerPlayer victim, DamageSource source)
    {
        if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim)
            return;
        InfoType weapon = findKillingWeapon(source, killer);
        if (weapon == null)
            return;

        PacketHandler.sendToDimension(victim.level().dimension(), new PacketKillMessage(new KillMessageData(
            source.is(FlanDamageSources.HEADSHOT), weapon.getOriginalShortName(),
            killer.getGameProfile().getName(), teamColour(killer),
            victim.getGameProfile().getName(), teamColour(victim))));
    }

    /**
     * The icon shown in the feed. Projectiles know the gun that fired them; thrown weapons and
     * melee kills fall back to the weapon the killer is holding.
     */
    @Nullable
    private static InfoType findKillingWeapon(DamageSource source, ServerPlayer killer)
    {
        if (source.getDirectEntity() instanceof Bullet bullet && bullet.getFiredShot() != null
            && bullet.getFiredShot().getFireableGun() != null)
            return bullet.getFiredShot().getFireableGun().getType();
        if (source.getDirectEntity() instanceof Shootable shootable)
            return shootable.getConfigType();
        if (!FlanDamageSources.isShootableDamage(source) && !source.is(FlanDamageSources.MELEE)
            && !source.is(FlanDamageSources.EXPLOSION))
            return null;
        return killer.getMainHandItem().getItem() instanceof GunItem gunItem ? gunItem.getConfigType() : null;
    }

    private static ChatFormatting teamColour(ServerPlayer player)
    {
        Team team = PlayerData.getInstance(player).getTeam();
        return team == null ? ChatFormatting.WHITE : team.getTextColour();
    }
}
