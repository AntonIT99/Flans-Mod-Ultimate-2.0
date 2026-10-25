package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.combat.NpcProjectileDamage;
import noppes.npcs.entity.EntityProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;

/** Makes Custom NPCs projectile hits use their configured damage even against players on Peaceful. */
@Mixin(value = EntityProjectile.class, remap = false)
public abstract class EntityProjectileMixin
{
    // The vanilla invocation must be remapped even though the Custom NPCs target is not.
    @SuppressWarnings("DefaultAnnotationParam")
    @Redirect(method = {"onHit",
        "m_6532_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;thrown(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", remap = true))
    private DamageSource wolffsmodnpcsUnscaledProjectile(DamageSources sources, Entity projectile, Entity owner)
    {
        return new NpcProjectileDamage(sources.thrown(projectile, owner));
    }
}
