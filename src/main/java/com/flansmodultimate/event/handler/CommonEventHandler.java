package com.flansmodultimate.event.handler;

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
import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.explosions.CraterCarver;
import com.flansmodultimate.common.explosions.ExplosionKillAudit;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.sync.ContentFingerprint;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketContentFingerprint;
import com.flansmodultimate.network.client.PacketKillMessage;
import com.flansmodultimate.platform.damage.MutableDamageContext;
import com.flansmodultimate.platform.world.LootTablePlatform;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEventHandler
{
    private static final Set<ResourceLocation> FLANS_LOOT_TABLES = Set.of(
        LootTablePlatform.id(BuiltInLootTables.ABANDONED_MINESHAFT),
        LootTablePlatform.id(BuiltInLootTables.VILLAGE_WEAPONSMITH),
        LootTablePlatform.id(BuiltInLootTables.END_CITY_TREASURE),
        LootTablePlatform.id(BuiltInLootTables.NETHER_BRIDGE),
        LootTablePlatform.id(BuiltInLootTables.DESERT_PYRAMID),
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

    /** Marks naturally spawned zombies and skeletons that will receive ambient armor. */
    public static void onMobFinalizeSpawn(Mob mob, MobSpawnType spawnType)
    {
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

    /** Runs at the end of every server tick. */
    public static void onServerTick(@Nullable MinecraftServer server)
    {
        if (ticker == Long.MAX_VALUE)
            ticker = 0;
        else
            ticker++;

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

    /** Runs at the end of every player tick, on both sides. */
    public static void onPlayerTick(Player player)
    {
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

    /** Whether the running Teams game type lets the player pick up the stack. */
    public static boolean canPickUp(ServerPlayer player, ItemStack stack)
    {
        return FlansMod.teamsManager.getCurrentGameType()
            .map(type -> type.canPlayerPickup(FlansMod.teamsManager, player, stack))
            .orElse(true);
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

    /** Runs at the end of every living entity tick. */
    public static void onLivingTick(LivingEntity living)
    {
        if (living.level().isClientSide)
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

    /**
     * Whether incoming damage is cancelled before any processing: entities riding Flan vehicles are immune,
     * the Teams game type may reject the attack, and a shield may block a melee hit or a thrown projectile.
     */
    public static boolean shouldCancelIncomingDamage(LivingEntity entity, DamageSource source, float amount)
    {
        boolean cancel = entity.getVehicle() instanceof Driveable || entity.getVehicle() instanceof Seat;

        if (!entity.level().isClientSide && entity instanceof ServerPlayer player
            && FlansMod.teamsManager.getCurrentGameType().map(type -> !type.playerAttacked(player, source)).orElse(false))
            cancel = true;

        if (!cancel && !entity.level().isClientSide && entity instanceof Player player && tryShieldBlock(player, source, amount))
            cancel = true;
        return cancel;
    }

    /**
     * Rolls the {@code ShieldBlockChance} of the best shield held against a hit from the front: a melee hit,
     * vanilla or custom melee alike, or a blockable projectile such as an arrow or a thrown gun. A blocked hit
     * deals no damage at all, but one stronger than the shield's {@code ShieldMaxBlockableMeleeDamage} cannot
     * be blocked.
     */
    private static boolean tryShieldBlock(Player player, DamageSource source, float amount)
    {
        if (!isShieldBlockable(source) || !isAttackFromFront(player, source))
            return false;

        float hitStrength = getShieldBlockHitStrength(source, amount);

        float blockChance = 0F;
        for (InteractionHand hand : InteractionHand.values())
        {
            if (player.getItemInHand(hand).getItem() instanceof GunItem gunItem && gunItem.getConfigType().isShield()
                && hitStrength <= gunItem.getConfigType().getShieldMaxBlockableMeleeDamage())
                blockChance = Math.max(blockChance, gunItem.getConfigType().getShieldBlockChance());
        }

        if (blockChance <= 0F || player.getRandom().nextFloat() >= blockChance)
            return false;

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1F, 0.8F + player.getRandom().nextFloat() * 0.4F);
        return true;
    }

    /**
     * How strong a hit is against {@code ShieldMaxBlockableMeleeDamage}: a Flan's weapon counts with its
     * {@code MeleeDamage}, thrown or swung, other melee with the attacker's attack damage and other
     * projectiles with the damage they deal.
     */
    private static float getShieldBlockHitStrength(DamageSource source, float amount)
    {
        if (source.getDirectEntity() instanceof ThrownGun thrownGun)
            return thrownGun.getGunType() != null ? thrownGun.getGunType().getMeleeDamage(thrownGun.getWeapon(), false) : amount;
        if (!isMeleeDamage(source))
            return amount;

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof GunItem gunItem)
            return gunItem.getConfigType().getMeleeDamage(living.getMainHandItem(), false);
        return attacker instanceof LivingEntity living ? (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE) : amount;
    }

    /** Melee, thrown guns and vanilla projectiles; Flan's bullets and grenades meet the shield hitbox instead */
    private static boolean isShieldBlockable(DamageSource source)
    {
        return isMeleeDamage(source) || source.getDirectEntity() instanceof ThrownGun
            || source.is(DamageTypeTags.IS_PROJECTILE) && !FlanDamageSources.isShootableDamage(source);
    }

    /** A direct hit by a player or mob, or a Flan's custom melee swing */
    private static boolean isMeleeDamage(DamageSource source)
    {
        return source.is(FlanDamageSources.MELEE) || source.is(DamageTypes.PLAYER_ATTACK)
            || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }

    /** Applies Flan damage modifiers once shields and attack cooldown have been handled. */
    public static void applyLivingHurt(MutableDamageContext damage)
    {
        LivingEntity entity = damage.entity();
        DamageSource source = damage.source();

        if (entity.level().isClientSide)
            return;

        EnchantmentModule.applyOffHandWeaponDamage(damage);
        EnchantmentModule.applyJuggernaut(damage);

        if (entity instanceof Player player)
        {
            float absorption = getShieldAbsorption(player);
            // Melee and thrown projectiles are instead blocked outright, or not at all, by ShieldBlockChance
            if (absorption > 0F && !FlanDamageSources.isShootableDamage(source) && !isShieldBlockable(source) && isAttackFromFront(player, source))
            {
                damage.setAmount(damage.amount() * (1F - absorption));
            }
        }

        if (entity instanceof Player || entity instanceof Mob)
        {
            if (FlanDamageSources.isShootableDamage(source) && CustomArmorItem.tryApplyIgnoreArmorShot(damage, entity, source))
                return;

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
        // A projectile has already reached the player, so the side it came from is where it flies from
        Vec3 toAttacker = attacker instanceof Projectile projectile && projectile.getDeltaMovement().lengthSqr() > 1.0E-6
            ? projectile.getDeltaMovement().reverse().normalize()
            : player.position().vectorTo(attacker.position()).normalize();
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
     * The icon shown in the feed. Projectiles know the gun that fired them and thrown guns know themselves;
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
        if (source.getDirectEntity() instanceof ThrownGun thrownGun)
            return thrownGun.getGunType();
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
