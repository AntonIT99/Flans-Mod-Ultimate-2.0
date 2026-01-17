package com.flansmodultimate.common.paintjob;

import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

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

    private final PaintableType type;
    @Getter
    private final int id;
    @Getter
    private final String displayName;
    @Getter
    private final String iconName;
    @Getter
    private final String textureName;
    @Getter
    private final List<Supplier<ItemStack>> dyesNeeded;
    @Getter @OnlyIn(Dist.CLIENT)
    private final ResourceLocation texture;

    @Getter @Setter
    private EnumPaintjobRarity rarity;
    @Getter @Setter
    private boolean addToTables = true;

    private Paintjob(PaintableType type, int id, String displayName, String iconName, String textureName, ResourceLocation texture, List<Supplier<ItemStack>> dyesNeeded)
    {
        this.type = type;
        this.id = id;
        this.displayName = displayName;
        this.iconName = iconName;
        this.dyesNeeded = dyesNeeded;
        this.rarity = EnumPaintjobRarity.UNKNOWN;
        this.texture = texture;
        this.textureName = textureName;
    }

    public Paintjob(PaintableType type, int id, String displayName, String iconName, String textureName, List<Supplier<ItemStack>> dyesNeeded)
    {
        this(type, id, displayName, iconName, textureName, (FMLEnvironment.dist == Dist.CLIENT) ? InfoType.loadTexture(textureName, type) : null, dyesNeeded);
    }

    public boolean isLegendary()
    {
        return !type.getNonLegendaryPaintjobs().containsKey(id);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Paintjob otherPaintjob))
            return false;
        return type.equals(otherPaintjob.type) && id == otherPaintjob.id;
    }

    @Override
    public int hashCode()
    {
        return type.hashCode() ^ id;
    }
}
