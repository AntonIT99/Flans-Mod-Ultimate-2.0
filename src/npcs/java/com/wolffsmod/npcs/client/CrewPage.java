package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.driveables.WeaponMount;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.teams.SkinOverrideVariant;
import com.wolffsmod.npcs.crew.CrewPost;
import com.wolffsmod.npcs.crew.CrewSource;
import com.wolffsmod.npcs.crew.NpcCrewSettings;
import com.wolffsmod.npcs.network.CrewActionPacket;
import com.wolffsmod.npcs.network.CrewResultPacket;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * The Crew menu: individual seats and their clone or class selections, plus shared rules on a separate tab. Legacy
 * role-wide sources remain the fallback until a seat is edited. Actions carry the current settings to the server and
 * wait for its result; opening or cancelling a picker never changes the settings.
 */
final class CrewPage implements ICustomScrollListener
{
    static final String TAB = "crew";
    static final String RULES_TAB = "crew_rules";
    private static final String PREFIX = "wolffsmodnpcs.crew.";
    private static final String HELP = ".help";
    private static final String[] NO_YES = {"gui.no", "gui.yes"};
    private static final int SEAT_ID = 2300;
    private static final int SOURCE_ID = 2301;
    private static final int SELECT_ID = 2302;
    private static final int COPY_TO_ID = 2303;
    private static final int COPY_ALL_ID = 2304;
    private static final int SHIELDED_ID = 2306;
    private static final int PASSENGERS_FIRE_ID = 2307;
    private static final int DESTROYED_ID = 2308;
    private static final int AUTO_ID = 2309;
    private static final int CREW_NOW_ID = 2310;
    private static final int REMOVE_ID = 2311;
    private static final int VISIBLE_ID = 2312;
    private static final int DISEMBARK_ID = 2313;
    private static final int PASSENGERS_DISEMBARK_ID = 2314;
    /** The seat's disembark choice as its button cycles: as its post, yes, no. */
    private static final String[] DISEMBARK_CHOICES = {PREFIX + "disembark.post", "gui.yes", "gui.no"};
    private static final int LABEL_ID = 2320;
    private static final int ROW_HEIGHT = 21;
    private static final int HALF_WIDTH = 196;

    private final GuiWolffsMod gui;
    private final EntityNPCInterface npc;
    private final GuiCustomScrollNop seatList;
    private List<WeaponMount> seats = List.of();
    private int selectedSeat;
    /** The seat that opened the picker, retained even if a server update rebuilds the page. */
    private int pickingSeat;
    private boolean ready;
    private boolean waiting;
    private Component result;

    CrewPage(GuiWolffsMod gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
        seatList = new GuiCustomScrollNop(gui, SEAT_ID);
        seatList.listener = this;
    }

    int changes(String tab)
    {
        if (TAB.equals(tab))
            return settings().selectionChanges();
        return RULES_TAB.equals(tab) ? settings().changes() - settings().selectionChanges() : 0;
    }

    static boolean owns(String tab)
    {
        return TAB.equals(tab) || RULES_TAB.equals(tab);
    }

    void init(String tab, boolean loaded, int top)
    {
        ready = loaded;
        int left = gui.guiLeft + 8;
        int y = gui.guiTop + top;
        seats = NpcKind.model(npc).map(FlansEntityTypes::getMounts).orElse(List.of());
        if (TAB.equals(tab))
            initSeats(left, y);
        else
            initRules(left, y);
        Component status = result;
        if (status == null && TAB.equals(tab))
            status = seats.isEmpty() ? Component.translatable(PREFIX + "no_seats") : status(source());
        if (status != null)
        {
            int statusWidth = 396;
            String text = Minecraft.getInstance().font.plainSubstrByWidth(status.getString(), statusWidth - 6);
            GuiLabel label = new GuiLabel(LABEL_ID, Component.literal(text).withStyle(status.getStyle()), 0x303030, left, gui.guiTop + 162, statusWidth, 10);
            label.setTooltip(Tooltip.create(status));
            gui.addLabel(label);
        }
        action(CREW_NOW_ID, left, gui.guiTop + 178, HALF_WIDTH, "crew_now", !seats.isEmpty());
        // The server decides whether there is crew to remove; passengers may not have reached the client yet.
        action(REMOVE_ID, left + HALF_WIDTH + 4, gui.guiTop + 178, HALF_WIDTH, "remove", !seats.isEmpty());
    }

    private void initSeats(int left, int y)
    {
        if (seats.stream().noneMatch(seat -> seat.seat() == selectedSeat) && !seats.isEmpty())
            selectedSeat = seats.get(0).seat();
        seatList.setSize(160, 4 * ROW_HEIGHT + 20);
        seatList.guiLeft = left;
        seatList.guiTop = y;
        seatList.setUnsortedList(
            seats.stream().map(seat -> Component.translatable(PREFIX + "seat", seat.seat() + 1, Component.translatable(PREFIX + "post." + CrewPost.of(seat).id())).getString()).toList());
        seatList.setSelectedIndex(seatIndex());
        gui.addScroll(seatList);
        CrewSource source = source();
        int x = left + 164;
        GuiButtonNop kind = button(SOURCE_ID, x, y, 232, Arrays.stream(CrewSource.Kind.values()).map(CrewSource.Kind::key).toArray(String[]::new), source.kind().ordinal());
        kind.setEnabled(ready && !seats.isEmpty());
        if (source.kind() != CrewSource.Kind.NONE)
            action(SELECT_ID, x, y + ROW_HEIGHT, 232, "select." + source.kind().name().toLowerCase(Locale.ROOT), !seats.isEmpty());
        toggle(VISIBLE_ID, x, y + 2 * ROW_HEIGHT, 232, "visible", settings().isSeatVisible(selectedSeat)).setEnabled(ready && !seats.isEmpty());
        disembarkButton(x, y + 3 * ROW_HEIGHT);
        action(COPY_ALL_ID, x, y + 4 * ROW_HEIGHT, 114, "copy_all", seats.size() > 1);
        action(COPY_TO_ID, x + 118, y + 4 * ROW_HEIGHT, 114, "copy_to", seats.size() > 1);
    }

    /** The selected seat's choice whether its crew member gets out to fight, showing what following its post comes to. */
    private void disembarkButton(int x, int y)
    {
        int index = seatIndex();
        Boolean own = settings().seatDisembarks(selectedSeat).orElse(null);
        int value = 0;
        if (own != null)
            value = Boolean.TRUE.equals(own) ? 1 : 2;
        GuiButtonNop button = button(DISEMBARK_ID, x, y, 232, DISEMBARK_CHOICES, value);
        boolean byPost = index >= 0 && settings().disembarks(selectedSeat, CrewPost.of(seats.get(index)));
        String byPostLabel = NO_YES[byPost ? 1 : 0];
        Component choice = own == null ? Component.translatable(DISEMBARK_CHOICES[0], Component.translatable(byPostLabel)) : Component.translatable(DISEMBARK_CHOICES[value]);
        button.setMessage(Component.translatable(PREFIX + "disembark", choice));
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + "disembark" + HELP)));
        button.setEnabled(ready && index >= 0);
    }

    private void initRules(int left, int y)
    {
        toggle(AUTO_ID, left, y, HALF_WIDTH, "auto", settings().isAutoCrew());
        toggle(SHIELDED_ID, left + HALF_WIDTH + 4, y, HALF_WIDTH, "shielded", settings().isProtectedCrew());
        NpcCrewSettings.Destroyed destroyed = settings().getDestroyed();
        GuiButtonNop destroyedButton = button(DESTROYED_ID, left, y + ROW_HEIGHT, 396, Arrays.stream(NpcCrewSettings.Destroyed.values()).map(NpcCrewSettings.Destroyed::key).toArray(String[]::new),
            destroyed.ordinal());
        destroyedButton.setMessage(Component.translatable(PREFIX + "destroyed", Component.translatable(destroyed.key())));
        destroyedButton.setTooltip(Tooltip.create(Component.translatable(PREFIX + "destroyed" + HELP)));
        toggle(PASSENGERS_FIRE_ID, left, y + 2 * ROW_HEIGHT, HALF_WIDTH, "passengers_fire", settings().isPassengersFire());
        toggle(PASSENGERS_DISEMBARK_ID, left + HALF_WIDTH + 4, y + 2 * ROW_HEIGHT, HALF_WIDTH, "passengers_disembark", settings().isPassengersDisembark());
    }

    private CrewSource source()
    {
        int index = seatIndex();
        return index < 0 ? CrewSource.NONE : settings().source(selectedSeat, CrewPost.of(seats.get(index)));
    }

    private int seatIndex()
    {
        for (int i = 0; i < seats.size(); i++)
            if (seats.get(i).seat() == selectedSeat)
                return i;
        return -1;
    }

    private List<WeaponMount> roleTargets(CrewPost post)
    {
        return seats.stream().filter(seat -> seat.seat() != selectedSeat && CrewPost.of(seat) == post).toList();
    }

    private EnumSet<CrewCopyMenu.Target> copyTargets()
    {
        EnumSet<CrewCopyMenu.Target> targets = EnumSet.noneOf(CrewCopyMenu.Target.class);
        if (seatIndex() >= 0 && seatIndex() + 1 < seats.size())
            targets.add(CrewCopyMenu.Target.NEXT);
        if (!roleTargets(CrewPost.DRIVER).isEmpty())
            targets.add(CrewCopyMenu.Target.DRIVER);
        if (!roleTargets(CrewPost.GUNNER).isEmpty())
            targets.add(CrewCopyMenu.Target.GUNNERS);
        if (!roleTargets(CrewPost.PASSENGER).isEmpty())
            targets.add(CrewCopyMenu.Target.PASSENGERS);
        return targets;
    }

    private void copyToSeats(List<WeaponMount> targets)
    {
        targets.forEach(seat -> copyTo(seat.seat()));
    }

    /** Gives a seat the selected seat's source, visibility and disembark choice. */
    private void copyTo(int seat)
    {
        settings().setSource(seat, source());
        settings().setSeatVisible(seat, settings().isSeatVisible(selectedSeat));
        settings().setSeatDisembarks(seat, settings().seatDisembarks(selectedSeat).orElse(null));
    }

    private static Component status(CrewSource source)
    {
        return switch (source.kind())
        {
            case NONE -> Component.translatable(PREFIX + "status.empty");
            case CLONE -> source.names()
                ? Component.translatable(PREFIX + "status.clone", source.cloneName(), source.cloneTab())
                : Component.translatable(PREFIX + "status.clone_unnamed").withStyle(ChatFormatting.DARK_RED);
            case CLASS -> FlanSoldierSelection.resolve(source.team(), source.playerClass()).<Component>map(selection ->
            {
                MutableComponent text = Component.translatable(PREFIX + "status.class", selection.playerClass().getNameComponent());
                if (selection.team() != null)
                    text.append(Component.translatable(PREFIX + "status.of_team", selection.team().getNameComponent()));
                var skins = selection.playerClass().getSkinOverrideVariants();
                if (!skins.isEmpty())
                    text.append(" · ").append(skinLabel(source, skins));
                return text;
            }).orElseGet(() -> Component.translatable(PREFIX + "status.unknown_class", source.playerClass(), source.team()).withStyle(ChatFormatting.DARK_RED));
        };
    }

    private GuiButtonNop button(int id, int x, int y, int width, String[] labels, int value)
    {
        GuiButtonNop button = new GuiButtonNop(gui, id, x, y, width, 20, labels, value);
        button.setEnabled(ready);
        gui.addButton(button);
        return button;
    }

    private static Component skinLabel(CrewSource source, List<SkinOverrideVariant> skins)
    {
        if (source.skin() == CrewSource.RANDOM_SKIN)
            return Component.translatable(PREFIX + "select.random_skin");
        int variant = source.skin() < skins.size() ? source.skin() : 0;
        return Component.literal(skins.get(variant).name());
    }

    private GuiButtonNop toggle(int id, int x, int y, int width, String key, boolean value)
    {
        GuiButtonNop button = button(id, x, y, width, NO_YES, value ? 1 : 0);
        button.setMessage(Component.translatable(PREFIX + key, Component.translatable(NO_YES[value ? 1 : 0])));
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + HELP)));
        return button;
    }

    private void action(int id, int x, int y, int width, String key, boolean enabled)
    {
        GuiButtonNop button = button(id, x, y, width, new String[]{PREFIX + key}, 0);
        button.setEnabled(ready && enabled && !waiting);
        button.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + HELP)));
    }

    boolean handles(int id)
    {
        return id >= SOURCE_ID && id <= PASSENGERS_DISEMBARK_ID;
    }

    boolean button(GuiButtonNop button)
    {
        result = null;
        NpcCrewSettings settings = settings();
        boolean yes = button.getValue() == 1;
        switch (button.id)
        {
            case SOURCE_ID -> settings.setSource(selectedSeat, source().withKind(CrewSource.Kind.values()[button.getValue()]));
            case SELECT_ID -> {
                pickingSeat = selectedSeat;
                gui.setSubGui(new CrewSelectionMenu(npc.getId(), source()));
                return false;
            }
            case COPY_TO_ID -> {
                gui.setSubGui(new CrewCopyMenu(copyTargets()));
                return false;
            }
            case COPY_ALL_ID -> copyToSeats(seats);
            case VISIBLE_ID -> settings.setSeatVisible(selectedSeat, yes);
            case DISEMBARK_ID -> settings.setSeatDisembarks(selectedSeat, button.getValue() == 0 ? null : button.getValue() == 1);
            case PASSENGERS_DISEMBARK_ID -> settings.setPassengersDisembark(yes);
            case SHIELDED_ID -> settings.setProtectedCrew(yes);
            case PASSENGERS_FIRE_ID -> settings.setPassengersFire(yes);
            case DESTROYED_ID -> settings.setDestroyed(NpcCrewSettings.Destroyed.values()[button.getValue()]);
            case AUTO_ID -> settings.setAutoCrew(yes);
            case CREW_NOW_ID, REMOVE_ID -> {
                waiting = true;
                result = Component.translatable(PREFIX + "working");
                gui.save();
                NpcNetwork.sendToServer(new CrewActionPacket(npc.getId(), button.id == CREW_NOW_ID ? CrewActionPacket.Action.CREW : CrewActionPacket.Action.REMOVE, settings.write()));
                return false;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    boolean selected(CrewSelectionMenu menu)
    {
        CrewSource selection = menu.selection().orElse(null);
        if (selection == null)
            return false;
        settings().setSource(pickingSeat, selection);
        result = null;
        return true;
    }

    boolean selected(CrewCopyMenu menu)
    {
        CrewCopyMenu.Target target = menu.selection();
        if (target == null || !copyTargets().contains(target))
            return false;
        switch (target)
        {
            case NEXT -> {
                int next = seats.get(seatIndex() + 1).seat();
                copyTo(next);
                selectedSeat = next;
            }
            case DRIVER -> copyToSeats(roleTargets(CrewPost.DRIVER));
            case GUNNERS -> copyToSeats(roleTargets(CrewPost.GUNNER));
            case PASSENGERS -> copyToSeats(roleTargets(CrewPost.PASSENGER));
        }
        result = null;
        return true;
    }

    void receive(CrewResultPacket packet)
    {
        if (packet.entityId() != npc.getId())
            return;
        waiting = false;
        result = packet.message();
        gui.init();
    }

    @Override
    public void scrollClicked(double mouseX, double mouseY, int button, GuiCustomScrollNop scroll)
    {
        int index = seatList.getSelectedIndex();
        if (ready && index >= 0 && index < seats.size())
        {
            selectedSeat = seats.get(index).seat();
            result = null;
            gui.init();
        }
    }

    @Override
    public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll)
    {
        // A single click chooses the seat; selecting its source remains explicit.
    }

    private NpcCrewSettings settings()
    {
        return NpcCrewSettings.of(npc);
    }
}
