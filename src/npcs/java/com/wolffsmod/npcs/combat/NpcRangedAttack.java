package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Bridges an NPC trigger to Flan's reusable projectile API. Custom NPCs retains timing and targeting. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcRangedAttack
{
    private static final WeaponMuzzle EYE_MUZZLE = new WeaponMuzzle(Vec3.ZERO, Optional.empty(), List.of());

    public static boolean fire(EntityNPCInterface npc, LivingEntity target, boolean highArc, int sequence)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        ItemStack ammo = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
        if (!usesFlanProjectile(options, ammo))
            return false;
        // A recognized Flan round is handled even when the call originates on the client.
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || npc.isKilled() || target == null || !target.isAlive() || target.level() != npc.level())
            return true;
        new Attack(npc, target, ammo, options, highArc, sequence).fire();
        return true;
    }

    public static boolean usesFlanProjectile(NpcWeaponOptions options, ItemStack ammo)
    {
        return options.enabled(Feature.PROJECTILES) && ammo != null && FlansProjectiles.isProjectile(ammo) && (!FlansProjectiles.isGrenade(ammo) || options.enabled(Feature.GRENADES));
    }

    private static final class Attack
    {
        private final EntityNPCInterface npc;
        private final ItemStack ammo;
        private final NpcWeaponOptions options;
        private final boolean highArc;
        private final int sequence;
        private final ItemStack held;
        private final FlanModelEntity model;
        private final IContentType definition;
        private final boolean secondary;
        private final Vec3 targetPoint;
        private final Vec3 targetVelocity;
        private final NpcEvent.RangedLaunchedEvent event;
        private final int shotCount;
        private boolean modelMuzzles;
        private boolean fired;
        private boolean soundPlayed;

        private Attack(EntityNPCInterface npc, LivingEntity target, ItemStack ammo, NpcWeaponOptions options, boolean highArc, int sequence)
        {
            this.npc = npc;
            this.ammo = ammo;
            this.options = options;
            this.highArc = highArc;
            this.sequence = sequence;
            held = npc.getMainHandItem();
            model = npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity flan ? flan : null;
            definition = model == null ? null : model.getInfoType();
            secondary = options.enabled(Feature.SECONDARY_BANK);
            targetPoint = target.position().add(0D, target.getBbHeight() * 0.5D, 0D);
            targetVelocity = options.enabled(Feature.LEAD_TARGET) ? target.getDeltaMovement() : Vec3.ZERO;
            event = new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, npc.stats.ranged.getStrength());
            shotCount = Math.max(1, Math.min(10, npc.stats.ranged.getShotCount()));
        }

        private static float modelScale(EntityNPCInterface npc)
        {
            return npc instanceof EntityCustomNpc custom && custom.modelData.simpleRender ? 1F : npc.display.getSize() / 5F;
        }

        private static Vec3 modelOrigin(EntityNPCInterface npc, FlanModelEntity model, Vec3 offset)
        {
            float height = model.getModelType() == null ? 0F : model.getModelType().getShape().modelHeight();
            Vec3 position = offset.add(0D, height, 0D).scale(modelScale(npc)).yRot((float) Math.toRadians(180F - npc.yBodyRot));
            double bodyOffset = npc.currentAnimation == 0 ? npc.ais.bodyOffsetY / 10D - 0.5D : 0D;
            if (npc.isPassenger() && npc instanceof EntityCustomNpc custom)
                bodyOffset -= 0.5D - custom.modelData.getLegsY() * 0.8D;
            else if (npc.isCrouching())
                bodyOffset -= 0.125D;
            bodyOffset *= npc.display.getSize() / 5D;
            return npc.position().add(position).add(0D, bodyOffset, 0D);
        }

        private void fire()
        {
            List<WeaponMuzzle> muzzles = definition != null && options.enabled(Feature.MODEL_MUZZLES)
                ? FlansProjectiles.getMuzzles(definition, secondary, sequence, options.enabled(Feature.ALTERNATE_BARRELS))
                : List.of();
            modelMuzzles = !muzzles.isEmpty();
            if (!modelMuzzles)
                muzzles = List.of(EYE_MUZZLE);
            for (WeaponMuzzle muzzle : muzzles)
                fireMuzzle(muzzle);
            if (fired)
                finishAttack();
        }

        private void fireMuzzle(WeaponMuzzle muzzle)
        {
            Vec3 origin = modelMuzzles ? modelOrigin(npc, model, muzzle.offset()) : npc.getEyePosition();
            for (int shotIndex = 0; shotIndex < shotCount; shotIndex++)
                fired = launch(muzzle, origin, shotIndex) || fired;
        }

        private boolean launch(WeaponMuzzle muzzle, Vec3 origin, int shotIndex)
        {
            ProjectileParameters parameters = new ProjectileParameters(sequence * shotCount + shotIndex, options.enabled(Feature.WEAPON_STATS), event.damage,
                (100F - npc.stats.ranged.getAccuracy()) * 0.2F, Math.max(0.01F, npc.stats.ranged.getSpeed() / 10F));
            var prepared = FlansProjectiles.prepare(npc, ammo, held, muzzle.weapon().orElse(null), definition, secondary, parameters);
            if (prepared.isEmpty())
                return false;
            var shot = prepared.get();
            Vec3 direction = ProjectileAim.direction(targetPoint.subtract(origin), targetVelocity, shot.speed(), options.enabled(Feature.BALLISTIC_AIM) ? shot.gravity() : 0D,
                options.enabled(Feature.BALLISTIC_AIM) ? shot.drag() : 1D, highArc);
            if (shot.launch(origin, direction, options.enabled(Feature.THROW_SOUNDS) && !soundPlayed && shotIndex == 0).isEmpty())
                return false;
            playLaunchEffects(muzzle, origin, shotIndex);
            return true;
        }

        private void playLaunchEffects(WeaponMuzzle muzzle, Vec3 origin, int shotIndex)
        {
            if (!soundPlayed)
            {
                if (FlansProjectiles.isGrenade(ammo))
                    soundPlayed = options.enabled(Feature.THROW_SOUNDS);
                else if (options.enabled(Feature.FIRE_SOUNDS))
                    soundPlayed = FlansProjectiles.playShootSound(npc, muzzle.weapon().orElse(definition), held, secondary);
            }
            if (shotIndex == 0 && options.enabled(Feature.SHOOT_PARTICLES) && (!FlansProjectiles.isGrenade(ammo) || modelMuzzles))
                FlansProjectiles.shootParticles(npc, muzzle, origin, npc.yBodyRot, modelScale(npc));
        }

        private void finishAttack()
        {
            if (!soundPlayed)
            {
                SoundEvent sound = npc.stats.ranged.getSoundEvent(0);
                if (sound != null)
                    npc.playSound(sound, 2F, 1F);
            }
            // Native Flan entities have their own hit handling rather than Custom NPCs' IProjectile wrapper.
            EventHooks.onNPCRangedLaunched(npc, event);
        }
    }
}
