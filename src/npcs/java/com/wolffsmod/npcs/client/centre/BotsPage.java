package com.wolffsmod.npcs.client.centre;

import com.flansmodultimate.api.client.CommandCentreRow;
import com.flansmodultimate.api.client.ICommandCentreView;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The bots of each round of the rotation: switching them on, applying a preset of bot settings, opening the bot editor
 * for everything else, how the bots of the round being played stand, and the markers bots use on a round's map.
 */
final class BotsPage extends BattlePage
{
    /** The command centre's rotation page, where rounds are added. */
    private static final String ROTATION = "flansmodultimate:rotation";
    private static final String MARKER = "teams ai marker";

    private int selected = -1;
    private String preset = "";
    private String markerType = "";
    private int listWidth;

    BotsPage()
    {
        super("bots", Items.ARMOR_STAND, 900);
    }

    @Override
    void layout(ICommandCentreView view)
    {
        List<Integer> indexes = data.rounds().stream().map(BattlesData.RoundInfo::index).toList();
        if (!indexes.contains(selected))
            selected = data.rounds().stream().filter(BattlesData.RoundInfo::current).map(BattlesData.RoundInfo::index).findFirst().orElse(indexes.isEmpty() ? -1 : indexes.get(0));
        listWidth = Math.max(130, view.getWidth() * 40 / 100);
        List<CommandCentreRow> rows = new ArrayList<>();
        data.rounds().forEach(round -> rows.add(row(round)));
        view.addList("rounds", new ScreenRectangle(view.getLeft(), view.getTop() + 14, listWidth, view.getHeight() - 14), rows, tr("bots.no_rounds"), key ->
        {
            selected = Integer.parseInt(key);
            view.rebuild();
        });
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        Optional<BattlesData.RoundInfo> round = data.rounds().stream().filter(entry -> entry.index() == selected).findFirst();
        if (round.isEmpty())
        {
            button(view, tr("bots.to_rotation"), x, view.getTop() + 50, width, () -> view.showPage(ROTATION));
            return;
        }
        initRound(view, round.get(), x, width);
    }

    /** A round in the list: its bots, and whether it is played, or its bots are on. */
    private CommandCentreRow row(BattlesData.RoundInfo round)
    {
        Component detail = round.enabled() ? tr("bots.detail", round.total(), value("mode", round.mode()), value("difficulty", round.difficulty())) : tr("bots.off");
        CommandCentreRow row = CommandCentreRow.of(Integer.toString(round.index()), tr("bots.round", round.index(), round.gameType(), round.map()), detail).selected(round.index() == selected);
        if (round.current())
            return row.withBadge(tr("badge.live"), CommandCentreRow.GOOD);
        return row.withBadge(tr(round.enabled() ? "badge.on" : "badge.off"), round.enabled() ? CommandCentreRow.NEUTRAL : CommandCentreRow.WARNING);
    }

    private void initRound(ICommandCentreView view, BattlesData.RoundInfo round, int x, int width)
    {
        int index = round.index();
        int half = (width - GAP) / 2;
        int y = view.getTop() + 18;
        button(view, tr("bots.editor"), x, y, half, tr("bots.editor.tip"), () -> run(view, "teams ai editor", index));
        toggle(view, tr("bots.enabled"), round.enabled(), x + half + GAP, y, half, enabled -> run(view, "teams ai round", index, Boolean.TRUE.equals(enabled) ? "enable" : "disable"));
        y += ROW;
        if (!data.botPresets().isEmpty())
        {
            preset = keep(data.botPresets(), preset, "");
            int buttonWidth = 50;
            int choiceWidth = width - 2 * buttonWidth - 2 * GAP;
            choice(view, tr("bots.preset"), data.botPresets(), preset, Component::literal, x, y, choiceWidth, chosen -> preset = chosen);
            button(view, tr("bots.apply"), x + choiceWidth + GAP, y, buttonWidth, tr("bots.apply.tip"), () -> run(view, "teams ai preset apply", preset, index));
            button(view, tr("bots.apply_all"), x + width - buttonWidth, y, buttonWidth, tr("bots.apply_all.tip"),
                () -> view.confirm(tr("bots.apply_all.confirm", preset), () -> run(view, "teams ai preset apply", preset, "all")));
        }
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT;
        String map = round.map();
        List<String> types = data.choices().markerTypes();
        int quarter = (width - 3 * GAP) / 4;
        if (!types.isEmpty())
        {
            markerType = keep(types, markerType, types.get(0));
            choice(view, tr("bots.marker_type"), types, markerType, type -> value("marker", type), x, bottom - ROW, 2 * quarter + GAP, chosen -> markerType = chosen);
            button(view, tr("bots.marker_place"), x + 2 * (quarter + GAP), bottom - ROW, quarter, tr("bots.marker_place.tip"), () -> run(view, MARKER, map, "add", markerType));
            button(view, tr("bots.marker_remove"), x + 3 * (quarter + GAP), bottom - ROW, quarter, tr("bots.marker_remove.tip"), () -> run(view, MARKER, map, "remove"));
        }
        int third = (width - 2 * GAP) / 3;
        button(view, tr("bots.marker_show"), x, bottom, third, tr("bots.marker_show.tip"), () -> run(view, MARKER, map, "show"));
        button(view, tr("bots.marker_clear"), x + third + GAP, bottom, third,
            () -> view.confirm(tr("bots.marker_clear.confirm", round.markers(), map), () -> run(view, MARKER, map, "clear"))).active = round.markers() > 0;
        button(view, tr("bots.clear"), x + 2 * (third + GAP), bottom, third, tr("bots.clear.tip"), () -> view.confirm(tr("bots.clear.confirm"), () -> run(view, "teams ai clear")));
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        label(view, graphics, tr("bots.title_list"), view.getLeft() + 2, view.getTop() + 2, listWidth - 4, ACCENT);
        int panelLeft = view.getLeft() + listWidth + 6;
        int panelWidth = view.getWidth() - listWidth - 6;
        int x = panelLeft + 6;
        int width = panelWidth - 12;
        Optional<BattlesData.RoundInfo> shown = data.rounds().stream().filter(entry -> entry.index() == selected).findFirst();
        if (shown.isEmpty())
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("bots.title"));
            paragraph(view, graphics, tr("bots.rotation_empty"), x, view.getTop() + 18, width, DIM);
            return;
        }
        BattlesData.RoundInfo round = shown.get();
        view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("bots.round", round.index(), round.gameType(), round.map()));
        int y = view.getTop() + 18 + 2 * ROW + 2;
        if (!round.configured())
            y += paragraph(view, graphics, tr("bots.not_configured"), x, y, width, MUTED) + 3;
        if (round.current() && !data.live().isEmpty())
        {
            label(view, graphics, tr("bots.live"), x, y, width, ACCENT);
            for (BattlesData.LiveTeam team : data.live())
            {
                y += 10;
                if (y > view.getTop() + view.getHeight() - 2 * ROW - 22)
                    break;
                graphics.fill(x, y, x + 3, y + 8, 0xFF000000 | team.colour());
                Component line = team.tickets() >= 0
                    ? tr("bots.live_team_tickets", team.team(), team.alive(), team.slots(), team.tickets())
                    : tr("bots.live_team", team.team(), team.alive(), team.slots());
                label(view, graphics, line, x + 6, y, width - 6, DIM);
            }
        }
        label(view, graphics, tr("bots.markers", round.markers(), round.map()), x, view.getTop() + view.getHeight() - WIDGET_HEIGHT - ROW - 11, width, MUTED);
    }
}
