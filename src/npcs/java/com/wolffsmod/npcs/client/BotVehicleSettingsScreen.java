package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.SquadTemplate;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/** Named vehicle settings; changes reach the enclosing picker only on Done. */
final class BotVehicleSettingsScreen extends Screen
{
    private static final String PREFIX = "wolffsmodnpcs.picker.vehicles.";
    private final Screen parent;
    private final BotSettings.VehicleEntry entry;
    private final Consumer<BotSettings.VehicleEntry> save;
    private final Map<String, String> templates = new LinkedHashMap<>();
    private final String[] keys = {"budget", "respawn", "tickets"};
    private final int[] values;
    private String carry;
    private List<Integer> paintjobs;

    BotVehicleSettingsScreen(Screen parent, List<SquadTemplate> templates, BotSettings.VehicleEntry entry, Consumer<BotSettings.VehicleEntry> save)
    {
        super(Component.literal(entry.type()));
        this.parent = parent;
        this.entry = entry;
        this.save = save;
        values = new int[]{entry.budget(), entry.respawnSeconds(), entry.ticketCost()};
        carry = entry.carry();
        paintjobs = entry.paintjobs();
        this.templates.put("", "");
        templates.forEach(template -> this.templates.put(template.id(), template.name()));
        this.templates.putIfAbsent(carry, carry);
    }

    @Override
    protected void init()
    {
        int left = width / 2 - 150;
        for (int i = 0; i < keys.length; i++)
        {
            final int index = i;
            EditBox box = new EditBox(font, left + 210, 42 + i * 30, 90, 20, Component.translatable(PREFIX + keys[i]));
            box.setMaxLength(10);
            box.setValue(Integer.toString(values[i]));
            box.setFilter(text -> text.isEmpty() || validNumber(text));
            box.setResponder(text -> values[index] = text.isEmpty() ? 0 : Integer.parseInt(text));
            addRenderableWidget(box);
        }
        addRenderableWidget(CycleButton.<String>builder(id -> id.isEmpty() ? Component.translatable(PREFIX + "none") : Component.literal(templates.get(id)))
            .withValues(new ArrayList<>(templates.keySet())).withInitialValue(carry).create(left, 136, 300, 20, Component.translatable(PREFIX + "carry"), (button, value) -> carry = value));
        addRenderableWidget(Button
            .builder(Component.translatable(PREFIX + "paintjobs"), button -> Minecraft.getInstance().setScreen(new BotPaintjobsScreen(this, entry.type(), paintjobs, selected -> paintjobs = selected)))
            .bounds(left, 160, 300, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button ->
        {
            save.accept(new BotSettings.VehicleEntry(entry.type(), entry.count(), values[0], values[1], values[2], entry.spawner(), carry, paintjobs));
            onClose();
        }).bounds(left, height - 28, 145, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(left + 155, height - 28, 145, 20).build());
    }

    private static boolean validNumber(String text)
    {
        try
        {
            return Integer.parseInt(text) >= 0;
        }
        catch (NumberFormatException ignored)
        {
            return false;
        }
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        for (int i = 0; i < keys.length; i++)
            graphics.drawString(font, Component.translatable(PREFIX + keys[i]), width / 2 - 150, 48 + i * 30, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
