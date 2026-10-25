package com.wolffsmod.npcs.combat;

import net.minecraft.world.damagesource.DamageSource;

/** Keeps NPC projectile damage and attribution independent of the world's difficulty. */
public final class NpcProjectileDamage extends DamageSource
{
    public NpcProjectileDamage(DamageSource original)
    {
        super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
    }

    @Override
    public boolean scalesWithDifficulty()
    {
        return false;
    }
}
