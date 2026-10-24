package com.wolffsmod.npcs.properties;

import lombok.Getter;

/** Explicit mappings to Custom NPCs' stored fields, independent of entity classes and GUI widget IDs. */
@Getter
public enum NpcTypeProperty
{
    HEALTH(Component.STATS, "MaxHealth"), AGGRO_RANGE(Component.STATS, "AggroRange"), CAN_DROWN(Component.STATS, "CanDrown"), NO_FALL_DAMAGE(Component.STATS, "NoFallDamage"), DAMAGE(Component.STATS,
        "pDamage"), ACCURACY(Component.STATS, "Accuracy"), SPEED(Component.STATS, "pSpeed"), RANGE(Component.STATS, "MaxFiringRange"), DELAY_MIN(Component.STATS, "minDelay"), DELAY_MAX(
            Component.STATS, "maxDelay"), BURST_DELAY(Component.STATS, "FireRate"), BURST(Component.STATS, "BurstCount"), SHOT_COUNT(Component.STATS, "ShotCount"), FIRING_SOUND(Component.STATS,
                "FiringSound"), MELEE_REACH(Component.STATS, "AttackRange"), WALKING_SPEED(Component.AI, "MoveSpeed"), IDLE_SOUND(Component.ADVANCED, "NpcIdleSound"), STEP_SOUND(Component.ADVANCED,
                    "NpcStepSound"), POTION_IMMUNE(Component.STATS, "PotionImmune"), IGNORE_COBWEB(Component.STATS, "IgnoreCobweb"), HEALTH_REGEN(Component.STATS,
                        "HealthRegen"), COMBAT_REGEN(Component.STATS, "CombatRegen"), MELEE_DAMAGE(Component.STATS, "AttackStrenght"), MELEE_DELAY(Component.STATS, "AttackSpeed");

    private final Component component;
    private final String nbtKey;

    public enum Component
    {
        STATS, AI, ADVANCED
    }

    NpcTypeProperty(Component component, String nbtKey)
    {
        this.component = component;
        this.nbtKey = nbtKey;
    }
}
