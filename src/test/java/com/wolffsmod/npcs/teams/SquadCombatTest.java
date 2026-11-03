package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SquadCombatTest
{
    private static SquadCombat.Member member(int id, boolean ready, boolean moving, boolean reloading)
    {
        return new SquadCombat.Member(new UUID(0L, id), new Vec3(id, 64D, 0D), 10, ready, moving, reloading, false);
    }

    @Test
    void oneSoldierBoundsWhileTheOtherKeepsCoveringAndRolesReleaseOnArrival()
    {
        SquadCombat combat = new SquadCombat();
        var first = member(1, true, false, false);
        var second = member(2, true, false, false);
        List<SquadCombat.Member> members = List.of(first, second);
        assertFalse(combat.covers(first, members, 0L));
        assertTrue(combat.covers(second, members, 0L));
        var moving = member(1, false, true, false);
        members = List.of(moving, second);
        assertFalse(combat.covers(moving, members, 1L));
        assertTrue(combat.covers(second, members, 20L));
        assertFalse(combat.covers(second, List.of(first, second), 21L), "arrival frees the next soldier to move");
    }

    @Test
    void aLoadedAllyCoversReloadsButEmptyOrDistantSoldiersDoNotDeadlockEachOther()
    {
        SquadCombat combat = new SquadCombat();
        var reloading = member(1, false, false, true);
        var loaded = member(2, true, false, false);
        assertTrue(combat.covers(loaded, List.of(reloading, loaded), 0L));
        assertFalse(combat.covers(reloading, List.of(reloading, loaded), 0L));
        var alsoReloading = member(2, false, false, true);
        assertFalse(new SquadCombat().covers(reloading, List.of(reloading, alsoReloading), 0L));
        assertFalse(new SquadCombat().covers(loaded, List.of(loaded), 0L));
        assertFalse(new SquadCombat().covers(loaded, List.of(loaded, member(50, false, true, true)), 0L));
    }

    @Test
    void plansThatNeverStartMovingExpireAndLossOfTheShooterReleasesTheLease()
    {
        SquadCombat combat = new SquadCombat();
        var first = member(1, true, false, false);
        var second = member(2, true, false, false);
        assertFalse(combat.covers(first, List.of(first, second), 0L));
        assertTrue(combat.covers(second, List.of(first, second), 1L));
        assertFalse(combat.covers(second, List.of(first, second), 10L));
        assertFalse(combat.covers(first, List.of(first), 11L));
    }

    @Test
    void machineGunCoversAssaultEvenWhenItAsksFirstAndFollowsOnceAssaultIsAtItsOrder()
    {
        SquadCombat combat = new SquadCombat();
        var mg = new SquadCombat.Member(new UUID(0L, 1), Vec3.ZERO, 10, true, false, false, false, BotSettings.ClassRole.SUPPORT, true);
        var assault = new SquadCombat.Member(new UUID(0L, 2), new Vec3(2D, 0D, 0D), 10, true, false, false, false, BotSettings.ClassRole.ASSAULT, true);
        assertTrue(combat.covers(mg, List.of(mg, assault), 0L));
        assertFalse(combat.covers(assault, List.of(mg, assault), 0L));
        var arrived = new SquadCombat.Member(assault.id(), assault.position(), 10, true, false, false, false, BotSettings.ClassRole.ASSAULT, false);
        assertFalse(combat.covers(mg, List.of(mg, arrived), 10L), "an assault soldier at its order must not keep the MG waiting forever");
    }

    @Test
    void anAssaultSoldierWhoNeverStartsCannotKeepTheMachineGunWaitingForever()
    {
        SquadCombat combat = new SquadCombat();
        var mg = new SquadCombat.Member(new UUID(0L, 1), Vec3.ZERO, 10, true, false, false, false, BotSettings.ClassRole.SUPPORT, true);
        var blocked = new SquadCombat.Member(new UUID(0L, 2), new Vec3(2D, 0D, 0D), 10, true, false, false, false, BotSettings.ClassRole.ASSAULT, true);
        assertTrue(combat.covers(mg, List.of(mg, blocked), 0L));
        assertFalse(combat.covers(mg, List.of(mg, blocked), 10L));
    }

    @Test
    void pinnedAssaultCannotReserveAMoveAndKeepSupportWaiting()
    {
        SquadCombat combat = new SquadCombat();
        var mg = new SquadCombat.Member(new UUID(0L, 1), Vec3.ZERO, 10, true, false, false, false, BotSettings.ClassRole.SUPPORT, true);
        var pinned = new SquadCombat.Member(new UUID(0L, 2), new Vec3(2D, 0D, 0D), 10, false, false, false, true, BotSettings.ClassRole.ASSAULT, true);
        assertFalse(combat.covers(mg, List.of(mg, pinned), 0L));
    }

    @Test
    void coverCannotBeDoubleBookedAndExpiresOnDeathTargetChangeOrTimeout()
    {
        SquadCombat combat = new SquadCombat();
        var first = member(1, true, false, false);
        var second = member(2, true, false, false);
        List<SquadCombat.Member> members = List.of(first, second);
        BlockPos cover = new BlockPos(10, 64, 0);
        assertTrue(combat.claim(first, cover, members, 0L));
        assertTrue(combat.claim(first, cover, members, 1L), "the owner may renew its reservation");
        assertFalse(combat.claim(second, cover, members, 2L));
        assertFalse(combat.available(second.id(), cover.east(), members, 2L), "nearby stands are also kept apart");
        assertTrue(combat.available(second.id(), cover, List.of(second), 3L), "dead and unloaded members release cover");
        assertTrue(combat.claim(first, cover, members, 4L));
        assertTrue(combat.available(second.id(), cover, members, 104L));
        assertTrue(combat.claim(first, cover, members, 105L));
        var changed = new SquadCombat.Member(first.id(), first.position(), 20, true, false, false, false);
        assertTrue(combat.available(second.id(), cover, List.of(changed, second), 106L));
        assertTrue(combat.claim(first, cover, members, 107L));
        combat.release(first.id());
        assertTrue(combat.available(second.id(), cover, members, 108L));
    }
}
