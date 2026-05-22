package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@net.neoforged.fml.common.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.GAME)
public final class CommonEventHandler
{
    @Getter
    private static long ticker;
    @Getter
    private static final Set<UUID> nightVisionPlayers = new HashSet<>();
    private static final Map<UUID, Integer> regenTimers = new HashMap<>();

    private static final Int2ObjectMap<Pair<Vec3, Vec3>> PREVIOUS_POS = new Int2ObjectOpenHashMap<>();

    /**
     * This is the only way to know if the entity is moving,
     * because server resets all the information about previous pos and movement after tick ends
     */
    public static Vec3 getPrevPos(LivingEntity entity) {
        return PREVIOUS_POS.get(entity.getId()).left();
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
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (ticker == Long.MAX_VALUE)
            ticker = 0;
        else
            ticker++;

        MinecraftServer server = event.getServer();
        if (server == null)
            return;

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
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e)
    {
        if (e.getEntity() instanceof ServerPlayer sp)
        {
            PacketHandler.sendTo(new PacketSyncCommonConfig(ModCommonConfig.get()), sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        ModCommonConfig.clearServerOverride();
        regenTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event)
    {
        if (!(event.getEntity() instanceof LivingEntity livingEntity))
            return;

        if (livingEntity.level().isClientSide)
            return;

        if (livingEntity instanceof Player || livingEntity instanceof Mob)
        {
            CustomArmorItem.handleSpecialEffects(livingEntity);
            CustomArmorItem.handleMobEffects(livingEntity);
        }

        Pair<Vec3, Vec3> posPair = PREVIOUS_POS.get(livingEntity.getId());
        Vec3 current = livingEntity.position();
        Vec3 prev;
        if (posPair != null) {
            prev = posPair.right();
        } else {
            prev = current;
        }
        PREVIOUS_POS.put(livingEntity.getId(), Pair.of(prev, current));
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
    public static void onLivingAttack(LivingIncomingDamageEvent event)
    {
        LivingEntity entity = event.getEntity();
        if (entity.getVehicle() instanceof Driveable || entity.getVehicle() instanceof Seat)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event)
    {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity.level().isClientSide)
            return;

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
            if (ModCommonConfig.get().enableOldArmorRatioSystem())
                CustomArmorItem.applyOldArmorRatioSystem(event, entity);

            if (FlanDamageSources.isShootableDamage(source))
            {
                if (CustomArmorItem.tryApplyIgnoreArmorShot(event, entity, source))
                    return;

                CustomArmorItem.applyArmorBulletDefense(event, entity);
            }
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
        if (entity instanceof Player player)
            PlayerData.getInstance(player).playerKilled();
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            PREVIOUS_POS.remove(entity.getId());
        }
    }
}