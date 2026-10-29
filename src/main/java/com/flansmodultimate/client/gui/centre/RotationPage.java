package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.common.teams.TeamsRound;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.stream.IntStream;

/**
 * The rotation: its rounds in the order they are played, starting or queueing one, moving it, changing its length,
 * score limit and attackers, removing it, and a builder that adds a round by picking its map, game type and the teams
 * of each side.
 */
final class RotationPage extends TeamsPage
{
    static final String ID = "flansmodultimate:rotation";
    private static final int DEFAULT_MINUTES = 15;
    private static final String NEW_MINUTES = "new_minutes";
    private static final String NEW_SCORE = "new_score";
    private static final String MINUTES = "minutes";
    private static final String SCORE_LIMIT = "score_limit";

    private String selected = "";
    private boolean building;
    private String map = "";
    private String gameType = "";
    private final List<List<String>> sides = new ArrayList<>();
    private int activeSide;
    /** The size the rotation had when the builder added a round, until the round shows up; -1 when none is awaited. */
    private int awaitedSize = -1;
    private int listWidth;
    /** The top of the fields of the shown round's limits, whose labels are drawn above them. */
    private int limitsTop;

    RotationPage()
    {
        super("rotation", Items.CLOCK, 200);
        for (int i = 0; i < TeamsRound.MAX_SIDES; i++)
            sides.add(new ArrayList<>());
    }

    /** Shows the rotation page with the builder open for a new round, on a map or, with an empty id, the first one. */
    static void openBuilder(ICommandCentreView view, String onMap)
    {
        CommandCentreClient.rotationPage().ifPresent(rotation -> rotation.startBuilding(onMap));
        view.showPage(ID);
    }

    /** Opens the builder for a new round, on a map or, with an empty id, the first one. */
    private void startBuilding(String onMap)
    {
        building = true;
        map = onMap;
        sides.forEach(List::clear);
        activeSide = 0;
        awaitedSize = -1;
    }

    @Override
    public void init(ICommandCentreView view)
    {
        TeamsData data = data();
        if (awaitedSize >= 0 && data.rounds().size() > awaitedSize)
        {
            building = false;
            awaitedSize = -1;
            selected = data.rounds().get(data.rounds().size() - 1).id();
        }
        if (building)
            initBuilder(view, data);
        else
            initList(view, data);
    }

    // --- The rotation ---

    private void initList(ICommandCentreView view, TeamsData data)
    {
        listWidth = Math.max(150, view.getWidth() * 45 / 100);
        button(view, tr("new_round"), view.getLeft(), view.getTop(), listWidth, () ->
        {
            startBuilding("");
            view.rebuild();
        });
        if (data.roundById(selected).isEmpty())
            selected = data.rounds().isEmpty() ? "" : data.rounds().get(0).id();
        List<CommandCentreRow> rows = new ArrayList<>();
        for (TeamsData.Round round : data.rounds())
        {
            CommandCentreRow row = CommandCentreRow.of(round.id(), Component.literal("#" + round.index() + "  ").append(data.title(round)), data.lineup(round)).selected(round.id().equals(selected));
            if (round.index() == data.current())
                row = row.withBadge(tr("badge.live"), CommandCentreRow.GOOD);
            else if (round.index() == data.queued())
                row = row.withBadge(tr("badge.queued"), CommandCentreRow.WARNING);
            else if (!playable(data, round))
                row = row.withBadge(tr("badge.unplayable"), CommandCentreRow.BAD);
            else if (round.index() == data.next())
                row = row.withBadge(tr("badge.next"), CommandCentreRow.NEUTRAL);
            rows.add(row);
        }
        view.addList("rounds", new ScreenRectangle(view.getLeft(), view.getTop() + WIDGET_HEIGHT + GAP, listWidth, view.getHeight() - WIDGET_HEIGHT - GAP), rows, tr("rotation_empty_list"), key ->
        {
            selected = key;
            view.rebuild();
        });
        data.roundById(selected).ifPresent(round -> initRound(view, data, round));
    }

    private void initRound(ICommandCentreView view, TeamsData data, TeamsData.Round round)
    {
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        int half = (width - GAP) / 2;
        int index = round.index();
        int y = view.getTop() + 54;
        button(view, tr("start_now"), x, y, half, tr("start_now.tip"), () -> run(view, "teams admin start", index));
        button(view, tr("play_next"), x + half + GAP, y, half, tr("play_next.tip"), () -> run(view, "teams admin setnext", index)).active = index != data.queued();
        y += ROW;
        button(view, tr("move_up"), x, y, half, () -> run(view, "teams admin round move", index, index - 1)).active = index > 0;
        button(view, tr("move_down"), x + half + GAP, y, half, () -> run(view, "teams admin round move", index, index + 1)).active = index < data.rounds().size() - 1;
        y += ROW;
        if (round.canAttack())
        {
            List<Integer> choices = IntStream.range(-1, round.sideCount()).boxed().toList();
            List<List<String>> bySide = round.teamsBySide();
            choice(view, tr("attackers"), choices, Math.min(round.attackers(), round.sideCount() - 1), side -> side < 0 ? tr("attackers.none") : sideName(data, bySide.get(side)), x, y, width,
                side -> run(view, "teams admin round attackers", index, side < 0 ? "none" : Integer.toString(side + 1)));
            y += ROW;
        }
        // The limits are labelled above their fields.
        y += 10;
        limitsTop = y;
        int fieldWidth = (width - 60 - 2 * GAP) / 2;
        view.addField("minutes@" + round.id(), x, y, fieldWidth, Integer.toString(round.minutes()), tr(MINUTES)).setTooltip(Tooltip.create(tr(MINUTES)));
        view.addField("score@" + round.id(), x + fieldWidth + GAP, y, fieldWidth, Integer.toString(round.scoreLimit()), tr(SCORE_LIMIT)).setTooltip(Tooltip.create(tr(SCORE_LIMIT)));
        button(view, tr("apply"), x + width - 60, y, 60, tr("limits.tip"), () ->
        {
            OptionalInt minutes = number(view, "minutes@" + round.id(), 1, 1440, tr(MINUTES));
            OptionalInt score = minutes.isPresent() ? number(view, "score@" + round.id(), 1, Integer.MAX_VALUE, tr(SCORE_LIMIT)) : OptionalInt.empty();
            if (score.isPresent())
                run(view, "teams admin round limits", index, minutes.getAsInt(), score.getAsInt());
        });
        y += ROW;
        button(view, tr("remove_round"), x, y, width, () -> view.confirm(tr("remove_round.confirm", data.title(round)), () -> run(view, "teams admin round remove", index)));
    }

    /** Whether a round's game type can be played on its map: objective modes need capture points there. */
    private static boolean playable(TeamsData data, TeamsData.Round round)
    {
        return data.map(round.map()).map(map -> map.playable().contains(round.gameType())).orElse(true);
    }

    private static MutableComponent sideName(TeamsData data, List<String> teams)
    {
        MutableComponent name = Component.empty();
        for (int i = 0; i < teams.size(); i++)
        {
            if (i > 0)
                name.append(" + ");
            name.append(data.teamName(teams.get(i)));
        }
        return name;
    }

    // --- The builder ---

    private void initBuilder(ICommandCentreView view, TeamsData data)
    {
        int left = view.getLeft() + 6;
        int width = view.getWidth() - 12;
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT - 6;
        if (data.maps().isEmpty())
        {
            button(view, tr("go_to_maps"), left, bottom, width / 2 - 2, () -> view.showPage(MapsPage.ID));
            button(view, tr("cancel"), left + width / 2 + 2, bottom, width / 2 - 2, () -> cancel(view));
            return;
        }
        if (data.map(map).isEmpty())
            map = data.maps().get(0).id();
        TeamsData.MapInfo chosenMap = data.map(map).orElseThrow();
        if (data.gameType(gameType).isEmpty())
            gameType = data.gameTypes().stream().map(TeamsData.GameTypeInfo::id).filter(chosenMap.playable()::contains).findFirst()
                .orElse(data.gameTypes().isEmpty() ? "" : data.gameTypes().get(0).id());
        int half = (width - GAP) / 2;
        int y = view.getTop() + 18;
        choice(view, tr("builder.map"), data.maps().stream().map(TeamsData.MapInfo::id).toList(), map, id -> Component.literal(data.mapName(id)), left, y, half, chosen ->
        {
            map = chosen;
            view.rebuild();
        });
        if (!data.gameTypes().isEmpty())
            choice(view, tr("builder.game_type"), data.gameTypes().stream().map(TeamsData.GameTypeInfo::id).toList(), gameType, id -> Component.literal(data.gameTypeName(id)), left + half + GAP, y,
                half, chosen ->
                {
                    gameType = chosen;
                    view.rebuild();
                });
        y += ROW + 12;
        int sideCount = maxSides(data);
        activeSide = Math.min(activeSide, sideCount - 1);
        int sideWidth = (width - GAP * (sideCount - 1)) / sideCount;
        for (int side = 0; side < sideCount; side++)
        {
            int chosen = side;
            Button tab = button(view, tr("builder.side", side + 1, sides.get(side).size()), left + side * (sideWidth + GAP), y, sideWidth, () ->
            {
                activeSide = chosen;
                view.rebuild();
            });
            tab.active = side != activeSide;
        }
        y += ROW;
        int pickerLeft = left + half + GAP;
        List<CommandCentreRow> rows = new ArrayList<>();
        for (TeamsData.TeamInfo team : data.teams())
        {
            int side = sideOf(team.id());
            CommandCentreRow row = CommandCentreRow.of(team.id(), Component.literal(team.name()), Component.literal(team.id())).withColour(team.colour()).selected(side == activeSide);
            rows.add(side < 0 ? row : row.withBadge(tr("builder.on_side", side + 1), side == activeSide ? CommandCentreRow.GOOD : CommandCentreRow.NEUTRAL));
        }
        view.addList("teams", new ScreenRectangle(pickerLeft, y, half, bottom - y - GAP - 10), rows, tr("builder.no_teams_loaded"), team ->
        {
            toggleTeam(team);
            view.rebuild();
        });
        int fieldWidth = 46;
        int defaultScore = data.gameType(gameType).map(TeamsData.GameTypeInfo::defaultScore).orElse(1);
        view.addField(NEW_MINUTES, left, bottom, fieldWidth, Integer.toString(DEFAULT_MINUTES), tr(MINUTES)).setTooltip(Tooltip.create(tr(MINUTES)));
        view.addField(NEW_SCORE, left + fieldWidth + GAP, bottom, fieldWidth + 10, Integer.toString(defaultScore), tr(SCORE_LIMIT)).setTooltip(Tooltip.create(tr(SCORE_LIMIT)));
        int buttonsLeft = left + 2 * fieldWidth + 10 + 2 * GAP;
        int buttonWidth = (left + width - buttonsLeft - 2 * GAP) / 3;
        button(view, tr("builder.add"), buttonsLeft, bottom, buttonWidth, () -> addRound(view, data));
        button(view, tr("builder.clear"), buttonsLeft + buttonWidth + GAP, bottom, buttonWidth, () ->
        {
            sides.forEach(List::clear);
            view.rebuild();
        });
        button(view, tr("cancel"), buttonsLeft + 2 * (buttonWidth + GAP), bottom, buttonWidth, () -> cancel(view));
    }

    private int maxSides(TeamsData data)
    {
        return Math.max(1, Math.min(TeamsRound.MAX_SIDES, data.gameType(gameType).map(TeamsData.GameTypeInfo::maxSides).orElse(TeamsRound.MAX_SIDES)));
    }

    private int sideOf(String team)
    {
        for (int side = 0; side < sides.size(); side++)
            if (sides.get(side).contains(team))
                return side;
        return -1;
    }

    /** Puts a team on the side being filled, or takes it off when it is there already. */
    private void toggleTeam(String team)
    {
        int side = sideOf(team);
        if (side >= 0)
            sides.get(side).remove(team);
        if (side != activeSide)
            sides.get(activeSide).add(team);
    }

    private void cancel(ICommandCentreView view)
    {
        building = false;
        awaitedSize = -1;
        view.rebuild();
    }

    private void addRound(ICommandCentreView view, TeamsData data)
    {
        List<String> lineup = sides.stream().limit(maxSides(data)).filter(side -> !side.isEmpty()).map(side -> quote(String.join("+", side))).toList();
        if (lineup.isEmpty())
        {
            view.showMessage(tr("builder.no_teams"), true);
            return;
        }
        OptionalInt minutes = number(view, NEW_MINUTES, 1, 1440, tr(MINUTES));
        OptionalInt score = minutes.isPresent() ? number(view, NEW_SCORE, 1, Integer.MAX_VALUE, tr(SCORE_LIMIT)) : OptionalInt.empty();
        if (score.isEmpty())
            return;
        awaitedSize = data.rounds().size();
        run(view, "teams admin round add", map, gameType, String.join(" vs ", lineup), minutes.getAsInt(), score.getAsInt());
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        TeamsData data = data();
        if (building)
        {
            renderBuilder(view, graphics, data);
            return;
        }
        int x = view.getLeft() + listWidth + 6;
        int width = view.getWidth() - listWidth - 6;
        Optional<TeamsData.Round> shown = data.roundById(selected);
        if (shown.isEmpty())
        {
            view.drawPanel(graphics, x, view.getTop(), width, view.getHeight(), tr("round"));
            paragraph(view, graphics, tr("rotation_help"), x + 6, view.getTop() + 20, width - 12, CentreStyle.DIM);
            return;
        }
        TeamsData.Round round = shown.get();
        view.drawPanel(graphics, x, view.getTop(), width, view.getHeight(), Component.literal("#" + round.index() + "  ").append(data.title(round)));
        label(view, graphics, data.lineup(round), x + 6, view.getTop() + 18, width - 12, CentreStyle.TEXT);
        label(view, graphics, tr("limits", round.minutes(), round.scoreLimit()), x + 6, view.getTop() + 29, width - 12, CentreStyle.DIM);
        Component attackers = round.attackers() >= 0 && round.attackers() < round.sideCount()
            ? tr("attack_round", sideName(data, round.teamsBySide().get(round.attackers())))
            : tr(round.canAttack() ? "every_side_captures" : "no_attackers");
        if (playable(data, round))
            label(view, graphics, attackers, x + 6, view.getTop() + 40, width - 12, CentreStyle.DIM);
        else
            label(view, graphics, tr("unplayable"), x + 6, view.getTop() + 40, width - 12, CentreStyle.BAD);
        int fieldWidth = (width - 12 - 60 - 2 * GAP) / 2;
        label(view, graphics, tr(MINUTES), x + 6, limitsTop - 10, fieldWidth, CentreStyle.MUTED);
        label(view, graphics, tr(SCORE_LIMIT), x + 6 + fieldWidth + GAP, limitsTop - 10, fieldWidth, CentreStyle.MUTED);
    }

    private void renderBuilder(ICommandCentreView view, GuiGraphics graphics, TeamsData data)
    {
        int left = view.getLeft() + 6;
        int width = view.getWidth() - 12;
        view.drawPanel(graphics, view.getLeft(), view.getTop(), view.getWidth(), view.getHeight(), tr("builder.title"));
        if (data.maps().isEmpty())
        {
            paragraph(view, graphics, tr("builder.no_maps"), left, view.getTop() + 20, width, CentreStyle.DIM);
            return;
        }
        int y = view.getTop() + 18 + ROW;
        Optional<TeamsData.GameTypeInfo> type = data.gameType(gameType);
        type.ifPresent(found ->
        {
            Component limits = tr("builder.limits", range(found.minTeams(), found.maxTeams()), range(found.minSides(), found.maxSides()));
            boolean playable = data.map(map).map(chosen -> chosen.playable().contains(found.id())).orElse(true);
            label(view, graphics, playable ? limits : tr("builder.not_playable"), left, y, width, playable ? CentreStyle.DIM : CentreStyle.BAD);
        });
        int sidesTop = y + 12 + ROW;
        int half = (width - GAP) / 2;
        int lineTop = sidesTop;
        for (int side = 0; side < maxSides(data); side++)
        {
            boolean active = side == activeSide;
            graphics.fill(left, lineTop, left + 2, lineTop + 10, active ? CentreStyle.ACCENT : CentreStyle.PANEL_BORDER);
            List<String> teams = sides.get(side);
            Component line = tr("builder.side_line", side + 1, teams.isEmpty() ? tr("builder.side_empty") : sideName(data, teams));
            label(view, graphics, line, left + 6, lineTop + 1, half - 8, active ? CentreStyle.TEXT : CentreStyle.DIM);
            lineTop += 12;
        }
        paragraph(view, graphics, tr("builder.help"), left, lineTop + 6, half - 4, CentreStyle.MUTED);
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT - 6;
        label(view, graphics, tr(MINUTES), left, bottom - 10, 46, CentreStyle.MUTED);
        label(view, graphics, tr(SCORE_LIMIT), left + 46 + GAP, bottom - 10, 56, CentreStyle.MUTED);
    }
}
