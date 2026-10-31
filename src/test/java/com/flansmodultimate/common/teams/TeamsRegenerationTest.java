package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.parts.IFlanDamageModel;
import com.flansmodultimate.common.types.Team;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamsRegenerationTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void fullFoodHealsOnlyOneHitPointEveryFiveSeconds()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            when(manager.isRegeneration()).thenReturn(true);
            Player player = player();
            for (int tick = 1; tick <= 100; tick++)
            {
                player.tickCount = tick;
                TeamsRegeneration.tick(player);
            }
            verify(player).heal(1F);
            verify(player).causeFoodExhaustion(6F);
        }
    }

    @Test
    void disablingRegenerationRetainsOwnershipUntilTheRoundEnds()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            Player player = player();
            assertTrue(TeamsRegeneration.applies(player));
            TeamsRegeneration.tick(player);
            verify(player, never()).heal(anyFloat());
            when(manager.isRoundRunning()).thenReturn(false);
            assertFalse(TeamsRegeneration.applies(player));
        }
    }

    @Test
    void hungerAndTheMinecraftGameruleStillPreventPassiveHealing()
    {
        try (var managers = mockStatic(TeamsManager.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            when(manager.isRegeneration()).thenReturn(true);
            Player player = player();
            player.getFoodData().setFoodLevel(17);
            TeamsRegeneration.tick(player);
            player.getFoodData().setFoodLevel(20);
            player.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, null);
            TeamsRegeneration.tick(player);
            verify(player, never()).heal(anyFloat());
        }
    }

    @Test
    void addonSoldiersShareTheRateButVehicleModelsDoNot()
    {
        try (var managers = mockStatic(TeamsManager.class); var combatants = mockStatic(TeamsCombatants.class))
        {
            TeamsManager manager = mock(TeamsManager.class);
            managers.when(TeamsManager::getInstance).thenReturn(manager);
            when(manager.isRoundRunning()).thenReturn(true);
            when(manager.isRegeneration()).thenReturn(true);
            LivingEntity soldier = mock(LivingEntity.class, withSettings().extraInterfaces(IFlanDamageModel.class));
            ServerLevel world = level();
            when(soldier.level()).thenReturn(world);
            when(soldier.isAlive()).thenReturn(true);
            when(soldier.getHealth()).thenReturn(10F);
            when(soldier.getMaxHealth()).thenReturn(20F);
            Team team = mock(Team.class);
            combatants.when(() -> TeamsCombatants.getCombatantTeam(manager, soldier)).thenReturn(team);
            IFlanDamageModel model = (IFlanDamageModel) soldier;
            when(model.flansmodultimateDamageModel()).thenReturn(Optional.empty());
            TeamsRegeneration.tick(soldier);
            verify(soldier).heal(1F);
            IContentType vehicle = mock(IContentType.class);
            when(model.flansmodultimateDamageModel()).thenReturn(Optional.of(vehicle));
            assertFalse(TeamsRegeneration.applies(soldier));
        }
    }

    private static ServerLevel level()
    {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getGameRules()).thenReturn(new GameRules());
        return level;
    }

    private static Player player()
    {
        Player player = mock(Player.class);
        ServerLevel world = level();
        when(player.level()).thenReturn(world);
        when(player.isAlive()).thenReturn(true);
        when(player.getHealth()).thenReturn(10F);
        when(player.getMaxHealth()).thenReturn(20F);
        when(player.getFoodData()).thenReturn(new FoodData());
        return player;
    }
}
