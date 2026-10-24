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
import noppes.npcs.entity.data.DataAdvanced;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
        installContainers(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        when(settings.wolffsmodnpcsTuning()).thenReturn(tuning);
        when(settings.wolffsmodnpcsWeaponOptions()).thenReturn(options);
        when(settings.wolffsmodnpcsPresentation()).thenReturn(presentation);
        when(settings.wolffsmodnpcsCombat()).thenReturn(combat);
        when(settings.wolffsmodnpcsTypeProperties()).thenReturn(properties);
        Level level = mock(Level.class);
        when(npc.level()).thenReturn(level);
        // Setting the walking speed sets the movement attributes as well.
        when(npc.getAttribute(any(Attribute.class))).thenAnswer(call -> new AttributeInstance(call.getArgument(0), changed ->
        {
        }));
        return npc;
    }

    @SuppressWarnings("java:S3011") // Mockito skips construction; install the real final data containers for integration tests.
    private static void installContainers(EntityNPCInterface npc)
    {
        try
        {
            var field = EntityNPCInterface.class.getField("ais");
            field.setAccessible(true);
            field.set(npc, new DataAI(npc));
            var advanced = EntityNPCInterface.class.getField("advanced");
            advanced.setAccessible(true);
            advanced.set(npc, new DataAdvanced(npc));
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(exception);
        }
    }

    @Test
    void nonSpeakingPresetsClearEveryLineGroupWhilePeopleKeepTheirDialogue()
    {
        for (NpcPreset preset : NpcPreset.values())
        {
            EntityNPCInterface npc = npc();
            for (int type = 0; type < 6; type++)
                npc.advanced.setLine(type, 0, "Hello", "minecraft:entity.villager.ambient");
            preset.apply(npc);
            boolean speaking = preset == NpcPreset.HUMAN || preset == NpcPreset.PLAYER;
            for (int type = 0; type < 6; type++)
                assertEquals(speaking ? 1 : 0, npc.advanced.getLineCount(type), preset + " line group " + type);
            assertTrue(preset.matches(npc));
        }
    }

    @Test
    void dialogueAloneCanReapplyAPresetAndUndoRestoresTextSlotsAndSounds()
    {
        EntityNPCInterface npc = npc();
        NpcPreset.MOB.apply(npc);
        npc.advanced.setLine(0, 3, "Hello", "minecraft:entity.villager.ambient");
        assertFalse(NpcPreset.MOB.matches(npc));
        NpcPresetRequests.apply(npc, NpcPreset.MOB);
        assertTrue(NpcPresetLines.isEmpty(npc));
        assertTrue(NpcPresetRequests.canUndo(npc));
        npc.advanced.setLine(1, 2, "A later edit", "");
        NpcPresetRequests.undo(npc);
        assertEquals("Hello", npc.advanced.getLine(0, 3));
        assertEquals("minecraft:entity.villager.ambient", npc.advanced.interactLines.lines.get(3).getSound());
        assertEquals("A later edit", npc.advanced.getLine(1, 2));
    }

    @Test
    void presetStateSynchronizesClearedAndRestoredLinesWithTheEditor()
    {
        EntityNPCInterface server = npc();
        EntityNPCInterface client = npc();
        server.advanced.setLine(0, 0, "Hello", "");
        client.advanced.setLine(0, 0, "Hello", "");
        NpcPresetRequests.apply(server, NpcPreset.SHIP);
        NpcPresetPacket.state(server).copyTo(client);
        assertTrue(NpcPresetLines.isEmpty(client));
        assertTrue(NpcPreset.SHIP.matches(client));
        NpcPresetRequests.undo(server);
        NpcPresetPacket.state(server).copyTo(client);
        assertEquals("Hello", client.advanced.getLine(0, 0));
        assertFalse(NpcPreset.SHIP.matches(client));
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
        assertFalse(combat.isCustom(NpcTypeProperty.NO_FALL_DAMAGE));
        assertFalse(npc.stats.noFallDamage);
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
    void vehiclePresetsPreserveBothInheritedAndExplicitCustomModes()
    {
        EntityNPCInterface npc = npc();
        combat.setCustom(NpcTypeProperty.POTION_IMMUNE, true);
        NpcPreset.VEHICLE.apply(npc);
        assertTrue(combat.isCustom(NpcTypeProperty.POTION_IMMUNE));
        assertFalse(combat.isCustom(NpcTypeProperty.CAN_DROWN));
        assertFalse(combat.isCustom(NpcTypeProperty.NO_FALL_DAMAGE));
        assertFalse(combat.isCustom(NpcTypeProperty.HEALTH_REGEN));
        assertTrue(options.enabled(Feature.DRIVER_GUNS));
    }

    @Test
    void theTurretPresetStopsTheNpcWalkingWhateverItsModelLends()
    {
        EntityNPCInterface npc = npc();
        npc.ais.setWalkingSpeed(5);
        NpcPreset.TURRET.apply(npc);
        assertEquals(0, npc.ais.getWalkingSpeed());
        // Its own value, so that no model's walking speed covers it.
        assertTrue(combat.isCustom(NpcTypeProperty.WALKING_SPEED));
        assertEquals(0, NpcPresetSetting.WALKING_SPEED.displayValue(NpcPresetSetting.WALKING_SPEED.get(npc)));
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
