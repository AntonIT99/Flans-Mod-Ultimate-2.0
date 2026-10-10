package com.wolffsmod.npcs.combat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * The damage groups Custom NPCs resistances understand. Custom NPCs only reads vanilla message IDs, so Flan melee, and
 * damage types that only carry the vanilla tags, would otherwise escape every resistance.
 */
public enum NpcDamageCategory
{
    PROJECTILE, MELEE, EXPLOSION, OTHER;

    private static final Set<String> PROJECTILE_IDS = Set.of("arrow", "thrown");
    private static final Set<String> MELEE_IDS = Set.of("player", "mob", "npc", "melee");
    private static final Set<String> EXPLOSION_IDS = Set.of("explosion", "explosion.player");

    /** Classifies like Custom NPCs first, then by vanilla tags and by who struck directly. */
    public static NpcDamageCategory of(DamageSource source)
    {
        String id = source.getMsgId();
        if (PROJECTILE_IDS.contains(id) || source.is(DamageTypeTags.IS_PROJECTILE))
            return PROJECTILE;
        if (EXPLOSION_IDS.contains(id) || source.is(DamageTypeTags.IS_EXPLOSION))
            return EXPLOSION;
        if (MELEE_IDS.contains(id) || source.is(DamageTypeTags.IS_PLAYER_ATTACK)
            || source.getDirectEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity())
            return MELEE;
        return OTHER;
    }
}
