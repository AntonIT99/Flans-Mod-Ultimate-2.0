package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.teams.BattleFactions;
import com.wolffsmod.npcs.teams.BotFactions;
import com.wolffsmod.npcs.teams.FactionMobSettings;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Custom NPCs' factions.
 * Persists the optional aggression toward the monsters covered by GetsAttacked.
 *
 * <ul>
 * <li>Teams bots: while a round runs, the faction of a Flan team treats a player by the team the player fights for, rather
 * than by the player's faction points: hostile to the other sides, friendly to its own, neutral to anyone not
 * fighting.</li>
 * <li>Managed factions: a faction managed for battles outside rounds treats a player by the managed faction the player
 * belongs to, rather than by the player's faction points; while managed factions are at rest, as during a Teams round, it
 * is neutral to every player and its NPCs attack no other NPC.</li>
 * </ul>
 */
@Mixin(value = Faction.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class FactionMixin implements FactionMobSettings
{
    @Unique
    private boolean wolffsmodnpcsAttackMobs;

    @Override
    public boolean wolffsmodnpcsAttackMobs()
    {
        return wolffsmodnpcsAttackMobs;
    }

    @Override
    public void wolffsmodnpcsSetAttackMobs(boolean enabled)
    {
        wolffsmodnpcsAttackMobs = enabled;
    }

    @Inject(method = "readNBT", at = @At("TAIL"))
    private void wolffsmodnpcsReadMobSettings(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsAttackMobs = tag.getBoolean("WolffsModAttackMobs");
    }

    @Inject(method = "writeNBT", at = @At("RETURN"))
    private void wolffsmodnpcsWriteMobSettings(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        callback.getReturnValue().putBoolean("WolffsModAttackMobs", wolffsmodnpcsAttackMobs);
    }

    @Inject(method = "isAggressiveToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsAggressive(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        wolffsmodnpcsStance(player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.HOSTILE));
    }

    @Inject(method = "isFriendlyToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsFriendly(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        wolffsmodnpcsStance(player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.FRIENDLY));
    }

    @Inject(method = "isNeutralToPlayer", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsTeamsNeutral(Player player, CallbackInfoReturnable<Boolean> callback)
    {
        wolffsmodnpcsStance(player).ifPresent(stance -> callback.setReturnValue(stance == BotFactions.Stance.NEUTRAL));
    }

    /** The stance of a round's team faction first, then of a managed faction; empty leaves it to faction points. */
    @Unique
    private Optional<BotFactions.Stance> wolffsmodnpcsStance(Player player)
    {
        int id = ((Faction) (Object) this).id;
        return BotFactions.stance(id, player).or(() -> BattleFactions.stance(id, player));
    }

    // --- Managed factions ---

    @Inject(method = "isAggressiveToNpc", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("ConstantValue") // IntelliJ reads the mixin self-cast as impossible; at run time this is the target.
    private void wolffsmodnpcsAtRest(EntityNPCInterface entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (entity.faction != null && BattleFactions.atRest(entity.getServer(), ((Faction) (Object) this).id, entity.faction.id))
            callback.setReturnValue(false);
    }
}
