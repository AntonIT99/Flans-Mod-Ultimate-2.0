package com.flansmodultimate.common.teams;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class GameType
{
    public boolean canPlayerBeAttacked(ServerPlayer victim, ServerPlayer attacker)
    {
        return true;
    }

    public boolean playerAttacked(ServerPlayer player, DamageSource source)
    {
        return true;
    }
}
