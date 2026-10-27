package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.wolffsmod.npcs.client.CombatPages.Group;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcDriving;
import com.wolffsmod.npcs.sim.NpcModels;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.DriveType;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * The rows of the Driving and Turning tabs of the model properties page: the movement values a content-pack model's
 * definition supplies. Each row edits the same tuning value as the Travel and Aiming pages, where 0 inherits the
 * model; mass and engine power are read-only readouts of what the inherited response is made of.
 */
final class ModelTuningRows
{
    static final int MODE_ID = 2000;
    static final int VALUE_ID = 2020;
    private static final String PREFIX = "wolffsmodnpcs.combat.";
    private static final String HELP_SUFFIX = ".help";
    private static final String[] MODES = {PREFIX + "mode.inherit", PREFIX + "mode.custom"};
    private static final int INHERITED_COLOR = 0x408040;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final int VALUE_LEFT = 130;
    private static final int VALUE_WIDTH = 112;
    private static final int MODE_LEFT = 258;
    private static final int MODE_WIDTH = 60;
    /** The drive types a custom value chooses between; Automatic is what Inherit means. */
    private static final DriveType[] CUSTOM_TYPES = {DriveType.WHEELED, DriveType.TRACKED, DriveType.HEAVY_TRACKED};

    private final GuiBasic gui;
    private final EntityNPCInterface npc;

    /** One movement value of the model; every row belongs to the Driving or the Turning tab. */
    enum Row
    {
        // spotless:off
        LAND_SPEED(Group.DRIVING, Value.LAND_SPEED),
        FLIGHT_SPEED(Group.DRIVING, Value.FLIGHT_SPEED),
        CRUISE_SPEED(Group.DRIVING, Value.CRUISE_SPEED),
        CLIMB_RATE(Group.DRIVING, Value.CLIMB_RATE),
        REVERSE_SPEED(Group.DRIVING, Value.DRIVE_REVERSE_SPEED),
        ACCELERATION(Group.DRIVING, Value.DRIVE_ACCELERATION),
        BRAKING(Group.DRIVING, Value.DRIVE_BRAKING),
        COASTING(Group.DRIVING, Value.DRIVE_COASTING),
        STEP_HEIGHT(Group.DRIVING, Value.STEP_HEIGHT),
        MASS(Group.DRIVING, null),
        ENGINE_POWER(Group.DRIVING, null),
        DRIVE_TYPE(Group.TURNING, Value.DRIVE_TYPE),
        HULL_TURN_RATE(Group.TURNING, Value.HULL_TURN_RATE),
        FLIGHT_TURN_RATE(Group.TURNING, Value.FLIGHT_TURN_RATE),
        TURN_RADIUS(Group.TURNING, Value.TURN_RADIUS),
        TRAVERSE(Group.TURNING, Value.AIM_SPEED_YAW),
        ELEVATION(Group.TURNING, Value.AIM_SPEED_PITCH);
        // spotless:on

        private final Group group;
        @Nullable
        private final Value value;

        Row(Group group, @Nullable Value value)
        {
            this.group = group;
            this.value = value;
        }

        /**
         * @return whether a model of the kind has the row: the gun's aim speeds belong to every content-pack model, speed and
         *         step height to a mecha too, flight to a plane, and the rest to a ground vehicle
         */
        boolean offeredFor(FlanModelKind kind)
        {
            return switch (this)
            {
                case TRAVERSE, ELEVATION -> true;
                case LAND_SPEED, STEP_HEIGHT -> kind == FlanModelKind.VEHICLE || kind == FlanModelKind.MECHA;
                case FLIGHT_SPEED, CRUISE_SPEED, CLIMB_RATE, FLIGHT_TURN_RATE -> kind == FlanModelKind.PLANE;
                default -> kind == FlanModelKind.VEHICLE;
            };
        }
    }

    ModelTuningRows(GuiBasic gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
    }

    /** @return whether the tab holds movement rows rather than model properties */
    static boolean holds(Group group)
    {
        return group == Group.DRIVING || group == Group.TURNING;
    }

    /** @return whether the tab is offered for the NPC's model: when the model has any of its rows */
    static boolean visible(EntityNPCInterface npc, Group group)
    {
        return !holds(group) || !rows(npc, group).isEmpty();
    }

    static List<Row> rows(EntityNPCInterface npc, Group group)
    {
        FlanModelKind kind = kind(npc);
        if (kind == null)
            return List.of();
        return Arrays.stream(Row.values()).filter(row -> row.group == group && row.offeredFor(kind)).toList();
    }

    @Nullable
    private static FlanModelKind kind(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        if (definition == null)
            return null;
        // A ship a content pack filed as a plane sails as a vehicle, so the driving rows are hers.
        return NpcKind.of(npc) == NpcKind.VEHICLE ? FlanModelKind.VEHICLE : FlanModelKind.of(definition);
    }

    static boolean handles(int id)
    {
        return id >= MODE_ID && id < VALUE_ID + Row.values().length;
    }

    void init(Group group, int top, boolean loaded)
    {
        List<Row> rows = rows(npc, group);
        for (int i = 0; i < rows.size(); i++)
            row(rows.get(i), top + i * 21, loaded);
    }

    private void row(Row row, int y, boolean loaded)
    {
        int x = gui.guiLeft + 8;
        String name = key(row);
        String help = row.value == null ? name + HELP_SUFFIX : "wolffsmodnpcs.tuning." + row.value.name().toLowerCase(Locale.ROOT) + HELP_SUFFIX;
        gui.addLabel(new GuiLabel(MODE_ID + row.ordinal(), name, x, y + 5, help));
        boolean inherits = row.value == null || inherits(row.value);
        if (row.value == null)
            readout(row, y);
        else if (row == Row.DRIVE_TYPE)
            driveType(y, inherits, loaded);
        else
            number(row, y, inherits, loaded);
        GuiButtonNop mode = new GuiButtonNop(gui, MODE_ID + row.ordinal(), x + MODE_LEFT, y, MODE_WIDTH, 20, MODES.clone(), inherits ? 0 : 1);
        mode.setEnabled(loaded && row.value != null && inheritable(row.value));
        mode.setTooltip(Tooltip.create(Component.translatable("wolffsmodnpcs.tuning.handling_mode.help")));
        gui.addButton(mode);
    }

    /** @return whether the value can come from the model at all, so that Inherit means something */
    private boolean inheritable(Value value)
    {
        return value == Value.DRIVE_TYPE || GroundHandlingControls.supported(npc, value);
    }

    private boolean inherits(Value value)
    {
        int stored = NpcTuning.of(npc).get(value);
        return stored == 0 && inheritable(value);
    }

    private void readout(Row row, int y)
    {
        GroundPropulsion engine = NpcDriving.propulsion(npc, ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).orElse(null);
        OptionalDouble value = OptionalDouble.empty();
        if (engine != null)
            value = row == Row.MASS ? engine.massKg() : engine.enginePowerKw();
        String text = value.isPresent() ? Component.translatable(key(row) + ".value", GroundHandlingControls.format(value.getAsDouble())).getString() : "-";
        GuiLabel shown = new GuiLabel(VALUE_ID + row.ordinal(), Component.literal(text), value.isPresent() ? INHERITED_COLOR : READ_ONLY_COLOR, gui.guiLeft + VALUE_LEFT, y + 5, 40, 0);
        shown.setTooltip(Tooltip.create(Component.translatable(key(row) + HELP_SUFFIX)));
        gui.addLabel(shown);
    }

    private void driveType(int y, boolean inherits, boolean loaded)
    {
        int x = gui.guiLeft + VALUE_LEFT;
        String key = "wolffsmodnpcs.tuning.drive_type.";
        if (inherits)
        {
            String resolved = key + resolvedType().name().toLowerCase(Locale.ROOT);
            gui.addLabel(new GuiLabel(VALUE_ID + Row.DRIVE_TYPE.ordinal(), Component.translatable(resolved), INHERITED_COLOR, x, y + 5, 40, 0));
            return;
        }
        String[] names = Arrays.stream(CUSTOM_TYPES).map(type -> key + type.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
        GuiButtonNop button = new GuiButtonNop(gui, VALUE_ID + Row.DRIVE_TYPE.ordinal(), x, y, VALUE_WIDTH, 20, names, Math.max(0, Arrays.asList(CUSTOM_TYPES).indexOf(NpcTuning.of(npc).driveType())));
        button.setEnabled(loaded);
        gui.addButton(button);
    }

    /** @return what Automatic comes to for the model: tracked for a tank, wheeled otherwise */
    private DriveType resolvedType()
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return model != null && model.getInfoType() instanceof IDriveableType type && type.isTracked() ? DriveType.TRACKED : DriveType.WHEELED;
    }

    private void number(Row row, int y, boolean inherits, boolean loaded)
    {
        Value value = row.value;
        String text = inherits ? GroundHandlingControls.format(GroundHandlingControls.effective(npc, value)) : Integer.toString(NpcTuning.of(npc).get(value));
        GuiTextFieldNop field = new GuiTextFieldNop(VALUE_ID + row.ordinal(), gui, gui.guiLeft + VALUE_LEFT, y, VALUE_WIDTH, 20, text);
        field.numbersOnly = !inherits;
        field.setMinMaxDefault(inheritable(value) ? 1 : value.minimum(), value.maximum(), value.fallback());
        field.setEditable(loaded && !inherits);
        field.setTextColorUneditable(INHERITED_COLOR);
        field.setTooltip(Tooltip.create(Component.translatable(PREFIX + (inherits ? "value.inherited" : "value.own"))));
        gui.addTextField(field);
    }

    /** @return whether the click belongs to these rows, or is Inherit all or Import all of a tab of them */
    boolean claims(int id, Group group)
    {
        return handles(id) || holds(group) && (id == CombatPages.INHERIT_ALL_ID || id == CombatPages.IMPORT_ALL_ID);
    }

    /** @return whether a click these rows claim changed a setting that has to be saved */
    boolean event(Group group, GuiButtonNop button)
    {
        if (button.id == CombatPages.INHERIT_ALL_ID)
            inheritAll(group);
        else if (button.id == CombatPages.IMPORT_ALL_ID)
            importAll(group);
        else
            return button(button);
        return true;
    }

    /** @return whether the click changed a setting that has to be saved */
    boolean button(GuiButtonNop button)
    {
        Row row = row(button.id);
        if (row == null || row.value == null)
            return false;
        if (button.id >= VALUE_ID)
        {
            GroundHandlingControls.store(npc, Value.DRIVE_TYPE, CUSTOM_TYPES[Math.floorMod(button.getValue(), CUSTOM_TYPES.length)].ordinal());
            return true;
        }
        boolean custom = button.getValue() == 1;
        if (row == Row.DRIVE_TYPE)
            GroundHandlingControls.store(npc, Value.DRIVE_TYPE, custom ? resolvedType().ordinal() : DriveType.AUTO.ordinal());
        else
            GroundHandlingControls.choose(npc, row.value, custom);
        return true;
    }

    /** @return whether the field was one of these rows and its value was stored */
    boolean field(GuiTextFieldNop field)
    {
        Row row = row(field.id);
        if (row == null || row.value == null || field.id < VALUE_ID || !field.isInteger() || inherits(row.value))
            return false;
        int minimum = inheritable(row.value) ? 1 : row.value.minimum();
        GroundHandlingControls.store(npc, row.value, Math.max(minimum, Math.min(row.value.maximum(), field.getInteger())));
        return true;
    }

    /** Clears every inheritable value of the tab, so that the model supplies it again. */
    void inheritAll(Group group)
    {
        for (Row row : rows(npc, group))
        {
            if (row.value != null && inheritable(row.value) && !inherits(row.value))
                store(row, false);
        }
    }

    /** Copies every value the model supplies into the NPC's own, as Custom does row by row. */
    void importAll(Group group)
    {
        for (Row row : rows(npc, group))
        {
            if (row.value != null && inherits(row.value))
                store(row, true);
        }
    }

    private void store(Row row, boolean custom)
    {
        if (row == Row.DRIVE_TYPE)
            GroundHandlingControls.store(npc, Value.DRIVE_TYPE, custom ? resolvedType().ordinal() : DriveType.AUTO.ordinal());
        else
            GroundHandlingControls.choose(npc, row.value, custom);
    }

    /** @return whether Inherit all has anything to do on the tab */
    boolean anyCustom(Group group)
    {
        return rows(npc, group).stream().anyMatch(row -> row.value != null && inheritable(row.value) && !inherits(row.value));
    }

    /** @return whether Import all has anything to do on the tab */
    boolean anyInherited(Group group)
    {
        return rows(npc, group).stream().anyMatch(row -> row.value != null && inherits(row.value));
    }

    private static String key(Row row)
    {
        return PREFIX + "property." + row.name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static Row row(int id)
    {
        int index = id >= VALUE_ID ? id - VALUE_ID : id - MODE_ID;
        return id >= MODE_ID && index >= 0 && index < Row.values().length ? Row.values()[index] : null;
    }
}
