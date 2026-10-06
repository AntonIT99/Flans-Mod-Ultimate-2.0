package com.wolffsmod.npcs.combat;

import net.minecraft.world.damagesource.CombatRules;

/** Display-only projection of the damage pipeline: Flan ratio, native armor, then enchantment protection. */
public final class NpcArmorPreview
{
    private NpcArmorPreview()
    {}

    public static float protection(float incoming, float defense, float armor, float toughness, int enchantments)
    {
        if (incoming <= 0F)
            return 0F;
        float remaining = incoming * (1F - Math.max(0F, Math.min(1F, defense)));
        remaining = CombatRules.getDamageAfterAbsorb(remaining, armor, toughness);
        remaining = CombatRules.getDamageAfterMagicAbsorb(remaining, enchantments);
        return 1F - remaining / incoming;
    }
}
