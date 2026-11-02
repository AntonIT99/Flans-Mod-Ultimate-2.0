package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.content.PaintjobVariant;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelNpcs;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Selects all paintjobs or a nonempty subset, with a preview of the last paintjob clicked. */
final class BotPaintjobsScreen extends Screen
{
    private static final String PREFIX = "wolffsmodnpcs.picker.vehicles.";
    private static final int PAGE_SIZE = 5;
    private final Screen parent;
    private final String type;
    private final Consumer<List<Integer>> save;
    private final List<PaintjobVariant> variants;
    private final Set<Integer> selected = new LinkedHashSet<>();
    private boolean all;
    private int page;
    private int preview;

    BotPaintjobsScreen(Screen parent, String type, List<Integer> allowed, Consumer<List<Integer>> save)
    {
        super(Component.translatable(PREFIX + "paintjobs"));
        this.parent = parent;
        this.type = type;
        this.save = save;
        variants = FlanModelNpcs.type(type).map(FlanModelEntityType::getInfoType).map(IContentType::getPaintjobVariants).orElse(List.of());
        selected.addAll(allowed);
        all = allowed.isEmpty();
        preview = allowed.isEmpty() ? 0 : allowed.get(0);
    }

    @Override
    protected void init()
    {
        int left = width / 2 - 150;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + (all ? "paintjobs.all_selected" : "paintjobs.all")), button ->
        {
            all = true;
            rebuildWidgets();
        }).bounds(left, 30, 300, 20).build());
        for (int row = 0; row < PAGE_SIZE && page * PAGE_SIZE + row < variants.size(); row++)
        {
            PaintjobVariant variant = variants.get(page * PAGE_SIZE + row);
            String name = variant.name().isBlank() ? "#" + variant.id() : variant.name();
            addRenderableWidget(Button.builder(Component.literal((all || selected.contains(variant.id()) ? "✓ " : "") + name), button ->
            {
                toggle(variant.id());
                rebuildWidgets();
            }).bounds(left, 56 + row * 22, 175, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button ->
        {
            page--;
            rebuildWidgets();
        }).bounds(left, 170, 40, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
        {
            page++;
            rebuildWidgets();
        }).bounds(left + 135, 170, 40, 20).build()).active = (page + 1) * PAGE_SIZE < variants.size();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button ->
        {
            save.accept(all ? List.of() : List.copyOf(selected));
            onClose();
        }).bounds(left, height - 28, 145, 20).build()).active = all || !selected.isEmpty();
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(left + 155, height - 28, 145, 20).build());
    }

    private void toggle(int id)
    {
        if (all)
        {
            selected.clear();
            variants.forEach(job -> selected.add(job.id()));
            all = false;
        }
        if (!selected.remove(id))
            selected.add(id);
        preview = id;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        var client = Minecraft.getInstance();
        if (client.level != null)
            FlanModelNpcs.type(type).ifPresent(modelType ->
            {
                FlanModelEntity model = new FlanModelEntity(modelType, client.level);
                model.setPaintjobId(preview);
                FlanModelMenuPreview.render(graphics, model, "", 35, width / 2 + 32, 56, width / 2 + 150, 166);
            });
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
