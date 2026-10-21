package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.BotOrderPacket;
import com.wolffsmod.npcs.network.BotOrdersStatePacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.teams.BotOrders;
import com.wolffsmod.npcs.teams.PlayerOrder;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Orders for the bots of the player's side: the squads around the player, or every squad of the side for the player in
 * its command seat, and the orders they may get. The server checks every order and sends the screen's state back.
 */
public final class BotOrdersScreen extends Screen
{
    private static final int ROWS = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_WIDTH = 200;
    private static final int BUTTON_WIDTH = 96;
    private static final int TOP = 40;
    private static final int GAP = 4;

    private BotOrdersStatePacket state;
    private int selected = BotOrders.NEARBY_SQUADS;
    private int page;

    public BotOrdersScreen(BotOrdersStatePacket state)
    {
        super(Component.translatable("wolffsmodnpcs.orders.title"));
        this.state = state;
    }

    /** Shows new state from the server, keeping the squad chosen while it is still listed. */
    public void update(BotOrdersStatePacket update)
    {
        state = update;
        if (state.squads().stream().noneMatch(squad -> squad.id() == selected))
            selected = BotOrders.NEARBY_SQUADS;
        rebuildWidgets();
    }

    @Override
    protected void init()
    {
        int left = Math.max(GAP, width / 2 - LIST_WIDTH - GAP);
        int right = width / 2 + GAP;
        if (!state.playing())
        {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(width / 2 - BUTTON_WIDTH / 2, height - 30, BUTTON_WIDTH, 20).build());
            return;
        }
        addSquadList(left);
        addOrders(right);
        Component seat = Component.translatable(state.commander() ? "wolffsmodnpcs.orders.leave_command" : "wolffsmodnpcs.orders.take_command");
        BotOrderPacket.Action action = state.commander() ? BotOrderPacket.Action.LEAVE_COMMAND : BotOrderPacket.Action.TAKE_COMMAND;
        addRenderableWidget(Button.builder(seat, button -> send(new BotOrderPacket(action, BotOrders.NEARBY_SQUADS, PlayerOrder.Kind.RELEASE, BotOrders.HERE)))
            .bounds(right, height - 54, BUTTON_WIDTH * 2 + GAP, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.orders.refresh"), button -> send(BotOrderPacket.refresh())).bounds(right, height - 30, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(right + BUTTON_WIDTH + GAP, height - 30, BUTTON_WIDTH, 20).build());
    }

    /** The squads the player may order, a page at a time, below the choice of every squad around the player. */
    private void addSquadList(int left)
    {
        addRow(left, 0, BotOrders.NEARBY_SQUADS, Component.translatable("wolffsmodnpcs.orders.nearby"));
        List<BotOrdersStatePacket.Squad> squads = state.squads();
        int pages = Math.max(1, (squads.size() + ROWS - 1) / ROWS);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < ROWS && page * ROWS + i < squads.size(); i++)
        {
            BotOrdersStatePacket.Squad squad = squads.get(page * ROWS + i);
            String mine = squad.mine() ? "* " : "";
            addRow(left, i + 1, squad.id(), Component.literal(mine + squad.name() + " x" + squad.strength() + " - " + squad.task() + " (" + squad.distance() + "m)"));
        }
        int below = TOP + (ROWS + 1) * ROW_HEIGHT;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1)).bounds(left, below, 20, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1)).bounds(left + LIST_WIDTH - 20, below, 20, 20).build()).active = page < pages - 1;
    }

    private void addRow(int left, int row, int squadId, Component label)
    {
        Component shown = squadId == selected ? Component.literal("> ").append(label) : label;
        addRenderableWidget(Button.builder(shown, button ->
        {
            selected = squadId;
            rebuildWidgets();
        }).bounds(left, TOP + row * ROW_HEIGHT, LIST_WIDTH, 20).build());
    }

    /** The orders: follow, hold and release, then attacking or defending each capture point of the round. */
    private void addOrders(int right)
    {
        addOrder(right, 0, 0, PlayerOrder.Kind.FOLLOW, BotOrders.HERE, Component.translatable("wolffsmodnpcs.orders.follow"));
        addOrder(right, 1, 0, PlayerOrder.Kind.HOLD, BotOrders.HERE, Component.translatable("wolffsmodnpcs.orders.hold"));
        addOrder(right, 0, 1, PlayerOrder.Kind.RELEASE, BotOrders.HERE, Component.translatable("wolffsmodnpcs.orders.release"));
        List<BotOrdersStatePacket.Objective> objectives = state.objectives();
        for (int i = 0; i < objectives.size(); i++)
        {
            BotOrdersStatePacket.Objective objective = objectives.get(i);
            addOrder(right, 0, i + 2, PlayerOrder.Kind.ATTACK, i, Component.translatable("wolffsmodnpcs.orders.attack", String.valueOf(objective.symbol())));
            addOrder(right, 1, i + 2, PlayerOrder.Kind.DEFEND, i, Component.translatable("wolffsmodnpcs.orders.defend", String.valueOf(objective.symbol())));
        }
    }

    private void addOrder(int right, int column, int row, PlayerOrder.Kind kind, int objective, Component label)
    {
        addRenderableWidget(Button.builder(label, button -> send(new BotOrderPacket(BotOrderPacket.Action.ORDER, selected, kind, objective)))
            .bounds(right + column * (BUTTON_WIDTH + GAP), TOP + row * ROW_HEIGHT, BUTTON_WIDTH, 20).build());
    }

    private void turn(int pages)
    {
        page = Math.max(0, page + pages);
        rebuildWidgets();
    }

    private static void send(BotOrderPacket packet)
    {
        NpcNetwork.sendToServer(packet);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component status;
        if (!state.playing())
            status = Component.translatable("wolffsmodnpcs.orders.not_playing");
        else if (state.commander())
            status = Component.translatable("wolffsmodnpcs.orders.you_command");
        else if (state.commanderName().isEmpty())
            status = Component.translatable("wolffsmodnpcs.orders.no_commander");
        else
            status = Component.translatable("wolffsmodnpcs.orders.commander", state.commanderName());
        graphics.drawCenteredString(font, status, width / 2, 24, 0xC0C0C0);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
