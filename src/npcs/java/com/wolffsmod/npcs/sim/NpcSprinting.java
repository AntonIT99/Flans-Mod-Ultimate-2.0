package com.wolffsmod.npcs.sim;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ai.EntityAISprintToTarget;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/** Chooses sprint speed each server tick without taking movement away from the goal that owns navigation. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcSprinting
{
    private static final double CHASE_START_DISTANCE = 8D;
    private static final double CHASE_STOP_DISTANCE = 6D;
    private static final double RETREAT_START_DISTANCE = 7D;
    private static final double RETREAT_STOP_DISTANCE = 9D;

    /** Removes the dependency's sprint goal, whose MOVE flag interrupts pursuit and clears its path. Called after each AI rebuild. */
    public static void removeLegacyGoal(GoalSelector goals)
    {
        // Take a snapshot because removing a goal mutates the selector's set.
        goals.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(EntityAISprintToTarget.class::isInstance).toList().forEach(goals::removeGoal);
    }

    /** Applies the decision after navigation has updated, including on ticks when the goal selector does not consider new goals. */
    public static void tick(EntityNPCInterface npc)
    {
        boolean sprinting = npc.isSprinting();
        boolean wanted = wantsSprint(npc, sprinting);
        if (wanted != sprinting)
            npc.setSprinting(wanted);
    }

    private static boolean wantsSprint(EntityNPCInterface npc, boolean sprinting)
    {
        if (!npc.ais.canSprint || !npc.isAlive() || npc.isKilled() || npc.isNoAi() || npc.isPassenger() || npc.ais.movementType != 0 || npc.isInWater() || npc.isInLava())
            return false;
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive() || target.level() != npc.level() || npc.getNavigation().isDone())
            return false;
        // Start from rest as soon as a path exists; a jump or damage reaction must not repeatedly cancel an ongoing sprint.
        if (!sprinting && !npc.onGround())
            return false;
        // Separate start and stop distances keep a moving target near the boundary from toggling the speed every tick.
        return switch (npc.ais.onAttack)
        {
            case 0 -> !npc.isInRange(target, sprinting ? CHASE_STOP_DISTANCE : CHASE_START_DISTANCE);
            case 2 -> npc.isInRange(target, sprinting ? RETREAT_STOP_DISTANCE : RETREAT_START_DISTANCE);
            default -> false;
        };
    }
}
