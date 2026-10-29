package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.diag.NpcCensus;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.network.ServerReportPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The lines of the Census page: what the server's limits did in the last tick, then two lines per level with loaded NPCs. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NpcCensusLines
{
    private static final String KEY = "wolffsmodnpcs.census.";

    static List<Component> lines(ServerReportPacket report)
    {
        List<Component> lines = new ArrayList<>();
        int total = report.census().stream().mapToInt(NpcCensus.Row::total).sum();
        lines.add(Component.translatable(KEY + "total", total, report.census().size()));
        if (ClientServerSettings.get(Setting.FREEZE_AI) == 1)
            lines.add(Component.translatable(KEY + "frozen").withStyle(ChatFormatting.GOLD));
        if (report.deferred().targets() > 0 || report.deferred().paths() > 0)
            lines.add(Component.translatable(KEY + "deferred", report.deferred().targets(), report.deferred().paths()).withStyle(ChatFormatting.GOLD));
        if (report.census().isEmpty())
            lines.add(Component.translatable(KEY + "empty"));
        for (NpcCensus.Row row : report.census())
        {
            lines.add(Component.translatable(KEY + "level", row.dimension(), row.total(), kinds(row)).withStyle(ChatFormatting.YELLOW));
            lines.add(Component.literal("  ").append(Component.translatable(KEY + "activity", row.fighting(), row.stayActive(), row.held(), row.refused(), row.dormant())));
        }
        return lines;
    }

    /** @return the kinds of NPC the level holds, the ones it has none of left out */
    private static Component kinds(NpcCensus.Row row)
    {
        MutableComponent kinds = Component.empty();
        for (NpcCensus.Kind kind : NpcCensus.Kind.values())
        {
            int count = row.count(kind);
            if (count == 0)
                continue;
            if (!kinds.getSiblings().isEmpty())
                kinds.append(", ");
            kinds.append(Component.translatable(KEY + "kind." + kind.name().toLowerCase(Locale.ROOT), count));
        }
        return kinds;
    }
}
