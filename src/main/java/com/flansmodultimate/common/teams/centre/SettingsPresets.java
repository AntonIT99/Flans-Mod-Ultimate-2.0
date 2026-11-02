package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.api.commandcentre.ISettingsPresets;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.teams.PacketPresetResult;
import com.flansmodultimate.platform.PlatformPaths;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Operator-only preset libraries and bounded file transfers. Registration at setup; requests on the server thread. */
public final class SettingsPresets
{
    private static final String APPLY = "apply";
    private static final String PREVIEW = "preview";
    private static final String IMPORT = "import";
    private static final String EXPORT = "export";
    private static final String LIBRARY = "library";
    private static final String TARGET = "target";
    public static final ResourceLocation TEAMS = ResourceLocation.fromNamespaceAndPath("flansmodultimate", "teams");
    private static final Map<ResourceLocation, ISettingsPresets> LIBRARIES = new java.util.concurrent.ConcurrentHashMap<>();

    static
    {
        register(TEAMS, new TeamsRulePresets());
    }

    private SettingsPresets()
    {}

    public static void register(ResourceLocation key, ISettingsPresets library)
    {
        if (!key.getPath().matches("[a-z0-9_-]+"))
            throw new IllegalArgumentException("Preset library must use one path segment");
        LIBRARIES.put(key, Objects.requireNonNull(library));
    }

    public static Path folder(ResourceLocation kind)
    {
        if (!kind.getPath().matches("[a-z0-9_-]+"))
            throw new IllegalArgumentException("Invalid preset library");
        return PlatformPaths.configDir().resolve(kind.getNamespace()).resolve("presets").resolve(kind.getPath());
    }

    public record Request(ResourceLocation kind, String action, String name, String target, String json, boolean replace)
    {}

    public static void handle(ServerPlayer player, int request, Request input)
    {
        ResourceLocation kind = input.kind();
        String action = input.action();
        String name = input.name();
        String target = input.target();
        String json = input.json();
        boolean replace = input.replace();
        if (!CommandCentre.mayUse(player))
            return;
        try
        {
            ISettingsPresets library = library(kind);
            if (action.equals(APPLY) && !json.isEmpty() && !PresetFiles.decode(kind, json).data().equals(PresetFiles.decode(kind, PresetFiles.encode(kind, name, library.read(name))).data()))
                throw new IllegalArgumentException("This preset changed after the preview. Preview it again before applying.");
            String output = execute(kind, library, action, name, target, json, replace);
            List<Component> preview = action.equals(PREVIEW) ? library.preview(library.read(name), target) : List.of();
            if (preview.size() > 512 || library.names().size() > 2048)
                throw new IllegalArgumentException("Too many preset entries or preview lines; select fewer rounds");
            PacketHandler.sendTo(new PacketPresetResult(request, false, output, List.copyOf(library.names()), library.names().stream().filter(library::builtIn).toList(), preview,
                Component.translatable("gui.flansmodultimate.presets.done")), player);
            CommandCentre.handle(player, "", false);
        }
        catch (IOException | RuntimeException e)
        {
            PacketHandler.sendTo(new PacketPresetResult(request, true, "", List.of(), List.of(), List.of(), Component.literal(Objects.toString(e.getMessage(), "Invalid preset"))), player);
        }
    }

    static String execute(ResourceLocation kind, ISettingsPresets library, String action, String name, String target, String json, boolean replace) throws IOException
    {
        if (action.equals("list"))
            return "";
        PresetFiles.checkName(name);
        switch (action)
        {
            case "save" -> {
                CompoundTag captured = library.capture(target);
                library.validate(captured);
                library.save(name, captured, replace);
            }
            case IMPORT -> {
                PresetFiles.Document document = PresetFiles.decode(kind, json);
                library.validate(document.data());
                library.save(name, document.data(), replace);
            }
            case "remove" -> library.remove(name);
            case EXPORT, PREVIEW -> {
                return PresetFiles.encode(kind, name, library.read(name));
            }
            case APPLY -> {
                CompoundTag preset = library.read(name);
                library.validate(preset);
                library.apply(preset, target);
            }
            default -> throw new IllegalArgumentException("Unknown preset action");
        }
        return "";
    }

    private static ISettingsPresets library(ResourceLocation kind)
    {
        ISettingsPresets library = LIBRARIES.get(kind);
        if (library == null)
            throw new IllegalArgumentException("This preset library is not installed on the server");
        return library;
    }

    /** Console equivalents use files in this server's preset folder; imports still only add library entries. */
    public static LiteralArgumentBuilder<CommandSourceStack> commands()
    {
        var root = Commands.literal("preset").requires(source -> source.hasPermission(2));
        var kind = Commands.argument(LIBRARY, ResourceLocationArgument.id())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(LIBRARIES.keySet().stream().map(ResourceLocation::toString), builder));
        for (String operation : List.of("list", "save", APPLY, "remove", IMPORT, EXPORT, PREVIEW))
        {
            var action = Commands.literal(operation);
            if (operation.equals("list"))
                action.executes(context -> command(context.getSource(), ResourceLocationArgument.getId(context, LIBRARY).toString(), operation, "", "", false));
            else
            {
                var name = Commands.argument("name", StringArgumentType.word()).suggests((context, builder) ->
                {
                    ISettingsPresets library = LIBRARIES.get(ResourceLocation.tryParse(ResourceLocationArgument.getId(context, LIBRARY).toString()));
                    return SharedSuggestionProvider.suggest(library == null ? List.of() : library.names(), builder);
                });
                name.executes(
                    context -> command(context.getSource(), ResourceLocationArgument.getId(context, LIBRARY).toString(), operation, StringArgumentType.getString(context, "name"), "", false));
                var target = Commands.argument(TARGET, StringArgumentType.word()).executes(context -> command(context.getSource(), ResourceLocationArgument.getId(context, LIBRARY).toString(),
                    operation, StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, TARGET), false));
                target.then(Commands.literal("replace").executes(context -> command(context.getSource(), ResourceLocationArgument.getId(context, LIBRARY).toString(), operation,
                    StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, TARGET), true)));
                name.then(target);
                action.then(name);
            }
            kind.then(action);
        }
        return root.then(kind);
    }

    // Brigadier command handlers return the successful command count.
    @SuppressWarnings("SameReturnValue")
    private static int command(CommandSourceStack source, String key, String action, String name, String target, boolean replace) throws com.mojang.brigadier.exceptions.CommandSyntaxException
    {
        try
        {
            ResourceLocation kind = ResourceLocation.tryParse(key);
            if (kind == null)
                throw new IllegalArgumentException("Invalid preset library");
            ISettingsPresets library = library(kind);
            if (action.equals("list"))
            {
                source.sendSuccess(() -> Component.literal(String.join(", ", library.names())), false);
                return 1;
            }
            PresetFiles.checkName(name);
            Path file = folder(kind).resolve(name + ".json");
            String json = action.equals(IMPORT) ? PresetFiles.read(file) : "";
            String output = execute(kind, library, action, name, target, json, replace);
            if (action.equals(EXPORT))
                PresetFiles.write(file, output, replace);
            if (action.equals(PREVIEW))
                library.preview(library.read(name), target).forEach(line -> source.sendSuccess(() -> line, false));
            source.sendSuccess(() -> Component.translatable("gui.flansmodultimate.presets.done"), true);
            return 1;
        }
        catch (IOException | RuntimeException e)
        {
            throw new SimpleCommandExceptionType(Component.literal(Objects.toString(e.getMessage(), "Invalid preset"))).create();
        }
    }
}
