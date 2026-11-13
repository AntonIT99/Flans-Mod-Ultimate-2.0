package com.wolffsmod.npcs.tactics;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcCover.Cover;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * What a tactic remembers of the fight it runs: the step it is at, the cover, side or circle it chose, how long the NPC's
 * reload or pause between bursts lasts, and the enemy last shooting at it. One per NPC, server side, not saved: a reloaded
 * NPC starts its tactic over.
 */
public final class NpcTacticState
{
    /** What a tactic of several steps is doing. */
    public enum Phase
    {
        /** Nothing chosen yet. */
        START,
        /** In cover, or going there. */
        HIDE,
        /** Stepped out of cover to fire. */
        PEEK,
        /** Standing to fire. */
        FIRE,
        /** Dashing to the next stop, or swinging out to the side. */
        MOVE,
        /** On the side it swung out to, attacking. */
        ATTACK
    }

    @Getter
    private Tactic tactic = Tactic.CUSTOM_NPCS;
    private int targetId = -1;
    @Getter
    private Phase phase = Phase.START;
    @Getter
    private long phaseStarted;
    /** The point the current step heads for, if it has one. */
    @Getter @Setter @Nullable
    private Vec3 point;
    /** The cover chosen, and when cover was last looked for. */
    @Getter @Nullable
    private Cover cover;
    @Getter
    private long coverSearchedAt = Long.MIN_VALUE / 2;
    /** Which way the NPC swings, circles or slips: 1 or -1. */
    @Getter @Setter
    private int side = 1;
    /** Radians round the target of the bearing a flank or a circle has reached. */
    @Getter @Setter
    private double bearing = Double.NaN;
    /** The point last handed to the navigator, and when, so that it is not asked for the same path every tick. */
    @Getter @Nullable
    private Vec3 ordered;
    @Getter
    private long orderedAt = Long.MIN_VALUE / 2;
    @Getter
    private long reloadedAt = Long.MIN_VALUE / 2;
    @Nullable
    private Item roleWeapon;
    @Getter
    private WeaponRole role = WeaponRole.GENERAL;
    /** The enemy that last hit or shot at the NPC, which it remembers until {@link #threatUntil}. */
    @Nullable
    private LivingEntity threat;
    private long threatUntil = Long.MIN_VALUE / 2;
    /** When the NPC last told its allies who is shooting at it. */
    private long alertedAt = Long.MIN_VALUE / 2;
    /** The firing range the NPC's weapon gives it, and the tick it was worked out in. */
    private int weaponRange;
    private long weaponRangeTick = Long.MIN_VALUE;
    /** Readings of the weapons the NPC asks about within one tick: both hands, each with and without its spare round. */
    private final HeldWeapon[] heldWeapons = {new HeldWeapon(), new HeldWeapon(), new HeldWeapon(), new HeldWeapon()};
    private final List<Threat> threats = new ArrayList<>();
    @Getter
    private final NpcCover.Searches coverSearches = new NpcCover.Searches();
    @Getter
    private final NpcSafetyState safety = new NpcSafetyState();
    @Getter
    private final NpcTacticalPosture posture = new NpcTacticalPosture();
    @Getter @Setter @Nullable
    private UUID meleeThrowTarget;
    /**
     * The point of its assault route the NPC heads for, and the route it was worked out on. It outlasts every fight, which
     * starts the tactic over but must not send the NPC back along its route; -1 until the NPC has worked it out.
     */
    private int assaultIndex = -1;
    private int assaultRoute;

    /** A position learned from incoming fire; a hidden attacker's subsequent movement is not followed. */
    public record Threat(LivingEntity entity, Vec3 position, long until)
    {}

    /**
     * One reading of what a held weapon fires, for the tick it was read in. The stacks are held by identity: an NPC keeps
     * the same stack objects while it carries them, and any swap or reload puts another stack in their place.
     */
    private static final class HeldWeapon
    {
        private long tick = Long.MIN_VALUE;
        @Nullable
        private ItemStack weapon;
        @Nullable
        private ItemStack ammunition;
        private Optional<EquippedWeaponProperties> properties = Optional.empty();

        private boolean matches(long now, ItemStack held, ItemStack round)
        {
            return tick == now && weapon == held && ammunition == round;
        }

        private Optional<EquippedWeaponProperties> read(long now, ItemStack held, ItemStack round, Optional<EquippedWeaponProperties> value)
        {
            tick = now;
            weapon = held;
            ammunition = round;
            properties = value;
            return value;
        }
    }

    /** Starts the tactic over when it or the target changes. */
    public void prepare(Tactic current, int target, long now, int defaultSide)
    {
        if (current == tactic && target == targetId)
            return;
        tactic = current;
        coverSearches.clear();
        targetId = target;
        enter(Phase.START, now);
        cover = null;
        coverSearchedAt = Long.MIN_VALUE / 2;
        side = defaultSide;
        bearing = Double.NaN;
        ordered = null;
    }

    public void enter(Phase next, long now)
    {
        phase = next;
        phaseStarted = now;
        point = null;
    }

    /** Records the point the navigator was sent to, or that it was sent to none. */
    public void order(@Nullable Vec3 point, long now)
    {
        ordered = point;
        orderedAt = now;
    }

    public void cover(@Nullable Cover found, long now)
    {
        cover = found;
        coverSearchedAt = now;
    }

    /** Records a reload that lasts the given ticks from now. */
    public void reloading(long now, int ticks)
    {
        reloadedAt = Math.max(reloadedAt, now + Math.max(0, ticks));
    }

    public boolean isReloading(long now)
    {
        return now < reloadedAt;
    }

    /** @return the point of the assault route the NPC heads for, or -1 when it has none yet or the route has changed since */
    public int assaultIndex(int route)
    {
        return route == assaultRoute ? assaultIndex : -1;
    }

    /** Records the point of the given assault route the NPC heads for. */
    public void assault(int route, int index)
    {
        assaultRoute = route;
        assaultIndex = index;
    }

    /** @return the role of the weapon, worked out again only when another weapon is held */
    public WeaponRole role(@Nullable Item weapon, Supplier<WeaponRole> classify)
    {
        if (weapon != roleWeapon)
        {
            roleWeapon = weapon;
            role = classify.get();
        }
        return role;
    }

    /**
     * Remembers the enemy as the NPC's threat until the given time. The threat the NPC is fighting is not displaced by
     * another while it still remembers it; the one fighting it is shot at by is remembered again.
     *
     * @param current
     *            the NPC's target
     */
    public void threat(LivingEntity enemy, @Nullable LivingEntity current, long now, long until)
    {
        threats.removeIf(known -> known.until() <= now || known.entity() == enemy || !known.entity().isAlive());
        if (threats.size() >= 4)
            threats.remove(0);
        threats.add(new Threat(enemy, enemy.getEyePosition(), until));
        if (enemy != threat && threat == current && remembers(threat, now))
            return;
        threat = enemy;
        threatUntil = until;
    }

    /** Recent attackers, independent of the firing target, with expired entries removed. Server thread. */
    public List<Threat> threats(long now)
    {
        threats.removeIf(known -> known.until() <= now || !known.entity().isAlive());
        return List.copyOf(threats);
    }

    /** @return whether the entity is the threat the NPC still remembers */
    public boolean remembers(@Nullable Entity entity, long now)
    {
        return entity != null && entity == threat && now < threatUntil;
    }

    /** @return the threat the NPC still remembers, or null */
    @Nullable
    public LivingEntity threat(long now)
    {
        return remembers(threat, now) ? threat : null;
    }

    /** @return whether the NPC may tell its allies of its threat again, counting this as its call when it may */
    public boolean alert(long now, int interval)
    {
        if (now - alertedAt < interval)
            return false;
        alertedAt = now;
        return true;
    }

    /** @return the firing range the NPC's weapon gives it, worked out once a tick */
    public int weaponRange(long now, IntSupplier measure)
    {
        if (now != weaponRangeTick)
        {
            weaponRangeTick = now;
            weaponRange = measure.getAsInt();
        }
        return weaponRange;
    }

    /**
     * Reading a Flan weapon's statistics copies the stack and works out its damage, muzzle velocity, spread, cadence and
     * reload time with its attachments and ammunition, which a target search, a firing goal and a range measurement would
     * each repeat within one tick.
     *
     * @param measure
     *            reads the weapon, asked only when this tick has no reading of that weapon and round yet
     * @return what the weapon fires, or empty for an item that is not a Flan weapon
     */
    public Optional<EquippedWeaponProperties> weaponProperties(long now, ItemStack weapon, ItemStack ammunition, Supplier<Optional<EquippedWeaponProperties>> measure)
    {
        HeldWeapon oldest = heldWeapons[0];
        for (HeldWeapon held : heldWeapons)
        {
            if (held.matches(now, weapon, ammunition))
                return held.properties;
            if (held.tick < oldest.tick)
                oldest = held;
        }
        return oldest.read(now, weapon, ammunition, measure.get());
    }

    public static NpcTacticState of(EntityNPCInterface npc)
    {
        return ((Holder) npc).wolffsmodnpcsTacticState();
    }

    /** Implemented on the Custom NPCs entity, which carries the state of its tactic. */
    public interface Holder
    {
        NpcTacticState wolffsmodnpcsTacticState();
    }
}
