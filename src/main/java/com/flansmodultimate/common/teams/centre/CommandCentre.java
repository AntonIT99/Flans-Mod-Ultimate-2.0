package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.api.commandcentre.FlansCommandCentre;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketCommandCentreState;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.util.*;
import java.util.function.Function;

/**
 * The server side of the Teams command centre. It sends operators the state the screen shows, built from the
 * registered sections, and runs the {@code /teams} commands they give in the screen as though they had typed them,
 * catching the commands' output to show in the screen instead of the chat. Server thread, but for registering sections.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandCentre
{
    /** Most lines of a command's output sent back to the screen. */
    public static final int MOST_FEEDBACK = 16;
    private static final Map<ResourceLocation, Function<ServerPlayer, CompoundTag>> SECTIONS = Collections.synchronizedMap(new LinkedHashMap<>());

    static
    {
        SECTIONS.put(TeamsSection.KEY, TeamsSection::build);
    }

    /** A line of a command's output. */
    public record Feedback(Component text, boolean failure)
    {}

    public static void addSection(ResourceLocation key, Function<ServerPlayer, CompoundTag> section)
    {
        SECTIONS.put(Objects.requireNonNull(key), Objects.requireNonNull(section));
    }

    public static boolean mayUse(ServerPlayer player)
    {
        return player.hasPermissions(FlansCommandCentre.PERMISSION_LEVEL);
    }

    /** Opens the screen for an operator, or tells anyone else that it is not for them. */
    public static void open(ServerPlayer player)
    {
        handle(player, "", true);
    }

    /**
     * Runs a command an operator gave in the screen, if any, and sends the screen's state back with its output.
     *
     * @param open
     *            whether the screen is to open, rather than only take the new state while it is open
     */
    public static void handle(ServerPlayer player, String command, boolean open)
    {
        if (!mayUse(player))
        {
            player.sendSystemMessage(Component.translatable("message.flansmodultimate.centre.not_operator").withStyle(ChatFormatting.RED));
            return;
        }
        List<Feedback> feedback = command.isBlank() ? List.of() : run(player, command);
        PacketHandler.sendTo(new PacketCommandCentreState(open, sections(player), feedback), player);
    }

    /** Runs a {@code /teams} command as the operator, and gives back what it told them. */
    static List<Feedback> run(ServerPlayer player, String command)
    {
        String line = CommandLines.normalise(command);
        if (!CommandLines.isCentreCommand(line))
            return List.of(new Feedback(Component.translatable("message.flansmodultimate.centre.not_teams"), true));
        Capture capture = new Capture();
        CommandSourceStack source = player.createCommandSourceStack().withSource(capture);
        if (player.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS))
            FlansLog.log.info("{} ran /{} from the Teams command centre", player.getScoreboardName(), line);
        player.server.getCommands().performPrefixedCommand(source, line);
        if (capture.lines.isEmpty())
            capture.lines.add(new Feedback(Component.translatable("message.flansmodultimate.centre.done"), false));
        return List.copyOf(capture.lines);
    }

    private static CompoundTag sections(ServerPlayer player)
    {
        Map<ResourceLocation, Function<ServerPlayer, CompoundTag>> registered;
        synchronized (SECTIONS)
        {
            registered = new LinkedHashMap<>(SECTIONS);
        }
        CompoundTag sections = new CompoundTag();
        registered.forEach((key, section) ->
        {
            try
            {
                sections.put(key.toString(), section.apply(player));
            }
            catch (RuntimeException e)
            {
                FlansLog.log.error("Teams command centre section {} failed", key, e);
            }
        });
        return sections;
    }

    /**
     * The output of a command, kept instead of shown in the chat. Failures come styled red, as {@link CommandSourceStack}
     * sends them. It informs no other operators: the screen is the operator's own.
     */
    private static final class Capture implements CommandSource
    {
        private static final TextColor FAILURE = TextColor.fromLegacyFormat(ChatFormatting.RED);
        private final List<Feedback> lines = new ArrayList<>();

        @Override
        public void sendSystemMessage(@NotNull Component component)
        {
            if (lines.size() < MOST_FEEDBACK)
                lines.add(new Feedback(component, Objects.equals(FAILURE, component.getStyle().getColor())));
        }

        @Override
        public boolean acceptsSuccess()
        {
            return true;
        }

        @Override
        public boolean acceptsFailure()
        {
            return true;
        }

        @Override
        public boolean shouldInformAdmins()
        {
            return false;
        }
    }
}
