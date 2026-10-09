package com.wolffsmod.npcs.combat;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.damagesource.CombatRules;

/** Display-only projection of the damage pipeline: Flan ratio, native armor, then enchantment protection. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcArmorPreview
{
    public static float protection(float incoming, float defense, float armor, float toughness, int enchantments)
    {
        if (incoming <= 0F)
            return 0F;
        float remaining = incoming * (1F - Math.clamp(defense, 0F, 1F));
        // The editor preview has no damage source; project the ordinary vanilla armor curve.
        float effectiveArmor = net.minecraft.util.Mth.clamp(armor - remaining / (2F + toughness / 4F), armor * 0.2F, 20F);
        remaining *= 1F - effectiveArmor / 25F;
        remaining = CombatRules.getDamageAfterMagicAbsorb(remaining, enchantments);
        return 1F - remaining / incoming;
    }
}
