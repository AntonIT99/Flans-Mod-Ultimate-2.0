package com.flansmodultimate.common.block;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockFactory
{
    @Nullable
    public static Block createBlock(InfoType config, BlockBehaviour.Properties properties)
    {
        try
        {
            Class<? extends InfoType> typeClass = config.getType().getTypeClass();
            Class<? extends IFlanBlock<?>> blockClass = config.getType().getBlockClass();
            return blockClass.getConstructor(typeClass, BlockBehaviour.Properties.class)
                .newInstance(typeClass.cast(config), properties).asBlock();
        }
        catch (Exception e)
        {
            FlansMod.log.error("Failed to instantiate Block for {}", config);
            LogUtils.logErrorWithoutStacktrace(e);
            return null;
        }
    }
}
