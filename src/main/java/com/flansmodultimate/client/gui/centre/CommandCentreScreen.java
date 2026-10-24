package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.CommandCentreRow;
import com.flansmodultimate.api.client.ICommandCentrePage;
import com.flansmodultimate.api.client.ICommandCentreView;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.common.teams.centre.CommandCentre;
import com.flansmodultimate.common.teams.centre.TeamsSection;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketCommandCentreAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.util.FlansLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Consumer;

/**
 * The Teams command centre: a frame with the pages in a side bar, the page shown in the content area and the output of
 * the last command in a status bar. What it shows is the state the server sent last; it asks for new state every few
 * seconds and after every command, and lays the page out again when the state changed, keeping what the operator is
 * typing. Client only.
 */
@SuppressWarnings("DataFlowIssue") // Screen initializes minecraft and font before init, callbacks and rendering.
public final class CommandCentreScreen extends Screen
{
    private static final int MAX_WIDTH = 640;
    private static final int MAX_HEIGHT = 360;
    private static final int HEADER = 22;
    private static final int FOOTER = 16;
    private static final int SIDEBAR_WIDE = 104;
    private static final int SIDEBAR_NARROW = 26;
    /** The narrowest frame that still shows the pages' names in the side bar. */
    private static final int WIDE_FRAME = 450;
    private static final int ENTRY_HEIGHT = 20;
    private static final int PADDING = 6;
    private static final int FIELD_LENGTH = 256;
    private static final int REFRESH_TICKS = 60;
    private static final int STATUS_TICKS = 240;
    private static final int FADE_TICKS = 40;
    private static final String KEY = "gui.flansmodultimate.centre.";

    /** The text of a field, and whether the operator typed it rather than it showing what the server holds. */
    private record Field(String value, boolean dirty)
    {}

    /** Output shown in the status bar since a client tick. */
    private record Status(List<CommandCentre.Feedback> lines, int shownAt)
    {}

    private final View view = new View();
    private final ItemStack headerIcon = new ItemStack(Items.SHIELD);
    /** The fields of each page by key, kept while the centre is open. */
    private final Map<String, Map<String, Field>> fields = new HashMap<>();
    /** The fields of the page as it is laid out. */
    private final Map<String, EditBox> boxes = new HashMap<>();
    /** How far each list of each page is scrolled, in pixels. */
    private final Map<String, Integer> scrolls = new HashMap<>();
    private List<ICommandCentrePage> shown = List.of();
    @Nullable
    private ICommandCentrePage page;
    @Nullable
    private Status status;
    /** The page that ran the command whose output is awaited. */
    @Nullable
    private String commandPage;
    /** The state the page was laid out from, without the clocks that tick by between updates. */
    private CompoundTag laidOutFrom = new CompoundTag();
    private int frameLeft;
    private int frameTop;
    private int frameWidth;
    private int frameHeight;
    private int sidebarWidth;
    private int ticks;
    private int sinceRefresh;

    public CommandCentreScreen()
    {
        super(Component.translatable(KEY + "title"));
    }

    @Override
    protected void init()
    {
        boxes.clear();
        absorbFeedback();
        frameWidth = Math.min(width - 12, MAX_WIDTH);
        frameHeight = Math.min(height - 12, MAX_HEIGHT);
        frameLeft = (width - frameWidth) / 2;
        frameTop = (height - frameHeight) / 2;
        sidebarWidth = frameWidth >= WIDE_FRAME ? SIDEBAR_WIDE : SIDEBAR_NARROW;
        shown = CommandCentreClient.pages().stream().filter(this::available).toList();
        page = shown.stream().filter(candidate -> candidate.getId().equals(CommandCentreClient.shownPage())).findFirst().orElse(shown.isEmpty() ? null : shown.get(0));
        for (int i = 0; i < shown.size(); i++)
            addRenderableWidget(new SidebarEntry(frameLeft, frameTop + HEADER + 4 + i * ENTRY_HEIGHT, sidebarWidth, shown.get(i)));
        Button close = addRenderableWidget(Button.builder(Component.literal("✕"), button -> onClose()).bounds(frameLeft + frameWidth - 19, frameTop + 3, 16, 16).build());
        close.setTooltip(Tooltip.create(Component.translatable(KEY + "close")));
        laidOutFrom = layoutState();
        if (page == null)
            return;
        try
        {
            page.init(view);
        }
        catch (RuntimeException e)
        {
            FlansLog.log.error("Teams command centre page {} failed to lay out", page.getId(), e);
            view.showMessage(Component.translatable(KEY + "page_failed"), true);
        }
    }

    private boolean available(ICommandCentrePage candidate)
    {
        try
        {
            return candidate.isAvailable(view);
        }
        catch (RuntimeException e)
        {
            FlansLog.log.error("Teams command centre page {} failed", candidate.getId(), e);
            return false;
        }
    }

    /** Shows the output of commands that arrived, and lets the fields of a page whose command succeeded follow the server again. */
    private void absorbFeedback()
    {
        List<CommandCentre.Feedback> lines = CommandCentreClient.takeFeedback();
        if (lines.isEmpty())
            return;
        status = new Status(lines, ticks);
        if (commandPage != null && lines.stream().noneMatch(CommandCentre.Feedback::failure))
            Optional.ofNullable(fields.get(commandPage)).ifPresent(pageFields -> pageFields.replaceAll((key, field) -> new Field(field.value(), false)));
        commandPage = null;
    }

    /** New state arrived: lays the page out again when it changed, keeping the field being typed in. */
    void stateChanged()
    {
        if (layoutState().equals(laidOutFrom) && commandPage == null)
        {
            absorbFeedback();
            return;
        }
        relayout(true);
    }

    /** The sections, but for the clocks of the round being played, which are counted on between updates. */
    private static CompoundTag layoutState()
    {
        CompoundTag state = CommandCentreClient.sections().copy();
        CompoundTag round = state.getCompound(TeamsSection.KEY.toString()).getCompound(TeamsSection.ROUND);
        round.remove(TeamsSection.TIME_LEFT);
        round.remove(TeamsSection.INTERMISSION_LEFT);
        return state;
    }

    private void relayout(boolean keepFocus)
    {
        String focusedKey = null;
        int cursor = 0;
        if (keepFocus && getFocused() instanceof EditBox focused)
        {
            for (Map.Entry<String, EditBox> entry : boxes.entrySet())
            {
                if (entry.getValue() == focused)
                {
                    focusedKey = entry.getKey();
                    cursor = focused.getCursorPosition();
                }
            }
        }
        rebuildWidgets();
        EditBox box = focusedKey == null ? null : boxes.get(focusedKey);
        if (box != null)
        {
            setFocused(box);
            box.setCursorPosition(cursor);
        }
    }

    @Override
    public void tick()
    {
        ticks++;
        boxes.values().forEach(ClientPlatform::tick);
        if (++sinceRefresh >= REFRESH_TICKS)
        {
            sinceRefresh = 0;
            PacketHandler.sendToServer(PacketCommandCentreAction.refresh());
        }
        if (page != null)
            page.tick(view);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (!(getFocused() instanceof EditBox) && KeyInputHandler.isCommandCentreKey(keyCode, scanCode))
        {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.fill(frameLeft - 1, frameTop - 1, frameLeft + frameWidth + 1, frameTop + frameHeight + 1, CentreStyle.FRAME_BORDER);
        graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight, CentreStyle.FRAME);
        renderHeader(graphics);
        graphics.fill(frameLeft, frameTop + HEADER, frameLeft + sidebarWidth, frameTop + frameHeight, CentreStyle.SIDEBAR);
        if (page != null)
        {
            try
            {
                page.render(view, graphics, mouseX, mouseY, partialTick);
            }
            catch (RuntimeException e)
            {
                FlansLog.log.error("Teams command centre page {} failed to draw", page.getId(), e);
                page = null;
                view.showMessage(Component.translatable(KEY + "page_failed"), true);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFooter(graphics, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics)
    {
        graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + HEADER, CentreStyle.HEADER);
        graphics.fill(frameLeft, frameTop + HEADER - 1, frameLeft + frameWidth, frameTop + HEADER, 0x80000000 | (CentreStyle.ACCENT & 0xFFFFFF));
        graphics.renderItem(headerIcon, frameLeft + 4, frameTop + 3);
        graphics.drawString(font, title, frameLeft + 24, frameTop + 7, CentreStyle.TEXT);
        int titleRight = frameLeft + 30 + font.width(title);
        Component live = liveChip();
        int chipRight = frameLeft + frameWidth - 24;
        int room = chipRight - titleRight;
        if (room > 40)
        {
            int chipWidth = Math.min(font.width(live), room);
            CentreStyle.drawText(graphics, font, live, chipRight - chipWidth, frameTop + 7, chipWidth, CentreStyle.TEXT);
        }
    }

    /** What the header says of the Teams game: off, the round being played and its clock, or none. */
    private Component liveChip()
    {
        TeamsData data = CommandCentreClient.teams();
        if (!data.enabled())
            return Component.translatable(KEY + "teams_off").withStyle(ChatFormatting.RED);
        Optional<TeamsData.LiveRound> live = data.live();
        if (live.isEmpty())
            return Component.translatable(KEY + "no_round").withStyle(ChatFormatting.GRAY);
        TeamsData.LiveRound round = live.get();
        int elapsed = CommandCentreClient.ticksSinceUpdate();
        if (!"NONE".equals(round.phase()))
            return Component.translatable(KEY + "break", Component.translatable(KEY + "phase." + round.phase().toLowerCase(Locale.ROOT)), CentreStyle.clock(round.intermissionLeft() - elapsed))
                .withStyle(ChatFormatting.GOLD);
        MutableComponent chip = Component.literal("● ").withStyle(ChatFormatting.GREEN);
        return chip.append(Component.translatable(KEY + "live", data.title(round.round()), CentreStyle.clock(round.timeLeft() - elapsed)).withStyle(ChatFormatting.WHITE));
    }

    private void renderFooter(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int top = frameTop + frameHeight - FOOTER;
        int left = frameLeft + sidebarWidth;
        graphics.fill(left, top, frameLeft + frameWidth, frameTop + frameHeight, 0x60000000);
        int textWidth = frameLeft + frameWidth - left - 12;
        int age = status == null ? Integer.MAX_VALUE : ticks - status.shownAt();
        if (status == null || age > STATUS_TICKS)
        {
            Component hint = Component.translatable(KEY + "footer", KeyInputHandler.commandCentreKeyName());
            CentreStyle.drawText(graphics, font, hint, left + 6, top + 4, textWidth, CentreStyle.MUTED);
            return;
        }
        List<CommandCentre.Feedback> lines = status.lines();
        CommandCentre.Feedback headline = lines.stream().filter(CommandCentre.Feedback::failure).findFirst().orElse(lines.get(lines.size() - 1));
        int alpha = age < STATUS_TICKS - FADE_TICKS ? 0xFF : Math.max(0x20, 0xFF * (STATUS_TICKS - age) / FADE_TICKS);
        int colour = (alpha << 24) | ((headline.failure() ? CentreStyle.BAD : CentreStyle.GOOD) & 0xFFFFFF);
        MutableComponent text = Component.literal(headline.failure() ? "✖ " : "✔ ").append(headline.text().copy().withStyle(style -> style.withColor((TextColor) null)));
        if (lines.size() > 1)
            text.append(Component.literal("  (+" + (lines.size() - 1) + ")").withStyle(ChatFormatting.DARK_GRAY));
        CentreStyle.drawText(graphics, font, text, left + 6, top + 4, textWidth, colour);
        if (lines.size() > 1 && mouseX >= left && mouseX < frameLeft + frameWidth && mouseY >= top && mouseY < frameTop + frameHeight)
            graphics.renderComponentTooltip(font, lines.stream().map(CommandCentre.Feedback::text).toList(), mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    /** A page's entry in the side bar: its icon, and its name where the frame is wide enough. */
    private final class SidebarEntry extends AbstractButton
    {
        private final ICommandCentrePage target;
        private final ItemStack icon;
        private final boolean wide;

        SidebarEntry(int x, int y, int width, ICommandCentrePage target)
        {
            super(x, y, width, ENTRY_HEIGHT, target.getTitle());
            this.target = target;
            this.icon = target.getIcon();
            this.wide = width > SIDEBAR_NARROW;
            Component description = target.getDescription();
            MutableComponent tooltip = wide ? Component.empty() : target.getTitle().copy();
            if (!description.getString().isEmpty())
                tooltip.append(wide ? Component.empty() : Component.literal("\n")).append(description.copy().withStyle(ChatFormatting.GRAY));
            if (!tooltip.getString().isEmpty())
                setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onPress()
        {
            view.showPage(target.getId());
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            boolean selected = target == page;
            if (selected)
                graphics.fill(getX(), getY(), getX() + width, getY() + height, CentreStyle.SIDEBAR_SELECTED);
            else if (isHoveredOrFocused())
                graphics.fill(getX(), getY(), getX() + width, getY() + height, CentreStyle.SIDEBAR_HOVER);
            if (selected)
                graphics.fill(getX(), getY(), getX() + 2, getY() + height, CentreStyle.ACCENT);
            graphics.renderItem(icon, getX() + 5, getY() + 2);
            if (wide)
                CentreStyle.drawText(graphics, font, getMessage(), getX() + 25, getY() + 6, width - 28, selected ? CentreStyle.TEXT : CentreStyle.DIM);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput output)
        {
            defaultButtonNarrationText(output);
        }
    }

    /** The centre as its pages see it. */
    private final class View implements ICommandCentreView
    {
        @Override
        public int getLeft()
        {
            return frameLeft + sidebarWidth + PADDING;
        }

        @Override
        public int getTop()
        {
            return frameTop + HEADER + PADDING;
        }

        @Override
        public int getWidth()
        {
            return frameLeft + frameWidth - PADDING - getLeft();
        }

        @Override
        public int getHeight()
        {
            return frameTop + frameHeight - FOOTER - PADDING - getTop();
        }

        @Override
        public Font getFont()
        {
            return font;
        }

        @Override
        public <T extends AbstractWidget> T addWidget(T widget)
        {
            return addRenderableWidget(widget);
        }

        private Map<String, Field> pageFields()
        {
            return fields.computeIfAbsent(page == null ? "" : page.getId(), ignored -> new HashMap<>());
        }

        @Override
        public EditBox addField(String key, int x, int y, int width, String initial, Component hint)
        {
            Map<String, Field> pageFields = pageFields();
            Field kept = pageFields.get(key);
            boolean dirty = kept != null && kept.dirty();
            String value = dirty ? kept.value() : initial;
            pageFields.put(key, new Field(value, dirty));
            // A bordered box draws its border around its bounds, so it is laid inside the area asked for.
            EditBox box = new EditBox(font, x + 1, y + 1, width - 2, 16, hint);
            box.setMaxLength(FIELD_LENGTH);
            box.setValue(value);
            box.moveCursorToStart();
            box.setHint(hint.copy().withStyle(ChatFormatting.DARK_GRAY));
            box.setResponder(text -> pageFields.put(key, new Field(text, true)));
            boxes.put(key, box);
            return addRenderableWidget(box);
        }

        @Override
        public String getField(String key)
        {
            Field field = pageFields().get(key);
            return field == null ? "" : field.value();
        }

        @Override
        public void setField(String key, String value)
        {
            pageFields().put(key, new Field(value, true));
            EditBox box = boxes.get(key);
            if (box != null)
                box.setValue(value);
        }

        @Override
        public void addList(String id, ScreenRectangle area, List<CommandCentreRow> rows, Component empty, Consumer<String> onSelect)
        {
            String scrollKey = (page == null ? "" : page.getId()) + "/" + id;
            addRenderableWidget(new CentreList(area, rows, empty, scrolls.getOrDefault(scrollKey, 0), onSelect, scroll -> scrolls.put(scrollKey, scroll)));
        }

        @Override
        public boolean hasSection(ResourceLocation key)
        {
            return CommandCentreClient.sections().contains(key.toString(), Tag.TAG_COMPOUND);
        }

        @Override
        public CompoundTag getSection(ResourceLocation key)
        {
            return CommandCentreClient.sections().getCompound(key.toString()).copy();
        }

        @Override
        public int getTicksSinceUpdate()
        {
            return CommandCentreClient.ticksSinceUpdate();
        }

        @Override
        public void runCommand(String command)
        {
            commandPage = page == null ? null : page.getId();
            sinceRefresh = 0;
            PacketHandler.sendToServer(PacketCommandCentreAction.run(command));
        }

        @Override
        public void confirm(Component question, Runnable action)
        {
            Minecraft game = minecraft;
            game.setScreen(new ConfirmScreen(confirmed ->
            {
                game.setScreen(CommandCentreScreen.this);
                if (confirmed)
                    action.run();
            }, Component.translatable(KEY + "confirm"), question));
        }

        @Override
        public void showMessage(Component message, boolean failure)
        {
            status = new Status(List.of(new CommandCentre.Feedback(message, failure)), ticks);
        }

        @Override
        public void showPage(String id)
        {
            if (shown.stream().noneMatch(candidate -> candidate.getId().equals(id)))
                return;
            CommandCentreClient.showPage(id);
            relayout(false);
        }

        @Override
        public void rebuild()
        {
            relayout(true);
        }

        @Override
        public void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, @Nullable Component heading)
        {
            CentreStyle.drawPanel(graphics, font, x, y, width, height, heading);
        }
    }
}
