package com.flansmodultimate.common.command;

import com.flansmodultimate.common.driveables.DriveableAmmoLoader;
import com.flansmodultimate.common.driveables.DriveableAmmoLoader.BankReport;
import com.flansmodultimate.common.driveables.DriveableAmmoLoader.LoadReport;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.item.DriveableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.ShootableType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Loads a driveable's bombs, missiles, shells and mounted-gun ammunition in one step.
 *
 * <p>The vehicle equivalent of {@code /defaultammo}: an operator sitting in an aircraft
 * should not have to open the inventory and drag seven bombs into it to test a bomb run.
 * Every round comes from the driveable's own definition, so the result is a loadout the
 * driveable can actually fire, and nothing is taken from the player's inventory.</p>
 *
 * <p>Ammunition already loaded is left alone, and a partly spent item is topped back up,
 * so repeating the command rearms without discarding a loadout chosen by hand. Naming a
 * round explicitly is the one way to overwrite what is loaded; whatever it displaces is
 * handed back to the operator.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RearmCommand
{
    private static final String NO_TARGET = "Ride a driveable or hold a driveable item to rearm it";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // Registered twice rather than redirected, so the namespaced spelling accepts the
        // bare form as well; a redirect only forwards the arguments that follow it.
        dispatcher.register(commandRoot("rearm"));
        dispatcher.register(commandRoot("flanrearm"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandRoot(String name)
    {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(context -> rearm(context, null))
            .then(Commands.argument("ammo", StringArgumentType.word())
                .suggests(RearmCommand::suggestAmmo)
                .executes(context -> rearm(context, StringArgumentType.getString(context, "ammo"))));
    }

    private static int rearm(CommandContext<CommandSourceStack> context, @Nullable String ammoName)
        throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Target target = findTarget(player);
        if (target == null)
        {
            context.getSource().sendFailure(Component.literal(NO_TARGET));
            return 0;
        }

        ShootableType requested = null;
        if (ammoName != null)
        {
            requested = ShootableType.findAmmoType(ammoName, target.type().getContentPack()).orElse(null);
            if (requested == null)
            {
                context.getSource().sendFailure(Component.literal("Unknown ammunition: " + ammoName));
                return 0;
            }
        }

        LoadReport report = DriveableAmmoLoader.load(target.type(), target.data(), requested);
        if (report.isEmpty())
        {
            context.getSource().sendFailure(Component.literal(
                target.type().getName() + " has no weapon or mounted-gun slots to load"));
            return 0;
        }

        int changed = report.changedSlots();
        if (changed > 0)
            target.commit();
        for (ItemStack displaced : report.displaced())
        {
            if (!player.addItem(displaced))
                player.drop(displaced, false);
        }

        send(context, ChatFormatting.GOLD, "=== " + target.type().getName() + target.suffix() + " ===");
        for (BankReport bank : report.banks())
            context.getSource().sendSuccess(() -> bankLine(bank), false);

        String summary = changed > 0
            ? "Rearmed " + target.type().getName() + ": " + changed + (changed == 1 ? " slot" : " slots")
            : "Nothing to load on " + target.type().getName() + "; everything is already loaded";
        context.getSource().sendSuccess(() -> Component.literal(summary), true);
        return changed;
    }

    private static Component bankLine(BankReport bank)
    {
        StringBuilder text = new StringBuilder("  ").append(bank.label())
            .append(" (").append(bank.slots()).append(bank.slots() == 1 ? " slot): " : " slots): ");
        if (bank.problem() != null)
            return Component.literal(text.append(bank.problem()).toString()).withStyle(ChatFormatting.RED);

        List<String> parts = new ArrayList<>(4);
        if (bank.loaded() > 0)
            parts.add(bank.loaded() + " loaded");
        if (bank.toppedUp() > 0)
            parts.add(bank.toppedUp() + " topped up");
        if (bank.replaced() > 0)
            parts.add(bank.replaced() + " replaced");
        if (bank.kept() > 0)
            parts.add(bank.kept() + " left as loaded");
        text.append(String.join(", ", parts)).append(" (").append(bank.ammoName()).append(')');
        return Component.literal(text.toString())
            .withStyle(bank.changed() > 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY);
    }

    private static CompletableFuture<Suggestions> suggestAmmo(CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder)
    {
        Entity executor = context.getSource().getEntity();
        Target target = executor instanceof ServerPlayer player ? findTarget(player) : null;
        return target == null ? Suggestions.empty()
            : SharedSuggestionProvider.suggest(DriveableAmmoLoader.loadableAmmo(target.type(), target.data())
                .stream().map(ShootableType::getShortName), builder);
    }

    @Nullable
    private static Target findTarget(ServerPlayer player)
    {
        Entity vehicle = player.getVehicle();
        Driveable driveable = vehicle instanceof Driveable direct ? direct
            : vehicle != null && vehicle.getVehicle() instanceof Driveable parent ? parent : null;
        if (driveable != null && driveable.getConfigType() != null && driveable.getDriveableData() != null)
            return new Target(driveable.getConfigType(), driveable.getDriveableData(), null);

        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DriveableItem<?, ?> item)
                return new Target(item.getConfigType(), DriveableData.fromStack(item.getConfigType(), stack), stack);
        }
        return null;
    }

    private static void send(CommandContext<CommandSourceStack> context, ChatFormatting color, String message)
    {
        context.getSource().sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    /**
     * The driveable being loaded. A ridden one owns its inventory and needs nothing written
     * back; an item carries it in NBT, so the loaded state has to be copied onto the stack.
     */
    private record Target(DriveableType type, DriveableData data, @Nullable ItemStack stack)
    {
        void commit()
        {
            if (stack != null)
                data.copyToStack(stack);
        }

        String suffix()
        {
            return stack == null ? " rearmed" : " rearmed (held item)";
        }
    }
}
