package com.wolffsmod.npcs.client.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Wars between managed factions: making one, the factions fighting it, the dimensions it is fought in, its rules,
 * starting and stopping it, and a map of the territory around the operator, where chunks are claimed for a faction or
 * freed again.
 */
final class WarsPage extends BattlePage
{
    private static final String NEW_NAME = "new_name";
    private static final int CLAIM_RADIUS = 2;
    private static final String RADIUS = "war.radius";
    private static final String SETTING = "war.setting.";
    private static final String SCOPE = "teams war scope";
    /** The settings given as numbers, with their bounds, by the {@code /teams war set} word that sets each. */
    private static final List<Setting> SETTINGS = List.of(new Setting("infantry", 0, 200), new Setting("respawn", 0, 3600), new Setting("tickets", 0, 100000), new Setting("range", 16, 512),
        new Setting("engagements", 1, 16), new Setting("flip", 5, 3600));

    private record Setting(String name, int least, int most)
    {}

    private enum Tab
    {
        FACTIONS, TERRITORY, RULES
    }

    private String selected = "";
    private boolean creating;
    private String awaited = "";
    private Tab tab = Tab.FACTIONS;
    private int addFaction = -1;
    private int faction = -1;
    private int claimFaction = -1;
    private String dimension = "";
    private int listWidth;
    /** Where the territory map is drawn, and the size of a chunk on it, as laid out. */
    private int mapLeft;
    private int mapTop;
    private int cell;

    WarsPage()
    {
        super("wars", Items.CARTOGRAPHY_TABLE, 800);
    }

    @Override
    void layout(ICommandCentreView view)
    {
        if (!awaited.isEmpty() && data.war(awaited).isPresent())
        {
            selected = awaited;
            awaited = "";
            creating = false;
        }
        if (data.war(selected).isEmpty())
            selected = data.wars().isEmpty() ? "" : data.wars().get(0).name();
        listWidth = Math.max(120, view.getWidth() * 34 / 100);
        button(view, tr("war.new"), view.getLeft(), view.getTop(), listWidth, () ->
        {
            creating = true;
            view.rebuild();
        });
        List<CommandCentreRow> rows = data.wars().stream().map(this::row).toList();
        view.addList("wars", new ScreenRectangle(view.getLeft(), view.getTop() + WIDGET_HEIGHT + GAP, listWidth, view.getHeight() - WIDGET_HEIGHT - GAP), rows, tr("war.none"), key ->
        {
            selected = key;
            creating = false;
            view.rebuild();
        });
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        Optional<BattlesData.WarInfo> war = data.war(selected);
        if (creating || war.isEmpty())
        {
            initCreate(view, x, width, war.isPresent());
            return;
        }
        String name = quote(war.get().name());
        int half = (width - GAP) / 2;
        int y = view.getTop() + 18;
        if (war.get().running())
            button(view, tr("war.stop"), x, y, half, tr("war.stop.tip"), () -> run(view, "teams war stop", name));
        else
            button(view, tr("war.start"), x, y, half, tr("war.start.tip"), () -> run(view, "teams war start", name));
        button(view, tr("war.delete"), x + half + GAP, y, half, () -> view.confirm(tr("war.delete.confirm", war.get().name()), () -> run(view, "teams war delete", name)));
        y += ROW;
        tabs(view, Tab.values(), tab, shown -> tr("war.tab." + shown.name().toLowerCase(Locale.ROOT)), x, y, width, chosen -> tab = chosen);
        y += ROW;
        switch (tab)
        {
            case FACTIONS -> initFactions(view, war.get(), x, y, width);
            case TERRITORY -> initTerritory(view, war.get(), x, y, width);
            case RULES -> initRules(view, war.get(), x, y, width);
        }
    }

    /** A war in the list: its factions, and whether it is fought. */
    private CommandCentreRow row(BattlesData.WarInfo war)
    {
        String factions = String.join(" vs ", war.factions().stream().map(data::factionName).toList());
        return CommandCentreRow.of(war.name(), Component.literal(war.name()), Component.literal(factions.isEmpty() ? "-" : factions))
            .withBadge(tr(war.running() ? "badge.running" : "badge.stopped"), war.running() ? CommandCentreRow.GOOD : CommandCentreRow.NEUTRAL).selected(war.name().equals(selected));
    }

    private void initCreate(ICommandCentreView view, int x, int width, boolean cancellable)
    {
        int y = view.getTop() + 24 + view.getFont().wordWrapHeight(tr(data.factions().size() < 2 ? "war.need_factions" : "war.new_help"), width);
        view.addField(NEW_NAME, x, y, width - 74, "", tr("war.name"));
        button(view, tr("war.create"), x + width - 70, y, 70, () ->
        {
            String name = view.getField(NEW_NAME).strip();
            if (name.isEmpty())
            {
                view.showMessage(tr("war.name_needed"), true);
                return;
            }
            awaited = name;
            run(view, "teams war create", quote(name));
        });
        if (cancellable)
            button(view, tr("cancel"), x, y + ROW, width, () ->
            {
                creating = false;
                awaited = "";
                view.rebuild();
            });
    }

    private void initFactions(ICommandCentreView view, BattlesData.WarInfo war, int x, int top, int width)
    {
        String name = quote(war.name());
        int listHeight = Math.max(22, view.getTop() + view.getHeight() - top - ROW);
        faction = keep(war.factions(), faction, -1);
        List<CommandCentreRow> rows = new ArrayList<>();
        for (int i = 0; i < war.factions().size(); i++)
        {
            int id = war.factions().get(i);
            int held = i < war.holdings().size() ? war.holdings().get(i) : 0;
            rows.add(CommandCentreRow.of(Integer.toString(id), Component.literal(data.factionDisplayName(id)), tr("war.holds", held)).withColour(data.factionColour(id)).selected(id == faction));
        }
        view.addList("factions@" + war.name(), new ScreenRectangle(x, top, width, listHeight), rows, tr("war.no_factions"), key ->
        {
            faction = Integer.parseInt(key);
            view.rebuild();
        });
        int y = top + listHeight + GAP;
        List<Integer> free = data.factions().stream().map(BattlesData.FactionInfo::id).filter(id -> !war.factions().contains(id)).toList();
        int buttonWidth = 56;
        int choiceWidth = width - 2 * buttonWidth - 2 * GAP;
        if (!free.isEmpty())
        {
            addFaction = keep(free, addFaction, -1);
            choice(view, tr("war.faction"), free, addFaction, id -> Component.literal(data.factionDisplayName(id)), x, y, choiceWidth, chosen -> addFaction = chosen);
            button(view, tr("war.add_faction"), x + choiceWidth + GAP, y, buttonWidth, () -> run(view, "teams war faction", name, "add", quote(data.factionName(addFaction))));
        }
        else if (data.factions().isEmpty())
            button(view, tr("war.to_factions"), x, y, choiceWidth + GAP + buttonWidth, () -> view.showPage(FactionsPage.ID));
        button(view, tr("war.remove_faction"), x + width - buttonWidth, y, buttonWidth, () -> run(view, "teams war faction", name, "remove", quote(data.factionName(faction)))).active = faction >= 0;
    }

    private void initTerritory(ICommandCentreView view, BattlesData.WarInfo war, int x, int top, int width)
    {
        String name = quote(war.name());
        BattlesData.Territory map = war.map();
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT;
        int side = Math.min(bottom - GAP - top, width * 55 / 100);
        cell = Math.max(2, side / Math.max(1, map.size()));
        mapLeft = x;
        mapTop = top;
        int radiusWidth = 30;
        int buttonWidth = 50;
        int choiceWidth = width - radiusWidth - 2 * buttonWidth - 3 * GAP;
        if (!war.factions().isEmpty())
        {
            claimFaction = keep(war.factions(), claimFaction, -1);
            choice(view, tr("war.claim_for"), war.factions(), claimFaction, id -> Component.literal(data.factionDisplayName(id)), x, bottom, choiceWidth, chosen -> claimFaction = chosen);
        }
        String radiusKey = "radius@" + war.name();
        view.addField(radiusKey, x + choiceWidth + GAP, bottom, radiusWidth, Integer.toString(CLAIM_RADIUS), tr(RADIUS)).setTooltip(Tooltip.create(tr("war.radius.tip")));
        button(view, tr("war.claim"), x + choiceWidth + radiusWidth + 2 * GAP, bottom, buttonWidth, tr("war.claim.tip"),
            () -> number(view, radiusKey, 0, 32, tr(RADIUS)).ifPresent(radius -> run(view, "teams war claim", name, quote(data.factionName(claimFaction)), radius))).active = !war.factions().isEmpty();
        button(view, tr("war.unclaim"), x + width - buttonWidth, bottom, buttonWidth, tr("war.unclaim.tip"),
            () -> number(view, radiusKey, 0, 32, tr(RADIUS)).ifPresent(radius -> run(view, "teams war unclaim", name, radius)));
    }

    private void initRules(ICommandCentreView view, BattlesData.WarInfo war, int x, int top, int width)
    {
        String name = quote(war.name());
        int half = (width - GAP) / 2;
        int fieldWidth = 36;
        int setWidth = 30;
        BattlesData.WarRules rules = war.rules();
        int[] values = {rules.infantry(), rules.respawn(), rules.tickets(), rules.range(), rules.engagements(), rules.flip()};
        for (int i = 0; i < SETTINGS.size(); i++)
        {
            Setting setting = SETTINGS.get(i);
            int columnRight = x + (i % 2) * (half + GAP) + half;
            int y = top + (i / 2) * ROW;
            String key = setting.name() + "@" + war.name();
            view.addField(key, columnRight - setWidth - GAP - fieldWidth, y, fieldWidth, Integer.toString(values[i]), tr(SETTING + setting.name()))
                .setTooltip(Tooltip.create(tr(SETTING + setting.name() + ".tip")));
            button(view, tr("set"), columnRight - setWidth, y, setWidth,
                () -> number(view, key, setting.least(), setting.most(), tr(SETTING + setting.name())).ifPresent(value -> run(view, "teams war set", name, setting.name(), value)));
        }
        int y = top + 3 * ROW;
        List<String> difficulties = data.choices().difficulties();
        if (!difficulties.isEmpty())
            difficultyChoice(view, tr("war.difficulty"), difficulties, keep(difficulties, rules.difficulty(), difficulties.get(0)), x, y, half,
                difficulty -> run(view, "teams war set", name, "difficulty", difficulty));
        int scopeWidth = (half - GAP) / 2;
        button(view, tr("war.scope_all"), x + half + GAP, y, scopeWidth, tr("war.scope_all.tip"), () -> run(view, SCOPE, name, "all")).active = !war.scope().isEmpty();
        button(view, tr("war.scope_here"), x + half + GAP + scopeWidth + GAP, y, scopeWidth, tr("war.scope_here.tip"), () -> run(view, SCOPE, name, "here"));
        y += ROW;
        if (data.dimensions().isEmpty())
            return;
        dimension = keep(data.dimensions(), dimension.isEmpty() ? data.here() : dimension, "");
        int buttonWidth = 30;
        int choiceWidth = width - 2 * buttonWidth - 2 * GAP;
        choice(view, tr("war.dimension"), data.dimensions(), dimension, Component::literal, x, y, choiceWidth, chosen -> dimension = chosen);
        button(view, Component.literal("+"), x + choiceWidth + GAP, y, buttonWidth, tr("war.scope_add.tip"), () -> run(view, SCOPE, name, "add", dimension)).active = !war.scope().contains(dimension);
        button(view, Component.literal("-"), x + width - buttonWidth, y, buttonWidth, tr("war.scope_remove.tip"), () -> run(view, SCOPE, name, "remove", dimension)).active = war.scope()
            .contains(dimension);
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int panelLeft = view.getLeft() + listWidth + 6;
        int panelWidth = view.getWidth() - listWidth - 6;
        int x = panelLeft + 6;
        int width = panelWidth - 12;
        Optional<BattlesData.WarInfo> shown = data.war(selected);
        if (creating || shown.isEmpty())
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("war.new_title"));
            paragraph(view, graphics, tr(data.factions().size() < 2 ? "war.need_factions" : "war.new_help"), x, view.getTop() + 18, width, DIM);
            return;
        }
        BattlesData.WarInfo war = shown.get();
        view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), Component.literal(war.name()));
        if (tab == Tab.TERRITORY)
            renderTerritory(view, graphics, war, x, width, mouseX, mouseY);
        else if (tab == Tab.RULES)
        {
            int top = view.getTop() + 18 + 2 * ROW;
            int half = (width - GAP) / 2;
            for (int i = 0; i < SETTINGS.size(); i++)
                label(view, graphics, tr(SETTING + SETTINGS.get(i).name()), x + (i % 2) * (half + GAP), top + (i / 2) * ROW + 5, half - 72, TEXT);
            Component scope = war.scope().isEmpty() ? tr("war.scope_everywhere") : tr("war.scope_only", String.join(", ", war.scope()));
            label(view, graphics, scope, x, top + 5 * ROW + 2, width, MUTED);
        }
    }

    /** The chunks around the operator in the colours of the factions holding them, with the fronts fought over, and a legend. */
    private void renderTerritory(ICommandCentreView view, GuiGraphics graphics, BattlesData.WarInfo war, int x, int width, int mouseX, int mouseY)
    {
        BattlesData.Territory map = war.map();
        int side = cell * map.size();
        graphics.fill(mapLeft - 1, mapTop - 1, mapLeft + side + 1, mapTop + side + 1, 0xFF2B3646);
        boolean blink = Util.getMillis() / 500L % 2L == 0L;
        for (int index = 0; index < map.size() * map.size(); index++)
        {
            int left = mapLeft + index % map.size() * cell;
            int top = mapTop + index / map.size() * cell;
            int holder = holder(war, index);
            graphics.fill(left, top, left + cell, top + cell, holder < 0 ? 0xFF141A22 : 0xC0000000 | data.factionColour(holder));
            if (blink && map.contested().contains(index))
                graphics.renderOutline(left, top, cell, cell, BAD);
        }
        int middle = map.radius() * cell;
        graphics.renderOutline(mapLeft + middle - 1, mapTop + middle - 1, cell + 2, cell + 2, TEXT);
        if (mouseX >= mapLeft && mouseY >= mapTop && mouseX < mapLeft + side && mouseY < mapTop + side && Minecraft.getInstance().screen != null)
        {
            int column = (mouseX - mapLeft) / cell;
            int row = (mouseY - mapTop) / cell;
            Minecraft.getInstance().screen.setTooltipForNextRenderPass(view.getFont().split(chunkTooltip(war, row, column), 200));
        }
        renderLegend(view, graphics, war, mapLeft + side + 8, x + width - mapLeft - side - 8, mapTop + side);
    }

    /** The faction holding a chunk of the territory map, by its index on the map, or -1 for no one. */
    private static int holder(BattlesData.WarInfo war, int index)
    {
        List<Integer> owners = war.map().owners();
        int owner = index < owners.size() ? owners.get(index) : -1;
        return owner >= 0 && owner < war.factions().size() ? war.factions().get(owner) : -1;
    }

    /** Where a chunk of the territory map is, who holds it, and whether it is fought over. */
    private Component chunkTooltip(BattlesData.WarInfo war, int row, int column)
    {
        BattlesData.Territory map = war.map();
        int index = row * map.size() + column;
        int holder = holder(war, index);
        int blockX = (map.centreX() - map.radius() + column) * 16 + 8;
        int blockZ = (map.centreZ() - map.radius() + row) * 16 + 8;
        Component name = holder < 0 ? tr("war.unheld") : Component.literal(data.factionDisplayName(holder));
        return tr(map.contested().contains(index) ? "war.chunk_contested" : "war.chunk", blockX, blockZ, name);
    }

    /** The factions of the war with the chunks each holds, and its engagements, beside the map down to its bottom. */
    private void renderLegend(ICommandCentreView view, GuiGraphics graphics, BattlesData.WarInfo war, int left, int width, int bottom)
    {
        int y = mapTop;
        if (!war.map().covered())
            y += paragraph(view, graphics, tr("war.not_here"), left, y, width, ACCENT) + 4;
        for (int i = 0; i < war.factions().size(); i++)
        {
            int id = war.factions().get(i);
            graphics.fill(left, y, left + 8, y + 8, 0xFF000000 | data.factionColour(id));
            int held = i < war.holdings().size() ? war.holdings().get(i) : 0;
            label(view, graphics, tr("war.legend", data.factionName(id), held), left + 12, y, width - 12, TEXT);
            y += 11;
        }
        y += 4;
        label(view, graphics, tr("war.engagements", war.engagements().size()), left, y, width, war.engagements().isEmpty() ? MUTED : ACCENT);
        for (BattlesData.Engagement engagement : war.engagements())
        {
            y += 10;
            if (y > bottom - 9)
                break;
            Component line = tr("war.engagement", data.factionName(engagement.first()), data.factionName(engagement.second()), engagement.x(), engagement.z(), engagement.progress(),
                war.rules().flip());
            label(view, graphics, line, left, y, width, DIM);
        }
    }
}
