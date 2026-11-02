package com.flansmodultimate.mcpdev;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import xyz.langyo.minecraft.mcp.common.ControlModeHelper;

/** All methods run on the Minecraft client thread. Snapshot IDs invalidate references when the screen or its controls change. */
final class McpUi
{
    private static final String SCREEN = "screen";
    private static final String SNAPSHOT = "snapshot";
    private final Minecraft minecraft = Minecraft.getInstance();
    private final Map<String, AbstractWidget> widgets = new LinkedHashMap<>();
    private Screen previousScreen;
    private String signature = "";
    private long revision;
    private List<AbstractWidget> previousWidgets = List.of();

    JsonObject state()
    {
        Screen screen = minecraft.screen;
        widgets.clear();
        JsonArray controls = new JsonArray();
        if (screen != null)
            visit(screen, "", controls, Collections.newSetFromMap(new IdentityHashMap<>()));
        JsonObject result = new JsonObject();
        result.addProperty(SCREEN, screen == null ? "InGame" : screen.getClass().getSimpleName());
        result.addProperty("screen_class", screen == null ? "" : screen.getClass().getName());
        result.addProperty("title", screen == null ? "" : screen.getTitle().getString());
        result.addProperty("gui_width", minecraft.getWindow().getGuiScaledWidth());
        result.addProperty("gui_height", minecraft.getWindow().getGuiScaledHeight());
        result.addProperty("framebuffer_width", minecraft.getMainRenderTarget().viewWidth);
        result.addProperty("framebuffer_height", minecraft.getMainRenderTarget().viewHeight);
        result.addProperty("gui_scale", minecraft.getWindow().getGuiScale());
        result.addProperty("coordinate_space", "gui");
        result.add("widgets", controls);
        String current = result.toString();
        List<AbstractWidget> currentWidgets = new ArrayList<>(widgets.values());
        if (screen != previousScreen || !current.equals(signature) || !currentWidgets.equals(previousWidgets))
        {
            revision++;
            previousScreen = screen;
            signature = current;
            previousWidgets = currentWidgets;
        }
        result.addProperty(SNAPSHOT, revision);
        result.addProperty("control_mode", ControlModeHelper.isMcpControlMode());
        result.addProperty("in_world", minecraft.level != null && minecraft.player != null);
        result.addProperty("test_profile", Boolean.getBoolean("fmu.mcp.testProfile"));
        return result;
    }

    private void visit(ContainerEventHandler parent, String path, JsonArray controls, Set<GuiEventListener> seen)
    {
        int index = 0;
        for (GuiEventListener child : parent.children())
        {
            String id = path + index++;
            if (!seen.add(child))
                continue;
            if (child instanceof AbstractWidget widget)
            {
                widgets.put(id, widget);
                JsonObject control = new JsonObject();
                control.addProperty("id", id);
                control.addProperty("type", widget.getClass().getSimpleName());
                control.addProperty("label", widget.getMessage().getString());
                control.addProperty("translation_key", translationKey(widget.getMessage()));
                control.addProperty("x", widget.getX());
                control.addProperty("y", widget.getY());
                control.addProperty("width", widget.getWidth());
                control.addProperty("height", widget.getHeight());
                control.addProperty("active", widget.active);
                control.addProperty("visible", widget.visible);
                if (widget instanceof EditBox edit)
                    control.addProperty("value", edit.getValue());
                controls.add(control);
            }
            if (child instanceof ContainerEventHandler container)
                visit(container, id + "/", controls, seen);
        }
    }

    private static String translationKey(Component component)
    {
        if (component.getContents() instanceof TranslatableContents translation)
            return translation.getKey();
        return "";
    }

    JsonObject click(JsonObject request)
    {
        JsonObject before = validateSnapshot(request);
        AbstractWidget widget = widgets.get(request.get("widget").getAsString());
        if (widget == null || !widget.visible || !widget.active)
            throw new IllegalArgumentException("Widget is absent, hidden or disabled");
        Screen screen = minecraft.screen;
        double x = widget.getX() + widget.getWidth() * 0.5D;
        double y = widget.getY() + widget.getHeight() * 0.5D;
        boolean consumed = screen.mouseClicked(x, y, 0);
        screen.mouseReleased(x, y, 0);
        JsonObject result = state();
        result.addProperty("input_consumed", consumed);
        result.addProperty("changed", result.get(SNAPSHOT).getAsLong() != before.get(SNAPSHOT).getAsLong());
        return result;
    }

    JsonObject close(JsonObject request)
    {
        validateSnapshot(request);
        if (minecraft.screen == null)
            throw new IllegalArgumentException("No screen is open");
        minecraft.screen.onClose();
        return state();
    }

    private JsonObject validateSnapshot(JsonObject request)
    {
        requireControl();
        JsonObject before = state();
        if (!request.has(SNAPSHOT) || request.get(SNAPSHOT).getAsLong() != revision)
            throw new IllegalArgumentException("Stale UI snapshot: observe again before input");
        String expected = request.get("expected_screen").getAsString();
        if (!expected.equals(before.get(SCREEN).getAsString()))
            throw new IllegalArgumentException("Unexpected screen: " + before.get(SCREEN));
        return before;
    }

    static void requireControl()
    {
        if (!ControlModeHelper.isMcpControlMode())
            throw new IllegalStateException("MCP is not in control mode; enter_control_mode before input");
    }

    JsonObject capture(JsonObject request) throws IOException
    {
        Path root = Path.of(System.getProperty("fmu.mcp.captureDir")).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path path = request.has("path") ? Path.of(request.get("path").getAsString()).toAbsolutePath().normalize() : root.resolve("capture-" + System.nanoTime() + ".png");
        if (!path.getParent().equals(root) || !path.getFileName().toString().endsWith(".png"))
            throw new IllegalArgumentException("Screenshots must be PNG files directly inside " + root);
        JsonObject result = state();
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget()))
        {
            Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y += Math.max(1, image.getHeight() / 24))
                for (int x = 0; x < image.getWidth(); x += Math.max(1, image.getWidth() / 32))
                    colors.add(image.getPixelRGBA(x, y));
            image.writeToFile(path);
            result.addProperty("image_width", image.getWidth());
            result.addProperty("image_height", image.getHeight());
            result.addProperty("sampled_colors", colors.size());
        }
        result.addProperty("file", path.toString());
        result.addProperty("size", Files.size(path));
        return result;
    }
}
