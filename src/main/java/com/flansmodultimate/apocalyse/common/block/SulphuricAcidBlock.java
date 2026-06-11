package com.flansmodultimate.apocalyse.common.block;

import com.flansmodultimate.apocalyse.common.ApocalypseDamageSources;
import com.flansmodultimate.config.ModApocalypseConfig;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class SulphuricAcidBlock extends LiquidBlock
{
    public SulphuricAcidBlock(RegistryObject<FlowingFluid> fluid, BlockBehaviour.Properties properties)
    {
        super(fluid, properties);
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity)
    {
        if (!level.isClientSide && ModApocalypseConfig.apocalypseEnabled() && ModApocalypseConfig.apocalypseAcidDamage() > 0.0F)
            entity.hurt(ApocalypseDamageSources.sulphuricAcid(level), ModApocalypseConfig.apocalypseAcidDamage());
        super.entityInside(state, level, pos, entity);
    }
}
