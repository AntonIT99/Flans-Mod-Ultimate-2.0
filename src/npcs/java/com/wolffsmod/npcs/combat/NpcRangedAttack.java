package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.EntityTypeProperties;
import com.flansmodultimate.api.entity.EntityTypeProperties.Weapon;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import com.wolffsmod.npcs.sim.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Bridges an NPC trigger to Flan's reusable projectile API. Custom NPCs retains timing and targeting. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcRangedAttack
{
    private static final WeaponMuzzle EYE_MUZZLE = new WeaponMuzzle(Vec3.ZERO, Optional.empty(), List.of());

    public static boolean fire(EntityNPCInterface npc, LivingEntity target, boolean highArc, int sequence)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        ItemStack ammo = NpcAmmunition.modelTemplate(npc);
        if (!usesFlanProjectile(options, ammo))
            return false;
        // A recognized Flan round is handled even when the call originates on the client.
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread() || npc.isKilled() || target == null || !target.isAlive() || target.level() != npc.level())
            return true;
        new Attack(npc, target, ammo, options, highArc, sequence).fire();
        return true;
    }

    /**
     * Resolves the round the NPC would fire next from its model's first muzzle, without firing it, so that a turret can
     * be laid for its speed and drop before the trigger is pulled. Server thread.
     */
    public static Optional<ProjectileShot> preview(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        ItemStack ammo = NpcAmmunition.modelTemplate(npc);
        if (!usesFlanProjectile(options, ammo) || !(npc.level() instanceof ServerLevel))
            return Optional.empty();
        Attack attack = new Attack(npc, null, ammo, options, false, 0);
        WeaponMuzzle muzzle = attack.muzzles().stream().findFirst().orElse(EYE_MUZZLE);
        return FlansProjectiles.prepare(npc, attack.ammunition(muzzle), attack.held, muzzle.weapon().orElse(null), attack.definition, attack.secondary,
            attack.parameters(0, attack.adjustedWeapon(muzzle).orElse(null)));
    }

    public static boolean usesFlanProjectile(NpcWeaponOptions options, ItemStack ammo)
    {
        return options.enabled(Feature.PROJECTILES) && ammo != null && FlansProjectiles.isProjectile(ammo) && (!FlansProjectiles.isGrenade(ammo) || options.enabled(Feature.GRENADES));
    }

    /**
     * Whether the selected model's weapons fire with their own damage, spread, speed and pellets. They only come from the
     * model: without one, or while model properties are not inherited, a held gun lends the shot nothing but its sound,
     * since its statistics are what Use equipped weapons governs.
     */
    public static boolean modelWeaponStats(NpcWeaponOptions options, @Nullable IContentType definition)
    {
        return definition != null && options.enabled(Feature.TYPE_PROPERTIES) && options.enabled(Feature.MODEL_WEAPON_STATS);
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
        @Nullable
        private final LivingEntity target;
        private final NpcVehicleState state;
        private final float damage;
        private final int shotCount;
        private NpcEvent.RangedLaunchedEvent event;
        private boolean modelMuzzles;
        private boolean fired;
        private boolean soundPlayed;

        private Attack(EntityNPCInterface npc, @Nullable LivingEntity target, ItemStack ammo, NpcWeaponOptions options, boolean highArc, int sequence)
        {
            this.npc = npc;
            this.ammo = ammo;
            this.options = options;
            this.highArc = highArc;
            this.sequence = sequence;
            held = npc.getMainHandItem();
            model = NpcModels.flan(npc);
            definition = model == null ? null : model.getInfoType();
            secondary = options.enabled(Feature.SECONDARY_BANK);
            this.target = target;
            state = NpcVehicleState.of(npc);
            damage = npc.stats.ranged.getStrength();
            shotCount = Mth.clamp(npc.stats.ranged.getShotCount(), 1, 10);
        }

        /** The yaw the model is drawn at: the hull of a vehicle, which need not be where the NPC looks. */
        private float hullYaw()
        {
            return state.isActive() ? state.getHullYaw() : npc.yBodyRot;
        }

        /** The model's muzzles for this trigger, carried round with the turret when the NPC aims one. */
        private List<WeaponMuzzle> muzzles()
        {
            if (definition == null || !options.enabled(Feature.MODEL_MUZZLES))
                return List.of();
            MountAim aim = state.isActive() && options.enabled(Feature.TURRET_MUZZLES) ? state.turretAim() : MountAim.REST;
            return FlansProjectiles.getMuzzles(definition, secondary, sequence, options.enabled(Feature.ALTERNATE_BARRELS), aim);
        }

        private void fire()
        {
            event = new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, damage);
            List<WeaponMuzzle> muzzles = muzzles();
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
            Vec3 origin = modelMuzzles ? NpcModels.world(npc, model, muzzle.offset(), hullYaw()) : npc.getEyePosition();
            Weapon adjusted = adjustedWeapon(muzzle).orElse(null);
            // A weapon firing with adjusted statistics still throws its own pellets, unless the shot count is adjusted too.
            int shots = adjusted != null && !own(NpcTypeProperty.SHOT_COUNT) ? Mth.clamp(adjusted.projectiles(), 1, 128) : shotCount;
            for (int shotIndex = 0; shotIndex < shots; shotIndex++)
                fired = launch(muzzle, origin, shotIndex, adjusted) || fired;
        }

        private boolean own(NpcTypeProperty property)
        {
            return NpcTypeProperties.usesOwnValue(npc, property);
        }

        private ItemStack ammunition(WeaponMuzzle muzzle)
        {
            return definition instanceof IDriveableType && options.enabled(Feature.DRIVER_GUNS) && muzzle.weapon().isPresent()
                ? NpcAmmunition.driverGunTemplate(npc, muzzle.weapon().orElseThrow())
                : ammo;
        }

        /**
         * The model weapon behind this muzzle, when the NPC keeps its own damage, accuracy or speed instead of the model's.
         * The shot then uses the NPC's values for those and the weapon's for the rest.
         */
        private Optional<Weapon> adjustedWeapon(WeaponMuzzle muzzle)
        {
            if (!modelWeaponStats(options, definition) || !(own(NpcTypeProperty.DAMAGE) || own(NpcTypeProperty.ACCURACY) || own(NpcTypeProperty.SPEED)))
                return Optional.empty();
            List<Weapon> weapons = NpcTypeProperties.of(npc).type().map(EntityTypeProperties::weapons).orElse(List.of());
            String name = muzzle.weapon().orElse(definition).getShortName();
            return weapons.stream().filter(weapon -> weapon.name().equals(name)).findFirst().or(() -> weapons.stream().findFirst());
        }

        private ProjectileParameters parameters(int shotIndex, @Nullable Weapon weapon)
        {
            int round = sequence * shotCount + shotIndex;
            float shotDamage = event == null ? damage : event.damage;
            float spread = (100F - npc.stats.ranged.getAccuracy()) * 0.2F;
            float speed = Math.max(0.01F, npc.stats.ranged.getSpeed() / 10F);
            if (weapon == null)
                return new ProjectileParameters(round, modelWeaponStats(options, definition), shotDamage, spread, speed);
            return new ProjectileParameters(round, false, own(NpcTypeProperty.DAMAGE) ? shotDamage : (float) weapon.damage(), own(NpcTypeProperty.ACCURACY) ? spread : (float) weapon.spread(),
                own(NpcTypeProperty.SPEED) ? speed : (float) weapon.speed().orElse(speed));
        }

        private boolean launch(WeaponMuzzle muzzle, Vec3 origin, int shotIndex, @Nullable Weapon adjusted)
        {
            ItemStack selectedAmmo = ammunition(muzzle);
            var prepared = FlansProjectiles.prepare(npc, selectedAmmo, held, muzzle.weapon().orElse(null), definition, secondary, parameters(shotIndex, adjusted));
            if (prepared.isEmpty())
                return false;
            var shot = prepared.get();
            boolean arc = options.enabled(Feature.BALLISTIC_AIM);
            // Only a preview has no target, and a preview never launches.
            LivingEntity aimed = Objects.requireNonNull(target);
            Vec3 from = throughCover(origin, NpcVehicleState.aimPoint(aimed));
            Vec3 direction = state.lead(NpcVehicleState.aimPoint(aimed).subtract(from), aimed, options, shot.speed(), arc ? shot.gravity() : 0D, arc ? shot.drag() : 1D, highArc);
            if (shot.launch(from, direction, options.enabled(Feature.THROW_SOUNDS) && !soundPlayed && shotIndex == 0).isEmpty())
                return false;
            if (modelMuzzles && shotIndex == 0 && !FlansProjectiles.isGrenade(selectedAmmo))
                NpcMuzzleFlashes.fired(npc, muzzle, direction, hullYaw(), secondary);
            playLaunchEffects(muzzle, origin, shotIndex, selectedAmmo);
            return true;
        }

        /** An NPC that fires through transparent blocks launches from beyond them, so the round is not stopped by what it aimed through. */
        private Vec3 throughCover(Vec3 origin, Vec3 aimPoint)
        {
            NpcTuning tuning = NpcTuning.of(npc);
            return tuning.sight() == NpcTuning.Sight.SEE_AND_FIRE_THROUGH ? NpcSight.launchPoint(npc, origin, aimPoint, tuning.get(NpcTuning.Value.FIRE_LIMIT)) : origin;
        }

        private void playLaunchEffects(WeaponMuzzle muzzle, Vec3 origin, int shotIndex, ItemStack selectedAmmo)
        {
            if (!soundPlayed)
            {
                if (FlansProjectiles.isGrenade(selectedAmmo))
                    soundPlayed = options.enabled(Feature.THROW_SOUNDS);
                else if (options.enabled(Feature.FIRE_SOUNDS) && !own(NpcTypeProperty.FIRING_SOUND))
                    soundPlayed = FlansProjectiles.playShootSound(npc, muzzle.weapon().orElse(definition), held, secondary);
            }
            if (shotIndex == 0 && options.enabled(Feature.SHOOT_PARTICLES) && (!FlansProjectiles.isGrenade(selectedAmmo) || modelMuzzles))
                FlansProjectiles.shootParticles(npc, muzzle, origin, hullYaw() + (state.isActive() ? state.turretAim().yaw() : 0F), NpcModels.scale(npc));
        }

        private void finishAttack()
        {
            if (modelMuzzles)
                // Every usual barrel count divides 120, so the remainder still names the barrel after the trip to the client.
                state.fired(Math.floorMod(sequence, 120));
            if (!soundPlayed)
            {
                SoundEvent sound = npc.stats.ranged.getSoundEvent(0);
                if (sound != null)
                    npc.playSound(sound, 2F, 1F);
            }
            // A held Flan gun that fired the round itself, rather than a model's mount, raises and cycles like a native shot.
            if (!modelMuzzles && !FlansProjectiles.isGrenade(ammo) && FlansEquipment.getWeaponProperties(held, ItemStack.EMPTY).isPresent())
                FlansEquipment.animateShot(npc, InteractionHand.MAIN_HAND);
            // Native Flan entities have their own hit handling rather than Custom NPCs' IProjectile wrapper.
            EventHooks.onNPCRangedLaunched(npc, event);
        }
    }
}
