package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * How parts lost together come away: each as one piece with the parts it carries. A wing shot off with its wing wheel
 * and top wing is one piece, not three; the core carries everything and so is never a piece of its own.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PartPieces
{
    /**
     * @param lost
     *            parts lost in the same moment
     * @return one set per piece, its leading part first, in the order the leading parts were lost
     */
    public static List<Set<EnumDriveablePart>> group(Collection<EnumDriveablePart> lost)
    {
        List<Set<EnumDriveablePart>> pieces = new ArrayList<>();
        for (EnumDriveablePart part : lost)
        {
            if (part == EnumDriveablePart.CORE || hasLostParent(part, lost))
                continue;
            Set<EnumDriveablePart> piece = EnumSet.of(part);
            collect(part, lost, piece);
            pieces.add(piece);
        }
        return pieces;
    }

    private static boolean hasLostParent(EnumDriveablePart part, Collection<EnumDriveablePart> lost)
    {
        for (EnumDriveablePart parent : part.getParents())
        {
            if (parent != EnumDriveablePart.CORE && lost.contains(parent))
                return true;
        }
        return false;
    }

    private static void collect(EnumDriveablePart part, Collection<EnumDriveablePart> lost, Set<EnumDriveablePart> into)
    {
        for (EnumDriveablePart child : part.getChildren())
        {
            if (lost.contains(child) && into.add(child))
                collect(child, lost, into);
        }
    }
}
