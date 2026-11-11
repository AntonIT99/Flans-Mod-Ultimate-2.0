package com.wolffsmod.npcs.tactics;

import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.TacticKind;
import com.wolffsmod.npcs.tactics.NpcCover.Cover;
import com.wolffsmod.npcs.tactics.NpcTacticState.Phase;
import com.wolffsmod.npcs.tactics.TacticDecision.*;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TacticDecisionTest
{
    private static final Engagement RIFLE = Engagement.of(64D, 0, WeaponRole.GENERAL);
    private static final Vec3 TARGET = Vec3.ZERO;

    @Test
    void anAutomaticDistanceSuitsTheWeaponAndAnyDistanceStaysWithinTheFiringRange()
    {
        assertEquals(64D * WeaponRole.GENERAL.share(), RIFLE.preferred(), 1.0E-9D);
        assertEquals(64D * WeaponRole.CLOSE.share(), Engagement.of(64D, 0, WeaponRole.CLOSE).preferred(), 1.0E-9D);
        assertEquals(64D * WeaponRole.MARKSMAN.share(), Engagement.of(64D, 0, WeaponRole.MARKSMAN).preferred(), 1.0E-9D);
        assertEquals(Engagement.MIN_AUTOMATIC, Engagement.of(5D, 0, WeaponRole.GENERAL).preferred(), 1.0E-9D);
        assertEquals(20D, Engagement.of(64D, 20, WeaponRole.CLOSE).preferred(), 1.0E-9D);
        assertEquals(15D, Engagement.of(15D, 40, WeaponRole.GENERAL).preferred(), 1.0E-9D);
        assertEquals(3D, Engagement.of(3D, 0, WeaponRole.GENERAL).preferred(), 1.0E-9D);
        Engagement band = Engagement.of(64D, 20, WeaponRole.GENERAL);
        assertEquals(15D, band.near(), 1.0E-9D);
        assertEquals(24D, band.far(), 1.0E-9D);
        assertEquals(20D, Engagement.of(20D, 20, WeaponRole.GENERAL).far(), 1.0E-9D, "never beyond the firing range");
    }

    @Test
    void anAdvancingNpcRunsWhileFarOffWalksTheRestAndStandsOnceCloseWithTheTargetInSight()
    {
        double preferred = RIFLE.preferred();
        assertEquals(Order.closeIn(TacticDecision.RUN), decide(Tactic.ADVANCE, at(preferred * 2D + 1D), true));
        assertEquals(Order.closeIn(TacticDecision.WALK), decide(Tactic.ADVANCE, at(preferred + 1D), true));
        assertEquals(Move.STAND, decide(Tactic.ADVANCE, at(preferred), true).move());
        assertEquals(Order.closeIn(TacticDecision.WALK), decide(Tactic.ADVANCE, at(preferred / 2D), false), "out of sight it keeps closing in");
    }

    @Test
    void aHoldingNpcStandsWhereItWasPlacedAndGoesBackWhenPushedAway()
    {
        Vec3 home = at(20D);
        assertEquals(Move.STAND, TacticDecision.decide(Tactic.HOLD, situation(home.add(TacticDecision.HOLD_RADIUS, 0D, 0D), true, home, false, false), new NpcTacticState(), 0L).move());
        Order back = TacticDecision.decide(Tactic.HOLD, situation(home.add(5D, 0D, 0D), true, home, false, false), new NpcTacticState(), 0L);
        assertEquals(Move.GO_TO, back.move());
        assertEquals(home, back.point());
        assertEquals(Move.STAND, TacticDecision.decide(Tactic.HOLD, situation(at(500D), false, at(500D), false, false), new NpcTacticState(), 0L).move(),
            "a target out of range or sight does not draw it away");
    }

    @Test
    void aStandOffNpcBacksAwayFromACloseTargetAndFollowsADistantOne()
    {
        Engagement band = RIFLE;
        Order back = decide(Tactic.STAND_OFF, at(band.near() - 2D), true);
        assertEquals(Move.GO_TO, back.move());
        assertTrue(back.point().x > band.near() - 2D, "it backs away from the target");
        assertEquals(Move.STAND, decide(Tactic.STAND_OFF, at(band.preferred()), true).move());
        assertEquals(Move.CLOSE_IN, decide(Tactic.STAND_OFF, at(band.far() + 1D), true).move());
        assertEquals(Move.CLOSE_IN, decide(Tactic.STAND_OFF, at(band.preferred()), false).move());
    }

    @Test
    void aCoverNpcHidesStepsOutOnceReadyAndDucksBackToReloadOrWhenHit()
    {
        NpcTacticState memory = new NpcTacticState();
        Cover cover = new Cover(new BlockPos(30, 0, 0), new BlockPos(30, 0, 2));
        memory.cover(cover, 0L);
        Vec3 inCover = cover.standPoint();
        assertEquals(Move.STAND, coverOrder(memory, inCover, false, false, 0L).move());
        assertEquals(Phase.HIDE, memory.getPhase());
        assertEquals(Move.GO_TO, coverOrder(memory, inCover, false, false, TacticDecision.HIDE_TICKS).move());
        assertEquals(Phase.PEEK, memory.getPhase());
        assertEquals(cover.peekPoint(), coverOrder(memory, inCover, false, false, TacticDecision.HIDE_TICKS + 1L).point());
        coverOrder(memory, cover.peekPoint(), true, false, TacticDecision.HIDE_TICKS + 2L);
        assertEquals(Phase.HIDE, memory.getPhase(), "a reload sends it back in");
        assertEquals(Phase.HIDE, peekAndGetHit(memory, cover).getPhase());
    }

    @Test
    void aCoverNpcStepsIntoAPeekSpotTheNextBlockOverAndBackIntoCover()
    {
        NpcTacticState memory = new NpcTacticState();
        Cover cover = new Cover(new BlockPos(30, 0, 0), new BlockPos(30, 0, 1));
        memory.cover(cover, 0L);
        coverOrder(memory, cover.standPoint(), false, false, 0L);
        Order out = coverOrder(memory, cover.standPoint(), false, false, TacticDecision.HIDE_TICKS);
        assertEquals(Phase.PEEK, memory.getPhase());
        assertEquals(new Order(Move.GO_TO, TacticDecision.WALK, cover.peekPoint()), out, "the cover block is not the peek spot");
        assertEquals(Move.STAND, coverOrder(memory, cover.peekPoint(), false, false, TacticDecision.HIDE_TICKS + 1L).move());
        Order back = coverOrder(memory, cover.peekPoint(), true, false, TacticDecision.HIDE_TICKS + 2L);
        assertEquals(new Order(Move.GO_TO, TacticDecision.WALK, cover.standPoint()), back, "the peek spot is not cover");
    }

    @Test
    void aCoverNpcWithoutCoverAdvances()
    {
        assertEquals(Order.closeIn(TacticDecision.WALK), decide(Tactic.COVER, at(RIFLE.preferred() + 1D), true));
    }

    @Test
    void aBoundingNpcFiresStandingThenDashesCloserAndStandsOnceCloseEnough()
    {
        NpcTacticState memory = new NpcTacticState();
        Vec3 far = at(RIFLE.preferred() + 20D);
        assertEquals(Move.STAND, TacticDecision.decide(Tactic.BOUNDING, situation(far, true, far, false, false), memory, 0L).move());
        Order dash = TacticDecision.decide(Tactic.BOUNDING, situation(far, true, far, false, false), memory, TacticDecision.FIRE_TICKS);
        assertEquals(Move.GO_TO, dash.move());
        assertEquals(TacticDecision.DASH_LENGTH, far.x - dash.point().x, 1.0E-9D, "a dash goes the dash length toward the target");
        Order arrived = TacticDecision.decide(Tactic.BOUNDING, situation(dash.point(), true, far, false, false), memory, TacticDecision.FIRE_TICKS + 10L);
        assertEquals(Move.STAND, arrived.move());
        assertEquals(Phase.FIRE, memory.getPhase());
        assertEquals(Move.GO_TO, TacticDecision.decide(Tactic.BOUNDING, situation(dash.point(), true, far, true, false), memory, TacticDecision.FIRE_TICKS + 11L).move(), "a reload is spent dashing");
        assertEquals(Move.STAND, decide(Tactic.BOUNDING, at(RIFLE.preferred()), true).move());
    }

    @Test
    void aFlankingNpcSwingsOutToItsSideAtItsDistanceAndThenAdvances()
    {
        NpcTacticState memory = new NpcTacticState();
        Vec3 start = at(60D);
        Order swing = TacticDecision.decide(Tactic.FLANK, situation(start, true, start, false, false), memory, 0L);
        assertEquals(Move.GO_TO, swing.move());
        assertEquals(RIFLE.preferred(), TacticDecision.horizontal(TARGET, swing.point()), 1.0E-9D);
        assertEquals(TacticDecision.FLANK_ANGLE, Math.abs(TacticDecision.bearing(TARGET, swing.point())), 1.0E-9D);
        Order attack = TacticDecision.decide(Tactic.FLANK, situation(swing.point(), true, start, false, false), memory, 20L);
        assertEquals(Phase.ATTACK, memory.getPhase());
        assertEquals(Move.STAND, attack.move());
    }

    @Test
    void aCirclingNpcMovesOnRoundItsTargetEachTimeItReachesAPoint()
    {
        NpcTacticState memory = new NpcTacticState();
        Vec3 start = at(RIFLE.preferred());
        Order first = TacticDecision.decide(Tactic.CIRCLE, situation(start, true, start, false, false), memory, 0L);
        Order second = TacticDecision.decide(Tactic.CIRCLE, situation(first.point(), true, start, false, false), memory, 20L);
        assertEquals(RIFLE.preferred(), TacticDecision.horizontal(TARGET, second.point()), 1.0E-9D);
        assertEquals(2D * TacticDecision.CIRCLE_STEP, Math.abs(TacticDecision.bearing(TARGET, second.point())), 1.0E-9D);
    }

    @Test
    void aStrafingHelicopterSlipsAcrossTheLineToItsTargetAndTurnsBack()
    {
        NpcTacticState memory = new NpcTacticState();
        Vec3 start = at(RIFLE.preferred());
        Order slip = TacticDecision.decide(Tactic.STRAFE, situation(start, true, start, false, false), memory, 0L);
        assertEquals(Move.GO_TO, slip.move());
        assertEquals(start.x, slip.point().x, 1.0E-9D);
        assertEquals(TacticDecision.STRAFE_STEP, Math.abs(slip.point().z), 1.0E-9D);
        int side = memory.getSide();
        TacticDecision.decide(Tactic.STRAFE, situation(slip.point(), true, start, false, false), memory, 10L);
        assertEquals(-side, memory.getSide(), "it turns back once it has slipped across");
    }

    @Test
    void tacticsThatAreNotFollowedAlongAPathAreNotDecidedHere()
    {
        Situation situation = situation(at(10D), true, at(10D), false, false);
        NpcTacticState memory = new NpcTacticState();
        for (Tactic tactic : new Tactic[]{Tactic.CUSTOM_NPCS, Tactic.STRAFING_RUN, Tactic.ORBIT})
            assertThrows(IllegalArgumentException.class, () -> TacticDecision.decide(tactic, situation, memory, 0L), tactic.name());
    }

    @Test
    void aTacticNeedsAnNpcThatFightsAtRangeAndAKindItIsWrittenFor()
    {
        assertEquals(Tactic.ADVANCE, NpcTactics.resolve(Tactic.ADVANCE, TacticKind.STANDARD, true));
        assertEquals(Tactic.CUSTOM_NPCS, NpcTactics.resolve(Tactic.ADVANCE, TacticKind.STANDARD, false));
        assertEquals(Tactic.CUSTOM_NPCS, NpcTactics.resolve(Tactic.COVER, TacticKind.VEHICLE, true));
        assertEquals(Tactic.CIRCLE, NpcTactics.resolve(Tactic.CIRCLE, TacticKind.VEHICLE, true));
        assertEquals(Tactic.STRAFING_RUN, NpcTactics.resolve(Tactic.STRAFING_RUN, TacticKind.PLANE, true));
        for (TacticKind kind : TacticKind.values())
            assertTrue(Tactic.CUSTOM_NPCS.appliesTo(kind), kind.name());
        assertFalse(NpcTactics.followsPath(Tactic.CUSTOM_NPCS));
        assertFalse(NpcTactics.followsPath(Tactic.ORBIT));
        assertTrue(NpcTactics.followsPath(Tactic.STRAFE));
    }

    @Test
    void theTransportTacticIsTheOneAVehicleWithoutAWeaponKeeps()
    {
        assertEquals(Tactic.TRANSPORT, NpcTactics.resolve(Tactic.TRANSPORT, TacticKind.VEHICLE, false), "it needs nothing to fight at range with");
        assertEquals(Tactic.TRANSPORT, NpcTactics.resolve(Tactic.TRANSPORT, TacticKind.VEHICLE, true), "an armed vehicle may be given it too");
        assertEquals(Tactic.CUSTOM_NPCS, NpcTactics.resolve(Tactic.TRANSPORT, TacticKind.STANDARD, false), "but it is written for vehicles only");
        // Every other tactic still needs a weapon, so an unarmed NPC keeps Custom NPCs' own movement.
        assertEquals(Tactic.CUSTOM_NPCS, NpcTactics.resolve(Tactic.STAND_OFF, TacticKind.VEHICLE, false));
        assertTrue(NpcTactics.followsPath(Tactic.TRANSPORT));
        assertTrue(Tactic.TRANSPORT.usesDistance());
    }

    @Test
    void aTransportBringsItsTroopsUpToItsDistanceAndStopsThere()
    {
        Engagement unloading = unloading();
        assertEquals(NpcTactics.UNLOAD_DISTANCE, unloading.preferred(), 1.0E-9D);
        assertEquals(Order.closeIn(TacticDecision.RUN), decide(Tactic.TRANSPORT, at(97D), true, unloading), "runs while far off");
        assertEquals(Order.closeIn(TacticDecision.WALK), decide(Tactic.TRANSPORT, at(60D), true, unloading));
        assertEquals(Order.STAND, decide(Tactic.TRANSPORT, at(48D), true, unloading), "stops at its distance");
        assertEquals(Order.STAND, decide(Tactic.TRANSPORT, at(40D), true, unloading), "and stays within the band");
    }

    @Test
    void aTransportDoesNotCarryItsTroopsIntoTheFightMerelyToSeeTheTarget()
    {
        Engagement unloading = unloading();
        // A tactic that fires closes in while it has no line of sight; a transport has nothing to shoot with, so it holds.
        assertEquals(Order.closeIn(TacticDecision.WALK), decide(Tactic.STAND_OFF, at(40D), false, unloading));
        assertEquals(Order.STAND, decide(Tactic.TRANSPORT, at(40D), false, unloading));
    }

    @Test
    void aTransportBacksAwayWhenTheTargetClosesOnIt()
    {
        Order order = decide(Tactic.TRANSPORT, at(10D), true, unloading());
        assertEquals(TacticDecision.Move.GO_TO, order.move());
        assertNotNull(order.point());
        assertTrue(order.point().distanceTo(TARGET) > 10D, "it goes away from the target, not toward it");
    }

    /** @return the distances a transport works with when its tactic distance is left automatic */
    private static Engagement unloading()
    {
        return Engagement.of(NpcTactics.UNLOAD_DISTANCE, NpcTactics.UNLOAD_DISTANCE, WeaponRole.GENERAL);
    }

    private static NpcTacticState peekAndGetHit(NpcTacticState memory, Cover cover)
    {
        long now = memory.getPhaseStarted() + TacticDecision.HIDE_TICKS;
        coverOrder(memory, cover.standPoint(), false, false, now);
        assertEquals(Phase.PEEK, memory.getPhase());
        coverOrder(memory, cover.peekPoint(), false, true, now + 1L);
        return memory;
    }

    private static Order coverOrder(NpcTacticState memory, Vec3 position, boolean reloading, boolean hit, long now)
    {
        return TacticDecision.decide(Tactic.COVER, situation(position, true, position, reloading, hit), memory, now);
    }

    @Test
    void anNpcWhoseOwnRoundsWouldCatchItNeverFightsFromInsideTheirReach()
    {
        // A launcher reaching 10 blocks keeps its distance band out beyond that, whatever the tactic distance says.
        Engagement close = Engagement.of(64D, 6, WeaponRole.LAUNCHER, 10D);
        assertEquals(10D, close.minimum(), 1.0E-9D);
        assertEquals(10D, close.preferred(), 1.0E-9D, "it stands off at least as far as its own blast reaches");
        assertEquals(10D, close.near(), 1.0E-9D);
        assertTrue(close.far() >= close.near());
        // A blast that outreaches the gun leaves the NPC standing at the edge of its range rather than unable to fight.
        assertEquals(12D, Engagement.of(12D, 0, WeaponRole.GENERAL, 30D).minimum(), 1.0E-9D);
    }

    @Test
    void anNpcInsideThatReachBacksAwayWhateverItsTacticIs()
    {
        Engagement blast = Engagement.of(64D, 0, WeaponRole.LAUNCHER, 20D);
        Order advance = decide(Tactic.ADVANCE, at(8D), true, blast);
        assertEquals(Move.GO_TO, advance.move(), "it does not stand and fire from inside its own blast");
        assertNotNull(advance.point());
        assertTrue(TacticDecision.horizontal(advance.point(), TARGET) > 8D, "the point it goes to lies further out");
        // Out of sight it still opens the range first rather than closing in blindly.
        assertEquals(Move.GO_TO, decide(Tactic.STAND_OFF, at(8D), false, blast).move());
        assertEquals(Move.STAND, decide(Tactic.ADVANCE, at(30D), true, blast).move());
    }

    @Test
    void aChargingNpcRunsAtItsTargetWhereverItIsAndNeverStandsToFire()
    {
        for (double distance : new double[] {RIFLE.preferred() * 3D, RIFLE.preferred(), 2D})
        {
            assertEquals(Order.closeIn(TacticDecision.RUN), decide(Tactic.CHARGE, at(distance), true), "in sight at " + distance);
            assertEquals(Order.closeIn(TacticDecision.RUN), decide(Tactic.CHARGE, at(distance), false), "out of sight at " + distance);
        }
        assertFalse(Tactic.CHARGE.usesDistance());
        assertEquals(Tactic.CHARGE, NpcTactics.resolve(Tactic.CHARGE, TacticKind.STANDARD, true));
        assertEquals(Tactic.CUSTOM_NPCS, NpcTactics.resolve(Tactic.CHARGE, TacticKind.VEHICLE, true), "it is written for soldiers only");
    }

    @Test
    void anAssaultingNpcRunsForItsObjectiveFightsWhatItMeetsAndHoldsTheObjective()
    {
        Vec3 objective = at(200D);
        Vec3 position = at(400D);
        Order onward = TacticDecision.decide(Tactic.ASSAULT, situation(position, true, objective, false, false), new NpcTacticState(), 0L);
        assertEquals(new Order(Move.GO_TO, TacticDecision.RUN, objective), onward, "a target in sight beyond its tactic distance does not stop it");
        Vec3 close = at(RIFLE.preferred());
        assertEquals(Move.STAND, TacticDecision.decide(Tactic.ASSAULT, situation(close, true, objective, false, false), new NpcTacticState(), 0L).move(), "it fights an enemy it meets");
        assertEquals(Move.GO_TO, TacticDecision.decide(Tactic.ASSAULT, situation(close, false, objective, false, false), new NpcTacticState(), 0L).move(), "one out of sight it passes");
        Vec3 there = objective.add(TacticDecision.HOLD_RADIUS, 0D, 0D);
        assertEquals(Move.STAND, TacticDecision.decide(Tactic.ASSAULT, situation(there, false, objective, false, false), new NpcTacticState(), 0L).move(), "it holds the objective");
        assertTrue(Tactic.ASSAULT.usesDistance());
        assertEquals(Tactic.ASSAULT, NpcTactics.resolve(Tactic.ASSAULT, TacticKind.VEHICLE, true));
    }

    @Test
    void anAssaultRouteGoesOnPastPointsReachedNeverBackAndStartsFromTheNearestAfterAReload()
    {
        List<Vec3> route = List.of(at(0D), at(20D), at(40D), at(60D));
        assertEquals(1, NpcAssault.advance(at(0D), route, -1), "from where it was placed it heads for the second point");
        assertEquals(1, NpcAssault.advance(at(10D), route, 1));
        assertEquals(2, NpcAssault.advance(at(20D - NpcAssault.REACHED), route, 1), "it moves on before it would stand at a point on the way");
        assertEquals(2, NpcAssault.advance(at(5D), route, 2), "pushed back, it still heads on");
        assertEquals(2, NpcAssault.advance(at(35D), route, -1), "a reloaded NPC starts from the point nearest it");
        assertEquals(3, NpcAssault.advance(at(60D), route, 3), "the last point it keeps");
        assertTrue(NpcAssault.REACHED > TacticDecision.HOLD_RADIUS);
    }

    @Test
    void anAssaultRouteIsForgottenWhenThePathChanges()
    {
        NpcTacticState memory = new NpcTacticState();
        assertEquals(-1, memory.assaultIndex(7));
        memory.assault(7, 2);
        assertEquals(2, memory.assaultIndex(7));
        memory.prepare(Tactic.ASSAULT, 3, 0L, 1);
        assertEquals(2, memory.assaultIndex(7), "a new fight does not send it back along its route");
        assertEquals(-1, memory.assaultIndex(8));
    }

    private static Order decide(Tactic tactic, Vec3 position, boolean inSight)
    {
        return TacticDecision.decide(tactic, situation(position, inSight, position, false, false), new NpcTacticState(), 0L);
    }

    private static Order decide(Tactic tactic, Vec3 position, boolean inSight, Engagement engagement)
    {
        Situation situation = new Situation(inSight, position, TARGET, position, engagement, false, false);
        return TacticDecision.decide(tactic, situation, new NpcTacticState(), 0L);
    }

    private static Situation situation(Vec3 position, boolean inSight, Vec3 home, boolean reloading, boolean hit)
    {
        return new Situation(inSight, position, TARGET, home, RIFLE, reloading, hit);
    }

    /** @return a point the given blocks from the target along x */
    private static Vec3 at(double distance)
    {
        return new Vec3(distance, 0D, 0D);
    }
}
