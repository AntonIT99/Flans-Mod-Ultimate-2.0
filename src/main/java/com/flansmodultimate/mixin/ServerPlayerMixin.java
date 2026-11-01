package com.flansmodultimate.mixin;

import com.flansmodultimate.common.teams.RoundRule;
import com.flansmodultimate.common.teams.TeamsCombatants;
import com.flansmodultimate.common.teams.TeamsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;

/** Applies round spectator camera restrictions and optional suppression of vanilla death messages. */
@Mixin(ServerPlayer.class)
@SuppressWarnings("DataFlowIssue") // The mixin is merged into ServerPlayer.
public abstract class ServerPlayerMixin
{
    @Inject(method = "setCamera", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateRestrictSpectatorCamera(Entity pEntityToSpectate, CallbackInfo callback)
    {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (pEntityToSpectate != null && !TeamsManager.getInstance().getRoundRuleRuntime().maySpectate(player, pEntityToSpectate))
            callback.cancel();
    }

    @SuppressWarnings("ConstantValue") // Mixin supplies the runtime game-rule key; static analysis cannot follow the redirect.
    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean flansmodultimateRoundDeathMessages(GameRules rules, GameRules.Key<GameRules.BooleanValue> pKey)
    {
        TeamsManager manager = TeamsManager.getInstance();
        if (pKey == GameRules.RULE_SHOWDEATHMESSAGES && TeamsCombatants.getPlayingTeam(manager, (ServerPlayer) (Object) this) != null
            && manager.getRoundRuleRuntime().rules().enabled(RoundRule.HIDE_KILL_MESSAGES))
            return false;
        return rules.getBoolean(pKey);
    }
}
