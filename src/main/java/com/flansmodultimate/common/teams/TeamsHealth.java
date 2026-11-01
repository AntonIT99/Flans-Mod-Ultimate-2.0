package com.flansmodultimate.common.teams;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Global maximum-health overrides, independent of whether a Teams round is running. Server thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsHealth
{
    // 1.21 identifies attribute modifiers by id rather than by UUID and name.
    private static final ResourceLocation OVERRIDE = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "teams_health");
    private static final double SAME = 1.0E-4D;

    /** Applies a maximum in hit points, or removes our modifier for zero, preserving the remaining health fraction. */
    public static void apply(LivingEntity entity, int maximum)
    {
        if (entity.level().isClientSide)
            return;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null)
            return;
        boolean applied = health.getModifier(OVERRIDE) != null;
        double before = health.getValue();
        if (maximum == 0 && !applied || maximum > 0 && applied && Math.abs(before - maximum) < SAME)
            return;
        float fraction = before > 0D ? (float) Math.min(1D, entity.getHealth() / before) : 1F;
        if (applied)
            health.removeModifier(OVERRIDE);
        if (maximum > 0 && health.getValue() > 0D)
        {
            // Keep the maximum with saved health across reconnects and chunk unloads. Never replace the entity's base.
            health.addPermanentModifier(new AttributeModifier(OVERRIDE, maximum / health.getValue() - 1D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (entity.isAlive())
            entity.setHealth(fraction * entity.getMaxHealth());
    }
}
