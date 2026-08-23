package com.flansmodultimate.apocalyse.common.block.entity;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.putInt(NBT_AGE, age);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        age = input.getIntOr(NBT_AGE, 0);
    }
}
