package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.teams.BotSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Chooses how often each class of a team is given to its bots: a weight per class, 0 leaving the class out. Weights of
 * 1 for every class, or none above 0, are saved as no weights, which gives every class the same chance. Client side.
 */
final class BotClassWeightsPicker extends BotClassPicker
{
    private static final int MAX_WEIGHT = 999;
    private final BotSettings.TeamForce force;
    private final Map<String, Integer> weights = new LinkedHashMap<>();

    BotClassWeightsPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, team, Component.translatable(PREFIX + "classes.title", team), Component.translatable(PREFIX + "classes.hint"));
        this.force = force;
        weights.putAll(force.effectiveClassWeights(offered()));
        force.getClassWeights().forEach(weights::putIfAbsent);
        setChoices(classChoices(force.getClassWeights().keySet()));
    }

    /** @return each offered class's weight as bots are given classes by it: 1 each while none is above 0 */
    private Map<String, Integer> effective()
    {
        BotSettings.TeamForce chosen = new BotSettings.TeamForce();
        chosen.getClassWeights().putAll(weights);
        return chosen.effectiveClassWeights(offered());
    }

    @Override
    protected int controlsWidth()
    {
        return STEPPER_WIDTH;
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        addStepper(x, y, weights.getOrDefault(choice.id(), 0), MAX_WEIGHT, value -> weights.put(choice.id(), value), Component.translatable(PREFIX + "classes.weight.tip"));
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return effective().getOrDefault(choice.id(), 0) > 0;
    }

    @Override
    protected Component suffix(Choice choice)
    {
        Map<String, Integer> effective = effective();
        int weight = effective.getOrDefault(choice.id(), 0);
        int sum = effective.values().stream().mapToInt(Integer::intValue).sum();
        if (weight <= 0 || sum <= 0)
            return Component.empty();
        return Component.literal(" · " + Math.round(weight * 100F / sum) + "%").withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void addFooter(int x, int y, int width)
    {
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "classes.equal"), button ->
        {
            weights.replaceAll((playerClass, weight) -> offers(playerClass) ? 1 : 0);
            refreshPage();
        }).bounds(x, y, Math.min(width, 120), BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(PREFIX + "classes.equal.tip"))).build());
    }

    @Override
    protected List<Component> summary()
    {
        List<Component> summary = new ArrayList<>(super.summary());
        boolean weighted = offered().stream().anyMatch(playerClass -> weights.getOrDefault(playerClass, 0) > 0);
        summary.add(Component.translatable(PREFIX + (weighted ? "classes.weighted" : "classes.unweighted")));
        return summary;
    }

    @Override
    protected void apply()
    {
        Map<String, Integer> saved = new LinkedHashMap<>();
        weights.forEach((playerClass, weight) ->
        {
            if (weight > 0)
                saved.put(playerClass, weight);
        });
        boolean equal = saved.size() == offered().size() && offered().stream().allMatch(playerClass -> saved.getOrDefault(playerClass, 0) == 1);
        boolean weighted = offered().stream().anyMatch(playerClass -> saved.getOrDefault(playerClass, 0) > 0);
        force.getClassWeights().clear();
        if (weighted && !equal)
            force.getClassWeights().putAll(saved);
    }
}
