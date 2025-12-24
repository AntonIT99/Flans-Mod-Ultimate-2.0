package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfigs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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

    private static final RandomSource random = RandomSource.create();

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
    public static void onPlayerTick(TickEvent.PlayerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
            return;

        Player player = e.player;
        PlayerData.getInstance(player).tick(player);

        int regenTimer = regenTimers.getOrDefault(player.getUUID(), 0);
        if (regenTimer >= ModCommonConfigs.bonusRegenTickDelay.get())
        {
            if (player.getFoodData().getFoodLevel() >= ModCommonConfigs.bonusRegenFoodLimit.get())
                player.heal(ModCommonConfigs.bonusRegenAmount.get());
            regenTimer = 0;
        }

        regenTimers.put(player.getUUID(), regenTimer + 1);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity.level().isClientSide)
            return;

        if (ModCommonConfigs.enableOldArmorRatioSystem.get())
            applyOldArmorRatioSystem(event, entity);

        if (FlanDamageSources.isShootableDamage(source))
        {
            if (tryApplyIgnoreArmorShot(event, entity, source))
                return;

            applyArmorBulletDefense(event, entity);
        }
    }

    private static void applyOldArmorRatioSystem(LivingHurtEvent event, LivingEntity entity)
    {
        float incoming = event.getAmount();
        if (incoming <= 0F)
            return;

        double rSum = 0.0;

        for (ItemStack stack : entity.getArmorSlots())
        {
            if (stack.getItem() instanceof CustomArmorItem armorItem)
            {
                double r = armorItem.getConfigType().getDefence();
                if (r > 0.0)
                    rSum += r;
            }
        }

        if (rSum <= 0.0)
            return;

        // Clamp so you never “heal” damage by accident
        rSum = Math.min(1.0, Math.max(0.0, rSum));

        float absorbed = (float) (incoming * rSum);
        float remaining = incoming - absorbed;

        event.setAmount(remaining);
    }

    private static boolean tryApplyIgnoreArmorShot(LivingHurtEvent event, LivingEntity entity, DamageSource source)
    {
        float damage = event.getAmount();
        if (damage <= 0.0F)
            return false;

        ShootableType shootableType = FlanDamageSources.getShootableTypeFromSource(source).orElse(null);
        if (shootableType == null)
            return false;

        // No ignore-armor behavior configured
        if (shootableType.getIgnoreArmorProbability() <= 0.0F)
            return false;

        // Random roll failed → fall back to normal armor handling
        if (random.nextFloat() >= shootableType.getIgnoreArmorProbability())
            return false;

        float originalDamage = damage;

        // Strip absorption hearts first
        float absorption = entity.getAbsorptionAmount();
        damage = Math.max(damage - absorption, 0.0F);
        entity.setAbsorptionAmount(absorption - (originalDamage - damage));

        // Apply ignore-armor damage multiplier
        damage *= shootableType.getIgnoreArmorDamageFactor();

        if (damage > 0.0F)
        {
            float health = entity.getHealth();

            // Directly hurt the entity (armor is 100% bypassed here)
            entity.setHealth(health - damage);

            // Update combat tracker (for death messages, stats, etc.)
            entity.getCombatTracker().recordDamage(source, damage);

            // Optionally adjust absorption again (mirroring the old logic)
            entity.setAbsorptionAmount(entity.getAbsorptionAmount() - damage);
        }

        //  Cancel the event so vanilla damage and your armor scaling don't run
        event.setCanceled(true);
        return true;
    }

    private static void applyArmorBulletDefense(LivingHurtEvent event, LivingEntity entity)
    {
        float totalNormalDef = 0.0F;
        float totalBulletDef = 0.0F;

        // Sum up defences from all 4 armor slots
        for (ItemStack stack : entity.getArmorSlots())
        {
            if (!(stack.getItem() instanceof CustomArmorItem armorItem))
                continue;
            totalNormalDef += (float) armorItem.getConfigType().getDefence();
            totalBulletDef += (float) armorItem.getConfigType().getBulletDefence();
        }

        totalNormalDef = Mth.clamp(totalNormalDef, 0F, 1F);
        totalBulletDef = Mth.clamp(totalBulletDef, 0F, 1F);

        float current = event.getAmount();
        float denom = 1.0F - totalNormalDef;
        float target = 1.0F - totalBulletDef;

        // If denom is 0, normal defence would be "100%". Just fall back to using bulletDef directly.
        float factor = (denom <= 0.0001F) ? target : (target / denom);

        float finalDamage = current * factor;
        if (finalDamage < 0.0F)
            finalDamage = 0.0F;

        event.setAmount(finalDamage);
    }
}
