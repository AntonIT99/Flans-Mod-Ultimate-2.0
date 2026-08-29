package com.flansmodultimate.client.input;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import net.minecraftforge.client.settings.IKeyConflictContext;

import net.minecraft.client.Minecraft;

public enum EnumKeyConflictContext implements IKeyConflictContext
{
    VEHICLE
    {
        @Override
        public boolean isActive()
        {
            return getRiddenDriveable() != null;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other)
        {
            return this == other;
        }
    },

    PLANE
    {
        @Override
        public boolean isActive()
        {
            return getRiddenDriveable() instanceof Plane;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other)
        {
            return this == other;
        }
    };

    private static Driveable getRiddenDriveable()
    {
        return KeyInputHandler.resolveDriveable(Minecraft.getInstance().player);
    }
}
