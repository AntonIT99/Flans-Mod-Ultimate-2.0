package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.teams.BotSettings;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Chooses the optional commander class of a team, with the usual class previews. Client side. */
final class BotCommanderClassPicker extends BotClassPicker
{
    private final BotSettings.TeamForce force;
    private String chosen;

    BotCommanderClassPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, team, Component.translatable("wolffsmodnpcs.editor.commander_class"), Component.translatable("wolffsmodnpcs.editor.commander_class.tip"));
        this.force = force;
        chosen = force.getCommanderClass();
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice("", Component.translatable(PREFIX + "crew.none"), List.of(), false));
        choices.addAll(classChoices(chosen.isEmpty() ? List.of() : List.of(chosen)));
        setChoices(choices);
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return choice.id().equals(chosen);
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
    protected void apply()
    {
        force.setCommanderClass(chosen);
    }
}
