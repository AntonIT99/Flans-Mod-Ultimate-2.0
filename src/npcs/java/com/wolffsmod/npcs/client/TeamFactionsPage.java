package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.FactionJoiningPacket;
import com.wolffsmod.npcs.network.FactionRelationsPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.teams.FactionJoining;
import com.wolffsmod.npcs.teams.FactionRelations;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;

/**
 * The Team factions page: creates a Custom NPCs faction from a Flan team, lists the factions, sets the ones chosen neutral,
 * allied or hostile to each other, and sets who may join the one chosen, what leaving it costs, and whether the builder
 * belongs to it. The server sends the list on request and after every change it makes.
 */
final class TeamFactionsPage implements ICustomScrollListener
{
    private static final String PREFIX = "wolffsmodnpcs.team_faction.";
    private static final int CREATE_ID = 2500;
    private static final int NEUTRAL_ID = 2501;
    private static final int ALLIED_ID = 2502;
    private static final int HOSTILE_ID = 2503;
    private static final int SELECT_ALL_ID = 2504;
    private static final int SELECT_NONE_ID = 2505;
    private static final int LIST_ID = 2506;
    private static final int RULE_ID = 2507;
    private static final int MEMBERSHIP_ID = 2508;
    private static final int LABEL_ID = 2510;
    private static final int PENALTY_ID = 2514;
    private static final int COOLDOWN_ID = 2515;
    private static final int LIST_WIDTH = 250;
    private static final int LIST_HEIGHT = 104;
    private static final int SIDE_WIDTH = 142;
    private static final int ROW_HEIGHT = 22;
    private static final int RULE_WIDTH = 124;
    private static final int FIELD_WIDTH = 40;
    private static final int LABEL_WIDTH = 46;
    private static final int MAX_NUMBER = 999_999;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;

    private final GuiWolffsMod gui;
    private final EntityNPCInterface npc;
    private final GuiCustomScrollNop list;
    /** The factions the server last listed, or null until it answers. */
    private List<FactionRelationsPacket.Entry> factions;
    /** The list's text for each faction id, unique even for factions of the same name. */
    private final Map<Integer, String> labels = new LinkedHashMap<>();
    private final Set<Integer> chosen = new LinkedHashSet<>();
    /** The faction the builder belongs to, as the server last listed it. */
    private int own = FactionRelationsPacket.NO_FACTION;
    private boolean requested;

    TeamFactionsPage(GuiWolffsMod gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
        list = new GuiCustomScrollNop(gui, LIST_ID, true);
        list.listener = this;
    }

    void init(int top)
    {
        if (!requested)
        {
            requested = true;
            NpcNetwork.sendToServer(FactionRelationsPacket.list(npc.getId()));
        }
        int left = gui.guiLeft + 8;
        int y = gui.guiTop + top;
        gui.addButton(new GuiButtonNop(gui, CREATE_ID, left, y, LIST_WIDTH, 20, PREFIX + "create"));
        int x = left + LIST_WIDTH + 4;
        label(LABEL_ID, status(), x, y + 5, SIDE_WIDTH);
        y += 24;
        list.setSize(LIST_WIDTH, LIST_HEIGHT);
        list.guiLeft = left;
        list.guiTop = y;
        list.setUnsortedList(new ArrayList<>(labels.values()));
        list.setSelectedList(chosen.stream().map(labels::get).filter(Objects::nonNull).toList());
        gui.addScroll(list);
        boolean pair = chosen.size() >= 2;
        action(NEUTRAL_ID, x, y, "make_neutral", pair);
        action(ALLIED_ID, x, y + ROW_HEIGHT, "make_allied", pair);
        action(HOSTILE_ID, x, y + 2 * ROW_HEIGHT, "make_hostile", pair);
        action(SELECT_ALL_ID, x, y + 3 * ROW_HEIGHT, "select_all", !labels.isEmpty() && chosen.size() < labels.size());
        action(SELECT_NONE_ID, x, y + 4 * ROW_HEIGHT, "select_none", !chosen.isEmpty());
        joining(left, y + LIST_HEIGHT + 6);
        label(LABEL_ID + 1, Component.translatable(PREFIX + "help"), left, y + LIST_HEIGHT + 30, LIST_WIDTH + 4 + SIDE_WIDTH);
    }

    /** The row setting who may join the one faction chosen and what leaving it costs, and the builder's membership. */
    private void joining(int left, int y)
    {
        FactionRelationsPacket.Entry entry = single();
        FactionJoining.Rule rule = entry == null ? FactionJoining.Rule.INVITE : entry.rule();
        String[] rules = Arrays.stream(FactionJoining.Rule.values()).map(value -> PREFIX + "join_rule." + value.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
        GuiButtonNop ruleButton = new GuiButtonNop(gui, RULE_ID, left, y, RULE_WIDTH, 20, rules, rule.ordinal());
        ruleButton.setEnabled(entry != null);
        ruleButton.setTooltip(Tooltip.create(Component.translatable(PREFIX + "join_rule.help")));
        gui.addButton(ruleButton);
        int x = left + RULE_WIDTH + 4;
        number(LABEL_ID + 2, PENALTY_ID, "penalty", entry == null ? null : entry.penalty(), x, y);
        x += LABEL_WIDTH + FIELD_WIDTH + 4;
        number(LABEL_ID + 3, COOLDOWN_ID, "cooldown", entry == null ? null : entry.cooldown(), x, y);
        x += LABEL_WIDTH + FIELD_WIDTH + 4;
        boolean member = entry != null && entry.id() == own;
        String key = member ? "leave" : "join";
        GuiButtonNop membership = new GuiButtonNop(gui, MEMBERSHIP_ID, x, y, left + LIST_WIDTH + 4 + SIDE_WIDTH - x, 20, PREFIX + key);
        membership.setEnabled(entry != null);
        membership.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + ".help", entry == null ? 0 : entry.members())));
        gui.addButton(membership);
    }

    /** A labelled number field of the joining row, blank and read-only unless one faction is chosen. */
    private void number(int labelId, int id, String key, Integer value, int x, int y)
    {
        Component help = Component.translatable(PREFIX + key + ".help");
        label(labelId, Component.translatable(PREFIX + key), help, x, y + 6, LABEL_WIDTH);
        GuiTextFieldNop field = new GuiTextFieldNop(id, gui, x + LABEL_WIDTH, y, FIELD_WIDTH, 20, value == null ? "" : Integer.toString(value));
        field.setEditable(value != null);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(0, MAX_NUMBER, value == null ? 0 : value);
        field.setTooltip(Tooltip.create(help));
        gui.addTextField(field);
    }

    private void action(int id, int x, int y, String key, boolean enabled)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, x, y, SIDE_WIDTH, 20, PREFIX + key);
        button.setEnabled(enabled);
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + ".help")));
        gui.addButton(button);
    }

    /** A one-line label cut to its width, with the whole text as its tooltip. */
    private void label(int id, Component text, int x, int y, int width)
    {
        label(id, text, text, x, y, width);
    }

    private void label(int id, Component text, Component tooltip, int x, int y, int width)
    {
        String shown = Minecraft.getInstance().font.plainSubstrByWidth(text.getString(), width);
        GuiLabel label = new GuiLabel(id, Component.literal(shown), 0x404040, x, y, width, 10);
        label.setTooltip(Tooltip.create(tooltip));
        gui.addLabel(label);
    }

    /** @return the one faction chosen, or null when none or several are */
    private FactionRelationsPacket.Entry single()
    {
        return chosen.size() == 1 ? entry(chosen.iterator().next()) : null;
    }

    /** @return how the one chosen faction stands toward the others, how many are chosen, or what the list is waiting for */
    private Component status()
    {
        if (factions == null)
            return Component.translatable(PREFIX + "loading");
        if (factions.isEmpty())
            return Component.translatable(PREFIX + "none");
        if (chosen.size() != 1)
            return Component.translatable(PREFIX + "chosen", chosen.size());
        FactionRelationsPacket.Entry entry = entry(chosen.iterator().next());
        if (entry == null)
            return Component.translatable(PREFIX + "chosen", 0);
        MutableComponent text = Component.translatable(PREFIX + "allies", names(entry.allies()));
        return text.append(" · ").append(Component.translatable(PREFIX + "enemies", names(entry.enemies())));
    }

    private Component names(List<Integer> ids)
    {
        MutableComponent text = Component.empty();
        for (Integer id : ids)
        {
            FactionRelationsPacket.Entry entry = entry(id);
            if (entry == null)
                continue;
            if (!text.getSiblings().isEmpty())
                text.append(", ");
            text.append(entry.name());
        }
        return text.getSiblings().isEmpty() ? Component.literal("-") : text;
    }

    private FactionRelationsPacket.Entry entry(int id)
    {
        return factions == null ? null : factions.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
    }

    boolean handles(int id)
    {
        return id >= CREATE_ID && id <= MEMBERSHIP_ID && id != LIST_ID;
    }

    /** @return whether the field was the leave penalty or rejoin cooldown, which it sends to the server when changed */
    boolean unFocused(GuiTextFieldNop field)
    {
        if (field.id != PENALTY_ID && field.id != COOLDOWN_ID)
            return false;
        FactionRelationsPacket.Entry entry = single();
        if (entry != null && field.isInteger())
        {
            int value = Math.max(0, Math.min(MAX_NUMBER, field.getInteger()));
            int penalty = field.id == PENALTY_ID ? value : entry.penalty();
            int cooldown = field.id == COOLDOWN_ID ? value : entry.cooldown();
            if (penalty != entry.penalty() || cooldown != entry.cooldown())
                send(entry, entry.rule(), penalty, cooldown, FactionJoiningPacket.Membership.KEEP);
        }
        return true;
    }

    private void send(FactionRelationsPacket.Entry entry, FactionJoining.Rule rule, int penalty, int cooldown, FactionJoiningPacket.Membership membership)
    {
        NpcNetwork.sendToServer(new FactionJoiningPacket(npc.getId(), entry.id(), rule, penalty, cooldown, membership));
    }

    void button(GuiButtonNop button)
    {
        switch (button.id)
        {
            case CREATE_ID -> {
                gui.save();
                gui.setSubGui(new TeamFactionMenu(npc.getId()));
            }
            case NEUTRAL_ID -> relate(FactionRelations.Relation.NEUTRAL);
            case ALLIED_ID -> relate(FactionRelations.Relation.ALLIED);
            case HOSTILE_ID -> relate(FactionRelations.Relation.HOSTILE);
            case SELECT_ALL_ID -> {
                chosen.addAll(labels.keySet());
                gui.init();
            }
            case SELECT_NONE_ID -> {
                chosen.clear();
                gui.init();
            }
            case RULE_ID -> {
                // The button has moved on to the next rule already.
                FactionRelationsPacket.Entry entry = single();
                if (entry != null)
                    send(entry, FactionJoining.Rule.values()[button.getValue()], entry.penalty(), entry.cooldown(), FactionJoiningPacket.Membership.KEEP);
            }
            case MEMBERSHIP_ID -> {
                FactionRelationsPacket.Entry entry = single();
                if (entry != null)
                    send(entry, entry.rule(), entry.penalty(), entry.cooldown(), entry.id() == own ? FactionJoiningPacket.Membership.LEAVE : FactionJoiningPacket.Membership.JOIN);
            }
            default -> {
                // Not a button of this page.
            }
        }
    }

    private void relate(FactionRelations.Relation relation)
    {
        if (chosen.size() >= 2)
            NpcNetwork.sendToServer(FactionRelationsPacket.set(npc.getId(), relation, chosen));
    }

    /** Takes the server's list, keeping the factions still listed chosen. */
    void receive(FactionRelationsPacket packet)
    {
        if (packet.entityId() != npc.getId())
            return;
        factions = packet.factions();
        own = packet.own();
        labels.clear();
        Map<String, Integer> named = new HashMap<>();
        factions.forEach(entry -> named.merge(entry.name().getString(), 1, Integer::sum));
        for (FactionRelationsPacket.Entry entry : factions)
        {
            String name = entry.name().getString();
            labels.put(entry.id(), named.get(name) > 1 ? name + " #" + entry.id() : name);
        }
        chosen.retainAll(labels.keySet());
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll)
    {
        Set<String> selected = new HashSet<>(list.getSelectedList());
        chosen.clear();
        labels.forEach((id, label) ->
        {
            if (selected.contains(label))
                chosen.add(id);
        });
        gui.init();
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll)
    {
        // Clicking toggles a faction; there is nothing to open.
    }
}
