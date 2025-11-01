package com.wolffsarmormod.common.paintjob;

import com.wolffsarmormod.common.types.PaintableType;
import lombok.Getter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class Paintjob
{
    public enum EnumPaintjobRarity
    {
        UNKNOWN,
        COMMON,
        UNCOMMON,
        RARE,
        LEGENDARY,
    }

    @Getter
    private final int id;
    private final PaintableType parent;
    private final String displayName;
    private final String icon;
    private final ResourceLocation texture;
    private final ItemStack[] dyesNeeded;
    private final EnumPaintjobRarity rarity;

    public Paintjob(PaintableType parent, int id, String icon, ResourceLocation texture, ItemStack[] dyesNeeded)
    {
        this(parent, id, "", icon, texture, dyesNeeded);
    }

    public Paintjob(PaintableType parent, int id, String displayName, String icon, ResourceLocation texture, ItemStack[] dyesNeeded)
    {
        this.parent = parent;
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.texture = texture;
        this.dyesNeeded = dyesNeeded;
        this.rarity = EnumPaintjobRarity.UNKNOWN;
    }

    public boolean isLegendary()
    {
        //TODO: uncomment this
        /*for (ItemStack stack : dyesNeeded)
        {
            if (stack.getItem() == FlansMod.rainbowPaintcan)
                return true;
        }*/
        return false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Paintjob otherPaintjob))
            return false;
        return parent.equals(otherPaintjob.parent) && id == otherPaintjob.id;
    }

    @Override
    public int hashCode()
    {
        return parent.hashCode() ^ id;
    }
}
