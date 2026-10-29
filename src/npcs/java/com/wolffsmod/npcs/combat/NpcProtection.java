package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.equipment.FlansMelee;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.platform.NpcPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.Resistances;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.*;

/** Per-NPC damage gates and knockback handling, applied on the server before Custom NPCs resolves a hit. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcProtection
{
    public static NpcCombatSettings settings(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat();
    }

    /**
     * Whether the hit is ignored outright: the NPC is invulnerable, or the hit is weaker than the minimum for its kind.
     * Damage that bypasses invulnerability, such as {@code /kill} and the void, always applies.
     */
    public static boolean ignores(EntityNPCInterface npc, DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return false;
        if (NpcEquipment.enabled(npc, Feature.INVULNERABLE))
            return true;
        NpcDamageCategory category = NpcDamageCategory.of(source);
        float minimum = settings(npc).minimumHit(category);
        if (category == NpcDamageCategory.MELEE && settings(npc).inheritsMeleeThreshold() && npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model
            && model.getInfoType() instanceof IDriveableType type)
            minimum = type.getMeleeDamageThreshold();
        return belowMinimum(minimum, amount);
    }

    /** Normalize melee before thresholds, editor resistances and damage scripts, using the striking hand's weapon. */
    public static float meleeDamage(EntityNPCInterface npc, DamageSource source, float amount)
    {
        if (npc.getVehicle() == null && npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity model && model.getInfoType() instanceof IDriveableType)
            return FlansMelee.againstVehicle(source, amount);
        return amount;
    }

    /** A hit of exactly the minimum is accepted; 0 disables the minimum. */
    static boolean belowMinimum(float minimum, float amount)
    {
        return minimum > 0 && amount < minimum;
    }

    /**
     * Applies the NPC's editor resistances. With Flan damage types recognized, Flan melee and tag-only damage types
     * reach the resistance of their kind; otherwise Custom NPCs classifies by message ID alone.
     */
    public static float resist(EntityNPCInterface npc, Resistances resistances, DamageSource source, float amount)
    {
        if (!NpcEquipment.enabled(npc, Feature.FLAN_DAMAGE_TYPES))
            return resistances.applyResistance(source, amount);
        return resist(NpcDamageCategory.of(source), resistances.arrow, resistances.melee, resistances.explosion, amount);
    }

    /** Custom NPCs resistance scale: 1 is neutral, 2 is immune and 0 doubles the damage. */
    static float resist(NpcDamageCategory category, float projectile, float melee, float explosion, float amount)
    {
        return switch (category)
        {
            case PROJECTILE -> amount * (2F - projectile);
            case MELEE -> amount * (2F - melee);
            case EXPLOSION -> amount * (2F - explosion);
            case OTHER -> amount;
        };
    }

    public static boolean knockbackImmune(EntityNPCInterface npc)
    {
        return knockbackImmune(settings(npc).getKnockback(), npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity);
    }

    static boolean knockbackImmune(Knockback mode, boolean flanModel)
    {
        return mode == Knockback.IMMUNE || mode == Knockback.MODEL && flanModel;
    }

    /**
     * Keeps a full knockback-resistance modifier on immune NPCs, the vanilla mechanism heavy mobs use, so every
     * knockback path that honors the attribute leaves them in place. Server thread.
     */
    public static void updateKnockback(EntityNPCInterface npc)
    {
        AttributeInstance resistance = npc.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance == null)
            return;
        NpcPlatform.setKnockbackImmunity(resistance, knockbackImmune(npc));
    }
}
