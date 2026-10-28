package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.FactionMembers;
import noppes.npcs.controllers.data.PlayerFactionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;

/**
 * Custom NPCs' faction points of a player.
 *
 * <ul>
 * <li>Members: a player whose points with the faction they belong to fall below its neutral points, as by killing its
 * NPCs, is thrown out of it.</li>
 * </ul>
 */
@Mixin(value = PlayerFactionData.class, remap = false)
public abstract class PlayerFactionDataMixin
{
    @Inject(method = "increasePoints", at = @At("TAIL"))
    private void wolffsmodnpcsExpelHated(Player player, int factionId, int points, CallbackInfo callback)
    {
        if (points < 0)
            FactionMembers.expelIfHated(player, factionId);
    }
}
