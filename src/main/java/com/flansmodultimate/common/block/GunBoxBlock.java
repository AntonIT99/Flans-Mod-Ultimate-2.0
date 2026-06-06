package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.types.GunBoxType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import lombok.Getter;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class GunBoxBlock extends Block implements IFlanBlock<GunBoxType>
{
    @Getter
    protected final GunBoxType configType;

    public GunBoxBlock(GunBoxType type)
    {
        super(Properties.of()
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
        if (!(block instanceof GunBoxBlock gunBoxBlock))
            throw new IllegalStateException("Block at " + pos + " is not an instance of " + GunBoxBlock.class.getSimpleName());
        return new SimpleMenuProvider((containerId, inv, player) -> new GunBoxMenu(containerId, inv, pos, gunBoxBlock), gunBoxBlock.getName());
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

    public void buyGunServer(InfoType item, ServerPlayer player)
    {
        GunBoxType.GunBoxEntry entry = configType.findEntry(item);
        if (entry == null)
            return;

        if (!player.getAbilities().instabuild && !hasRequiredParts(player, entry))
            return;

        InfoType entryType = entry.getType();
        if (entryType == null)
            return;

        ItemStack resultStack = ModUtils.getItemStack(entryType).orElse(ItemStack.EMPTY);
        if (resultStack.isEmpty())
            return;

        prepareCraftedStack(resultStack, entryType);

        if (!player.getAbilities().instabuild)
        {
            for (ItemStack requiredPart : entry.getRequiredParts())
                InventoryHelper.consumeFromInventory(player.getInventory(), requiredPart);
        }

        if (!player.getInventory().add(resultStack))
            player.drop(resultStack, false);
    }

    private boolean hasRequiredParts(ServerPlayer player, GunBoxType.GunBoxEntry entry)
    {
        for (ItemStack requiredPart : entry.getRequiredParts())
        {
            if (InventoryHelper.countInInventory(player.getInventory(), requiredPart) < requiredPart.getCount())
                return false;
        }
        return true;
    }

    private void prepareCraftedStack(ItemStack stack, InfoType type)
    {
        if (type instanceof PaintableType paintableType && stack.getItem() instanceof IPaintableItem<?>)
            paintableType.applyPaintjobToStack(stack, paintableType.getDefaultPaintjob());

        if (stack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            CompoundTag tag = stack.getOrCreateTag();

            if (!tag.contains(GunItem.NBT_AMMO, Tag.TAG_LIST))
            {
                ListTag ammoList = new ListTag();
                for (int i = 0; i < gunType.getNumAmmoItemsInGun(stack); i++)
                    ammoList.add(new CompoundTag());
                tag.put(GunItem.NBT_AMMO, ammoList);
            }

            if (!tag.contains(IPaintableItem.NBT_PAINTJOB_ID, Tag.TAG_INT))
                gunType.applyPaintjobToStack(stack, gunType.getDefaultPaintjob());

            gunType.checkForTags(stack);
            gunType.getFireMode(stack);
        }
    }
}
