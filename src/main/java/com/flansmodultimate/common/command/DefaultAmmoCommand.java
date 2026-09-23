package com.flansmodultimate.common.command;

import com.flansmodultimate.common.driveables.DriveableAmmoLoader;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.DriveableItem;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        List<AmmoSource> sources = getHeldSources(player);
        if (sources.isEmpty())
        {
            AmmoSource ridden = getRiddenSource(player);
            if (ridden != null)
                sources = List.of(ridden);
        }
        if (sources.isEmpty())
        {
            context.getSource().sendFailure(Component.literal(
                "Hold an ammo-using Flan's Mod item or ride a driveable, AA gun, or deployed gun"));
            return 0;
        }

        Set<ShootableType> ammoTypes = new LinkedHashSet<>();
        sources.forEach(source -> ammoTypes.addAll(source.ammo()));
        String sourceNames = String.join(", ", sources.stream().map(AmmoSource::name).distinct().toList());
        if (ammoTypes.isEmpty())
        {
            context.getSource().sendFailure(Component.literal(sourceNames + " has no default ammunition"));
            return 0;
        }

        int totalGiven = 0;
        List<String> givenAmmo = new ArrayList<>();
        List<String> missingAmmo = new ArrayList<>();
        for (ShootableType ammoType : ammoTypes)
        {
            ItemStack ammoStack = ModUtils.getItemStack(ammoType, amount).orElse(ItemStack.EMPTY);
            if (ammoStack.isEmpty())
            {
                missingAmmo.add(ammoType.getShortName());
                continue;
            }

            if (ammoStack.getItem() instanceof ShootableItem && ammoType.getRoundsPerItem() > 1)
                ShootableItem.setRoundsRemaining(ammoStack, ammoType.getRoundsPerItem());

            ItemStack displayStack = ammoStack.copy();
            if (!player.addItem(ammoStack) && !ammoStack.isEmpty())
                player.drop(ammoStack, false);

            totalGiven += displayStack.getCount();
            givenAmmo.add(displayStack.getCount() + "x " + displayStack.getHoverName().getString());
        }

        if (!givenAmmo.isEmpty())
            context.getSource().sendSuccess(() -> Component.literal(
                "Gave " + String.join(", ", givenAmmo) + " for " + sourceNames), true);
        if (!missingAmmo.isEmpty())
            context.getSource().sendFailure(Component.literal(
                "Default ammunition item is not registered: " + String.join(", ", missingAmmo)));
        return totalGiven;
    }

    private static List<AmmoSource> getHeldSources(ServerPlayer player)
    {
        List<AmmoSource> sources = new ArrayList<>(2);
        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            AmmoSource source = getHeldSource(stack, player.registryAccess());
            if (source != null)
                sources.add(source);
        }
        return sources;
    }

    @Nullable
    private static AmmoSource getHeldSource(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries)
    {
        if (stack.isEmpty())
            return null;
        String name = stack.getHoverName().getString();
        if (stack.getItem() instanceof GunItem gunItem)
            return source(name, defaultAmmo(gunItem.getConfigType().getDefaultAmmo()));
        if (stack.getItem() instanceof AAGunItem aaGunItem)
            return source(name, defaultAmmo(aaGunItem.getConfigType().getDefaultAmmo()));
        if (stack.getItem() instanceof DriveableItem<?, ?> driveableItem)
        {
            DriveableData data = DriveableData.fromStack(driveableItem.getConfigType(), stack, registries);
            return source(name, DriveableAmmoLoader.defaultAmmo(driveableItem.getConfigType(), data));
        }
        return null;
    }

    @Nullable
    private static AmmoSource getRiddenSource(ServerPlayer player)
    {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AAGun gun && gun.getConfigType() != null)
            return source(gun.getConfigType().getName(), defaultAmmo(gun.getConfigType().getDefaultAmmo()));
        if (vehicle instanceof DeployedGun gun && gun.getConfigType() != null)
            return source(gun.getConfigType().getName(), defaultAmmo(gun.getConfigType().getDefaultAmmo()));

        Driveable driveable = vehicle instanceof Driveable direct ? direct
            : vehicle instanceof Seat seat ? seat.getDriveable()
            : vehicle != null && vehicle.getVehicle() instanceof Driveable parent ? parent : null;
        if (driveable == null || driveable.getConfigType() == null || driveable.getDriveableData() == null)
            return null;
        return source(driveable.getConfigType().getName(),
            DriveableAmmoLoader.defaultAmmo(driveable.getConfigType(), driveable.getDriveableData()));
    }

    private static Set<ShootableType> defaultAmmo(Optional<ShootableType> ammo)
    {
        Set<ShootableType> result = new LinkedHashSet<>();
        ammo.ifPresent(result::add);
        return result;
    }

    @Nullable
    private static AmmoSource source(String name, Set<ShootableType> ammo)
    {
        return ammo.isEmpty() ? null : new AmmoSource(name, ammo);
    }

    private record AmmoSource(String name, Set<ShootableType> ammo) {}
}
