package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.entity.EntityTypeProperties;
import com.flansmodultimate.api.entity.FlansEntityTypes;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * The weapon group of the NPC's model beside its main attack, plus a separate driver-seat gun. Its gun mounts fire with their
 * guns' cadence; the rest of its muzzles, such as a tank's shells or rockets and a plane's bombs, fire as the model's own
 * group, at the group's cadence and reload. Each uses compatible inventory ammunition as a template, or its own default
 * ammunition. Rounds that are fired wait for the barrels to point at the target; bombs fall only on targets worth them, and
 * only once they would come down on them. One instance per NPC; all firing and timing run on the server thread. The main
 * attack's group remains owned by the ordinary ranged attack.
 */
public final class NpcDriverGuns
{
    private static final int MAX_SHOTS_PER_TICK = 8;
    /** Seat number of a gun mount in the group, and of the group's muzzles that have no gun of their own. */
    private static final int BANK_GUN = -1;
    private static final int PLATFORM = -2;
    private final Map<Mount, Gun> guns = new LinkedHashMap<>();
    private IContentType definition;
    private boolean secondary;

    /**
     * @param fixed
     *            the bank's muzzles, as selected with the aim, that stay fixed to the hull of a turreted model
     */
    private record Context(EntityNPCInterface npc, LivingEntity target, FlanModelEntity model, NpcVehicleState state, NpcWeaponOptions options, MountAim aim, Set<WeaponMuzzle> fixed)
    {}

    /**
     * A bank gun, the group's own muzzles and a driver's seat gun retain independent firing and reload progress.
     *
     * @param weapon
     *            the gun, or the model itself for the group's own muzzles
     */
    private record Mount(IContentType weapon, int seat)
    {
        /** @return whether the muzzle is one of this mount's: one of its gun's, or, for the group's own, one without a gun */
        boolean fires(WeaponMuzzle muzzle)
        {
            return seat == PLATFORM ? muzzle.weapon().isEmpty() : muzzle.weapon().filter(weapon::equals).isPresent();
        }
    }

    /**
     * How a mount fires.
     *
     * @param magazine
     *            rounds it fires before it reloads
     */
    private record Timing(double shotDelay, double reloadTime, int magazine)
    {}

    private static final class Gun
    {
        private double cooldown;
        private int fired;
        private int sequence;

        private void afterShot(Timing timing, boolean reload)
        {
            sequence = sequence >= Integer.MAX_VALUE / 2 ? 0 : sequence + 1;
            fired++;
            if (reload && fired >= Math.max(1, timing.magazine()))
            {
                fired = 0;
                cooldown += Math.max(timing.shotDelay(), timing.reloadTime());
            }
            else
                cooldown += Math.max(1D / MAX_SHOTS_PER_TICK, timing.shotDelay());
        }
    }

    /**
     * @return the shot the group beside the main attack fires first, for how it falls and whether it drops bombs; empty while
     *         the NPC fires no such group. Server thread.
     */
    public static Optional<ProjectileShot> preview(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType current = model == null ? null : model.getInfoType();
        if (!options.enabled(Feature.DRIVER_GUNS) || !(current instanceof IDriveableType) || !(npc.level() instanceof ServerLevel))
            return Optional.empty();
        boolean other = !NpcEquipment.secondaryBank(options, current);
        return FlansProjectiles.getMuzzles(current, other, 0, false).stream().findFirst().flatMap(muzzle ->
        {
            IContentType weapon = muzzle.weapon().orElse(null);
            ItemStack ammo = weapon == null ? NpcAmmunition.bankTemplate(npc, current, other) : NpcAmmunition.driverGunTemplate(npc, weapon);
            return ammo.isEmpty() ? Optional.empty() : FlansProjectiles.prepare(npc, ammo, ItemStack.EMPTY, weapon, current, other, new ProjectileParameters(0, true, 0F, 0F, 1F));
        });
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
        boolean bank = !NpcEquipment.secondaryBank(options, current);
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
        Map<Mount, List<WeaponMuzzle>> muzzles = new LinkedHashMap<>();
        List<WeaponMuzzle> selected = FlansProjectiles.getMuzzles(current, secondary, 0, false, aim);
        for (WeaponMuzzle muzzle : selected)
        {
            Mount mount = muzzle.weapon().map(weapon -> new Mount(weapon, BANK_GUN)).orElseGet(() -> new Mount(current, PLATFORM));
            muzzles.computeIfAbsent(mount, key -> new ArrayList<>()).add(muzzle);
        }
        Context context = new Context(npc, target, model, state, options, aim, fixedMuzzles(current, state, selected));
        muzzles.forEach((mount, points) -> fire(context, mount, points));
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
            && !NpcCrew.holdsFire(npc);
    }

    private void fire(Context context, Mount mount, List<WeaponMuzzle> points)
    {
        EntityNPCInterface npc = context.npc();
        NpcWeaponOptions options = context.options();
        IContentType weapon = mount.weapon();
        Gun gun = guns.computeIfAbsent(mount, key -> new Gun());
        if (gun.cooldown >= 1D)
            return;
        ItemStack ammo = ammunition(npc, mount);
        Timing timing = ammo.isEmpty() ? null : timing(mount, ammo).orElse(null);
        if (timing == null)
            return;
        // A mount loaded with something explosive waits until the target is out of its own blast.
        if (NpcBlastSafety.tooClose(npc, context.target(), NpcBlastSafety.reach(ammo)))
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
            gun.afterShot(timing, options.enabled(Feature.RELOAD_CYCLE));
        }
    }

    /** @return what the mount fires: compatible inventory ammunition or its default; empty for a gun without an item */
    private ItemStack ammunition(EntityNPCInterface npc, Mount mount)
    {
        if (mount.seat() == PLATFORM)
            return NpcAmmunition.bankTemplate(npc, definition, secondary);
        return mount.weapon().getItem() == null ? ItemStack.EMPTY : NpcAmmunition.driverGunTemplate(npc, mount.weapon());
    }

    /** @return how the mount fires that ammunition: as its gun does, or, for the group's own muzzles, as the model fires the group */
    private Optional<Timing> timing(Mount mount, ItemStack ammo)
    {
        if (mount.seat() != PLATFORM)
        {
            Item item = mount.weapon().getItem();
            if (item == null)
                return Optional.empty();
            Optional<EquippedWeaponProperties> properties = FlansEquipment.getWeaponProperties(new ItemStack(item), ammo);
            return properties.map(gun -> new Timing(gun.shotDelay(), gun.reloadTime(), FlansProjectiles.getRoundsPerItem(ammo)));
        }
        Optional<ReloadCycle> cycle = FlansEntityTypes.getReloadCycle(definition, secondary, ammo);
        if (cycle.isPresent())
            return cycle.map(group -> new Timing(group.shotDelay(), group.reloadTicks(), group.volleys()));
        OptionalDouble delay = FlansEntityTypes.getProperties(definition, secondary, false).map(EntityTypeProperties::shootDelay).orElse(OptionalDouble.empty());
        return delay.isPresent() ? Optional.of(new Timing(delay.getAsDouble(), delay.getAsDouble(), FlansProjectiles.getRoundsPerItem(ammo))) : Optional.empty();
    }

    private List<WeaponMuzzle> selectedMuzzles(Context context, Mount mount, Gun gun, List<WeaponMuzzle> points)
    {
        if (mount.seat() >= 0)
            return FlansProjectiles.getSeatMuzzle(definition, mount.seat(), gun.sequence, context.aim()).map(List::of).orElse(List.of());
        if (!context.options().enabled(Feature.ALTERNATE_BARRELS))
            return points;
        // A bank may alternate between different gun types. Skip the other types' turns without starving this gun.
        int count = FlansProjectiles.getMuzzles(definition, secondary, 0, false).size();
        for (int turn = 0; turn < count; turn++)
        {
            List<WeaponMuzzle> selected = FlansProjectiles.getMuzzles(definition, secondary, gun.sequence, true, context.aim()).stream().filter(mount::fires).toList();
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
        ProjectileShot shot = FlansProjectiles.prepare(npc, ammo, ItemStack.EMPTY, muzzle.weapon().orElse(null), definition, secondary, new ProjectileParameters(round, true, 0F, 0F, 1F)).orElse(null);
        if (shot == null)
            return false;
        if (shot.dropped())
            return dropBomb(context, origin, shot);
        if (options.enabled(Feature.FIRE_WHEN_ALIGNED) && !state.isGunAligned())
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
            NpcMuzzleFlashes.shootParticles(npc, muzzle, origin, state.getHullYaw() + state.turretAim().yaw(), NpcModels.scale(npc), secondary);
        return true;
    }

    /** A bomb falls away from the bay without a flash, only on a target worth it, and only once it would come down on it. */
    private static boolean dropBomb(Context context, Vec3 origin, ProjectileShot shot)
    {
        EntityNPCInterface npc = context.npc();
        LivingEntity target = context.target();
        NpcVehicleState state = context.state();
        if (!NpcBombTargets.worthBombing(target) || !state.bombDrop(npc, origin, shot.gravity(), shot.drag(), target, context.options()).hits())
            return false;
        return !shot.launch(origin, state.barrelDirection(npc, false), false).isEmpty();
    }
}
