package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsSpawnProtectionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void protectionSparesDamageUntilItRunsOut()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = runningRound();
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            ServerPlayer player = player();
            PlayerData data = PlayerData.getInstance(player);
            TeamsSpawnProtection.grant(player, data, 3);
            assertEquals(60, data.getSpawnProtectionTicks());
            assertTrue(TeamsSpawnProtection.spares(player, damage(null)));
            data.setSpawnProtectionTicks(0);
            assertFalse(TeamsSpawnProtection.spares(player, damage(null)));
        }
    }

    @Test
    void damageBypassingInvulnerabilityAndEndedRoundsAreNotSpared()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = runningRound();
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            ServerPlayer player = player();
            PlayerData.getInstance(player).setSpawnProtectionTicks(60);
            DamageSource kill = damage(null);
            when(kill.is(DamageTypeTags.BYPASSES_INVULNERABILITY)).thenReturn(true);
            assertFalse(TeamsSpawnProtection.spares(player, kill));
            when(manager.isRoundRunning()).thenReturn(false);
            assertFalse(TeamsSpawnProtection.spares(player, damage(null)));
        }
    }

    @Test
    void attackingSomeoneElseEndsTheAttackersProtection()
    {
        ServerPlayer attacker = player();
        ServerPlayer victim = player();
        PlayerData attackerData = PlayerData.getInstance(attacker);
        PlayerData victimData = PlayerData.getInstance(victim);
        attackerData.setSpawnProtectionTicks(60);
        victimData.setSpawnProtectionTicks(60);
        // Hurting oneself, as with one's own grenade, keeps the protection.
        TeamsSpawnProtection.attacked(attacker, damage(attacker));
        assertEquals(60, attackerData.getSpawnProtectionTicks());
        TeamsSpawnProtection.attacked(victim, damage(attacker));
        assertEquals(0, attackerData.getSpawnProtectionTicks());
        assertEquals(60, victimData.getSpawnProtectionTicks());
    }

    @Test
    void disabledProtectionGrantsNothing()
    {
        ServerPlayer player = player();
        PlayerData data = PlayerData.getInstance(player);
        TeamsSpawnProtection.grant(player, data, 0);
        assertEquals(0, data.getSpawnProtectionTicks());
        verify(player, never()).displayClientMessage(any(), anyBoolean());
    }

    private static TeamsManager runningRound()
    {
        TeamsManager manager = mock(TeamsManager.class);
        when(manager.isRoundRunning()).thenReturn(true);
        return manager;
    }

    private static ServerPlayer player()
    {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerLevel level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        return player;
    }

    private static DamageSource damage(ServerPlayer attacker)
    {
        DamageSource source = mock(DamageSource.class);
        when(source.getEntity()).thenReturn(attacker);
        return source;
    }
}
