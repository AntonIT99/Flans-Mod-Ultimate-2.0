package com.flansmodultimate.apocalyse.common.block.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.platform.block.FlanBlockEntity;
import lombok.Getter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class PowerCubeBlockEntity extends FlanBlockEntity
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
    protected void saveData(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt(NBT_AGE, age);
    }

    @Override
    protected void loadData(CompoundTag tag, HolderLookup.Provider registries)
    {
        age = tag.getInt(NBT_AGE);
    }
}
