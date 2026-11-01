package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcSkill;
import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotSettingsText;
import com.wolffsmod.npcs.teams.BotTraining;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * Edits the values of the custom bot difficulty of a round, skirmish or war: every value a preset level sets, typed, and
 * filled in from a preset with one click. Done hands the values on and goes back; Cancel and Escape go back without
 * changing anything. Values out of bounds are kept within them. Client side.
 */
public final class BotDifficultyScreen extends Screen
{
    private static final String PREFIX = "wolffsmodnpcs.difficulty_editor.";
    private static final String TIP = ".tip";
    private static final int ROW_HEIGHT = 22;
    private static final int COLUMN_WIDTH = 168;
    private static final int GAP = 4;
    private static final int BOX_WIDTH = 46;
    private static final int TOP = 40;
    private static final int LABEL_COLOUR = 0xE0E0E0;
    private static final int HINT_COLOUR = 0xA0A0A0;
    // The typed values, in the order the command takes them.
    private static final String ACCURACY = "accuracy";
    private static final String AGGRO = "aggro";
    private static final String REACTION = "reaction";
    private static final String SETTLE = "settle";
    private static final String ERROR = "error";
    private static final String LEAD = "lead";
    private static final String PLAYER_DAMAGE = "player_damage";

    private final Screen parent;
    private final Consumer<BotTraining> onDone;
    /** The values as shown, taken from the fields whenever the page is built again. */
    private BotTraining training;
    private final Map<String, EditBox> fields = new LinkedHashMap<>();
    private final List<Label> labels = new ArrayList<>();

    private record Label(Component text, int x, int y)
    {}

    /**
     * @param onDone
     *            what takes the values, after the parent is shown again
     */
    public BotDifficultyScreen(@Nullable Screen parent, BotTraining training, Consumer<BotTraining> onDone)
    {
        super(Component.translatable(PREFIX + "title"));
        this.parent = parent;
        this.training = training;
        this.onDone = onDone;
    }

    @Override
    protected void init()
    {
        fields.clear();
        labels.clear();
        int left = left();
        int width = 2 * COLUMN_WIDTH + GAP;
        // A row of the preset levels to fill the values in from.
        List<BotSettings.Difficulty> presets = BotSettings.Difficulty.presets();
        int presetWidth = (width - (presets.size() - 1) * 2) / presets.size();
        for (int i = 0; i < presets.size(); i++)
        {
            BotSettings.Difficulty level = presets.get(i);
            String name = level.name().toLowerCase(Locale.ROOT);
            addRenderableWidget(Button.builder(Component.translatable("wolffsmodnpcs.centre.difficulty." + name), button -> fill(level)).bounds(left + i * (presetWidth + 2), TOP, presetWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "fill.tip", Component.translatable("wolffsmodnpcs.centre.difficulty." + name)))).build());
        }
        NpcSkill skill = training.skill();
        int second = left + COLUMN_WIDTH + GAP;
        addField(left, 1, ACCURACY, Integer.toString(training.accuracyPercent()));
        addField(second, 1, AGGRO, Integer.toString(training.aggroPercent()));
        addField(left, 2, REACTION, Integer.toString(skill.reactionTicks()));
        addField(second, 2, SETTLE, Integer.toString(skill.settleTicks()));
        addField(left, 3, ERROR, Float.toString(skill.startError()));
        addField(second, 3, LEAD, Integer.toString(training.leadPercent()));
        addField(left, 4, PLAYER_DAMAGE, Integer.toString(training.playerDamagePercent()));
        addRenderableWidget(CycleButton.onOffBuilder(skill.nonLethal()).withTooltip(chosen -> tip("non_lethal")).create(second, y(4), COLUMN_WIDTH, 20, Component.translatable(PREFIX + "non_lethal"),
            (button, chosen) -> training = read(chosen)));
        int half = COLUMN_WIDTH;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done()).bounds(left, height - 30, half, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(second, height - 30, half, 20).build());
    }

    private int left()
    {
        return width / 2 - COLUMN_WIDTH - GAP / 2;
    }

    private static int y(int row)
    {
        return TOP + row * ROW_HEIGHT;
    }

    private static Tooltip tip(String key)
    {
        return Tooltip.create(Component.translatable(PREFIX + key + TIP));
    }

    /** A value of a column: its label on the left, the box on the right. */
    private void addField(int x, int row, String key, String value)
    {
        EditBox box = new EditBox(font, x + COLUMN_WIDTH - BOX_WIDTH, y(row), BOX_WIDTH, 20, Component.translatable(PREFIX + key));
        box.setValue(value);
        box.setTooltip(tip(key));
        fields.put(key, addRenderableWidget(box));
        labels.add(new Label(Component.translatable(PREFIX + key), x, y(row) + 6));
    }

    private void fill(BotSettings.Difficulty level)
    {
        training = level.training(training);
        rebuildWidgets();
    }

    /** @return the values the fields hold, each as it was where its text is no number */
    private BotTraining read(boolean nonLethal)
    {
        NpcSkill skill = training.skill();
        return BotTraining.of(number(ACCURACY, training.accuracyPercent()), number(AGGRO, training.aggroPercent()), number(REACTION, skill.reactionTicks()), number(SETTLE, skill.settleTicks()),
            BotSettingsText.parseDecimal(text(ERROR), skill.startError()), number(LEAD, training.leadPercent()), number(PLAYER_DAMAGE, training.playerDamagePercent()), nonLethal);
    }

    private String text(String key)
    {
        EditBox box = fields.get(key);
        return box == null ? "" : box.getValue();
    }

    private int number(String key, int fallback)
    {
        return BotSettingsText.parseNumber(text(key), fallback);
    }

    private void done()
    {
        BotTraining chosen = read(training.skill().nonLethal());
        onClose();
        onDone.accept(chosen);
    }

    @Override
    public void onClose()
    {
        if (minecraft != null)
            minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(PREFIX + "hint"), width / 2, 24, HINT_COLOUR);
        for (Label label : labels)
            graphics.drawString(font, label.text(), label.x(), label.y(), LABEL_COLOUR);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
