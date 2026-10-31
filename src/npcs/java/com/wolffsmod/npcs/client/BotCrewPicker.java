package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the class that crews one kind of seat of a team's vehicles: one of its classes, any class by the class
 * weights, or nobody. Client side.
 */
final class BotCrewPicker extends BotClassPicker
{
    private final BotSettings.TeamForce force;
    private final CrewPost post;
    private final boolean crewsOn;
    /** The choice as the editor shows it: a class, {@code any} or {@code none}. */
    private String chosen;

    BotCrewPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force, CrewPost post, boolean crewsOn)
    {
        super(parent, team, Component.translatable(PREFIX + "crew.title", Component.translatable(PREFIX + "crew." + post.id()), team), Component.translatable(PREFIX + "crew.hint." + post.id()));
        this.force = force;
        this.post = post;
        this.crewsOn = crewsOn;
        chosen = BotSettingsText.crewClass(force.crewClass(post));
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(BotSettingsText.ANY_CLASS, Component.translatable(PREFIX + "crew.any"), List.of(Component.translatable(PREFIX + "crew.any.tip")), false));
        choices.add(new Choice(BotSettings.TeamForce.NO_CREW, Component.translatable(PREFIX + "crew.none"), List.of(Component.translatable(PREFIX + "crew.none.tip")), false));
        boolean named = !chosen.equals(BotSettingsText.ANY_CLASS) && !chosen.equalsIgnoreCase(BotSettings.TeamForce.NO_CREW);
        choices.addAll(classChoices(named ? List.of(chosen) : List.of()));
        setChoices(choices);
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return choice.id().equalsIgnoreCase(chosen);
    }

    @Override
    protected void clicked(Choice choice)
    {
        chosen = choice.id();
    }

    @Override
    protected boolean doubleClickIsDone()
    {
        return true;
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
        force.setCrewClass(post, BotSettingsText.parseCrewClass(chosen));
    }
}
