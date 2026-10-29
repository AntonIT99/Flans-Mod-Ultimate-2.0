package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.IFriendlyFireRule;
import com.flansmodultimate.common.teams.TeamsCombatants;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which hits between living entities are held back as friendly fire: between two fighters of a running Teams round, as its
 * game type rules; between any others, as the rules addons registered. Server side.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FriendlyFireRules
{
    private static final List<IFriendlyFireRule> RULES = new CopyOnWriteArrayList<>();

    public static void add(IFriendlyFireRule rule)
    {
        if (!RULES.contains(rule))
            RULES.add(rule);
    }

    public static void remove(IFriendlyFireRule rule)
    {
        RULES.remove(rule);
    }

    /** Whether the hit is held back as friendly fire. Clients and an entity hurting itself are never spared. */
    public static boolean spares(LivingEntity victim, LivingEntity attacker)
    {
        if (victim == attacker || victim.level().isClientSide)
            return false;
        Optional<Boolean> round = TeamsCombatants.mayHarm(TeamsManager.getInstance(), victim, attacker);
        if (round.isPresent())
            return !round.get();
        return sparedByRules(victim, attacker);
    }

    /**
     * Whether a registered rule holds back the damage, between entities the running Teams round does not judge; the game
     * type already turns away the hits it judges, before this is asked.
     */
    public static boolean sparesDamage(LivingEntity victim, DamageSource source)
    {
        LivingEntity attacker = TeamsCombatants.getCause(source);
        return attacker != null && attacker != victim && !victim.level().isClientSide && TeamsCombatants.mayHarm(TeamsManager.getInstance(), victim, attacker).isEmpty()
            && sparedByRules(victim, attacker);
    }

    private static boolean sparedByRules(LivingEntity victim, LivingEntity attacker)
    {
        for (IFriendlyFireRule rule : RULES)
        {
            try
            {
                if (rule.spares(victim, attacker))
                    return true;
            }
            catch (RuntimeException e)
            {
                FlansLog.log.error("Friendly fire rule {} failed", rule.getClass().getName(), e);
            }
        }
        return false;
    }
}
