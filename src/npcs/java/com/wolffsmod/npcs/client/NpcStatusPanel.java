package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.parts.IPartBody;
import com.flansmodultimate.api.parts.PartState;
import com.wolffsmod.npcs.diag.NpcStatus;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.NpcStatusPacket;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import com.wolffsmod.npcs.sim.NpcVehicleStateHolder;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The live status of the NPC on the General hub tab, beside and below the Start here pages: a health bar, what the NPC is
 * doing and at whom, how fast it moves and how far from home it is, warnings such as reloading or out of ammunition, and,
 * for a driveable, the hit points of each part as bars and as a top view of the model coloured by damage. Other NPCs show
 * their equipment in place of the top view.
 *
 * <p>
 * Health, the parts and equipment come from the client's own copy of the NPC, which the server keeps in sync; what the NPC
 * is doing comes from {@link NpcStatusView}, which the tab asks the server for while it shows the panel. Client only.
 * </p>
 */
@RequiredArgsConstructor
final class NpcStatusPanel
{
    private static final String KEY = "wolffsmodnpcs.status.";
    private static final String HELP = ".help";
    private static final int HEADING_HEIGHT = 12;
    private static final int BAR_HEIGHT = 12;
    private static final int LINE_HEIGHT = 12;
    private static final int PART_ROW_HEIGHT = 10;
    /** The least and most room for part names, which take the width of the longest one between them. */
    private static final int PART_NAME_MIN_WIDTH = 40;
    private static final int PART_NAME_MAX_WIDTH = 100;
    private static final int PART_COLUMN_GAP = 4;
    private static final int PART_BAR_HEIGHT = 6;
    private static final int CHIP_PADDING = 3;
    private static final int CHIP_GAP = 3;
    private static final int DOT_SIZE = 7;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int MAP_MARGIN = 3;
    private static final int TOOLTIP_WIDTH = 220;
    private static final float KMH_PER_BLOCK_PER_SECOND = 3.6F;
    private static final float PERCENT = 100F;
    /** Client ticks between two requests of what the NPC is doing: four a second, so that the panel follows a fight. */
    private static final int REQUEST_INTERVAL = 5;
    private static final int BORDER = 0xFF1E1E1E;
    private static final int EMPTY = 0xFF5A5A5A;
    private static final int SHINE = 0x40FFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF808080;
    private static final int HOVER = 0x40FFFFFF;
    private static final int BROKEN = 0xFF6A1A1A;
    private static final int UNBREAKABLE = 0xFF4F7FA8;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int MAP_BACKGROUND = 0xFF2A2A2A;
    private static final int RED = 0xFFC83737;
    private static final int AMBER = 0xFFD08A1E;
    private static final int BLUE = 0xFF3C78B4;
    private static final int GREEN = 0xFF3C9A3C;
    private static final int YELLOW = 0xFFC8B428;
    private static final int SLATE = 0xFF6A6A7A;
    private static final int ICE = 0xFF5AA0C8;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Screen screen;
    private final EntityNPCInterface npc;
    /** Region a tooltip belongs to, found once the panel is drawn. */
    private final List<Hover> hovers = new ArrayList<>();
    private int left;
    private int top;
    private int width;
    private int bottom;
    private int pictureLeft;
    private int pictureTop;
    private int pictureWidth;
    /** The first part the list shows, when the parts do not all fit. */
    private int partOffset;
    @Nullable
    private String hoveredPart;
    /** Client ticks the panel has been shown, which time its requests; the first one asks at once. */
    private int ticks;

    private record Hover(int x0, int y0, int x1, int y1, List<FormattedCharSequence> lines)
    {
        private boolean contains(int x, int y)
        {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** Where a part is drawn on the top view. */
    private record MapBox(PartState part, int x0, int y0, int x1, int y1)
    {
        private boolean contains(int x, int y)
        {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** One warning the panel shows as a coloured chip. */
    private record Chip(Component label, Component help, int color)
    {}

    /**
     * Places the panel: the status column to the right of the Start here pages, and the picture below them.
     */
    void layout(int left, int top, int width, int bottom, int pictureLeft, int pictureTop, int pictureWidth)
    {
        this.left = left;
        this.top = top;
        this.width = width;
        this.bottom = bottom;
        this.pictureLeft = pictureLeft;
        this.pictureTop = pictureTop;
        this.pictureWidth = pictureWidth;
    }

    /** Asks the server what the NPC is doing, every few ticks while the panel is shown. */
    void tick()
    {
        if (ticks++ % REQUEST_INTERVAL == 0)
            NpcNetwork.sendToServer(NpcStatusPacket.request(npc.getId()));
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY)
    {
        hovers.clear();
        Font font = Minecraft.getInstance().font;
        int text = CustomNpcsPlatform.defaultTextColor();
        Optional<NpcStatus> status = NpcStatusView.get(npc.getId());
        graphics.drawString(font, Component.translatable(KEY + "heading"), left, top, text, false);
        if (status.isEmpty())
        {
            Component waiting = Component.translatable(KEY + "waiting");
            graphics.drawString(font, waiting, left + width - font.width(waiting), top, GREY, false);
        }
        int y = top + HEADING_HEIGHT;
        healthBar(graphics, font, status.orElse(null), y);
        y += BAR_HEIGHT + 4;
        if (status.isPresent())
        {
            activityLine(graphics, font, status.get(), y, text);
            measures(graphics, font, status.get(), y + LINE_HEIGHT, text);
            chips(graphics, font, status.get(), y + 2 * LINE_HEIGHT);
        }
        y += 3 * LINE_HEIGHT + 2;
        List<PartState> parts = parts();
        hoveredPart = null;
        if (parts.isEmpty())
            equipment(graphics, font, mouseX, mouseY, text);
        else
        {
            partList(graphics, font, parts, y, mouseX, mouseY, text);
            damageMap(graphics, font, parts, mouseX, mouseY, text);
        }
        for (int index = hovers.size() - 1; index >= 0; index--)
        {
            if (hovers.get(index).contains(mouseX, mouseY))
            {
                screen.setTooltipForNextRenderPass(hovers.get(index).lines());
                break;
            }
        }
    }

    /** Scrolls the part list under the mouse. */
    boolean scroll(double mouseX, double mouseY, double delta)
    {
        if (mouseX < left || mouseX >= left + width || mouseY < top || mouseY >= bottom || parts().isEmpty() || delta == 0D)
            return false;
        partOffset += delta > 0D ? -1 : 1;
        return true;
    }

    // Health and activity

    private void healthBar(GuiGraphics graphics, Font font, @Nullable NpcStatus status, int y)
    {
        // The client's copy of a Custom NPC keeps neither its health nor its maximum, which only the server sets.
        boolean killed = npc.isKilled() || status != null && status.activity() == NpcStatus.Activity.KILLED;
        float max = status == null ? 0F : status.maxHealth();
        float health = killed || status == null ? 0F : Mth.clamp(status.health(), 0F, max);
        float share = max > 0F ? health / max : 0F;
        bar(graphics, left, y, width, BAR_HEIGHT, share, killed ? BROKEN : healthColor(share));
        Component label;
        if (killed)
            label = killedLabel(status);
        else if (status == null)
            label = Component.translatable(KEY + "health_unknown");
        else
            label = Component.translatable(KEY + "health", whole(health), whole(max), Math.round(share * PERCENT));
        String shown = cut(font, label.getString(), width - 4);
        graphics.drawString(font, shown, left + (width - font.width(shown)) / 2, y + 2, WHITE, true);
        hover(left, y, left + width, y + BAR_HEIGHT, font, Component.translatable(KEY + "health" + HELP));
    }

    private static Component killedLabel(@Nullable NpcStatus status)
    {
        if (status == null || status.respawnSeconds() == NpcStatus.NEVER)
            return Component.translatable(KEY + "killed");
        if (status.respawnSeconds() == 0)
            return Component.translatable(KEY + "killed_waiting");
        return Component.translatable(KEY + "killed_respawn", status.respawnSeconds());
    }

    /** A coloured dot and what the NPC is doing, with its target. */
    private void activityLine(GuiGraphics graphics, Font font, NpcStatus status, int y, int text)
    {
        NpcStatus.Behaviour behaviour = status.activity() == NpcStatus.Activity.KILLED ? NpcStatus.Behaviour.IDLE : status.behaviour();
        String name = behaviour.name().toLowerCase(Locale.ROOT);
        int dotTop = y + 1;
        graphics.fill(left, dotTop, left + DOT_SIZE, dotTop + DOT_SIZE, BORDER);
        graphics.fill(left + 1, dotTop + 1, left + DOT_SIZE - 1, dotTop + DOT_SIZE - 1, behaviourColor(status));
        MutableComponent line = Component.translatable(KEY + "behaviour." + name);
        if (status.target() != null)
            line.append(Component.translatable(KEY + "target", status.target(), whole(status.targetDistance())));
        graphics.drawString(font, cut(font, line.getString(), width - DOT_SIZE - 4), left + DOT_SIZE + 4, y, text, false);
        MutableComponent help = Component.translatable(KEY + "behaviour." + name + HELP);
        if (status.target() != null)
            help.append("\n").append(Component.translatable(KEY + (status.targetInSight() ? "in_sight" : "out_of_sight"), status.target()));
        hover(left, y, left + width, y + LINE_HEIGHT - 2, font, help);
    }

    private static int behaviourColor(NpcStatus status)
    {
        if (status.activity() == NpcStatus.Activity.KILLED)
            return GREY;
        return switch (status.behaviour())
        {
            case IDLE -> GREEN;
            case ENGAGING, FIRING -> RED;
            case EVADING, RETREATING -> AMBER;
            case IN_COVER, PEEKING -> BLUE;
            case FLANKING, SPREADING, ADVANCING -> YELLOW;
        };
    }

    /** Speed, distance from home and crew, side by side. */
    private void measures(GuiGraphics graphics, Font font, NpcStatus status, int y, int text)
    {
        int x = left;
        x = measure(graphics, font, x, y, text, Component.translatable(KEY + "speed", tenth(status.speed() * KMH_PER_BLOCK_PER_SECOND)), "speed");
        x = measure(graphics, font, x, y, text, Component.translatable(KEY + "home", whole(status.homeDistance())), "home");
        if (status.crewSeats() != NpcStatus.NO_CREW)
            measure(graphics, font, x, y, text, Component.translatable(KEY + "crew", status.crewAboard(), status.crewSeats()), "crew");
    }

    private int measure(GuiGraphics graphics, Font font, int x, int y, int text, Component label, String key)
    {
        int labelWidth = font.width(label);
        if (x + labelWidth > left + width)
            return x;
        graphics.drawString(font, label, x, y, text, false);
        hover(x, y, x + labelWidth, y + LINE_HEIGHT - 2, font, Component.translatable(KEY + key + HELP));
        return x + labelWidth + 12;
    }

    /** The warnings that apply, as coloured chips; a grey line when none does. */
    private void chips(GuiGraphics graphics, Font font, NpcStatus status, int y)
    {
        List<Chip> chips = new ArrayList<>();
        if (status.reloading())
            chips.add(chip("reloading", AMBER));
        if (status.outOfAmmunition())
            chips.add(chip("out_of_ammo", RED));
        if (status.repairing())
            chips.add(chip("repairing", BLUE));
        if (status.threat() != null)
            chips.add(new Chip(Component.translatable(KEY + "under_fire"), Component.translatable(KEY + "under_fire" + HELP, status.threat()), RED));
        switch (status.activity())
        {
            case DORMANT -> chips.add(chip("dormant", SLATE));
            case FROZEN -> chips.add(chip("frozen", ICE));
            case HELD -> chips.add(chip("held", GREEN));
            default ->
            {
                // An awake or killed NPC needs no chip: the health bar and the activity line already say so.
            }
        }
        if (chips.isEmpty())
        {
            graphics.drawString(font, Component.translatable(KEY + "all_clear"), left, y + 1, GREY, false);
            return;
        }
        int x = left;
        for (Chip chip : chips)
        {
            int chipWidth = font.width(chip.label()) + 2 * CHIP_PADDING;
            if (x + chipWidth > left + width)
                break;
            graphics.fill(x, y, x + chipWidth, y + LINE_HEIGHT - 1, chip.color());
            graphics.fill(x, y, x + chipWidth, y + 1, SHINE);
            graphics.drawString(font, chip.label(), x + CHIP_PADDING, y + 2, WHITE, true);
            hover(x, y, x + chipWidth, y + LINE_HEIGHT - 1, font, chip.help());
            x += chipWidth + CHIP_GAP;
        }
    }

    private static Chip chip(String key, int color)
    {
        return new Chip(Component.translatable(KEY + key), Component.translatable(KEY + key + HELP), color);
    }

    // Parts

    /** @return the parts of the model that hold hit points, in the model's order; none for an NPC without part hitboxes */
    private List<PartState> parts()
    {
        if (!(npc instanceof NpcVehicleStateHolder))
            return List.of();
        return NpcVehicleState.of(npc).getParts().body().map(IPartBody::getParts).orElse(List.of()).stream().filter(PartState::enabled).toList();
    }

    private void partList(GuiGraphics graphics, Font font, List<PartState> parts, int y, int mouseX, int mouseY, int text)
    {
        long broken = parts.stream().filter(NpcStatusPanel::broken).count();
        MutableComponent heading = Component.translatable(KEY + "parts");
        if (broken > 0)
            heading.append(Component.translatable(KEY + "parts_broken", broken).withStyle(ChatFormatting.DARK_RED));
        graphics.drawString(font, heading, left, y, text, false);
        int rowsTop = y + HEADING_HEIGHT - 1;
        int visible = Math.max(1, (bottom - rowsTop) / PART_ROW_HEIGHT);
        partOffset = Mth.clamp(partOffset, 0, Math.max(0, parts.size() - visible));
        boolean scrolls = parts.size() > visible;
        int rowWidth = width - (scrolls ? 4 : 0);
        int nameWidth = Mth.clamp(parts.stream().mapToInt(part -> font.width(part.displayName())).max().orElse(0), PART_NAME_MIN_WIDTH, PART_NAME_MAX_WIDTH);
        int valueWidth = parts.stream().mapToInt(part -> font.width(value(part))).max().orElse(0);
        int barLeft = left + nameWidth + PART_COLUMN_GAP;
        int barWidth = rowWidth - nameWidth - valueWidth - 2 * PART_COLUMN_GAP;
        for (int row = 0; row < visible && partOffset + row < parts.size(); row++)
        {
            PartState part = parts.get(partOffset + row);
            int rowTop = rowsTop + row * PART_ROW_HEIGHT;
            boolean over = mouseX >= left && mouseX < left + rowWidth && mouseY >= rowTop && mouseY < rowTop + PART_ROW_HEIGHT;
            if (over)
            {
                graphics.fill(left - 1, rowTop, left + rowWidth, rowTop + PART_ROW_HEIGHT, HOVER);
                hoveredPart = part.name();
            }
            boolean isBroken = broken(part);
            graphics.drawString(font, cut(font, part.displayName(), nameWidth), left, rowTop + 1, isBroken ? RED : text, false);
            bar(graphics, barLeft, rowTop + 2, barWidth, PART_BAR_HEIGHT, unbreakable(part) ? 1F : share(part), partColor(part));
            String value = value(part);
            graphics.drawString(font, value, left + rowWidth - font.width(value), rowTop + 1, isBroken ? RED : text, false);
            hover(left, rowTop, left + rowWidth, rowTop + PART_ROW_HEIGHT, font, partHelp(part));
        }
        if (scrolls)
        {
            int trackTop = rowsTop;
            int trackHeight = visible * PART_ROW_HEIGHT;
            int thumbHeight = Math.max(4, trackHeight * visible / parts.size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * partOffset / Math.max(1, parts.size() - visible);
            graphics.fill(left + width - 2, trackTop, left + width, trackTop + trackHeight, SLOT_DARK);
            graphics.fill(left + width - 2, thumbTop, left + width, thumbTop + thumbHeight, SLOT);
        }
    }

    /**
     * The parts seen from above, front up, each filled with the colour of its health; higher parts, such as a turret, are
     * drawn over lower ones. The part under the mouse here or in the list is outlined.
     */
    private void damageMap(GuiGraphics graphics, Font font, List<PartState> parts, int mouseX, int mouseY, int text)
    {
        graphics.drawString(font, Component.translatable(KEY + "map"), pictureLeft, pictureTop, text, false);
        int mapTop = pictureTop + HEADING_HEIGHT - 2;
        int mapBottom = bottom;
        int mapRight = pictureLeft + pictureWidth;
        graphics.fill(pictureLeft, mapTop, mapRight, mapBottom, BORDER);
        graphics.fill(pictureLeft + 1, mapTop + 1, mapRight - 1, mapBottom - 1, MAP_BACKGROUND);
        hover(pictureLeft, mapTop, mapRight, mapBottom, font, Component.translatable(KEY + "map" + HELP));
        // The arrow marking the front takes the top of the map.
        int arrowTop = mapTop + 2;
        int centre = pictureLeft + pictureWidth / 2;
        for (int step = 0; step < 3; step++)
            graphics.fill(centre - step, arrowTop + step, centre + step + 1, arrowTop + step + 1, GREY);
        int innerTop = arrowTop + 4;
        List<MapBox> boxes = mapBoxes(parts, pictureLeft + MAP_MARGIN, innerTop, pictureWidth - 2 * MAP_MARGIN, mapBottom - MAP_MARGIN - innerTop);
        MapBox under = null;
        for (int index = boxes.size() - 1; index >= 0 && under == null; index--)
        {
            if (boxes.get(index).contains(mouseX, mouseY))
                under = boxes.get(index);
        }
        String outlined = under != null ? under.part().name() : hoveredPart;
        for (MapBox box : boxes)
        {
            graphics.fill(box.x0(), box.y0(), box.x1(), box.y1(), box.part().name().equals(outlined) ? WHITE : BORDER);
            graphics.fill(box.x0() + 1, box.y0() + 1, box.x1() - 1, box.y1() - 1, partColor(box.part()));
        }
        if (under != null)
            hover(under.x0(), under.y0(), under.x1(), under.y1(), font, partHelp(under.part()));
    }

    /** @return each part's footprint scaled to fit the given area, front up, lowest part first */
    private static List<MapBox> mapBoxes(List<PartState> parts, int areaLeft, int areaTop, int areaWidth, int areaHeight)
    {
        AABB extent = parts.get(0).box().bounds();
        for (PartState part : parts)
            extent = extent.minmax(part.box().bounds());
        double spanX = Math.max(extent.getXsize(), 1.0E-3D);
        double spanZ = Math.max(extent.getZsize(), 1.0E-3D);
        double scale = Math.min(areaWidth / spanX, areaHeight / spanZ);
        double offsetX = areaLeft + (areaWidth - spanX * scale) / 2D - extent.minX * scale;
        double offsetZ = areaTop + (areaHeight - spanZ * scale) / 2D - extent.minZ * scale;
        List<MapBox> boxes = new ArrayList<>();
        for (PartState part : parts.stream().sorted(Comparator.comparingDouble(part -> part.box().bounds().maxY)).toList())
        {
            AABB bounds = part.box().bounds();
            int x0 = (int) Math.round(offsetX + bounds.minX * scale);
            int y0 = (int) Math.round(offsetZ + bounds.minZ * scale);
            boxes.add(new MapBox(part, x0, y0, Math.max(x0 + 2, (int) Math.round(offsetX + bounds.maxX * scale)), Math.max(y0 + 2, (int) Math.round(offsetZ + bounds.maxZ * scale))));
        }
        return boxes;
    }

    private static boolean unbreakable(PartState part)
    {
        return part.box().health() <= 0F;
    }

    private static boolean broken(PartState part)
    {
        return !unbreakable(part) && (part.destroyed() || part.health() <= 0F);
    }

    private static float share(PartState part)
    {
        return broken(part) ? 0F : Mth.clamp(part.health() / part.box().health(), 0F, 1F);
    }

    private static int partColor(PartState part)
    {
        if (unbreakable(part))
            return UNBREAKABLE;
        return broken(part) ? BROKEN : healthColor(share(part));
    }

    private static String value(PartState part)
    {
        if (unbreakable(part))
            return "-";
        return whole(broken(part) ? 0F : part.health()) + "/" + whole(part.box().health());
    }

    private static Component partHelp(PartState part)
    {
        MutableComponent help = Component.literal(part.displayName()).withStyle(ChatFormatting.WHITE);
        help.append("\n");
        if (unbreakable(part))
            help.append(Component.translatable(KEY + "part_unbreakable"));
        else if (broken(part))
            help.append(Component.translatable(KEY + "part_broken", whole(part.box().health())));
        else
            help.append(Component.translatable(KEY + "part_health", whole(part.health()), whole(part.box().health()), Math.round(share(part) * PERCENT)));
        return help;
    }

    // Equipment

    /** The items in the NPC's hands and armour slots, with their durability, below the Start here pages. */
    private void equipment(GuiGraphics graphics, Font font, int mouseX, int mouseY, int text)
    {
        graphics.drawString(font, Component.translatable(KEY + "equipment"), pictureLeft, pictureTop, text, false);
        int y = pictureTop + HEADING_HEIGHT - 1;
        for (int index = 0; index < SLOTS.length && pictureLeft + index * (SLOT_SIZE + SLOT_GAP) + SLOT_SIZE <= pictureLeft + pictureWidth; index++)
            slot(graphics, font, SLOTS[index], pictureLeft + index * (SLOT_SIZE + SLOT_GAP), y, mouseX, mouseY);
    }

    /** One equipment slot, framed as in an inventory, with the item's own tooltip. */
    private void slot(GuiGraphics graphics, Font font, EquipmentSlot equipmentSlot, int x, int y, int mouseX, int mouseY)
    {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_LIGHT);
        graphics.fill(x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT);
        ItemStack stack = npc.getItemBySlot(equipmentSlot);
        Component name = Component.translatable(KEY + "slot." + equipmentSlot.getName());
        if (stack.isEmpty())
        {
            hover(x, y, x + SLOT_SIZE, y + SLOT_SIZE, font, name.copy().append("\n").append(Component.translatable(KEY + "slot_empty").withStyle(ChatFormatting.GRAY)));
            return;
        }
        graphics.renderItem(stack, x + 1, y + 1);
        graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE)
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, HOVER);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(name.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
        Screen.getTooltipFromItem(Minecraft.getInstance(), stack).forEach(line -> lines.add(line.getVisualOrderText()));
        hovers.add(new Hover(x, y, x + SLOT_SIZE, y + SLOT_SIZE, lines));
    }

    // Drawing

    /** A framed bar filled from the left by the given share. */
    private static void bar(GuiGraphics graphics, int x, int y, int barWidth, int height, float share, int color)
    {
        graphics.fill(x, y, x + barWidth, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + barWidth - 1, y + height - 1, EMPTY);
        int filled = Math.round((barWidth - 2) * Mth.clamp(share, 0F, 1F));
        if (filled <= 0)
            return;
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + 2, SHINE);
    }

    /** @return red for none left through amber to green for all */
    private static int healthColor(float share)
    {
        return 0xFF000000 | Mth.hsvToRgb(Mth.clamp(share, 0F, 1F) / 3F, 0.75F, 0.8F);
    }

    private void hover(int x0, int y0, int x1, int y1, Font font, Component tooltip)
    {
        hovers.add(new Hover(x0, y0, x1, y1, font.split(tooltip, TOOLTIP_WIDTH)));
    }

    private static String cut(Font font, String text, int room)
    {
        if (font.width(text) <= room)
            return text;
        return font.plainSubstrByWidth(text, room - font.width("...")) + "...";
    }

    private static String whole(float value)
    {
        return Float.isFinite(value) ? Integer.toString(Math.round(value)) : "?";
    }

    private static String tenth(float value)
    {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
