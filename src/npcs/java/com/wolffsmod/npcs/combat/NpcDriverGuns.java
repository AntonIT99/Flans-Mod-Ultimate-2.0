package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.flansmodultimate.api.projectiles.ProjectileShot;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.*;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Driver-controlled gun mounts in the other bank, plus a separate driver-seat gun. They fire independently of the main
 * attack, using compatible inventory ammunition as templates, or their own default ammunition. One instance per NPC;
 * all firing and timing run on the server thread. The selected bank remains owned by the ordinary ranged attack.
 */
public final class NpcDriverGuns
{
    private static final int MAX_SHOTS_PER_TICK = 8;
    private final Map<Mount, Gun> guns = new LinkedHashMap<>();
    private IContentType definition;
    private boolean secondary;

    /**
     * @param fixed
     *            the bank's muzzles, as selected with the aim, that stay fixed to the hull of a turreted model
     */
    private record Context(EntityNPCInterface npc, LivingEntity target, FlanModelEntity model, NpcVehicleState state, NpcWeaponOptions options, MountAim aim, Set<WeaponMuzzle> fixed)
    {}

    /** A bank gun and a driver's seat gun retain independent firing and reload progress. */
    private record Mount(IContentType weapon, int seat)
    {}

    private static final class Gun
    {
        private double cooldown;
        private int fired;
        private int sequence;

        private void afterShot(EquippedWeaponProperties properties, ItemStack ammo, boolean reload)
        {
            sequence = sequence >= Integer.MAX_VALUE / 2 ? 0 : sequence + 1;
            fired++;
            if (reload && fired >= Math.max(1, FlansProjectiles.getRoundsPerItem(ammo)))
            {
                fired = 0;
                cooldown += Math.max(properties.shotDelay(), properties.reloadTime());
            }
            else
                cooldown += Math.max(1D / MAX_SHOTS_PER_TICK, properties.shotDelay());
        }
    }

    /** Runs after the vehicle model has been aimed and its damaged parts have been updated. */
    public void tick(EntityNPCInterface npc)
    {
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            return;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (NpcRepairs.active(npc) || !options.enabled(Feature.DRIVER_GUNS))
        {
            guns.clear();
            return;
        }
        guns.values().forEach(gun -> gun.cooldown = Math.max(0D, gun.cooldown - 1D));
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType current = model == null ? null : model.getInfoType();
        boolean bank = !options.enabled(Feature.SECONDARY_BANK);
        if (definition != current || secondary != bank)
        {
            guns.clear();
            definition = current;
            secondary = bank;
        }
        LivingEntity target = npc.getTarget();
        NpcVehicleState state = NpcVehicleState.of(npc);
        if (!(current instanceof IDriveableType) || !engages(npc, target, state, options))
            return;
        MountAim aim = options.enabled(Feature.TURRET_MUZZLES) ? state.turretAim() : MountAim.REST;
        Map<IContentType, List<WeaponMuzzle>> muzzles = new LinkedHashMap<>();
        List<WeaponMuzzle> selected = FlansProjectiles.getMuzzles(current, secondary, 0, false, aim);
        for (WeaponMuzzle muzzle : selected)
            muzzle.weapon().ifPresent(weapon -> muzzles.computeIfAbsent(weapon, key -> new ArrayList<>()).add(muzzle));
        Context context = new Context(npc, target, model, state, options, aim, fixedMuzzles(current, state, selected));
        muzzles.forEach((weapon, points) -> fire(context, new Mount(weapon, -1), points));
        // A DriverGun/PilotGun on seat zero is separate from the primary/secondary shoot-point banks.
        FlansProjectiles.getSeatMuzzle(current, 0, 0, aim).filter(muzzle -> muzzle.weapon().isPresent()).ifPresent(muzzle ->
        {
            boolean inBank = FlansProjectiles.getMuzzles(current, !secondary, 0, false, aim).contains(muzzle) || muzzles.values().stream().anyMatch(points -> points.contains(muzzle));
            if (!inBank)
                fire(context, new Mount(muzzle.weapon().orElseThrow(), 0), List.of(muzzle));
        });
    }

    /**
     * @param selected
     *            the bank's muzzles, as the guns fire them
     * @return those of them that stay fixed to the hull of a turreted model while its turret turns
     */
    private Set<WeaponMuzzle> fixedMuzzles(IContentType current, NpcVehicleState state, List<WeaponMuzzle> selected)
    {
        boolean[] flags = state.fixedMuzzles(FlansProjectiles.getMuzzles(current, secondary, 0, false, MountAim.REST), FlansProjectiles.getMuzzles(current, secondary, 0, false, state.turretAim()));
        Set<WeaponMuzzle> fixed = new HashSet<>();
        for (int index = 0; index < selected.size() && index < flags.length; index++)
            if (flags[index])
                fixed.add(selected.get(index));
        return fixed;
    }

    private static boolean engages(EntityNPCInterface npc, LivingEntity target, NpcVehicleState state, NpcWeaponOptions options)
    {
        return options.enabled(Feature.PROJECTILES) && options.enabled(Feature.MODEL_MUZZLES) && npc.isAlive() && !npc.isKilled() && target != null && target.isAlive() && target.level() == npc.level()
            && target.distanceTo(npc) <= npc.stats.ranged.getRange() && !state.getFlight().holdsFire() && !state.getParts().effects().gunDisabled() && !NpcDrivers.holdsFire(npc)
            && !NpcCrew.holdsFire(npc) && (!options.enabled(Feature.FIRE_WHEN_ALIGNED) || state.isGunAligned());
    }

    private void fire(Context context, Mount mount, List<WeaponMuzzle> points)
    {
        EntityNPCInterface npc = context.npc();
        NpcWeaponOptions options = context.options();
        IContentType weapon = mount.weapon();
        Gun gun = guns.computeIfAbsent(mount, key -> new Gun());
        if (gun.cooldown >= 1D || weapon.getItem() == null)
            return;
        ItemStack ammo = NpcAmmunition.driverGunTemplate(npc, weapon);
        EquippedWeaponProperties properties = FlansEquipment.getWeaponProperties(new ItemStack(weapon.getItem()), ammo).orElse(null);
        if (ammo.isEmpty() || properties == null)
            return;
        for (int trigger = 0; trigger < MAX_SHOTS_PER_TICK && gun.cooldown < 1D; trigger++)
        {
            List<WeaponMuzzle> selected = selectedMuzzles(context, mount, gun, points);
            boolean fired = false;
            for (WeaponMuzzle muzzle : selected)
                fired = launch(context, muzzle, ammo, gun.fired) || fired;
            if (!fired)
                return;
            if (trigger == 0 && options.enabled(Feature.FIRE_SOUNDS))
                FlansProjectiles.playShootSound(npc, weapon, ItemStack.EMPTY, secondary);
            gun.afterShot(properties, ammo, options.enabled(Feature.RELOAD_CYCLE));
        }
    }

    private List<WeaponMuzzle> selectedMuzzles(Context context, Mount mount, Gun gun, List<WeaponMuzzle> points)
    {
        if (mount.seat() >= 0)
            return FlansProjectiles.getSeatMuzzle(definition, mount.seat(), gun.sequence, context.aim()).map(List::of).orElse(List.of());
        IContentType weapon = mount.weapon();
        if (!context.options().enabled(Feature.ALTERNATE_BARRELS))
            return points;
        // A bank may alternate between different gun types. Skip the other types' turns without starving this gun.
        int count = FlansProjectiles.getMuzzles(definition, secondary, 0, false).size();
        for (int turn = 0; turn < count; turn++)
        {
            List<WeaponMuzzle> selected = FlansProjectiles.getMuzzles(definition, secondary, gun.sequence, true, context.aim()).stream()
                .filter(muzzle -> muzzle.weapon().filter(weapon::equals).isPresent()).toList();
            if (!selected.isEmpty())
                return selected;
            gun.sequence = gun.sequence >= Integer.MAX_VALUE / 2 ? 0 : gun.sequence + 1;
        }
        return List.of();
    }

    private boolean launch(Context context, WeaponMuzzle muzzle, ItemStack ammo, int round)
    {
        EntityNPCInterface npc = context.npc();
        LivingEntity target = context.target();
        NpcVehicleState state = context.state();
        NpcWeaponOptions options = context.options();
        Vec3 origin = NpcModels.world(npc, context.model(), muzzle.offset(), state.getHullYaw());
        Vec3 aimPoint = NpcVehicleState.aimPoint(target);
        NpcTuning tuning = NpcTuning.of(npc);
        int transparent = tuning.sight() == NpcTuning.Sight.SEE_AND_FIRE_THROUGH ? tuning.get(NpcTuning.Value.FIRE_LIMIT) : 0;
        if (!NpcSight.clear(npc.level(), origin, aimPoint, transparent, npc))
            return false;
        if (tuning.sight() == NpcTuning.Sight.SEE_AND_FIRE_THROUGH)
            origin = NpcSight.launchPoint(npc, origin, aimPoint, tuning.get(NpcTuning.Value.FIRE_LIMIT));
        ProjectileShot shot = FlansProjectiles.prepare(npc, ammo, ItemStack.EMPTY, muzzle.weapon().orElseThrow(), definition, secondary, new ProjectileParameters(round, true, 0F, 0F, 1F))
            .orElse(null);
        if (shot == null)
            return false;
        // Rounds leave along the barrels, as a player's do: the turret's, or the hull's for a gun fixed to it, which waits for the
        // hull to point where they have to go. The driver's own seat gun is the turret's.
        boolean fixed = context.fixed().contains(muzzle);
        if (fixed && !state.isHullAligned())
            return false;
        Vec3 direction = state.barrelDirection(npc, fixed);
        if (shot.launch(origin, direction, false).isEmpty())
            return false;
        NpcMuzzleFlashes.fired(npc, muzzle, direction, state.getHullYaw(), secondary);
        if (options.enabled(Feature.SHOOT_PARTICLES))
            FlansProjectiles.shootParticles(npc, muzzle, origin, state.getHullYaw() + state.turretAim().yaw(), NpcModels.scale(npc));
        return true;
    }
}
