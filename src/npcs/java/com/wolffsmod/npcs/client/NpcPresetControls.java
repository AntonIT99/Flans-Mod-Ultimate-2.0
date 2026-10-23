package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.network.*;
import com.wolffsmod.npcs.presets.*;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.*;

import java.util.*;

/** Shared preset selection, compact previews and server state for the overview and model picker. Client only. */
public final class NpcPresetControls
{
    private static final String PREFIX = "wolffsmodnpcs.preset.";
    private static final Map<EntityNPCInterface, Selection> SELECTIONS = new WeakHashMap<>();

    private NpcPresetControls()
    {}

    private static final class Selection
    {
        private NpcPreset preset = NpcPreset.HUMAN;
        private String model;
        private boolean loaded;
        private boolean canUndo;
    }

    private static Selection state(EntityNPCInterface npc)
    {
        return SELECTIONS.computeIfAbsent(npc, key -> new Selection());
    }

    /** Changes suggestions only when the selected model or class changes, never on folder or variant navigation. */
    public static NpcPreset selected(EntityNPCInterface npc)
    {
        Selection state = state(npc);
        String identity = "";
        if (npc instanceof EntityCustomNpc custom)
        {
            String playerClass = custom.modelData.extra.getString(FlanSoldierSelection.CLASS_KEY);
            // Skin variants may use different human model layouts without changing the selected class.
            identity = playerClass.isEmpty() ? Objects.toString(custom.modelData.getEntityName(), "") : "class:" + playerClass;
        }
        if (!Objects.equals(identity, state.model))
        {
            state.model = identity;
            state.preset = NpcPreset.suggested(npc);
        }
        return state.preset;
    }

    public static void select(EntityNPCInterface npc, int ordinal)
    {
        state(npc).preset = NpcPreset.values()[Math.floorMod(ordinal, NpcPreset.values().length)];
    }

    public static void receive(EntityNPCInterface npc, NpcPresetPacket packet)
    {
        state(npc).loaded = true;
        state(npc).canUndo = packet.values().getBoolean("CanUndo");
    }

    public static boolean canUndo(EntityNPCInterface npc)
    {
        return state(npc).loaded && state(npc).canUndo;
    }

    public static void addPicker(GuiCreationScreenInterface screen)
    {
        EntityNPCInterface npc = screen.npc;
        Selection state = state(npc);
        NpcPreset preset = selected(npc);
        int x = screen.guiLeft + 124;
        int y = screen.guiTop + 212;
        if (screen.getSlider(500) != null)
            screen.getSlider(500).setY(screen.guiTop + 190);
        GuiButtonNop select = new GuiButtonNop(screen, 9130, x, y, 140, 20, button ->
        {
            select(npc, ((GuiButtonNop) button).getValue());
            screen.init();
        }, preset.ordinal(), Arrays.stream(NpcPreset.values()).map(NpcPreset::key).toArray(String[]::new));
        select.setTooltip(Tooltip.create(tooltip(npc, preset, state.loaded)));
        screen.addButton(select);
        GuiButtonNop apply = new GuiButtonNop(screen, 9131, x + 144, y, 60, 20, PREFIX + "apply", button ->
        {
            screen.save();
            NpcNetwork.sendToServer(NpcPresetPacket.apply(npc.getId(), selected(npc)));
        });
        apply.setTooltip(select.getTooltip());
        apply.setEnabled(state.loaded && !preset.matches(npc));
        screen.addButton(apply);
        GuiButtonNop undo = new GuiButtonNop(screen, 9132, x + 208, y, 64, 20, PREFIX + "undo", button ->
        {
            screen.save();
            NpcNetwork.sendToServer(NpcPresetPacket.undo(npc.getId()));
        });
        undo.setTooltip(Tooltip.create(Component.translatable(PREFIX + "undo.help")));
        undo.setEnabled(canUndo(npc));
        screen.addButton(undo);
    }

    public static Component tooltip(EntityNPCInterface npc, NpcPreset preset, boolean loaded)
    {
        MutableComponent text = Component.empty().append(Component.translatable(preset.key()).withStyle(ChatFormatting.BOLD));
        text.append("\n").append(Component.translatable(preset.key() + ".help").withStyle(ChatFormatting.RESET));
        if (!loaded)
            return text.append("\n").append(Component.translatable(PREFIX + "loading"));
        for (String group : List.of("movement", "combat", "survival", "appearance"))
        {
            List<NpcPreset.Target> targets = preset.getTargets().stream().filter(target -> target.setting().group().equals(group)).toList();
            if (targets.isEmpty())
                continue;
            text.append("\n").append(Component.translatable(PREFIX + "group." + group).withStyle(ChatFormatting.GOLD));
            for (int index = 0; index < targets.size(); index++)
            {
                NpcPreset.Target target = targets.get(index);
                NpcPresetSetting setting = target.setting();
                int value = preset.value(npc, target);
                text.append(index % 2 == 0 ? "\n" : "  |  ").append(Component.translatable(PREFIX + "entry", Component.translatable(setting.key()), value(setting, value))
                    .withStyle(setting.get(npc) == value ? ChatFormatting.GRAY : ChatFormatting.YELLOW));
            }
        }
        return text;
    }

    private static Component value(NpcPresetSetting setting, int value)
    {
        if (setting == NpcPresetSetting.WALKING_RANGE && value == 0)
            return Component.translatable(PREFIX + "unlimited");
        int display = setting.displayValue(value);
        if (setting == NpcPresetSetting.TERRAIN || setting == NpcPresetSetting.FLIGHT_TYPE || setting == NpcPresetSetting.TACTIC)
        {
            String name = switch (setting)
            {
                case TERRAIN -> NpcTuning.Terrain.values()[value].name();
                case FLIGHT_TYPE -> NpcTuning.Flight.values()[value].name();
                default -> NpcTuning.Tactic.values()[value].name();
            };
            String prefix = switch (setting)
            {
                case TERRAIN -> "terrain";
                case FLIGHT_TYPE -> "flight_type";
                default -> "tactic";
            };
            return Component.translatable("wolffsmodnpcs.tuning." + prefix + "." + name.toLowerCase(Locale.ROOT));
        }
        if (!setting.getValueKeys().isEmpty())
            return Component.translatable(setting.getValueKeys().get(display));
        if (setting == NpcPresetSetting.CAN_DROWN || setting == NpcPresetSetting.NO_FALL_DAMAGE || setting == NpcPresetSetting.POTION_IMMUNE || setting == NpcPresetSetting.IGNORE_COBWEB)
            return Component.translatable(display == 0 ? "gui.no" : "gui.yes");
        return Component.literal(Integer.toString(display));
    }
}
