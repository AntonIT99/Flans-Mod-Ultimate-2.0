package com.flansmodultimate.client.input;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;

/**
 * Narrows a key mapping to the kind of driveable it actually controls, so a
 * bind is only live while the player is in something that answers to it.
 *
 * <p>This is what makes the plane axes able to sit on W/A/S/D: they report
 * themselves inactive on foot and in a tank. It does not silence the controls
 * screen, though. {@code KeyMapping.same} ends with a bare key comparison
 * whenever two contexts do not conflict, and every vanilla mapping is
 * {@link KeyConflictContext#UNIVERSAL}, which conflicts with everything, so a
 * shared key is always drawn in red no matter what is declared here.</p>
 */
public enum EnumKeyConflictContext implements IKeyConflictContext
{
    /** Live in any driveable: planes, ground vehicles and mechas. */
    DRIVEABLE
    {
        @Override
        public boolean isActive()
        {
            return getRiddenDriveable() != null;
        }
    },

    /** Live only in a plane or helicopter. */
    PLANE
    {
        @Override
        public boolean isActive()
        {
            return getRiddenDriveable() instanceof Plane;
        }
    },

    /** Live only in a ground vehicle or a mecha. */
    GROUND_DRIVEABLE
    {
        @Override
        public boolean isActive()
        {
            Driveable driveable = getRiddenDriveable();
            return driveable != null && !(driveable instanceof Plane);
        }
    },

    /** Live only in a ground vehicle, not in a plane or mecha. */
    VEHICLE
    {
        @Override
        public boolean isActive()
        {
            return getRiddenDriveable() instanceof Vehicle;
        }
    };

    /**
     * Two contexts conflict when they can be live at the same moment. A plane
     * bind and a ground bind never can, so they are free to share a key.
     *
     * <p>The {@link KeyConflictContext#IN_GAME} case is not cosmetic. Forge
     * reads "does not conflict with IN_GAME" as "this is a modifier context",
     * and {@code KeyModifier.NONE} then reports itself inactive while Shift,
     * Ctrl or Alt is held, which switches every binding in this context off.
     * These are all in-game contexts, so they have to say so.</p>
     */
    @Override
    public boolean conflicts(IKeyConflictContext other)
    {
        if (other == KeyConflictContext.IN_GAME || other == this)
            return true;
        if (!(other instanceof EnumKeyConflictContext context))
            return false;
        // DRIVEABLE covers every specialized context. VEHICLE is a subset of
        // GROUND_DRIVEABLE, while neither can ever overlap PLANE.
        return this == DRIVEABLE || context == DRIVEABLE
            || this == GROUND_DRIVEABLE && context == VEHICLE
            || this == VEHICLE && context == GROUND_DRIVEABLE;
    }

    @Nullable
    private static Driveable getRiddenDriveable()
    {
        return KeyInputHandler.resolveDriveable(Minecraft.getInstance().player);
    }
}
