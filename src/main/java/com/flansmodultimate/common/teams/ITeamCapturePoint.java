package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.entity.Flag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A base players take by standing on it, the objective of Domination, Conquest and Assault.
 *
 * <p>
 * Its owner is whoever holds it at the moment, so the spawners connected to it change hands
 * with it. The running game type decides how it is captured; the point only stores the result.
 * </p>
 */
public interface ITeamCapturePoint extends ITeamBase
{
    /** Letter from A to E the point is known by on the HUD and the deployment map. */
    char getSymbol();

    /** Horizontal radius, in blocks, within which a player counts as standing on the point. */
    int getCaptureRadius();

    CaptureState getCaptureState();

    void setCaptureState(CaptureState state);

    /**
     * The round this point last received its starting state for.
     *
     * <p>
     * Capture points can load after a round has begun, and a server can restart in the middle of
     * one. Stamping the round lets the game type tell a point that still needs its starting
     * state from one that is carrying progress it must keep.
     * </p>
     */
    @Nullable
    UUID getRoundId();

    void setRoundId(@Nullable UUID roundId);

    @Nullable
    @Override
    default Flag getFlag()
    {
        return null;
    }
}
