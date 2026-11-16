package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.entity.EngineSound;
import com.flansmodultimate.api.entity.FlansEntitySounds;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Server-owned idle/movement audio; the live NPC, never its detached model entity, owns the sound and timer.
 * <p>
 * A sound repeated at its own length is looped by the clients, which plays it without a break; repeating it as clips
 * from here started each one a network delay late and cut or padded it to whole ticks. Only a repeat interval set by
 * hand keeps the clips, as that interval is the sound the user asked for.
 */
public final class NpcEngineAudio
{
    /** Ticks an NPC still counts as moving after it last moved, so a pause of a tick or two keeps its sound running. */
    static final int MOVING_HOLD_TICKS = 10;

    private final NpcEngineCadence cadence = new NpcEngineCadence();
    private UUID instance;
    private boolean lastMoving;
    private boolean lastVariablePitch;
    private boolean lastAutomaticRepeat;
    private int movingHoldTicks;
    /** The sound clients loop, or null when no loop runs. */
    private EngineSound looping;
    private boolean loopingVariablePitch;
    private int loopRenewTicks;

    public void tick(EntityNPCInterface npc)
    {
        NpcVehicleSounds settings = NpcPresentation.of(npc).getVehicleSounds();
        boolean moving = holdMoving(NpcVehicleSounds.moving(npc));
        boolean active = npc.isAlive() && !npc.isKilled() && !npc.isSilent();
        boolean variablePitch = moving && settings.isVariablePitch();
        boolean automaticRepeat = settings.automaticRepeat(moving);
        EngineSound desired = active ? settings.resolve(npc, moving).orElse(null) : null;
        boolean changed = moving != lastMoving || variablePitch != lastVariablePitch || automaticRepeat != lastAutomaticRepeat;
        lastMoving = moving;
        lastVariablePitch = variablePitch;
        lastAutomaticRepeat = automaticRepeat;

        if (automaticRepeat)
        {
            stop(npc);
            cadence.reset();
            loop(npc, desired, variablePitch);
            return;
        }
        stopLoop(npc);

        if (changed)
        {
            stop(npc);
            cadence.reset();
        }
        if (cadence.needsStop(desired, active))
            stop(npc);
        // A hand-set interval is kept as it is, whatever the pitch.
        if (cadence.tick(desired, active))
        {
            // Stop the preceding clip even when an edited interval is shorter than the actual asset.
            stop(npc);
            UUID next = UUID.randomUUID();
            if (FlansEntitySounds.playEngineSound(npc, desired, next, variablePitch))
                instance = next;
        }
    }

    boolean holdMoving(boolean movedThisTick)
    {
        if (movedThisTick)
            movingHoldTicks = MOVING_HOLD_TICKS;
        else if (movingHoldTicks > 0)
            --movingHoldTicks;
        return movingHoldTicks > 0;
    }

    /** Starts the loop, replaces it when the sound changes and renews it before clients let it lapse. */
    private void loop(EntityNPCInterface npc, @Nullable EngineSound desired, boolean variablePitch)
    {
        if (desired == null)
        {
            stopLoop(npc);
            return;
        }
        if (--loopRenewTicks > 0 && desired.equals(looping) && variablePitch == loopingVariablePitch)
            return;
        if (FlansEntitySounds.loopEngineSound(npc, desired, variablePitch))
        {
            looping = desired;
            loopingVariablePitch = variablePitch;
            loopRenewTicks = FlansEntitySounds.ENGINE_LOOP_RENEW_TICKS;
        }
        else
            stopLoop(npc);
    }

    private void stopLoop(EntityNPCInterface npc)
    {
        if (looping != null)
        {
            FlansEntitySounds.stopEngineLoop(npc);
            looping = null;
        }
        loopRenewTicks = 0;
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
