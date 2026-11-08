package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcAmmoSharing;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.entity.EntityNPCInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotAmmoSharingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void ordinaryRiflemenSupplyVisibleAlliesWithReachAndCooldownAndNeverEnemies()
    {
        NpcDuties.forget();
        EntityNPCInterface donor = soldier();
        EntityNPCInterface ally = soldier();
        EntityNPCInterface enemy = soldier();
        ServerLevel level = mock(ServerLevel.class);
        var self = new NpcDuties.Fighter(donor, 0, BotSettings.ClassRole.RIFLEMAN, false, 0);
        var friend = new NpcDuties.Fighter(ally, 0, BotSettings.ClassRole.RIFLEMAN, false, 0);
        var foe = new NpcDuties.Fighter(enemy, 1, BotSettings.ClassRole.RIFLEMAN, false, 0);
        List<NpcDuties.Fighter> fighters = List.of(self, foe, friend);
        when(ally.distanceTo(donor)).thenReturn(8F);
        when(donor.hasLineOfSight(ally)).thenReturn(true);
        try (var equipment = mockStatic(NpcEquipment.class); var sharing = mockStatic(NpcAmmoSharing.class))
        {
            equipment.when(() -> NpcEquipment.enabled(donor, Feature.CONSUME_AMMO)).thenReturn(true);
            sharing.when(() -> NpcAmmoSharing.spare(donor, ally)).thenReturn(new ItemStack(Items.ARROW));
            sharing.when(() -> NpcAmmoSharing.transfer(donor, ally)).thenReturn(true);
            NpcDuties.serve(level, donor, self, fighters, 0L, entity -> true, entity -> false);
            assertEquals(Optional.of(ally.position()), NpcDuties.destination(donor));
            sharing.verify(() -> NpcAmmoSharing.transfer(donor, ally), never());
            NpcDuties.clearDestination(donor);
            when(ally.distanceTo(donor)).thenReturn(3F);
            when(donor.hasLineOfSight(ally)).thenReturn(false);
            NpcDuties.serve(level, donor, self, fighters, 20L, entity -> true, entity -> false);
            sharing.verify(() -> NpcAmmoSharing.transfer(donor, ally), never());
            when(donor.hasLineOfSight(ally)).thenReturn(true);
            NpcDuties.serve(level, donor, self, fighters, 40L, entity -> true, entity -> false);
            NpcDuties.serve(level, donor, self, fighters, 60L, entity -> true, entity -> false);
            sharing.verify(() -> NpcAmmoSharing.transfer(donor, ally), times(1));
            sharing.verify(() -> NpcAmmoSharing.spare(donor, enemy), never());
            equipment.when(() -> NpcEquipment.enabled(donor, Feature.CONSUME_AMMO)).thenReturn(false);
            NpcDuties.forget();
            NpcDuties.serve(level, donor, self, fighters, 80L, entity -> true, entity -> false);
            sharing.verify(() -> NpcAmmoSharing.transfer(donor, ally), times(1));
        }
        finally
        {
            NpcDuties.forget();
        }
    }

    @Test
    void battleRuleAppliesImmediatelyAndOrdinaryNpcsKeepTheirOwnSetting()
    {
        EntityNPCInterface npc = soldier();
        BotSettings settings = new BotSettings();
        BotBattle battle = new BotBattle(UUID.randomUUID(), mock(Battleground.class), settings, Map.of());
        try (var battles = mockStatic(BotBattles.class))
        {
            battles.when(() -> BotBattles.of(npc)).thenReturn(Optional.empty());
            assertTrue(TeamsBots.consumesAmmo(npc, true));
            assertFalse(TeamsBots.consumesAmmo(npc, false));
            battles.when(() -> BotBattles.of(npc)).thenReturn(Optional.of(battle));
            assertFalse(TeamsBots.consumesAmmo(npc, true));
            settings.setConsumeAmmo(true);
            assertTrue(TeamsBots.consumesAmmo(npc, false));
            settings.setConsumeAmmo(false);
            assertFalse(TeamsBots.consumesAmmo(npc, true));
        }
    }

    private static EntityNPCInterface soldier()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.getUUID()).thenReturn(UUID.randomUUID());
        when(npc.isAlive()).thenReturn(true);
        when(npc.position()).thenReturn(Vec3.ZERO);
        return npc;
    }
}
