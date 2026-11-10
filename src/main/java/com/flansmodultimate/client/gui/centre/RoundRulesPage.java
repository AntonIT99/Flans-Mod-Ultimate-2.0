package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.common.teams.RoundRule;
import com.flansmodultimate.common.teams.RoundRules;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.*;

/** Searchable, scrollable configuration of every opt-in round rule, with explicit inheritance and value bounds. */
final class RoundRulesPage extends TeamsPage
{
    static final String ID = "flansmodultimate:round_rules";
    private static final String DEFAULTS = "defaults";
    private static final String VALUE = "round_rule_value";
    private static final String RULE_KEY = "round_rule.";
    private String target = DEFAULTS;
    private RoundRule selected = RoundRule.RESPAWN_WAVE_SECONDS;
    private int editorLeft;
    private int editorWidth;
    private int editorTop;
    private int controlsTop;

    RoundRulesPage()
    {
        super("round_rules", Items.REPEATER, 410);
    }

    static void open(ICommandCentreView view, String roundId)
    {
        CommandCentreClient.roundRulesPage().ifPresent(page -> page.target = roundId);
        view.showPage(ID);
    }

    private RoundRules defaults()
    {
        return RoundRules.load(data().rules().getCompound("roundRuleDefaults"));
    }

    private RoundRules overrides()
    {
        return DEFAULTS.equals(target) ? defaults() : RoundRules.load(data().rules().getCompound("roundRuleOverrides").getCompound(target));
    }

    private String value(RoundRule rule)
    {
        return overrides().resolve(defaults()).get(rule);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        List<String> targets = new ArrayList<>();
        targets.add(DEFAULTS);
        targets.addAll(data().rounds().stream().map(TeamsData.Round::id).toList());
        if (!targets.contains(target))
            target = DEFAULTS;
        choice(view, tr("round_rules.target"), targets, target,
            id -> DEFAULTS.equals(id)
                ? tr("round_rules.defaults")
                : data().roundById(id).map(round -> Component.literal("#" + round.index() + " ").append(data().title(round))).orElse(Component.empty()),
            view.getLeft(), view.getTop(), view.getWidth(), id ->
            {
                target = id;
                view.rebuild();
            }).setTooltip(Tooltip.create(tr("round_rules.next_start")));
        int top = view.getTop() + ROW + 24;
        int listWidth = Math.max(140, view.getWidth() / 2 - GAP);
        editorLeft = view.getLeft() + listWidth + 10;
        editorWidth = view.getWidth() - listWidth - 12;
        editorTop = top;
        controlsTop = view.getTop() + view.getHeight() - 2 * ROW - 16;
        List<CommandCentreRow> rows = new ArrayList<>();
        for (RoundRule rule : RoundRule.values())
        {
            boolean overridden = overrides().overrides(rule);
            rows.add(CommandCentreRow.of(rule.getKey(), tr(RULE_KEY + rule.getKey()), Component.literal(displayValue(rule, value(rule)))).selected(rule == selected)
                .withBadge(tr(overridden ? "round_rules.override" : "round_rules.inherited"), overridden ? CommandCentreRow.WARNING : CommandCentreRow.NEUTRAL));
        }
        view.addList("round_rules", new ScreenRectangle(view.getLeft(), top, listWidth, view.getHeight() - (top - view.getTop())), rows, Component.empty(), key ->
        {
            RoundRule.find(key).ifPresent(rule -> selected = rule);
            view.rebuild();
        });
        initEditor(view);
    }

    private void initEditor(ICommandCentreView view)
    {
        int y = controlsTop;
        if (selected.getKind() == RoundRule.Kind.BOOLEAN)
            toggle(view, tr(RULE_KEY + selected.getKey()), Boolean.parseBoolean(value(selected)), editorLeft, y, editorWidth, tr(RULE_KEY + selected.getKey() + ".tip"),
                enabled -> set(view, Boolean.toString(enabled)));
        else
        {
            var field = view.addField(VALUE + "@" + target + "@" + selected.getKey(), editorLeft, y, editorWidth - 44, value(selected), tr(RULE_KEY + selected.getKey()));
            field.setMaxLength(1024);
            String retained = view.getField(VALUE + "@" + target + "@" + selected.getKey());
            if (!field.getValue().equals(retained))
                field.setValue(retained);
            field.setTooltip(Tooltip.create(tr(RULE_KEY + selected.getKey() + ".tip")));
            button(view, tr("set"), editorLeft + editorWidth - 40, y, 40, () ->
            {
                String text = view.getField(VALUE + "@" + target + "@" + selected.getKey());
                try
                {
                    set(view, selected.validate(text));
                }
                catch (IllegalArgumentException exception)
                {
                    view.showMessage(Component.literal(exception.getMessage()), true);
                }
            });
        }
        button(view, tr(DEFAULTS.equals(target) ? "round_rules.reset" : "round_rules.inherit"), editorLeft, y + ROW, editorWidth, () -> set(view, "inherit")).active = overrides().overrides(selected);
    }

    private void set(ICommandCentreView view, String value)
    {
        String destination = DEFAULTS.equals(target) ? DEFAULTS : data().roundById(target).map(round -> Integer.toString(round.index())).orElse("");
        if (!destination.isEmpty())
            run(view, "teams admin roundrule", destination, selected.getKey(), quote(value));
    }

    private static String displayValue(RoundRule rule, String value)
    {
        return (rule.getKind() == RoundRule.Kind.ITEMS || rule.getKind() == RoundRule.Kind.ERA) && value.isEmpty() ? "—" : value;
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        label(view, graphics, tr("round_rules.help"), view.getLeft() + 2, view.getTop() + ROW + 4, view.getWidth() - 4, CentreStyle.MUTED);
        label(view, graphics, tr(RULE_KEY + selected.getKey()), editorLeft, editorTop, editorWidth, CentreStyle.ACCENT);
        var description = view.getFont().split(tr(RULE_KEY + selected.getKey() + ".tip"), editorWidth);
        int descriptionTop = editorTop + 16;
        for (var line : description)
        {
            if (descriptionTop + view.getFont().lineHeight > controlsTop - GAP)
                break;
            graphics.drawString(view.getFont(), line, editorLeft, descriptionTop, CentreStyle.TEXT, false);
            descriptionTop += view.getFont().lineHeight;
        }
        int bottom = controlsTop + 2 * ROW + GAP;
        if (selected.getKind() == RoundRule.Kind.INTEGER)
        {
            paragraph(view, graphics, tr("round_rules.bounds", selected.getMinimum(), selected.getMaximum()), editorLeft, bottom, editorWidth, CentreStyle.MUTED);
        }

    }
}
