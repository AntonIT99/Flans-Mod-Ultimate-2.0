package com.flansmodultimate.apocalyse.common.block.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class PowerCubeBlockEntity extends BlockEntity
{
    private static final String NBT_AGE = "age";
    private int age;

    public PowerCubeBlockEntity(BlockPos pos, BlockState state)
    {
        super(ApocalypseContent.powerCubeBlockEntity.get(), pos, state);
    }

    public static void tick(PowerCubeBlockEntity cube)
    {
        cube.age++;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_AGE, age);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        age = tag.getInt(NBT_AGE);
    }
}
