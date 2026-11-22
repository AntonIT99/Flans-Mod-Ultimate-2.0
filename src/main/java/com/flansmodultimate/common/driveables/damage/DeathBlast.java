package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.explosions.ExplosionVisuals;
import com.flansmodultimate.common.explosions.FragmentationModel;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.effects.PacketCombatFeedback;
import com.flansmodultimate.network.client.effects.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.effects.PacketPlaySound;
import lombok.AllArgsConstructor;
import lombok.Getter;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The Labjac fork's death blasts: a destroyed machine flagged {@code Death}, {@code bigDeath} or {@code nuclearDeath}
 * goes up with a sound heard far off, a screen shake for those near it and a fireball, on top of its normal death.
 * <p>
 * The blast is cosmetic, like the fork's: damage comes only from the machine's own death explosion. The sounds and the
 * shake strengths and reaches are the fork's; its flash particles are drawn with the explosion visuals instead.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum DeathBlast
{
    /** A tank going up. */
    TANK("Death", "explcls6", 88F, 69F, 8F, 16F),
    /** A warship going up. */
    SHIP("bigDeath", "battleshipgun", 120F, 80F, 16F, 32F),
    /** A nuclear-powered or nuclear-armed machine going up. */
    NUCLEAR("nuclearDeath", "yamatogun", 250F, 120F, 40F, 80F);

    /** How far the fork sends the death sound, in blocks. */
    public static final double SOUND_RANGE = 250D;

    /** The boolean definition key that turns the blast on. */
    private final String key;
    /** The sound played, as the pack's sound name. */
    private final String sound;
    /** Strength of the screen shake, in the units of a shootable's {@code powerShake}. */
    private final float shakeStrength;
    /** Blocks within which players feel the shake. */
    private final float shakeRange;
    /** Size the fireball is drawn at, in blocks. */
    private final float visualRadius;
    /** Reach of the drawn blast wave, in blocks. */
    private final float blastRadius;

    /** Plays the blast at a destroyed machine for everyone in range. Server side. */
    public void play(ServerLevel level, Vec3 position)
    {
        PacketPlaySound.sendSoundPacket(position, SOUND_RANGE, level.dimension(), sound, false, null);
        PacketHandler.sendToAllAround(new PacketCombatFeedback(true, shakeStrength, 0F), position, shakeRange, level.dimension());
        int smoke = Math.round(visualRadius * 3F);
        int debris = Math.round(visualRadius * 1.5F);
        double reach = Math.max(blastRadius * 2D, ExplosionVisuals.landmarkRange(visualRadius));
        PacketHandler.sendToAllAround(new PacketFlanExplosionParticles(position, smoke, debris, blastRadius, visualRadius, 0F, 0F, 0D, FragmentationModel.Pattern.RADIAL, Vec3.ZERO, true),
            position, reach, level.dimension());
    }
}
