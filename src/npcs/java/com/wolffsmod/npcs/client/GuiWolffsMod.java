package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.select.GuiSoundSelection;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Per-NPC controls grouped inside the Wolff's Mod tab, using the Custom NPCs stats transport and sound picker. */
// Custom NPCs' editor requires the external GUI inheritance chain.
@SuppressWarnings("java:S110")
public class GuiWolffsMod extends GuiNPCInterface2 implements IGuiData, ITextfieldListener
{
    public static final int MENU_ID = 100;
    private static final int CATEGORY_BUTTON_ID = 200;
    private static final int BACK_BUTTON_ID = 300;
    private static final int HURT_FLASH_ID = 400;
    private static final int DEATH_ROTATION_ID = 401;
    private static final int HIDE_BODY_ID = 402;
    private static final int VEHICLE_SOUNDS_ID = 403;
    private static final int IDLE_ENABLED_ID = 404;
    private static final int MOVEMENT_ENABLED_ID = 405;
    private static final int VARIABLE_PITCH_ID = 406;
    private static final int MODEL_DEFAULTS_ID = 407;
    private static final int EDIT_IDLE_ID = 408;
    private static final int EDIT_MOVEMENT_ID = 409;
    private static final int OVERRIDE_REPEAT_ID = 410;
    private static final int CLEAR_SOUND_ID = 411;
    private static final int WEAPON_SOUNDS_ID = 412;
    private static final int ENGINE_SOUND_ID = 500;
    private static final int ENGINE_RANGE_ID = 501;
    private static final int ENGINE_REPEAT_ID = 502;
    private static final int ENGINE_SOUND_FIELD_ID = 503;
    private static final int VALUE_ID = 1000;
    private static final int AGGRO_RANGE_ID = 1100;
    private static final int FIRING_RANGE_ID = 1101;
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final String TUNING = "wolffsmodnpcs.tuning.";
    /** The overview lists its categories in two columns of this many. */
    private static final int OVERVIEW_ROWS = 7;
    private static final String HELP_SUFFIX = ".help";

    /** A page of the tab. Its rows are switches and numbers, listed top to bottom in the order given. */
    private enum Category
    {
        EQUIPMENT(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.DUAL_WIELD, Feature.TYPE_PROPERTIES, Feature.WEAPON_STATS), FIRING(Feature.PROJECTILES, Feature.GRENADES, Feature.NATIVE_THROWABLES,
            Feature.MODEL_MUZZLES, Feature.ALTERNATE_BARRELS, Feature.SECONDARY_BANK, Feature.RELOAD_CYCLE), AIMING(Feature.LEAD_TARGET, Feature.ADVANCED_LEAD, Feature.BALLISTIC_AIM,
                Feature.TURRET_MUZZLES), TURRET(Feature.GRADUAL_AIM, Value.AIM_SPEED_YAW, Value.AIM_SPEED_PITCH, Feature.FIRE_WHEN_ALIGNED, Value.ALIGN_TOLERANCE, Feature.HULL_ASSISTS_AIM,
                    Feature.PASSENGER_GUNS, Feature.TURRET_LOCKED), VEHICLE(Feature.HULL_FACES_TRAVEL, Value.HULL_TURN_RATE, Feature.MISALIGNED_THROTTLE, Value.THROTTLE_FLOOR, Feature.RIDER_AT_SEAT,
                        Feature.EYE_AT_MUZZLE), MODEL(Feature.ANIMATED_MODEL, Feature.DOORS_OPEN, Feature.GEAR_UP, Feature.WINGS_FOLDED, Feature.ENGINE_RUNNING, Feature.FULL_THROTTLE,
                            Feature.MODEL_CULLING), MOVEMENT(Value.TERRAIN, Value.MAX_WADE_DEPTH, Value.MIN_WATER_DEPTH, Value.LAND_SPEED, Value.WATER_SPEED, Feature.ADAPTIVE_PATH_RANGE,
                                Value.PATH_MARGIN, Value.PATH_RETRY_INTERVAL), SENSES(Value.SIGHT, Value.VISION_LIMIT, Value.FIRE_LIMIT, Feature.PROGRESSIVE_SEARCH, Value.SEARCH_NEAR_INTERVAL,
                                    Value.SEARCH_FAR_INTERVAL, Value.TARGET_MARGIN), RANGES(Feature.STAY_ACTIVE, Value.ACTIVE_RANGE), PROTECTION(Feature.VEHICLE_DAMAGE, Feature.MODEL_ARMOUR,
                                        Feature.FLAN_DAMAGE_TYPES, Feature.INVULNERABLE), ANIMATIONS(Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS, Feature.GUN_SHOT_ANIMATION,
                                            Feature.GUN_RELOAD_ANIMATION, Feature.GUN_MELEE_ANIMATION), EFFECTS(Feature.MODEL_SOUNDS, Feature.SHOOT_PARTICLES), DISPLAY;

        private final Object[] rows;

        Category(Object... rows)
        {
            this.rows = rows;
        }

        private String key()
        {
            return "wolffsmodnpcs.category." + name().toLowerCase(Locale.ROOT);
        }

        /** Whether the page edits simulation settings rather than weapons, which its hint line reflects. */
        private boolean simulation()
        {
            return this == TURRET || this == VEHICLE || this == MODEL || this == MOVEMENT || this == SENSES || this == RANGES;
        }
    }

    private boolean loaded;
    private boolean changed;
    private Category category;
    private boolean vehicleSoundsPage;
    private boolean weaponSoundsPage;
    private NpcEngineSettings editingSound;
    private final CombatPages combat;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
        combat = new CombatPages(this, npc);
        Packets.sendServer(new SPacketMenuGet(EnumMenuType.STATS));
    }

    @Override
    public void init()
    {
        super.init();
        if (category == null)
            initOverview();
        else
            initCategory();
        String hint = category == Category.DISPLAY || vehicleSoundsPage ? "wolffsmodnpcs.presentation.hint" : "wolffsmodnpcs.weapons.ammo_hint";
        if (category == null)
            hint = TUNING + "overview_hint";
        else if (category.simulation())
            hint = TUNING + "hint";
        if (category != null && combat.active())
            hint = combat.hint();
        addLabel(new GuiLabel(101, loaded ? hint : "wolffsmodnpcs.weapons.loading", guiLeft + 8, guiTop + 203));
    }

    private void initOverview()
    {
        addLabel(new GuiLabel(100, "wolffsmodnpcs.weapons.title", guiLeft + 8, guiTop + 10));
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++)
        {
            int x = guiLeft + 8 + i / OVERVIEW_ROWS * 200;
            int y = guiTop + 29 + i % OVERVIEW_ROWS * 24;
            String key = categories[i].key();
            GuiButtonNop button = new GuiButtonNop(this, CATEGORY_BUTTON_ID + i, x, y, 192, 20, new String[]{key}, 0);
            button.setEnabled(loaded);
            button.setTooltip(Tooltip.create(Component.translatable(key + HELP_SUFFIX)));
            addButton(button);
        }
    }

    private void initCategory()
    {
        addLabel(new GuiLabel(100, pageTitle(), guiLeft + 8, guiTop + 10));
        addButton(new GuiButtonNop(this, BACK_BUTTON_ID, guiLeft + 354, guiTop + 4, 50, 20, new String[]{"gui.back"}, 0));
        if (combat.active())
            combat.init(loaded);
        else if (category == Category.DISPLAY)
        {
            NpcPresentation settings = NpcPresentation.of(npc);
            toggle(HURT_FLASH_ID, "hurt_flash", settings.isHurtFlash(), 29);
            toggle(DEATH_ROTATION_ID, "death_rotation", settings.isDeathRotation(), 53);
            toggle(HIDE_BODY_ID, "hide_body", npc.stats.hideKilledBody, 77);
        }
        else if (vehicleSoundsPage)
        {
            if (editingSound == null)
                initVehicleSounds();
            else
                initSoundEditor();
        }
        else if (weaponSoundsPage)
            initRows(new Object[]{Feature.FIRE_SOUNDS, Feature.RELOAD_SOUNDS, Feature.THROW_SOUNDS, Feature.MELEE_SOUNDS});
        else
            initSwitches();
    }

    private String pageTitle()
    {
        if (combat.active())
            return combat.title();
        if (editingSound != null)
            return editingSound == sounds().getIdle() ? "wolffsmodnpcs.presentation.edit_idle" : "wolffsmodnpcs.presentation.edit_movement";
        if (weaponSoundsPage)
            return "wolffsmodnpcs.presentation.weapon_sounds";
        return vehicleSoundsPage ? "wolffsmodnpcs.presentation.vehicle_sounds" : category.key();
    }

    /** A category's switches, followed by the controls and page links that are more than a switch. */
    private void initSwitches()
    {
        initRows(category.rows);
        int below = 29 + category.rows.length * 21;
        if (category == Category.RANGES)
        {
            rangeField(AGGRO_RANGE_ID, "aggro_range", npc.stats.aggroRange, below);
            rangeField(FIRING_RANGE_ID, "firing_range", npc.stats.ranged.getRange(), below + 21);
        }
        if (category == Category.EQUIPMENT)
            combat.initEquipmentLinks(loaded, below + 6);
        else if (category == Category.PROTECTION)
            combat.initProtection(loaded, below);
        else if (category == Category.EFFECTS)
        {
            addButton(new GuiButtonNop(this, WEAPON_SOUNDS_ID, guiLeft + 8, guiTop + 80, 160, 20, new String[]{"wolffsmodnpcs.presentation.weapon_sounds"}, 0));
            addButton(new GuiButtonNop(this, VEHICLE_SOUNDS_ID, guiLeft + 8, guiTop + 104, 160, 20, new String[]{"wolffsmodnpcs.presentation.vehicle_sounds"}, 0));
        }
    }

    /** Lays out switches and numbers one below the other; their controls share a column wide enough for either. */
    private void initRows(Object[] rows)
    {
        boolean wide = false;
        for (Object row : rows)
            wide |= row instanceof Value;
        int control = guiLeft + (wide ? 228 : 156);
        for (int i = 0; i < rows.length; i++)
        {
            int y = guiTop + 29 + i * 21;
            if (rows[i] instanceof Feature feature)
                featureRow(feature, control, y);
            else if (rows[i] instanceof Value value)
                valueRow(value, control, y);
        }
    }

    private void featureRow(Feature feature, int control, int y)
    {
        String key = "wolffsmodnpcs.weapons." + feature.name().toLowerCase(Locale.ROOT);
        addLabel(new GuiLabel(feature.ordinal(), key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        boolean enabled = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(feature);
        GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), control, y, 48, 20, new String[]{"gui.no", "gui.yes"}, enabled ? 1 : 0);
        button.setEnabled(loaded);
        addButton(button);
    }

    private void valueRow(Value value, int control, int y)
    {
        String key = TUNING + value.name().toLowerCase(Locale.ROOT);
        int id = VALUE_ID + value.ordinal();
        int current = NpcTuning.of(npc).get(value);
        addLabel(new GuiLabel(id, key, guiLeft + 8, y + 5, key + HELP_SUFFIX));
        if (value == Value.TERRAIN || value == Value.SIGHT)
        {
            Enum<?>[] choices = value == Value.TERRAIN ? NpcTuning.Terrain.values() : NpcTuning.Sight.values();
            String[] names = new String[choices.length];
            for (int choice = 0; choice < choices.length; choice++)
                names[choice] = key + "." + choices[choice].name().toLowerCase(Locale.ROOT);
            GuiButtonNop button = new GuiButtonNop(this, id, control, y, 150, 20, names, current);
            button.setEnabled(loaded);
            addButton(button);
            return;
        }
        numberField(id, control, y, current, value.minimum(), value.maximum(), value.fallback());
    }

    /** The two Custom NPCs ranges, editable here up to the server's limit rather than the editor's. */
    private void rangeField(int id, String name, int current, int y)
    {
        String key = TUNING + name;
        addLabel(new GuiLabel(id, key, guiLeft + 8, guiTop + y + 5, key + HELP_SUFFIX));
        numberField(id, guiLeft + 228, guiTop + y, current, 1, NpcMobility.rangeLimit(), current);
    }

    private void numberField(int id, int x, int y, int current, int minimum, int maximum, int fallback)
    {
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, x, y, 60, 20, Integer.toString(current));
        field.setEditable(loaded);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(minimum, maximum, fallback);
        addTextField(field);
    }

    private void toggle(int id, String name, boolean value, int y)
    {
        String key = "wolffsmodnpcs.presentation." + name;
        addLabel(new GuiLabel(id, key, guiLeft + 8, guiTop + y + 5, key + HELP_SUFFIX));
        GuiButtonNop button = new GuiButtonNop(this, id, guiLeft + 278, guiTop + y, 48, 20, new String[]{"gui.no", "gui.yes"}, value ? 1 : 0);
        button.setEnabled(loaded);
        addButton(button);
    }

    private NpcVehicleSounds sounds()
    {
        return NpcPresentation.of(npc).getVehicleSounds();
    }

    private void initVehicleSounds()
    {
        NpcVehicleSounds settings = sounds();
        toggle(IDLE_ENABLED_ID, "idle_enabled", settings.isIdleEnabled(), 29);
        toggle(MOVEMENT_ENABLED_ID, "movement_enabled", settings.isMovementEnabled(), 53);
        if (settings.isMovementEnabled())
            toggle(VARIABLE_PITCH_ID, "variable_pitch", settings.isVariablePitch(), 77);
        toggle(MODEL_DEFAULTS_ID, "model_defaults", settings.isModelDefaults(), 101);
        addButton(new GuiButtonNop(this, EDIT_IDLE_ID, guiLeft + 8, guiTop + 129, 220, 20, new String[]{"wolffsmodnpcs.presentation.edit_idle"}, 0));
        addButton(new GuiButtonNop(this, EDIT_MOVEMENT_ID, guiLeft + 8, guiTop + 153, 220, 20, new String[]{"wolffsmodnpcs.presentation.edit_movement"}, 0));
    }

    private void initSoundEditor()
    {
        boolean editable = !sounds().isModelDefaults();
        String sound = editingSound.getSound();
        addLabel(new GuiLabel(ENGINE_SOUND_ID, "wolffsmodnpcs.presentation.engine_sound", guiLeft + 8, guiTop + 34));
        GuiTextFieldNop soundField = new GuiTextFieldNop(ENGINE_SOUND_FIELD_ID, this, guiLeft + 8, guiTop + 53, 252, 20, sound);
        soundField.setMaxLength(256);
        soundField.setEditable(false);
        soundField.setTextColorUneditable(READ_ONLY_COLOR);
        addTextField(soundField);
        addButton(new GuiButtonNop(this, ENGINE_SOUND_ID, guiLeft + 266, guiTop + 53, 72, 20, new String[]{"wolffsmodnpcs.presentation.select_sound"}, 0));
        addButton(new GuiButtonNop(this, CLEAR_SOUND_ID, guiLeft + 344, guiTop + 53, 60, 20, new String[]{"wolffsmodnpcs.presentation.clear_sound"}, 0));
        getButton(ENGINE_SOUND_ID).setEnabled(loaded && editable);
        getButton(CLEAR_SOUND_ID).setEnabled(loaded && editable);
        engineField(ENGINE_RANGE_ID, "engine_range", editingSound.getRange(), 81, 4096, editable);
        if (sound.startsWith("flansmod:"))
        {
            toggle(OVERRIDE_REPEAT_ID, "override_repeat", editingSound.isOverrideRepeat(), 105);
            getButton(OVERRIDE_REPEAT_ID).setEnabled(loaded && editable);
        }
        engineField(ENGINE_REPEAT_ID, "engine_repeat", editingSound.effectiveRepeatTicks(), 129, 12000, editable && !editingSound.automaticRepeat());
        if (!editable)
            addLabel(new GuiLabel(601, "wolffsmodnpcs.presentation.vehicle_read_only", guiLeft + 8, guiTop + 180));
        addLabel(new GuiLabel(600, editingSound.automaticRepeat() ? "wolffsmodnpcs.presentation.repeat_automatic" : "wolffsmodnpcs.presentation.repeat_manual", guiLeft + 8, guiTop + 160));
    }

    private void engineField(int id, String name, int value, int y, int maximum, boolean enabled)
    {
        String key = "wolffsmodnpcs.presentation." + name;
        addLabel(new GuiLabel(id, key, guiLeft + 8, guiTop + y + 5, key + HELP_SUFFIX));
        GuiTextFieldNop field = new GuiTextFieldNop(id, this, guiLeft + 278, guiTop + y, 70, 20, Integer.toString(value));
        // Custom NPCs hides disabled fields entirely; EditBox's editability keeps read-only values visible.
        field.setEditable(loaded && enabled);
        field.setTextColorUneditable(READ_ONLY_COLOR);
        field.numbersOnly = true;
        field.setMinMaxDefault(1, maximum, value);
        addTextField(field);
    }

    @Override
    public void subGuiClosed(Screen subgui)
    {
        if (subgui instanceof GuiSoundSelection picker && picker.selectedResource != null && editingSound != null && !sounds().isModelDefaults())
        {
            editingSound.setSound(picker.selectedResource.toString());
            changed = true;
        }
        init();
    }

    @Override
    public void unFocused(GuiTextFieldNop field)
    {
        if (loaded && combat.unFocused(field))
        {
            changed = true;
            return;
        }
        if (loaded && editNumber(field))
        {
            changed = true;
            return;
        }
        if (!loaded || editingSound == null || sounds().isModelDefaults())
            return;
        switch (field.id)
        {
            case ENGINE_RANGE_ID -> editingSound.setRange(field.isInteger() ? field.getInteger() : 1);
            case ENGINE_REPEAT_ID -> {
                if (editingSound.automaticRepeat())
                    return;
                editingSound.setRepeatTicks(field.isInteger() ? field.getInteger() : 20);
            }
            default -> {
                return;
            }
        }
        changed = true;
    }

    /** @return whether the field was one of the simulation numbers or ranges */
    private boolean editNumber(GuiTextFieldNop field)
    {
        if (!field.isInteger())
            return false;
        if (field.id >= VALUE_ID && field.id < VALUE_ID + Value.values().length)
            NpcTuning.of(npc).set(Value.values()[field.id - VALUE_ID], field.getInteger());
        else if (field.id == AGGRO_RANGE_ID)
            npc.stats.aggroRange = Math.max(1, Math.min(NpcMobility.rangeLimit(), field.getInteger()));
        else if (field.id == FIRING_RANGE_ID)
            npc.stats.ranged.setRange(Math.max(1, Math.min(NpcMobility.rangeLimit(), field.getInteger())));
        else
            return false;
        return true;
    }

    @Override
    public void buttonEvent(GuiButtonNop button)
    {
        if (!loaded)
            return;
        GuiTextFieldNop.unfocus();
        if (button.id == BACK_BUTTON_ID)
        {
            goBack();
        }
        else if (button.id >= CATEGORY_BUTTON_ID && button.id < CATEGORY_BUTTON_ID + Category.values().length)
            category = Category.values()[button.id - CATEGORY_BUTTON_ID];
        else if (button.id >= 0 && button.id < Feature.values().length)
        {
            ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().set(Feature.values()[button.id], button.getValue() == 1);
            changed = true;
        }
        else if (button.id >= VALUE_ID && button.id < VALUE_ID + Value.values().length)
        {
            NpcTuning.of(npc).set(Value.values()[button.id - VALUE_ID], button.getValue());
            changed = true;
        }
        else if (combat.handles(button.id))
            changed |= combat.buttonEvent(button);
        else if (button.id == ENGINE_SOUND_ID)
        {
            String selection = editingSound.getSound();
            // Select the namespace without committing an empty-path placeholder when the picker closes.
            GuiSoundSelection picker = new GuiSoundSelection(selection.isBlank() ? "flansmod:" : selection);
            if (selection.isBlank())
                picker.selectedResource = null;
            setSubGui(picker);
            return;
        }
        else
        {
            editSettings(button);
            changed = true;
        }
        init();
    }

    private void goBack()
    {
        if (combat.back())
            return;
        if (editingSound != null)
            editingSound = null;
        else if (vehicleSoundsPage)
            vehicleSoundsPage = false;
        else if (weaponSoundsPage)
            weaponSoundsPage = false;
        else
            category = null;
    }

    private void editSettings(GuiButtonNop button)
    {
        NpcPresentation settings = NpcPresentation.of(npc);
        boolean yes = button.getValue() == 1;
        switch (button.id)
        {
            case HURT_FLASH_ID -> settings.setHurtFlash(yes);
            case DEATH_ROTATION_ID -> settings.setDeathRotation(yes);
            case HIDE_BODY_ID -> npc.stats.hideKilledBody = yes;
            case VEHICLE_SOUNDS_ID -> vehicleSoundsPage = true;
            case WEAPON_SOUNDS_ID -> weaponSoundsPage = true;
            case IDLE_ENABLED_ID -> sounds().setIdleEnabled(yes);
            case MOVEMENT_ENABLED_ID -> sounds().setMovementEnabled(yes);
            case VARIABLE_PITCH_ID -> sounds().setVariablePitch(yes);
            case MODEL_DEFAULTS_ID -> {
                if (yes)
                    sounds().useVehicleDefaults(npc);
                else
                {
                    sounds().setModelDefaults(false);
                    sounds().initializeCustom(npc);
                }
            }
            case EDIT_IDLE_ID, EDIT_MOVEMENT_ID -> {
                if (sounds().isModelDefaults())
                    sounds().useVehicleDefaults(npc);
                else
                    sounds().initializeCustom(npc);
                editingSound = button.id == EDIT_IDLE_ID ? sounds().getIdle() : sounds().getMovement();
            }
            case CLEAR_SOUND_ID -> editingSound.setSound("");
            case OVERRIDE_REPEAT_ID -> {
                int interval = editingSound.effectiveRepeatTicks();
                editingSound.setOverrideRepeat(yes);
                if (yes)
                    editingSound.setRepeatTicks(interval);
            }
            default -> {
                /* Other GUI buttons belong to the native tab base. */ }
        }
    }

    @Override
    public void setGuiData(CompoundTag tag)
    {
        npc.stats.readToNBT(tag);
        loaded = true;
        editingSound = null;
        init();
    }

    public static GuiMenuTopButton createTab(IGuiInterface parent, EntityNPCInterface npc, int x, int y)
    {
        return new GuiMenuTopButton(parent, MENU_ID, x, y, "menu.wolffsmodnpcs")
        {
            @Override
            public void onClick(double mouseX, double mouseY)
            {
                GuiTextFieldNop.unfocus();
                parent.save();
                NoppesUtil.openGUI(Minecraft.getInstance().player, new GuiWolffsMod(npc));
            }
        };
    }

    @Override
    public void save()
    {
        GuiTextFieldNop.unfocus();
        if (loaded && changed)
        {
            Packets.sendServer(new SPacketMenuSave(EnumMenuType.STATS, npc.stats.save(new CompoundTag())));
            combat.saved();
            changed = false;
        }
    }
}
