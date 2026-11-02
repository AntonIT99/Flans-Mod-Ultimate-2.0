package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotCommanderIntelligenceTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void commanderDoesNotDiscoverUnseenEnemiesOrTrackLostContacts()
    {
        MinecraftServer server = mock(MinecraftServer.class);
        ServerLevel level = mock(ServerLevel.class);
        Battleground ground = mock(Battleground.class);
        when(ground.sideCount()).thenReturn(2);
        EntityNPCInterface observer = mock(EntityNPCInterface.class);
        LivingEntity enemy = mock(LivingEntity.class);
        UUID observerId = UUID.randomUUID();
        UUID enemyId = UUID.randomUUID();
        when(observer.getUUID()).thenReturn(observerId);
        when(enemy.getUUID()).thenReturn(enemyId);
        when(observer.level()).thenReturn(level);
        when(enemy.level()).thenReturn(level);
        when(observer.isAlive()).thenReturn(true);
        when(enemy.isAlive()).thenReturn(true);
        when(observer.getTarget()).thenReturn(enemy);
        Sensing sensing = mock(Sensing.class);
        when(observer.getSensing()).thenReturn(sensing);
        when(ground.areEnemies(observer, enemy)).thenReturn(true);
        var team = mock(com.flansmodultimate.api.teams.ITeam.class);
        when(ground.team("us")).thenReturn(java.util.Optional.of(team));
        when(ground.sideOf(team)).thenReturn(0);
        BotBattle battle = new BotBattle(UUID.randomUUID(), ground, new BotSettings(), Map.of());
        BotSlot slot = battle.newSlot("us", BotSlot.Source.INFANTRY, -1);
        slot.setEntity(observerId);
        battle.getSlotOfEntity().put(observerId, slot.getId());
        when(level.getEntity(observerId)).thenReturn(observer);
        AtomicLong now = new AtomicLong();
        when(level.getGameTime()).thenAnswer(call -> now.get());
        BotBattles.add(battle);
        try (var commanders = mockStatic(CommanderBots.class); var targeting = mockStatic(BotTargeting.class))
        {
            commanders.when(() -> CommanderBots.disrupted(battle, 0, 0L)).thenReturn(false);
            targeting.when(() -> BotTargeting.armoured(battle, enemy)).thenReturn(true);
            BotCommander.plan(server, level, battle);
            assertTrue(battle.getIntelligence().contacts(0, 0L).isEmpty());
            verify(enemy, never()).position();

            Vec3 reported = new Vec3(80D, 64D, 0D);
            when(enemy.position()).thenReturn(reported);
            when(sensing.hasLineOfSight(enemy)).thenReturn(true);
            now.set(40L);
            BotCommander.plan(server, level, battle);
            assertEquals(reported, battle.getIntelligence().contacts(0, 40L).get(0).position());
            assertTrue(battle.getIntelligence().contacts(1, 40L).isEmpty());

            when(enemy.position()).thenReturn(new Vec3(160D, 64D, 0D));
            when(sensing.hasLineOfSight(enemy)).thenReturn(false);
            now.set(80L);
            BotCommander.plan(server, level, battle);
            assertEquals(reported, battle.getIntelligence().contacts(0, 80L).get(0).position());
            now.set(640L);
            BotCommander.plan(server, level, battle);
            assertTrue(battle.getIntelligence().contacts(0, 640L).isEmpty());

            TeamsBots.reportContact(observer, enemy);
            assertEquals(160D, battle.getIntelligence().contacts(0, 640L).get(0).position().x, "fresh incoming fire can report an unseen shooter");
            battle.getIntelligence().clear();
            when(ground.dimension()).thenReturn(Level.OVERWORLD);
            when(level.dimension()).thenReturn(Level.NETHER);
            TeamsBots.reportContact(observer, enemy);
            assertTrue(battle.getIntelligence().contacts(0, 640L).isEmpty(), "teleported bots cannot send another dimension's contacts into the battle");
        }
        finally
        {
            BotBattles.remove(battle);
        }
    }
}
