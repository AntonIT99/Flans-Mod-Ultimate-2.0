package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.ICommandCentrePage;
import com.flansmodultimate.common.teams.centre.CommandCentre;
import com.flansmodultimate.common.teams.centre.TeamsSection;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketCommandCentreAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client side of the Teams command centre: its pages, the state the server sent last and the output of commands
 * not shown yet. Pages are added during client setup; everything else happens on the client thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandCentreClient
{
    private static final Map<String, ICommandCentrePage> PAGES = new ConcurrentHashMap<>();
    private static final List<CommandCentre.Feedback> PENDING = new ArrayList<>();
    private static CompoundTag sections = new CompoundTag();
    @Nullable
    private static TeamsData teams;
    private static long receivedAt = Util.getMillis();
    /** The page shown last, shown again when the centre opens. */
    private static String page = OverviewPage.ID;

    static
    {
        for (ICommandCentrePage builtIn : List.of(new OverviewPage(), new RotationPage(), new MapsPage(), new RulesPage(), new ServerPage()))
            PAGES.put(builtIn.getId(), builtIn);
    }

    public static void addPage(ICommandCentrePage added)
    {
        PAGES.put(Objects.requireNonNull(added.getId()), added);
    }

    /** Every page, in side bar order. */
    static List<ICommandCentrePage> pages()
    {
        return PAGES.values().stream().sorted(Comparator.comparingInt(ICommandCentrePage::getOrder).thenComparing(ICommandCentrePage::getId)).toList();
    }

    /** The rotation page, which other pages send the operator to with a round to set up; empty once an addon replaced it. */
    static Optional<RotationPage> rotationPage()
    {
        return PAGES.get(RotationPage.ID) instanceof RotationPage rotation ? Optional.of(rotation) : Optional.empty();
    }

    static String shownPage()
    {
        return page;
    }

    static void showPage(String id)
    {
        page = id;
    }

    public static void requestOpen(String pageId)
    {
        if (!pageId.isEmpty())
            page = pageId;
        PacketHandler.sendToServer(PacketCommandCentreAction.open());
    }

    /** Takes new state from the server, and opens the centre when it was asked to. */
    public static void accept(boolean open, CompoundTag update, List<CommandCentre.Feedback> feedback)
    {
        sections = update;
        teams = null;
        receivedAt = Util.getMillis();
        PENDING.addAll(feedback);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CommandCentreScreen screen)
            screen.stateChanged();
        else if (open)
            minecraft.setScreen(new CommandCentreScreen());
    }

    /** The output of commands that arrived since the centre last took it. */
    static List<CommandCentre.Feedback> takeFeedback()
    {
        List<CommandCentre.Feedback> taken = List.copyOf(PENDING);
        PENDING.clear();
        return taken;
    }

    static CompoundTag sections()
    {
        return sections;
    }

    static int ticksSinceUpdate()
    {
        return (int) ((Util.getMillis() - receivedAt) / 50L);
    }

    /** The Teams section, read once for each state. */
    static TeamsData teams()
    {
        if (teams == null)
            teams = TeamsData.read(sections.getCompound(TeamsSection.KEY.toString()));
        return teams;
    }
}
