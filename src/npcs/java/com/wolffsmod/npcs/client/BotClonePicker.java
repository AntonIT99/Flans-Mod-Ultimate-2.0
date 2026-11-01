package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.crew.CrewSource;
import com.wolffsmod.npcs.network.BotClonesPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.teams.BotSettings;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * Chooses the Custom NPCs clone a team's bots are made from, or none for plain NPCs, from the clones saved on the
 * server, a tab of the cloner at a time, each previewed as it was saved. Whether bots keep the clone's equipment is
 * chosen here too. Client side.
 */
final class BotClonePicker extends BotPickerScreen
{
    /** The entry that makes plain NPCs. */
    private static final String PLAIN = "";
    private static final int ARROW_WIDTH = 20;
    private final BotSettings.TeamForce force;
    /** The tab browsed. */
    private int tab;
    /** The tab of the chosen clone, which need not be the one browsed. */
    private int chosenTab;
    private String chosenName;
    private boolean keepEquipment;
    private boolean loading;
    private List<String> names = List.of();
    @Nullable
    private UUID listToken;
    /** Preview requests waiting for an answer, by clone name. */
    private final Map<String, UUID> pending = new HashMap<>();
    private final Map<String, LivingEntity> previews = new HashMap<>();
    /** Clones whose preview came back empty. */
    private final Set<String> unavailable = new HashSet<>();

    BotClonePicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, Component.translatable(PREFIX + "clone.title", team), Component.translatable(PREFIX + "clone.hint"));
        this.force = force;
        chosenTab = force.usesClone() ? Math.max(1, Math.min(CrewSource.MAX_CLONE_TAB, force.getCloneTab())) : 0;
        chosenName = force.usesClone() ? force.getCloneName() : PLAIN;
        tab = Math.max(1, chosenTab);
        keepEquipment = force.isKeepCloneEquipment();
        showChoices();
    }

    @Override
    protected void init()
    {
        super.init();
        if (listToken == null && !loading)
            requestList();
    }

    private void requestList()
    {
        loading = true;
        names = List.of();
        pending.clear();
        previews.clear();
        unavailable.clear();
        listToken = UUID.randomUUID();
        showChoices();
        NpcNetwork.sendToServer(BotClonesPacket.list(tab, listToken));
    }

    private void showChoices()
    {
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice(PLAIN, Component.translatable(PREFIX + "clone.plain"), List.of(Component.translatable(PREFIX + "clone.plain.tip")), false));
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        sorted.forEach(name -> choices.add(new Choice(name, Component.literal(name), List.of(Component.translatable(PREFIX + "clone.saved", tab)), false)));
        // A chosen clone that is no longer saved stays listed, so the operator sees why bots will be plain.
        if (!loading && tab == chosenTab && !chosenName.isEmpty() && !names.contains(chosenName))
            choices.add(new Choice(chosenName, Component.literal(chosenName), List.of(Component.translatable(PREFIX + "clone.gone", tab)), true));
        setChoices(choices);
    }

    /** Takes the server's answer: the clones of the tab browsed, or a clone's preview. */
    void receive(BotClonesPacket packet)
    {
        if (packet.request() || packet.tab() != tab)
            return;
        if (packet.name().isEmpty())
        {
            if (!packet.token().equals(listToken))
                return;
            names = packet.names();
            loading = false;
            showChoices();
            return;
        }
        if (!packet.token().equals(pending.get(packet.name())) || minecraft == null || minecraft.level == null)
            return;
        pending.remove(packet.name());
        NpcPreviews.ofClone(minecraft.level, packet.data()).ifPresentOrElse(npc -> previews.put(packet.name(), npc), () -> unavailable.add(packet.name()));
    }

    @Override
    protected void addHeader(int x, int y, int width)
    {
        addRenderableWidget(Button.builder(Component.literal("<"), button -> browse(-1)).bounds(x, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
        Button label = addRenderableWidget(Button.builder(Component.translatable(PREFIX + "clone.tab", tab), button ->
        {
            // The label only names the tab.
        }).bounds(x + ARROW_WIDTH + 2, y, width - 2 * ARROW_WIDTH - 4, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable(PREFIX + "clone.tab.tip"))).build());
        label.active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> browse(1)).bounds(x + width - ARROW_WIDTH, y, ARROW_WIDTH, BUTTON_HEIGHT).build());
    }

    private void browse(int step)
    {
        tab = Math.floorMod(tab - 1 + step, CrewSource.MAX_CLONE_TAB) + 1;
        requestList();
        rebuildWidgets();
    }

    @Override
    protected void addFooter(int x, int y, int width)
    {
        addRenderableWidget(CycleButton.onOffBuilder(keepEquipment).withTooltip(value -> Tooltip.create(Component.translatable(PREFIX + "clone.keep.tip"))).create(x, y, Math.min(width, 150),
            BUTTON_HEIGHT, Component.translatable(PREFIX + "clone.keep"), (button, value) -> keepEquipment = value));
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return choice.id().isEmpty() ? chosenName.isEmpty() : tab == chosenTab && choice.id().equals(chosenName);
    }

    @Override
    protected void clicked(Choice choice)
    {
        chosenName = choice.id();
        chosenTab = chosenName.isEmpty() ? 0 : tab;
    }

    @Override
    protected boolean doubleClickIsDone()
    {
        return true;
    }

    /** Asks the server for a clone's preview the first time it is shown. */
    @Nullable
    @Override
    protected LivingEntity preview(Choice choice)
    {
        String name = choice.id();
        if (name.isEmpty() || loading || !names.contains(name))
            return null;
        LivingEntity preview = previews.get(name);
        if (preview == null && !pending.containsKey(name) && !unavailable.contains(name))
        {
            UUID token = UUID.randomUUID();
            pending.put(name, token);
            NpcNetwork.sendToServer(BotClonesPacket.preview(tab, name, token));
        }
        return preview;
    }

    @Override
    protected Component previewStatus(Choice choice)
    {
        if (choice.id().isEmpty())
            return Component.translatable(PREFIX + "clone.plain.preview");
        return Component.translatable(PREFIX + (pending.containsKey(choice.id()) ? "loading" : "no_preview"));
    }

    @Override
    protected List<Component> summary()
    {
        List<Component> summary = new ArrayList<>();
        if (loading)
            summary.add(Component.translatable(PREFIX + "clone.loading", tab));
        else if (names.isEmpty())
            summary.add(Component.translatable(PREFIX + "clone.empty", tab).withStyle(ChatFormatting.GOLD));
        else
            summary.add(Component.translatable(PREFIX + "clone.count", names.size(), tab));
        summary.add(chosenName.isEmpty() ? Component.translatable(PREFIX + "clone.chosen_plain") : Component.translatable(PREFIX + "clone.chosen", chosenTab, chosenName));
        return summary;
    }

    @Override
    protected void apply()
    {
        force.setCloneTab(chosenName.isEmpty() ? 0 : chosenTab);
        force.setCloneName(chosenName);
        force.setKeepCloneEquipment(keepEquipment);
    }
}
