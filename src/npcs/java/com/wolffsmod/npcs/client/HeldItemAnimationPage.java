package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.equipment.ArmPoseRule;
import com.flansmodultimate.api.equipment.HeldItemSettings;
import com.flansmodultimate.api.equipment.HeldItemTransform;
import com.flansmodultimate.api.equipment.WeaponPoseRules;
import com.wolffsmod.npcs.properties.NpcPresentation;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.Arrays;
import java.util.Locale;

/** Editor-local settings; the enclosing stats menu supplies Custom NPCs' permission-checked save and synchronization. */
final class HeldItemAnimationPage
{
    static final String ITEMS = "held_items";
    static final String ARMS_TAB = "arm_poses";
    private static final String PREFIX = "wolffsmodnpcs.held_item.";
    private static final int HAND_ID = 2200;
    private static final int STATE_ID = 2201;
    private static final int POSE = 2202;
    private static final int RESET = 2203;
    private static final int HOLD = 2204;
    private static final int AXIS = 2210;
    private static final int SLIDER = 2220;
    private static final int FIELD = 2230;
    private static final int LABEL = 2240;
    private static final String[] AXES = {"x", "y", "z", "rotation_x", "rotation_y", "rotation_z"};

    private final GuiWolffsMod gui;
    private final EntityNPCInterface npc;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private WeaponPoseRules.State state = WeaponPoseRules.State.IDLE;
    private boolean arms;
    private boolean ready;

    HeldItemAnimationPage(GuiWolffsMod gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
    }

    int changes(String tab)
    {
        if (!ITEMS.equals(tab) && !ARMS_TAB.equals(tab))
            return 0;
        int count = 0;
        for (InteractionHand selected : InteractionHand.values())
        {
            HeldItemSettings settings = NpcPresentation.of(npc).hand(selected);
            if (ITEMS.equals(tab) && !settings.transform().equals(HeldItemTransform.IDENTITY) || ARMS_TAB.equals(tab) && !settings.poses().equals(WeaponPoseRules.DEFAULT))
                count++;
        }
        return count;
    }

    void init(String tab, boolean loaded, int top)
    {
        ready = loaded;
        arms = ARMS_TAB.equals(tab);
        int y = gui.guiTop + top;
        button(HAND_ID, gui.guiLeft + 8, y, 132, new String[]{PREFIX + "main_hand", PREFIX + "off_hand"}, hand.ordinal(), loaded);
        button(RESET, gui.guiLeft + 324, y, 80, new String[]{PREFIX + "reset"}, 0, loaded);
        if (arms)
        {
            button(STATE_ID, gui.guiLeft + 144, y, 176, keys("state.", WeaponPoseRules.State.values()), state.ordinal(), loaded);
            button(POSE, gui.guiLeft + 144, y + 21, 260, keys("pose.", ArmPoseRule.Pose.values()), rule().pose().ordinal(), loaded);
            label(6, "pose", gui.guiLeft + 8, y + 26);
            for (int axis = 0; axis < 3; axis++)
            {
                boolean custom = rule().pose() == ArmPoseRule.Pose.CUSTOM;
                boolean selected = (rule().axes() & 1 << axis) != 0;
                button(AXIS + axis, gui.guiLeft + 80, y + 42 + axis * 21, 60, new String[]{PREFIX + "inherit", PREFIX + "override"}, selected ? 1 : 0, loaded && custom);
                number(axis, y + 42 + axis * 21, loaded && custom && selected);
            }
            label(4, "hold", gui.guiLeft + 8, y + 110);
            GuiTextFieldNop hold = new GuiTextFieldNop(HOLD, gui, gui.guiLeft + 344, y + 105, 60, 20, Integer.toString(settings().poses().shotHoldTicks()));
            hold.setNumbersOnly().setMinMaxDefault(0, 200, 20);
            hold.setEditable(loaded);
            gui.addTextField(hold);
            label(5, "priority", gui.guiLeft + 8, y + 134);
        }
        else
        {
            for (int axis = 0; axis < 6; axis++)
                number(axis, y + 21 + axis * 21, loaded);
        }
    }

    private static String[] keys(String prefix, Enum<?>[] values)
    {
        return Arrays.stream(values).map(value -> PREFIX + prefix + value.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    }

    private void button(int id, int x, int y, int width, String[] labels, int value, boolean enabled)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, x, y, width, 20, labels, value);
        button.setEnabled(enabled);
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + (id == STATE_ID ? "states_help" : "help"))));
        gui.addButton(button);
    }

    private void label(int id, String key, int x, int y)
    {
        GuiLabel label = new GuiLabel(LABEL + id, PREFIX + key, x, y);
        label.setTooltip(Tooltip.create(Component.translatable(PREFIX + "help")));
        gui.addLabel(label);
    }

    private void number(int axis, int y, boolean enabled)
    {
        float value = values()[axis];
        float limit = limit(axis);
        label(axis, (arms ? "arm_" : "") + AXES[axis], gui.guiLeft + 8, y + 5);
        // The string constructor does not call the listener while the widget is being constructed.
        GuiSliderNop slider = new GuiSliderNop(gui, SLIDER + axis, gui.guiLeft + 144, y, "", (value + limit) / (2F * limit));
        slider.setWidth(196);
        slider.active = enabled;
        slider.setString(number(value));
        gui.addSlider(slider);
        GuiTextFieldNop field = new GuiTextFieldNop(FIELD + axis, gui, gui.guiLeft + 344, y, 60, 20, number(value));
        field.setFloatsOnly().setMinMaxDefault(-limit, limit, value);
        field.setEditable(enabled);
        gui.addTextField(field);
    }

    private float limit(int axis)
    {
        return arms || axis >= 3 ? 180F : 64F;
    }

    private static String number(float value)
    {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    boolean handles(int id)
    {
        return id >= HAND_ID && id < LABEL;
    }

    /** Navigation is kept separate from edits so choosing a hand/state does not mark an NPC dirty. */
    boolean button(GuiButtonNop button)
    {
        switch (button.id)
        {
            case HAND_ID -> hand = InteractionHand.values()[button.getValue()];
            case STATE_ID -> state = WeaponPoseRules.State.values()[button.getValue()];
            case POSE -> {
                ArmPoseRule old = rule();
                ArmPoseRule.Pose pose = ArmPoseRule.Pose.values()[button.getValue()];
                setRule(new ArmPoseRule(pose, old.axes(), old.x(), old.y(), old.z()));
                return true;
            }
            case RESET -> {
                HeldItemSettings old = settings();
                store(arms ? new HeldItemSettings(old.transform(), WeaponPoseRules.DEFAULT) : new HeldItemSettings(HeldItemTransform.IDENTITY, old.poses()));
                return true;
            }
            default -> {
                int axis = button.id - AXIS;
                if (axis >= 0 && axis < 3)
                {
                    ArmPoseRule old = rule();
                    setRule(new ArmPoseRule(old.pose(), old.axes() ^ 1 << axis, old.x(), old.y(), old.z()));
                    return true;
                }
            }
        }
        return false;
    }

    boolean field(GuiTextFieldNop field)
    {
        if (!ready || !handles(field.id))
            return false;
        if (field.id == HOLD && field.isInteger())
        {
            WeaponPoseRules old = settings().poses();
            store(new HeldItemSettings(settings().transform(), new WeaponPoseRules(old.idle(), old.moving(), old.targeting(), old.firing(), old.using(), field.getInteger())));
            return true;
        }
        int axis = field.id - FIELD;
        if (axis < 0 || axis >= (arms ? 3 : 6) || !field.isFloat() || !Float.isFinite(field.getFloat()))
            return false;
        value(axis, field.getFloat());
        GuiSliderNop slider = gui.getSlider(SLIDER + axis);
        slider.sliderValue = (values()[axis] + limit(axis)) / (2F * limit(axis));
        slider.setString(number(values()[axis]));
        return true;
    }

    boolean slider(GuiSliderNop slider)
    {
        int axis = slider.id - SLIDER;
        if (!ready || !slider.active || axis < 0 || axis >= (arms ? 3 : 6))
            return false;
        float value = Math.round((slider.sliderValue * 2F - 1F) * limit(axis) * 100F) / 100F;
        value(axis, value);
        slider.setString(number(value));
        gui.getTextField(FIELD + axis).setValue(number(value));
        return true;
    }

    private void value(int axis, float value)
    {
        float[] values = values();
        values[axis] = value;
        HeldItemSettings old = settings();
        if (arms)
            setRule(new ArmPoseRule(rule().pose(), rule().axes(), values[0], values[1], values[2]));
        else
            store(new HeldItemSettings(new HeldItemTransform(values[0], values[1], values[2], values[3], values[4], values[5]), old.poses()));
    }

    private float[] values()
    {
        if (arms)
            return new float[]{rule().x(), rule().y(), rule().z()};
        HeldItemTransform t = settings().transform();
        return new float[]{t.x(), t.y(), t.z(), t.rotationX(), t.rotationY(), t.rotationZ()};
    }

    private HeldItemSettings settings()
    {
        return NpcPresentation.of(npc).hand(hand);
    }

    private ArmPoseRule rule()
    {
        return settings().poses().get(state);
    }

    private void setRule(ArmPoseRule rule)
    {
        store(new HeldItemSettings(settings().transform(), settings().poses().with(state, rule)));
    }

    private void store(HeldItemSettings settings)
    {
        NpcPresentation.of(npc).hand(hand, settings);
    }
}
