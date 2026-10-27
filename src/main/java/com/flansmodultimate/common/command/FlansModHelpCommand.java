package com.flansmodultimate.common.command;

import com.mojang.brigadier.CommandDispatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** A compact command topic menu, with clickable commands and usage hints on each topic's page. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModHelpCommand
{
    private static final String PREFIX = "/" + FlansModCommandAliases.ROOT + " ";
    private static final List<Topic> TOPICS = List.of(
        new Topic("equipment", "Equipment & ammunition", "Attachments, ammunition and crafting ingredients", false,
            List.of(new Entry("attachments", "Show attachments for your held gun", "", "", false), new Entry("defaultammo", "Give compatible ammunition", "[amount]", "/defaultammo", true),
                new Entry("rearm", "Load your vehicle or mounted gun", "[ammo]", "/rearm", true), new Entry("craftingparts", "Give ingredients for your held item", "", "/craftingparts", true),
                new Entry("digitalammo", "Manage players' digital ammunition", "get <players> | set/add <players> <type> <amount> | fill <players> <type>", "/digitalammo", true))),
        new Topic("teams", "Teams & loadouts", "Join teams, choose classes and view ranks", false,
            List.of(new Entry("teams help", "Browse team and round commands", "", "/teams help", false), new Entry("teams admin", "Open the operator command centre", "", "/teams admin", true),
                new Entry("ranks", "View rank and progression commands", "[help | prestige | leaderboard | stats]", "/ranks", false),
                new Entry("tryclass", "Try a class loadout", "<class> | list [filter] | clear", "/tryclass", true),
                new Entry("tryteam", "Try a team's equipment", "<team> | list [filter] | clear", "/tryteam", true))),
        new Topic("admin", "Server administration", "Find entities, change physics and spawn particles", true,
            List.of(new Entry("entities list", "Find Flan entities", "radius <radius> | dimension <dimension> | world", "", true),
                new Entry("entities remove", "Remove Flan entities", "[force] radius <radius> | [force] dimension <dimension> | [force] world", "", true),
                new Entry("repair", "Fully repair every driveable part", "[target]", "", true),
                new Entry("physics", "Inspect dimension physics", "[gravity | drag] [<dimension> [<factor> | reset]]", "/flansphysics", true),
                new Entry("particle", "Spawn a mod particle", "<name> [<pos> [<delta> <speed> <count> ...]]", "/fmparticle", true))),
        new Topic("debug", "Vehicle diagnostics", "Inspect physics, edit hitboxes and shoot points", true,
            List.of(new Entry("vehiclephysics", "Report vehicle physics", "", "/vehiclephysics", true), new Entry("debug hitbox", "Edit model hitboxes", "", "/flansdebug hitbox", true),
                new Entry("debug shootpoint list", "List weapon shoot points", "", "/flansdebug shootpoint list", true),
                new Entry("debug vehiclecollision", "Control vehicle collision", "[player <targets> | all] [true | false]", "/flansdebug vehiclecollision", true))),
        new Topic("client", "Client tools", "Render counters, model reloads and aim previews", false,
            List.of(new Entry("renderstats", "Inspect client rendering", "[start | stop | reset | bridge [indices]]", "/flansrenderstats", false),
                new Entry("reloadModels", "Reload client model caches", "", "", false),
                new Entry("debug aimoffset", "Preview held-gun aim offsets", "[x y z | reset | clear]", "/flansdebug aimoffset", false))));

    private record Topic(String id, String title, String description, boolean operator, List<Entry> entries)
    {}

    private record Entry(String command, String description, String arguments, String shortcut, boolean operator)
    {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        var help = Commands.literal("help").executes(context -> overview(context.getSource()));
        for (Topic topic : TOPICS)
            help.then(Commands.literal(topic.id()).requires(source -> !topic.operator() || source.hasPermission(2)).executes(context -> page(context.getSource(), topic)));
        dispatcher.register(Commands.literal(FlansModCommandAliases.ROOT).executes(context -> overview(context.getSource())).then(help));
    }

    @SuppressWarnings("SameReturnValue") // Brigadier callbacks return success after displaying help.
    private static int overview(CommandSourceStack source)
    {
        heading(source, "Command help");
        send(source, Component.literal("Choose a topic: " + PREFIX + "help <topic> (or click below)").withStyle(ChatFormatting.GRAY));
        for (Topic topic : TOPICS)
        {
            if (topic.operator() && !source.hasPermission(2))
                continue;
            send(source, link(topic.id(), PREFIX + "help " + topic.id(), topic.description() + "\nClick to open this help topic", ClickEvent.Action.RUN_COMMAND)
                .append(Component.literal(" - " + topic.title()).withStyle(ChatFormatting.GRAY)));
        }
        send(source, Component.literal("Tab completes commands after " + PREFIX.trim() + ".").withStyle(ChatFormatting.GRAY));
        send(source, Component.literal("Operator commands require permission level 2 and are shown to operators.").withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    @SuppressWarnings("SameReturnValue") // Brigadier callbacks return success after displaying help.
    private static int page(CommandSourceStack source, Topic topic)
    {
        heading(source, topic.title());
        send(source, Component.literal("Commands below use " + PREFIX.trim() + ".").withStyle(ChatFormatting.GRAY));
        send(source, Component.literal("Click a command to fill chat; hover for usage and shortcuts.").withStyle(ChatFormatting.GRAY));
        if (topic.id().equals("client"))
            send(source, Component.literal("These tools run on your client. HotSwap Java changes before reloading models.").withStyle(ChatFormatting.GRAY));
        for (Entry entry : topic.entries())
        {
            if (entry.operator() && !source.hasPermission(2))
                continue;
            String command = PREFIX + entry.command();
            String usage = "Usage: " + command + (entry.arguments().isEmpty() ? "" : " " + entry.arguments());
            if (!entry.shortcut().isEmpty())
                usage += "\nShortcut: " + entry.shortcut();
            if (entry.operator())
                usage += "\nRequires operator permission level 2";
            send(source, link(entry.command(), command + " ", usage, ClickEvent.Action.SUGGEST_COMMAND).append(Component.literal(" - " + entry.description()).withStyle(ChatFormatting.GRAY)));
        }
        send(source, link("[Back to topics]", PREFIX + "help", "Open command help", ClickEvent.Action.RUN_COMMAND));
        return 1;
    }

    private static void heading(CommandSourceStack source, String title)
    {
        send(source, Component.literal("Flan's Mod Ultimate > " + title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static MutableComponent link(String label, String command, String hint, ClickEvent.Action action)
    {
        return Component.literal(label).withStyle(
            style -> style.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(action, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hint))));
    }

    private static void send(CommandSourceStack source, Component message)
    {
        source.sendSuccess(() -> message, false);
    }
}
