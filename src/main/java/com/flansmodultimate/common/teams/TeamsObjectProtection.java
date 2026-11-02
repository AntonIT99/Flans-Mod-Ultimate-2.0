package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.block.CapturePointBlock;
import com.flansmodultimate.common.block.TeamBannerBlock;
import com.flansmodultimate.common.block.TeamSpawnerBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.BooleanSupplier;

/** Protects round infrastructure; removal permission is scoped to one directly edited block. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsObjectProtection
{
    private record Removal(Level level, BlockPos pos)
    {}

    private static final ThreadLocal<Removal> DIRECT_REMOVAL = new ThreadLocal<>();

    public static boolean isProtected(Level level, BlockPos pos)
    {
        if (level.isClientSide || !TeamsManager.getInstance().isRoundRunning())
            return false;
        var block = level.getBlockState(pos).getBlock();
        return block instanceof TeamSpawnerBlock || block instanceof TeamBannerBlock || block instanceof CapturePointBlock;
    }

    @SuppressWarnings("AutoCloseableResource") // The record borrows its world; it must never close the server's world.
    public static boolean preventsRemoval(Level level, BlockPos pos)
    {
        Removal removal = DIRECT_REMOVAL.get();
        return (removal == null || removal.level() != level || !removal.pos().equals(pos)) && isProtected(level, pos);
    }

    /** Called by permission-checked direct breaking, operator stick destruction and trusted round terrain restoration. */
    public static boolean removeDirectly(Level level, BlockPos pos, BooleanSupplier action)
    {
        Removal previous = DIRECT_REMOVAL.get();
        DIRECT_REMOVAL.set(new Removal(level, pos.immutable()));
        try
        {
            return action.getAsBoolean();
        }
        finally
        {
            if (previous == null)
                DIRECT_REMOVAL.remove();
            else
                DIRECT_REMOVAL.set(previous);
        }
    }
}
