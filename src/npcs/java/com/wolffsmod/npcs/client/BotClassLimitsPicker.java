package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.teams.BotSettings;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Edits independent caps for automatically assigned infantry, applying them to the editor copy only with Done. */
final class BotClassLimitsPicker extends BotClassPicker
{
    private static final String KEY = "class_limits.";
    private static final int FIELD_WIDTH = 52;
    private final BotSettings.TeamForce force;
    private final Map<String, BotSettings.ClassLimit> limits = new LinkedHashMap<>();

    BotClassLimitsPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, team, Component.translatable(PREFIX + KEY + "title", team), Component.translatable(PREFIX + KEY + "hint"));
        this.force = force;
        limits.putAll(force.getClassLimits());
        setChoices(classChoices(limits.keySet()));
    }

    @Override
    protected int controlsWidth()
    {
        return FIELD_WIDTH * 2 + GAP;
    }

    @Override
    protected int headingHeight()
    {
        return 12;
    }

    @Override
    protected void addHeadings(int x, int y)
    {
        addRenderableWidget(new StringWidget(x, y, FIELD_WIDTH, 10, Component.translatable(PREFIX + KEY + "count"), font));
        addRenderableWidget(new StringWidget(x + FIELD_WIDTH + GAP, y, FIELD_WIDTH, 10, Component.translatable(PREFIX + KEY + "percent"), font));
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        BotSettings.ClassLimit limit = limits.getOrDefault(choice.id(), BotSettings.ClassLimit.UNLIMITED);
        addLimitField(choice.id(), x, y, Integer.toString(limit.count()), false);
        addLimitField(choice.id(), x + FIELD_WIDTH + GAP, y, Float.toString(limit.percent()), true);
    }

    private void addLimitField(String playerClass, int x, int y, String value, boolean percent)
    {
        Component help = Component.translatable(PREFIX + KEY + (percent ? "percent.tip" : "count.tip"));
        EditBox box = new EditBox(font, x, y + 1, FIELD_WIDTH, BUTTON_HEIGHT - 2, help);
        box.setMaxLength(10);
        box.setFilter(text -> validNumber(text, percent));
        box.setValue(value);
        box.setResponder(text ->
        {
            if (text.isEmpty() || "-".equals(text) || ".".equals(text) || "-.".equals(text))
                return;
            BotSettings.ClassLimit previous = limits.getOrDefault(playerClass, BotSettings.ClassLimit.UNLIMITED);
            BotSettings.ClassLimit chosen = percent ? new BotSettings.ClassLimit(previous.count(), Float.parseFloat(text)) : new BotSettings.ClassLimit(Integer.parseInt(text), previous.percent());
            if (chosen.equals(BotSettings.ClassLimit.UNLIMITED))
                limits.remove(playerClass);
            else
                limits.put(playerClass, chosen);
            refreshNames();
        });
        box.setTooltip(Tooltip.create(help));
        addRowWidget(box);
    }

    private static boolean validNumber(String text, boolean percent)
    {
        if (!text.matches(percent ? "-?\\d*(\\.\\d*)?" : "-?\\d*"))
            return false;
        if (text.isEmpty() || "-".equals(text) || ".".equals(text) || "-.".equals(text))
            return true;
        try
        {
            double number = Double.parseDouble(text);
            return number == -1D || number >= 0D && number <= (percent ? 100D : 1000000D);
        }
        catch (NumberFormatException ignored)
        {
            return false;
        }
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return limits.containsKey(choice.id());
    }

    @Override
    protected void addFooter(int x, int y, int width)
    {
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + KEY + "reset"), button ->
        {
            limits.clear();
            refreshPage();
        }).bounds(x, y, Math.min(width, 120), BUTTON_HEIGHT).build());
    }

    @Override
    protected void apply()
    {
        force.getClassLimits().clear();
        force.getClassLimits().putAll(limits);
    }
}
