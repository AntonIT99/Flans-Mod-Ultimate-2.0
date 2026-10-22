package com.flansmodultimate.client.particle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * The wind rising smoke drifts with: one direction and strength for the whole sky, so every smoke column over a
 * battlefield leans the same way.
 *
 * <p>
 * It turns slowly and gusts over minutes of game time, and blows harder in rain and storms. It is worked out from the
 * world's time and dimension alone, so every player sees the same wind without anything being sent.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleWind
{
    /** Push per tick, in blocks per tick squared, of a steady breeze. */
    private static final float BREEZE = 0.0006F;
    /** Ticks the wind takes to turn once around the compass. */
    private static final float TURN_TICKS = 48000F;
    /** Ticks between one gust and the next. */
    private static final float GUST_TICKS = 2400F;

    private static float x;
    private static float z;

    /** Works out this tick's wind. Client tick. */
    // The wind is shared by every particle in the world.
    @SuppressWarnings("java:S2696")
    public static void tick()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            x = 0F;
            z = 0F;
            return;
        }
        float[] wind = at(level.getGameTime(), level.dimension().location().hashCode(), level.getRainLevel(1F), level.getThunderLevel(1F));
        x = wind[0];
        z = wind[1];
    }

    /**
     * @param seed
     *            fixes where the wind starts, so that each dimension has its own
     * @return the push of the wind along x and z, in blocks per tick squared
     */
    static float[] at(long gameTime, int seed, float rain, float thunder)
    {
        float angle = (seed & 0xFFFF) / 65536F * Mth.TWO_PI + (gameTime % (long) TURN_TICKS) / TURN_TICKS * Mth.TWO_PI;
        float gust = 0.7F + 0.3F * Mth.sin((gameTime % (long) (GUST_TICKS * 100F)) / GUST_TICKS * Mth.TWO_PI);
        float strength = BREEZE * gust * (1F + 0.6F * rain + 0.6F * thunder);
        return new float[]{Mth.cos(angle) * strength, Mth.sin(angle) * strength};
    }

    public static float x()
    {
        return x;
    }

    public static float z()
    {
        return z;
    }
}
