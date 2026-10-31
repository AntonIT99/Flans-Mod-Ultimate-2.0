package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.parts.IFlanDamageModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

/** Server-side passive healing shared by round players and addon soldiers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsRegeneration
{
    /** Half a heart every five seconds, even when the hunger override keeps saturation full. */
    public static final int INTERVAL_TICKS = 100;

    public static boolean applies(LivingEntity entity)
    {
        TeamsManager manager = TeamsManager.getInstance();
        return !entity.level().isClientSide && manager.isRoundRunning() && (entity instanceof Player || TeamsCombatants.getCombatantTeam(manager, entity) != null)
            && !(entity instanceof IFlanDamageModel model && model.flansmodultimateDamageModel().isPresent());
    }

    public static void tick(LivingEntity entity)
    {
        if (!applies(entity) || !TeamsManager.getInstance().isRegeneration() || !entity.isAlive() || !entity.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION))
            return;
        if (entity instanceof Player player && player.getFoodData().getFoodLevel() < 18)
            return;
        if (entity.tickCount % INTERVAL_TICKS == 0 && entity.getHealth() < entity.getMaxHealth())
        {
            entity.heal(1F);
            if (entity instanceof Player player)
                player.causeFoodExhaustion(6F);
        }
    }
}
