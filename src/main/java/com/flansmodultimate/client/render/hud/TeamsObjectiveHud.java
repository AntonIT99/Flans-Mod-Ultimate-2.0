package com.flansmodultimate.client.render.hud;

import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.teams.ObjectiveMode;
import com.flansmodultimate.common.teams.ObjectiveSnapshot;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

/**
 * The objective banner of Domination, Conquest and Assault, ported from the Labjac fork: what
 * each side has left, the clock, one square per capture point in the colour of whoever holds
 * it, and a compass cue towards the point worth going for.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsObjectiveHud
{
    private static final int BAR_BACKGROUND = 0xB07A7A7A;
    private static final int BAR_OUTLINE = 0xB0000000;
    private static final int NEUTRAL = 0xFFB8B8B8;
    private static final int TEXT = 0xFFEFEFEF;
    private static final int CONTESTED = 0xFFFFD966;
    private static final int NAVIGATION = 0xFFFF4D24;
    private static final int ACTIVE_OUTLINE = 0xFFE0E0E0;
    private static final int IDLE_OUTLINE = 0xCC111111;
    private static final int TOP = 5;
    private static final int BAR_WIDTH = 76;
    private static final int BAR_HEIGHT = 7;
    private static final int BAR_GAP = 28;
    private static final int SIDE_BAR_WIDTH = 46;
    private static final int SIDE_BAR_GAP = 6;
    private static final int ICON_SIZE = 10;
    private static final int ICON_GAP = 5;
    /** How far to either side of straight ahead the compass cue slides before it turns into an arrow. */
    private static final float NAVIGATION_VIEW_DEGREES = 75F;

    /**
     * Draws the banner when the running round has capture points.
     *
     * @return whether it did, in which case the plain Teams banner is left out
     */
    public static boolean render(GuiGraphics graphics, int screenWidth)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        PacketTeamsState state = TeamsClientState.get();
        if (player == null || state == null || !state.isEnabled() || !state.isRoundRunning() || state.getObjectiveMode() == ObjectiveMode.NONE)
            return false;
        List<ObjectiveSnapshot> objectives = TeamsClientState.objectives();
        PacketTeamsState.TeamScore left = state.getTeamScore(state.getAttackingTeamId());
        PacketTeamsState.TeamScore right = state.getTeamScore(state.getDefendingTeamId());
        if (objectives.isEmpty() || left == null || right == null)
            return false;
        if (minecraft.options.hideGui)
            return true;

        Font font = minecraft.font;
        int centre = screenWidth / 2;
        int leftX = centre - BAR_GAP / 2 - BAR_WIDTH;
        int rightX = centre + BAR_GAP / 2;
        boolean assault = state.getObjectiveMode() == ObjectiveMode.ASSAULT;
        ObjectiveSnapshot frontline = assault ? frontline(objectives, state.getAttackingTeamId()) : null;
        List<ObjectiveSnapshot> shown = frontline == null ? objectives : List.of(frontline);
        ObjectiveSnapshot standingOn = standingOn(player, shown);

        int limit = Math.max(1, state.getScoreLimit());
        List<Integer> sideIds = state.getSideIds();
        if (sideIds.size() > 2)
        {
            drawSideBars(graphics, font, state, sideIds, centre, limit);
            int seconds = Math.max(0, state.getTimeLeftTicks() / 20);
            String line = state.getMapName() + "  " + String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60) + "  " + state.getGameType();
            drawCentred(graphics, font, line, centre, TOP + 9, 0.62F, TEXT);
            drawObjectives(graphics, font, state, player, objectives, shown, standingOn, centre);
            return true;
        }
        if (assault)
        {
            // Defenders are measured in objectives they still hold, attackers in reinforcements left.
            int holding = (int) objectives.stream().filter(objective -> !objective.state().isHeldBy(state.getAttackingTeamId())).count();
            drawBar(graphics, font, leftX, right.colour(), holding, objectives.size(), true);
            drawBar(graphics, font, rightX, left.colour(), limit - left.score(), limit, false);
        }
        else
        {
            // Every point the enemy scores is a ticket this side has lost.
            drawBar(graphics, font, leftX, left.colour(), limit - right.score(), limit, false);
            drawBar(graphics, font, rightX, right.colour(), limit - left.score(), limit, true);
        }
        int seconds = Math.max(0, state.getTimeLeftTicks() / 20);
        drawCentred(graphics, font, String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60), centre, TOP + 1, 0.72F, TEXT);
        drawCentred(graphics, font, state.getMapName(), leftX + BAR_WIDTH / 2, TOP + 9, 0.62F, TEXT);
        drawCentred(graphics, font, state.getGameType(), rightX + BAR_WIDTH / 2, TOP + 9, 0.62F, TEXT);
        drawObjectives(graphics, font, state, player, objectives, shown, standingOn, centre);
        return true;
    }

    /** The row of capture point squares and, under it, the compass cue towards the one worth going for. */
    // The banner is laid out around these values; bundling them would only move the list elsewhere.
    @SuppressWarnings("java:S107")
    private static void drawObjectives(GuiGraphics graphics, Font font, PacketTeamsState state, LocalPlayer player, List<ObjectiveSnapshot> objectives, List<ObjectiveSnapshot> shown,
        @Nullable ObjectiveSnapshot standingOn, int centre)
    {
        int rowWidth = shown.size() * ICON_SIZE + Math.max(0, shown.size() - 1) * ICON_GAP;
        for (int i = 0; i < shown.size(); i++)
            drawObjective(graphics, font, state, shown.get(i), centre - rowWidth / 2 + i * (ICON_SIZE + ICON_GAP), TOP + 15, shown.get(i) == standingOn);

        ObjectiveSnapshot target = navigationTarget(state, objectives, player);
        if (target != null)
            drawNavigation(graphics, font, player, target, standingOn != null && standingOn.id().equals(target.id()), centre, TOP + 28);
    }

    /**
     * With more than two sides there is no single enemy whose score is this side's lost tickets,
     * so each side gets a bar of its own filling up towards the score limit.
     */
    private static void drawSideBars(GuiGraphics graphics, Font font, PacketTeamsState state, List<Integer> sideIds, int centre, int limit)
    {
        int rowWidth = sideIds.size() * SIDE_BAR_WIDTH + (sideIds.size() - 1) * SIDE_BAR_GAP;
        for (int i = 0; i < sideIds.size(); i++)
        {
            PacketTeamsState.TeamScore side = state.getTeamScore(sideIds.get(i));
            if (side == null)
                continue;
            int x = centre - rowWidth / 2 + i * (SIDE_BAR_WIDTH + SIDE_BAR_GAP);
            int fill = Math.min(SIDE_BAR_WIDTH, Math.round(SIDE_BAR_WIDTH * (Math.max(0, side.score()) / (float) limit)));
            graphics.fill(x - 1, TOP - 1, x + SIDE_BAR_WIDTH + 1, TOP + BAR_HEIGHT + 1, BAR_OUTLINE);
            graphics.fill(x, TOP, x + SIDE_BAR_WIDTH, TOP + BAR_HEIGHT, BAR_BACKGROUND);
            graphics.fill(x, TOP, x + fill, TOP + BAR_HEIGHT, 0xFF000000 | side.colour());
            drawCentred(graphics, font, Integer.toString(side.score()), x + SIDE_BAR_WIDTH / 2, TOP + 1, 0.62F, 0xFFFFFFFF);
        }
    }

    /** The objective an Assault is being fought over: the first one the attackers do not hold outright. */
    @Nullable
    public static ObjectiveSnapshot frontline(List<ObjectiveSnapshot> objectives, int attackingTeamId)
    {
        return objectives.stream().filter(objective -> !objective.state().isHeldBy(attackingTeamId)).findFirst().orElse(null);
    }

    @Nullable
    private static ObjectiveSnapshot standingOn(LocalPlayer player, List<ObjectiveSnapshot> objectives)
    {
        for (ObjectiveSnapshot objective : objectives)
        {
            double dx = player.getX() - objective.x();
            double dz = player.getZ() - objective.z();
            if (dx * dx + dz * dz <= (double) objective.radius() * objective.radius() && Math.abs(player.getY() - objective.y()) <= objective.radius())
                return objective;
        }
        return null;
    }

    /**
     * The point the compass cue leads to: the front line for the attackers of an Assault, and in
     * Conquest the nearest neutral point, or failing that the nearest one the enemy holds.
     */
    @Nullable
    private static ObjectiveSnapshot navigationTarget(PacketTeamsState state, List<ObjectiveSnapshot> objectives, LocalPlayer player)
    {
        // Capture points are owned by sides, so the viewer is compared by the side they fight for.
        int teamId = state.getPlayerSideId();
        if (state.getObjectiveMode() == ObjectiveMode.ASSAULT)
            return teamId == state.getAttackingTeamId() ? frontline(objectives, state.getAttackingTeamId()) : null;
        if (state.getObjectiveMode() != ObjectiveMode.CONQUEST || teamId < 2)
            return null;

        ObjectiveSnapshot nearestNeutral = null;
        ObjectiveSnapshot nearestEnemy = null;
        for (ObjectiveSnapshot objective : objectives)
        {
            if (objective.ownerTeamId() == 0 && (nearestNeutral == null || distanceSquared(player, objective) < distanceSquared(player, nearestNeutral)))
                nearestNeutral = objective;
            else if (objective.ownerTeamId() > 1 && objective.ownerTeamId() != teamId && (nearestEnemy == null || distanceSquared(player, objective) < distanceSquared(player, nearestEnemy)))
                nearestEnemy = objective;
        }
        return nearestNeutral != null ? nearestNeutral : nearestEnemy;
    }

    private static double distanceSquared(LocalPlayer player, ObjectiveSnapshot objective)
    {
        double dx = objective.x() - player.getX();
        double dz = objective.z() - player.getZ();
        return dx * dx + dz * dz;
    }

    private static void drawBar(GuiGraphics graphics, Font font, int x, int teamColour, int remaining, int total, boolean fillFromLeft)
    {
        int value = Math.max(0, remaining);
        int fill = Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * (value / (float) Math.max(1, total))));
        graphics.fill(x - 1, TOP - 1, x + BAR_WIDTH + 1, TOP + BAR_HEIGHT + 1, BAR_OUTLINE);
        graphics.fill(x, TOP, x + BAR_WIDTH, TOP + BAR_HEIGHT, BAR_BACKGROUND);
        if (fillFromLeft)
            graphics.fill(x, TOP, x + fill, TOP + BAR_HEIGHT, 0xFF000000 | teamColour);
        else
            graphics.fill(x + BAR_WIDTH - fill, TOP, x + BAR_WIDTH, TOP + BAR_HEIGHT, 0xFF000000 | teamColour);
        drawCentred(graphics, font, Integer.toString(value), x + BAR_WIDTH / 2, TOP + 1, 0.62F, 0xFFFFFFFF);
    }

    private static void drawObjective(GuiGraphics graphics, Font font, PacketTeamsState state, ObjectiveSnapshot objective, int x, int y, boolean active)
    {
        int ownerColour = teamColour(state, objective.ownerTeamId());
        int border = active ? ACTIVE_OUTLINE : IDLE_OUTLINE;
        if (objective.contested())
            border = CONTESTED;
        int grow = active ? 2 : 0;
        int left = x - grow;
        int top = y - grow;
        int size = ICON_SIZE + grow * 2;
        graphics.fill(left, top, left + size, top + size, border);
        graphics.fill(left + 1, top + 1, left + size - 1, top + size - 1, ownerColour);
        if (objective.capturingTeamId() != 0 && objective.progress() > 0F && objective.progress() < 1F)
        {
            int fill = Math.max(1, Math.round((size - 2) * objective.progress()));
            graphics.fill(left + 1, top + size - 3, left + 1 + fill, top + size - 1, teamColour(state, objective.capturingTeamId()));
        }
        drawCentred(graphics, font, String.valueOf(objective.symbol()), left + size / 2, top + size / 2 - 3, 0.64F, 0xFFFFFFFF);
    }

    private static int teamColour(PacketTeamsState state, int teamId)
    {
        PacketTeamsState.TeamScore team = state.getTeamScore(teamId);
        return team == null ? NEUTRAL : 0xFF000000 | team.colour();
    }

    /** A cue that slides left and right with the bearing of the point and turns into an arrow when it is behind the player. */
    private static void drawNavigation(GuiGraphics graphics, Font font, LocalPlayer player, ObjectiveSnapshot objective, boolean onObjective, int centre, int y)
    {
        if (onObjective)
        {
            drawCentred(graphics, font, Component.translatable("gui.flansmod.teams.on_objective").getString(), centre, y, 0.62F, NAVIGATION);
            return;
        }
        double dx = objective.x() - player.getX();
        double dz = objective.z() - player.getZ();
        // Yaw 0 looks towards +Z and grows clockwise seen from above, hence the mirrored X.
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float relative = Mth.wrapDegrees(bearing - player.getYRot());
        String arrow = "^";
        if (relative < -NAVIGATION_VIEW_DEGREES)
            arrow = "<";
        else if (relative > NAVIGATION_VIEW_DEGREES)
            arrow = ">";
        int arrowX = centre + Math.round(Mth.clamp(relative, -NAVIGATION_VIEW_DEGREES, NAVIGATION_VIEW_DEGREES) * 1.35F);
        drawCentred(graphics, font, arrow, arrowX, y, 1F, NAVIGATION);
        drawCentred(graphics, font, objective.symbol() + " " + Math.round(Math.sqrt(dx * dx + dz * dz)) + "m", centre, y + 8, 0.62F, NAVIGATION);
    }

    private static void drawCentred(GuiGraphics graphics, Font font, String text, int centreX, int y, float scale, int colour)
    {
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, y, 0F);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, text, -font.width(text) / 2, 0, colour, true);
        graphics.pose().popPose();
    }
}
