package com.wolffsmod.npcs.presets;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.network.NpcPresetPacket;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import io.netty.buffer.Unpooled;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcPresetApplicationTest
{
    private final NpcTuning tuning = new NpcTuning();
    private final NpcWeaponOptions options = new NpcWeaponOptions();
    private final NpcPresentation presentation = new NpcPresentation();
    private final NpcCombatSettings combat = new NpcCombatSettings();
    private final NpcTypeProperties properties = new NpcTypeProperties();

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private EntityNPCInterface npc()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        installAI(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        when(settings.wolffsmodnpcsTuning()).thenReturn(tuning);
        when(settings.wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(settings.wolffsmodnpcsPresentation()).thenReturn(presentation);
        when(settings.wolffsmodnpcsCombat()).thenReturn(combat);
        when(settings.wolffsmodnpcsTypeProperties()).thenReturn(properties);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        return npc;
    }

    @SuppressWarnings("java:S3011") // Mockito skips construction; install the real final AI container for integration tests.
    private static void installAI(EntityNPCInterface npc)
    {
        try
        {
            var field = EntityNPCInterface.class.getField("ais");
            field.setAccessible(true);
            field.set(npc, new DataAI(npc));
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(exception);
        }
    }

    @Test
    void switchingFromAircraftToHumanClearsFlightAndHoldBehaviourWithoutTouchingAmmo()
    {
        EntityNPCInterface npc = npc();
        NpcPreset.PLANE.apply(npc);
        assertEquals(1, npc.ais.movementType);
        assertEquals(NpcTuning.Flight.PLANE, tuning.flight());
        tuning.set(Value.TACTIC, NpcTuning.Tactic.HOLD.ordinal());
        options.set(Feature.CONSUME_AMMO, true);
        NpcPreset.HUMAN.apply(npc);
        assertEquals(0, npc.ais.movementType);
        assertEquals(0, npc.ais.walkingRange);
        assertFalse(npc.ais.returnToStart);
        assertEquals(NpcTuning.Flight.NORMAL, tuning.flight());
        assertEquals(NpcTuning.Terrain.DEFAULT, tuning.terrain());
        assertEquals(NpcTuning.Tactic.CUSTOM_NPCS, tuning.tactic());
        assertTrue(options.enabled(Feature.CONSUME_AMMO));
        assertTrue(npc.ais.canSprint);
        assertEquals(1, npc.ais.doorInteract);
    }

    @Test
    void undoRestoresOnlyChangedSettingsAndTheirInheritanceModes()
    {
        EntityNPCInterface npc = npc();
        npc.stats.healthRegen = 7;
        npc.stats.canDrown = true;
        NpcPresetRequests.apply(npc, NpcPreset.VEHICLE);
        assertTrue(NpcPresetRequests.canUndo(npc));
        assertEquals(0, npc.stats.healthRegen);
        assertFalse(npc.stats.canDrown);
        assertTrue(combat.isCustom(NpcTypeProperty.NO_FALL_DAMAGE));
        assertTrue(npc.stats.noFallDamage);
        assertTrue(presentation.isKeepWreck());
        // Already-matching fields are excluded from Undo, preserving later manual edits to them.
        npc.ais.returnToStart = true;
        options.set(Feature.CONSUME_AMMO, true);
        NpcPresetRequests.undo(npc);
        assertEquals(7, npc.stats.healthRegen);
        assertTrue(npc.stats.canDrown);
        assertFalse(combat.isCustom(NpcTypeProperty.NO_FALL_DAMAGE));
        assertFalse(npc.stats.noFallDamage);
        assertFalse(presentation.isKeepWreck());
        assertTrue(npc.ais.returnToStart);
        assertTrue(options.enabled(Feature.CONSUME_AMMO));
        assertFalse(NpcPresetRequests.canUndo(npc));
    }

    @Test
    void applyingAnAlreadyMatchingPresetKeepsThePreviousUndo()
    {
        EntityNPCInterface npc = npc();
        NpcPresetRequests.apply(npc, NpcPreset.PLAYER);
        NpcPresetRequests.apply(npc, NpcPreset.PLAYER);
        NpcPresetRequests.undo(npc);
        assertFalse(options.enabled(Feature.CONSUME_AMMO));
    }

    @Test
    void presetsReplaceTheSingleUndoAndDoNotShareItWithOtherNpcs()
    {
        EntityNPCInterface npc = npc();
        NpcPresetRequests.apply(npc, NpcPreset.PLAYER);
        NpcPresetRequests.apply(npc, NpcPreset.MOB);
        assertFalse(NpcPresetRequests.canUndo(npc()));
        NpcPresetRequests.undo(npc);
        assertEquals(0, npc.stats.spawnCycle);
        assertTrue(npc.ais.canSprint);
        assertTrue(options.enabled(Feature.CONSUME_AMMO));
    }

    @Test
    void wreckPresentationAndUndoAvailabilitySurviveTheirRespectiveTransports()
    {
        EntityNPCInterface npc = npc();
        NpcPresetRequests.apply(npc, NpcPreset.TURRET);
        CompoundTag saved = new CompoundTag();
        presentation.save(saved);
        NpcPresentation loaded = new NpcPresentation();
        loaded.load(saved);
        assertTrue(loaded.isKeepWreck());
        assertFalse(loaded.isDeathRotation());
        assertFalse(loaded.isHurtFlash());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            NpcPresetPacket.state(npc).write(buffer);
            NpcPresetPacket packet = NpcPresetPacket.read(buffer);
            assertTrue(packet.values().getBoolean("CanUndo"));
            assertEquals(1, packet.values().getInt("KEEP_WRECK"));
            assertEquals(1, packet.values().getInt("RETURN_TO_START"));
        }
        finally
        {
            buffer.release();
        }
    }
}
