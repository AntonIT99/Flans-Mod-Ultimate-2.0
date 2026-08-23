package com.flansmodultimate.common.command;

import com.flansmodultimate.common.digitalammo.DigitalAmmoHelper;
import com.flansmodultimate.common.digitalammo.PlayerBulletStorage;
import com.flansmodultimate.config.CommonConfigSnapshot;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lombok.NoArgsConstructor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Collection;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DigitalAmmoCommand
{
    private static boolean isDigitalAmmoEnabled()
    {
        try
        {
            CommonConfigSnapshot config = ModCommonConfig.get();
            return config != null && config.enableDigitalAmmoSystem();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static int getNumTypes()
    {
        try
        {
            CommonConfigSnapshot config = ModCommonConfig.get();
            return config != null ? config.digitalAmmoNumTypes() : 7;
        }
        catch (Exception e)
        {
            return 7;
        }
    }

    private static int getSupplyAmount()
    {
        try
        {
            CommonConfigSnapshot config = ModCommonConfig.get();
            return config != null ? config.digitalAmmoSupplyAmount() : 100;
        }
        catch (Exception e)
        {
            return 100;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("digitalammo")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("type", IntegerArgumentType.integer(1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(DigitalAmmoCommand::setAmmo)
                        )
                    )
                )
            )
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("type", IntegerArgumentType.integer(1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(DigitalAmmoCommand::addAmmo)
                        )
                    )
                )
            )
            .then(Commands.literal("fill")
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("type", StringArgumentType.string())
                        .executes(DigitalAmmoCommand::fillAmmo)
                    )
                )
            )
            .then(Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(DigitalAmmoCommand::getAmmo)
                )
            )
        );
    }

    private static int setAmmo(CommandContext<CommandSourceStack> context)
    {
        if (!isDigitalAmmoEnabled())
        {
            context.getSource().sendFailure(Component.literal("Digital ammo system is disabled"));
            return 0;
        }

        Collection<ServerPlayer> players;
        try
        {
            players = EntityArgument.getPlayers(context, "player");
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }

        int type = IntegerArgumentType.getInteger(context, "type");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int numTypes = getNumTypes();

        if (type > numTypes)
        {
            context.getSource().sendFailure(Component.literal("Type must be between 1 and " + numTypes));
            return 0;
        }

        for (ServerPlayer player : players)
        {
            DigitalAmmoHelper.setPlayerAmmo(player, type, amount);
        }

        context.getSource().sendSuccess(() -> Component.literal("Set ammo type " + type + " to " + amount + " for " + players.size() + " player(s)"), true);
        return 1;
    }

    private static int addAmmo(CommandContext<CommandSourceStack> context)
    {
        if (!isDigitalAmmoEnabled())
        {
            context.getSource().sendFailure(Component.literal("Digital ammo system is disabled"));
            return 0;
        }

        Collection<ServerPlayer> players;
        try
        {
            players = EntityArgument.getPlayers(context, "player");
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }

        int type = IntegerArgumentType.getInteger(context, "type");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int numTypes = getNumTypes();

        if (type > numTypes)
        {
            context.getSource().sendFailure(Component.literal("Type must be between 1 and " + numTypes));
            return 0;
        }

        for (ServerPlayer player : players)
        {
            DigitalAmmoHelper.addAmmoToPlayer(player, type, amount);
        }

        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " to ammo type " + type + " for " + players.size() + " player(s)"), true);
        return 1;
    }

    private static int fillAmmo(CommandContext<CommandSourceStack> context)
    {
        if (!isDigitalAmmoEnabled())
        {
            context.getSource().sendFailure(Component.literal("Digital ammo system is disabled"));
            return 0;
        }

        Collection<ServerPlayer> players;
        try
        {
            players = EntityArgument.getPlayers(context, "player");
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }

        String typeStr = StringArgumentType.getString(context, "type").toLowerCase();
        int numTypes = getNumTypes();

        if (typeStr.equals("all"))
        {
            for (ServerPlayer player : players)
            {
                PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
                int maxAmount = bulletData.getMaxAmount();
                for (int i = 1; i <= numTypes; i++)
                {
                    DigitalAmmoHelper.setPlayerAmmo(player, i, maxAmount);
                }
            }
            context.getSource().sendSuccess(() -> Component.literal("Filled all ammo types for " + players.size() + " player(s)"), true);
        }
        else
        {
            try
            {
                int type = Integer.parseInt(typeStr);
                if (type < 1 || type > numTypes)
                {
                    context.getSource().sendFailure(Component.literal("Type must be between 1 and " + numTypes + " or 'all'"));
                    return 0;
                }

                for (ServerPlayer player : players)
                {
                    PlayerBulletStorage.PlayerBulletData bulletData = PlayerBulletStorage.getBulletDataByPlayer(player.getUUID());
                    int maxAmount = bulletData.getMaxAmount();
                    DigitalAmmoHelper.setPlayerAmmo(player, type, maxAmount);
                }
                context.getSource().sendSuccess(() -> Component.literal("Filled ammo type " + type + " for " + players.size() + " player(s)"), true);
            }
            catch (NumberFormatException e)
            {
                context.getSource().sendFailure(Component.literal("Type must be a number (1-" + numTypes + ") or 'all'"));
                return 0;
            }
        }

        return 1;
    }

    private static int getAmmo(CommandContext<CommandSourceStack> context)
    {
        if (!isDigitalAmmoEnabled())
        {
            context.getSource().sendFailure(Component.literal("Digital ammo system is disabled"));
            return 0;
        }

        Collection<ServerPlayer> players;
        try
        {
            players = EntityArgument.getPlayers(context, "player");
        }
        catch (Exception e)
        {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }

        int numTypes = getNumTypes();

        for (ServerPlayer player : players)
        {
            StringBuilder sb = new StringBuilder(player.getName().getString() + "'s ammo: ");
            for (int i = 1; i <= numTypes; i++)
            {
                double amount = DigitalAmmoHelper.getPlayerAmmo(player, i);
                sb.append("Type ").append(i).append(": ").append((int)amount);
                if (i < numTypes) sb.append(", ");
            }
            context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        }

        return 1;
    }
}
