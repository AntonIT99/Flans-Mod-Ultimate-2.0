package com.wolffsmod.npcs.teams;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataAI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotStandingMigrationTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void generatedStandingBotsAlreadySavedInABattleBecomeWandering() throws ReflectiveOperationException
    {
        EntityNPCInterface npc = npc(false);
        BotFactory.keepTrained(npc, new BotSettings());
        assertEquals(1, npc.ais.getMovingType());
        assertTrue(npc.updateAI);
        assertTrue(npc.updateClient);
    }

    @Test
    void enlistedNpcsSeatedCrewAndPathedBotsKeepTheirMovementChoices() throws ReflectiveOperationException
    {
        EntityNPCInterface enlisted = npc(true);
        BotFactory.keepTrained(enlisted, new BotSettings());
        assertEquals(0, enlisted.ais.getMovingType());
        EntityNPCInterface seated = npc(false);
        when(seated.isPassenger()).thenReturn(true);
        BotFactory.keepTrained(seated, new BotSettings());
        assertEquals(0, seated.ais.getMovingType());
        EntityNPCInterface pathed = npc(false);
        pathed.ais.setMovingType(2);
        BotFactory.keepTrained(pathed, new BotSettings());
        assertEquals(2, pathed.ais.getMovingType());
    }

    @SuppressWarnings("java:S3011") // Mockito skips construction; supply the real final Custom NPCs AI container.
    private static EntityNPCInterface npc(boolean enlisted) throws ReflectiveOperationException
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        when(npc.getPersistentData()).thenReturn(new CompoundTag());
        var ai = EntityNPCInterface.class.getDeclaredField("ais");
        ai.setAccessible(true);
        ai.set(npc, new DataAI(npc));
        npc.ais.setMovingType(0);
        BotTag.write(npc, new BotTag.Data(UUID.randomUUID(), 1, "test", true, BotSettings.Nameplates.ALWAYS, false, enlisted));
        return npc;
    }
}
