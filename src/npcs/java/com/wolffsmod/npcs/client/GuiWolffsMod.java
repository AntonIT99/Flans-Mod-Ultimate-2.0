package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.properties.*;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;

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
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final String HELP_SUFFIX = ".help";

    private enum Category
    {
        EQUIPMENT(Feature.ITEM_WEAPONS, Feature.ITEM_ARMOR, Feature.TYPE_PROPERTIES, Feature.WEAPON_STATS), FIRING(Feature.PROJECTILES, Feature.GRENADES, Feature.MODEL_MUZZLES,
            Feature.ALTERNATE_BARRELS, Feature.SECONDARY_BANK), AIMING(Feature.LEAD_TARGET,
                Feature.BALLISTIC_AIM), ANIMATIONS(Feature.WEAPON_ANIMATIONS, Feature.ARMOR_ANIMATIONS), EFFECTS(Feature.MODEL_SOUNDS, Feature.SHOOT_PARTICLES), DISPLAY;
        private final Feature[] features;
        Category(Feature... features)
        {
            this.features = features;
        }

        private String key()
        {
            return "wolffsmodnpcs.category." + name().toLowerCase(Locale.ROOT);
        }
    }

    private boolean loaded;
    private boolean changed;
    private Category category;
    private boolean vehicleSoundsPage;
    private boolean weaponSoundsPage;
    private NpcEngineSettings editingSound;

    public GuiWolffsMod(EntityNPCInterface npc)
    {
        super(npc, MENU_ID);
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
        addLabel(new GuiLabel(101, loaded ? hint : "wolffsmodnpcs.weapons.loading", guiLeft + 8, guiTop + 203));
    }

    private void initOverview()
    {
        addLabel(new GuiLabel(100, "wolffsmodnpcs.weapons.title", guiLeft + 8, guiTop + 10));
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++)
        {
            int y = guiTop + 29 + i * 24;
            String key = categories[i].key();
            GuiButtonNop button = new GuiButtonNop(this, CATEGORY_BUTTON_ID + i, guiLeft + 8, y, 120, 20, new String[]{key}, 0);
            button.setEnabled(loaded);
            addButton(button);
            addLabel(new GuiLabel(CATEGORY_BUTTON_ID + i, key + HELP_SUFFIX, guiLeft + 136, y + 6));
        }
    }

    private void initCategory()
    {
        String title = vehicleSoundsPage ? "wolffsmodnpcs.presentation.vehicle_sounds" : category.key();
        if (weaponSoundsPage)
            title = "wolffsmodnpcs.presentation.weapon_sounds";
        if (editingSound != null)
            title = editingSound == sounds().getIdle() ? "wolffsmodnpcs.presentation.edit_idle" : "wolffsmodnpcs.presentation.edit_movement";
        addLabel(new GuiLabel(100, title, guiLeft + 8, guiTop + 10));
        addButton(new GuiButtonNop(this, BACK_BUTTON_ID, guiLeft + 354, guiTop + 4, 50, 20, new String[]{"gui.back"}, 0));
        if (category == Category.DISPLAY)
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
            initFeatures(Feature.FIRE_SOUNDS, Feature.RELOAD_SOUNDS, Feature.THROW_SOUNDS);
        else
        {
            initFeatures(category.features);
            if (category == Category.EFFECTS)
            {
                addButton(new GuiButtonNop(this, WEAPON_SOUNDS_ID, guiLeft + 8, guiTop + 80, 160, 20, new String[]{"wolffsmodnpcs.presentation.weapon_sounds"}, 0));
                addButton(new GuiButtonNop(this, VEHICLE_SOUNDS_ID, guiLeft + 8, guiTop + 104, 160, 20, new String[]{"wolffsmodnpcs.presentation.vehicle_sounds"}, 0));
            }
        }
    }

    private void initFeatures(Feature... features)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        for (int i = 0; i < features.length; i++)
        {
            Feature feature = features[i];
            int x = guiLeft + 8;
            int y = guiTop + 29 + i * 21;
            String key = "wolffsmodnpcs.weapons." + feature.name().toLowerCase(Locale.ROOT);
            addLabel(new GuiLabel(feature.ordinal(), key, x, y + 5, key + HELP_SUFFIX));
            GuiButtonNop button = new GuiButtonNop(this, feature.ordinal(), x + 148, y, 48, 20, new String[]{"gui.no", "gui.yes"}, options.enabled(feature) ? 1 : 0);
            button.setEnabled(loaded);
            addButton(button);
        }
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
            changed = false;
        }
    }
}
