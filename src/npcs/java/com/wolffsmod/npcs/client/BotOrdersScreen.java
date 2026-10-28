package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Orders for the bots of the player's side: the squads around the player, or every squad of the side for the player in
 * its command seat, and the orders they may get. Squads show as a list or on a map of the battlefield, where a click picks
 * a squad, sends it at an objective, or has it hold the place clicked. The server checks every order and sends the
 * screen's state back; the screen asks for it again every two seconds while open.
 */
public final class BotOrdersScreen extends Screen
{
    private static final int ROWS = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_WIDTH = 200;
    private static final int BUTTON_WIDTH = 96;
    private static final int TOP = 40;
    private static final int GAP = 4;
    private static final int REFRESH_TICKS = 40;
    /** Rows of the order column above the objectives: orders, then leading a squad. */
    private static final int LEADING_ROWS = 3;
    private static final BotLeaders.SpawnChoice[] SPAWN_CHOICES = BotLeaders.SpawnChoice.values();
    /** Pixels around a squad or an objective on the map within which a click picks it. */
    private static final int PICK_RADIUS = 6;
    /** Blocks the map shows beyond the farthest squad, objective or player. */
    private static final double MAP_MARGIN = 24D;
    private static final int MAP_BACKGROUND = 0xA0101010;
    private static final int OWN_COLOUR = 0xFF55FF55;
    private static final int ENEMY_COLOUR = 0xFFFF5555;
    private static final int NEUTRAL_COLOUR = 0xFFFFFFFF;
    private static final int SELECTED_COLOUR = 0xFFFFFF55;
    private static final int MINE_COLOUR = 0xFF55FFFF;
    private static final int PLAYER_COLOUR = 0xFFFFFFFF;
    private static final int HELP_COLOUR = 0xA0A0A0;

    private BotOrdersStatePacket state;
    private int selected = BotOrders.NEARBY_SQUADS;
    private int page;
    private boolean map;
    private int ticks;
    /** The corner and the scale of the world area the map shows, fitted again to every new state. */
    private double minX;
    private double minZ;
    private double scale = 1D;

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
    public void tick()
    {
        if (++ticks % REFRESH_TICKS == 0)
            send(BotOrderPacket.refresh());
    }

    @Override
    protected void init()
    {
        int left = left();
        int right = width / 2 + GAP;
        if (!state.playing())
        {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(width / 2 - BUTTON_WIDTH / 2, height - 30, BUTTON_WIDTH, 20).build());
            return;
        }
        if (map)
            fitMap();
        else
            addSquadList(left);
        addOrders(right);
        Component view = Component.translatable(map ? "wolffsmodnpcs.orders.list" : "wolffsmodnpcs.orders.map");
        addRenderableWidget(Button.builder(view, button ->
        {
            map = !map;
            rebuildWidgets();
        }).bounds(left, height - 30, LIST_WIDTH, 20).build());
        Component seat = Component.translatable(state.commander() ? "wolffsmodnpcs.orders.leave_command" : "wolffsmodnpcs.orders.take_command");
        BotOrderPacket.Action action = state.commander() ? BotOrderPacket.Action.LEAVE_COMMAND : BotOrderPacket.Action.TAKE_COMMAND;
        addRenderableWidget(Button.builder(seat, button -> send(new BotOrderPacket(action, BotOrders.NEARBY_SQUADS, PlayerOrder.Kind.RELEASE, BotOrders.HERE, null)))
            .bounds(right, height - 54, BUTTON_WIDTH * 2 + GAP, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.orders.refresh"), button -> send(BotOrderPacket.refresh())).bounds(right, height - 30, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(right + BUTTON_WIDTH + GAP, height - 30, BUTTON_WIDTH, 20).build());
    }

    private int left()
    {
        return Math.max(GAP, width / 2 - LIST_WIDTH - GAP);
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
        addLeading(right);
        List<BotOrdersStatePacket.Objective> objectives = state.objectives();
        for (int i = 0; i < objectives.size(); i++)
        {
            BotOrdersStatePacket.Objective objective = objectives.get(i);
            addOrder(right, 0, i + LEADING_ROWS, PlayerOrder.Kind.ATTACK, i, Component.translatable("wolffsmodnpcs.orders.attack", String.valueOf(objective.symbol())));
            addOrder(right, 1, i + LEADING_ROWS, PlayerOrder.Kind.DEFEND, i, Component.translatable("wolffsmodnpcs.orders.defend", String.valueOf(objective.symbol())));
        }
    }

    /**
     * Leading a squad: taking the lead of the squad chosen, or of the nearest one, or giving it up; placing its rally
     * point; and choosing where to come back after dying.
     */
    private void addLeading(int right)
    {
        BotOrdersStatePacket.Leading leading = state.leading();
        boolean leads = leading.squad() >= 0;
        Component lead = leads ? Component.translatable("wolffsmodnpcs.orders.leave_squad", "#" + leading.squad()) : Component.translatable("wolffsmodnpcs.orders.lead");
        addRenderableWidget(
            Button
                .builder(lead,
                    button -> send(
                        leads ? BotOrderPacket.of(BotOrderPacket.Action.LEAVE_SQUAD) : new BotOrderPacket(BotOrderPacket.Action.LEAD, selected, PlayerOrder.Kind.RELEASE, BotOrders.HERE, null)))
                .bounds(right + BUTTON_WIDTH + GAP, TOP + ROW_HEIGHT, BUTTON_WIDTH, 20).build());
        Component rally = Component.translatable(leading.rally() ? "wolffsmodnpcs.orders.move_rally" : "wolffsmodnpcs.orders.rally");
        addRenderableWidget(Button.builder(rally, button -> send(BotOrderPacket.of(BotOrderPacket.Action.RALLY))).bounds(right, TOP + 2 * ROW_HEIGHT, BUTTON_WIDTH, 20).build()).active = leads
            && leading.rallyPoints();
        int next = (leading.spawnChoice() + 1) % SPAWN_CHOICES.length;
        Component spawn = Component.translatable("wolffsmodnpcs.orders.spawn",
            Component.translatable("wolffsmodnpcs.orders.spawn." + SPAWN_CHOICES[leading.spawnChoice() % SPAWN_CHOICES.length].name().toLowerCase(Locale.ROOT)));
        addRenderableWidget(Button.builder(spawn, button -> send(new BotOrderPacket(BotOrderPacket.Action.SPAWN_CHOICE, selected, PlayerOrder.Kind.RELEASE, next, null)))
            .bounds(right + BUTTON_WIDTH + GAP, TOP + 2 * ROW_HEIGHT, BUTTON_WIDTH, 20).build()).active = leads;
    }

    private void addOrder(int right, int column, int row, PlayerOrder.Kind kind, int objective, Component label)
    {
        addRenderableWidget(Button.builder(label, button -> send(new BotOrderPacket(BotOrderPacket.Action.ORDER, selected, kind, objective, null)))
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

    // --- Map ---

    private int mapSize()
    {
        return Math.max(1, Math.min(LIST_WIDTH, height - TOP - 50));
    }

    /** Fits the squads, the objectives and the player into the square map, with a margin round them. */
    private void fitMap()
    {
        double lowX = Double.POSITIVE_INFINITY;
        double lowZ = Double.POSITIVE_INFINITY;
        double highX = Double.NEGATIVE_INFINITY;
        double highZ = Double.NEGATIVE_INFINITY;
        for (Vec3 point : mapPoints())
        {
            lowX = Math.min(lowX, point.x);
            lowZ = Math.min(lowZ, point.z);
            highX = Math.max(highX, point.x);
            highZ = Math.max(highZ, point.z);
        }
        if (lowX > highX)
        {
            lowX = 0D;
            lowZ = 0D;
            highX = 0D;
            highZ = 0D;
        }
        double span = Math.max(highX - lowX, highZ - lowZ) + MAP_MARGIN * 2D;
        minX = (lowX + highX - span) / 2D;
        minZ = (lowZ + highZ - span) / 2D;
        scale = mapSize() / span;
    }

    private List<Vec3> mapPoints()
    {
        List<Vec3> points = new ArrayList<>();
        state.squads().forEach(squad -> points.add(new Vec3(squad.x(), 0D, squad.z())));
        state.objectives().forEach(objective -> points.add(new Vec3(objective.x(), 0D, objective.z())));
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
            points.add(player.position());
        return points;
    }

    private int screenX(double x)
    {
        return left() + (int) ((x - minX) * scale);
    }

    private int screenY(double z)
    {
        return TOP + (int) ((z - minZ) * scale);
    }

    private boolean onMap(double mouseX, double mouseY)
    {
        int size = mapSize();
        return map && state.playing() && mouseX >= left() && mouseX < left() + size && mouseY >= TOP && mouseY < TOP + size;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;
        if (!onMap(mouseX, mouseY))
            return false;
        BotOrdersStatePacket.Squad squad = squadAt(mouseX, mouseY);
        if (squad != null)
        {
            selected = squad.id();
            return true;
        }
        int objective = objectiveAt(mouseX, mouseY);
        if (objective >= 0)
        {
            PlayerOrder.Kind kind = state.objectives().get(objective).ownerSide() == state.side() ? PlayerOrder.Kind.DEFEND : PlayerOrder.Kind.ATTACK;
            send(new BotOrderPacket(BotOrderPacket.Action.ORDER, selected, kind, objective, null));
            return true;
        }
        Vec3 ground = new Vec3(minX + (mouseX - left()) / scale, 0D, minZ + (mouseY - TOP) / scale);
        send(new BotOrderPacket(BotOrderPacket.Action.ORDER, selected, PlayerOrder.Kind.HOLD, BotOrders.HERE, ground));
        return true;
    }

    @Nullable
    private BotOrdersStatePacket.Squad squadAt(double mouseX, double mouseY)
    {
        return state.squads().stream().filter(squad -> near(mouseX, mouseY, squad.x(), squad.z())).findFirst().orElse(null);
    }

    /** The objective under the mouse, by play order, or -1. */
    private int objectiveAt(double mouseX, double mouseY)
    {
        List<BotOrdersStatePacket.Objective> objectives = state.objectives();
        for (int i = 0; i < objectives.size(); i++)
            if (near(mouseX, mouseY, objectives.get(i).x(), objectives.get(i).z()))
                return i;
        return -1;
    }

    private boolean near(double mouseX, double mouseY, double x, double z)
    {
        return Math.abs(mouseX - screenX(x)) <= PICK_RADIUS && Math.abs(mouseY - screenY(z)) <= PICK_RADIUS;
    }

    private void renderMap(GuiGraphics graphics)
    {
        int size = mapSize();
        graphics.fill(left(), TOP, left() + size, TOP + size, MAP_BACKGROUND);
        for (BotOrdersStatePacket.Objective objective : state.objectives())
            graphics.drawCenteredString(font, String.valueOf(objective.symbol()), screenX(objective.x()), screenY(objective.z()) - 4, ownerColour(objective.ownerSide()));
        for (BotOrdersStatePacket.Squad squad : state.squads())
        {
            int x = screenX(squad.x());
            int y = screenY(squad.z());
            graphics.fill(x - 2, y - 2, x + 3, y + 3, squadColour(squad));
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
        {
            int x = screenX(player.getX());
            int y = screenY(player.getZ());
            graphics.fill(x - 1, y - 1, x + 2, y + 2, PLAYER_COLOUR);
        }
        graphics.drawString(font, Component.translatable("wolffsmodnpcs.orders.map_help"), left(), TOP + size + 2, HELP_COLOUR);
    }

    private int ownerColour(int ownerSide)
    {
        if (ownerSide < 0)
            return NEUTRAL_COLOUR;
        return ownerSide == state.side() ? OWN_COLOUR : ENEMY_COLOUR;
    }

    private int squadColour(BotOrdersStatePacket.Squad squad)
    {
        if (squad.id() == selected)
            return SELECTED_COLOUR;
        return squad.mine() ? MINE_COLOUR : OWN_COLOUR;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        if (map && state.playing())
            renderMap(graphics);
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
