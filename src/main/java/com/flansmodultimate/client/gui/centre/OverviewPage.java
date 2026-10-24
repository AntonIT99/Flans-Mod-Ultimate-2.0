package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.ICommandCentreView;
import com.flansmodultimate.common.teams.centre.TeamsSection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The first page of the command centre: the round being played with its clock and standings, starting, skipping and
 * stopping rounds, switching Teams on and off, and what is still missing before a first round can be played.
 */
final class OverviewPage extends TeamsPage
{
    static final String ID = "flansmodultimate:overview";
    private static final int TEAMS_PANEL = 86;
    /** The steps a round needs before it can start: Teams on, a map, a round and bases on its map. */
    private static final int SET_UP_STEPS = 4;
    private static final String NEXT = "teams admin next";
    private static final String ENABLE = "teams admin enabled true";
    private static final String KIT = "teams admin kit";

    /** A step of setting up a first round, done or not, with where to go to do it. */
    private record Step(Component text, boolean done, Runnable go)
    {}

    private int leftWidth;
    private int rightLeft;
    private int rightWidth;

    OverviewPage()
    {
        super("overview", Items.COMPASS, 100);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        TeamsData data = data();
        leftWidth = Math.max(180, view.getWidth() * 58 / 100);
        rightLeft = view.getLeft() + leftWidth + 6;
        rightWidth = view.getWidth() - leftWidth - 6;
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT - 6;
        int half = (leftWidth - 12 - GAP) / 2;
        int left = view.getLeft() + 6;
        if (!data.enabled())
            button(view, tr("switch_on"), left, bottom, leftWidth - 12, () -> run(view, ENABLE));
        else if (data.live().isPresent())
        {
            button(view, tr("next_round"), left, bottom, half, tr("next_round.tip"), () -> run(view, NEXT));
            button(view, tr("stop_round"), left + half + GAP, bottom, half, () -> view.confirm(tr("stop_round.confirm"), () -> run(view, "teams admin stop")));
        }
        else if (data.rounds().isEmpty())
            button(view, tr("set_up_round"), left, bottom, leftWidth - 12, () -> RotationPage.openBuilder(view, ""));
        else
        {
            button(view, tr("start_next"), left, bottom, half, () -> run(view, NEXT));
            button(view, tr("choose_round"), left + half + GAP, bottom, half, () -> view.showPage(RotationPage.ID));
        }

        toggle(view, tr("teams_enabled"), data.enabled(), rightLeft + 6, view.getTop() + 18, rightWidth - 12, tr("teams_enabled.tip"), enabled -> run(view, "teams admin enabled", enabled));
        button(view, tr("kit"), rightLeft + 6, view.getTop() + TEAMS_PANEL - WIDGET_HEIGHT - 6, rightWidth - 12, tr("kit.tip"), () -> run(view, KIT));

        int stepsBottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT - 6;
        if (!setUp(view, data))
            steps(view, data).stream().filter(step -> !step.done()).findFirst().ifPresent(missing -> button(view, tr("do_step"), rightLeft + 6, stepsBottom, rightWidth - 12, missing.go()));
        else if (data.next() >= 0)
            button(view, tr("change_next"), rightLeft + 6, stepsBottom, rightWidth - 12, () -> view.showPage(RotationPage.ID));
    }

    /** Whether everything a round needs before it starts is done, so the page shows what comes next instead of the steps. */
    private static boolean setUp(ICommandCentreView view, TeamsData data)
    {
        // A round being played needs no setting up, whatever the steps say.
        return data.live().isPresent() || steps(view, data).stream().limit(SET_UP_STEPS).allMatch(Step::done);
    }

    /** What a first round needs, in the order it is set up. */
    private static List<Step> steps(ICommandCentreView view, TeamsData data)
    {
        Optional<TeamsData.MapInfo> nextMap = data.round(data.next()).flatMap(round -> data.map(round.map()));
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(tr("step.enabled"), data.enabled(), () -> run(view, ENABLE)));
        steps.add(new Step(tr("step.map"), !data.maps().isEmpty(), () -> view.showPage(MapsPage.ID)));
        steps.add(new Step(tr("step.round"), !data.rounds().isEmpty(), () -> RotationPage.openBuilder(view, "")));
        steps.add(new Step(tr("step.bases"), nextMap.map(map -> map.bases() > 0).orElse(false), () -> run(view, KIT)));
        steps.add(new Step(tr("step.playing"), data.live().isPresent(), () -> run(view, NEXT)));
        return steps;
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        TeamsData data = data();
        renderRound(view, graphics, data);
        view.drawPanel(graphics, rightLeft, view.getTop(), rightWidth, TEAMS_PANEL, tr("teams"));
        int countsTop = view.getTop() + 18 + WIDGET_HEIGHT + 5;
        label(view, graphics, tr("counts", data.maps().size(), data.rounds().size()), rightLeft + 6, countsTop, rightWidth - 12, CentreStyle.DIM);
        label(view, graphics, tr("online", data.players()), rightLeft + 6, countsTop + 10, rightWidth - 12, CentreStyle.DIM);

        int lowerTop = view.getTop() + TEAMS_PANEL + 6;
        int lowerHeight = view.getHeight() - TEAMS_PANEL - 6;
        if (setUp(view, data))
            renderNext(view, graphics, data, lowerTop, lowerHeight);
        else
            renderSteps(view, graphics, data, lowerTop, lowerHeight);
    }

    /** The steps of setting up a first round, those done ticked off. */
    private void renderSteps(ICommandCentreView view, GuiGraphics graphics, TeamsData data, int top, int height)
    {
        view.drawPanel(graphics, rightLeft, top, rightWidth, height, tr("getting_started"));
        int y = top + 18;
        for (Step step : steps(view, data))
        {
            if (y > top + height - WIDGET_HEIGHT - 16)
                break;
            graphics.drawString(view.getFont(), step.done() ? "✔" : "•", rightLeft + 7, y, step.done() ? CentreStyle.GOOD : CentreStyle.ACCENT);
            label(view, graphics, step.text(), rightLeft + 17, y, rightWidth - 23, step.done() ? CentreStyle.MUTED : CentreStyle.TEXT);
            y += 11;
        }
    }

    private void renderNext(ICommandCentreView view, GuiGraphics graphics, TeamsData data, int top, int height)
    {
        view.drawPanel(graphics, rightLeft, top, rightWidth, height, tr("up_next"));
        Optional<TeamsData.Round> next = data.round(data.next());
        if (next.isEmpty())
            return;
        TeamsData.Round round = next.get();
        int width = rightWidth - 12;
        label(view, graphics, Component.literal("#" + round.index() + " ").append(data.title(round)), rightLeft + 6, top + 18, width, CentreStyle.TEXT);
        label(view, graphics, data.lineup(round), rightLeft + 6, top + 29, width, CentreStyle.DIM);
        label(view, graphics, tr("limits", round.minutes(), round.scoreLimit()), rightLeft + 6, top + 40, width, CentreStyle.DIM);
        if (data.queued() >= 0)
            label(view, graphics, tr("queued"), rightLeft + 6, top + 51, width, CentreStyle.ACCENT);
        else if (data.settings().getBoolean(TeamsSection.VOTING))
            label(view, graphics, tr("vote_may_change"), rightLeft + 6, top + 51, width, CentreStyle.MUTED);
    }

    private void renderRound(ICommandCentreView view, GuiGraphics graphics, TeamsData data)
    {
        int left = view.getLeft();
        int top = view.getTop();
        int width = leftWidth - 12;
        view.drawPanel(graphics, left, top, leftWidth, view.getHeight(), tr("current_round"));
        if (!data.enabled())
        {
            paragraph(view, graphics, tr("teams_off_detail"), left + 6, top + 20, width, CentreStyle.DIM);
            return;
        }
        Optional<TeamsData.LiveRound> live = data.live();
        if (live.isEmpty())
        {
            int y = top + 20 + paragraph(view, graphics, tr(data.rounds().isEmpty() ? "rotation_empty" : "no_round_detail"), left + 6, top + 20, width, CentreStyle.DIM) + 8;
            data.round(data.next()).ifPresent(round ->
            {
                label(view, graphics, tr("next_is", data.title(round)), left + 6, y, width, CentreStyle.TEXT);
                label(view, graphics, data.lineup(round), left + 6, y + 11, width, CentreStyle.DIM);
            });
            return;
        }
        TeamsData.LiveRound round = live.get();
        label(view, graphics, data.title(round.round()), left + 6, top + 19, width, CentreStyle.TEXT);
        label(view, graphics, data.lineup(round.round()), left + 6, top + 30, width, CentreStyle.DIM);
        int elapsed = view.getTicksSinceUpdate();
        boolean breakTime = !"NONE".equals(round.phase());
        String clock = CentreStyle.clock(breakTime ? round.intermissionLeft() - elapsed : round.timeLeft() - elapsed);
        graphics.pose().pushPose();
        graphics.pose().translate(left + 6F, top + 44F, 0F);
        graphics.pose().scale(2F, 2F, 1F);
        graphics.drawString(view.getFont(), clock, 0, 0, breakTime ? CentreStyle.ACCENT : CentreStyle.TEXT);
        graphics.pose().popPose();
        int clockRight = left + 12 + view.getFont().width(clock) * 2;
        Component caption = breakTime ? tr("phase." + round.phase().toLowerCase(java.util.Locale.ROOT)) : tr("time_left", round.round().scoreLimit());
        label(view, graphics, caption, clockRight, top + 52, left + leftWidth - 6 - clockRight, CentreStyle.DIM);
        renderStandings(view, graphics, round, left + 6, top + 66, width, top + view.getHeight() - WIDGET_HEIGHT - 10);
    }

    /** One line for each side: its teams, its score and how many players it has. */
    private static void renderStandings(ICommandCentreView view, GuiGraphics graphics, TeamsData.LiveRound round, int x, int y, int width, int bottom)
    {
        List<Integer> sides = round.teams().stream().map(TeamsData.LiveTeam::side).distinct().toList();
        int line = y;
        for (int side : sides)
        {
            if (line + 12 > bottom)
                break;
            List<TeamsData.LiveTeam> teams = round.teams().stream().filter(team -> team.side() == side).toList();
            int score = teams.stream().mapToInt(TeamsData.LiveTeam::score).max().orElse(0);
            int players = teams.stream().mapToInt(TeamsData.LiveTeam::players).sum();
            graphics.fill(x, line, x + 3, line + 10, CentreStyle.opaque(teams.get(0).colour()));
            String scoreText = Integer.toString(score);
            Component playersText = tr("players_short", players);
            int right = x + width;
            graphics.drawString(view.getFont(), scoreText, right - view.getFont().width(scoreText), line + 1, CentreStyle.TEXT);
            int playersRight = right - view.getFont().width(scoreText) - 8;
            graphics.drawString(view.getFont(), playersText, playersRight - view.getFont().width(playersText), line + 1, CentreStyle.MUTED);
            Component names = Component.literal(String.join(" + ", teams.stream().map(TeamsData.LiveTeam::name).toList()));
            label(view, graphics, names, x + 7, line + 1, playersRight - view.getFont().width(playersText) - x - 12, CentreStyle.opaque(teams.get(0).colour()));
            line += 12;
        }
    }
}
