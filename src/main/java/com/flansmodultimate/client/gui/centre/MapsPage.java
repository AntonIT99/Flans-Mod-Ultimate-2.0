package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.CommandCentreRow;
import com.flansmodultimate.api.client.ICommandCentreView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The maps rounds are played on: making one where the operator stands, renaming it, giving it a playing area and a
 * deployment image, starting a round on it and removing it.
 */
final class MapsPage extends TeamsPage
{
    static final String ID = "flansmodultimate:maps";
    private static final Pattern MAP_ID = Pattern.compile("[0-9A-Za-z_.+-]+");
    private static final Pattern IMAGE = Pattern.compile("[0-9A-Za-z_-]+");
    private static final Pattern POSITION = Pattern.compile("\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s*");
    private static final String NEW_ID = "new_id";
    private static final String NEW_NAME = "new_name";

    private enum Tab
    {
        GENERAL, AREA
    }

    private String selected = "";
    private boolean creating;
    /** The map just asked for, selected once it shows up. */
    private String awaited = "";
    private Tab tab = Tab.GENERAL;
    private int listWidth;

    MapsPage()
    {
        super("maps", Items.FILLED_MAP, 300);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        TeamsData data = data();
        if (!awaited.isEmpty() && data.map(awaited).isPresent())
        {
            selected = awaited;
            awaited = "";
            creating = false;
        }
        if (data.map(selected).isEmpty())
            selected = data.maps().isEmpty() ? "" : data.maps().get(0).id();
        listWidth = Math.max(140, view.getWidth() * 40 / 100);
        button(view, tr("new_map"), view.getLeft(), view.getTop(), listWidth, () ->
        {
            creating = true;
            view.rebuild();
        });
        List<CommandCentreRow> rows = new ArrayList<>();
        for (TeamsData.MapInfo map : data.maps())
        {
            long rounds = data.rounds().stream().filter(round -> round.map().equals(map.id())).count();
            rows.add(CommandCentreRow.of(map.id(), Component.literal(map.name()), Component.literal(map.id() + " · " + map.dimension())).selected(map.id().equals(selected))
                .withBadge(tr("badge.rounds", rounds), rounds > 0 ? CommandCentreRow.NEUTRAL : CommandCentreRow.WARNING));
        }
        view.addList("maps", new ScreenRectangle(view.getLeft(), view.getTop() + WIDGET_HEIGHT + GAP, listWidth, view.getHeight() - WIDGET_HEIGHT - GAP), rows, tr("maps_empty"), key ->
        {
            selected = key;
            creating = false;
            view.rebuild();
        });
        int x = view.getLeft() + listWidth + 12;
        int width = view.getWidth() - listWidth - 18;
        Optional<TeamsData.MapInfo> map = data.map(selected);
        if (creating || map.isEmpty())
            initCreate(view, x, width, map.isPresent());
        else
        {
            tabs(view, Tab.values(), tab, shown -> tr("map_tab." + shown.name().toLowerCase(java.util.Locale.ROOT)), x, view.getTop() + 18, width, chosen -> tab = chosen);
            if (tab == Tab.GENERAL)
                initGeneral(view, data, map.get(), x, width);
            else
                initArea(view, map.get(), x, width);
        }
    }

    private void initCreate(ICommandCentreView view, int x, int width, boolean cancellable)
    {
        int y = view.getTop() + 26 + view.getFont().wordWrapHeight(tr("new_map_help"), width);
        view.addField(NEW_ID, x, y, width, "", tr("map_id"));
        view.addField(NEW_NAME, x, y + ROW, width, "", tr("map_name"));
        int half = (width - GAP) / 2;
        button(view, tr("create_map"), x, y + 2 * ROW, cancellable ? half : width, () ->
        {
            String id = view.getField(NEW_ID).strip();
            String name = view.getField(NEW_NAME).strip();
            if (!MAP_ID.matcher(id).matches())
            {
                view.showMessage(tr("bad_map_id"), true);
                return;
            }
            awaited = id.toLowerCase(java.util.Locale.ROOT);
            run(view, "teams admin map add", id, name.isEmpty() ? id : name);
        });
        if (cancellable)
            button(view, tr("cancel"), x + half + GAP, y + 2 * ROW, half, () ->
            {
                creating = false;
                awaited = "";
                view.rebuild();
            });
    }

    private void initGeneral(ICommandCentreView view, TeamsData data, TeamsData.MapInfo map, int x, int width)
    {
        int y = view.getTop() + 18 + ROW + 36;
        int buttonWidth = 70;
        int fieldWidth = width - buttonWidth - GAP;
        view.addField("name@" + map.id(), x, y, fieldWidth, map.name(), tr("map_name"));
        button(view, tr("rename"), x + fieldWidth + GAP, y, buttonWidth, () ->
        {
            String name = view.getField("name@" + map.id()).strip();
            if (!name.isEmpty())
                run(view, "teams admin map rename", map.id(), name);
        });
        y += ROW;
        view.addField("image@" + map.id(), x, y, fieldWidth, map.image(), tr("map_image"));
        button(view, tr("set_image"), x + fieldWidth + GAP, y, buttonWidth, tr("set_image.tip"), () ->
        {
            String image = view.getField("image@" + map.id()).strip();
            if (IMAGE.matcher(image).matches())
                run(view, "teams admin map image", map.id(), image);
            else
                view.showMessage(tr("bad_image"), true);
        });
        y += ROW;
        int half = (width - GAP) / 2;
        button(view, tr("round_here"), x, y, half, () -> RotationPage.openBuilder(view, map.id()));
        long rounds = data.rounds().stream().filter(round -> round.map().equals(map.id())).count();
        button(view, tr("remove_map"), x + half + GAP, y, half, () -> view.confirm(tr("remove_map.confirm", map.name(), rounds), () -> run(view, "teams admin map remove", map.id())));
    }

    private static void initArea(ICommandCentreView view, TeamsData.MapInfo map, int x, int width)
    {
        int y = view.getTop() + 18 + ROW + 26;
        int buttonWidth = 50;
        int fieldWidth = width - buttonWidth - GAP;
        List<Integer> bounds = map.bounds();
        String first = bounds.size() == 6 ? bounds.get(0) + " " + bounds.get(1) + " " + bounds.get(2) : "";
        String second = bounds.size() == 6 ? bounds.get(3) + " " + bounds.get(4) + " " + bounds.get(5) : "";
        String firstKey = "from@" + map.id();
        String secondKey = "to@" + map.id();
        view.addField(firstKey, x, y, fieldWidth, first, tr("corner", 1));
        button(view, tr("here"), x + fieldWidth + GAP, y, buttonWidth, tr("here.tip"), () -> view.setField(firstKey, here()));
        view.addField(secondKey, x, y + ROW, fieldWidth, second, tr("corner", 2));
        button(view, tr("here"), x + fieldWidth + GAP, y + ROW, buttonWidth, tr("here.tip"), () -> view.setField(secondKey, here()));
        int half = (width - GAP) / 2;
        button(view, tr("set_area"), x, y + 2 * ROW, half, () ->
        {
            Matcher from = POSITION.matcher(view.getField(firstKey));
            Matcher to = POSITION.matcher(view.getField(secondKey));
            if (!from.matches() || !to.matches())
            {
                view.showMessage(tr("bad_corner"), true);
                return;
            }
            run(view, "teams admin map bounds", map.id(), from.group(1), from.group(2), from.group(3), to.group(1), to.group(2), to.group(3));
        });
        button(view, tr("clear_area"), x + half + GAP, y + 2 * ROW, half, () -> run(view, "teams admin map clearBounds", map.id())).active = bounds.size() == 6;
    }

    /** The block the local player stands in, as a command writes a position. */
    private static String here()
    {
        var player = Minecraft.getInstance().player;
        BlockPos position = player == null ? BlockPos.ZERO : player.blockPosition();
        return position.getX() + " " + position.getY() + " " + position.getZ();
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        TeamsData data = data();
        int panelLeft = view.getLeft() + listWidth + 6;
        int panelWidth = view.getWidth() - listWidth - 6;
        int x = panelLeft + 6;
        int width = panelWidth - 12;
        Optional<TeamsData.MapInfo> shown = data.map(selected);
        if (creating || shown.isEmpty())
        {
            view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), tr("new_map_title"));
            paragraph(view, graphics, tr("new_map_help"), x, view.getTop() + 20, width, CentreStyle.DIM);
            return;
        }
        TeamsData.MapInfo map = shown.get();
        view.drawPanel(graphics, panelLeft, view.getTop(), panelWidth, view.getHeight(), Component.literal(map.name()));
        int y = view.getTop() + 18 + ROW + 2;
        if (tab == Tab.GENERAL)
        {
            label(view, graphics, tr("map_where", map.id(), map.dimension()), x, y, width, CentreStyle.DIM);
            label(view, graphics, tr(map.capturePoints() ? "map_bases_points" : "map_bases", map.bases()), x, y + 11, width, map.bases() > 0 ? CentreStyle.DIM : CentreStyle.ACCENT);
            Component playable = map.playable().isEmpty() ? tr("map_playable_none") : tr("map_playable", String.join(", ", map.playable().stream().map(data::gameTypeName).toList()));
            label(view, graphics, playable, x, y + 22, width, CentreStyle.DIM);
            return;
        }
        List<Integer> bounds = map.bounds();
        Component area = bounds.size() == 6 ? tr("area_set", bounds.get(0) + " " + bounds.get(1) + " " + bounds.get(2), bounds.get(3) + " " + bounds.get(4) + " " + bounds.get(5)) : tr("area_none");
        label(view, graphics, area, x, y, width, CentreStyle.DIM);
        paragraph(view, graphics, tr("area_help"), x, y + 24 + 3 * ROW, width, CentreStyle.MUTED);
    }
}
