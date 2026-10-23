package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.NpcPartSettings;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

/**
 * The part hitbox page that the Damage & armour page opens: one part of the selected model at a time, with a switch that
 * takes it out of the NPC, and the NPC's own box, hit points, penetration resistance and optional armour in place of the model's.
 *
 * <p>
 * Boxes are shown as type files give them, in model pixels of a sixteenth of a block, from the model origin: X to the
 * right, Y up and Z to the rear. Everything is edited on the client copy of the NPC's settings, which the tab sends when
 * it is left; the client's own copy of the hitboxes follows at once, so outlined hitboxes show an edit as it is made.
 * </p>
 */
final class PartHitboxPage
{
    static final int PREVIOUS_ID = 1400;
    static final int NEXT_ID = 1401;
    static final int RESET_ID = 1402;
    static final int RESET_ALL_ID = 1403;
    static final int ENABLED_ID = 1404;
    static final int MODE_ID = 1405;
    static final int ARMOR_MODE_ID = 1406;
    static final int ARMOR_ID = 1407;
    static final int HEAT_ARMOR_ID = 1408;
    static final int FIELD_ID = 1410;
    private static final int LABEL_ID = 1430;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final float PIXELS = 16F;
    private static final String PREFIX = "wolffsmodnpcs.parts.";
    private static final String HELP_SUFFIX = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};

    private final GuiBasic gui;
    private final EntityNPCInterface npc;
    /** A body of the selected model that only this page reads the parts from. */
    @Nullable
    private IPartBody body;
    @Nullable
    private IContentType bodyType;
    private int selected;

    /** The numbers of a part that can be edited, in the order of their fields. */
    private enum Field
    {
        CORNER_X, CORNER_Y, CORNER_Z, SIZE_X, SIZE_Y, SIZE_Z, HEALTH, RESISTANCE
    }

    PartHitboxPage(GuiBasic gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
    }

    private NpcPartSettings settings()
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsParts();
    }

    /** @return the parts of the selected model with the NPC's changes applied; empty for a model without part boxes */
    private List<PartState> parts()
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType type = model == null ? null : model.getInfoType();
        if (type != bodyType)
        {
            bodyType = type;
            body = type == null ? null : FlansPartBodies.create(npc, type).orElse(null);
        }
        if (body == null)
            return List.of();
        settings().apply(body);
        return body.getParts();
    }

    void init(boolean loaded)
    {
        List<PartState> parts = parts();
        int x = gui.guiLeft + 8;
        if (parts.isEmpty())
        {
            gui.addLabel(new GuiLabel(LABEL_ID, PREFIX + "none", x, gui.guiTop + 34));
            return;
        }
        selected = Math.floorMod(selected, parts.size());
        PartState part = parts.get(selected);
        boolean custom = part.custom();

        navigation(loaded, x, part, parts.size());
        int y = gui.guiTop + 52;
        gui.addLabel(new GuiLabel(ENABLED_ID, PREFIX + "enabled", x, y + 5, PREFIX + "enabled" + HELP_SUFFIX));
        GuiButtonNop enabled = new GuiButtonNop(gui, ENABLED_ID, x + 122, y, 48, 20, NO_YES, part.enabled() ? 1 : 0);
        enabled.setEnabled(loaded);
        gui.addButton(enabled);
        gui.addLabel(new GuiLabel(MODE_ID, PREFIX + "mode", x + 196, y + 5, PREFIX + "mode" + HELP_SUFFIX));
        GuiButtonNop mode = new GuiButtonNop(gui, MODE_ID, x + 258, y, 70, 20, new String[]{PREFIX + "mode.model", PREFIX + "mode.custom"}, custom ? 1 : 0);
        mode.setEnabled(loaded);
        gui.addButton(mode);

        boolean editable = loaded && custom;
        y += 23;
        row(Field.HEALTH, "health", x, y, 0, part, editable);
        gui.addLabel(new GuiLabel(FIELD_ID + Field.RESISTANCE.ordinal(), PREFIX + "resistance", x + 196, y + 5, PREFIX + "resistance" + HELP_SUFFIX));
        field(Field.RESISTANCE, x + 258, y, part, editable);
        y += 23;
        row(Field.CORNER_X, "corner", x, y, 3, part, editable);
        y += 23;
        row(Field.SIZE_X, "size", x, y, 3, part, editable);
        armor(loaded, x, y + 23, part);

        PartBox model = part.definition();
        AABB bounds = model.bounds();
        gui.addLabel(
            new GuiLabel(LABEL_ID + 1, I18n.get(PREFIX + "definition", number(model.health()), number(model.penetrationResistance()), number(bounds.minX * PIXELS), number(bounds.minY * PIXELS),
                number(bounds.minZ * PIXELS), number(bounds.getXsize() * PIXELS), number(bounds.getYsize() * PIXELS), number(bounds.getZsize() * PIXELS)), x, gui.guiTop + 169, READ_ONLY_COLOR));
        gui.addLabel(new GuiLabel(LABEL_ID + 2, share(part), x, gui.guiTop + 181, READ_ONLY_COLOR));
        gui.addLabel(new GuiLabel(LABEL_ID + 3, PREFIX + (part.turretMounted() ? "frame_turret" : "frame"), x, gui.guiTop + 193, READ_ONLY_COLOR));
    }

    private void armor(boolean loaded, int x, int y, PartState part)
    {
        PartArmor armor = body == null ? null : body.getArmor(part.name()).orElse(null);
        gui.addLabel(new GuiLabel(ARMOR_MODE_ID, PREFIX + "armor_override", x, y + 5, PREFIX + "armor_override" + HELP_SUFFIX));
        GuiButtonNop mode = new GuiButtonNop(gui, ARMOR_MODE_ID, x + 122, y, 48, 20, NO_YES, armor == null ? 0 : 1);
        mode.setEnabled(loaded);
        gui.addButton(mode);
        armorField(ARMOR_ID, "armor", x + 184, x + 204, y, armor == null ? 0F : armor.thicknessMm(), loaded && armor != null);
        armorField(HEAT_ARMOR_ID, "armor_heat", x + 274, x + 330, y, armor == null ? 0F : armor.heatThicknessMm(), loaded && armor != null);
    }

    private void armorField(int id, String name, int x, int controlX, int y, float value, boolean editable)
    {
        gui.addLabel(new GuiLabel(id, PREFIX + name, x, y + 5, PREFIX + name + HELP_SUFFIX));
        GuiTextFieldNop text = new GuiTextFieldNop(id, gui, controlX, y, 60, 20, number(value));
        text.setEditable(editable);
        text.setTextColorUneditable(READ_ONLY_COLOR);
        text.floatsOnly = true;
        text.setMinMaxDefault(0F, NpcPartSettings.MAX_ARMOR_MM, value);
        gui.addTextField(text);
    }

    private void navigation(boolean loaded, int x, PartState part, int count)
    {
        int y = gui.guiTop + 29;
        GuiButtonNop previous = new GuiButtonNop(gui, PREVIOUS_ID, x, y, 20, 20, new String[]{"<"}, 0);
        previous.setEnabled(loaded && count > 1);
        gui.addButton(previous);
        GuiButtonNop next = new GuiButtonNop(gui, NEXT_ID, x + 22, y, 20, 20, new String[]{">"}, 0);
        next.setEnabled(loaded && count > 1);
        gui.addButton(next);
        gui.addLabel(new GuiLabel(LABEL_ID, I18n.get(PREFIX + "selected", selected + 1, count, part.displayName(), part.name()), x + 48, y + 6));
        GuiButtonNop reset = new GuiButtonNop(gui, RESET_ID, x + 258, y, 66, 20, new String[]{PREFIX + "reset"}, 0);
        reset.setEnabled(loaded && (part.custom() || !part.enabled() || settings().get(part.name()).armor() != null));
        reset.setTooltip(Tooltip.create(Component.translatable(PREFIX + "reset" + HELP_SUFFIX)));
        gui.addButton(reset);
        GuiButtonNop all = new GuiButtonNop(gui, RESET_ALL_ID, x + 326, y, 70, 20, new String[]{PREFIX + "reset_all"}, 0);
        all.setEnabled(loaded && !settings().isEmpty());
        all.setTooltip(Tooltip.create(Component.translatable(PREFIX + "reset_all" + HELP_SUFFIX)));
        gui.addButton(all);
    }

    /** A label followed by one field, or by the three fields of a point. */
    private void row(Field first, String name, int x, int y, int span, PartState part, boolean editable)
    {
        gui.addLabel(new GuiLabel(FIELD_ID + first.ordinal(), PREFIX + name, x, y + 5, PREFIX + name + HELP_SUFFIX));
        for (int index = 0; index < Math.max(1, span); index++)
            field(Field.values()[first.ordinal() + index], x + 122 + index * 64, y, part, editable);
    }

    private void field(Field field, int x, int y, PartState part, boolean editable)
    {
        GuiTextFieldNop text = new GuiTextFieldNop(FIELD_ID + field.ordinal(), gui, x, y, 60, 20, number(value(field, part.box())));
        text.setEditable(editable);
        text.setTextColorUneditable(READ_ONLY_COLOR);
        text.floatsOnly = true;
        float current = value(field, part.box());
        switch (field)
        {
            case CORNER_X, CORNER_Y, CORNER_Z -> text.setMinMaxDefault(-NpcPartSettings.MAX_EXTENT * PIXELS, NpcPartSettings.MAX_EXTENT * PIXELS, current);
            case SIZE_X, SIZE_Y, SIZE_Z -> text.setMinMaxDefault(0F, NpcPartSettings.MAX_EXTENT * PIXELS, current);
            case HEALTH -> text.setMinMaxDefault(0F, NpcPartSettings.MAX_HEALTH, current);
            case RESISTANCE -> text.setMinMaxDefault(0F, NpcPartSettings.MAX_RESISTANCE, current);
        }
        gui.addTextField(text);
    }

    private static float value(Field field, PartBox box)
    {
        AABB bounds = box.bounds();
        return switch (field)
        {
            case CORNER_X -> (float) bounds.minX * PIXELS;
            case CORNER_Y -> (float) bounds.minY * PIXELS;
            case CORNER_Z -> (float) bounds.minZ * PIXELS;
            case SIZE_X -> (float) bounds.getXsize() * PIXELS;
            case SIZE_Y -> (float) bounds.getYsize() * PIXELS;
            case SIZE_Z -> (float) bounds.getZsize() * PIXELS;
            case HEALTH -> box.health();
            case RESISTANCE -> box.penetrationResistance();
        };
    }

    private static PartBox with(Field field, PartBox box, float value)
    {
        AABB bounds = box.bounds();
        double[] corner = {bounds.minX, bounds.minY, bounds.minZ};
        double[] size = {bounds.getXsize(), bounds.getYsize(), bounds.getZsize()};
        float health = box.health();
        float resistance = box.penetrationResistance();
        switch (field)
        {
            case CORNER_X, CORNER_Y, CORNER_Z -> corner[field.ordinal()] = value / PIXELS;
            case SIZE_X, SIZE_Y, SIZE_Z -> size[field.ordinal() - Field.SIZE_X.ordinal()] = Math.max(0F, value) / PIXELS;
            case HEALTH -> health = value;
            case RESISTANCE -> resistance = value;
        }
        return new PartBox(new AABB(corner[0], corner[1], corner[2], corner[0] + size[0], corner[1] + size[1], corner[2] + size[2]), health, resistance);
    }

    /** How much of the NPC's health the part holds: its hit points as a share of all enabled parts' together. */
    private String share(PartState part)
    {
        if (body == null || !part.enabled())
            return I18n.get(PREFIX + "share.none");
        float capacity = body.getMaxHealth();
        float health = capacity > 0F ? part.box().health() / capacity * npc.getMaxHealth() : 0F;
        return I18n.get(PREFIX + "share", number(health), number(npc.getMaxHealth()));
    }

    private static String number(double value)
    {
        String text = String.format(Locale.ROOT, "%.2f", value);
        int end = text.length();
        while (text.charAt(end - 1) == '0')
            end--;
        if (text.charAt(end - 1) == '.')
            end--;
        text = text.substring(0, end);
        return "-0".equals(text) ? "0" : text;
    }

    // Events

    boolean handles(int id)
    {
        return id >= PREVIOUS_ID && id <= ARMOR_MODE_ID;
    }

    /** @return whether the click changed a setting that has to be saved */
    boolean buttonEvent(GuiButtonNop button)
    {
        List<PartState> parts = parts();
        if (parts.isEmpty())
            return false;
        selected = Math.floorMod(selected, parts.size());
        PartState part = parts.get(selected);
        switch (button.id)
        {
            case PREVIOUS_ID -> {
                selected = Math.floorMod(selected - 1, parts.size());
                return false;
            }
            case NEXT_ID -> {
                selected = Math.floorMod(selected + 1, parts.size());
                return false;
            }
            case RESET_ID -> {
                settings().setEnabled(part.name(), true);
                settings().setBox(part.name(), null);
                settings().setArmor(part.name(), null);
            }
            case RESET_ALL_ID -> settings().clear();
            case ENABLED_ID -> settings().setEnabled(part.name(), button.getValue() == 1);
            // The NPC's own box starts as the model's.
            case MODE_ID -> settings().setBox(part.name(), button.getValue() == 1 ? part.definition() : null);
            case ARMOR_MODE_ID -> settings().setArmor(part.name(), button.getValue() == 1 ? new PartArmor(0F, 0F) : null);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** @return whether the field belongs to this page and its value was stored */
    boolean unFocused(GuiTextFieldNop text)
    {
        if (text.id == ARMOR_ID || text.id == HEAT_ARMOR_ID)
            return armorUnFocused(text);
        int index = text.id - FIELD_ID;
        if (index < 0 || index >= Field.values().length)
            return false;
        List<PartState> parts = parts();
        if (parts.isEmpty() || !text.isFloat())
            return false;
        PartState part = parts.get(Math.floorMod(selected, parts.size()));
        if (!part.custom())
            return false;
        settings().setBox(part.name(), with(Field.values()[index], part.box(), text.getFloat()));
        return true;
    }

    private boolean armorUnFocused(GuiTextFieldNop text)
    {
        List<PartState> parts = parts();
        if (parts.isEmpty() || !text.isFloat())
            return false;
        PartState part = parts.get(Math.floorMod(selected, parts.size()));
        PartArmor armor = settings().get(part.name()).armor();
        if (armor == null)
            return false;
        settings().setArmor(part.name(), new PartArmor(text.id == ARMOR_ID ? text.getFloat() : armor.thicknessMm(), text.id == HEAT_ARMOR_ID ? text.getFloat() : armor.heatThicknessMm()));
        return true;
    }
}
