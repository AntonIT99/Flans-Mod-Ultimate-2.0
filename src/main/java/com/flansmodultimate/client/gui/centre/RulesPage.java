package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.centre.TeamsSection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * The Teams rules: what may be broken and who may do what, weapons and drops, how long abandoned driveables last, the
 * game type being played and its score limit, and the arena and survival presets that set many rules at once.
 */
final class RulesPage extends TeamsPage
{
    private static final List<String> PROTECTION = List.of("explosions", "canBreakGlass", "canBreakGuns", "canBreakAAGuns", "vehiclesBreakBlocks", "survivalCanBreakVehicles",
        "survivalCanPlaceVehicles", "forceAdventure", "pacifism", "parachutesCanBeHit");
    private static final List<String> COMBAT = List.of("bombs", "shells", "bullets", "armourDrops", "overrideHunger", "alliedSpawns", "fuelNeeded", "vehiclesCanZoom", "movieProp", "bloodEffects");
    private static final List<String> WEAPON_DROPS = List.of("on", "smart", "off");
    /** The game type variable every game type takes, for the score limit of the round being played. */
    private static final String SCORE_LIMIT = "scorelimit";
    private static final String SCORE_LIMIT_TEXT = "score_limit";
    private static final String RULE = "rule.";
    private static final String TEAMS = "teams";
    private static final String VARIABLE = "variable.";
    private static final String GAME_RULE = "gamerule.";
    private static final String SET_VARIABLE = "teams admin setvariable";
    private static final String SEA_LEVEL = "sea_level";
    private static final String SEA_LEVEL_FIELD = "sea_level_field";

    private enum Tab
    {
        PROTECTION, COMBAT, HEALTH, CLEANUP, GAME_TYPE, MINECRAFT
    }

    /** The first row shown of each tab that scrolls. */
    private final Map<Tab, Integer> scrolls = new EnumMap<>(Tab.class);
    private Tab tab = Tab.PROTECTION;
    private String selectedGameRule = "";

    RulesPage()
    {
        super("rules", Items.WRITABLE_BOOK, 400);
    }

    /**
     * The rows of a scrolling tab that fit above the presets button. Rows are laid out from {@code first}; the rest are not
     * built, so nothing overlaps however many rules a tab gets.
     */
    private record Viewport(int left, int top, int width, int height, int first, int visible, int total)
    {
        boolean scrolling()
        {
            return total > visible;
        }

        /** Width of the rows, leaving room for the scroll bar when there is one. */
        int contentWidth()
        {
            return scrolling() ? width - CentreScrollArea.BAR_SPACE : width;
        }

        int columnWidth()
        {
            return (contentWidth() - GAP) / 2;
        }

        boolean shows(int row)
        {
            return row >= first && row < first + visible;
        }

        int y(int row)
        {
            return top + (row - first) * ROW;
        }
    }

    private static int totalRows(Tab tab)
    {
        return switch (tab)
        {
            // The spawn protection and the weapon drops take the slot after the last switch.
            case PROTECTION -> (PROTECTION.size() + 1) / 2 + 1;
            case COMBAT -> COMBAT.size() / 2 + 1;
            default -> 0;
        };
    }

    private Viewport viewport(ICommandCentreView view)
    {
        int top = view.getTop() + WIDGET_HEIGHT + GAP + 16;
        int height = Math.max(WIDGET_HEIGHT, presetsTop(view) - GAP - top);
        int visible = Math.max(1, (height - WIDGET_HEIGHT) / ROW + 1);
        int total = totalRows(tab);
        int first = Math.max(0, Math.min(scrolls.getOrDefault(tab, 0), total - visible));
        return new Viewport(view.getLeft(), top, view.getWidth(), height, first, visible, total);
    }

    /** Where the presets button sits on every tab that has one: the bottom of the content area. */
    private static int presetsTop(ICommandCentreView view)
    {
        return view.getTop() + view.getHeight() - WIDGET_HEIGHT;
    }

    @Override
    public void init(ICommandCentreView view)
    {
        TeamsData data = data();
        int left = view.getLeft();
        int width = view.getWidth();
        int y = view.getTop() + tabs(view, Tab.values(), tab, shown -> tr("rules_tab." + shown.name().toLowerCase(Locale.ROOT)), left, view.getTop(), width, chosen -> tab = chosen);
        switch (tab)
        {
            case PROTECTION -> {
                Viewport port = scrollArea(view);
                switches(view, data.rules(), PROTECTION, port);
                int row = (PROTECTION.size() + 1) / 2;
                if (port.shows(row))
                    initSpawnProtection(view, data.rules(), left, port.y(row), port.contentWidth());
            }
            case COMBAT -> {
                Viewport port = scrollArea(view);
                switches(view, data.rules(), COMBAT, port);
                int slot = COMBAT.size();
                if (port.shows(slot / 2))
                {
                    choice(view, tr(RULE + "weaponDrops"), WEAPON_DROPS, data.rules().getString(TeamsSection.WEAPON_DROPS), mode -> tr("weapon_drops." + mode),
                        left + (slot % 2) * (port.columnWidth() + GAP), port.y(slot / 2), port.columnWidth(), mode -> run(view, "teams weaponDrops", mode))
                        .setTooltip(Tooltip.create(tr(RULE + "weaponDrops.tip")));
                }
            }
            case HEALTH -> initHealth(view, data.rules(), left, y + 16, width);
            case CLEANUP -> initCleanup(view, data.rules(), left, y + 16, width);
            case GAME_TYPE -> initGameType(view, data, left, y + 16, width);
            case MINECRAFT -> initMinecraftRules(view, data.rules().getCompound(TeamsSection.GAME_RULES), left, y + 16, width);
        }
        if (tab != Tab.GAME_TYPE && tab != Tab.MINECRAFT)
        {
            button(view, Component.translatable("gui.flansmodultimate.presets.manage"), left, presetsTop(view), width,
                () -> com.flansmodultimate.api.client.commandcentre.FlansCommandCentreClient.openPresets(com.flansmodultimate.common.teams.centre.SettingsPresets.TEAMS, ""));
        }
    }

    /** Adds the scrolling of the tab, before its rows so that it takes the mouse wheel first, and gives the rows that fit. */
    private Viewport scrollArea(ICommandCentreView view)
    {
        Viewport port = viewport(view);
        scrolls.put(tab, port.first());
        Tab scrolled = tab;
        view.addWidget(new CentreScrollArea(port.left(), port.top(), port.width(), port.height(), port.total(), port.visible(), port.first(), row ->
        {
            scrolls.put(scrolled, row);
            view.rebuild();
        }));
        return port;
    }

    private void initMinecraftRules(ICommandCentreView view, CompoundTag rules, int left, int top, int width)
    {
        List<String> names = rules.getAllKeys().stream().sorted().toList();
        if (!names.contains(selectedGameRule))
            selectedGameRule = names.isEmpty() ? "" : names.get(0);
        int listWidth = (width - GAP) / 2;
        List<CommandCentreRow> rows = new ArrayList<>();
        for (String name : names)
        {
            String value = rules.contains(name, Tag.TAG_BYTE) ? Boolean.toString(rules.getBoolean(name)) : Integer.toString(rules.getInt(name));
            rows.add(CommandCentreRow.of(name, Component.translatable(GAME_RULE + name), Component.literal(name + " = " + value)).selected(name.equals(selectedGameRule)));
        }
        view.addList("minecraft_rules", new ScreenRectangle(left, top, listWidth, view.getTop() + view.getHeight() - top), rows, Component.empty(), name ->
        {
            selectedGameRule = name;
            view.rebuild();
        });
        if (selectedGameRule.isEmpty())
            return;
        String name = selectedGameRule;
        int x = left + listWidth + GAP;
        int editorWidth = width - listWidth - GAP;
        Component title = Component.translatable(GAME_RULE + name);
        Component description = Component.translatableWithFallback(GAME_RULE + name + ".description", name);
        if (rules.contains(name, Tag.TAG_BYTE))
            toggle(view, title, rules.getBoolean(name), x, top, editorWidth, description, enabled -> run(view, "gamerule", name, enabled));
        else
        {
            view.addField(GAME_RULE + name, x, top, editorWidth - 44, Integer.toString(rules.getInt(name)), title).setTooltip(Tooltip.create(title.copy().append("\n").append(description)));
            button(view, tr("set"), x + editorWidth - 40, top, 40,
                () -> number(view, GAME_RULE + name, Integer.MIN_VALUE, Integer.MAX_VALUE, title).ifPresent(value -> run(view, "gamerule", name, value)));
        }
    }

    /** Lays out on/off switches for rules in two columns. */
    private static void switches(ICommandCentreView view, CompoundTag rules, List<String> names, Viewport port)
    {
        int columnWidth = port.columnWidth();
        for (int i = 0; i < names.size(); i++)
        {
            if (!port.shows(i / 2))
                continue;
            String rule = names.get(i);
            int x = port.left() + (i % 2) * (columnWidth + GAP);
            toggle(view, tr(RULE + rule), rules.getBoolean(rule), x, port.y(i / 2), columnWidth, tr(RULE + rule + ".tip"), enabled -> run(view, TEAMS, rule, enabled));
        }
    }

    private static void initSpawnProtection(ICommandCentreView view, CompoundTag rules, int left, int top, int width)
    {
        int columnWidth = (width - GAP) / 2;
        int fieldWidth = 48;
        int buttonWidth = 40;
        int x = left + columnWidth - fieldWidth - buttonWidth - GAP;
        view.addField(TeamsSection.SPAWN_PROTECTION, x, top, fieldWidth, Integer.toString(rules.getInt(TeamsSection.SPAWN_PROTECTION)), tr("seconds"))
            .setTooltip(Tooltip.create(tr(RULE + TeamsSection.SPAWN_PROTECTION + ".tip")));
        button(view, tr("set"), x + fieldWidth + GAP, top, buttonWidth,
            () -> number(view, TeamsSection.SPAWN_PROTECTION, 0, TeamsManager.MAX_SPAWN_PROTECTION_SECONDS, tr(RULE + TeamsSection.SPAWN_PROTECTION))
                .ifPresent(seconds -> run(view, TEAMS, TeamsSection.SPAWN_PROTECTION, seconds)));
    }

    private static void initHealth(ICommandCentreView view, CompoundTag rules, int left, int top, int width)
    {
        toggle(view, tr(RULE + "bodyPartDamageModifiers"), rules.getBoolean("bodyPartDamageModifiers"), left, top, width, tr(RULE + "bodyPartDamageModifiers.tip"),
            enabled -> run(view, "teams bodyPartDamageModifiers", enabled));
        int row = 1;
        for (String rule : List.of("playerHealth", "botHealth", "playerRegenSeconds", "botRegenSeconds"))
        {
            int x = left + width - 100;
            int y = top + row++ * ROW;
            view.addField(rule, x, y, 56, Integer.toString(rules.getInt(rule)), tr(RULE + rule)).setTooltip(Tooltip.create(tr(RULE + rule + ".tip")));
            button(view, tr("set"), x + 60, y, 40, () -> number(view, rule, 0, rule.endsWith("Health") ? TeamsManager.MAX_HEALTH_OVERRIDE : Integer.MAX_VALUE, tr(RULE + rule)).ifPresent(health -> run(view, TEAMS, rule, health)));
        }
    }

    private static void initCleanup(ICommandCentreView view, CompoundTag rules, int left, int top, int width)
    {
        toggle(view, tr(RULE + "restoreBlocks"), rules.getBoolean("restoreBlocks"), left, top, width, tr(RULE + "restoreBlocks.tip"), enabled -> run(view, "teams restoreBlocks", enabled));
        top += ROW;
        int columnWidth = (width - GAP) / 2;
        int fieldWidth = 48;
        int buttonWidth = 40;
        for (int i = 0; i < TeamsSection.LIFETIMES.size(); i++)
        {
            String rule = TeamsSection.LIFETIMES.get(i);
            int x = left + (i % 2) * (columnWidth + GAP) + columnWidth - fieldWidth - buttonWidth - GAP;
            int y = top + (i / 2) * ROW;
            view.addField(rule, x, y, fieldWidth, Integer.toString(rules.getInt(rule)), tr("seconds"));
            button(view, tr("set"), x + fieldWidth + GAP, y, buttonWidth, () -> number(view, rule, 0, Integer.MAX_VALUE, tr(RULE + rule)).ifPresent(seconds -> run(view, TEAMS, rule, seconds)));
        }
        int y = top + (TeamsSection.LIFETIMES.size() + 1) / 2 * ROW + 10;
        int x = left + columnWidth - fieldWidth - buttonWidth - GAP;
        String seaLevel = rules.contains(TeamsSection.SEA_LEVEL) ? Integer.toString(rules.getInt(TeamsSection.SEA_LEVEL)) : "";
        view.addField(SEA_LEVEL_FIELD, x, y, fieldWidth, seaLevel, tr("sea_level.hint"));
        button(view, tr("set"), x + fieldWidth + GAP, y, buttonWidth, tr("sea_level.tip"),
            () -> number(view, SEA_LEVEL_FIELD, -2048, 2048, tr(SEA_LEVEL)).ifPresent(level -> run(view, "teams seaLevel", level)));
        button(view, tr("sea_level.default"), left + columnWidth + GAP, y, Math.min(columnWidth, 110), () -> run(view, "teams seaLevel default")).active = rules.contains(TeamsSection.SEA_LEVEL);
    }

    private static void initGameType(ICommandCentreView view, TeamsData data, int left, int top, int width)
    {
        Optional<TeamsData.LiveRound> live = data.live();
        if (live.isEmpty())
            return;
        int columnWidth = (width - GAP) / 2;
        int fieldWidth = 56;
        int buttonWidth = 40;
        int x = left + columnWidth - fieldWidth - buttonWidth - GAP;
        view.addField(SCORE_LIMIT, x, top, fieldWidth, Integer.toString(live.get().round().scoreLimit()), tr(SCORE_LIMIT_TEXT));
        button(view, tr("set"), x + fieldWidth + GAP, top, buttonWidth, tr("score_limit.tip"),
            () -> number(view, SCORE_LIMIT, 1, Integer.MAX_VALUE, tr(SCORE_LIMIT_TEXT)).ifPresent(score -> run(view, SET_VARIABLE, SCORE_LIMIT, score)));
        int index = 0;
        for (Map.Entry<String, String> variable : data.variables().entrySet())
        {
            String name = variable.getKey();
            int slot = index + 2;
            int columnLeft = left + (slot % 2) * (columnWidth + GAP);
            int y = top + (slot / 2) * ROW;
            if (y + WIDGET_HEIGHT > view.getTop() + view.getHeight())
                break;
            if ("true".equals(variable.getValue()) || "false".equals(variable.getValue()))
                toggle(view, variableName(name), Boolean.parseBoolean(variable.getValue()), columnLeft, y, columnWidth, tr("variable_tip", name), enabled -> run(view, SET_VARIABLE, name, enabled));
            else
            {
                int fieldLeft = columnLeft + columnWidth - fieldWidth - buttonWidth - GAP;
                view.addField(VARIABLE + name, fieldLeft, y, fieldWidth, variable.getValue(), Component.literal(name))
                    .setTooltip(Tooltip.create(variableName(name).copy().append("\n").append(tr("variable_tip", name))));
                button(view, tr("set"), fieldLeft + fieldWidth + GAP, y, buttonWidth,
                    () -> number(view, VARIABLE + name, 0, Integer.MAX_VALUE, variableName(name)).ifPresent(value -> run(view, SET_VARIABLE, name, value)));
            }
            index++;
        }
    }

    /** A game type variable's name in words, or as it is spelt where it has no words yet. */
    private static Component variableName(String name)
    {
        String key = KEY + VARIABLE + name;
        return net.minecraft.locale.Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(name);
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        TeamsData data = data();
        int left = view.getLeft();
        int top = view.getTop() + WIDGET_HEIGHT + GAP;
        int width = view.getWidth();
        label(view, graphics, tr("rules_tab." + tab.name().toLowerCase(Locale.ROOT) + ".help"), left + 2, top + 2, width - 4, CentreStyle.MUTED);
        int rowsTop = top + 16;
        int columnWidth = (width - GAP) / 2;
        switch (tab)
        {
            case CLEANUP -> {
                rowsTop += ROW;
                for (int i = 0; i < TeamsSection.LIFETIMES.size(); i++)
                {
                    String rule = TeamsSection.LIFETIMES.get(i);
                    label(view, graphics, tr(RULE + rule), left + (i % 2) * (columnWidth + GAP) + 2, rowsTop + (i / 2) * ROW + 5, columnWidth - 104, CentreStyle.TEXT);
                }
                int y = rowsTop + (TeamsSection.LIFETIMES.size() + 1) / 2 * ROW + 10;
                label(view, graphics, tr(SEA_LEVEL), left + 2, y + 5, columnWidth - 104, CentreStyle.TEXT);
            }
            case HEALTH -> {
                int row = 1;
                for (String rule : List.of("playerHealth", "botHealth", "playerRegenSeconds", "botRegenSeconds"))
                    label(view, graphics, tr(RULE + rule), left + 2, rowsTop + row++ * ROW + 5, width - 104, CentreStyle.TEXT);
                paragraph(view, graphics, tr("health.help"), left + 2, rowsTop + 5 * ROW + GAP, width - 4, CentreStyle.MUTED);
            }
            case PROTECTION -> {
                Viewport port = viewport(view);
                int row = (PROTECTION.size() + 1) / 2;
                if (port.shows(row))
                    label(view, graphics, tr(RULE + TeamsSection.SPAWN_PROTECTION), left + 2, port.y(row) + 5, port.columnWidth() - 104, CentreStyle.TEXT);
            }
            case GAME_TYPE -> renderGameType(view, graphics, data, left, rowsTop, width, columnWidth);
            default -> {
                // The switches name themselves.
            }
        }
    }

    private static void renderGameType(ICommandCentreView view, GuiGraphics graphics, TeamsData data, int left, int top, int width, int columnWidth)
    {
        Optional<TeamsData.LiveRound> live = data.live();
        if (live.isEmpty())
        {
            paragraph(view, graphics, tr("game_type_none"), left + 2, top, width - 4, CentreStyle.DIM);
            return;
        }
        label(view, graphics, tr(SCORE_LIMIT_TEXT), left + 2, top + 5, columnWidth - 104, CentreStyle.TEXT);
        label(view, graphics, tr("game_type_playing", data.gameTypeName(live.get().round().gameType())), left + columnWidth + GAP + 2, top + 5, columnWidth - 4, CentreStyle.ACCENT);
        int index = 0;
        for (Map.Entry<String, String> variable : data.variables().entrySet())
        {
            int slot = index + 2;
            int y = top + (slot / 2) * ROW;
            boolean flag = "true".equals(variable.getValue()) || "false".equals(variable.getValue());
            if (!flag)
                label(view, graphics, variableName(variable.getKey()), left + (slot % 2) * (columnWidth + GAP) + 2, y + 5, columnWidth - 104, CentreStyle.TEXT);
            index++;
        }
    }
}
