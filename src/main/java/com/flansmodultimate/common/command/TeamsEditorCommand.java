package com.flansmodultimate.common.command;

import com.flansmodultimate.common.teams.editor.*;
import com.flansmodultimate.common.types.PlayerClass;
import com.flansmodultimate.common.types.Team;
import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Console-capable editing; all mutations share the GUI's server service. */
public final class TeamsEditorCommand
{
    private static final String ID = "id";
    private static final String VALUE = "value";
    private static final String SLOT = "slot";
    private static final String ARMOUR = "armour";

    private TeamsEditorCommand()
    {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(Commands.literal("editor").requires(EditorPermissions::mayEdit).executes(context -> open(context, "list", ""))
            .then(Commands.literal("team").then(id(true).executes(context -> open(context, "team", key(context, true)))))
            .then(Commands.literal("class").then(id(false).executes(context -> open(context, "class", key(context, false)))))
            .then(Commands.literal("reload").executes(context -> run(context, () -> TeamEditorService.get().reload())))
            .then(Commands.literal("export").then(
                Commands.argument("preset", StringArgumentType.word()).executes(context -> run(context, () -> TeamEditorService.get().exportPreset(StringArgumentType.getString(context, "preset"))))))
            .then(Commands.literal("import").then(Commands.argument("preset", StringArgumentType.word())
                .executes(context -> run(context, () -> TeamEditorService.get().importPreset(StringArgumentType.getString(context, "preset")))))));
        root.then(Commands.literal("edit").requires(EditorPermissions::mayEdit).executes(TeamsEditorCommand::help).then(branch(true)).then(branch(false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> branch(boolean team)
    {
        var branch = Commands.literal(team ? "team" : "class");
        branch.then(Commands.literal("create").then(Commands.argument(ID, StringArgumentType.string()).then(Commands.argument("name", StringArgumentType.greedyString()).executes(context ->
        {
            JsonObject json = new JsonObject();
            json.addProperty("name", StringArgumentType.getString(context, "name"));
            return edit(context, team, "create", json);
        }))));
        var target = id(team).executes(context -> inspect(context, team));
        for (String action : List.of("delete", "disable", "enable", "restore"))
            target.then(Commands.literal(action).executes(context -> edit(context, team, action, new JsonObject())));
        target.then(Commands.literal("duplicate").then(Commands.argument("newId", StringArgumentType.string()).executes(context -> run(context, () ->
        {
            JsonObject description = describe(context, team);
            String newKey = "custom:" + StringArgumentType.getString(context, "newId").replaceFirst("^custom:", "").toLowerCase(Locale.ROOT);
            TeamEditorService.get().edit(team, newKey, "create", description, TeamEditorService.get().revision());
        }))));
        target.then(Commands.literal("set")
            .then(Commands.argument("field", StringArgumentType.word())
                .suggests((context,
                    builder) -> SharedSuggestionProvider.suggest(team
                        ? List.of("name", "colour", "textColour", "classes", "roundsGenerator", "victorySoundEnabled", "victorySound", "defeatSound", "victoryFlag")
                        : List.of("name", "skins", "unlockLevel", "requiredPrestigeLevel", "classLimit", "classLimitPercent", "items"), builder))
                .then(Commands.argument(VALUE, StringArgumentType.greedyString()).executes(context -> run(context, () ->
                {
                    JsonObject patch = new JsonObject();
                    patch.add(StringArgumentType.getString(context, "field"), value(StringArgumentType.getString(context, VALUE)));
                    save(context, team, patch);
                })))));
        target.then(Commands.literal("patch").then(Commands.argument("json", StringArgumentType.greedyString())
            .executes(context -> run(context, () -> save(context, team, JsonParser.parseString(StringArgumentType.getString(context, "json")).getAsJsonObject())))));
        var armourCommand = Commands.literal(ARMOUR);
        armourCommand.then(Commands.argument(SLOT, StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("head", "chest", "legs", "feet"), builder))
            .then(Commands.argument(VALUE, StringArgumentType.greedyString()).executes(context -> run(context, () ->
            {
                JsonObject armour = new JsonObject();
                armour.add(StringArgumentType.getString(context, SLOT), value(StringArgumentType.getString(context, VALUE)));
                JsonObject patch = new JsonObject();
                patch.add(ARMOUR, armour);
                save(context, team, patch);
            }))));
        armourCommand.then(Commands.literal("copy").executes(context -> run(context, () ->
        {
            JsonObject armour = new JsonObject();
            for (EquipmentSlot slot : EditorDescriptions.ARMOUR)
            {
                ItemStack stack = context.getSource().getPlayerOrException().getItemBySlot(slot);
                armour.add(slot.getName(), stack.isEmpty() ? new JsonPrimitive("empty") : EditorStacks.save(stack));
            }
            JsonObject patch = new JsonObject();
            patch.add(ARMOUR, armour);
            save(context, team, patch);
        })));
        target.then(armourCommand);
        if (team)
            addMembership(target);
        else
        {
            addItems(target);
            addSkins(target);
            target.then(Commands.literal("capture").executes(context -> run(context, () ->
            {
                var player = context.getSource().getPlayerOrException();
                JsonObject items = new JsonObject();
                for (int slot = 0; slot < 41; slot++)
                {
                    if (slot >= 36 && slot != 40)
                        continue;
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (!stack.isEmpty())
                        items.add(Integer.toString(slot), EditorStacks.save(stack));
                }
                JsonObject patch = new JsonObject();
                patch.add("items", items);
                save(context, false, patch);
            })));
        }
        return branch.then(target);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> id(boolean team)
    {
        return Commands.argument(ID, StringArgumentType.string()).suggests((context, builder) -> SharedSuggestionProvider
            .suggest((team ? Team.values().stream().map(Team::getShortName) : PlayerClass.values().stream().map(PlayerClass::getShortName)).map(StringArgumentType::escapeIfRequired), builder));
    }

    private static void addItems(com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> target)
    {
        target.then(Commands.literal("item")
            .then(Commands.literal("set")
                .then(Commands.argument(SLOT, IntegerArgumentType.integer(0, 40))
                    .then(Commands.argument(VALUE, StringArgumentType.greedyString()).executes(context -> run(context, () -> setItem(context, value(StringArgumentType.getString(context, VALUE))))))))
            .then(Commands.literal("clear").then(Commands.argument(SLOT, IntegerArgumentType.integer(0, 40)).executes(context -> run(context, () -> setItem(context, JsonNull.INSTANCE)))))
            .then(Commands.literal("copy").then(Commands.argument(SLOT, IntegerArgumentType.integer(0, 40))
                .then(Commands.argument("source", EntityArgument.entity()).then(Commands.argument("sourceSlot", SlotArgument.slot()).executes(context -> run(context, () ->
                {
                    Entity source = EntityArgument.getEntity(context, "source");
                    ItemStack stack = source.getSlot(SlotArgument.getSlot(context, "sourceSlot")).get();
                    setItem(context, stack.isEmpty() ? JsonNull.INSTANCE : EditorStacks.save(stack));
                })))))));
    }

    private static void setItem(CommandContext<CommandSourceStack> context, JsonElement item) throws IOException
    {
        JsonObject items = describe(context, false).getAsJsonObject("items").deepCopy();
        String slot = Integer.toString(IntegerArgumentType.getInteger(context, SLOT));
        if (Integer.parseInt(slot) > 35 && !"40".equals(slot))
            throw new IllegalArgumentException("Inventory slots are 0-35 or 40 for offhand");
        if (item.isJsonNull())
            items.remove(slot);
        else
        {
            if (item.isJsonPrimitive())
            {
                JsonObject stack = new JsonObject();
                stack.addProperty("item", item.getAsString());
                stack.addProperty("count", 1);
                item = stack;
            }
            items.add(slot, item);
        }
        JsonObject patch = new JsonObject();
        patch.add("items", items);
        save(context, false, patch);
    }

    private static void addSkins(com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> target)
    {
        for (String operation : List.of("add", "remove"))
            target.then(Commands.literal("skin").then(Commands.literal(operation).then(Commands.argument("texture", StringArgumentType.greedyString()).executes(context -> run(context, () ->
            {
                JsonArray skins = describe(context, false).getAsJsonArray("skins").deepCopy();
                JsonPrimitive texture = new JsonPrimitive(StringArgumentType.getString(context, "texture"));
                if ("remove".equals(operation))
                    skins.remove(texture);
                else if (!skins.contains(texture))
                    skins.add(texture);
                JsonObject patch = new JsonObject();
                patch.add("skins", skins);
                save(context, false, patch);
            })))));
    }

    private static void addMembership(com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> target)
    {
        for (String operation : List.of("add", "remove"))
            target.then(Commands.literal("class").then(Commands.literal(operation).then(Commands.argument("classId", StringArgumentType.string()).executes(context -> run(context, () ->
            {
                JsonArray classes = describe(context, true).getAsJsonArray("classes").deepCopy();
                JsonPrimitive id = new JsonPrimitive(TeamEditorService.resolveKey(false, StringArgumentType.getString(context, "classId")));
                if ("remove".equals(operation))
                    classes.remove(id);
                else if (!classes.contains(id))
                    classes.add(id);
                JsonObject patch = new JsonObject();
                patch.add("classes", classes);
                save(context, true, patch);
            })))));
    }

    private static JsonElement value(String text)
    {
        try
        {
            return JsonParser.parseString(text);
        }
        catch (JsonParseException ignored)
        {
            return new JsonPrimitive(text);
        }
    }

    private static String key(CommandContext<CommandSourceStack> context, boolean team)
    {
        return TeamEditorService.resolveKey(team, StringArgumentType.getString(context, ID));
    }

    private static JsonObject describe(CommandContext<CommandSourceStack> context, boolean team)
    {
        String key = key(context, team);
        if (team)
        {
            Team found = Team.values().stream().filter(type -> EditorCatalog.key(type).equals(key)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown team"));
            return EditorDescriptions.team(found);
        }
        PlayerClass found = PlayerClass.values().stream().filter(type -> EditorCatalog.key(type).equals(key)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown class"));
        return EditorDescriptions.playerClass(found);
    }

    private static void save(CommandContext<CommandSourceStack> context, boolean team, JsonObject patch) throws IOException
    {
        TeamEditorService service = TeamEditorService.get();
        service.edit(team, key(context, team), "save", patch, service.revision());
    }

    private static int edit(CommandContext<CommandSourceStack> context, boolean team, String action, JsonObject json)
    {
        return run(context, () -> TeamEditorService.get().edit(team, key(context, team), action, json, TeamEditorService.get().revision()));
    }

    private static int inspect(CommandContext<CommandSourceStack> context, boolean team)
    {
        return run(context, () -> context.getSource().sendSuccess(() -> Component.literal(describe(context, team).toString()), false));
    }

    @SuppressWarnings("SameReturnValue") // Brigadier's successful command result is one.
    private static int open(CommandContext<CommandSourceStack> context, String kind, String key) throws CommandSyntaxException
    {
        TeamEditorService.get().sync(context.getSource().getPlayerOrException(), kind, key, "");
        return 1;
    }

    @SuppressWarnings("SameReturnValue") // Brigadier's successful command result is one.
    private static int help(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(() -> Component.literal(
            "/teams editor [reload|import <preset>|export <preset>]\n/teams edit <team|class> create <id> <name>\n/teams edit <team|class> <id> [set <field> <value>|patch <json>|duplicate <id>|disable|enable|delete|restore]\nClass: item set/clear/copy <slot>, armour <slot> <item JSON|inherit|empty>, skin add/remove <texture>, capture\nTeam: class add/remove <class>, armour <slot> <item JSON|empty>"),
            false);
        return 1;
    }

    @FunctionalInterface
    private interface Action
    {
        void run() throws IOException, CommandSyntaxException;
    }

    private static int run(CommandContext<CommandSourceStack> context, Action action)
    {
        try
        {
            action.run();
            context.getSource().sendSuccess(() -> Component.literal("Team editor: done"), true);
            return 1;
        }
        catch (IOException | CommandSyntaxException | RuntimeException exception)
        {
            context.getSource().sendFailure(Component.literal("Team editor: " + exception.getMessage()));
            return 0;
        }
    }
}
