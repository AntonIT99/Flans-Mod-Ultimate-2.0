package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * How steady a vehicle's guns are while it moves. A hull under way, and one still swinging round, throw a gun off its
 * mark, so rounds fired on the move scatter the faster the vehicle goes and the harder it turns. A vehicle whose guns
 * have no stabiliser stops to shoot instead: it stands for a while, fires, and drives on again, as infantry alternate
 * fire with advances, so that it never freezes short of its goal.
 *
 * <p>
 * Only something that fights from the ground or the water is unsettled this way. An aircraft's guns are bore-sighted
 * along its airframe and aimed by flying it, so its own speed through the air does not throw them off, and it has
 * nowhere to stand still.
 * </p>
 *
 * <p>
 * One instance per NPC, held by its {@link NpcVehicleState}, which both a vehicle NPC and an NPC at the wheel of a real
 * driveable read. Server thread.
 * </p>
 */
public final class NpcGunSteadiness
{
    /** Share of the spread the hull's turn accounts for at a full turn; its speed accounts for the rest. */
    private static final float TURN_SHARE = 0.5F;
    /** Degrees the hull turns in one tick counted as turning as hard as it can. */
    private static final float FULL_TURN = 4F;
    /** Blocks along the hull in one tick counted as going as fast as it matters: a brisk vehicle, about seven blocks a second. */
    private static final double FULL_SPEED = 0.35D;
    /** Steps of the moving-spread setting per degree. */
    private static final float SPREAD_STEPS = 10F;
    /** Ticks a vehicle stands to shoot before it drives on, and the fewest it then keeps moving before it may stand again. */
    private static final int FIRE_TICKS = 50;
    private static final int MOVE_TICKS = 40;

    /** Ticks it has stood to shoot, and ticks it has driven since; neither runs on while it has nothing to shoot at. */
    private int halted;
    private int rolled;

    /**
     * @return whether the vehicle stands still to shoot this tick, rather than firing on the move. It drives on again after
     *         a while whether or not its target is down, so that a standing shot never costs it its goal.
     */
    public boolean haltsToFire(EntityNPCInterface npc, NpcVehicleState state, NpcWeaponOptions options)
    {
        LivingEntity target = npc.getTarget();
        boolean inReach = target != null && target.isAlive() && target.distanceTo(npc) <= npc.stats.ranged.getRange();
        return haltsToFire(options, inReach && state.isGunAligned() && !flies(state));
    }

    /**
     * Alternates standing halts with bounds.
     *
     * @param bears
     *            whether standing still now would gain it a shot. Only something driven along the ground is asked: an
     *            aircraft has nowhere to stand, and a ship steams on.
     */
    public boolean haltsToFire(NpcWeaponOptions options, boolean bears)
    {
        if (!unstabilised(options) || !bears)
        {
            halted = 0;
            rolled = 0;
            return false;
        }
        if (halted < FIRE_TICKS)
        {
            halted++;
            rolled = 0;
            return true;
        }
        rolled++;
        if (rolled >= MOVE_TICKS)
            halted = 0;
        return false;
    }

    /** @return how far the rounds of a vehicle NPC's guns go astray for the motion of its hull now */
    public static NpcSkill.AimError spread(EntityNPCInterface npc, NpcVehicleState state, NpcWeaponOptions options)
    {
        return spread(npc, Math.abs(state.getTravel()), Math.abs(state.getTurn()), options, flies(state));
    }

    /**
     * @param speed
     *            blocks the hull moved along itself during the last tick
     * @param turn
     *            degrees the hull turned during the last tick; 0 where that is not measured, as on a real driveable, whose
     *            speed alone then unsteadies its guns
     * @param airborne
     *            whether it fights from the air, whose guns its speed does not throw off
     * @return how far the rounds of this trigger pull go astray for that motion
     */
    public static NpcSkill.AimError spread(EntityNPCInterface npc, double speed, float turn, NpcWeaponOptions options, boolean airborne)
    {
        if (!unstabilised(options) || airborne)
            return NpcSkill.AimError.NONE;
        float most = NpcTuning.of(npc).get(Value.MOVING_SPREAD) / SPREAD_STEPS;
        if (most <= 0F)
            return NpcSkill.AimError.NONE;
        float moving = (float) Mth.clamp(speed / FULL_SPEED, 0D, 1D);
        float turning = Mth.clamp(turn / FULL_TURN, 0F, 1F);
        float unsteady = Math.min(1F, moving * (1F - TURN_SHARE) + turning * TURN_SHARE);
        return NpcSkill.AimError.random(npc.getRandom(), most * unsteady);
    }

    /** Whether the rule is on for the vehicle and its guns have no stabiliser to hold them on target as it moves. */
    private static boolean unstabilised(NpcWeaponOptions options)
    {
        return options.enabled(Feature.UNSTEADY_GUNS) && !options.enabled(Feature.GUN_STABILISER);
    }

    /**
     * Whether what the NPC is drawn as fights from the air, where its guns are laid by flying rather than against the
     * ground. A ship a content pack filed as a plane sails, and shoots, as a vessel.
     */
    private static boolean flies(NpcVehicleState state)
    {
        return !state.drivesHull() && state.kind() == FlanModelKind.PLANE;
    }
}
