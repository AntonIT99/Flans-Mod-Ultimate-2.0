package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.render.hud.TeamsObjectiveHud;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.teams.*;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.network.server.teams.*;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.mojang.math.Axis;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The deployment map of Conquest and Assault, ported from the Labjac fork. It takes the place of
 * the death screen: the dead pick headquarters or a capture point their team holds and come back
 * there once the deployment delay has run out.
 *
 * <p>
 * Operators can switch to an edit mode in which the spawn choices are dragged to where they
 * belong on the map image and the image is cropped by its corner handles; leaving the mode saves
 * the layout with the map.
 * </p>
 */
public final class TeamsDeployScreen extends Screen
{
    private static final int NODE_SIZE = 17;
    private static final int HANDLE_SIZE = 6;
    private static final int BUTTON_HEIGHT = 13;
    private static final int EDIT_BUTTON_WIDTH = 34;
    private static final int CLASS_BUTTON_WIDTH = 42;
    private static final int MARGIN = 6;
    private static final int MAP_LEFT = 8;
    private static final int MAP_TOP = 30;
    /** Virtual size the image is addressed in, so a crop given in fractions maps to whole texels. */
    private static final int IMAGE_UNITS = 1000;
    private static final int STATUS_READY = 0xA8FFA8;
    private static final int STATUS_WAITING = 0xC8C8C8;
    private static final int LINE_COLOUR = 0xAA1C1C1C;
    /** How long a chosen spawn waits for the server before the choice is offered again. */
    private static final long DEPLOY_TIMEOUT_MILLIS = 5000L;
    /**
     * When the death being waited out happened. Kept outside the screen because the class menu
     * replaces it and brings back a new one, which must not start the deployment delay over.
     */
    private static long deathMillis;

    private enum Status
    {
        READY("gui.flansmod.teams.deploy.ready", STATUS_READY), STAND_BY("gui.flansmod.teams.deploy.stand_by", STATUS_WAITING), UNAVAILABLE("gui.flansmod.teams.deploy.unavailable",
            STATUS_WAITING), NO_SPAWN("gui.flansmod.teams.deploy.no_spawn", STATUS_WAITING), UNDER_ATTACK("gui.flansmod.teams.deploy.under_attack", 0xFF9C9C), FRONTLINE(
                "gui.flansmod.teams.deploy.frontline",
                0xFFE08A), RETAKE("gui.flansmod.teams.deploy.retake", 0xFFFFFF), LOCKED("gui.flansmod.teams.deploy.locked", 0x8A8A8A), MOVE("gui.flansmod.teams.deploy.move", STATUS_READY);

        private final String key;
        private final int colour;

        Status(String key, int colour)
        {
            this.key = key;
            this.colour = colour;
        }
    }

    /** One spawn choice on the map: headquarters when {@code objective} is null. */
    private static final class Node
    {
        private final String key;
        private final String label;
        private final Component name;
        @Nullable
        private ObjectiveSnapshot objective;
        private int x;
        private int y;
        private boolean enabled;
        private Status status = Status.STAND_BY;

        private Node(String key, String label, Component name, @Nullable ObjectiveSnapshot objective)
        {
            this.key = key;
            this.label = label;
            this.name = name;
            this.objective = objective;
        }

        private boolean contains(double mouseX, double mouseY)
        {
            int half = NODE_SIZE / 2 + 2;
            return mouseX >= x - half && mouseX <= x + half && mouseY >= y - half && mouseY <= y + half;
        }
    }

    /** Opened from a command rather than by dying, so it can be closed and choosing does not respawn. */
    @Getter
    private final boolean manuallyOpened;
    private final long openedMillis;
    private long deployedMillis;
    private final List<Node> nodes = new ArrayList<>();
    private String layoutSignature = "";
    private boolean editMode;
    private boolean deployed;
    @Nullable
    private Node draggingNode;
    private int draggingHandle;
    private TeamsMap.ImageCrop crop = TeamsMap.ImageCrop.FULL;
    @Nullable
    private String checkedImage;
    @Nullable
    private ResourceLocation imageTexture;

    public TeamsDeployScreen()
    {
        this(false);
    }

    public TeamsDeployScreen(boolean manuallyOpened)
    {
        super(Component.translatable("gui.flansmod.teams.deploy.title"));
        this.manuallyOpened = manuallyOpened;
        if (!manuallyOpened && deathMillis == 0L)
            startDeath();
        openedMillis = manuallyOpened ? System.currentTimeMillis() : deathMillis;
    }

    private static void startDeath()
    {
        deathMillis = System.currentTimeMillis();
    }

    /** Forgets the death the delay was counted from, once the player is back or has left the server. */
    public static void resetDeath()
    {
        deathMillis = 0L;
    }

    @Override
    protected void init()
    {
        layoutSignature = "";
        refresh();
    }

    @Override
    public void tick()
    {
        PacketTeamsState state = TeamsClientState.get();
        boolean alive = minecraft != null && minecraft.player != null && minecraft.player.isAlive();
        // The round ended, or the player is back in the world: there is nothing left to choose.
        if (alive)
            resetDeath();
        if (state == null || state.getObjectiveMode() == ObjectiveMode.NONE || !manuallyOpened && alive)
        {
            if (minecraft != null)
                minecraft.setScreen(null);
            return;
        }
        if (deployed && System.currentTimeMillis() - deployedMillis > DEPLOY_TIMEOUT_MILLIS)
            deployed = false;
        refresh();
    }

    // --- Layout ---

    private int mapWidth()
    {
        return width - 16;
    }

    private int mapHeight()
    {
        return height - 36;
    }

    /** Rebuilds the spawn choices when the points or the saved layout changed, and updates what each offers. */
    private void refresh()
    {
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
            return;
        List<ObjectiveSnapshot> objectives = TeamsClientState.objectives();
        StringBuilder signature = new StringBuilder().append(editMode).append(state.getPlayerTeamId()).append(state.getSideIds()).append(width).append('x').append(height);
        objectives.forEach(objective -> signature.append(objective.id()).append(objective.symbol()).append(objective.name()));
        state.getDeployLayout().forEach(entry -> signature.append(entry.key()).append(entry.x()).append(entry.y()));
        signature.append(state.getDeployImageCrop());
        if (!signature.toString().equals(layoutSignature) && draggingNode == null)
        {
            layoutSignature = signature.toString();
            if (!editMode)
                crop = state.getDeployImageCrop();
            rebuildNodes(state, objectives);
        }
        updateNodeStates(state, objectives);
    }

    private void rebuildNodes(PacketTeamsState state, List<ObjectiveSnapshot> objectives)
    {
        nodes.clear();
        int count = objectives.size() + 1;
        int usableWidth = Math.max(80, mapWidth() - 84);
        int gap = count <= 1 ? 0 : usableWidth / (count - 1);
        int startX = MAP_LEFT + mapWidth() / 2 - usableWidth / 2;
        int y = MAP_TOP + mapHeight() / 2;

        if (editMode)
        {
            // Every headquarters is placed while editing; in play each side only sees its own.
            List<Integer> sideIds = new ArrayList<>(List.of(state.getAttackingTeamId(), state.getDefendingTeamId()));
            state.getSideIds().stream().filter(sideId -> !sideIds.contains(sideId)).forEach(sideIds::add);
            for (int i = 0; i < sideIds.size(); i++)
                addNode(state, new Node(DeployLayoutEntry.HQ_PREFIX + sideIds.get(i), "HQ" + (i + 1), teamName(state, sideIds.get(i)), null), startX, y - 16 * (sideIds.size() - 1) + 32 * i);
        }
        else
        {
            // Allied teams share one headquarters marker; which base they come back at is up to the server.
            int sideId = state.getPlayerSideId() < 2 ? state.getAttackingTeamId() : state.getPlayerSideId();
            addNode(state, new Node(DeployLayoutEntry.HQ_PREFIX + sideId, "HQ", Component.translatable("gui.flansmod.teams.deploy.headquarters"), null), startX, y);
        }
        for (int i = 0; i < objectives.size(); i++)
        {
            ObjectiveSnapshot objective = objectives.get(i);
            addNode(state, new Node(DeployLayoutEntry.OBJECTIVE_PREFIX + objective.id(), String.valueOf(objective.symbol()), Component.literal(objective.name()), objective), startX + (i + 1) * gap,
                y);
        }
    }

    private static Component teamName(PacketTeamsState state, int teamId)
    {
        PacketTeamsState.TeamScore team = state.getTeamScore(teamId);
        return team == null ? Component.translatable("gui.flansmod.teams.deploy.headquarters") : Component.literal(state.getSideName(team.sideId()));
    }

    /** Puts a choice where the saved layout has it, or on the default row for one that was never placed. */
    private void addNode(PacketTeamsState state, Node node, int defaultX, int defaultY)
    {
        DeployLayoutEntry saved = state.getDeployLayout().stream().filter(entry -> entry.key().equals(node.key)).findFirst().orElse(null);
        int x = saved == null ? defaultX : MAP_LEFT + Math.round(saved.x() * mapWidth());
        int y = saved == null ? defaultY : MAP_TOP + Math.round(saved.y() * mapHeight());
        node.x = Mth.clamp(x, MAP_LEFT + 8, MAP_LEFT + mapWidth() - 8);
        node.y = Mth.clamp(y, MAP_TOP + 8, MAP_TOP + mapHeight() - 8);
        nodes.add(node);
    }

    private void updateNodeStates(PacketTeamsState state, List<ObjectiveSnapshot> objectives)
    {
        boolean ready = cooldownSeconds(state) <= 0;
        boolean assault = state.getObjectiveMode() == ObjectiveMode.ASSAULT;
        ObjectiveSnapshot frontline = assault ? TeamsObjectiveHud.frontline(objectives, state.getAttackingTeamId()) : null;
        ObjectiveSnapshot assaultSpawn = assault ? assaultSpawn(state, objectives) : null;
        for (Node node : nodes)
        {
            ObjectiveSnapshot known = node.objective;
            if (known != null)
                node.objective = objectives.stream().filter(candidate -> candidate.id().equals(known.id())).findFirst().orElse(known);
            ObjectiveSnapshot objective = node.objective;
            if (editMode)
                setState(node, true, Status.MOVE);
            else if (objective == null)
                setState(node, ready, ready ? Status.READY : Status.STAND_BY);
            else if (!ready)
                setState(node, false, Status.STAND_BY);
            else if (assault)
                updateAssaultNode(node, objective, frontline, assaultSpawn);
            else
                updateConquestNode(node, objective, state.getPlayerSideId());
        }
    }

    private static void setState(Node node, boolean enabled, Status status)
    {
        node.enabled = enabled;
        node.status = status;
    }

    private static void updateConquestNode(Node node, ObjectiveSnapshot objective, int teamId)
    {
        if (objective.ownerTeamId() != teamId)
            setState(node, false, Status.UNAVAILABLE);
        else if (!objective.spawnPoint())
            setState(node, false, Status.NO_SPAWN);
        else
            setState(node, true, objective.contested() || objective.progress() < 1F ? Status.UNDER_ATTACK : Status.READY);
    }

    private static void updateAssaultNode(Node node, ObjectiveSnapshot objective, @Nullable ObjectiveSnapshot frontline, @Nullable ObjectiveSnapshot spawn)
    {
        boolean isFrontline = frontline != null && frontline.id().equals(objective.id());
        if (spawn != null && spawn.id().equals(objective.id()))
            setState(node, true, Status.READY);
        else if (isFrontline)
            setState(node, false, objective.ownerTeamId() == 0 ? Status.RETAKE : Status.FRONTLINE);
        else
            setState(node, false, Status.LOCKED);
    }

    /** The objective an Assault side comes back at: the nearest one it holds on its side of the front line. */
    @Nullable
    private static ObjectiveSnapshot assaultSpawn(PacketTeamsState state, List<ObjectiveSnapshot> objectives)
    {
        int teamId = state.getPlayerSideId();
        int active = 0;
        while (active < objectives.size() && objectives.get(active).state().isHeldBy(state.getAttackingTeamId()))
            active++;
        if (teamId == state.getDefendingTeamId())
        {
            for (int i = active; i < objectives.size(); i++)
                if (objectives.get(i).ownerTeamId() == teamId)
                    return objectives.get(i);
        }
        else if (teamId == state.getAttackingTeamId())
        {
            for (int i = Math.min(active - 1, objectives.size() - 1); i >= 0; i--)
                if (objectives.get(i).ownerTeamId() == teamId)
                    return objectives.get(i);
        }
        return null;
    }

    private int cooldownSeconds(PacketTeamsState state)
    {
        if (manuallyOpened)
            return 0;
        long remaining = openedMillis + state.getDeployDelaySeconds() * 1000L - System.currentTimeMillis();
        return remaining <= 0L ? 0 : (int) ((remaining + 999L) / 1000L);
    }

    // --- Rendering ---

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        PacketTeamsState state = TeamsClientState.get();
        if (state == null)
            return;
        graphics.fill(0, 0, width, height, 0xEE050505);
        renderMap(graphics, state);

        int cooldown = cooldownSeconds(state);
        Component heading = title;
        if (deployed)
            heading = Component.translatable("gui.flansmod.teams.deploy.deploying");
        else if (cooldown > 0)
            heading = Component.translatable("gui.flansmod.teams.deploy.countdown", cooldown);
        graphics.drawCenteredString(font, heading, width / 2, 6, cooldown > 0 ? 0xFFE08A : 0xFFFFFF);
        graphics.drawCenteredString(font, state.getMapName() + " - " + state.getGameType(), width / 2, 18, 0xC8C8C8);

        Node previous = null;
        for (Node node : nodes)
        {
            if (node.objective == null)
                continue;
            if (previous != null)
                drawLine(graphics, previous.x, previous.y, node.x, node.y);
            previous = node;
        }
        for (Node node : nodes)
            renderNode(graphics, node, mouseX, mouseY);
        if (editMode)
            renderCropHandles(graphics);

        if (!editMode)
            renderButton(graphics, Component.translatable("gui.flansmod.teams.deploy.class"), MARGIN, CLASS_BUTTON_WIDTH, mouseX, mouseY);
        if (state.canEditDeployLayout())
            renderButton(graphics, Component.translatable(editMode ? "gui.flansmod.teams.deploy.save" : "gui.flansmod.teams.deploy.edit"), width - MARGIN - EDIT_BUTTON_WIDTH, EDIT_BUTTON_WIDTH,
                mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMap(GuiGraphics graphics, PacketTeamsState state)
    {
        int left = MAP_LEFT;
        int top = MAP_TOP;
        int right = left + mapWidth();
        int bottom = top + mapHeight();
        graphics.fill(left - 4, top - 4, right + 4, bottom + 4, 0xE6101010);
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xD2B8B8B8);
        ResourceLocation image = imageTexture(state.getDeployImage());
        if (image == null)
        {
            // No image set for the map, or none of that name in any resource pack: a plain chart.
            graphics.fill(left, top, right, bottom, 0xFF23302A);
            for (int x = left + 24; x < right; x += 24)
                graphics.fill(x, top, x + 1, bottom, 0x30FFFFFF);
            for (int y = top + 24; y < bottom; y += 24)
                graphics.fill(left, y, right, y + 1, 0x30FFFFFF);
            return;
        }
        int u = Math.round(crop.minU() * IMAGE_UNITS);
        int v = Math.round(crop.minV() * IMAGE_UNITS);
        int uWidth = Math.max(1, Math.round(crop.maxU() * IMAGE_UNITS) - u);
        int vHeight = Math.max(1, Math.round(crop.maxV() * IMAGE_UNITS) - v);
        graphics.blit(image, left, top, mapWidth(), mapHeight(), u, v, uWidth, vHeight, IMAGE_UNITS, IMAGE_UNITS);
        graphics.fill(left, top, right, bottom, 0x0C000000);
    }

    /** The texture of a deployment image, or null when the map has none or no resource pack provides it. */
    @Nullable
    private ResourceLocation imageTexture(String image)
    {
        if (!image.equals(checkedImage) && minecraft != null)
        {
            checkedImage = image;
            ResourceLocation location = image.isBlank() ? null : ResourceLocation.tryBuild(FlansMod.FLANSMOD_ID, "textures/gui/deploy/" + image + ".png");
            imageTexture = location != null && minecraft.getResourceManager().getResource(location).isPresent() ? location : null;
        }
        return imageTexture;
    }

    private void renderNode(GuiGraphics graphics, Node node, int mouseX, int mouseY)
    {
        boolean hover = node == draggingNode || node.enabled && node.contains(mouseX, mouseY);
        int half = NODE_SIZE / 2;
        int fill = 0x4A4A4A;
        if (node.enabled && node.objective == null)
            fill = 0x5E84C4;
        else if (node.enabled)
            fill = node.status == Status.UNDER_ATTACK ? 0xB88A32 : 0x4F9D5D;
        else if (node.status == Status.RETAKE)
            fill = 0xDCDCDC;
        int alpha = node.enabled ? 0xD2000000 : 0xA5000000;
        int text = node.enabled ? 0xFFFFFF : 0xB0B0B0;
        graphics.fill(node.x - half - 2, node.y - half - 2, node.x + half + 2, node.y + half + 2, hover ? 0xFFEFEFEF : 0xCD111111);
        graphics.fill(node.x - half, node.y - half, node.x + half, node.y + half, alpha | fill);
        drawScaled(graphics, Component.literal(node.label), node.x, node.y - 4, 0.78F, text);
        drawScaled(graphics, node.name, node.x, node.y + half + 4, 0.7F, text);
        drawScaled(graphics, Component.translatable(node.status.key), node.x, node.y + half + 13, 0.66F, node.status.colour);
    }

    private void renderCropHandles(GuiGraphics graphics)
    {
        int left = MAP_LEFT + Math.round(crop.minU() * mapWidth());
        int top = MAP_TOP + Math.round(crop.minV() * mapHeight());
        int right = MAP_LEFT + Math.round(crop.maxU() * mapWidth());
        int bottom = MAP_TOP + Math.round(crop.maxV() * mapHeight());
        graphics.renderOutline(left, top, right - left, bottom - top, 0xDCF0F0F0);
        int[][] corners = {{left, top}, {right, top}, {left, bottom}, {right, bottom}};
        for (int i = 0; i < corners.length; i++)
        {
            int half = HANDLE_SIZE / 2;
            boolean active = draggingHandle == i + 1;
            graphics.fill(corners[i][0] - half - 1, corners[i][1] - half - 1, corners[i][0] + half + 1, corners[i][1] + half + 1, active ? 0xE6FFFFFF : 0xE6111111);
            graphics.fill(corners[i][0] - half, corners[i][1] - half, corners[i][0] + half, corners[i][1] + half, active ? 0xE66C8BC6 : 0xE6E6E6E6);
        }
    }

    private void renderButton(GuiGraphics graphics, Component label, int x, int buttonWidth, int mouseX, int mouseY)
    {
        boolean hover = inButton(mouseX, mouseY, x, buttonWidth);
        graphics.fill(x, MARGIN, x + buttonWidth, MARGIN + BUTTON_HEIGHT, hover ? 0xE0606060 : 0xE0303030);
        graphics.renderOutline(x, MARGIN, buttonWidth, BUTTON_HEIGHT, 0xFFB8B8B8);
        drawScaled(graphics, label, x + buttonWidth / 2, MARGIN + 3, 0.8F, 0xFFFFFF);
    }

    private static boolean inButton(double mouseX, double mouseY, int x, int buttonWidth)
    {
        return mouseX >= x && mouseX <= x + buttonWidth && mouseY >= MARGIN && mouseY <= MARGIN + BUTTON_HEIGHT;
    }

    private void drawScaled(GuiGraphics graphics, Component text, int centreX, int y, float scale, int colour)
    {
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, y, 0F);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, text, -font.width(text) / 2, 0, colour, true);
        graphics.pose().popPose();
    }

    /** Joins two capture points on the map, in the order they are fought over. */
    private static void drawLine(GuiGraphics graphics, int fromX, int fromY, int toX, int toY)
    {
        graphics.pose().pushPose();
        graphics.pose().translate(fromX, fromY, 0F);
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2((double) toY - fromY, (double) toX - fromX)));
        graphics.fill(0, -2, (int) Math.round(Math.hypot((double) toX - fromX, (double) toY - fromY)), 2, LINE_COLOUR);
        graphics.pose().popPose();
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        PacketTeamsState state = TeamsClientState.get();
        if (button != 0 || state == null || deployed)
            return super.mouseClicked(mouseX, mouseY, button);
        if (state.canEditDeployLayout() && inButton(mouseX, mouseY, width - MARGIN - EDIT_BUTTON_WIDTH, EDIT_BUTTON_WIDTH))
        {
            toggleEditMode(state);
            return true;
        }
        if (editMode)
        {
            draggingHandle = handleAt(mouseX, mouseY);
            draggingNode = draggingHandle == 0 ? nodeAt(mouseX, mouseY) : null;
            return true;
        }
        if (inButton(mouseX, mouseY, MARGIN, CLASS_BUTTON_WIDTH))
        {
            PacketHandler.sendToServer(PacketTeamsAction.openClassMenu());
            return true;
        }
        Node clicked = nodeAt(mouseX, mouseY);
        if (clicked != null && clicked.enabled && cooldownSeconds(state) <= 0)
        {
            deployAt(clicked);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void deployAt(Node node)
    {
        PacketHandler.sendToServer(new PacketDeploySpawnSelect(node.objective == null ? null : node.objective.id()));
        if (minecraft == null)
            return;
        if (manuallyOpened || minecraft.player == null)
        {
            minecraft.setScreen(null);
            return;
        }
        // The choice travels ahead of the respawn request, so the server knows it when it places the player.
        deployed = true;
        deployedMillis = System.currentTimeMillis();
        minecraft.player.respawn();
    }

    private void toggleEditMode(PacketTeamsState state)
    {
        if (editMode)
        {
            List<DeployLayoutEntry> entries = nodes.stream()
                .map(node -> new DeployLayoutEntry(node.key, (node.x - MAP_LEFT) / (float) Math.max(1, mapWidth()), (node.y - MAP_TOP) / (float) Math.max(1, mapHeight()))).toList();
            PacketHandler.sendToServer(new PacketDeployLayoutSave(state.getMapId(), crop, entries));
        }
        editMode = !editMode;
        draggingNode = null;
        draggingHandle = 0;
        layoutSignature = "";
        refresh();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (!editMode || button != 0)
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (draggingHandle != 0)
            dragCropHandle(mouseX, mouseY);
        else if (draggingNode != null)
        {
            draggingNode.x = Mth.clamp((int) mouseX, MAP_LEFT + 8, MAP_LEFT + mapWidth() - 8);
            draggingNode.y = Mth.clamp((int) mouseY, MAP_TOP + 8, MAP_TOP + mapHeight() - 8);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        draggingNode = null;
        draggingHandle = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Nullable
    private Node nodeAt(double mouseX, double mouseY)
    {
        return nodes.stream().filter(node -> node.contains(mouseX, mouseY)).findFirst().orElse(null);
    }

    /** The crop corner under the mouse: 1 top left, 2 top right, 3 bottom left, 4 bottom right, 0 for none. */
    private int handleAt(double mouseX, double mouseY)
    {
        float u = (float) (mouseX - MAP_LEFT) / Math.max(1, mapWidth());
        float v = (float) (mouseY - MAP_TOP) / Math.max(1, mapHeight());
        float rangeU = (HANDLE_SIZE + 3F) / Math.max(1, mapWidth());
        float rangeV = (HANDLE_SIZE + 3F) / Math.max(1, mapHeight());
        boolean nearLeft = Math.abs(u - crop.minU()) <= rangeU;
        boolean nearRight = Math.abs(u - crop.maxU()) <= rangeU;
        boolean nearTop = Math.abs(v - crop.minV()) <= rangeV;
        boolean nearBottom = Math.abs(v - crop.maxV()) <= rangeV;
        if (nearTop && (nearLeft || nearRight))
            return nearLeft ? 1 : 2;
        if (nearBottom && (nearLeft || nearRight))
            return nearLeft ? 3 : 4;
        return 0;
    }

    private void dragCropHandle(double mouseX, double mouseY)
    {
        float u = (float) (mouseX - MAP_LEFT) / Math.max(1, mapWidth());
        float v = (float) (mouseY - MAP_TOP) / Math.max(1, mapHeight());
        boolean leftSide = draggingHandle == 1 || draggingHandle == 3;
        boolean topSide = draggingHandle == 1 || draggingHandle == 2;
        crop = TeamsMap.ImageCrop.normalised(leftSide ? Math.min(u, crop.maxU()) : crop.minU(), topSide ? Math.min(v, crop.maxV()) : crop.minV(), leftSide ? crop.maxU() : Math.max(u, crop.minU()),
            topSide ? crop.maxV() : Math.max(v, crop.minV()));
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        // Closing it on a dead player would only bring it straight back.
        return manuallyOpened;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
