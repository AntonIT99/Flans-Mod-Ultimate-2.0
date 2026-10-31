package com.flansmodultimate.mixin;

import com.flansmodultimate.common.teams.TeamsRegeneration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;

/** Leaves hunger processing intact while Teams owns passive player healing during rounds. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin
{
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean flansmodultimateRoundRegeneration(GameRules rules, GameRules.Key<GameRules.BooleanValue> pKey, Player pPlayer)
    {
        return rules.getBoolean(pKey) && !TeamsRegeneration.applies(pPlayer);
    }
}
