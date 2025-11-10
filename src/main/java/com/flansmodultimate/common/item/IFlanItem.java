package com.flansmodultimate.common.item;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfigs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public interface IFlanItem<T extends InfoType> extends ItemLike
{
    T getConfigType();

    default String getContentPack()
    {
        return FilenameUtils.getBaseName(getConfigType().getContentPack().getName());
    }

    default void appendHoverText(@NotNull List<Component> tooltipComponents)
    {
        if (BooleanUtils.isTrue(ModClientConfigs.showPackNameInItemDescriptions.get()) && !getContentPack().isBlank())
            tooltipComponents.add(Component.literal(getContentPack()).withStyle(ChatFormatting.GRAY));

        for (String line : getConfigType().getDescription().split("_"))
        {
            if (!line.isBlank())
                tooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.WHITE));
        }
    }

    /** Helper to render "BlueLabel: gray value" */
    static Component statLine(String label, String value)
    {
        return Component.literal(label)
            .withStyle(ChatFormatting.BLUE)
            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(value).withStyle(ChatFormatting.GRAY));
    }
}
