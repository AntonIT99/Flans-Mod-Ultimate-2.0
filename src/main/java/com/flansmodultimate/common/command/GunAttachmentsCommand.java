package com.flansmodultimate.common.command;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.AttachmentType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.util.ModUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Player-facing inspection of the attachments accepted by the held gun. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GunAttachmentsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(commandRoot("flans", true));
        dispatcher.register(commandRoot("flansmodultimate", false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandRoot(String name,
        boolean includeLegacyAlias)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
            .then(Commands.literal("attachments").executes(GunAttachmentsCommand::listAttachments));
        if (includeLegacyAlias)
            root.then(Commands.literal("allowedAttachments").executes(GunAttachmentsCommand::listAttachments));
        return root;
    }

    private static int listAttachments(CommandContext<CommandSourceStack> context)
        throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack gunStack = heldGun(player);
        if (gunStack.isEmpty())
        {
            context.getSource().sendFailure(Component.translatable(
                "commands.flansmodultimate.attachments.no_gun"));
            return 0;
        }

        GunType gun = ((GunItem)gunStack.getItem()).getConfigType();
        if (gun.isAllowAllAttachments())
        {
            context.getSource().sendSuccess(() -> Component.translatable(
                "commands.flansmodultimate.attachments.all", gunStack.getHoverName())
                .withStyle(ChatFormatting.BLUE), false);
            return 1;
        }

        List<AttachmentType> attachments = gun.getAllowedAttachments().stream()
            .filter(Objects::nonNull)
            .toList();
        if (attachments.isEmpty())
        {
            context.getSource().sendSuccess(() -> Component.translatable(
                "commands.flansmodultimate.attachments.none", gunStack.getHoverName())
                .withStyle(ChatFormatting.BLUE), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable(
            "commands.flansmodultimate.attachments.header", gunStack.getHoverName(), attachments.size())
            .withStyle(ChatFormatting.BLUE), false);
        for (int i = 0; i < attachments.size(); i++)
            sendAttachment(context.getSource(), i + 1, attachments.get(i));
        return attachments.size();
    }

    private static ItemStack heldGun(ServerPlayer player)
    {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof GunItem)
            return mainHand;

        ItemStack offHand = player.getOffhandItem();
        return offHand.getItem() instanceof GunItem ? offHand : ItemStack.EMPTY;
    }

    private static void sendAttachment(CommandSourceStack source, int index, AttachmentType attachment)
    {
        ItemStack stack = ModUtils.getItemStack(attachment).orElse(ItemStack.EMPTY);
        Component name = ModUtils.getDisplayName(attachment);
        ResourceLocation id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null)
            id = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, attachment.getShortName());

        Component line = Component.literal(index + ". ").withStyle(ChatFormatting.GOLD)
            .append(name.copy().withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" — ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(id.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        source.sendSuccess(() -> line, false);
    }
}
