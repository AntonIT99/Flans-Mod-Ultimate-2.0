package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.model.*;
import com.wolffsmod.npcs.teams.BotSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/** Browses shared crew defaults, driveable kinds and definition overrides on a private draft. Client side. */
final class BotCrewScopesPicker extends BotPickerScreen
{
    private final String team;
    private final BotSettings.TeamForce force;
    private final BotSettings.TeamForce draft = new BotSettings.TeamForce();
    private final boolean crewsOn;
    private final Map<String, FlanModelEntityType> definitions = new LinkedHashMap<>();
    private final Map<String, LivingEntity> previews = new HashMap<>();

    BotCrewScopesPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force, boolean crewsOn)
    {
        super(parent, Component.translatable(PREFIX + "crew.scopes.title", team), Component.translatable(PREFIX + "crew.scopes.hint"));
        this.team = team;
        this.force = force;
        this.crewsOn = crewsOn;
        draft.copyCrewFrom(force);
        FlanModelEntities.getEntityTypes().stream().sorted(Comparator.comparing(FlanModelEntityType::getShortName))
            .forEach(type -> definitions.put(BotSettings.TeamForce.crewDefinitionScope(type.getShortName()), type));
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice("", Component.translatable(PREFIX + "crew.scope.all"), List.of(Component.translatable(PREFIX + "crew.scope.all.tip")), false));
        for (FlanModelKind kind : FlanModelKind.values())
            choices.add(new Choice(BotSettings.TeamForce.crewScope(kind), kindName(kind), List.of(Component.translatable(PREFIX + "crew.scope.kind.tip")), false));
        definitions.forEach((scope, type) ->
        {
            var definition = type.getInfoType();
            Component name = definition == null ? Component.literal(type.getShortName()) : definition.getNameComponent();
            if (name.getString().isBlank())
                name = Component.literal(type.getShortName());
            choices.add(new Choice(scope, name, List.of(Component.literal(type.getShortName()), kindName(type.getKind())), false));
        });
        draft.crewScopes().stream().filter(scope -> choices.stream().noneMatch(choice -> choice.id().equals(scope)))
            .forEach(scope -> choices.add(new Choice(scope, Component.literal(scope), List.of(Component.translatable(PREFIX + "vehicles.missing")), true)));
        setChoices(choices);
    }

    private static Component kindName(FlanModelKind kind)
    {
        return Component.translatable(PREFIX + "crew.scope." + kind.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected int controlsWidth()
    {
        return 40;
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        addRowWidget(Button.builder(Component.translatable(PREFIX + "crew.edit"), button -> open(choice)).bounds(x, y, 40, BUTTON_HEIGHT).build());
    }

    private void open(Choice choice)
    {
        Minecraft.getInstance().setScreen(new BotCrewSettingsScreen(parent(), this, team, draft, choice.id(), choice.name(), crewsOn));
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return !choice.id().isEmpty() && draft.crewScopes().contains(choice.id());
    }

    @Override
    protected Component suffix(Choice choice)
    {
        if (choice.id().isEmpty())
            return Component.empty();
        long count = Arrays.stream(CrewPost.values()).filter(post -> draft.crewClass(choice.id(), post) != null).count();
        return Component.translatable(PREFIX + "crew.scopes.count", count);
    }

    @Override
    protected LivingEntity preview(Choice choice)
    {
        FlanModelEntityType type = definitions.get(choice.id());
        if (type == null || minecraft == null || minecraft.level == null)
            return null;
        return previews.computeIfAbsent(choice.id(), ignored -> new FlanModelEntity(type, minecraft.level));
    }

    @Override
    protected List<Component> summary()
    {
        return List.of(Component.translatable(PREFIX + (crewsOn ? "crew.scopes.hint" : "crew.off")));
    }

    @Override
    protected void apply()
    {
        force.copyCrewFrom(draft);
    }
}
