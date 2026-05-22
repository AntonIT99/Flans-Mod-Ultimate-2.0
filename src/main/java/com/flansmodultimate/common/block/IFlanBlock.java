package com.flansmodultimate.common.block;

import com.flansmodultimate.common.types.InfoType;

import net.minecraft.world.level.block.Block;

public interface IFlanBlock<T extends InfoType>
{
    T getConfigType();

    Block asBlock();
}
