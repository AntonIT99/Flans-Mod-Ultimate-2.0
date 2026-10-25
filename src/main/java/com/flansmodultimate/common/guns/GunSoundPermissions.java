package com.flansmodultimate.common.guns;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Finite sound allowances issued only by accepted server shots and reloads. Animation timing stays client-side. */
public final class GunSoundPermissions
{
    public enum Kind
    {
        ACTION, INSERT
    }

    private final Map<InteractionHand, Allowance> actions = new EnumMap<>(InteractionHand.class);
    private final Map<InteractionHand, Allowance> inserts = new EnumMap<>(InteractionHand.class);

    private static final class Allowance
    {
        private final ItemStack gun;
        private final long expires;
        private int remaining;
        private long lastPlayed = Long.MIN_VALUE;
        private int playedThisTick;

        private Allowance(ItemStack gun, long expires, int remaining)
        {
            this.gun = gun;
            this.expires = expires;
            this.remaining = remaining;
        }
    }

    public void shot(InteractionHand hand, ItemStack gun, long now)
    {
        // One pump/bolt cycle per accepted shot. Slow content-pack animations may finish well after the shot cooldown.
        actions.put(hand, new Allowance(gun, now + 1200L, 1));
    }

    public void reload(InteractionHand hand, ItemStack gun, long now, float duration, int count)
    {
        // Animation boundaries can add a start/end click beside the individual insertions.
        inserts.put(hand, new Allowance(gun, now + (long) Math.ceil(duration) + 40L, Math.max(0, count) + 2));
    }

    public void clear(InteractionHand hand)
    {
        actions.remove(hand);
        inserts.remove(hand);
    }

    public boolean take(InteractionHand hand, Kind kind, ItemStack gun, long now)
    {
        Allowance allowance = (kind == Kind.ACTION ? actions : inserts).get(hand);
        if (allowance == null || allowance.gun != gun || now > allowance.expires || allowance.remaining <= 0)
            return false;
        if (allowance.lastPlayed != now)
            allowance.playedThisTick = 0;
        // The final insertion and an animation boundary may coincide, including a delayed double-barrel insert.
        if (allowance.playedThisTick >= 2)
            return false;
        allowance.remaining--;
        allowance.playedThisTick++;
        allowance.lastPlayed = now;
        return true;
    }
}
