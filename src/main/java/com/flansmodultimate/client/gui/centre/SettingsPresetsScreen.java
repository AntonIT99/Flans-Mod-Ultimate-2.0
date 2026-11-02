package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.CommandCentreRow;
import com.flansmodultimate.common.teams.centre.PresetFiles;
import com.flansmodultimate.common.teams.centre.SettingsPresets;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketPresetAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Shared client-side preset library UI; the server owns all configuration and validates every action. */
public final class SettingsPresetsScreen extends Screen
{
    private static final String PREVIEW = "preview";
    private static final String IMPORT = "import";
    private static final String EXPORT = "export";
    private static final String JSON_EXTENSION = ".json";
    private static final String KEY = "gui.flansmodultimate.presets.";
    private static int nextRequest;
    @Nullable
    private static SettingsPresetsScreen active;

    @Nullable
    private final Screen parent;
    private final ResourceLocation library;
    private final String target;
    private List<String> names = List.of();
    private List<String> builtIns = List.of();
    private List<Component> previewLines = List.of();
    private String selected = "";
    private String savedName = "";
    private String previewJson = "";
    private String previewTarget = "";
    private String action = "";
    private Component status = Component.empty();
    private boolean failed;
    private int request;
    private int listScroll;
    private int previewScroll;
    private boolean waiting;
    @Nullable
    private Path exportFile;
    private boolean exportReplace;
    private String requestedName = "";

    private SettingsPresetsScreen(@Nullable Screen parent, ResourceLocation library, String target)
    {
        super(tr("title"));
        this.parent = parent;
        this.library = library;
        this.target = target;
    }

    public static void open(ResourceLocation library, String target)
    {
        Minecraft game = Minecraft.getInstance();
        SettingsPresetsScreen screen = new SettingsPresetsScreen(game.screen, library, target);
        active = screen;
        game.setScreen(screen);
        screen.send("list", "", target, "", false);
    }

    private static Component tr(String key, Object... args)
    {
        return Component.translatable(KEY + key, args);
    }

    @Override
    protected void init()
    {
        int left = 12;
        int top = 34;
        int column = Math.max(100, (width - 36) / 3);
        int right = left + column + 12;
        int rightWidth = width - right - 12;
        int bottom = height - 76;
        List<CommandCentreRow> rows = names.stream().map(name -> CommandCentreRow.of(name, Component.literal(name), Component.empty()).selected(name.equals(selected))).toList();
        addRenderableWidget(new CentreList(new ScreenRectangle(left, top, column, Math.max(22, bottom - top)), rows, tr("empty"), listScroll, name ->
        {
            if (waiting)
                return;
            selected = name;
            previewLines = List.of();
            previewJson = "";
            previewScroll = 0;
            rebuild();
        }, value -> listScroll = value));
        List<CommandCentreRow> lines = new ArrayList<>();
        for (Component line : previewLines)
        {
            for (var wrapped : font.split(line, Math.max(60, rightWidth - 12)))
            {
                StringBuilder text = new StringBuilder();
                wrapped.accept((index, style, codePoint) ->
                {
                    text.appendCodePoint(codePoint);
                    return true;
                });
                lines.add(CommandCentreRow.of(Integer.toString(lines.size()), Component.literal(text.toString()), Component.empty()));
            }
        }
        addRenderableWidget(new CentreList(new ScreenRectangle(right, top, rightWidth, Math.max(22, bottom - top - 48)), lines, tr("preview_hint"), previewScroll, ignored ->
        {
        }, value -> previewScroll = value));
        EditBox nameField = new EditBox(font, right, bottom - 42, Math.max(60, rightWidth - 94), 18, tr("name"));
        nameField.setMaxLength(32);
        nameField.setValue(savedName);
        nameField.setHint(tr("name"));
        nameField.setResponder(value -> savedName = value);
        addRenderableWidget(nameField);
        button("save", right + rightWidth - 88, bottom - 42, 88, !waiting, () -> overwrite(savedName, () -> send("save", savedName, target, "", names.contains(savedName))));
        int third = (rightWidth - 8) / 3;
        button(PREVIEW, right, bottom - 20, third, !waiting && !selected.isEmpty(), () -> send(PREVIEW, selected, target, "", false));
        button("apply", right + third + 4, bottom - 20, third, !waiting && !previewJson.isEmpty(),
            () -> confirm(tr("apply_confirm", selected, previewTarget.isEmpty() ? tr("global") : previewTarget), () -> send("apply", selected, previewTarget, previewJson, false)));
        button("all", right + 2 * (third + 4), bottom - 20, third, !waiting && !selected.isEmpty() && !target.isEmpty(), () -> send(PREVIEW, selected, "all", "", false));
        int quarter = (width - 36) / 4;
        button(IMPORT, left, height - 66, quarter, !waiting, this::importFile);
        button(EXPORT, left + quarter + 4, height - 66, quarter, !waiting && !selected.isEmpty(), this::exportFile);
        button("folder", left + 2 * (quarter + 4), height - 66, quarter, !waiting, () ->
        {
            try
            {
                Path folder = SettingsPresets.folder(library);
                Files.createDirectories(folder);
                Util.getPlatform().openFile(folder.toFile());
            }
            catch (IOException | RuntimeException e)
            {
                error(e);
            }
        });
        boolean builtIn = builtIns.contains(selected);
        button("delete", left + 3 * (quarter + 4), height - 66, quarter, !waiting && !selected.isEmpty() && !builtIn,
            () -> confirm(tr("delete_confirm", selected), () -> send("remove", selected, target, "", false)));
        button("back", width / 2 - 50, height - 24, 100, true, this::onClose);
    }

    private void button(String key, int x, int y, int buttonWidth, boolean enabled, Runnable callback)
    {
        Button button = addRenderableWidget(Button.builder(tr(key), ignored -> callback.run()).bounds(x, y, buttonWidth, 18).build());
        button.active = enabled;
    }

    private void rebuild()
    {
        clearWidgets();
        init();
    }

    private void confirm(Component text, Runnable callback)
    {
        Minecraft game = Minecraft.getInstance();
        game.setScreen(new ConfirmScreen(yes ->
        {
            game.setScreen(this);
            if (yes)
                callback.run();
        }, title, text));
    }

    private void overwrite(String name, Runnable callback)
    {
        try
        {
            PresetFiles.checkName(name);
            if (names.contains(name))
                confirm(tr("overwrite", name), callback);
            else
                callback.run();
        }
        catch (RuntimeException e)
        {
            error(e);
        }
    }

    private static int allocateRequestId()
    {
        return ++nextRequest;
    }

    private void send(String operation, String name, String destination, String json, boolean replace)
    {
        previewJson = "";
        action = operation;
        previewTarget = destination;
        request = allocateRequestId();
        requestedName = name;
        waiting = true;
        status = tr("waiting");
        PacketHandler.sendToServer(new PacketPresetAction(request, library, operation, name, destination, json, replace));
        rebuild();
    }

    @Nullable
    private Path chooseFile(boolean save)
    {
        try
        {
            Path folder = SettingsPresets.folder(library).toAbsolutePath();
            Files.createDirectories(folder);
            try (MemoryStack stack = MemoryStack.stackPush())
            {
                var filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.json")).flip();
                String initial = save ? folder.resolve(selected + JSON_EXTENSION).toString() : folder + java.io.File.separator;
                String chosen = save
                    ? TinyFileDialogs.tinyfd_saveFileDialog(tr(EXPORT).getString(), initial, filters, "JSON")
                    : TinyFileDialogs.tinyfd_openFileDialog(tr(IMPORT).getString(), initial, filters, "JSON", false);
                if (chosen == null)
                    return null;
                if (save && !chosen.toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION))
                    chosen += JSON_EXTENSION;
                return Path.of(chosen);
            }
        }
        catch (IOException | RuntimeException e)
        {
            error(e);
            return null;
        }
    }

    private void importFile()
    {
        Path file = chooseFile(false);
        if (file == null)
            return;
        try
        {
            String json = PresetFiles.read(file);
            String name = PresetFiles.decode(library, json).name();
            overwrite(name, () -> send(IMPORT, name, target, json, names.contains(name)));
        }
        catch (IOException | RuntimeException e)
        {
            error(e);
        }
    }

    private void exportFile()
    {
        Path file = chooseFile(true);
        if (file == null)
            return;
        Runnable export = () ->
        {
            exportFile = file;
            exportReplace = Files.exists(file);
            send(EXPORT, selected, target, "", false);
        };
        if (Files.exists(file))
            confirm(tr("overwrite", file.getFileName()), export);
        else
            export.run();
    }

    public static void accept(int request, boolean failure, String json, List<String> names, List<String> builtIns, List<Component> lines, Component message)
    {
        SettingsPresetsScreen screen = active;
        if (screen == null || screen.request != request || Minecraft.getInstance().screen != screen)
            return;
        screen.waiting = false;
        screen.failed = failure;
        screen.status = message;
        if (!failure)
            screen.acceptSuccess(json, names, builtIns, lines);
        screen.rebuild();
    }

    private void acceptSuccess(String json, List<String> newNames, List<String> newBuiltIns, List<Component> lines)
    {
        names = List.copyOf(newNames);
        builtIns = List.copyOf(newBuiltIns);
        if ((action.equals("save") || action.equals(IMPORT)) && names.contains(requestedName))
        {
            selected = requestedName;
            listScroll = names.indexOf(selected) * CentreList.ROW_HEIGHT;
        }
        if (!names.contains(selected))
            selected = names.isEmpty() ? "" : names.get(0);
        if (action.equals(PREVIEW))
        {
            previewLines = List.copyOf(lines);
            previewJson = json;
            previewScroll = 0;
        }
        else if (action.equals(EXPORT) && exportFile != null)
            finishExport(json);
        else
        {
            previewJson = "";
            previewLines = List.of();
        }
    }

    private void finishExport(String json)
    {
        Path file = exportFile;
        if (file == null)
            return;
        try
        {
            PresetFiles.decode(library, json);
            PresetFiles.write(file, json, exportReplace);
            status = tr("exported", file.toString());
        }
        catch (IOException | RuntimeException e)
        {
            error(e);
        }
        exportFile = null;
    }

    private static void closeActiveScreen(@Nullable Screen parent)
    {
        active = null;
        Minecraft.getInstance().setScreen(parent);
    }

    private void error(Exception exception)
    {
        status = Component.literal(Objects.toString(exception.getMessage(), "Invalid preset"));
        failed = true;
    }

    @Override
    public void onClose()
    {
        closeActiveScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, CentreStyle.TEXT);
        String shownTarget = previewJson.isEmpty() ? target : previewTarget;
        CentreStyle.drawText(graphics, font, tr("scope", library, shownTarget.isEmpty() ? tr("global") : shownTarget), 12, 23, width - 24, CentreStyle.MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
        CentreStyle.drawText(graphics, font, status, 12, height - 43, width - 24, failed ? CentreStyle.BAD : CentreStyle.GOOD);
    }
}
