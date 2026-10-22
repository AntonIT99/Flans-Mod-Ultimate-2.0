package com.wolffsmod.npcs.combat;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

/** Server-local hand alternation; the existing melee AI still owns one shared attack cooldown. */
public final class NpcMeleeState
{
    @Getter
    private InteractionHand nextHand = InteractionHand.MAIN_HAND;
    @Nullable
    private InteractionHand attackingHand;
    private int lastVictim;
    private long lastHitTick;
    private int lastRecovery;

    public interface Holder
    {
        NpcMeleeState wolffsmodnpcsMeleeState();
    }

    /** Pins the hand while item callbacks run, without moving either inventory stack. */
    public void begin(InteractionHand hand)
    {
        attackingHand = hand;
    }

    /** A cancelled or rejected strike still consumed its turn and the AI's shared recovery. */
    public void finish(InteractionHand hand)
    {
        attackingHand = null;
        nextHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Vanilla sword/tool callbacks name MAINHAND even when the item actually struck from the off hand. */
    public EquipmentSlot breakSlot(EquipmentSlot requested)
    {
        return attackingHand == InteractionHand.OFF_HAND && requested == EquipmentSlot.MAINHAND ? EquipmentSlot.OFFHAND : requested;
    }

    /** Only an accepted fast hit on this victim can let a slower hand follow before vanilla's ten-tick window ends. */
    public boolean followsRapidHit(int victim, long tick)
    {
        long elapsed = tick - lastHitTick;
        return lastRecovery > 0 && lastRecovery < 10 && lastVictim == victim && elapsed >= lastRecovery && elapsed < 10;
    }

    public void hit(int victim, long tick, int recovery)
    {
        lastVictim = victim;
        lastHitTick = tick;
        lastRecovery = recovery;
    }
}
