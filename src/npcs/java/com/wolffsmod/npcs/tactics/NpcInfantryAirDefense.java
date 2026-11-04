package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.sim.NpcCombatants;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.teams.TeamsBots;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Infantry responses to observed air attacks. Hidden aircraft are never followed; only a short-lived snapshot is kept. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcInfantryAirDefense
{
    static final long DANGER_TICKS = 100L;
    private static final double NOTICE_RANGE = 160D;
    private static final double MAX_ATTACK_HEIGHT = 96D;
    private static final double APPROACH_TICKS = 60D;
    private static final double CORRIDOR = 20D;
    private static final double MAX_SHOT_TICKS = 40D;

    @Getter
    public static final class Memory
    {
        private long until;
        private long scannedAt = Long.MIN_VALUE / 2;
        private long reportedAt = Long.MIN_VALUE / 2;
        private long snapshotAt;
        private int scanCursor;
        private List<Vec3> sources = List.of();
        private Vec3 escapeDirection = Vec3.ZERO;

        public boolean active(long now)
        {
            return now < until;
        }

        void remember(Vec3 soldier, Vec3 position, Vec3 velocity, long now)
        {
            // Keep a stable search through the pass, so a fast-moving source cannot restart it every decision.
            if (!active(now) || now - snapshotAt >= 60L)
            {
                sources = List.of(position, position.add(velocity.scale(20D)));
                escapeDirection = sideways(soldier, position, velocity);
                snapshotAt = now;
            }
            until = now + DANGER_TICKS;
        }
    }

    public static boolean infantry(EntityNPCInterface npc)
    {
        return !npc.isPassenger() && TacticKind.of(npc) == TacticKind.STANDARD;
    }

    public static boolean airborne(Entity entity)
    {
        return NpcAirCombat.airborne(entity);
    }

    /** An airborne target is never a useful destination for a soldier's ground path, even when it can be shot. */
    public static boolean holdsPosition(EntityNPCInterface npc, @Nullable LivingEntity target)
    {
        return target != null && airborne(target) && infantry(npc);
    }

    /** Called only for actual incoming fire; no visibility or enemy target-field assumption is needed. */
    public static void attacked(EntityNPCInterface npc, LivingEntity attacker)
    {
        if (!airborne(attacker) || !infantry(npc) || !NpcDrivers.hostile(npc, attacker))
            return;
        Entity platform = NpcVehicleThreats.platform(attacker);
        Memory memory = NpcTacticState.of(npc).getSafety().getAirDefense();
        long now = npc.level().getGameTime();
        memory.remember(npc.position(), platform.getEyePosition(), velocity(platform), now);
        wake(npc, now);
        report(npc, attacker, memory, now);
    }

    /** Checks local, visible attack approaches at most once a second, using the shared loaded-entity list. */
    public static void observe(EntityNPCInterface npc)
    {
        if (!infantry(npc) || !(npc.level() instanceof ServerLevel level) || !NpcEquipment.enabled(npc, Feature.TACTICAL_COVER))
            return;
        Memory memory = NpcTacticState.of(npc).getSafety().getAirDefense();
        long now = level.getGameTime();
        if (now >= memory.scannedAt && now - memory.scannedAt < 20L)
            return;
        memory.scannedAt = now;
        NpcCombatants combatants = NpcCombatants.of(level);
        int sightChecks = 0;
        int count = combatants.size();
        int index = 0;
        while (index++ < count && sightChecks < 4)
        {
            int cursor = Math.floorMod(memory.scanCursor++, count);
            LivingEntity enemy = combatants.get(cursor).entity();
            if (!noticeable(npc, enemy))
                continue;
            Entity platform = NpcVehicleThreats.platform(enemy);
            Vec3 motion = velocity(platform);
            boolean engaging = platform instanceof Mob mob && mob.getTarget() != null && (mob.getTarget() == npc || NpcDrivers.ally(npc, mob.getTarget()));
            if (attackRun(npc.position(), platform.position(), motion, engaging))
            {
                sightChecks++;
                if (npc.getSensing().hasLineOfSight(enemy))
            {
                    memory.remember(npc.position(), platform.getEyePosition(), motion, now);
                    wake(npc, now);
                    report(npc, enemy, memory, now);
                    return;
                }
            }
        }
    }

    private static boolean noticeable(EntityNPCInterface npc, LivingEntity enemy)
    {
        return enemy != npc && enemy.isAlive() && npc.distanceToSqr(enemy) <= NOTICE_RANGE * NOTICE_RANGE && airborne(enemy) && NpcDrivers.hostile(npc, enemy);
    }

    private static void wake(EntityNPCInterface npc, long now)
    {
        NpcSafetyState safety = NpcTacticState.of(npc).getSafety();
        safety.setUrgentUntil(Math.max(safety.getUrgentUntil(), now + 20L));
    }

    private static void report(EntityNPCInterface npc, LivingEntity enemy, Memory memory, long now)
    {
        if (now - memory.reportedAt >= 20L)
        {
            TeamsBots.reportAirAttack(npc, enemy);
            memory.reportedAt = now;
        }
    }

    static boolean attackRun(Vec3 soldier, Vec3 aircraft, Vec3 velocity, boolean engaging)
    {
        Vec3 offset = soldier.subtract(aircraft);
        if (-offset.y > MAX_ATTACK_HEIGHT || offset.y > 12D)
            return false;
        double speedSquared = velocity.horizontalDistanceSqr();
        // A hovering gunship attacking nearby troops remains dangerous without a forward pass.
        if (speedSquared < 0.01D)
            return engaging && offset.lengthSqr() <= 64D * 64D;
        double time = (offset.x * velocity.x + offset.z * velocity.z) / speedSquared;
        if (time < 0D || time > APPROACH_TICKS)
            return false;
        double miss = offset.subtract(velocity.scale(time)).horizontalDistanceSqr();
        // A low diving approach is observable evidence, even for a player pilot with no Mob target field.
        return miss <= CORRIDOR * CORRIDOR && (engaging || velocity.y < -0.08D);
    }

    static Vec3 sideways(Vec3 soldier, Vec3 aircraft, Vec3 velocity)
    {
        Vec3 side = new Vec3(-velocity.z, 0D, velocity.x).normalize();
        if (side.lengthSqr() < 0.01D)
            return soldier.subtract(aircraft).multiply(1D, 0D, 1D).normalize();
        return side.scale(soldier.subtract(aircraft).dot(side) >= 0D ? 1D : -1D);
    }

    private static Vec3 velocity(Entity entity)
    {
        Vec3 measured = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        return measured.lengthSqr() > 16D ? entity.getDeltaMovement() : measured;
    }

    /** Avoid speculative fire only while suited-target selection is enabled; mounted weapons keep their own policy. */
    public static boolean holdsFire(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        return NpcEquipment.enabled(npc, Feature.SUITED_TARGETS) && holdsPosition(npc, target) && !usefulWeapon(npc, target, weapon);
    }

    /** Covers both authoritative hand items and Custom NPCs' projectile-slot fallback, including scripted triggers. */
    public static boolean holdsFire(EntityNPCInterface npc, LivingEntity target)
    {
        if (!NpcEquipment.enabled(npc, Feature.SUITED_TARGETS) || !holdsPosition(npc, target))
            return false;
        if (NpcEquipment.weaponAuthority(npc))
            return !usefulWeapon(npc, target, NpcEquipment.rangedWeapon(npc));
        ItemStack ammo = NpcAmmunition.modelTemplate(npc);
        if (ammo.isEmpty() || blockedByArmor(npc, target, ItemStack.EMPTY, ammo))
            return true;
        double speed = NpcEquipment.speed(npc.stats.ranged.getSpeed());
        if (!npc.isInRange(target, NpcWeaponRange.firing(npc)))
            return true;
        var shot = FlansProjectiles.prepare(npc, ammo, ItemStack.EMPTY, null, null, false, new ProjectileParameters(0, false, 0F, 0F, (float) speed));
        return shot.map(value -> value.dropped() || !plausibleShot(target.getEyePosition().subtract(npc.getEyePosition()), velocity(NpcVehicleThreats.platform(target)), value.speed(),
            value.gravity(), value.drag())).orElseGet(() -> !plausibleShot(target.getEyePosition().subtract(npc.getEyePosition()), velocity(NpcVehicleThreats.platform(target)), speed, 0.03D, 0.99D));
    }

    public static boolean canEngage(EntityNPCInterface npc, LivingEntity target)
    {
        if (!holdsPosition(npc, target))
            return true;
        if (!npc.isInRange(target, NpcWeaponRange.firing(npc)))
            return false;
        if (!NpcEquipment.weaponAuthority(npc))
            return !holdsFire(npc, target);
        return NpcAmmunition.supplies(npc).stream().anyMatch(weapon -> NpcEquipment.ranged(weapon) && usefulWeapon(npc, target, weapon));
    }

    private static boolean usefulWeapon(EntityNPCInterface npc, LivingEntity target, ItemStack weapon)
    {
        if (!npc.isInRange(target, NpcWeaponRange.firing(npc)) || weapon.isEmpty() || FlansProjectiles.isGrenade(weapon) || FlansEquipment.isDeployable(weapon))
            return false;
        ItemStack ammo = NpcWeaponChoice.ammunition(npc, weapon);
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO) && !FlansEquipment.hasLoadedRound(weapon)
            && NpcAmmunition.supplies(npc).stream().noneMatch(supply -> FlansEquipment.acceptsAmmunition(weapon, supply)))
            return false;
        var properties = FlansEquipment.getWeaponProperties(weapon, ammo);
        if (properties.isEmpty())
            // Native or third-party weapons without readable ballistics retain short-range opportunities.
            return npc.distanceToSqr(target) <= 32D * 32D;
        if (properties.get().throwable() || properties.get().shield())
            return false;
        if (blockedByArmor(npc, target, weapon, ammo))
            return false;
        InteractionHand hand = weapon == npc.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean inherited = NpcEquipment.inherits(npc, EquipmentProperty.RANGED_SPEED.inHand(hand));
        var shot = FlansProjectiles.prepare(npc, ammo, weapon, null, null, false,
            new ProjectileParameters(FlansEquipment.getLoadedRoundIndex(weapon), inherited, 0F, 0F, inherited ? 1F : (float) NpcEquipment.speed(npc.stats.ranged.getSpeed())));
        if (shot.isEmpty() || shot.get().dropped())
            return false;
        return plausibleShot(target.getEyePosition().subtract(npc.getEyePosition()), velocity(NpcVehicleThreats.platform(target)), shot.get().speed(), shot.get().gravity(), shot.get().drag());
    }

    private static boolean blockedByArmor(EntityNPCInterface npc, LivingEntity target, ItemStack weapon, ItemStack ammo)
    {
        var armor = NpcVehicleThreats.protection(target, npc.position());
        var round = FlansProjectiles.getArmorPerformance(ammo, 0);
        double penetration = weapon.isEmpty() ? round.map(AmmunitionArmor::penetrationMm).orElse(Double.NaN) : FlansEquipment.getArmorPenetration(weapon, ammo).orElse(Double.NaN);
        boolean heat = round.map(AmmunitionArmor::shapedCharge).orElse(false);
        return armor.isPresent() && penetration < (heat ? armor.get().heatThicknessMm() : armor.get().thicknessMm());
    }

    static boolean plausibleShot(Vec3 offset, Vec3 velocity, double speed, double gravity, double drag)
    {
        ProjectileAim.Solution solution = ProjectileAim.solve(offset, velocity, speed, gravity, drag, false);
        return solution.ticks() > 0D && solution.ticks() <= MAX_SHOT_TICKS;
    }
}
