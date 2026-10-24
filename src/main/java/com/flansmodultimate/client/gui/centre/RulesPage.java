package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.common.teams.centre.TeamsSection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * The Teams rules: what may be broken and who may do what, weapons and drops, how long abandoned driveables last, the
 * game type being played and its score limit, and the arena and survival presets that set many rules at once.
 */
final class RulesPage extends TeamsPage
{
    private static final List<String> PROTECTION = List.of("explosions", "canBreakGlass", "canBreakGuns", "vehiclesBreakBlocks", "survivalCanBreakVehicles", "survivalCanPlaceVehicles",
        "forceAdventure", "pacifism");
    private static final List<String> COMBAT = List.of("bombs", "shells", "bullets", "armourDrops", "overrideHunger", "alliedSpawns", "fuelNeeded", "vehiclesCanZoom", "movieProp");
    private static final List<String> WEAPON_DROPS = List.of("on", "smart", "off");
    /** The game type variable every game type takes, for the score limit of the round being played. */
    private static final String SCORE_LIMIT = "scorelimit";
    private static final String SCORE_LIMIT_TEXT = "score_limit";
    private static final String RULE = "rule.";
    private static final String VARIABLE = "variable.";
    private static final String SET_VARIABLE = "teams admin setvariable";
    private static final String SEA_LEVEL = "sea_level";
    private static final String SEA_LEVEL_FIELD = "sea_level_field";

    private enum Tab
    {
        PROTECTION, COMBAT, CLEANUP, GAME_TYPE
    }

    private Tab tab = Tab.PROTECTION;

    RulesPage()
    {
        super("rules", Items.WRITABLE_BOOK, 400);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        TeamsData data = data();
        int left = view.getLeft();
        int width = view.getWidth();
        int y = view.getTop() + tabs(view, Tab.values(), tab, shown -> tr("rules_tab." + shown.name().toLowerCase(Locale.ROOT)), left, view.getTop(), width, chosen -> tab = chosen);
        int bottom = view.getTop() + view.getHeight() - WIDGET_HEIGHT;
        switch (tab)
        {
            case PROTECTION -> switches(view, data.rules(), PROTECTION, left, y + 16, width);
            case COMBAT -> {
                switches(view, data.rules(), COMBAT, left, y + 16, width);
                // Weapon drops take the slot after the last switch.
                int columnWidth = (width - GAP) / 2;
                int slot = COMBAT.size();
                choice(view, tr(RULE + "weaponDrops"), WEAPON_DROPS, data.rules().getString(TeamsSection.WEAPON_DROPS), mode -> tr("weapon_drops." + mode), left + (slot % 2) * (columnWidth + GAP),
                    y + 16 + (slot / 2) * ROW, columnWidth, mode -> run(view, "teams weaponDrops", mode)).setTooltip(Tooltip.create(tr(RULE + "weaponDrops.tip")));
            }
            case CLEANUP -> initCleanup(view, data.rules(), left, y + 16, width);
            case GAME_TYPE -> initGameType(view, data, left, y + 16, width);
        }
        if (tab != Tab.GAME_TYPE)
        {
            int half = (width - GAP) / 2;
            button(view, tr("preset.arena"), left, bottom, half, tr("preset.arena.tip"), () -> view.confirm(tr("preset.arena.confirm"), () -> run(view, "teams admin arena")));
            button(view, tr("preset.survival"), left + half + GAP, bottom, half, tr("preset.survival.tip"), () -> view.confirm(tr("preset.survival.confirm"), () -> run(view, "teams admin survival")));
        }
    }

    /** Lays out on/off switches for rules in two columns. */
    private static void switches(ICommandCentreView view, CompoundTag rules, List<String> names, int left, int top, int width)
    {
        int columnWidth = (width - GAP) / 2;
        for (int i = 0; i < names.size(); i++)
        {
            String rule = names.get(i);
            int x = left + (i % 2) * (columnWidth + GAP);
            int y = top + (i / 2) * ROW;
            toggle(view, tr(RULE + rule), rules.getBoolean(rule), x, y, columnWidth, tr(RULE + rule + ".tip"), enabled -> run(view, "teams", rule, enabled));
        }
    }

    private static void initCleanup(ICommandCentreView view, CompoundTag rules, int left, int top, int width)
    {
        int columnWidth = (width - GAP) / 2;
        int fieldWidth = 48;
        int buttonWidth = 40;
        for (int i = 0; i < TeamsSection.LIFETIMES.size(); i++)
        {
            String rule = TeamsSection.LIFETIMES.get(i);
            int x = left + (i % 2) * (columnWidth + GAP) + columnWidth - fieldWidth - buttonWidth - GAP;
            int y = top + (i / 2) * ROW;
            view.addField(rule, x, y, fieldWidth, Integer.toString(rules.getInt(rule)), tr("seconds"));
            button(view, tr("set"), x + fieldWidth + GAP, y, buttonWidth, () -> number(view, rule, 0, Integer.MAX_VALUE, tr(RULE + rule)).ifPresent(seconds -> run(view, "teams", rule, seconds)));
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
                for (int i = 0; i < TeamsSection.LIFETIMES.size(); i++)
                {
                    String rule = TeamsSection.LIFETIMES.get(i);
                    label(view, graphics, tr(RULE + rule), left + (i % 2) * (columnWidth + GAP) + 2, rowsTop + (i / 2) * ROW + 5, columnWidth - 104, CentreStyle.TEXT);
                }
                int y = rowsTop + (TeamsSection.LIFETIMES.size() + 1) / 2 * ROW + 10;
                label(view, graphics, tr(SEA_LEVEL), left + 2, y + 5, columnWidth - 104, CentreStyle.TEXT);
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
