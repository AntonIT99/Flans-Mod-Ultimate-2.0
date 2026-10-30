package com.flansmodultimate.common.teams;

/** A Teams base with an individually saved pole height, in blocks. */
public interface ITeamFlagpole extends ITeamBase
{
    int getPoleHeight();

    void setPoleHeight(int height);
}
