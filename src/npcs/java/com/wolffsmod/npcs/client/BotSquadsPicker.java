package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.SquadTemplate;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Chooses the squads a team fields from the squad templates: how many of each fight at once and how many it may raise in
 * all. Each template is previewed by its leader, with its members and posture; a template the team's settings name that
 * does not exist is listed after the others, marked. Client side.
 */
final class BotSquadsPicker extends BotPickerScreen
{
    private static final int MAX_COUNT = 99;
    private static final int MAX_BUDGET = 999;
    private static final int HEADING_HEIGHT = 12;
    private final BotSettings.TeamForce force;
    private final String teamName;
    @Nullable
    private final ITeam team;
    private final Map<String, SquadTemplate> templates = new LinkedHashMap<>();
    /** Squads of each template at once, in the order the team lists them, then the other templates. */
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, Integer> budgets = new HashMap<>();
    private final Map<String, LivingEntity> previews = new HashMap<>();

    BotSquadsPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, Component.translatable(PREFIX + "squads.title", team), Component.translatable(PREFIX + "squads.hint"));
        this.force = force;
        teamName = team;
        this.team = FlansModApi.getTeam(team).orElse(null);
        parent.templates().forEach(template -> templates.put(template.id(), template));
        // The same template listed twice, as commands allow, is shown as one entry raising both.
        for (BotSettings.SquadEntry entry : force.getSquads())
        {
            counts.merge(entry.template(), entry.count(), Integer::sum);
            budgets.merge(entry.template(), entry.budget(), (first, second) -> first == 0 || second == 0 ? 0 : first + second);
        }
        templates.keySet().forEach(id -> counts.putIfAbsent(id, 0));
        List<Choice> choices = new ArrayList<>();
        templates.values().forEach(template -> choices.add(templateChoice(template)));
        counts.keySet().stream().filter(id -> !templates.containsKey(id))
            .forEach(id -> choices.add(new Choice(id, Component.literal(id), List.of(Component.translatable(PREFIX + "squads.missing")), true)));
        setChoices(choices);
    }

    private Choice templateChoice(SquadTemplate template)
    {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal(template.id()).withStyle(ChatFormatting.GRAY));
        details.add(Component.translatable(PREFIX + "squads.posture", Component.translatable("wolffsmodnpcs.editor.posture." + template.posture().name().toLowerCase(Locale.ROOT))));
        details.add(Component.translatable(PREFIX + "squads.size", template.roster().size()));
        boolean leader = true;
        for (SquadTemplate.Member member : template.members())
        {
            Component name = FlansModApi.getPlayerClass(member.playerClass()).<Component>map(IPlayerClass::getNameComponent).orElse(Component.literal(member.playerClass()));
            Component line = Component.translatable(PREFIX + (leader ? "squads.leader" : "squads.member"), member.count(), name);
            details.add(offers(member.playerClass()) ? line : line.copy().append(Component.translatable(PREFIX + "squads.stand_in")).withStyle(ChatFormatting.GOLD));
            leader = false;
        }
        return new Choice(template.id(), Component.literal(template.name().isBlank() ? template.id() : template.name()), details, false);
    }

    private boolean offers(String playerClass)
    {
        return team != null && team.getClasses().stream().anyMatch(candidate -> candidate.getShortName().equals(playerClass));
    }

    @Override
    protected int controlsWidth()
    {
        return 2 * STEPPER_WIDTH + GAP;
    }

    @Override
    protected int headingHeight()
    {
        return HEADING_HEIGHT;
    }

    @Override
    protected void addHeadings(int x, int y)
    {
        addHeading(x, y, "squads.at_once");
        addHeading(x + STEPPER_WIDTH + GAP, y, "squads.in_all");
    }

    private void addHeading(int x, int y, String key)
    {
        StringWidget heading = new StringWidget(x, y, STEPPER_WIDTH, HEADING_HEIGHT - 2, Component.translatable(PREFIX + key), font).alignCenter();
        heading.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + ".tip")));
        addRenderableWidget(heading);
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        addStepper(x, y, counts.getOrDefault(choice.id(), 0), MAX_COUNT, value -> counts.put(choice.id(), value), Component.translatable(PREFIX + "squads.at_once.tip"));
        addStepper(x + STEPPER_WIDTH + GAP, y, budgets.getOrDefault(choice.id(), 0), MAX_BUDGET, value -> budgets.put(choice.id(), value), Component.translatable(PREFIX + "squads.in_all.tip"));
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return counts.getOrDefault(choice.id(), 0) > 0;
    }

    @Override
    protected Component suffix(Choice choice)
    {
        int count = counts.getOrDefault(choice.id(), 0);
        return count > 0 ? Component.literal(" ×" + count).withStyle(ChatFormatting.GRAY) : Component.empty();
    }

    /** Previews a template by its leader, dressed as the team's class when the team offers it. */
    @Nullable
    @Override
    protected LivingEntity preview(Choice choice)
    {
        SquadTemplate template = templates.get(choice.id());
        if (template == null || template.members().isEmpty() || minecraft == null || minecraft.level == null)
            return null;
        String leader = template.members().get(0).playerClass();
        Level level = minecraft.level;
        return FlanSoldierSelection.resolve(offers(leader) ? teamName : "", leader).map(selection -> previews.computeIfAbsent(choice.id(), id -> NpcPreviews.ofClass(level, selection))).orElse(null);
    }

    @Override
    protected List<Component> summary()
    {
        int squads = 0;
        int soldiers = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet())
        {
            squads += entry.getValue();
            SquadTemplate template = templates.get(entry.getKey());
            soldiers += template == null ? 0 : entry.getValue() * template.roster().size();
        }
        if (templates.isEmpty())
            return List.of(Component.translatable(PREFIX + "squads.no_templates").withStyle(ChatFormatting.GOLD));
        return List.of(Component.translatable(PREFIX + "squads.total", squads, soldiers));
    }

    @Override
    protected void apply()
    {
        List<BotSettings.SquadEntry> squads = new ArrayList<>();
        counts.forEach((template, count) ->
        {
            if (count > 0)
                squads.add(new BotSettings.SquadEntry(template, count, budgets.getOrDefault(template, 0)));
        });
        force.getSquads().clear();
        force.getSquads().addAll(squads);
    }
}
