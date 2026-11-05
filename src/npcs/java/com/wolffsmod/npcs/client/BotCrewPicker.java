package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the classes that crew one kind of seat of a team's vehicles: selected classes, any class by the class
 * weights, or nobody. Client side.
 */
final class BotCrewPicker extends BotClassPicker
{
    private static final String INHERIT = ":inherit";
    private final BotSettings.TeamForce force;
    private final CrewPost post;
    private final String scope;
    private final boolean crewsOn;
    /** Exclusive mode, or empty when individual classes are selected. */
    private String mode;
    private final List<String> chosen = new ArrayList<>();

    BotCrewPicker(BotEditorScreen editor, Screen parent, String team, BotSettings.TeamForce force, String scope, CrewPost post, boolean crewsOn)
    {
        super(editor, parent, team, Component.translatable(PREFIX + "crew.title", Component.translatable(PREFIX + "crew." + post.id()), team),
            Component.translatable(PREFIX + "crew.hint." + post.id()));
        this.force = force;
        this.post = post;
        this.scope = scope;
        this.crewsOn = crewsOn;
        String value = force.crewClass(scope, post);
        chosen.addAll(BotSettingsText.crewClasses(value));
        mode = chosen.isEmpty() ? (value == null ? INHERIT : BotSettingsText.crewClass(value)) : "";
        setChoices(crewChoices());
    }

    private List<Choice> crewChoices()
    {
        List<Choice> choices = new ArrayList<>();
        if (!scope.isEmpty())
            choices.add(new Choice(INHERIT, Component.translatable(PREFIX + "crew.inherit"), List.of(Component.translatable(PREFIX + "crew.inherit.tip")), false));
        choices.add(new Choice(BotSettingsText.ANY_CLASS, Component.translatable(PREFIX + "crew.any"), List.of(Component.translatable(PREFIX + "crew.any.tip")), false));
        choices.add(new Choice(BotSettings.TeamForce.NO_CREW, Component.translatable(PREFIX + "crew.none"), List.of(Component.translatable(PREFIX + "crew.none.tip")), false));
        choices.addAll(classChoices(chosen));
        return choices;
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return choice.id().equalsIgnoreCase(mode) || chosen.contains(choice.id());
    }

    @Override
    protected void clicked(Choice choice)
    {
        String id = choice.id();
        if (INHERIT.equals(id) || BotSettingsText.ANY_CLASS.equals(id) || BotSettings.TeamForce.NO_CREW.equals(id))
        {
            chosen.clear();
            mode = id;
        }
        else
        {
            mode = "";
            if (!chosen.remove(id))
                chosen.add(id);
            if (chosen.isEmpty())
                mode = BotSettingsText.ANY_CLASS;
        }
    }

    @Override
    protected List<Component> summary()
    {
        List<Component> summary = new ArrayList<>(super.summary());
        if (!crewsOn)
            summary.add(Component.translatable(PREFIX + "crew.off").withStyle(ChatFormatting.GOLD));
        return summary;
    }

    @Override
    protected void apply()
    {
        force.setCrewClass(scope, post, INHERIT.equals(mode) ? null : mode.isEmpty() ? String.join("|", chosen) : BotSettingsText.parseCrewClass(mode));
    }
}
