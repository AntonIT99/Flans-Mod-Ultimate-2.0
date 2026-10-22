package com.flansmodultimate.client.gui;

import com.flansmodultimate.client.render.layer.PlayerSkinOverrides;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.platform.client.ClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;

/** Searches installed resources; the preview rejects images that are not player skins. */
@SuppressWarnings("DataFlowIssue") // Screen initializes minecraft before init, callbacks and rendering.
public final class EditorSkinPickerScreen extends Screen
{
    private final Screen parent;
    private final Consumer<ResourceLocation> selected;
    private List<ResourceLocation> textures = List.of();
    private String query = "";
    private int page;
    private int rows;
    private int left;
    private int panelWidth;

    public EditorSkinPickerScreen(Screen parent, Consumer<ResourceLocation> selected)
    {
        super(TeamsEditorScreen.tr("choose_skin"));
        this.parent = parent;
        this.selected = selected;
    }

    @Override
    protected void init()
    {
        panelWidth = Math.min(560, width - 24);
        left = (width - panelWidth) / 2;
        rows = Math.max(1, (height - 120) / 30);
        Set<ResourceLocation> all = new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        all.addAll(minecraft.getResourceManager().listResources("textures/skins", id -> id.getPath().endsWith(".png")).keySet());
        PlayerClass.values().forEach(type -> type.getSkinOverrideVariants().forEach(skin ->
        {
            if (skin.texture() != null)
                all.add(skin.texture());
        }));
        textures = List.copyOf(all);
        EditBox search = new EditBox(font, left, 35, panelWidth, 20, TeamsEditorScreen.tr("search"));
        search.setMaxLength(256);
        search.setHint(TeamsEditorScreen.tr("texture_hint"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            page = 0;
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1)).bounds(left, height - 42, 25, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
        {
            if ((page + 1) * rows < filtered().size())
                page++;
        }).bounds(left + 30, height - 42, 25, 20).build());
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("add_texture"), button -> choose(ResourceLocation.tryParse(query))).bounds(left + 65, height - 42, 130, 20).build());
        addRenderableWidget(Button.builder(TeamsEditorScreen.tr("back"), button -> onClose()).bounds(left + panelWidth - 85, height - 42, 85, 20).build());
    }

    private List<ResourceLocation> filtered()
    {
        return textures.stream().filter(id -> id.toString().contains(query.toLowerCase(Locale.ROOT))).toList();
    }

    private void choose(ResourceLocation texture)
    {
        if (texture != null && PlayerSkinOverrides.previewTexture(texture) != null)
        {
            selected.accept(texture);
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button)
    {
        if (x >= left && x < left + panelWidth && y >= 65 && y < 65 + rows * 30)
        {
            int index = page * rows + ((int) y - 65) / 30;
            List<ResourceLocation> choices = filtered();
            if (index < choices.size())
                choose(choices.get(index));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        List<ResourceLocation> choices = filtered();
        ResourceLocation hovered = null;
        for (int index = page * rows; index < Math.min(choices.size(), (page + 1) * rows); index++)
        {
            ResourceLocation texture = choices.get(index);
            int y = 65 + index % rows * 30;
            ResourceLocation preview = PlayerSkinOverrides.previewTexture(texture);
            graphics.fill(left, y, left + panelWidth, y + 28, preview == null ? 0xFF462C30 : 0xFF2D394C);
            if (preview != null)
                graphics.blit(preview, left + 2, y + 2, 24, 24, 0, 0, 64, 64, 64, 64);
            graphics.drawString(font, font.plainSubstrByWidth(texture.toString(), panelWidth - 36), left + 32, y + 9, preview == null ? 0xFF8888 : 0xFFFFFF);
            if (mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + 28)
                hovered = texture;
        }
        if (hovered != null)
            graphics.renderTooltip(font, Component.literal(hovered.toString()), mouseX, mouseY);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
