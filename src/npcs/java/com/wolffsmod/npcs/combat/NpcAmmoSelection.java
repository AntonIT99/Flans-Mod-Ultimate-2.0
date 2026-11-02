package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import lombok.Getter;
import lombok.Setter;

import net.minecraft.world.item.ItemStack;

/**
 * One weapon's remembered choice of round, so that the rounds it may fire are scored against the target at most every
 * {@link #CHECK_INTERVAL} ticks and a round once chosen stands for {@link #CHANGE_INTERVAL} ticks. Without that a weapon
 * whose rounds score alike would alternate between them from shot to shot. Server thread.
 */
@Getter
@Setter
public final class NpcAmmoSelection
{
    /** Ticks a choice stands before the rounds are scored against the target again. */
    public static final long CHECK_INTERVAL = 10L;
    /** Ticks a chosen round stands before another may replace it, as loading another round takes a loader time. */
    public static final long CHANGE_INTERVAL = 40L;

    /** The weapon a remembered choice belongs to: a mounted gun, or a model's primary or secondary bank. */
    public record Key(IContentType weapon, boolean secondary)
    {}

    /** A single round of the chosen ammunition, kept for comparison only and never fired or consumed. */
    private ItemStack ammunition = ItemStack.EMPTY;
    private long checkedAt = Long.MIN_VALUE / 2;
    private long changedAt = Long.MIN_VALUE / 2;
    /** The target the choice was made against; another target is scored for at once. */
    private int target = -1;
}
