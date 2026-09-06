package com.flansmodultimate.client.render;

import com.flansmodultimate.common.KillMessageData;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The client-side kill feed shown in the corner of the HUD, as in the legacy Teams HUD. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KillMessageFeed
{
    /** Ticks one message stays on screen. */
    private static final int MESSAGE_LIFETIME = 200;
    /** Older messages are pushed up and dropped once they leave the feed. */
    private static final int MAX_LINES = 10;

    private static final List<Entry> MESSAGES = new ArrayList<>();

    /** One kill feed line. The weapon icon is resolved once, when the message arrives. */
    public static final class Entry
    {
        @Getter private final Component killer;
        @Getter private final Component victim;
        @Getter private final boolean headshot;
        @Getter private final ItemStack weapon;
        @Getter private int line;
        private int timer = MESSAGE_LIFETIME;

        private Entry(KillMessageData data)
        {
            killer = Component.literal(data.killerName()).withStyle(data.killerColour());
            victim = Component.literal(data.victimName()).withStyle(data.victimColour());
            headshot = data.headshot();
            weapon = resolveWeapon(data);
        }
    }

    public static void add(KillMessageData data)
    {
        for (Entry entry : MESSAGES)
        {
            entry.line++;
            if (entry.line > MAX_LINES)
                entry.timer = 0;
        }
        MESSAGES.add(new Entry(data));
    }

    public static void tick()
    {
        MESSAGES.removeIf(entry -> --entry.timer <= 0);
    }

    public static List<Entry> getMessages()
    {
        return Collections.unmodifiableList(MESSAGES);
    }

    public static void clear()
    {
        MESSAGES.clear();
    }

    /**
     * The icon shows the weapon that scored the kill. When the killer is still holding it,
     * the paintjob they used is copied over so the feed matches what they are carrying.
     */
    private static ItemStack resolveWeapon(KillMessageData data)
    {
        InfoType type = InfoType.getInfoType(data.weaponShortName());
        ItemStack stack = ModUtils.getItemStack(type).orElse(ItemStack.EMPTY);
        if (stack.isEmpty())
            return ItemStack.EMPTY;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return stack;

        for (Player player : minecraft.level.players())
        {
            if (!player.getGameProfile().getName().equals(data.killerName()))
                continue;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof IPaintableItem<?> && held.getItem() == stack.getItem() && held.hasTag())
                stack.setTag(held.getOrCreateTag().copy());
            break;
        }
        return stack;
    }
}
