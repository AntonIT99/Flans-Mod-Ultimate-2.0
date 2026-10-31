package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.platform.client.NpcClientPlatform;
import com.wolffsmod.npcs.teams.BotSettingsText;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * A choice the bot editor opens instead of asking for a line of text: what may be chosen as a list, filtered by a search
 * and shown a page at a time, with a preview beside it of the entry under the mouse, or of the last one clicked. The
 * editor's settings change only with Done; Cancel and Escape go back to the editor without changing anything. Client
 * side.
 */
public abstract class BotPickerScreen extends Screen
{
    protected static final String PREFIX = "wolffsmodnpcs.picker.";
    protected static final int BUTTON_HEIGHT = 20;
    protected static final int GAP = 4;
    /** Width of a number with a button on either side to lower and raise it. */
    protected static final int STEPPER_WIDTH = 58;
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_WIDTH = 460;
    private static final int MAX_PREVIEW_WIDTH = 140;
    private static final int TITLE_Y = 6;
    private static final int HINT_Y = 18;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_HINT_LINES = 2;
    /** Height under the list: the summary and page row, then the Done row. */
    private static final int FOOTER_HEIGHT = 54;
    private static final int DONE_WIDTH = 80;
    private static final int PAGE_BUTTON_WIDTH = 20;
    private static final int PAGE_LABEL_WIDTH = 64;
    private static final int STEP_BUTTON_WIDTH = 14;
    private static final int NUMBER_WIDTH = 28;
    /** Height the preview model needs at least to be worth drawing. */
    private static final int MIN_MODEL_HEIGHT = 56;
    /** Model height, in pixels, per unit of the entity display's zoom. */
    private static final float MODEL_PIXELS_PER_ZOOM = 70F;
    private static final float MAX_ZOOM = 2.2F;
    private static final long DOUBLE_CLICK_MILLIS = 300L;
    private static final int PANEL_COLOUR = 0x70000000;
    private static final int TEXT_COLOUR = 0xE0E0E0;
    private static final int HINT_COLOUR = 0xA0A0A0;

    private final BotEditorScreen parent;
    private final Component hint;
    private List<Choice> choices = List.of();
    private List<Choice> shown = List.of();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final Map<String, Button> nameButtons = new HashMap<>();
    @Nullable
    private EditBox search;
    @Nullable
    private Button previousPage;
    @Nullable
    private Button nextPage;
    private String filter = "";
    private int page;
    /** The entry clicked last, previewed while the mouse is over no entry. */
    @Nullable
    private Choice focused;
    @Nullable
    private Choice lastClicked;
    private long lastClickTime;
    private List<FormattedCharSequence> hintLines = List.of();
    // Where the parts of the screen are, worked out for its size in init.
    @Getter(AccessLevel.PROTECTED)
    private int left;
    @Getter(AccessLevel.PROTECTED)
    private int listWidth;
    @Getter(AccessLevel.PROTECTED)
    private int listTop;
    private int listBottom;
    private int rowsPerPage;
    @Getter(AccessLevel.PROTECTED)
    private int previewLeft;
    @Getter(AccessLevel.PROTECTED)
    private int previewWidth;

    /**
     * An entry of the list.
     *
     * @param id
     *            what choosing it stands for, unique in the list
     * @param name
     *            what the list shows
     * @param details
     *            what the preview and the tooltip tell about it below its name
     * @param warning
     *            whether it will not work as it stands, such as a class the team does not offer
     */
    protected record Choice(String id, Component name, List<Component> details, boolean warning)
    {
        public Choice
        {
            details = List.copyOf(details);
        }
    }

    protected BotPickerScreen(BotEditorScreen parent, Component title, Component hint)
    {
        super(title);
        this.parent = parent;
        this.hint = hint;
    }

    // --- What a picker shows and does ---

    /** Takes the picker's choices into the editor's settings, on Done. */
    protected abstract void apply();

    /** Width the row controls of {@link #addControls} take on the right of each entry; 0 for none. */
    protected int controlsWidth()
    {
        return 0;
    }

    /** Adds the controls of an entry's row, from {@code x} to the end of the list, with {@link #addRowWidget}. */
    protected void addControls(Choice choice, int x, int y)
    {
        // Entries are only clicked by default.
    }

    /** Height above the list for the column headings of the row controls, which {@link #addHeadings} adds. */
    protected int headingHeight()
    {
        return 0;
    }

    /** Adds the column headings of the row controls, the controls starting at {@code x}. */
    protected void addHeadings(int x, int y)
    {
        // No headings by default.
    }

    /** Adds controls in the space right of the search box, as wide as the preview. */
    protected void addHeader(int x, int y, int width)
    {
        // Nothing beside the search box by default.
    }

    /** Adds controls in the space left of Done and Cancel. */
    protected void addFooter(int x, int y, int width)
    {
        // Nothing beside Done and Cancel by default.
    }

    /** Whether the entry is chosen, marked with a tick. */
    protected boolean isChosen(Choice choice)
    {
        return false;
    }

    /** What clicking an entry does besides previewing it. */
    protected void clicked(Choice choice)
    {
        // Entries are only previewed by default.
    }

    /** Whether a double click on an entry, after choosing it, is Done. */
    protected boolean doubleClickIsDone()
    {
        return false;
    }

    /**
     * @param choice
     *            the entry, which pickers without suffixes ignore
     * @return text after the entry's name, such as its share
     */
    protected Component suffix(Choice choice)
    {
        return Component.empty();
    }

    /** The model that previews an entry, or null for none. */
    @Nullable
    protected LivingEntity preview(Choice choice)
    {
        return null;
    }

    /**
     * @param choice
     *            the entry without a model, which pickers telling the same for every entry ignore
     * @return what the preview shows instead of a model
     */
    protected Component previewStatus(Choice choice)
    {
        return Component.translatable(PREFIX + "no_preview");
    }

    /** Lines under the list about the choice as a whole. */
    protected List<Component> summary()
    {
        return List.of();
    }

    // --- The list ---

    /** Replaces what the list offers, keeping the search, the page as far as it goes and the entry previewed. */
    protected final void setChoices(List<Choice> offered)
    {
        choices = List.copyOf(offered);
        if (focused != null)
            focused = choices.stream().filter(choice -> choice.id().equals(focused.id())).findFirst().orElse(null);
        if (focused == null)
            focused = choices.stream().filter(this::isChosen).findFirst().orElse(null);
        applyFilter();
        if (minecraft != null)
            showPage();
    }

    /** Shows the current page again, after a change the rows' controls must show. */
    protected final void refreshPage()
    {
        if (minecraft != null)
            showPage();
    }

    /** Shows again whether each entry on the page is chosen, and its suffix. */
    protected final void refreshNames()
    {
        for (Choice choice : shown)
        {
            Button button = nameButtons.get(choice.id());
            if (button != null)
                button.setMessage(label(choice));
        }
    }

    /** Adds a control of the page's rows, which goes when the page is left. */
    protected final <T extends AbstractWidget> T addRowWidget(T widget)
    {
        rowWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    /**
     * Adds a whole number from 0 to {@code max} that can be typed or lowered and raised with the buttons beside it, by 10
     * with Shift held, {@link #STEPPER_WIDTH} wide.
     */
    protected final void addStepper(int x, int y, int value, int max, IntConsumer setter, Component help)
    {
        Tooltip tooltip = Tooltip.create(help.copy().append("\n").append(Component.translatable(PREFIX + "stepper")));
        EditBox box = new EditBox(font, x + STEP_BUTTON_WIDTH + 1, y + 1, NUMBER_WIDTH, BUTTON_HEIGHT - 2, help);
        box.setMaxLength(Integer.toString(max).length());
        box.setFilter(text -> text.chars().allMatch(Character::isDigit));
        box.setValue(Integer.toString(value));
        box.setResponder(text ->
        {
            setter.accept(Math.min(max, Math.max(0, BotSettingsText.parseNumber(text, 0))));
            refreshNames();
        });
        box.setTooltip(tooltip);
        addRowWidget(Button.builder(Component.literal("-"), button -> step(box, -1, max)).bounds(x, y, STEP_BUTTON_WIDTH, BUTTON_HEIGHT).tooltip(tooltip).build());
        addRowWidget(box);
        addRowWidget(
            Button.builder(Component.literal("+"), button -> step(box, 1, max)).bounds(x + STEP_BUTTON_WIDTH + NUMBER_WIDTH + 2, y, STEP_BUTTON_WIDTH, BUTTON_HEIGHT).tooltip(tooltip).build());
    }

    private static void step(EditBox box, int direction, int max)
    {
        int step = hasShiftDown() ? 10 : 1;
        box.setValue(Integer.toString(Mth.clamp(BotSettingsText.parseNumber(box.getValue(), 0) + direction * step, 0, max)));
    }

    // --- Layout ---

    @Override
    protected void init()
    {
        rowWidgets.clear();
        nameButtons.clear();
        int contentWidth = Math.min(MAX_WIDTH, width - 16);
        left = (width - contentWidth) / 2;
        previewWidth = Math.min(MAX_PREVIEW_WIDTH, contentWidth / 3);
        previewLeft = left + contentWidth - previewWidth;
        listWidth = contentWidth - previewWidth - GAP;
        hintLines = font.split(hint, contentWidth).stream().limit(MAX_HINT_LINES).toList();
        int searchY = HINT_Y + hintLines.size() * LINE_HEIGHT + 2;
        listTop = searchY + BUTTON_HEIGHT + GAP + headingHeight();
        listBottom = height - FOOTER_HEIGHT;
        rowsPerPage = Math.max(1, (listBottom - listTop + ROW_HEIGHT - BUTTON_HEIGHT) / ROW_HEIGHT);

        String typed = search == null ? filter : search.getValue();
        search = new EditBox(font, left + 1, searchY + 1, listWidth - 2, BUTTON_HEIGHT - 2, Component.translatable(PREFIX + "search"));
        search.setHint(Component.translatable(PREFIX + "search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(typed);
        search.setResponder(text ->
        {
            filter = text;
            page = 0;
            applyFilter();
            showPage();
        });
        search.setTooltip(Tooltip.create(Component.translatable(PREFIX + "search.tip")));
        addRenderableWidget(search);
        addHeader(previewLeft, searchY, previewWidth);
        if (headingHeight() > 0 && controlsWidth() > 0)
            addHeadings(left + listWidth - controlsWidth(), listTop - headingHeight());

        int pageRow = listBottom + GAP;
        int pageLeft = left + listWidth - 2 * PAGE_BUTTON_WIDTH - PAGE_LABEL_WIDTH;
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1)).bounds(pageLeft, pageRow, PAGE_BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(PREFIX + "page.tip"))).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1)).bounds(pageLeft + PAGE_BUTTON_WIDTH + PAGE_LABEL_WIDTH, pageRow, PAGE_BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(PREFIX + "page.tip"))).build());

        int doneRow = height - BUTTON_HEIGHT - 6;
        int doneLeft = left + contentWidth - 2 * DONE_WIDTH - GAP;
        addFooter(left, doneRow, doneLeft - left - GAP);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done()).bounds(doneLeft, doneRow, DONE_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(doneLeft + DONE_WIDTH + GAP, doneRow, DONE_WIDTH, BUTTON_HEIGHT).build());
        showPage();
    }

    private void applyFilter()
    {
        String wanted = filter.strip().toLowerCase(Locale.ROOT);
        shown = wanted.isEmpty()
            ? choices
            : choices.stream().filter(choice -> choice.id().toLowerCase(Locale.ROOT).contains(wanted) || choice.name().getString().toLowerCase(Locale.ROOT).contains(wanted)).toList();
    }

    private int pageCount()
    {
        return Math.max(1, (shown.size() + rowsPerPage - 1) / rowsPerPage);
    }

    private void turnPage(int step)
    {
        page = Mth.clamp(page + step, 0, pageCount() - 1);
        showPage();
    }

    private void showPage()
    {
        rowWidgets.forEach(this::removeWidget);
        rowWidgets.clear();
        nameButtons.clear();
        page = Mth.clamp(page, 0, pageCount() - 1);
        int controls = controlsWidth();
        int nameWidth = controls > 0 ? listWidth - controls - GAP : listWidth;
        for (int row = 0; row < rowsPerPage; row++)
        {
            int index = page * rowsPerPage + row;
            if (index >= shown.size())
                break;
            Choice choice = shown.get(index);
            int y = listTop + row * ROW_HEIGHT;
            Button name = addRowWidget(Button.builder(label(choice), button -> click(choice)).bounds(left, y, nameWidth, BUTTON_HEIGHT).tooltip(Tooltip.create(describe(choice))).build());
            nameButtons.put(choice.id(), name);
            if (controls > 0)
                addControls(choice, left + nameWidth + GAP, y);
        }
        if (previousPage != null)
            previousPage.active = page > 0;
        if (nextPage != null)
            nextPage.active = page < pageCount() - 1;
    }

    private void click(Choice choice)
    {
        long now = Util.getMillis();
        boolean twice = choice.equals(lastClicked) && now - lastClickTime < DOUBLE_CLICK_MILLIS;
        lastClicked = choice;
        lastClickTime = now;
        focused = choice;
        clicked(choice);
        refreshNames();
        if (twice && doubleClickIsDone())
            done();
    }

    private Component label(Choice choice)
    {
        MutableComponent label = Component.empty();
        if (isChosen(choice))
            label.append(Component.literal("✔ ").withStyle(ChatFormatting.GREEN));
        label.append(choice.warning() ? choice.name().copy().withStyle(ChatFormatting.GOLD) : choice.name().copy());
        return label.append(suffix(choice));
    }

    private static Component describe(Choice choice)
    {
        MutableComponent text = choice.name().copy().withStyle(choice.warning() ? ChatFormatting.GOLD : ChatFormatting.WHITE);
        for (Component line : choice.details())
            text.append("\n").append(line);
        return text;
    }

    @Nullable
    private Choice hovered(double mouseX, double mouseY)
    {
        if (mouseX < left || mouseX >= left + listWidth || mouseY < listTop)
            return null;
        int offset = (int) mouseY - listTop;
        int row = offset / ROW_HEIGHT;
        int index = page * rowsPerPage + row;
        return row < rowsPerPage && index < shown.size() && offset % ROW_HEIGHT < BUTTON_HEIGHT ? shown.get(index) : null;
    }

    // --- Leaving ---

    private void done()
    {
        apply();
        back();
    }

    /** Goes back to the editor; Escape and Cancel change nothing. */
    @Override
    public void onClose()
    {
        back();
    }

    private void back()
    {
        if (minecraft != null)
            minecraft.setScreen(parent);
    }

    /** @return the editor the picker returns to */
    BotEditorScreen parent()
    {
        return parent;
    }

    // --- Input and drawing ---

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (delta != 0 && pageCount() > 1 && mouseX >= left && mouseX < left + listWidth && mouseY >= listTop && mouseY < listBottom)
        {
            turnPage(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        NpcClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, TITLE_Y, 0xFFFFFF);
        int y = HINT_Y;
        for (FormattedCharSequence line : hintLines)
        {
            graphics.drawCenteredString(font, line, width / 2, y, HINT_COLOUR);
            y += LINE_HEIGHT;
        }
        if (shown.isEmpty())
            graphics.drawString(font, Component.translatable(choices.isEmpty() ? PREFIX + "empty" : PREFIX + "no_match"), left + GAP, listTop + 6, HINT_COLOUR);
        int pageLeft = left + listWidth - PAGE_BUTTON_WIDTH - PAGE_LABEL_WIDTH;
        graphics.drawCenteredString(font, Component.translatable(PREFIX + "page", page + 1, pageCount()), pageLeft + PAGE_LABEL_WIDTH / 2, listBottom + GAP + 6, TEXT_COLOUR);
        int summaryWidth = listWidth - 2 * PAGE_BUTTON_WIDTH - PAGE_LABEL_WIDTH - GAP;
        int line = 0;
        for (Component text : summary())
            for (FormattedCharSequence part : font.split(text, summaryWidth))
                if (line < 2)
                    graphics.drawString(font, part, left, listBottom + GAP + 1 + LINE_HEIGHT * line++, HINT_COLOUR);
        renderPreview(graphics, mouseX, mouseY);
    }

    /** The preview panel: the entry's name, its model and what it tells about itself, as far as they fit. */
    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int right = previewLeft + previewWidth;
        graphics.fill(previewLeft, listTop, right, listBottom, PANEL_COLOUR);
        Choice hover = hovered(mouseX, mouseY);
        Choice choice = hover != null ? hover : focused;
        int textWidth = previewWidth - 2 * GAP;
        if (choice == null)
        {
            List<FormattedCharSequence> lines = font.split(Component.translatable(PREFIX + "preview_hint"), textWidth);
            int y = (listTop + listBottom) / 2 - lines.size() * LINE_HEIGHT / 2;
            for (FormattedCharSequence line : lines)
            {
                graphics.drawCenteredString(font, line, previewLeft + previewWidth / 2, y, HINT_COLOUR);
                y += LINE_HEIGHT;
            }
            return;
        }
        int y = listTop + GAP;
        for (FormattedCharSequence line : font.split(choice.name(), textWidth).stream().limit(2).toList())
        {
            graphics.drawString(font, line, previewLeft + GAP, y, choice.warning() ? 0xFFAA00 : 0xFFFFFF);
            y += LINE_HEIGHT;
        }
        LivingEntity entity = preview(choice);
        List<FormattedCharSequence> details = new ArrayList<>();
        for (Component detail : choice.details())
            details.addAll(font.split(detail, textWidth));
        int roomForDetails = listBottom - y - GAP - (entity != null ? MIN_MODEL_HEIGHT : LINE_HEIGHT * 2);
        int shownDetails = Math.max(0, Math.min(details.size(), roomForDetails / LINE_HEIGHT));
        int modelBottom = listBottom - GAP - shownDetails * LINE_HEIGHT;
        if (entity != null)
            renderModel(graphics, entity, y, modelBottom, mouseX, mouseY);
        else
            graphics.drawCenteredString(font, previewStatus(choice), previewLeft + previewWidth / 2, (y + modelBottom) / 2 - 4, HINT_COLOUR);
        for (int i = 0; i < shownDetails; i++)
            graphics.drawString(font, details.get(i), previewLeft + GAP, modelBottom + i * LINE_HEIGHT, TEXT_COLOUR);
    }

    private void renderModel(GuiGraphics graphics, LivingEntity entity, int top, int bottom, int mouseX, int mouseY)
    {
        float zoom = Math.min(MAX_ZOOM, (bottom - top) / MODEL_PIXELS_PER_ZOOM);
        graphics.enableScissor(previewLeft, top, previewLeft + previewWidth, bottom);
        try
        {
            NpcClientPlatform.drawNpc(graphics, entity, previewLeft + previewWidth / 2, bottom - GAP, zoom, mouseX, mouseY);
        }
        finally
        {
            graphics.disableScissor();
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
