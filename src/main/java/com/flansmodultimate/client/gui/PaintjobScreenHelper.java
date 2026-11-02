package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModItems;
import com.flansmodultimate.common.paintjob.Paintjob;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PaintjobScreenHelper
{
    static void addPaymentTooltip(List<Component> lines, Paintjob paintjob)
    {
        if (!paintjob.hasDyeCost())
            return;

        lines.add(Component.translatable("gui.flansmod.paintjob.use_dyes"));
        int cans = paintjob.getDyesNeeded(true).stream().filter(stack -> stack.is(FlansModItems.rainbowPaintcan.get())).mapToInt(ItemStack::getCount).sum();
        lines.add(Component.translatable("gui.flansmod.paintjob.use_rainbow", cans));
    }
}
