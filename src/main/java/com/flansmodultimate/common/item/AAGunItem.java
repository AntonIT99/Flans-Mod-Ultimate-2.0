package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.hooks.ClientHooks;
import lombok.Getter;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

public class AAGunItem extends Item implements IFlanItem<AAGunType>, ICustomRendereredItem<AAGunType>
{
    @Getter
    protected final AAGunType configType;

    public AAGunItem(AAGunType configType)
    {
        super(new Item.Properties());
        this.configType = configType;
    }

    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        ClientHooks.RENDER.initCustomBewlr(consumer);
    }

    @Override
    public boolean useCustomRendererInHand()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererOnGround()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInFrame()
    {
        return true;
    }

    @Override
    public boolean useCustomRendererInGui()
    {
        return true;
    }

    @Override
    @NotNull
    public InteractionResult useOn(@NotNull UseOnContext context)
    {
        Level level = context.getLevel();
        var player = context.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        BlockState stateAtHit = level.getBlockState(pos);
        if (stateAtHit.is(Blocks.SNOW) || stateAtHit.getBlock() instanceof SnowLayerBlock)
        {
            pos = pos.below();
        }

        Direction direction = player.getDirection();

        BlockPos base = pos;
        BlockPos aaPos = pos.above();
        BlockPos forwardAbove = aaPos.relative(direction);

        if (!level.isClientSide
            && isSolidTop(level, base)
            && isReplaceableOrAir(level, aaPos)
            && isReplaceableOrAir(level, forwardAbove)
            && isReplaceableOrAir(level, base.relative(direction)))
        {
            boolean exists = !level.getEntitiesOfClass(AAGun.class, new AABB(aaPos)).isEmpty();
            if (!exists)
            {
                AAGun aaGun = new AAGun(level, aaPos, direction, configType);
                level.addFreshEntity(aaGun);

                if (!player.getAbilities().instabuild)
                    stack.shrink(1);

                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.FAIL;
    }

    private static boolean isReplaceableOrAir(Level level, BlockPos pos)
    {
        BlockState st = level.getBlockState(pos);
        return st.isAir() || st.is(Blocks.SNOW) || st.canBeReplaced();
    }

    private static boolean isSolidTop(Level level, BlockPos pos)
    {
        BlockState st = level.getBlockState(pos);
        return st.isSolidRender(level, pos) && st.canOcclude();
    }
}