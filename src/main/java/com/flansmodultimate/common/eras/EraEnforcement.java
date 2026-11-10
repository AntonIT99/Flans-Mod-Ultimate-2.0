package com.flansmodultimate.common.eras;

import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.hooks.ClientHooks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The calls item and entity code makes to honour era restrictions. On the server they ask {@link EraRestrictions}; on a
 * client they only predict for the local player, so a blocked gun does not appear to fire, and the server still decides.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EraEnforcement
{
    private static final long MESSAGE_INTERVAL_MILLIS = 1500L;
    private static final String OPEN_BOUND = "…";
    private static final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();

    /** Whether the holder may have and use the type. */
    public static boolean permits(Entity holder, InfoType type)
    {
        if (holder.level().isClientSide)
            return ClientHooks.ERA.permits(holder, type);
        return EraRestrictions.isAllowed(holder, type);
    }

    /** Whether the holder may have and use what the stack holds; stacks of other mods are never restricted. */
    public static boolean permits(Entity holder, ItemStack stack)
    {
        InfoType type = typeOf(stack);
        return type == null || permits(holder, type);
    }

    /**
     * Refuses a use the holder attempted: {@code true} when the type is blocked, after telling a player why on the action
     * bar, at most every 1.5 seconds so a held button does not flood it.
     */
    public static boolean refuses(Entity holder, InfoType type)
    {
        if (holder.level().isClientSide)
            return !ClientHooks.ERA.permits(holder, type);
        EraVerdicts.Verdict verdict = EraRestrictions.check(holder, type);
        if (verdict.allowed())
            return false;
        if (holder instanceof ServerPlayer player)
        {
            long now = System.currentTimeMillis();
            Long previous = lastMessage.get(player.getUUID());
            if (previous == null || now - previous >= MESSAGE_INTERVAL_MILLIS)
            {
                lastMessage.put(player.getUUID(), now);
                player.displayClientMessage(blockedMessage(type, verdict, EraRestrictions.verdicts().getEras()), true);
            }
        }
        return true;
    }

    /** The types of the list the holder may use, in order; the list itself when nothing in it is blocked. */
    public static <T extends InfoType> List<T> permitted(Entity holder, List<T> types)
    {
        if (types.stream().allMatch(type -> permits(holder, type)))
            return types;
        return types.stream().filter(type -> permits(holder, type)).toList();
    }

    public static void forget(UUID player)
    {
        lastMessage.remove(player);
    }

    @Nullable
    public static InfoType typeOf(ItemStack stack)
    {
        return stack.getItem() instanceof IFlanItem<?> item ? item.getConfigType() : null;
    }

    public static Component blockedMessage(InfoType type, EraVerdicts.Verdict verdict, EraCatalog eras)
    {
        String[] window = describeWindow(verdict.blockedBy(), eras);
        return Component.translatable("message.flansmodultimate.era.blocked", Component.translatable(type.getNameTranslationKey()), window[0], window[1]);
    }

    /** The window's two ends in the content syntax, an ellipsis standing for an open end. */
    public static String[] describeWindow(@Nullable ScopedRestriction scoped, EraCatalog eras)
    {
        if (scoped == null)
            return new String[]{OPEN_BOUND, OPEN_BOUND};
        int lower = scoped.restriction().lowerMonth(eras);
        int upper = scoped.restriction().upperMonth(eras);
        return new String[]{lower == ServiceDate.OPEN_LOW ? OPEN_BOUND : ServiceDate.formatMonth(lower), upper == ServiceDate.OPEN_HIGH ? OPEN_BOUND : ServiceDate.formatMonth(upper)};
    }

    /** A service period for display, such as {@code 1942-02 – 1957} or {@code ~1944 –}. */
    public static String describePeriod(ServicePeriod period)
    {
        return period.introduction() + " –" + (period.retirement() == null ? "" : " " + period.retirement());
    }
}
