package com.flansmodultimate.common.command;

import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.ModUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DefaultAmmoCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("defaultammo")
            .requires(source -> source.hasPermission(2))
            .executes(context -> giveDefaultAmmo(context, 1))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                .executes(context -> giveDefaultAmmo(context, IntegerArgumentType.getInteger(context, "amount")))
            )
        );
    }

    private static int giveDefaultAmmo(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HeldWeapon heldWeapon = getHeldWeapon(player);
        if (heldWeapon == null)
        {
            context.getSource().sendFailure(Component.literal("Hold a Flan's Mod gun or AA gun in either hand"));
            return 0;
        }

        Optional<ShootableType> ammoType = getDefaultAmmoType(heldWeapon.stack());
        if (ammoType.isEmpty())
        {
            context.getSource().sendFailure(Component.literal("Held item has no default ammo"));
            return 0;
        }

        ItemStack ammoStack = ModUtils.getItemStack(ammoType.get(), amount).orElse(ItemStack.EMPTY);
        if (ammoStack.isEmpty())
        {
            context.getSource().sendFailure(Component.literal("Default ammo item is not registered: " + ammoType.get().getShortName()));
            return 0;
        }

        if (ammoStack.getItem() instanceof ShootableItem)
            ShootableItem.setRoundsRemaining(ammoStack, ammoType.get().getRoundsPerItem());

        ItemStack displayStack = ammoStack.copy();
        boolean added = player.addItem(ammoStack);
        if (!added && !ammoStack.isEmpty())
            player.drop(ammoStack, false);

        String weaponName = heldWeapon.stack().getHoverName().getString();
        String ammoName = displayStack.getHoverName().getString();
        int given = displayStack.getCount();
        context.getSource().sendSuccess(() -> Component.literal("Gave " + given + "x " + ammoName + " for " + weaponName), true);
        return given;
    }

    @Nullable
    private static HeldWeapon getHeldWeapon(ServerPlayer player)
    {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isSupportedWeapon(mainHand))
            return new HeldWeapon(mainHand);

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isSupportedWeapon(offHand))
            return new HeldWeapon(offHand);

        return null;
    }

    private static boolean isSupportedWeapon(ItemStack stack)
    {
        return !stack.isEmpty() && (stack.getItem() instanceof GunItem || stack.getItem() instanceof AAGunItem);
    }

    private static Optional<ShootableType> getDefaultAmmoType(ItemStack weaponStack)
    {
        if (weaponStack.getItem() instanceof GunItem gunItem)
            return gunItem.getConfigType().getDefaultAmmo();
        if (weaponStack.getItem() instanceof AAGunItem aaGunItem)
            return aaGunItem.getConfigType().getDefaultAmmo();
        return Optional.empty();
    }

    private record HeldWeapon(ItemStack stack) {}
}
