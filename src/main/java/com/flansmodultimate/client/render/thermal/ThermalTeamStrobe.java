package com.flansmodultimate.client.render.thermal;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Thermal IFF strobe from the Labjac Edition: during a Teams round, team-mates and the driveables they crew
 * blink in thermal sights with a double pulse every second, hot for 3 ticks, cold for 3, hot for 3 and cold
 * for the rest, so they can be told apart from enemies at a glance.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThermalTeamStrobe
{
    static final int PERIOD_TICKS = 20;
    static final int FIRST_PULSE_END = 3;
    static final int SECOND_PULSE_START = 6;
    static final int SECOND_PULSE_END = 9;

    /** Whether an entity is a team-mate, or crewed by one, that should be drawn cold at this moment. */
    public static boolean isFriendlyInColdPhase(Entity entity, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || isFlashOn(mc.level.getGameTime(), partialTick))
            return false;
        Player crew = entity instanceof Driveable driveable ? controllingCrew(driveable) : null;
        Player player = entity instanceof Player p ? p : crew;
        return player != null && player != mc.player && TeamsClientState.isTeamMate(player);
    }

    /** Whether the strobe is in one of its two hot pulses. */
    static boolean isFlashOn(long gameTime, float partialTick)
    {
        float phase = (gameTime % PERIOD_TICKS + partialTick) % PERIOD_TICKS;
        return phase < FIRST_PULSE_END || phase >= SECOND_PULSE_START && phase < SECOND_PULSE_END;
    }

    /** The driver if a player drives, otherwise the first player in a passenger seat. */
    @Nullable
    private static Player controllingCrew(Driveable driveable)
    {
        for (Seat seat : driveable.getSeats())
            if (seat != null && seat.getFirstPassenger() instanceof Player player)
                return player;
        return null;
    }
}
