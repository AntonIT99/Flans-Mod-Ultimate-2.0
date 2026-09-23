package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-side debug subscription and chat reporting for damage dealt to driveables. */
public final class DriveableDamageDebug
{
    private static final Set<ServerPlayer> ENABLED_PLAYERS =
        Collections.newSetFromMap(new WeakHashMap<>());

    private DriveableDamageDebug() {}

    public static void setEnabled(ServerPlayer player, boolean enabled)
    {
        if (enabled)
            ENABLED_PLAYERS.add(player);
        else
            ENABLED_PLAYERS.remove(player);
    }

    public static boolean isEnabled(@Nullable ServerPlayer player)
    {
        return player != null && ENABLED_PLAYERS.contains(player);
    }

    @Nullable
    public static ServerPlayer playerFrom(@Nullable DamageSource source)
    {
        return source != null && source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    public static void reportDamage(@Nullable ServerPlayer player, Driveable driveable,
                                    EnumDriveablePart part, float damage)
    {
        if (!isEnabled(player) || damage <= 0F)
            return;
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
            "[FMU Debug] %s - %s: %.2f damage",
            driveable.getConfigType().getName(), part.getName(), damage)).withStyle(ChatFormatting.YELLOW));
    }

    public static void reportArmorBlock(@Nullable ServerPlayer player, Driveable driveable,
                                        EnumDriveablePart part, float penetrationMm, float armorMm)
    {
        if (!isEnabled(player))
            return;
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
            "[FMU Debug] %s - %s: blocked (penetration %.2f mm, effective armor %.2f mm)",
            driveable.getConfigType().getName(), part.getName(), penetrationMm, armorMm))
            .withStyle(ChatFormatting.RED));
    }
}
