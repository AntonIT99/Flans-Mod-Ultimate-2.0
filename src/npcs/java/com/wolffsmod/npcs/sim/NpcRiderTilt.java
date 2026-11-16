package com.wolffsmod.npcs.sim;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import net.minecraft.util.Mth;

/** The pitch and bank a Flan-model NPC is drawn with, as its riders take them; see {@code FlanModelEntityModel}. */
public final class NpcRiderTilt
{
    private NpcRiderTilt()
    {}

    /**
     * @param pitch
     *            degrees the model is pitched nose down
     * @param bank
     *            degrees the model is rolled to its right
     * @return the model's tilt in the NPC body frame, or null while level
     */
    @Nullable
    public static Quaternionf of(float pitch, float bank)
    {
        if (pitch == 0F && bank == 0F)
            return null;
        return new Quaternionf().rotateX(-pitch * Mth.DEG_TO_RAD).rotateZ(-bank * Mth.DEG_TO_RAD);
    }
}
