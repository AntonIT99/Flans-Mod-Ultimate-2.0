package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcSight;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.tactics.NpcCombatMovement;
import com.wolffsmod.npcs.tactics.NpcCover;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcVehicleThreats;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Chooses the useful model bank before its ballistics, turret aim and firing position are calculated. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcVehicleCombat
{
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
            if (ammo.isEmpty())
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
