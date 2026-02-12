package com.flansmodultimate.common.types;

import lombok.Getter;

public abstract class BlockType extends InfoType
{
    @Getter
    protected String topTextureName;
    @Getter
    protected String bottomTextureName;
    @Getter
    protected String sideTextureName;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        topTextureName = readResource("TopTexture", topTextureName, file);
        bottomTextureName = readResource("BottomTexture", bottomTextureName, file);
        sideTextureName = readResource("SideTexture", sideTextureName, file);
    }
}
