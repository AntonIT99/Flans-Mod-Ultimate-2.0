package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.ContentNameBinding;
import com.wolffsmod.npcs.names.FactionDisplayName;
import com.wolffsmod.npcs.teams.BattleFactions;
import com.wolffsmod.npcs.teams.BotFactions;
import com.wolffsmod.npcs.teams.FactionAllies;
import com.wolffsmod.npcs.teams.FactionFriendlyFire;
import com.wolffsmod.npcs.teams.FactionMobSettings;
import com.wolffsmod.npcs.teams.FriendlyFire;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Custom NPCs' factions.
 * Persists the optional aggression toward the monsters covered by GetsAttacked, display-only team name bindings, the
 * factions it is allied with and its own friendly fire settings.
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
public abstract class FactionMixin implements FactionMobSettings, FactionDisplayName, FactionAllies, FactionFriendlyFire
{
    @Unique
    private static final String WOLFFSMODNPCS_NAME_BINDING = "WolffsModNameBinding";
    @Unique
    private static final String WOLFFSMODNPCS_ALLIES = "WolffsModAllies";
    @Unique
    private static final String WOLFFSMODNPCS_FRIENDLY_FIRE = "WolffsModFriendlyFire";

    @Unique
    private boolean wolffsmodnpcsAttackMobs;
    @Unique @Nullable
    private ContentNameBinding wolffsmodnpcsName;
    @Unique
    private final Set<Integer> wolffsmodnpcsAllies = new HashSet<>();
    @Unique
    private final Map<FriendlyFire.Kind, FriendlyFire.Setting> wolffsmodnpcsFriendlyFire = new EnumMap<>(FriendlyFire.Kind.class);

    @Override
    public void wolffsmodnpcsBindName(ContentNameBinding binding)
    {
        wolffsmodnpcsName = binding;
    }

    @Override
    public Component wolffsmodnpcsNameComponent()
    {
        String literal = ((Faction) (Object) this).name;
        return wolffsmodnpcsName == null ? Component.literal(literal) : wolffsmodnpcsName.component(literal);
    }

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

    @Override
    public Set<Integer> wolffsmodnpcsAllies()
    {
        return wolffsmodnpcsAllies;
    }

    @Override
    public FriendlyFire.Setting wolffsmodnpcsFriendlyFire(FriendlyFire.Kind kind)
    {
        return wolffsmodnpcsFriendlyFire.getOrDefault(kind, FriendlyFire.Setting.DEFAULT);
    }

    @Override
    public void wolffsmodnpcsSetFriendlyFire(FriendlyFire.Kind kind, FriendlyFire.Setting setting)
    {
        wolffsmodnpcsFriendlyFire.put(kind, setting);
    }

    @Inject(method = "readNBT", at = @At("TAIL"))
    private void wolffsmodnpcsReadMobSettings(CompoundTag tag, CallbackInfo callback)
    {
        wolffsmodnpcsAttackMobs = tag.getBoolean("WolffsModAttackMobs");
        wolffsmodnpcsName = ContentNameBinding.read(tag.getCompound(WOLFFSMODNPCS_NAME_BINDING), ((Faction) (Object) this).name).orElse(null);
        wolffsmodnpcsAllies.clear();
        Arrays.stream(tag.getIntArray(WOLFFSMODNPCS_ALLIES)).forEach(wolffsmodnpcsAllies::add);
        FriendlyFire.load(tag.getCompound(WOLFFSMODNPCS_FRIENDLY_FIRE), wolffsmodnpcsFriendlyFire);
    }

    @Inject(method = "writeNBT", at = @At("RETURN"))
    @SuppressWarnings("ConstantValue") // The mixin's binding is populated by its load hook and the added interface.
    private void wolffsmodnpcsWriteMobSettings(CompoundTag tag, CallbackInfoReturnable<CompoundTag> callback)
    {
        callback.getReturnValue().putBoolean("WolffsModAttackMobs", wolffsmodnpcsAttackMobs);
        CompoundTag result = callback.getReturnValue();
        result.remove(WOLFFSMODNPCS_NAME_BINDING);
        if (wolffsmodnpcsName != null && wolffsmodnpcsName.literal().equals(((Faction) (Object) this).name))
            result.put(WOLFFSMODNPCS_NAME_BINDING, wolffsmodnpcsName.save());
        result.remove(WOLFFSMODNPCS_ALLIES);
        if (!wolffsmodnpcsAllies.isEmpty())
            result.putIntArray(WOLFFSMODNPCS_ALLIES, wolffsmodnpcsAllies.stream().mapToInt(Integer::intValue).sorted().toArray());
        result.remove(WOLFFSMODNPCS_FRIENDLY_FIRE);
        CompoundTag friendlyFire = FriendlyFire.save(wolffsmodnpcsFriendlyFire);
        if (!friendlyFire.isEmpty())
            result.put(WOLFFSMODNPCS_FRIENDLY_FIRE, friendlyFire);
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
