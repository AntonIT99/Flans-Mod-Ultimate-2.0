package com.flansmodultimate.common.explosions;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Server audit records for the Flan bullet/grenade explosion kills covered by 1.7.10. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExplosionKillAudit
{
    public static void logIfApplicable(ServerPlayer victim, DamageSource source)
    {
        if (!source.is(FlanDamageSources.EXPLOSION)
            || !(source.getDirectEntity() instanceof Shootable shootable)
            || !(shootable instanceof Bullet || shootable instanceof Grenade)
            || !(source.getEntity() instanceof ServerPlayer killer))
            return;

        ShootableType weapon = shootable.getConfigType();
        if (weapon == null)
            return;

        int lifetimeSeconds = victim.tickCount / 20;
        String message = formatKillRecord(
            killer.getGameProfile().getName(), victim.getGameProfile().getName(), weaponName(weapon), lifetimeSeconds,
            (int) victim.getX(), (int) victim.getY(), (int) victim.getZ(),
            (int) killer.getX(), (int) killer.getY(), (int) killer.getZ(),
            armorName(victim.getItemBySlot(EquipmentSlot.CHEST)),
            armorName(killer.getItemBySlot(EquipmentSlot.CHEST)));
        FlansMod.log.info(message);

        int warningThresholdSeconds = ModCommonConfig.get().noticeSpawnKillTime();
        if (isPossibleSpawnKill(lifetimeSeconds, warningThresholdSeconds))
        {
            String warning = formatSpawnKillWarning(killer.getGameProfile().getName(), victim.getGameProfile().getName(), lifetimeSeconds, warningThresholdSeconds);
            FlansMod.log.warn(warning);
        }
    }

    public static boolean isPossibleSpawnKill(int victimLifetimeSeconds, int warningThresholdSeconds)
    {
        return warningThresholdSeconds > 0 && victimLifetimeSeconds < warningThresholdSeconds;
    }

    public static String formatKillRecord(String killer, String victim, String weapon, int victimLifetimeSeconds,
                                   int victimX, int victimY, int victimZ,
                                   int killerX, int killerY, int killerZ,
                                   String victimChestArmor, String killerChestArmor)
    {
        return String.format(
            "Explosion kill: killer=%s victim=%s weapon=%s victimLifetimeSeconds=%d "
                + "victimPos=(%d,%d,%d) killerPos=(%d,%d,%d) victimChestArmor=%s killerChestArmor=%s",
            killer, victim, weapon, victimLifetimeSeconds,
            victimX, victimY, victimZ, killerX, killerY, killerZ,
            victimChestArmor, killerChestArmor);
    }

    public static String formatSpawnKillWarning(String killer, String victim, int victimLifetimeSeconds, int warningThresholdSeconds)
    {
        return String.format(
            "Possible spawn kill: killer=%s victim=%s victimLifetimeSeconds=%d warningThresholdSeconds=%d",
            killer, victim, victimLifetimeSeconds, warningThresholdSeconds);
    }

    private static String weaponName(ShootableType weapon)
    {
        return StringUtils.defaultIfBlank(weapon.getName(), weapon.getOriginalShortName());
    }

    private static String armorName(ItemStack stack)
    {
        return stack.isEmpty() ? "none" : stack.getDescriptionId();
    }
}
