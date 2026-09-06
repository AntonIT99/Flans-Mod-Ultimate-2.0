package com.flansmodultimate.common.item;

import com.flansmodultimate.common.teams.ITeamBase;
import com.flansmodultimate.common.teams.ITeamObject;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.TeamsMap;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketBaseEditState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ItemOpStick extends Item
{
    /** Vanilla operator level 2, the same gate the teams commands use. */
    public static final int PERMISSION_LEVEL = 2;
    private static final String NBT_MODE = "teams_mode";
    private static final String NBT_CONNECTION = "teams_connection";
    private static final String NBT_CONNECTION_BASE = "teams_connection_is_base";

    public enum Mode
    {
        OWNERSHIP("Ownership"),
        CONNECTING("Connecting"),
        MAPPING("Mapping"),
        DESTRUCTION("Destruction");

        private final String displayName;

        Mode(String displayName)
        {
            this.displayName = displayName;
        }
    }

    public ItemOpStick()
    {
        super(new Properties().stacksTo(1));
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown())
            return InteractionResultHolder.pass(stack);
        if (!canUse(player))
        {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("item.flansmodultimate.operator_stick.no_permission").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide)
        {
            Mode next = Mode.values()[(getMode(stack).ordinal() + 1) % Mode.values().length];
            stack.getOrCreateTag().putInt(NBT_MODE, next.ordinal());
            clearConnection(stack);
            player.displayClientMessage(Component.literal("Operator stick: " + next.displayName).withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Every operator stick action is reserved for server operators. */
    public static boolean canUse(Player player)
    {
        return player.hasPermissions(PERMISSION_LEVEL);
    }

    public void useOnTeamObject(ServerPlayer player, ITeamObject object, ItemStack stack)
    {
        if (!canUse(player))
        {
            player.displayClientMessage(Component.translatable("item.flansmodultimate.operator_stick.no_permission").withStyle(ChatFormatting.RED), true);
            return;
        }
        switch (getMode(stack))
        {
            case OWNERSHIP -> changeOwnership(player, object);
            case CONNECTING -> connect(player, object, stack);
            case MAPPING -> changeMap(player, object);
            case DESTRUCTION -> {
                TeamsManager.getInstance().destroyObject(object);
                player.displayClientMessage(Component.literal("Team object removed"), false);
            }
        }
    }

    private void changeOwnership(ServerPlayer player, ITeamObject object)
    {
        if (!(object instanceof ITeamBase base))
        {
            player.displayClientMessage(Component.literal("Ownership is configured on bases"), false);
            return;
        }
        PacketHandler.sendTo(PacketBaseEditState.create(TeamsManager.getInstance(), base), player);
    }

    private void connect(ServerPlayer player, ITeamObject object, ItemStack stack)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(NBT_CONNECTION))
        {
            tag.putUUID(NBT_CONNECTION, object.getObjectId());
            tag.putBoolean(NBT_CONNECTION_BASE, object instanceof ITeamBase);
            player.displayClientMessage(Component.literal("First endpoint selected"), false);
            return;
        }

        UUID firstId = tag.getUUID(NBT_CONNECTION);
        boolean firstIsBase = tag.getBoolean(NBT_CONNECTION_BASE);
        TeamsManager manager = TeamsManager.getInstance();
        ITeamBase base;
        ITeamObject child;
        if (firstIsBase && !(object instanceof ITeamBase))
        {
            base = manager.getBase(firstId).orElse(null);
            child = object;
        }
        else if (!firstIsBase && object instanceof ITeamBase clickedBase)
        {
            base = clickedBase;
            child = manager.getObject(firstId).orElse(null);
        }
        else
        {
            player.displayClientMessage(Component.literal("Connect one base to one team object"), false);
            clearConnection(stack);
            return;
        }

        if (base == null || child == null)
            player.displayClientMessage(Component.literal("The first endpoint is no longer loaded"), false);
        else
        {
            manager.connectObject(base, child);
            player.displayClientMessage(Component.literal("Team object connected to " + base.getBaseName()), false);
        }
        clearConnection(stack);
    }

    private void changeMap(ServerPlayer player, ITeamObject object)
    {
        if (!(object instanceof ITeamBase base))
        {
            player.displayClientMessage(Component.literal("Maps are configured on bases"), false);
            return;
        }
        List<TeamsMap> maps = TeamsManager.getInstance().getMaps().stream()
            .filter(map -> map.getDimension().equals(player.level().dimension())).toList();
        if (maps.isEmpty())
        {
            player.displayClientMessage(Component.literal("Create a map first with /teams map add"), false);
            return;
        }
        int current = -1;
        for (int i = 0; i < maps.size(); i++)
            if (maps.get(i).getShortName().equals(base.getMapId())) current = i;
        TeamsMap next = maps.get((current + 1) % maps.size());
        TeamsManager.getInstance().assignBaseToMap(base, next);
        player.displayClientMessage(Component.literal("Base assigned to " + next.getName()), false);
    }

    public static Mode getMode(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        int value = tag == null ? 0 : tag.getInt(NBT_MODE);
        return Mode.values()[Math.floorMod(value, Mode.values().length)];
    }

    private static void clearConnection(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null)
        {
            tag.remove(NBT_CONNECTION);
            tag.remove(NBT_CONNECTION_BASE);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag)
    {
        tooltip.add(Component.translatable(TooltipKeys.OPERATOR_STICK_MODE,
            Component.translatable("tooltip.flansmodultimate.operator_stick.mode." + getMode(stack).name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(TooltipKeys.OPERATOR_STICK_CHANGE_MODE).withStyle(ChatFormatting.GRAY));
    }
}
