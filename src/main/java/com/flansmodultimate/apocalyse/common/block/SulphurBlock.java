package com.flansmodultimate.apocalyse.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

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
}
