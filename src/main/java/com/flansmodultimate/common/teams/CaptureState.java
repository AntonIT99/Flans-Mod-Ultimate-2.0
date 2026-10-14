package com.flansmodultimate.common.teams;

/**
 * Who holds a capture point and how far a capture has progressed.
 *
 * <p>
 * Team ids are those of the round: 0 for nobody, 1 for spectators, 2 and up for the playing
 * teams. {@code progress} runs from 0 (neutral) to 1 (fully held) and belongs to the owner when
 * there is one, otherwise to the capturing team.
 * </p>
 */
public record CaptureState(int ownerTeamId, int capturingTeamId, float progress, boolean contested)
{
    public static final CaptureState NEUTRAL = new CaptureState(0, 0, 0F, false);

    public CaptureState
    {
        ownerTeamId = Math.max(0, ownerTeamId);
        capturingTeamId = Math.max(0, capturingTeamId);
        progress = Float.isFinite(progress) ? Math.max(0F, Math.min(1F, progress)) : 0F;
    }

    /** A point fully held by {@code teamId}, or a neutral one for team 0. */
    public static CaptureState ownedBy(int teamId)
    {
        return teamId <= 0 ? NEUTRAL : new CaptureState(teamId, 0, 1F, false);
    }

    /** Whether {@code teamId} owns the point outright, which is what earns tickets and spawns. */
    public boolean isHeldBy(int teamId)
    {
        return teamId > 0 && ownerTeamId == teamId && progress >= 1F;
    }

    /** The team whose colours the point shows: its owner, or whoever is taking a neutral point. */
    public int displayTeamId()
    {
        return ownerTeamId != 0 ? ownerTeamId : capturingTeamId;
    }
}
