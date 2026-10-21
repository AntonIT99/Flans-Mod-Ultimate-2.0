package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.BotFactions;
import noppes.npcs.controllers.data.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

/**
 * Custom NPCs' factions.
 *
 * <ul>
 * <li>Teams bots: while a round runs, the faction of a Flan team treats a player by the team the player fights for, rather
 * than by the player's faction points: hostile to the other sides, friendly to its own, neutral to anyone not
 * fighting.</li>
 * </ul>
 */
@Mixin(value = Faction.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class FactionMixin
{
    @Inject(method = "isAggressiveToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsAggressive(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        BotFactions.stance(((Faction) (Object) this).id, player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.HOSTILE));
    }

    @Inject(method = "isFriendlyToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsFriendly(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        BotFactions.stance(((Faction) (Object) this).id, player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.FRIENDLY));
    }

    @Inject(method = "isNeutralToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsNeutral(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        BotFactions.stance(((Faction) (Object) this).id, player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.NEUTRAL));
    }
}
