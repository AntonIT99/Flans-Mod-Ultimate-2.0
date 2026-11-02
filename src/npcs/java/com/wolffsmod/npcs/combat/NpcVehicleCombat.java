package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.parts.IFlanPartTarget;
import com.flansmodultimate.api.parts.PartArmor;
import com.flansmodultimate.api.projectiles.AmmunitionArmor;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import com.wolffsmod.npcs.tactics.NpcCover;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ai.selector.NPCAttackSelector;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Chooses the useful model bank before its ballistics, turret aim and firing position are calculated. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcVehicleCombat
{
    /** Only confirmed heavy-round impacts on the current target can start an armour flank. */
    public static void impact(LivingEntity attacker, Entity target, String part, Vec3 normal, AmmunitionArmor round, boolean blocked)
    {
        if (NpcArmorEngagement.smallArms(round) || !(attacker instanceof EntityNPCInterface npc) || !(npc instanceof NpcTacticState.Holder) || npc.getTarget() == null
            || NpcVehicleThreats.platform(npc.getTarget()) != target || !NpcVehicleThreats.groundVehicle(npc) || !NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT))
            return;
        NpcTacticState.of(npc).getSafety().getArmorEngagement().impact(target.getId(), part, normal, round, blocked, npc.level().getGameTime());
    }

    /** Unknown armour, exposed occupants and explosive rounds remain eligible. */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // Absence explicitly means unknown ammunition, which must remain eligible.
    public static boolean mayFire(EntityNPCInterface npc, LivingEntity target, Vec3 origin, Optional<AmmunitionArmor> round, boolean explosive)
    {
        // Shielded NPC crew take their vehicle's damage. Independent gunners never call the NPC's setTarget redirection.
        EntityNPCInterface shielding = NpcCrew.shieldingVehicle(target).orElse(null);
        if (shielding != null)
            target = shielding;
        if (!NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) || !NpcVehicleThreats.groundVehicle(target) || explosive || round.isEmpty() || !NpcArmorEngagement.smallArms(round.get()))
            return true;
        Entity platform = NpcVehicleThreats.platform(target);
        if (platform != target)
        {
            Vec3 aim = NpcVehicleState.aimPoint(target);
            Vec3 contact = target.getBoundingBox().clip(origin, aim).orElse(aim);
            return FlansEntityTypes.getArmorAlongRay(platform, origin, contact).map(armor -> round.get().penetrationMm() >= thickness(round.get(), armor)).orElse(true);
        }
        Optional<PartArmor> hull = NpcVehicleThreats.protection(target, origin, "core");
        Optional<PartArmor> turret = turretProtection(target, origin);
        return !blocked(round.get(), hull, turret);
    }

    /** An absent casemate turret offers no alternative shot; a real hull can also establish protection along the aimed ray. */
    private static Optional<PartArmor> turretProtection(LivingEntity target, Vec3 origin)
    {
        Optional<PartArmor> turret = NpcVehicleThreats.protection(target, origin, "turret");
        if (turret.isPresent())
            return turret;
        if (target instanceof IFlanPartTarget vehicle && vehicle.flansmodultimatePartBody().filter(body -> body.getPart("turret").isEmpty()).isPresent())
            return NpcVehicleThreats.protection(target, origin, "core");
        return FlansEntityTypes.getArmorAlongRay(target, origin, NpcVehicleState.aimPoint(target));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // Unknown plates must remain distinct from known zero-thickness plates.
    static boolean blocked(AmmunitionArmor round, Optional<PartArmor> hull, Optional<PartArmor> turret)
    {
        return hull.isPresent() && turret.isPresent() && round.penetrationMm() < thickness(round, hull.get()) && round.penetrationMm() < thickness(round, turret.get());
    }

    private static double thickness(AmmunitionArmor round, PartArmor armor)
    {
        return round.shapedCharge() ? armor.heatThicknessMm() : armor.thicknessMm();
    }

    /** Keeps the main target when useful; an independently aimed gun otherwise searches for another visible enemy. */
    @Nullable
    public static LivingEntity gunnerTarget(EntityNPCInterface npc, ISeat seat, Vec3 origin, @Nullable LivingEntity preferred, double range)
    {
        if (!NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT))
            return preferred;
        if (preferred == null && !NpcEquipment.enabled(npc, Feature.DEFENSIVE_GUNNERS))
            return null;
        var armor = seat.getGunArmor();
        if (preferred != null && preferred.isAlive() && mayFire(npc, preferred, origin, armor, false))
            return preferred;
        var state = NpcTacticState.of(npc).getSafety();
        long now = npc.level().getGameTime();
        LivingEntity kept = state.getGunnerTarget();
        if (now - state.getGunnerCheckedAt() < 10L)
            return kept != null && kept.isAlive() && kept.distanceTo(npc) <= range && mayFire(npc, kept, origin, armor, false)
                && NpcSight.clear(npc.level(), origin, NpcVehicleState.aimPoint(kept), 0, npc) ? kept : null;
        state.setGunnerCheckedAt(now);
        NPCAttackSelector enemies = new NPCAttackSelector(npc);
        LivingEntity found = npc.level()
            .getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(range),
                candidate -> candidate != npc && candidate.getRootVehicle() != npc.getRootVehicle() && candidate.isAlive() && candidate.distanceTo(npc) <= range
                    && mayFire(npc, candidate, origin, armor, false))
            .stream().filter(enemies::isEntityApplicable).sorted(Comparator.comparingDouble(npc::distanceToSqr))
            .filter(candidate -> NpcSight.clear(npc.level(), origin, NpcVehicleState.aimPoint(candidate), 0, npc)).findFirst().orElse(null);
        state.setGunnerTarget(found);
        return found;
    }

    /** A reachable arc waypoint toward a side or rear after three frontal shell failures. Searches at most once a second. */
    @Nullable
    public static Vec3 armorFlank(EntityNPCInterface npc, LivingEntity target)
    {
        if (!(npc instanceof NpcTacticState.Holder))
            return null;
        var memory = NpcTacticState.of(npc).getSafety().getArmorEngagement();
        long now = npc.level().getGameTime();
        Entity victim = NpcVehicleThreats.platform(target);
        if (!memory.flanking(victim.getId(), now) || !NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT) || !NpcCombatMovement.allowed(npc) || !NpcVehicleThreats.groundVehicle(npc)
            || !NpcVehicleThreats.groundVehicle(target) || NpcVehicleThreats.type(npc).map(IDriveableType::isShip).orElse(false))
            return null;
        Vec3 position = NpcVehicleThreats.platform(npc).position();
        Vec3 radial = position.subtract(victim.position()).multiply(1D, 0D, 1D);
        // Once at the side, try the gun again. A successful penetration also clears this intent immediately.
        if (radial.normalize().dot(NpcVehicleThreats.front(victim)) <= 0.1D)
        {
            memory.reset(victim.getId());
            return null;
        }
        if (now - memory.getSearchedAt() < 20L)
            return memory.getWaypoint() != null && NpcCombatMovement.permits(npc, memory.getWaypoint()) ? memory.getWaypoint() : null;
        memory.setSearchedAt(now);
        memory.setWaypoint(null);
        return findArmorFlank(npc, memory, position, victim.position(), radial.horizontalDistance());
    }

    @Nullable
    private static Vec3 findArmorFlank(EntityNPCInterface npc, NpcArmorEngagement memory, Vec3 position, Vec3 target, double distance)
    {
        if (memory.getSide() == 0)
            memory.setSide((npc.getId() & 1) == 0 ? 1 : -1);
        double radius = Math.max(8D, distance);
        double step = Math.min(Math.toRadians(25D), 12D / radius);
        for (int side : new int[]{memory.getSide(), -memory.getSide()})
        {
            Vec3 point = flankGround(npc, flankStep(position, target, step * side));
            if (!reachable(npc, point))
                continue;
            memory.setSide(side);
            memory.setWaypoint(point);
            return point;
        }
        return null;
    }

    private static boolean reachable(EntityNPCInterface npc, @Nullable Vec3 point)
    {
        if (point == null)
            return false;
        var path = npc.getNavigation().createPath(BlockPos.containing(point), 0);
        return path != null && path.canReach();
    }

    static Vec3 flankStep(Vec3 position, Vec3 target, double radians)
    {
        Vec3 radial = position.subtract(target).multiply(1D, 0D, 1D);
        return target.add(radial.yRot((float) radians)).add(0D, position.y - target.y, 0D);
    }

    @Nullable
    private static Vec3 flankGround(EntityNPCInterface npc, Vec3 wanted)
    {
        BlockPos ground = BlockPos.containing(wanted);
        for (int rise : new int[]{0, 1, -1, 2, -2, 3, -3})
        {
            BlockPos stand = ground.above(rise);
            Vec3 point = Vec3.atBottomCenterOf(stand);
            if (NpcCombatMovement.permits(npc, point) && NpcCover.standable(npc.level(), stand))
                return point;
        }
        return null;
    }

    public static boolean bank(EntityNPCInterface npc, @Nullable IContentType model, boolean configured)
    {
        LivingEntity target = npc.getTarget();
        if (!(model instanceof IDriveableType) || target == null || !(npc instanceof NpcTacticState.Holder) || npc.level().isClientSide || !NpcEquipment.enabled(npc, Feature.ADAPTIVE_VEHICLE_COMBAT))
            return configured;
        var memory = NpcTacticState.of(npc).getSafety();
        long now = npc.level().getGameTime();
        if (memory.getBankModel() == model && now < memory.getBankCheckedAt() + 10L)
            return memory.isSecondary();
        boolean selected = configured;
        if (score(npc, target, model, !configured) > score(npc, target, model, configured) + 2D)
            selected = !configured;
        if (memory.getBankModel() == model && selected != memory.isSecondary() && now < memory.getBankChangedAt() + 40L)
            selected = memory.isSecondary();
        if (memory.getBankModel() != model || selected != memory.isSecondary())
            memory.setBankChangedAt(now);
        memory.setBankModel(model);
        memory.setBankCheckedAt(now);
        memory.setSecondary(selected);
        return selected;
    }

    private static double score(EntityNPCInterface npc, LivingEntity target, IContentType model, boolean secondary)
    {
        List<WeaponMuzzle> muzzles = FlansProjectiles.getMuzzles(model, secondary, 0, false);
        double best = -10000D;
        for (WeaponMuzzle muzzle : muzzles)
        {
            ItemStack ammo = muzzle.weapon().map(gun -> NpcAmmunition.driverGunTemplate(npc, gun)).orElseGet(() -> NpcAmmunition.bankTemplate(npc, model, secondary));
            if (ammo.isEmpty() || !mayFire(npc, target, npc.position(), FlansProjectiles.getArmorPerformance(ammo, 0, muzzle.weapon().orElse(null), model), NpcBlastSafety.reach(ammo) > 0D))
                continue;
            double value = NpcWeaponChoice.ammunitionScore(npc, target, ammo);
            // Machine guns are the useful default against single infantry; explosives retain their use at a safe distance.
            if (!NpcVehicleThreats.groundVehicle(target) && NpcBlastSafety.reach(ammo) <= 0D)
                value += 30D;
            best = Math.max(best, value);
        }
        return best;
    }

    /** Minimum useful spacing against infantry, independent of any change to speed or weapon statistics. */
    public static double infantryDistance(double firingRange, double blastDistance)
    {
        return Math.min(Math.max(1D, firingRange) * 0.8D, Math.max(12D, blastDistance + 3D));
    }

    /** Recent attackers first, then visibly carried anti-armour weapons; never examines an enemy's hidden inventory. */
    public static int threatRank(EntityNPCInterface npc, LivingEntity candidate)
    {
        if (!(npc instanceof NpcTacticState.Holder holder))
            return 2;
        if (holder.wolffsmodnpcsTacticState().threats(npc.level().getGameTime()).stream().anyMatch(threat -> threat.entity() == candidate))
            return 0;
        ItemStack held = candidate.getMainHandItem();
        if (!held.isEmpty() && FlansEquipment.getArmorPenetration(held, FlansEquipment.getLoadedAmmunition(held).orElse(ItemStack.EMPTY)).orElse(0D) >= 40D)
            return 1;
        return 2;
    }

    /** A nearby sideways firing position usable by NPC hulls and real vehicle controllers. Searches at most once a second. */
    @Nullable
    public static Vec3 lateralPosition(EntityNPCInterface npc, Vec3 muzzle, LivingEntity target)
    {
        var state = NpcTacticState.of(npc).getSafety();
        long now = npc.level().getGameTime();
        if (state.getFiringTarget() == target.getId() && now < state.getFiringPositionAt() + 20L)
            return state.getFiringPosition();
        state.setFiringPositionAt(now);
        state.setFiringTarget(target.getId());
        state.setFiringPosition(null);
        Vec3 position = NpcVehicleThreats.platform(npc).position();
        Vec3 toward = target.position().subtract(position).multiply(1D, 0D, 1D).normalize();
        Vec3 side = new Vec3(-toward.z, 0D, toward.x);
        for (double distance : new double[]{4D, -4D, 8D, -8D})
        {
            Vec3 shift = side.scale(distance);
            Vec3 point = position.add(shift);
            if (NpcCombatMovement.permits(npc, point) && NpcCover.standable(npc.level(), BlockPos.containing(point))
                && NpcSight.clear(npc.level(), muzzle.add(shift), NpcVehicleState.aimPoint(target), 0, npc))
            {
                state.setFiringPosition(point);
                return point;
            }
        }
        return null;
    }
}
