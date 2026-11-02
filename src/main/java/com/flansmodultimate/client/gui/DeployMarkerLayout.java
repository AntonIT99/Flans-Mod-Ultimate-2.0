package com.flansmodultimate.client.gui;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/** Keeps automatic vehicle markers selectable when several spawners share a headquarters or objective. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DeployMarkerLayout
{
    private static final List<Point> DIRECTIONS = List.of(new Point(0, 1), new Point(0, -1), new Point(1, 0), new Point(-1, 0), new Point(1, 1), new Point(-1, 1), new Point(1, -1), new Point(-1, -1));

    record Point(int x, int y)
    {}

    static Point separate(Point requested, List<Point> occupied, int minX, int minY, int maxX, int maxY)
    {
        if (clear(requested, occupied))
            return requested;
        for (int ring = 1; ring <= 4; ring++)
        {
            for (Point direction : DIRECTIONS)
            {
                Point candidate = new Point(Math.max(minX, Math.min(maxX, requested.x() + direction.x() * 24 * ring)), Math.max(minY, Math.min(maxY, requested.y() + direction.y() * 36 * ring)));
                if (clear(candidate, occupied))
                    return candidate;
            }
        }
        return requested;
    }

    private static boolean clear(Point point, List<Point> occupied)
    {
        return occupied.stream().noneMatch(other -> Math.abs(point.x() - other.x()) < 24 && Math.abs(point.y() - other.y()) < 36);
    }
}
