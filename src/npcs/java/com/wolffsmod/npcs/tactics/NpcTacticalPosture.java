package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.teams.NpcSuppression;
import noppes.npcs.api.constants.AnimationType;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Brief, infrequent infantry postures. Owns only temporary animation state, never the saved authored animation. */
public final class NpcTacticalPosture
{
    private int animation;
    private long until;
    private long nextChoice;
    @Nullable
    private Vec3 anchor;
    private int previousAnimation;

    public void tick(EntityNPCInterface npc)
    {
        long now = npc.level().getGameTime();
        if (!eligible(npc) || animation != 0 && (now >= until || !safe(npc, animation)))
        {
            clear(npc, now);
            return;
        }
        if (animation != 0 || now < nextChoice)
            return;
        nextChoice = now + 40L;
        // Most opportunities pass without a posture change; after each actual use there is a longer cooldown.
        if (npc.getRandom().nextFloat() >= 0.2F)
            return;
        boolean pinned = NpcSuppression.isPinned(npc);
        boolean support = NpcInfantryRole.support(npc);
        NpcSafetyState safety = NpcTacticState.of(npc).getSafety();
        boolean hiding = safety.active(now) && safety.getAction() == NpcSafetyState.Action.HIDE;
        int chosen = (support || pinned) && flat(npc) && safe(npc, AnimationType.CRAWL) ? AnimationType.CRAWL : AnimationType.CROUCH;
        if (!support && !pinned && !hiding || !safe(npc, chosen))
            return;
        animation = chosen;
        previousAnimation = npc.currentAnimation;
        until = now + (npc.getNavigation().isDone() ? 60L + npc.getRandom().nextInt(41) : 40L);
        anchor = npc.position();
        npc.setCurrentAnimation(animation);
        npc.refreshDimensions();
        npc.setSprinting(false);
    }

    private boolean eligible(EntityNPCInterface npc)
    {
        LivingEntity target = npc.getTarget();
        return npc.isAlive() && !npc.isKilled() && !npc.isNoAi() && !npc.isPassenger() && npc.onGround() && !npc.isInWater() && !npc.isInLava() && TacticKind.of(npc) == TacticKind.STANDARD
            && NpcEquipment.enabled(npc, Feature.TACTICAL_POSTURES) && NpcEquipmentRangedGoal.engages(npc) && npc.ais.animationType == 0 && npc.animateAi != null && npc.animateAi.temp == 0
            && (npc.currentAnimation == 0 || npc.currentAnimation == AnimationType.AIM || npc.currentAnimation == animation) && target != null && target.isAlive() && target.level() == npc.level()
            && !npc.isInRange(target, 8D);
    }

    private boolean safe(EntityNPCInterface npc, int chosen)
    {
        long now = npc.level().getGameTime();
        NpcSafetyState safety = NpcTacticState.of(npc).getSafety();
        if (safety.active(now))
        {
            var action = safety.getAction();
            if (action == NpcSafetyState.Action.EVADE || action == NpcSafetyState.Action.RETREAT || action == NpcSafetyState.Action.ADVANCE || action == NpcSafetyState.Action.FLANK)
                return false;
        }
        if (!safeMovement(npc, chosen, safety))
            return false;
        if (anchor != null && animation != 0 && npc.position().distanceToSqr(anchor) > 9D)
            return false;
        // Use the eye height of the actual dependency's crouch/crawl box, including the NPC's display scale.
        double height = chosen == AnimationType.CRAWL ? 0.34D : 1.2D;
        Vec3 eye = npc.position().add(0D, height * npc.display.getSize() * 0.2D, 0D);
        LivingEntity target = npc.getTarget();
        if (target == null)
            return false;
        boolean clearShot = npc.level().clip(new ClipContext(eye, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc)).getType() == HitResult.Type.MISS;
        return clearShot || safety.getAction() == NpcSafetyState.Action.HIDE && (NpcSuppression.isPinned(npc) || NpcTacticState.of(npc).isReloading(now));
    }

    private static boolean safeMovement(EntityNPCInterface npc, int chosen, NpcSafetyState safety)
    {
        if (npc.getNavigation().isDone())
            return true;
        // Crawling is limited to a short, level approach to nearby cover, never a long route or an assault bound.
        Vec3 destination = safety.getDestination();
        return safety.getAction() == NpcSafetyState.Action.HIDE && destination != null && npc.position().distanceToSqr(destination) <= 9D && Math.abs(destination.y - npc.getY()) <= 0.25D
            && (chosen != AnimationType.CRAWL || flat(npc));
    }

    private static boolean flat(EntityNPCInterface npc)
    {
        BlockPos origin = npc.blockPosition();
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (!NpcCover.standable(npc.level(), origin.offset(x, 0, z)))
                    return false;
        return true;
    }

    private void clear(EntityNPCInterface npc, long now)
    {
        if (animation == 0)
            return;
        int owned = animation;
        animation = 0;
        anchor = null;
        nextChoice = now + 400L + npc.getRandom().nextInt(301);
        if (npc.currentAnimation == owned)
        {
            npc.setCurrentAnimation(previousAnimation);
            npc.refreshDimensions();
        }
    }

    /** Keep ordinary idle/aim updates from undoing an active posture; explicit animations still take precedence. */
    public int animation(int requested)
    {
        return animation != 0 && (requested == 0 || requested == AnimationType.AIM) ? animation : requested;
    }

    public static void update(EntityNPCInterface npc)
    {
        NpcTacticState.of(npc).getPosture().tick(npc);
    }

    public static float speed(EntityNPCInterface npc, float walking)
    {
        if (npc.currentAnimation == AnimationType.CRAWL)
            return walking * 0.3F;
        return npc.currentAnimation == AnimationType.CROUCH ? walking * 0.6F : walking;
    }
}
