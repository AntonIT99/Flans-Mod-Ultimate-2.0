package com.flansmodultimate.apocalyse.common.block.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PowerCubeBlockEntity extends BlockEntity
{
    private int age;

    public PowerCubeBlockEntity(BlockPos pos, BlockState state)
    {
        super(ApocalypseContent.POWER_CUBE_BLOCK_ENTITY.get(), pos, state);
    }

    public int getAge()
    {
        return age;
    }

    public static void tick(PowerCubeBlockEntity cube)
    {
        cube.age++;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("Age", age);
    }

    @Override
    public void load(@NotNull CompoundTag tag)
    {
        super.load(tag);
        age = tag.getInt("Age");
    }
}
