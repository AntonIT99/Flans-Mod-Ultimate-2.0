package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcThrowing.Choice;
import com.wolffsmod.npcs.combat.NpcThrowing.Kind;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.EventHooks;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Throws a grenade, a throwable Flan gun or a native throwable now and then beside the NPC's other attacks, as the strategy
 * for its kind says (see {@link NpcThrowing}). The throw winds up for the item's charge time with the item shown in hand,
 * while the NPC's other ranged attacks wait, and is given up once the target comes too close, goes out of sight or the item
 * is gone. A kind thrown waits for its cooldown, and a new target first waits a random part of it, so that NPCs meeting the
 * same enemy do not all throw at once.
 *
 * <p>
 * The goal claims no flag: whatever moves the NPC keeps moving it, and the melee attack goes on while the target is near.
 * Server thread.
 * </p>
 */
public final class NpcThrowGoal extends Goal
{
    /** Ticks between looks through the inventory for something worth throwing. */
    private static final int SCAN_INTERVAL = 10;
    /** Ticks before a kind is tried again after a throw that was given up or came to nothing. */
    private static final int RETRY_DELAY = 40;
    /** Wind-up of an item without a charge time of its own, such as a potion. */
    private static final int DEFAULT_WINDUP = 6;

    private final EntityNPCInterface npc;
    /** Game time from which each kind, by ordinal, may be thrown again. */
    private final long[] readyAt = new long[Kind.values().length];
    /** The throw being wound up, if any. */
    @Nullable
    private Choice pending;
    private int windup;
    private int scan;
    /** The target the cooldowns were last staggered for. */
    @Nullable
    private LivingEntity engaged;

    public NpcThrowGoal(EntityNPCInterface npc)
    {
        this.npc = npc;
    }

    /** @return whether the NPC is winding up a throw beside its other attacks, which wait meanwhile */
    public static boolean throwing(EntityNPCInterface npc)
    {
        NpcThrowGoal goal = npc instanceof Holder holder ? holder.wolffsmodnpcsThrowGoal() : null;
        return goal != null && goal.pending != null;
    }

    @Override
    public boolean canUse()
    {
        return !npc.isKilled() && npc.getTarget() != null && (pending != null || NpcThrowing.throwsOccasionally(npc));
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (com.wolffsmod.npcs.sim.NpcRepairs.active(npc))
        {
            NpcThrowPresentation.clear(npc);
            pending = null;
            return;
        }
        LivingEntity target = npc.getTarget();
        if (target == null)
            return;
        long now = npc.level().getGameTime();
        if (target != engaged)
        {
            engaged = target;
            stagger(now);
        }
        if (pending != null)
        {
            windUp(pending, target, now);
            return;
        }
        if (--scan > 0)
            return;
        scan = SCAN_INTERVAL;
        NpcThrowing.occasionalThrow(npc, target, readyAt, now).ifPresent(choice ->
        {
            pending = choice;
            int charge = NpcEquipment.chargeTime(choice.stack(), npc);
            windup = charge > 0 ? charge : DEFAULT_WINDUP;
        });
    }

    private void windUp(Choice choice, LivingEntity target, long now)
    {
        if (!NpcThrowing.stillWorthThrowing(npc, target, choice))
        {
            giveUp(now);
            return;
        }
        npc.getLookControl().setLookAt(target, 30F, 30F);
        boolean animated = NpcEquipment.enabled(npc, Feature.WEAPON_ANIMATIONS);
        if (animated)
            NpcThrowPresentation.show(npc, choice.stack());
        else
            NpcThrowPresentation.clear(npc);
        if (--windup > 0)
            return;
        pending = null;
        boolean thrown = NpcItemAttacks.throwCarried(npc, target, choice.stack());
        NpcThrowPresentation.clear(npc);
        readyAt[choice.kind().ordinal()] = now + (thrown ? NpcThrowing.cooldown(npc, choice.kind()) : RETRY_DELAY);
        if (!thrown)
            return;
        if (animated)
            npc.swing(InteractionHand.MAIN_HAND);
        npc.updateClient = true;
        EventHooks.onNPCRangedLaunched(npc, new NpcEvent.RangedLaunchedEvent(npc.wrappedNPC, target, npc.stats.ranged.getStrength()));
    }

    private void giveUp(long now)
    {
        if (pending == null)
            return;
        readyAt[pending.kind().ordinal()] = now + RETRY_DELAY;
        pending = null;
        NpcThrowPresentation.clear(npc);
    }

    private void stagger(long now)
    {
        for (Kind kind : Kind.values())
        {
            int cooldown = NpcThrowing.cooldown(npc, kind);
            if (cooldown > 0)
                readyAt[kind.ordinal()] = Math.max(readyAt[kind.ordinal()], now + npc.getRandom().nextInt(cooldown / 2 + 1));
        }
    }

    @Override
    public void stop()
    {
        giveUp(npc.level().getGameTime());
        engaged = null;
    }

    /** Implemented on the Custom NPCs entity, which keeps the goal it last set up. */
    public interface Holder
    {
        @Nullable
        NpcThrowGoal wolffsmodnpcsThrowGoal();
    }
}
