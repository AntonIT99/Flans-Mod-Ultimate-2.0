package com.flansmodultimate.common.driveables.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.ResolvedArmorHit;
import com.flansmodultimate.common.entity.Driveable;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.*;

/** Server-side debug subscription and chat reporting for damage dealt to driveables. */
public final class DriveableDamageDebug
{
    private static final Set<ServerPlayer> ENABLED_PLAYERS = Collections.newSetFromMap(new WeakHashMap<>());

    private DriveableDamageDebug()
    {}

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

    public static void reportDamage(@Nullable ServerPlayer player, Driveable driveable, EnumDriveablePart part, float damage)
    {
        if (!isEnabled(player) || damage <= 0F)
            return;
        player.sendSystemMessage(
            Component.literal(String.format(java.util.Locale.ROOT, "[FMU Debug] %s - %s: %.2f damage", driveable.getConfigType().getName(), part.getName(), damage)).withStyle(ChatFormatting.YELLOW));
    }

    /** Reports damage behind an armoured face together with the penetration check that let it through. */
    public static void reportPenetration(@Nullable ServerPlayer player, Driveable driveable, EnumDriveablePart part, float damage, float penetrationMm, ResolvedArmorHit armorHit)
    {
        if (!isEnabled(player) || damage <= 0F)
            return;
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT, "[FMU Debug] %s - %s: %.2f damage (penetration %.2f mm vs %s)", driveable.getConfigType().getName(),
            part.getName(), damage, penetrationMm, describeArmor(armorHit))).withStyle(ChatFormatting.YELLOW));
    }

    public static void reportArmorBlock(@Nullable ServerPlayer player, Driveable driveable, EnumDriveablePart part, float penetrationMm, ResolvedArmorHit armorHit)
    {
        if (!isEnabled(player))
            return;
        player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT, "[FMU Debug] %s - %s: blocked (penetration %.2f mm vs %s)", driveable.getConfigType().getName(), part.getName(),
            penetrationMm, describeArmor(armorHit))).withStyle(ChatFormatting.RED));
    }

    /**
     * Effective thickness with the struck face, the impact angle against its sloped normal and the nominal
     * thickness it was derived from (the HEAT value for a HEAT hit).
     */
    static String describeArmor(ResolvedArmorHit armorHit)
    {
        float nominal = (float) (armorHit.effectiveArmorMm() * Math.cos(Math.toRadians(armorHit.impactAngleDeg())));
        return String.format(java.util.Locale.ROOT, "effective armor %.2f mm: %s face, %.2f mm nominal at %.1f deg", armorHit.effectiveArmorMm(),
            armorHit.facing().name().toLowerCase(java.util.Locale.ROOT), nominal, armorHit.impactAngleDeg());
    }
}
