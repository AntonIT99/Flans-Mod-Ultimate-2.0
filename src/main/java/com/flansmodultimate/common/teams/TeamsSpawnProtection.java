package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.PlayerData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side spawn protection of round players: for {@link TeamsManager#getSpawnProtectionSeconds()} after taking the
 * field a player cannot be hurt, unless they attack first. Damage that bypasses invulnerability, such as leaving the
 * playing area or falling out of the world, still kills them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsSpawnProtection
{
    /** Starts the protection of a player who just took the field, telling them how long it lasts. */
    static void grant(ServerPlayer player, PlayerData data, int seconds)
    {
        data.setSpawnProtectionTicks(seconds * 20);
        if (seconds > 0)
            player.displayClientMessage(Component.translatable("message.flansmodultimate.teams.spawn_protection", seconds), true);
    }

    /** Whether spawn protection spares a player this damage. */
    public static boolean spares(LivingEntity entity, DamageSource source)
    {
        return entity instanceof ServerPlayer player && TeamsManager.getInstance().isRoundRunning() && PlayerData.getInstance(player).getSpawnProtectionTicks() > 0
            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** Ends the protection of the player behind an attack on someone else, so protected players cannot fight unharmed. */
    public static void attacked(LivingEntity victim, DamageSource source)
    {
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim)
            end(attacker);
    }

    /** Ends a player's protection because they fired, threw or swung a weapon. Server side. */
    public static void end(Player player)
    {
        if (!player.level().isClientSide)
            PlayerData.getInstance(player).setSpawnProtectionTicks(0);
    }
}
