package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.presets.NpcPreset;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.tactics.CustomNpcTactics;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataAdvanced;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotVehiclePresetMigrationTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void savedGeneratedMachinesReceiveTheirPresetOnceAndTurretsStayStill() throws ReflectiveOperationException
    {
        for (NpcPreset preset : new NpcPreset[]{NpcPreset.VEHICLE, NpcPreset.SHIP, NpcPreset.PLANE, NpcPreset.TURRET})
        {
            EntityNPCInterface npc = npc(false);
            try (var presets = mockStatic(NpcPreset.class, CALLS_REAL_METHODS))
            {
                presets.when(() -> NpcPreset.suggested(npc)).thenReturn(preset);
                IDriveableType definition = mock(IDriveableType.class);
                when(definition.getDriveableKind())
                    .thenReturn(preset == NpcPreset.PLANE ? com.flansmodultimate.api.driveables.DriveableKind.PLANE : com.flansmodultimate.api.driveables.DriveableKind.VEHICLE);
                presets.when(() -> NpcPreset.model(npc)).thenReturn(definition);
                BotFactory.keepTrained(npc, new BotSettings());
                assertEquals(preset == NpcPreset.PLANE ? 1 : preset == NpcPreset.SHIP ? 2 : 0, npc.ais.movementType, preset.name());
                assertEquals(preset == NpcPreset.TURRET ? 0 : 1, npc.ais.getMovingType(), preset.name());
                assertEquals(3, npc.stats.spawnCycle);
                assertFalse(npc.ais.returnToStart);
                assertTrue(npc.updateAI);
                // An author's later edit is retained: upkeep is a migration, not a reset every second.
                npc.ais.movementType = 2;
                BotFactory.keepTrained(npc, new BotSettings());
                assertEquals(2, npc.ais.movementType);
            }
        }
    }

    @Test
    void handEnlistedMachinesRetainTheirAuthoredMovement() throws ReflectiveOperationException
    {
        EntityNPCInterface npc = npc(true);
        BotFactory.keepTrained(npc, new BotSettings());
        assertEquals(0, npc.ais.movementType);
        assertEquals(0, npc.ais.getMovingType());
        assertFalse(npc.updateAI);
    }

    @SuppressWarnings("java:S3011") // Mockito skips constructors; install the real final Custom NPCs data containers.
    private static EntityNPCInterface npc(boolean enlisted) throws ReflectiveOperationException
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        var ai = EntityNPCInterface.class.getField("ais");
        ai.setAccessible(true);
        // The NeoForge module restores Custom NPCs' tactical variant on DataAI through a mixin, which plain JUnit does not apply.
        DataAI data = mock(DataAI.class, withSettings().useConstructor(npc).defaultAnswer(CALLS_REAL_METHODS).extraInterfaces(CustomNpcTactics.Holder.class));
        doReturn(new CustomNpcTactics()).when((CustomNpcTactics.Holder) data).wolffsmodnpcsCustomTactics();
        ai.set(npc, data);
        var advanced = EntityNPCInterface.class.getField("advanced");
        advanced.setAccessible(true);
        advanced.set(npc, new DataAdvanced(npc));
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        when(settings.wolffsmodnpcsTuning()).thenReturn(new NpcTuning());
        when(settings.wolffsmodnpcsWeaponOptions()).thenReturn(new NpcWeaponOptions());
        when(settings.wolffsmodnpcsPresentation()).thenReturn(new NpcPresentation());
        when(settings.wolffsmodnpcsCombat()).thenReturn(new NpcCombatSettings());
        when(settings.wolffsmodnpcsTypeProperties()).thenReturn(new NpcTypeProperties());
        // Minecraft 1.21 looks attributes up by registry holder.
        when(npc.getAttribute(any())).thenAnswer(call -> new AttributeInstance(call.getArgument(0), changed ->
        {
        }));
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        when(npc.getPersistentData()).thenReturn(new CompoundTag());
        BotTag.write(npc, new BotTag.Data(UUID.randomUUID(), 1, "test", true, BotSettings.Nameplates.ALWAYS, false, enlisted));
        npc.ais.setMovingType(0);
        npc.updateAI = false;
        npc.updateClient = false;
        return npc;
    }
}
