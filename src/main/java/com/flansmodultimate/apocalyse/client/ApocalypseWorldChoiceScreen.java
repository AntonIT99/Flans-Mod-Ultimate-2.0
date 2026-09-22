package com.flansmodultimate.apocalyse.client;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Asks the player whether a world gets the Apocalypse dimension, spelling out that a world with
 * it can no longer be opened once the mod is removed. There is no default answer to click past:
 * the player picks one of the two, or backs out.
 */
public class ApocalypseWorldChoiceScreen extends Screen
{
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private final Component message;
    private final Component withLabel;
    private final Component withoutLabel;
    private final Consumer<Boolean> onChoice;
    private final Runnable onCancel;
    private MultiLineLabel messageLines = MultiLineLabel.EMPTY;

    private ApocalypseWorldChoiceScreen(Component message, Component withLabel, Component withoutLabel,
                                        Consumer<Boolean> onChoice, Runnable onCancel)
    {
        super(Component.translatable("gui.flansmodultimate.apocalypse_choice.title"));
        this.message = message;
        this.withLabel = withLabel;
        this.withoutLabel = withoutLabel;
        this.onChoice = onChoice;
        this.onCancel = onCancel;
    }

    /** Asked when a new world is about to be created. */
    public static ApocalypseWorldChoiceScreen forNewWorld(Consumer<Boolean> onChoice, Runnable onCancel)
    {
        return new ApocalypseWorldChoiceScreen(
            Component.translatable("gui.flansmodultimate.apocalypse_choice.new_world"),
            Component.translatable("gui.flansmodultimate.apocalypse_choice.create_with"),
            Component.translatable("gui.flansmodultimate.apocalypse_choice.create_without"),
            onChoice, onCancel);
    }

    /** Asked once when a saved world without the Apocalypse is opened. */
    public static ApocalypseWorldChoiceScreen forExistingWorld(Consumer<Boolean> onChoice, Runnable onCancel)
    {
        return new ApocalypseWorldChoiceScreen(
            Component.translatable("gui.flansmodultimate.apocalypse_choice.existing_world"),
            Component.translatable("gui.flansmodultimate.apocalypse_choice.add"),
            Component.translatable("gui.flansmodultimate.apocalypse_choice.keep_without"),
            onChoice, onCancel);
    }

    @Override
    protected void init()
    {
        messageLines = MultiLineLabel.create(font, message, width - 50);
        int buttonY = contentTop() + messageLines.getLineCount() * font.lineHeight + 20;
        int x = (width - BUTTON_WIDTH) / 2;
        addRenderableWidget(Button.builder(withLabel, button -> onChoice.accept(true))
            .bounds(x, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(withoutLabel, button -> onChoice.accept(false))
            .bounds(x, buttonY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onCancel.run())
            .bounds(x, buttonY + 2 * BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        int top = contentTop();
        graphics.drawCenteredString(font, title, width / 2, top - 20, 0xFFFFFF);
        messageLines.renderCentered(graphics, width / 2, top);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Vertically centres the message and the three buttons below it. */
    private int contentTop()
    {
        int contentHeight = messageLines.getLineCount() * font.lineHeight + 20 + 3 * BUTTON_SPACING;
        return Math.max(40, (height - contentHeight) / 2);
    }

    @Override
    public void onClose()
    {
        onCancel.run();
    }
}
