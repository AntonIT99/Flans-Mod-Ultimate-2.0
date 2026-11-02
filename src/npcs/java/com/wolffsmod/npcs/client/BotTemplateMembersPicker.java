package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.teams.SquadTemplate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Chooses a reusable squad's class counts and leader using the class search and loadout previews. Client side. */
final class BotTemplateMembersPicker extends BotClassPicker
{
    private static final int MAX_COUNT = 999;
    private static final int LEADER_WIDTH = 52;
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, Button> leaderButtons = new HashMap<>();
    private final Consumer<List<SquadTemplate.Member>> save;
    private String leader;

    BotTemplateMembersPicker(BotEditorScreen parent, List<SquadTemplate.Member> members, Consumer<List<SquadTemplate.Member>> save)
    {
        super(parent, null, Component.translatable(PREFIX + "members.title"), Component.translatable(PREFIX + "members.hint"));
        this.save = save;
        members.forEach(member -> counts.merge(member.playerClass(), member.count(), Integer::sum));
        leader = members.isEmpty() ? "" : members.get(0).playerClass();
        setChoices(classChoices(counts.keySet()));
    }

    @Override
    protected int controlsWidth()
    {
        return LEADER_WIDTH + GAP + STEPPER_WIDTH;
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        Button leaderButton = addRowWidget(Button.builder(Component.translatable(PREFIX + "members.leader"), button ->
        {
            leader = choice.id();
            refreshPage();
        }).bounds(x, y, LEADER_WIDTH, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(PREFIX + "members.leader.tip"))).build());
        leaderButton.active = isChosen(choice) && !choice.id().equals(leader);
        leaderButtons.put(choice.id(), leaderButton);
        // Existing larger counts remain editable without truncating them just by opening the picker.
        addStepper(x + LEADER_WIDTH + GAP, y, counts.getOrDefault(choice.id(), 0), Math.max(MAX_COUNT, counts.getOrDefault(choice.id(), 0)), value -> setCount(choice.id(), value),
            Component.translatable(PREFIX + "members.count.tip"));
    }

    private void setCount(String playerClass, int count)
    {
        counts.put(playerClass, count);
        if (counts.getOrDefault(leader, 0) == 0)
            leader = counts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse("");
        // Keep the number box focused while typing; only the leader buttons need their state updated.
        leaderButtons.forEach((id, button) -> button.active = counts.getOrDefault(id, 0) > 0 && !id.equals(leader));
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return counts.getOrDefault(choice.id(), 0) > 0;
    }

    @Override
    protected Component suffix(Choice choice)
    {
        return choice.id().equals(leader) ? Component.literal(" · ").append(Component.translatable(PREFIX + "members.leader")).withStyle(ChatFormatting.GRAY) : Component.empty();
    }

    @Override
    protected List<Component> summary()
    {
        return List.of(Component.translatable(PREFIX + "members.total", counts.values().stream().mapToInt(Integer::intValue).sum()));
    }

    @Override
    protected void apply()
    {
        List<SquadTemplate.Member> members = new ArrayList<>();
        if (counts.getOrDefault(leader, 0) > 0)
            members.add(new SquadTemplate.Member(leader, counts.get(leader)));
        counts.forEach((playerClass, count) ->
        {
            if (count > 0 && !playerClass.equals(leader))
                members.add(new SquadTemplate.Member(playerClass, count));
        });
        save.accept(List.copyOf(members));
    }
}
