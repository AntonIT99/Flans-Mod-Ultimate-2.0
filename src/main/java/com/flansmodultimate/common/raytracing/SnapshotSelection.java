package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.PlayerData;

public record SnapshotSelection(PlayerSnapshot snapshot, int snapshotIndex, float snapshotPortion)
{
    public static SnapshotSelection selectSnapshotForPing(PlayerData data, int pingOfShooter)
    {
        int snapshotToTry = FlansMod.teamsManager.getBulletSnapshotMin();
        int snapshotPortion = Math.round(pingOfShooter / (float) FlansMod.teamsManager.getBulletSnapshotDivisor());

        // Just make sure it's positive...
        snapshotToTry = Math.max(0, snapshotToTry);

        if (FlansMod.teamsManager.getBulletSnapshotDivisor() > 0)
        {
            snapshotToTry += snapshotPortion;
        }

        if (snapshotToTry >= data.getSnapshots().length)
        {
            snapshotToTry = data.getSnapshots().length - 1;
        }

        PlayerSnapshot snapshot;
        if (data.getSnapshots()[snapshotToTry] != null)
        {
            snapshot = data.getSnapshots()[snapshotToTry];
        }
        else
        {
            snapshot = data.getSnapshots()[0];
        }

        // Returns null snapshot if even snapshots[0] is null -> caller falls back to normal hit detect
        return new SnapshotSelection(snapshot, snapshotToTry, snapshotPortion);
    }
}
