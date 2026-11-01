package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.teams.BotSettings;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Stream;

/**
 * Chooses the role of each class of a team: what its soldiers do for their squad besides fighting. A class left on Auto
 * takes the role its equipment suggests, which the list shows. Client side.
 */
final class BotRolesPicker extends BotClassPicker
{
    private static final int ROLE_WIDTH = 112;
    /** Auto, then every role. */
    private static final List<Role> CHOICES = Stream.concat(Stream.of(new Role(null)), Arrays.stream(BotSettings.ClassRole.values()).map(Role::new)).toList();
    private final BotSettings.TeamForce force;
    private final Map<String, BotSettings.ClassRole> roles = new LinkedHashMap<>();

    /** A choice of the role buttons: a role set by hand, or none for the one the equipment suggests. */
    private record Role(@Nullable BotSettings.ClassRole role)
    {}

    BotRolesPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, team, Component.translatable(PREFIX + "roles.title", team), Component.translatable(PREFIX + "roles.hint"));
        this.force = force;
        roles.putAll(force.getClassRoles());
        setChoices(classChoices(force.getClassRoles().keySet()));
    }

    @Override
    protected int controlsWidth()
    {
        return ROLE_WIDTH;
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        String playerClass = choice.id();
        addRowWidget(CycleButton.<Role>builder(role -> roleLabel(playerClass, role)).withValues(CHOICES).withInitialValue(new Role(roles.get(playerClass))).displayOnlyValue()
            .withTooltip(role -> Tooltip.create(roleTip(playerClass, role))).create(x, y, ROLE_WIDTH, BUTTON_HEIGHT, Component.translatable(PREFIX + "roles.role"), (button, role) ->
            {
                if (role.role() == null)
                    roles.remove(playerClass);
                else
                    roles.put(playerClass, role.role());
                refreshNames();
            }));
    }

    private Component roleLabel(String playerClass, Role role)
    {
        if (role.role() != null)
            return roleName(role.role());
        return Component.translatable(PREFIX + "roles.auto", roleName(suggestedRole(playerClass))).withStyle(ChatFormatting.GRAY);
    }

    private Component roleTip(String playerClass, Role role)
    {
        if (role.role() != null)
            return roleName(role.role()).copy().append("\n").append(roleHelp(role.role()));
        BotSettings.ClassRole suggested = suggestedRole(playerClass);
        return Component.translatable(PREFIX + "roles.auto.tip", roleName(suggested)).append("\n").append(roleHelp(suggested));
    }

    /** A class whose role is set by hand. */
    @Override
    protected boolean isChosen(Choice choice)
    {
        return roles.containsKey(choice.id());
    }

    @Override
    protected void addFooter(int x, int y, int width)
    {
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "roles.all_auto"), button ->
        {
            roles.clear();
            refreshPage();
        }).bounds(x, y, Math.min(width, 120), BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(PREFIX + "roles.all_auto.tip"))).build());
    }

    @Override
    protected List<Component> summary()
    {
        List<Component> summary = new ArrayList<>(super.summary());
        summary.add(Component.translatable(PREFIX + "roles.summary", roles.size()));
        return summary;
    }

    @Override
    protected void apply()
    {
        force.getClassRoles().clear();
        force.getClassRoles().putAll(roles);
    }
}
