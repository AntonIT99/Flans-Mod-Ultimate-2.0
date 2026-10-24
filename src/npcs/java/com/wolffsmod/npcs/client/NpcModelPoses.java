package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.client.model.ModelPose;
import com.flansmodultimate.api.driveables.MountAim;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Turns an NPC's vehicle state and model switches into the pose its content-pack model is drawn in. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcModelPoses
{
    /** Blocks per tick a walking mob settles at, per unit of its speed squared. */
    private static final float GROUND_SPEED_FACTOR = 2.158F;

    /** @return the pose to draw, or null to draw the model at rest because the NPC's animation is switched off */
    @Nullable
    public static ModelPose of(EntityNPCInterface npc)
    {
        NpcVehicleState state = NpcVehicleState.of(npc);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (!state.isActive() || !options.enabled(Feature.ANIMATED_MODEL))
            return null;
        List<MountAim> seats = new ArrayList<>(state.seatCount());
        for (int seat = 0; seat < state.seatCount(); seat++)
            seats.add(state.seatAim(seat));
        boolean running = options.enabled(Feature.ENGINE_RUNNING) && !npc.isKilled();
        float speed = Math.max(0.01F, npc.ais.getWalkingSpeed() / 20F);
        float cruise = GROUND_SPEED_FACTOR * speed * speed;
        float travel = state.getTravel();
        float throttle = Mth.clamp(travel / cruise, -1F, 1F);
        if (running && options.enabled(Feature.FULL_THROTTLE))
        {
            // A parked display piece still turns its wheels and tracks.
            throttle = 1F;
            if (Math.abs(travel) < 1.0E-3F)
                travel = cruise;
        }
        if (!running)
            throttle = 0F;
        // Only aircraft have gear to draw, so only they look for the ground beneath them.
        boolean dynamic = options.enabled(Feature.GEAR_DYNAMIC) && state.kind() == FlanModelKind.PLANE;
        boolean gearDown = dynamic ? state.dynamicGearDown(npc) : !options.enabled(Feature.GEAR_UP);
        return new ModelPose(state.turretAim(), seats, throttle, travel, state.getTurn(), options.enabled(Feature.DOORS_OPEN), gearDown, options.enabled(Feature.WINGS_FOLDED), running,
            state.getShots(), state.getBarrel());
    }
}
