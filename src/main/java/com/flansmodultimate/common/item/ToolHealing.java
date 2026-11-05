package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.ToolType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Native tool healing: authored HealStrength opts into the legacy regeneration duration/amplifier semantics. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolHealing
{
    public static void apply(ToolType type, LivingEntity target)
    {
        if (target.level().isClientSide || type.getHealAmount() <= 0)
            return;
        if (type.getHealStrength() < 0)
            target.heal(type.getHealAmount());
        else
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, type.getHealAmount(), type.getHealStrength()));
    }
}
