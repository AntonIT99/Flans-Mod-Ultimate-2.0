package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlansDamageSources;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.ShootableType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEventHandler
{
    @Getter
    private static long ticker;
    private static final RandomSource random = RandomSource.create();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            ticker++;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END)
            return;

        Player p = e.player;

        if (!p.level().isClientSide)
            PlayerData.getInstance(p).serverTick(p);
        else
            PlayerData.getInstance(p).clientTick(p);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        // server side only
        if (entity.level().isClientSide || !FlansDamageSources.isShootableDamage(source))
            return;

        if (tryApplyIgnoreArmorShot(event, entity, source))
            return;

        applyArmorScaling(event, entity);
    }

    private static boolean tryApplyIgnoreArmorShot(LivingHurtEvent event, LivingEntity entity, DamageSource source)
    {
        float damage = event.getAmount();
        if (damage <= 0.0F)
            return false;

        ShootableType shootableType = FlansDamageSources.getShootableTypeFromSource(source).orElse(null);
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

    private static void applyArmorScaling(LivingHurtEvent event, LivingEntity entity)
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
