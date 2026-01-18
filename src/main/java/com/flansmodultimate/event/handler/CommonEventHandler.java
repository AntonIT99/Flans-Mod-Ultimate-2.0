package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.config.ModCommonConfigs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.BooleanUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
    @Getter
    private static long ticker;
    @Getter
    private static final Set<UUID> nightVisionPlayers = new HashSet<>();
    private static final Map<UUID, Integer> regenTimers = new HashMap<>();

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
            if (regenTimer >= ModCommonConfigs.bonusRegenTickDelay.get())
            {
                if (player.getFoodData().getFoodLevel() >= ModCommonConfigs.bonusRegenFoodLimit.get())
                    player.heal(ModCommonConfigs.bonusRegenAmount.get());
                regenTimers.put(player.getUUID(), 0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        regenTimers.remove(event.getEntity().getUUID());
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
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity.level().isClientSide)
            return;

        if (event.getEntity() instanceof Player || event.getEntity() instanceof Mob)
        {
            if (BooleanUtils.isTrue(ModCommonConfigs.enableOldArmorRatioSystem.get()))
                CustomArmorItem.applyOldArmorRatioSystem(event, entity);

            if (FlanDamageSources.isShootableDamage(source))
            {
                if (CustomArmorItem.tryApplyIgnoreArmorShot(event, entity, source))
                    return;

                CustomArmorItem.applyArmorBulletDefense(event, entity);
            }
        }
    }
}
