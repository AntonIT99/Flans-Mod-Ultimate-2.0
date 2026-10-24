package com.flansmodultimate.common.driveables.visual;

import com.flansmodultimate.common.driveables.EnumMechaSlotType;

import net.minecraft.world.item.ItemStack;

/** Leg motion of a mecha. */
public interface MechaVisual extends DriveableVisual
{
    /** Tool attached to the arm, or empty when its ordinary hand should be drawn. */
    default ItemStack getMechaTool(boolean left)
    {
        var data = getDriveableData();
        return data == null ? ItemStack.EMPTY : data.getMechaAddon(left ? EnumMechaSlotType.LEFT_TOOL : EnumMechaSlotType.RIGHT_TOOL);
    }

    /** @return phase of the walk cycle, from 0 to 1 */
    float getLegSwing();

    /** @return yaw the legs face, in degrees, in the same frame as the hull yaw */
    float getLegYaw();

    float getPrevLegYaw();
}
