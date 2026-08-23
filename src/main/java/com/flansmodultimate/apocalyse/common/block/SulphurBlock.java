package com.flansmodultimate.apocalyse.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public class SulphurBlock extends FallingBlock
{
    public static final MapCodec<SulphurBlock> CODEC = simpleCodec(SulphurBlock::new);
    public SulphurBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec()
    {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos)
    {
        return 0xD8C34A;
    }
}
