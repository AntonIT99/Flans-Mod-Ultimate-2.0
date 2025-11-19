package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GameType
{
    public boolean canPlayerBeAttacked(ServerPlayer victim, ServerPlayer attacker)
    {
        return true;
    }
}
