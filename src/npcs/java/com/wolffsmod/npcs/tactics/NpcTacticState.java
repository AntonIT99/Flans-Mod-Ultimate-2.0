package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.tactics.NpcCover.Cover;
import lombok.Getter;
import lombok.Setter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

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

    /** Starts the tactic over when it or the target changes. */
    public void prepare(Tactic current, int target, long now, int defaultSide)
    {
        if (current == tactic && target == targetId)
            return;
        tactic = current;
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
        if (enemy != threat && threat == current && remembers(threat, now))
            return;
        threat = enemy;
        threatUntil = until;
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
