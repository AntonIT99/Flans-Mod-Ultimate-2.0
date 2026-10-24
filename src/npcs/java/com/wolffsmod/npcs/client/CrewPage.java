package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.crew.CrewSource;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.crew.NpcCrewSettings;
import com.wolffsmod.npcs.network.CrewActionPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.List;

/**
 * The Crew tab of the Wolff's Mod tab, for a vehicle, plane or mecha NPC: who crews each kind of seat, a saved clone or a
 * new NPC with a class loadout, what the crew may do and suffer aboard, and buttons that crew the empty seats now or
 * remove the crew. The settings travel with the stats menu; the buttons ask the server, which checks the editor's
 * permissions.
 */
final class CrewPage
{
    static final String TAB = "crew";
    private static final String PREFIX = "wolffsmodnpcs.crew.";
    private static final String HELP = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    private static final int POST_ID = 2300;
    private static final int SOURCE_ID = 2301;
    private static final int SELECT_ID = 2302;
    private static final int SHIELDED_ID = 2306;
    private static final int PASSENGERS_FIRE_ID = 2307;
    private static final int DESTROYED_ID = 2308;
    private static final int AUTO_ID = 2309;
    private static final int CREW_NOW_ID = 2310;
    private static final int REMOVE_ID = 2311;
    private static final int LABEL_ID = 2320;
    private static final int LAST_ID = 2329;
    private static final int ROW_HEIGHT = 21;
    private static final int HALF_WIDTH = 196;

    private final GuiWolffsMod gui;
    private final EntityNPCInterface npc;
    /** The kind of seat whose crew is shown; choosing it changes nothing about the NPC. */
    private CrewPost post = CrewPost.DRIVER;
    private boolean ready;
    /** Where the status line stands and whether the model has seats of the kind shown, to redraw the line after an edit. */
    private int statusX;
    private int statusY;
    private boolean postHasSeats;

    CrewPage(GuiWolffsMod gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
    }

    int changes(String tab)
    {
        return TAB.equals(tab) ? settings().changes() : 0;
    }

    void init(boolean loaded, int top)
    {
        ready = loaded;
        int left = gui.guiLeft + 8;
        int y = gui.guiTop + top;
        List<WeaponMount> seats = NpcKind.model(npc).map(FlansEntityTypes::getMounts).orElse(List.of());
        GuiButtonNop postButton = button(POST_ID, left, y, 160, Arrays.stream(CrewPost.values()).map(value -> PREFIX + "post." + value.id()).toArray(String[]::new), post.ordinal());
        postButton.setMessage(Component.translatable(PREFIX + "post." + post.id()).append(" (" + seats.stream().filter(seat -> CrewPost.of(seat) == post).count() + ")"));
        CrewSource source = settings().source(post);
        button(SOURCE_ID, left + 164, y, 232, Arrays.stream(CrewSource.Kind.values()).map(CrewSource.Kind::key).toArray(String[]::new), source.kind().ordinal());
        switch (source.kind())
        {
            case CLONE, CLASS -> {
                GuiButtonNop select = button(SELECT_ID, left, y + ROW_HEIGHT, 396, new String[]{PREFIX + "select." + source.kind().name().toLowerCase(java.util.Locale.ROOT)}, 0);
                select.setTooltip(Tooltip.create(Component.translatable(PREFIX + "select.help")));
            }
            case NONE -> {
                // Nobody crews these seats; there is nothing to name.
            }
        }
        statusX = left;
        statusY = y + 2 * ROW_HEIGHT + 5;
        postHasSeats = seats.stream().anyMatch(seat -> CrewPost.of(seat) == post);
        showStatus();
        toggle(SHIELDED_ID, left, y + 3 * ROW_HEIGHT, "shielded", settings().isProtectedCrew());
        toggle(PASSENGERS_FIRE_ID, left + HALF_WIDTH + 4, y + 3 * ROW_HEIGHT, "passengers_fire", settings().isPassengersFire());
        NpcCrewSettings.Destroyed destroyed = settings().getDestroyed();
        GuiButtonNop destroyedButton = button(DESTROYED_ID, left, y + 4 * ROW_HEIGHT, 396, Arrays.stream(NpcCrewSettings.Destroyed.values()).map(NpcCrewSettings.Destroyed::key).toArray(String[]::new),
            destroyed.ordinal());
        destroyedButton.setMessage(Component.translatable(PREFIX + "destroyed", Component.translatable(destroyed.key())));
        destroyedButton.setTooltip(Tooltip.create(Component.translatable(PREFIX + "destroyed" + HELP)));
        toggle(AUTO_ID, left, y + 5 * ROW_HEIGHT, "auto", settings().isAutoCrew());
        action(CREW_NOW_ID, left + HALF_WIDTH + 4, y + 5 * ROW_HEIGHT, "crew_now", !seats.isEmpty());
        action(REMOVE_ID, left + HALF_WIDTH + 4 + 98, y + 5 * ROW_HEIGHT, "remove", npc.getPassengers().stream().anyMatch(passenger -> NpcCrew.seat(passenger) >= 0));
    }

    /** Adds the status line, or replaces it: labels are kept by id. */
    private void showStatus()
    {
        gui.addLabel(new GuiLabel(LABEL_ID + 2, status(settings().source(post), postHasSeats), 0xE0E0E0, statusX, statusY, 396, 10));
    }

    /** @return what the source of the shown seats will crew them with, in red when it names nobody that exists */
    private static Component status(CrewSource source, boolean hasSeats)
    {
        if (!hasSeats)
            return Component.translatable(PREFIX + "status.no_seats").withStyle(ChatFormatting.GRAY);
        return switch (source.kind())
        {
            case NONE -> Component.translatable(PREFIX + "status.empty");
            case CLONE -> source.names()
                ? Component.translatable(PREFIX + "status.clone", source.cloneName(), source.cloneTab())
                : Component.translatable(PREFIX + "status.clone_unnamed").withStyle(ChatFormatting.RED);
            case CLASS -> FlanSoldierSelection.resolve(source.team(), source.playerClass()).<Component>map(selection ->
            {
                MutableComponent text = Component.translatable(PREFIX + "status.class", selection.playerClass().getName());
                return selection.team() == null ? text : text.append(Component.translatable(PREFIX + "status.of_team", selection.team().getName()));
            }).orElseGet(() -> Component.translatable(PREFIX + "status.unknown_class", source.playerClass(), source.team()).withStyle(ChatFormatting.RED));
        };
    }

    private GuiButtonNop button(int id, int x, int y, int width, String[] labels, int value)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, x, y, width, 20, labels, value);
        button.setEnabled(ready);
        gui.addButton(button);
        return button;
    }

    private void toggle(int id, int x, int y, String key, boolean value)
    {
        GuiButtonNop button = button(id, x, y, HALF_WIDTH, NO_YES, value ? 1 : 0);
        button.setMessage(Component.translatable(PREFIX + key, Component.translatable(NO_YES[value ? 1 : 0])));
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + HELP)));
    }

    private void action(int id, int x, int y, String key, boolean enabled)
    {
        GuiButtonNop button = button(id, x, y, 94, new String[]{PREFIX + key}, 0);
        button.setEnabled(ready && enabled);
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + HELP)));
    }

    boolean handles(int id)
    {
        return id >= POST_ID && id <= LAST_ID;
    }

    /** @return whether the settings changed; choosing the kind of seat shown and the action buttons change none */
    boolean button(GuiButtonNop button)
    {
        NpcCrewSettings settings = settings();
        boolean yes = button.getValue() == 1;
        switch (button.id)
        {
            case POST_ID -> post = CrewPost.values()[button.getValue()];
            case SOURCE_ID -> {
                settings.setSource(post, settings.source(post).withKind(CrewSource.Kind.values()[button.getValue()]));
                return true;
            }
            case SELECT_ID -> gui.setSubGui(new CrewSelectionMenu(settings.source(post)));
            case SHIELDED_ID -> {
                settings.setProtectedCrew(yes);
                return true;
            }
            case PASSENGERS_FIRE_ID -> {
                settings.setPassengersFire(yes);
                return true;
            }
            case DESTROYED_ID -> {
                settings.setDestroyed(NpcCrewSettings.Destroyed.values()[button.getValue()]);
                return true;
            }
            case AUTO_ID -> {
                settings.setAutoCrew(yes);
                return true;
            }
            case CREW_NOW_ID -> NpcNetwork.sendToServer(new CrewActionPacket(npc.getId(), CrewActionPacket.Action.CREW, settings.write()));
            case REMOVE_ID -> NpcNetwork.sendToServer(new CrewActionPacket(npc.getId(), CrewActionPacket.Action.REMOVE, settings.write()));
            default -> {
                // Labels are not buttons.
            }
        }
        return false;
    }

    /** Applies a confirmed menu choice; cancelling leaves this seat's source untouched. */
    boolean selected(CrewSelectionMenu menu)
    {
        CrewSource source = settings().source(post);
        CrewSource selection = menu.selection().orElse(source);
        if (source.equals(selection))
            return false;
        settings().setSource(post, selection);
        return true;
    }

    private NpcCrewSettings settings()
    {
        return NpcCrewSettings.of(npc);
    }
}
