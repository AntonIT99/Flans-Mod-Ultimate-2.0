package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EngineSound;
import com.flansmodultimate.api.FlansEntitySounds;
import noppes.npcs.entity.EntityNPCInterface;

import java.util.UUID;

/** Server-owned idle/movement audio; the live NPC, never its detached model entity, owns the sound and timer. */
public final class NpcEngineAudio
{
    private final NpcEngineCadence cadence = new NpcEngineCadence();
    private UUID instance;
    private boolean lastMoving;
    private boolean lastVariablePitch;
    private boolean lastAutomaticRepeat;

    public void tick(EntityNPCInterface npc)
    {
        NpcVehicleSounds settings = NpcPresentation.of(npc).getVehicleSounds();
        boolean moving = NpcVehicleSounds.moving(npc);
        boolean active = npc.isAlive() && !npc.isKilled() && !npc.isSilent();
        boolean variablePitch = moving && settings.isVariablePitch();
        boolean automaticRepeat = settings.automaticRepeat(moving);
        EngineSound desired = active ? settings.resolve(npc, moving).orElse(null) : null;
        if (moving != lastMoving || variablePitch != lastVariablePitch || automaticRepeat != lastAutomaticRepeat)
        {
            stop(npc);
            cadence.reset();
        }
        lastMoving = moving;
        lastVariablePitch = variablePitch;
        lastAutomaticRepeat = automaticRepeat;
        if (cadence.needsStop(desired, active))
            stop(npc);
        float playbackRate = variablePitch && automaticRepeat ? NpcVehicleSounds.pitch(npc) : 1F;
        if (cadence.tick(desired, active, playbackRate))
        {
            // Stop the preceding clip even when an edited interval is shorter than the actual asset.
            stop(npc);
            UUID next = UUID.randomUUID();
            if (FlansEntitySounds.playEngineSound(npc, desired, next, variablePitch))
                instance = next;
        }
    }

    private void stop(EntityNPCInterface npc)
    {
        if (instance != null)
        {
            FlansEntitySounds.stopEngineSound(npc, instance);
            instance = null;
        }
    }
}
