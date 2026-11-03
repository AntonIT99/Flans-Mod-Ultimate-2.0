package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcSkill;
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
        return haltsToFire(options, inReach && state.isGunAligned());
    }

    /**
     * @param bears
     *            whether the vehicle's weapons are on a target they could fire at from where it stands
     * @return whether it stands still to shoot this tick
     */
    public boolean haltsToFire(NpcWeaponOptions options, boolean bears)
    {
        if (!steadied(options) || !bears)
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
        return spread(npc, Math.abs(state.getTravel()), Math.abs(state.getTurn()), options);
    }

    /**
     * @param speed
     *            blocks the hull moved along itself during the last tick
     * @param turn
     *            degrees the hull turned during the last tick; 0 where that is not measured, as on a real driveable, whose
     *            speed alone then unsteadies its guns
     * @return how far the rounds of this trigger pull go astray for that motion
     */
    public static NpcSkill.AimError spread(EntityNPCInterface npc, double speed, float turn, NpcWeaponOptions options)
    {
        if (!steadied(options))
            return NpcSkill.AimError.NONE;
        float most = NpcTuning.of(npc).get(Value.MOVING_SPREAD) / SPREAD_STEPS;
        if (most <= 0F)
            return NpcSkill.AimError.NONE;
        float moving = (float) Mth.clamp(speed / FULL_SPEED, 0D, 1D);
        float turning = Mth.clamp(turn / FULL_TURN, 0F, 1F);
        float unsteady = Math.min(1F, moving * (1F - TURN_SHARE) + turning * TURN_SHARE);
        return unsteady <= 0F ? NpcSkill.AimError.NONE : NpcSkill.AimError.random(npc.getRandom(), most * unsteady);
    }

    /** Whether the vehicle's motion unsettles its guns at all: the rule is on for it and its guns are not stabilised. */
    private static boolean steadied(NpcWeaponOptions options)
    {
        return options.enabled(Feature.UNSTEADY_GUNS) && !options.enabled(Feature.GUN_STABILISER);
    }
}
