package com.wolffsmod.npcs.client.centre;

import com.flansmodultimate.api.client.CommandCentreRow;
import com.flansmodultimate.api.client.ICommandCentreView;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Skirmishes between managed factions: drafting one with its sides where the operator stands, its rules, starting,
 * pausing and stopping it, its standings while it is fought, a quick skirmish where the operator looks, and presets to
 * draft alike again.
 */
final class SkirmishesPage extends BattlePage
{
    private static final Pattern WORD = Pattern.compile("[0-9A-Za-z_.+-]+");
    private static final String NEW_NAME = "new_name";
    private static final String QUICK_COUNT = "quick_count";
    private static final String QUICK_DISTANCE = "quick_distance";
    private static final int DEFAULT_RADIUS = 16;
    private static final String PAUSED = "paused";
    private static final String STOP = "teams skirmish stop";
    private static final String SET = "teams skirmish set";
    private static final String INFANTRY = "skirmish.infantry";
    private static final String MINUTES = "skirmish.minutes";
    private static final String RESPAWN_SECONDS = "skirmish.respawn_seconds";

    private enum Mode
    {
        DETAIL, NEW, QUICK
    }

    private enum Tab
    {
        SIDES, RULES, SAVE
    }

    private String selected = "";
    private Mode mode = Mode.DETAIL;
    private Tab tab = Tab.SIDES;
    /** How many skirmishes there were when one was asked for, until it shows up; -1 when none is awaited. */
    private int awaitedCount = -1;
    private String awaitedName = "";
    private int sideFaction = -1;
    private int side = -1;
    private int quickFirst = -1;
    private int quickSecond = -1;
    private String preset = "";
    private int listWidth;
    /** Where the form for a new skirmish starts, below its help. */
    private int formTop;

    SkirmishesPage()
    {
        super("skirmishes", Items.IRON_SWORD, 700);
    }

    @Override
    void layout(ICommandCentreView view)
    {
        if (awaitedCount >= 0 && data.skirmishes().size() > awaitedCount)
        {
            mode = Mode.DETAIL;
            awaitedCount = -1;
            selected = data.skirmish(awaitedName).map(BattlesData.SkirmishInfo::name).orElse(data.skirmishes().get(data.skirmishes().size() - 1).name());
            awaitedName = "";
        }
        if (data.skirmish(selected).isEmpty())
            selected = data.skirmishes().isEmpty() ? "" : data.skirmishes().get(0).name();
        listWidth = Math.max(130, view.getWidth() * 38 / 100);
        int buttonWidth = (listWidth - GAP) / 2;
        button(view, tr("skirmish.new"), view.getLeft(), view.getTop(), buttonWidth, () -> switchMode(view, Mode.NEW));
        button(view, tr("skirmish.quick"), view.getLeft() + listWidth - buttonWidth, view.getTop(), buttonWidth, tr("skirmish.quick.tip"), () -> switchMode(view, Mode.QUICK));
        List<CommandCentreRow> rows = new ArrayList<>();
        for (BattlesData.SkirmishInfo skirmish : data.skirmishes())
        {
            String sides = String.join(" vs ", skirmish.sides().stream().map(BattlesData.Side::name).toList());
            rows.add(CommandCentreRow.of(skirmish.name(), Component.literal(skirmish.name()), Component.literal(sides.isEmpty() ? "-" : sides))
                .withColour(skirmish.sides().isEmpty() ? 0 : skirmish.sides().get(0).colour()).withBadge(value("state", skirmish.state()), stateColour(skirmish.state()))
                .selected(mode == Mode.DETAIL && skirmish.name().equals(selected)));
        }
        view.addList("skirmishes", new ScreenRectangle(view.getLeft(), view.getTop() + WIDGET_HEIGHT + GAP, listWidth, view.getHeight() - WIDGET_HEIGHT - GAP), rows, tr("skirmish.none"), key ->
        {
            selected = key;
            mode = Mode.DETAIL;
            view.rebuild();
        });
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        Optional<BattlesData.SkirmishInfo> skirmish = data.skirmish(selected);
        if (mode == Mode.QUICK)
            initQuick(view, x, width);
        else if (mode == Mode.NEW || skirmish.isEmpty())
            initNew(view, x, width, skirmish.isPresent());
        else
            initDetail(view, skirmish.get(), x, width);
    }

    private void switchMode(ICommandCentreView view, Mode chosen)
    {
        mode = chosen;
        awaitedCount = -1;
        view.rebuild();
    }

    private static int stateColour(String state)
    {
        return switch (state)
        {
            case "running" -> CommandCentreRow.GOOD;
            case PAUSED -> CommandCentreRow.WARNING;
            case "ended" -> CommandCentreRow.BAD;
            default -> CommandCentreRow.NEUTRAL;
        };
    }

    private List<Integer> factionIds()
    {
        return data.factions().stream().map(BattlesData.FactionInfo::id).toList();
    }

    private Component factionName(int id)
    {
        return Component.literal(data.factionName(id));
    }

    // --- Drafting and quick skirmishes ---

    private void initNew(ICommandCentreView view, int x, int width, boolean cancellable)
    {
        formTop = view.getTop() + 24 + view.getFont().wordWrapHeight(tr("skirmish.new_help"), width);
        int y = formTop;
        view.addField(NEW_NAME, x, y, width - 74, "", tr("skirmish.name"));
        button(view, tr("skirmish.create"), x + width - 70, y, 70, () ->
        {
            String name = view.getField(NEW_NAME).strip();
            if (name.isEmpty())
            {
                view.showMessage(tr("skirmish.name_needed"), true);
                return;
            }
            awaitedCount = data.skirmishes().size();
            awaitedName = name;
            run(view, "teams skirmish create", quote(name));
        });
        y += ROW + 14;
        if (!data.presets().isEmpty())
        {
            preset = keep(data.presets(), preset, "");
            int third = (width - 2 * GAP) / 3;
            choice(view, tr("skirmish.preset"), data.presets(), preset, Component::literal, x, y, width, chosen -> preset = chosen);
            y += ROW;
            button(view, tr("skirmish.apply_here"), x, y, third, tr("skirmish.apply_here.tip"), () -> applyPreset(view, true));
            button(view, tr("skirmish.apply_there"), x + third + GAP, y, third, tr("skirmish.apply_there.tip"), () -> applyPreset(view, false));
            button(view, tr("skirmish.remove_preset"), x + 2 * (third + GAP), y, third,
                () -> view.confirm(tr("skirmish.remove_preset.confirm", preset), () -> run(view, "teams skirmish preset remove", preset)));
        }
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT - 6;
        int half = (width - GAP) / 2;
        button(view, tr("skirmish.clear"), x, bottom, cancellable ? half : width, tr("skirmish.clear.tip"),
            () -> view.confirm(tr("skirmish.clear.confirm"), () -> run(view, "teams skirmish clear"))).active = !data.skirmishes().isEmpty();
        if (cancellable)
            button(view, tr("cancel"), x + half + GAP, bottom, half, () -> switchMode(view, Mode.DETAIL));
    }

    private void applyPreset(ICommandCentreView view, boolean here)
    {
        String name = view.getField(NEW_NAME).strip();
        if (name.isEmpty())
        {
            view.showMessage(tr("skirmish.name_needed"), true);
            return;
        }
        awaitedCount = data.skirmishes().size();
        awaitedName = name;
        if (here)
            run(view, "teams skirmish preset apply", preset, quote(name), "here");
        else
            run(view, "teams skirmish preset apply", preset, quote(name));
    }

    private void initQuick(ICommandCentreView view, int x, int width)
    {
        List<Integer> factions = factionIds();
        int y = view.getTop() + 24 + view.getFont().wordWrapHeight(tr(factions.size() < 2 ? "skirmish.need_factions" : "skirmish.quick_help"), width);
        if (factions.size() < 2)
        {
            button(view, tr("skirmish.to_factions"), x, y, width, () -> view.showPage(FactionsPage.ID));
            return;
        }
        quickFirst = keep(factions, quickFirst, -1);
        if (quickSecond == quickFirst || !factions.contains(quickSecond))
            quickSecond = factions.stream().filter(id -> id != quickFirst).findFirst().orElse(-1);
        choice(view, tr("skirmish.quick_near"), factions, quickFirst, this::factionName, x, y, width, chosen -> quickFirst = chosen);
        choice(view, tr("skirmish.quick_far"), factions, quickSecond, this::factionName, x, y + ROW, width, chosen -> quickSecond = chosen);
        y += 2 * ROW;
        int half = (width - GAP) / 2;
        view.addField(QUICK_COUNT, x, y, half, "16", tr(INFANTRY)).setTooltip(Tooltip.create(tr("skirmish.quick_count.tip")));
        view.addField(QUICK_DISTANCE, x + half + GAP, y, half, "96", tr("skirmish.distance")).setTooltip(Tooltip.create(tr("skirmish.quick_distance.tip")));
        y += ROW;
        button(view, tr("skirmish.quick_start"), x, y, width, () ->
        {
            if (quickFirst == quickSecond)
            {
                view.showMessage(tr("skirmish.same_faction"), true);
                return;
            }
            OptionalInt count = number(view, QUICK_COUNT, 0, 200, tr(INFANTRY));
            OptionalInt distance = count.isPresent() ? number(view, QUICK_DISTANCE, 16, 512, tr("skirmish.distance")) : OptionalInt.empty();
            if (distance.isEmpty())
                return;
            awaitedCount = data.skirmishes().size();
            run(view, "teams skirmish quick", quote(data.factionName(quickFirst)), quote(data.factionName(quickSecond)), count.getAsInt(), distance.getAsInt());
        });
    }

    // --- A skirmish ---

    private void initDetail(ICommandCentreView view, BattlesData.SkirmishInfo skirmish, int x, int width)
    {
        String name = quote(skirmish.name());
        int half = (width - GAP) / 2;
        int y = view.getTop() + 18;
        switch (skirmish.state())
        {
            case "draft" -> {
                button(view, tr("skirmish.start"), x, y, half, tr("skirmish.start.tip"), () -> run(view, "teams skirmish start", name)).active = skirmish.sides().size() >= 2;
                button(view, tr("skirmish.delete"), x + half + GAP, y, half, () -> view.confirm(tr("skirmish.delete.confirm", skirmish.name()), () -> run(view, STOP, name)));
            }
            case "running", PAUSED -> {
                boolean paused = PAUSED.equals(skirmish.state());
                button(view, tr(paused ? "skirmish.resume" : "skirmish.pause"), x, y, half, () -> run(view, paused ? "teams skirmish resume" : "teams skirmish pause", name));
                button(view, tr("skirmish.stop"), x + half + GAP, y, half, () -> view.confirm(tr("skirmish.stop.confirm", skirmish.name()), () -> run(view, STOP, name)));
            }
            default -> button(view, tr("skirmish.remove"), x, y, width, () -> run(view, STOP, name));
        }
        y += ROW;
        tabs(view, Tab.values(), tab, shown -> tr("skirmish.tab." + shown.name().toLowerCase(Locale.ROOT)), x, y, width, chosen -> tab = chosen);
        y += ROW;
        switch (tab)
        {
            case SIDES -> initSides(view, skirmish, x, y, width);
            case RULES -> initRules(view, skirmish, x, y, width);
            case SAVE -> {
                int fieldWidth = width - 94;
                view.addField("preset@" + skirmish.name(), x, y + 30, fieldWidth, "", tr("skirmish.preset_name"));
                button(view, tr("skirmish.save_preset"), x + fieldWidth + GAP, y + 30, 90, () ->
                {
                    String presetName = view.getField("preset@" + skirmish.name()).strip();
                    if (WORD.matcher(presetName).matches())
                        run(view, "teams skirmish preset save", name, presetName);
                    else
                        view.showMessage(tr("skirmish.bad_preset"), true);
                });
            }
        }
    }

    /** How a side stands in a skirmish being fought, or where it comes in and what it fields in a draft. */
    private static Component sideDetail(BattlesData.Side entry)
    {
        BattlesData.Fight fight = entry.fight();
        if (fight == null)
            return tr("skirmish.side_detail", entry.position(), (int) entry.radius(), entry.infantry() < 0 ? tr("skirmish.listed") : Component.literal(Integer.toString(entry.infantry())));
        if (fight.tickets() >= 0)
            return tr("skirmish.fight_tickets", fight.standing(), fight.kills(), fight.deaths(), fight.tickets());
        return tr("skirmish.fight", fight.standing(), fight.kills(), fight.deaths());
    }

    private void initSides(ICommandCentreView view, BattlesData.SkirmishInfo skirmish, int x, int top, int width)
    {
        String name = quote(skirmish.name());
        boolean editable = skirmish.draft();
        int listHeight = Math.max(44, view.getTop() + view.getHeight() - top - (editable ? 2 * ROW + GAP : 0));
        List<Integer> sideFactions = skirmish.sides().stream().map(BattlesData.Side::faction).toList();
        side = keep(sideFactions, side, -1);
        List<CommandCentreRow> rows = new ArrayList<>();
        for (BattlesData.Side entry : skirmish.sides())
            rows.add(
                CommandCentreRow.of(Integer.toString(entry.faction()), Component.literal(entry.name()), sideDetail(entry)).withColour(entry.colour()).selected(editable && entry.faction() == side));
        view.addList("sides@" + skirmish.name(), new ScreenRectangle(x, top, width, listHeight), rows, tr("skirmish.no_sides"), key ->
        {
            side = Integer.parseInt(key);
            view.rebuild();
        });
        if (!editable)
            return;
        int y = top + listHeight + GAP;
        List<Integer> free = factionIds().stream().filter(id -> !sideFactions.contains(id)).toList();
        if (!free.isEmpty())
        {
            sideFaction = keep(free, sideFaction, -1);
            int radiusWidth = 34;
            int addWidth = 64;
            int choiceWidth = width - radiusWidth - addWidth - 2 * GAP;
            choice(view, tr("skirmish.side_faction"), free, sideFaction, this::factionName, x, y, choiceWidth, chosen -> sideFaction = chosen);
            view.addField("radius@" + skirmish.name(), x + choiceWidth + GAP, y, radiusWidth, Integer.toString(DEFAULT_RADIUS), tr("skirmish.radius"))
                .setTooltip(Tooltip.create(tr("skirmish.radius.tip")));
            button(view, tr("skirmish.add_side"), x + width - addWidth, y, addWidth, tr("skirmish.add_side.tip"), () -> number(view, "radius@" + skirmish.name(), 2, 256, tr("skirmish.radius"))
                .ifPresent(radius -> run(view, "teams skirmish side", name, "add", quote(data.factionName(sideFaction)), radius)));
        }
        else if (data.factions().size() < 2)
            button(view, tr("skirmish.to_factions"), x, y, width, () -> view.showPage(FactionsPage.ID));
        y += ROW;
        Optional<BattlesData.Side> chosen = skirmish.sides().stream().filter(entry -> entry.faction() == side).findFirst();
        if (chosen.isEmpty())
            return;
        BattlesData.Side entry = chosen.get();
        String faction = quote(entry.name());
        String key = "infantry@" + skirmish.name() + "/" + entry.faction();
        int fieldWidth = width - 3 * 50 - 3 * GAP;
        view.addField(key, x, y, fieldWidth, entry.infantry() < 0 ? "" : Integer.toString(entry.infantry()), tr(INFANTRY)).setTooltip(Tooltip.create(tr("skirmish.infantry.tip")));
        button(view, tr("set"), x + fieldWidth + GAP, y, 50, () -> number(view, key, 0, 500, tr(INFANTRY)).ifPresent(count -> run(view, "teams skirmish force", name, faction, "infantry", count)));
        button(view, tr("skirmish.listed"), x + fieldWidth + 50 + 2 * GAP, y, 50, tr("skirmish.listed.tip"), () -> run(view, "teams skirmish force", name, faction, "listed"));
        button(view, tr("skirmish.remove_side"), x + width - 50, y, 50, () -> run(view, "teams skirmish side", name, "remove", faction));
    }

    private void initRules(ICommandCentreView view, BattlesData.SkirmishInfo skirmish, int x, int top, int width)
    {
        String name = quote(skirmish.name());
        BattlesData.Rules rules = skirmish.rules();
        BattlesData.Choices choices = data.choices();
        int half = (width - GAP) / 2;
        int fieldWidth = half - 44;
        int right = x + half + GAP;
        int y = top;
        if (!choices.intents().isEmpty())
            choice(view, tr("skirmish.intent"), choices.intents(), keep(choices.intents(), rules.intent(), choices.intents().get(0)), intent -> value("intent", intent), x, y, half,
                intent -> run(view, SET, name, "intent", intent)).setTooltip(Tooltip.create(tr("skirmish.intent.tip")));
        if (!choices.difficulties().isEmpty())
            choice(view, tr("skirmish.difficulty"), choices.difficulties(), keep(choices.difficulties(), rules.difficulty(), choices.difficulties().get(0)),
                difficulty -> value("difficulty", difficulty), right, y, half, difficulty -> run(view, SET, name, "difficulty", difficulty)).active = skirmish.draft();
        y += ROW;
        String minutesKey = "minutes@" + skirmish.name();
        if (!choices.ends().isEmpty())
            choice(view, tr("skirmish.end"), choices.ends(), keep(choices.ends(), rules.end(), choices.ends().get(0)), end -> value("end", end), x, y, half, end ->
            {
                if (!"time".equals(end))
                    run(view, SET, name, "end", end);
                else
                    // A choice the field refuses goes back to the end the skirmish has.
                    number(view, minutesKey, 1, 600, tr(MINUTES)).ifPresentOrElse(minutes -> run(view, SET, name, "end time", minutes), view::rebuild);
            }).setTooltip(Tooltip.create(tr("skirmish.end.tip")));
        view.addField(minutesKey, right, y, fieldWidth, Integer.toString(rules.minutes()), tr(MINUTES)).setTooltip(Tooltip.create(tr(MINUTES)));
        button(view, tr("set"), right + fieldWidth + GAP, y, 40, () -> number(view, minutesKey, 1, 600, tr(MINUTES)).ifPresent(minutes -> run(view, SET, name, "end time", minutes)));
        y += ROW;
        String respawnKey = "respawn@" + skirmish.name();
        toggle(view, tr("skirmish.respawn"), rules.respawn(), x, y, half, enabled ->
        {
            if (Boolean.TRUE.equals(enabled))
                number(view, respawnKey, 0, 3600, tr(RESPAWN_SECONDS)).ifPresentOrElse(seconds -> run(view, SET, name, "respawn", seconds), view::rebuild);
            else
                run(view, SET, name, "respawn off");
        }).active = skirmish.draft();
        view.addField(respawnKey, right, y, fieldWidth, Integer.toString(rules.respawnSeconds()), tr(RESPAWN_SECONDS)).setTooltip(Tooltip.create(tr(RESPAWN_SECONDS)));
        button(view, tr("set"), right + fieldWidth + GAP, y, 40,
            () -> number(view, respawnKey, 0, 3600, tr(RESPAWN_SECONDS)).ifPresent(seconds -> run(view, SET, name, "respawn", seconds))).active = skirmish.draft();
        y += ROW;
        String ticketsKey = "tickets@" + skirmish.name();
        view.addField(ticketsKey, x, y, fieldWidth, Integer.toString(rules.tickets()), tr("skirmish.tickets")).setTooltip(Tooltip.create(tr("skirmish.tickets.tip")));
        button(view, tr("set"), x + fieldWidth + GAP, y, 40,
            () -> number(view, ticketsKey, 0, Integer.MAX_VALUE, tr("skirmish.tickets")).ifPresent(tickets -> run(view, SET, name, "tickets", tickets))).active = skirmish.draft();
        toggle(view, tr("skirmish.protection"), rules.protection(), right, y, half, enabled -> run(view, SET, name, "protection", Boolean.TRUE.equals(enabled) ? "on" : "off"))
            .setTooltip(Tooltip.create(tr("skirmish.protection.tip")));
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int panelLeft = view.getLeft() + listWidth + 6;
        int panelWidth = view.getWidth() - listWidth - 6;
        int x = panelLeft + 6;
        int width = panelWidth - 12;
        Optional<BattlesData.SkirmishInfo> shown = data.skirmish(selected);
        if (mode == Mode.QUICK)
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("skirmish.quick_title"));
            paragraph(view, graphics, tr(data.factions().size() < 2 ? "skirmish.need_factions" : "skirmish.quick_help"), x, view.getTop() + 18, width, DIM);
            return;
        }
        if (mode == Mode.NEW || shown.isEmpty())
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("skirmish.new_title"));
            paragraph(view, graphics, tr("skirmish.new_help"), x, view.getTop() + 18, width, DIM);
            if (!data.presets().isEmpty())
                label(view, graphics, tr("skirmish.or_preset"), x, formTop + ROW + 4, width, MUTED);
            return;
        }
        BattlesData.SkirmishInfo skirmish = shown.get();
        view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), Component.literal(skirmish.name()).append(" · ").append(value("state", skirmish.state())));
        int y = view.getTop() + 18 + 2 * ROW;
        if (tab == Tab.SAVE)
            paragraph(view, graphics, tr("skirmish.save_help"), x, y, width, DIM);
        if (tab == Tab.RULES && !skirmish.draft())
            label(view, graphics, tr("skirmish.draft_only"), x, view.getTop() + view.getHeight() - 11, width, MUTED);
        if (skirmish.war())
            label(view, graphics, tr("skirmish.of_war"), x, view.getTop() + view.getHeight() - 11, width, ACCENT);
    }
}
