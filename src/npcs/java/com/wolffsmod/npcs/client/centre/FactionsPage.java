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
 * The factions managed for battles outside Teams rounds: making one, or one of a Flan team, how it treats players of no
 * faction, joining it, the factions it is at war with, and the Flan teams that serve it as branches with what each
 * fields.
 */
final class FactionsPage extends BattlePage
{
    static final String ID = "wolffsmodnpcs:factions";
    private static final Pattern COLOUR = Pattern.compile("#?[0-9A-Fa-f]{6}");
    private static final String NEW_NAME = "new_name";
    private static final String NEW_COLOUR = "new_colour";
    private static final String CREATE_COMMAND = "teams faction create";
    private static final String BRANCH_COMMAND = "teams faction branch";

    private enum Tab
    {
        GENERAL, RELATIONS, BRANCHES
    }

    private int selected = -1;
    private boolean creating;
    /** The faction just asked for, selected once it shows up. */
    private String awaited = "";
    private Tab tab = Tab.GENERAL;
    private String fromTeam = "";
    private String newBranch = "";
    private String branch = "";
    private int listWidth;
    /** Where the form for a new faction starts, below its help. */
    private int formTop;

    FactionsPage()
    {
        super("factions", Items.RED_BANNER, 600);
    }

    @Override
    void layout(ICommandCentreView view)
    {
        if (!awaited.isEmpty())
            data.factions().stream().filter(faction -> faction.name().equalsIgnoreCase(awaited)).findFirst().ifPresent(faction ->
            {
                selected = faction.id();
                creating = false;
                awaited = "";
            });
        if (data.managed(selected).isEmpty())
            selected = data.factions().isEmpty() ? -1 : data.factions().get(0).id();
        listWidth = Math.max(130, view.getWidth() * 38 / 100);
        button(view, tr("faction.new"), view.getLeft(), view.getTop(), listWidth, () ->
        {
            creating = true;
            view.rebuild();
        });
        List<CommandCentreRow> rows = new ArrayList<>();
        for (BattlesData.FactionInfo faction : data.factions())
        {
            CommandCentreRow row = CommandCentreRow.of(Integer.toString(faction.id()), Component.literal(faction.name()), tr("faction.detail", faction.branches().size(), faction.members()))
                .withColour(faction.colour()).selected(faction.id() == selected);
            rows.add(faction.member() ? row.withBadge(tr("badge.member"), CommandCentreRow.GOOD) : row);
        }
        view.addList("factions", new ScreenRectangle(view.getLeft(), view.getTop() + WIDGET_HEIGHT + GAP, listWidth, view.getHeight() - WIDGET_HEIGHT - GAP), rows, tr("faction.none"), key ->
        {
            selected = Integer.parseInt(key);
            creating = false;
            view.rebuild();
        });
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        Optional<BattlesData.FactionInfo> faction = data.managed(selected);
        if (creating || faction.isEmpty())
        {
            initCreate(view, x, width, faction.isPresent());
            return;
        }
        tabs(view, Tab.values(), tab, shown -> tr("faction.tab." + shown.name().toLowerCase(Locale.ROOT)), x, view.getTop() + 18, width, chosen -> tab = chosen);
        switch (tab)
        {
            case GENERAL -> initGeneral(view, faction.get(), x, width);
            case RELATIONS -> initRelations(view, faction.get(), x, width);
            case BRANCHES -> initBranches(view, faction.get(), x, width);
        }
    }

    private void initCreate(ICommandCentreView view, int x, int width, boolean cancellable)
    {
        formTop = view.getTop() + 24 + view.getFont().wordWrapHeight(tr("faction.new_help"), width);
        int y = formTop;
        int colourWidth = 64;
        view.addField(NEW_NAME, x, y, width - colourWidth - GAP, "", tr("faction.name"));
        view.addField(NEW_COLOUR, x + width - colourWidth, y, colourWidth, "", tr("faction.colour")).setTooltip(Tooltip.create(tr("faction.colour.tip")));
        y += ROW;
        button(view, tr("faction.create"), x, y, width, () ->
        {
            String name = view.getField(NEW_NAME).strip();
            String colour = view.getField(NEW_COLOUR).strip();
            if (name.isEmpty())
            {
                view.showMessage(tr("faction.name_needed"), true);
                return;
            }
            if (!colour.isEmpty() && !COLOUR.matcher(colour).matches())
            {
                view.showMessage(tr("faction.bad_colour"), true);
                return;
            }
            awaited = name;
            if (colour.isEmpty())
                run(view, CREATE_COMMAND, quote(name));
            else
                run(view, CREATE_COMMAND, quote(name), colour.replace("#", ""));
        });
        y += ROW + 16;
        if (!data.teams().isEmpty())
        {
            List<String> teams = data.teams().stream().map(BattlesData.TeamInfo::id).toList();
            fromTeam = keep(teams, fromTeam, "");
            int half = (width - GAP) / 2;
            choice(view, tr("faction.team"), teams, fromTeam, id -> Component.literal(data.team(id).map(BattlesData.TeamInfo::name).orElse(id)), x, y, half, chosen -> fromTeam = chosen);
            button(view, tr("faction.create_from_team"), x + half + GAP, y, half, tr("faction.create_from_team.tip"), () ->
            {
                String name = view.getField(NEW_NAME).strip();
                if (name.isEmpty())
                    name = data.team(fromTeam).map(BattlesData.TeamInfo::name).orElse(fromTeam);
                awaited = name;
                run(view, CREATE_COMMAND, quote(name), "from", fromTeam);
            });
            y += ROW;
        }
        if (cancellable)
            button(view, tr("cancel"), x, y + 6, width, () ->
            {
                creating = false;
                awaited = "";
                view.rebuild();
            });
    }

    private void initGeneral(ICommandCentreView view, BattlesData.FactionInfo faction, int x, int width)
    {
        String name = quote(faction.name());
        int y = view.getTop() + 18 + ROW + 36;
        List<String> rules = data.choices().outsiders();
        if (!rules.isEmpty())
            choice(view, tr("faction.outsiders"), rules, keep(rules, faction.outsiders(), rules.get(0)), rule -> value("outsiders", rule), x, y, width,
                rule -> run(view, "teams faction outsiders", name, rule)).setTooltip(Tooltip.create(tr("faction.outsiders.tip")));
        y += ROW;
        int half = (width - GAP) / 2;
        if (faction.member())
            button(view, tr("faction.leave"), x, y, half, () -> run(view, "teams faction leave"));
        else
            button(view, tr("faction.join"), x, y, half, tr("faction.join.tip"), () -> run(view, "teams faction join", name));
        button(view, tr("faction.remove"), x + half + GAP, y, half, () -> view.confirm(tr("faction.remove.confirm", faction.name()), () -> run(view, "teams faction remove", name)));
    }

    private void initRelations(ICommandCentreView view, BattlesData.FactionInfo faction, int x, int width)
    {
        List<CommandCentreRow> rows = new ArrayList<>();
        for (BattlesData.FactionInfo other : data.allFactions())
        {
            if (other.id() == faction.id())
                continue;
            boolean war = faction.enemies().contains(other.id());
            Component detail = tr(data.managed(other.id()).isPresent() ? "faction.managed" : "faction.unmanaged");
            rows.add(CommandCentreRow.of(Integer.toString(other.id()), Component.literal(other.name()), detail).withColour(other.colour()).withBadge(tr(war ? "badge.war" : "badge.peace"),
                war ? CommandCentreRow.BAD : CommandCentreRow.NEUTRAL));
        }
        int top = view.getTop() + 18 + ROW;
        view.addList("relations@" + faction.id(), new ScreenRectangle(x, top, width, view.getTop() + view.getHeight() - 26 - top), rows, tr("faction.no_others"), key ->
        {
            int other = Integer.parseInt(key);
            boolean war = faction.enemies().contains(other);
            run(view, "teams faction", war ? "peace" : "hostile", quote(faction.name()), quote(data.factionName(other)));
        });
    }

    private void initBranches(ICommandCentreView view, BattlesData.FactionInfo faction, int x, int width)
    {
        String name = quote(faction.name());
        int y = view.getTop() + 18 + ROW;
        List<String> free = data.teams().stream().map(BattlesData.TeamInfo::id).filter(team -> faction.branches().stream().noneMatch(served -> served.team().equals(team))).toList();
        if (!free.isEmpty())
        {
            newBranch = keep(free, newBranch, "");
            choice(view, tr("faction.branch_team"), free, newBranch, this::teamName, x, y, width - 64, chosen -> newBranch = chosen);
            button(view, tr("faction.add_branch"), x + width - 60, y, 60, () -> run(view, BRANCH_COMMAND, name, "add", newBranch));
        }
        y += ROW;
        List<String> served = faction.branches().stream().map(BattlesData.Branch::team).toList();
        branch = keep(served, branch, "");
        List<CommandCentreRow> rows = new ArrayList<>();
        for (BattlesData.Branch entry : faction.branches())
        {
            Component detail = tr("faction.branch_detail", entry.weight(), entry.count(), entry.roles().isEmpty() ? tr("faction.roles_auto") : Component.literal(entry.roles()));
            rows.add(CommandCentreRow.of(entry.team(), teamName(entry.team()), detail).withColour(data.team(entry.team()).map(BattlesData.TeamInfo::colour).orElse(0))
                .selected(entry.team().equals(branch)));
        }
        int listHeight = Math.max(22, view.getTop() + view.getHeight() - y - 2 * ROW - GAP);
        view.addList("branches@" + faction.id(), new ScreenRectangle(x, y, width, listHeight), rows, tr("faction.no_branches"), key ->
        {
            branch = key;
            view.rebuild();
        });
        Optional<BattlesData.Branch> chosen = faction.branches().stream().filter(entry -> entry.team().equals(branch)).findFirst();
        if (chosen.isEmpty())
            return;
        BattlesData.Branch entry = chosen.get();
        y += listHeight + GAP;
        int half = (width - GAP) / 2;
        int fieldWidth = half - 44;
        String key = faction.id() + "/" + entry.team();
        view.addField("weight@" + key, x, y, fieldWidth, Integer.toString(entry.weight()), tr("faction.weight")).setTooltip(Tooltip.create(tr("faction.weight.tip")));
        button(view, tr("set"), x + fieldWidth + GAP, y, 40,
            () -> number(view, "weight@" + key, 1, Integer.MAX_VALUE, tr("faction.weight")).ifPresent(weight -> run(view, BRANCH_COMMAND, name, entry.team(), "weight", weight)));
        view.addField("count@" + key, x + half + GAP, y, fieldWidth, Integer.toString(entry.count()), tr("faction.count")).setTooltip(Tooltip.create(tr("faction.count.tip")));
        button(view, tr("set"), x + half + GAP + fieldWidth + GAP, y, 40,
            () -> number(view, "count@" + key, 0, Integer.MAX_VALUE, tr("faction.count")).ifPresent(count -> run(view, BRANCH_COMMAND, name, entry.team(), "count", count)));
        y += ROW;
        int rolesWidth = width - 44 - 64;
        view.addField("roles@" + key, x, y, rolesWidth, entry.roles(), tr("faction.roles_hint")).setTooltip(Tooltip.create(tr("faction.roles.tip", String.join(" ", data.choices().roles()))));
        button(view, tr("set"), x + rolesWidth + GAP, y, 40, () ->
        {
            String roles = view.getField("roles@" + key).strip();
            run(view, BRANCH_COMMAND, name, entry.team(), "roles", roles.isEmpty() ? "auto" : roles);
        });
        button(view, tr("faction.remove_branch"), x + width - 60, y, 60, () -> run(view, BRANCH_COMMAND, name, "remove", entry.team()));
    }

    private Component teamName(String team)
    {
        return Component.literal(data.team(team).map(BattlesData.TeamInfo::name).orElse(team));
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int panelLeft = view.getLeft() + listWidth + 6;
        int panelWidth = view.getWidth() - listWidth - 6;
        int x = panelLeft + 6;
        int width = panelWidth - 12;
        Optional<BattlesData.FactionInfo> shown = data.managed(selected);
        if (creating || shown.isEmpty())
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("faction.new_title"));
            paragraph(view, graphics, tr("faction.new_help"), x, view.getTop() + 18, width, DIM);
            if (!data.teams().isEmpty())
                label(view, graphics, tr("faction.or_from_team"), x, formTop + 2 * ROW + 4, width, MUTED);
            return;
        }
        BattlesData.FactionInfo faction = shown.get();
        view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), Component.literal(faction.name()));
        int y = view.getTop() + 18 + ROW + 2;
        switch (tab)
        {
            case GENERAL -> {
                graphics.fill(x, y, x + 9, y + 9, 0xFF000000 | faction.colour());
                label(view, graphics, tr("faction.colour_line", String.format(Locale.ROOT, "%06X", faction.colour())), x + 13, y + 1, width - 13, DIM);
                label(view, graphics, tr(faction.member() ? "faction.members_you" : "faction.members", faction.members()), x, y + 12, width, DIM);
                String branches = String.join(", ", faction.branches().stream().map(BattlesData.Branch::team).toList());
                label(view, graphics, tr("faction.branches_line", branches.isEmpty() ? "-" : branches), x, y + 23, width, faction.branches().isEmpty() ? ACCENT : DIM);
            }
            case RELATIONS -> label(view, graphics, tr("faction.relations_help"), x, view.getTop() + view.getHeight() - 20, width, MUTED);
            case BRANCHES -> {
                // The list and fields name themselves.
            }
        }
    }
}
