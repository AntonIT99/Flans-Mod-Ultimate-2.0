package com.flansmodultimate.common.types;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class GloveType extends InfoType
{
    @Getter
    protected int enchantability = 20;
    @Getter
    protected int durability = 200;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        enchantability = readValue("Enchantability", enchantability, file);
        durability = readValue("Durability", durability, file);
    }

    public boolean hasDurability()
    {
        return durability > 0;
    }

    public static Optional<GloveType> getGlove(String shortName)
    {
        if (InfoType.getInfoType(shortName) instanceof GloveType gloveType)
            return Optional.of(gloveType);
        return Optional.empty();
    }
}
