package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class ArmorBoxBlock extends Block implements IFlanBlock<ArmorBoxType>
{
    @Getter
    protected final ArmorBoxType configType;

    public ArmorBoxBlock(ArmorBoxType type)
    {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 4.0F)
            .sound(SoundType.WOOD)
        );
        configType = type;
    }

    @Override
    @NotNull
    public Block asBlock()
    {
        return this;
    }

    @Override
    @NotNull
    public MenuProvider getMenuProvider(BlockState state, @NotNull Level level, @NotNull BlockPos pos)
    {
        Block block = state.getBlock();
        if (!(block instanceof ArmorBoxBlock armorBoxBlock))
            throw new IllegalStateException("Block at " + pos + " is not an instance of " + ArmorBoxBlock.class.getSimpleName());
        return new SimpleMenuProvider((containerId, inv, player) -> new ArmorBoxMenu(containerId, inv, pos, armorBoxBlock), armorBoxBlock.getName());
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (player.isShiftKeyDown())
            return InteractionResult.PASS;

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            MenuProvider provider = getMenuProvider(state, level, pos);
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void buyArmorServer(int pageIndex, int pieceIndex, ServerPlayer player)
    {
        if (pageIndex < 0 || pageIndex >= configType.getPages().size())
            return;
        if (pieceIndex < 0 || pieceIndex >= 4)
            return;

        ArmorBoxType.ArmourBoxEntry entry = configType.getPages().get(pageIndex);
        ArmorType armorType = entry.getArmorType(pieceIndex);
        if (armorType == null)
            return;

        ItemStack resultStack = ModUtils.getItemStack(armorType).orElse(ItemStack.EMPTY);
        if (resultStack.isEmpty())
            return;

        if (!player.getAbilities().instabuild && !hasRequiredParts(player, entry, pieceIndex))
            return;

        if (!player.getAbilities().instabuild)
        {
            for (ItemStack requiredPart : entry.getRequiredStacks().get(pieceIndex))
                InventoryHelper.consumeFromInventory(player.getInventory(), requiredPart);
        }

        if (!player.getInventory().add(resultStack))
            player.drop(resultStack, false);
    }

    private boolean hasRequiredParts(ServerPlayer player, ArmorBoxType.ArmourBoxEntry entry, int pieceIndex)
    {
        for (ItemStack requiredPart : entry.getRequiredStacks().get(pieceIndex))
        {
            if (InventoryHelper.countInInventory(player.getInventory(), requiredPart) < requiredPart.getCount())
                return false;
        }
        return true;
    }
}
