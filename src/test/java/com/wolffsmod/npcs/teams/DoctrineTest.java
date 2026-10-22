package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.TeamsCapturePoint;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DoctrineTest
{
    /** The planner never reads the dimension, and a dimension key needs a bootstrapped registry. */
    private static final ResourceKey<Level> WORLD = null;
    private static final int US = 0;
    private static final int GERMANY = 1;
    private static final Vec3 US_FLAG = new Vec3(0D, 66D, 0D);
    private static final Vec3 GERMAN_FLAG = new Vec3(300D, 66D, 0D);

    private static TeamsCapturePoint point(char symbol, double x, int owner, int capturing, boolean contested)
    {
        return new TeamsCapturePoint(UUID.randomUUID(), symbol, "Point " + symbol, WORLD, new Vec3(x, 64D, 0D), 6, owner, capturing, owner < 0 ? 0F : 1F, contested, true);
    }

    private static Doctrine.SquadView squad(int id, BotRole role, double x)
    {
        return new Doctrine.SquadView(id, role, SquadTemplate.Posture.BALANCED, new Vec3(x, 64D, 0D), 4, false, null);
    }

    @Test
    void withoutObjectivesSquadsHuntTheNearestEnemy()
    {
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(new Vec3(100D, 64D, 0D), new Vec3(-30D, 64D, 0D)), List.of(), List.of(), List.of());
        Map<Integer, SquadTask> tasks = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 0D)));
        assertEquals(SquadTask.Kind.HUNT, tasks.get(1).kind());
        assertEquals(-30D, tasks.get(1).target().x);
    }

    @Test
    void withoutEnemiesInSightSquadsHeadForTheEnemySpawn()
    {
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(), List.of(new Vec3(500D, 64D, 0D)), List.of(), List.of());
        assertEquals(500D, Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 0D))).get(1).target().x);
    }

    @Test
    void squadsSpreadOverThePointsTheyNeedToTake()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, US, -1, false), point('B', 100D, -1, -1, false), point('C', 200D, GERMANY, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(), List.of());
        Map<Integer, SquadTask> tasks = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 100D), squad(2, BotRole.INFANTRY, 150D)));
        assertEquals(SquadTask.Kind.ATTACK, tasks.get(1).kind());
        assertEquals(100D, tasks.get(1).target().x, "the nearest point nobody holds comes first");
        assertEquals(SquadTask.Kind.ATTACK, tasks.get(2).kind());
        assertNotEquals(tasks.get(1).target(), tasks.get(2).target(), "a second squad goes elsewhere rather than crowd the first");
    }

    @Test
    void aHeldPointUnderAttackIsDefended()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, US, GERMANY, true), point('B', 300D, GERMANY, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(), List.of());
        SquadTask task = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 10D))).get(1);
        assertEquals(SquadTask.Kind.DEFEND, task.kind());
        assertEquals(0D, task.target().x);
    }

    @Test
    void assaultAttackersPushTheObjectiveInPlayAndDefendersHoldItAndTheNext()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, GERMANY, -1, false), point('B', 100D, US, -1, false), point('C', 200D, US, -1, false));
        Doctrine.Battlefield attackers = new Doctrine.Battlefield(GERMANY, GERMANY, points, List.of(), List.of(), List.of(), List.of());
        assertEquals(100D, Doctrine.plan(attackers, List.of(squad(1, BotRole.INFANTRY, 0D))).get(1).target().x, "A is taken, so B is in play");

        Doctrine.Battlefield defenders = new Doctrine.Battlefield(US, GERMANY, points, List.of(), List.of(), List.of(), List.of());
        Map<Integer, SquadTask> tasks = Doctrine.plan(defenders, List.of(squad(1, BotRole.INFANTRY, 150D), squad(2, BotRole.INFANTRY, 150D), squad(3, BotRole.INFANTRY, 150D)));
        long atFront = tasks.values().stream().filter(task -> task.target().x == 100D).count();
        assertEquals(2, atFront);
        assertTrue(tasks.values().stream().allMatch(task -> task.kind() == SquadTask.Kind.DEFEND));
    }

    @Test
    void aircraftPatrolOverTheContestedFrontAndAaGunsHold()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, -1, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(), List.of());
        Map<Integer, SquadTask> tasks = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 50D), squad(2, BotRole.PLANE, 500D), squad(3, BotRole.AA, 20D)));
        assertEquals(SquadTask.Kind.PATROL, tasks.get(2).kind());
        assertEquals(0D, tasks.get(2).target().x);
        assertEquals(SquadTask.Kind.HOLD, tasks.get(3).kind());
        assertEquals(20D, tasks.get(3).target().x);
    }

    @Test
    void shipsPatrolTheNavalPointNearestTheFront()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, -1, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(new Vec3(400D, 62D, 0D), new Vec3(40D, 62D, 0D)), List.of());
        SquadTask task = Doctrine.plan(field, List.of(squad(1, BotRole.NAVAL, 300D))).get(1);
        assertEquals(SquadTask.Kind.PATROL, task.kind());
        assertEquals(40D, task.target().x);
    }

    @Test
    void aSquadCarryingTheEnemyFlagTakesItHome()
    {
        List<Doctrine.FlagView> flags = List.of(new Doctrine.FlagView(US, US_FLAG, US_FLAG, true, -1), new Doctrine.FlagView(GERMANY, new Vec3(150D, 66D, 0D), GERMAN_FLAG, false, US));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(), List.of(), List.of(), flags);
        Doctrine.SquadView carrier = new Doctrine.SquadView(1, BotRole.INFANTRY, SquadTemplate.Posture.BALANCED, new Vec3(150D, 64D, 0D), 3, true, null);
        SquadTask task = Doctrine.plan(field, List.of(carrier)).get(1);
        assertEquals(SquadTask.Kind.ATTACK, task.kind());
        assertEquals(US_FLAG, task.target());
    }

    @Test
    void squadsRaidTheEnemyFlagGuardTheirOwnAndHuntItsThief()
    {
        Vec3 stolen = new Vec3(200D, 66D, 0D);
        List<Doctrine.FlagView> flags = List.of(new Doctrine.FlagView(US, stolen, US_FLAG, false, GERMANY), new Doctrine.FlagView(GERMANY, GERMAN_FLAG, GERMAN_FLAG, true, -1));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(), List.of(), List.of(), flags);
        Map<Integer, SquadTask> tasks = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 190D), squad(2, BotRole.INFANTRY, 0D), squad(3, BotRole.INFANTRY, 10D)));
        assertEquals(SquadTask.Kind.HUNT, tasks.get(1).kind(), "the squad nearest the thief chases it");
        assertEquals(stolen, tasks.get(1).target());
        assertTrue(tasks.values().stream().anyMatch(task -> task.kind() == SquadTask.Kind.ATTACK && task.target().equals(GERMAN_FLAG)));
        assertTrue(tasks.values().stream().anyMatch(task -> task.kind() == SquadTask.Kind.DEFEND && task.target().equals(US_FLAG)));
    }

    @Test
    void aSquadKeepsTheObjectiveItIsAlreadyHeadingForWhenAnotherIsAsGood()
    {
        List<TeamsCapturePoint> points = List.of(point('B', 100D, -1, -1, false), point('C', 200D, -1, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(), List.of());
        for (double kept : new double[]{100D, 200D})
        {
            Doctrine.SquadView squad = new Doctrine.SquadView(1, BotRole.INFANTRY, SquadTemplate.Posture.BALANCED, new Vec3(150D, 64D, 0D), 4, false, new Vec3(kept, 64D, 0D));
            assertEquals(kept, Doctrine.plan(field, List.of(squad)).get(1).target().x);
        }
    }

    @Test
    void defendersWhoCannotCaptureOnlyHoldTheirPoints()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, US, -1, false), point('B', 100D, -1, -1, false), point('C', 200D, GERMANY, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(), List.of(), List.of(), List.of(), false);
        Map<Integer, SquadTask> tasks = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 100D), squad(2, BotRole.ARMOR, 200D)));
        for (SquadTask task : tasks.values())
        {
            assertEquals(0D, task.target().x, "only the point the side holds is worth anything to it");
            assertNotEquals(SquadTask.Kind.ATTACK, task.kind());
        }
    }

    @Test
    void defendersWithoutPointsHuntTheEnemy()
    {
        List<TeamsCapturePoint> points = List.of(point('A', 0D, GERMANY, -1, false));
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, points, List.of(new Vec3(40D, 64D, 0D)), List.of(), List.of(), List.of(), false);
        assertEquals(SquadTask.Kind.HUNT, Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 0D))).get(1).kind());
    }

    @Test
    void withAFrontSquadsMeetTheEnemyThereRatherThanHuntingIt()
    {
        Vec3 front = new Vec3(150D, 64D, 0D);
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(new Vec3(300D, 64D, 0D)), List.of(), List.of(), List.of(), false, front);
        SquadTask task = Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 0D))).get(1);
        assertEquals(SquadTask.Kind.HUNT, task.kind());
        assertEquals(front, task.target());
    }

    @Test
    void anEnemyCloseByDrawsASquadOffItsWayToTheFront()
    {
        Doctrine.Battlefield field = new Doctrine.Battlefield(US, -1, List.of(), List.of(new Vec3(30D, 64D, 0D)), List.of(), List.of(), List.of(), false, new Vec3(150D, 64D, 0D));
        assertEquals(30D, Doctrine.plan(field, List.of(squad(1, BotRole.INFANTRY, 0D))).get(1).target().x);
    }
}
