package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.sim.NpcDormancy;
import com.wolffsmod.npcs.sim.NpcTargetSearch;
import noppes.npcs.ai.target.NpcNearestAttackableTargetGoal;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.function.Predicate;

/**
 * Custom NPCs' target acquisition.
 *
 * <ul>
 * <li>With progressive search, targets are looked for in distance bands on their own schedules, nearest candidates first,
 * instead of testing every entity within aggro range on each search.</li>
 * <li>A target the NPC already has is kept for a margin beyond its aggro range, so tactical manoeuvres do not lose it.</li>
 * <li>A dormant NPC, beyond its dormant range from every player, looks for no target at all.</li>
 * </ul>
 */
@Mixin(value = NpcNearestAttackableTargetGoal.class, remap = false)
@SuppressWarnings("AddedMixinMembersNamePattern")
public abstract class NpcNearestAttackableTargetGoalMixin<T extends LivingEntity> extends NearestAttackableTargetGoal<T>
{
    /** Created on first use: the goal's constructor already asks for its follow distance. */
    @Unique
    private NpcTargetSearch wolffsmodnpcsSearch;

    protected NpcNearestAttackableTargetGoalMixin(Mob mob, Class<T> type, int interval, boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> selector)
    {
        super(mob, type, interval, mustSee, mustReach, selector);
    }

    @Override
    public boolean canUse()
    {
        if (mob instanceof EntityNPCInterface resting && NpcDormancy.dormant(resting))
            return false;
        if (!(mob instanceof EntityNPCInterface npc) || !NpcTargetSearch.enabled(mob))
            return super.canUse();
        if (wolffsmodnpcsSearch == null)
            wolffsmodnpcsSearch = new NpcTargetSearch();
        target = wolffsmodnpcsSearch.find(npc, targetConditions, mob.getAttributeValue(Attributes.FOLLOW_RANGE));
        return target != null;
    }

    @Override
    protected double getFollowDistance()
    {
        return super.getFollowDistance() + NpcTargetSearch.margin(mob);
    }
}
