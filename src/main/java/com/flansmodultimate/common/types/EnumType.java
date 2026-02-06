package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ToolItem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum EnumType
{
    ARMOR("armor", "armorFiles", ContentManager.TEXTURES_ARMOR_FOLDER, ArmorType.class, CustomArmorItem.class, false),
    ATTACHMENT("attachment", "attachments", ContentManager.TEXTURES_SKINS_FOLDER, AttachmentType.class, AttachmentItem.class, false),
    BULLET("bullet", "bullets", ContentManager.TEXTURES_SKINS_FOLDER, BulletType.class, BulletItem.class, false),
    GRENADE("grenade", "grenades", ContentManager.TEXTURES_SKINS_FOLDER, GrenadeType.class, GrenadeItem.class, false),
    GUN("gun", "guns", ContentManager.TEXTURES_SKINS_FOLDER, GunType.class, GunItem.class, false),
    PARTS("part", "parts", ContentManager.TEXTURES_SKINS_FOLDER, PartType.class, PartItem.class, false),
    TOOLS("tool", "tools", ContentManager.TEXTURES_SKINS_FOLDER, ToolType.class, ToolItem.class, false);

    private final String displayName;
    private final String configFolderName;
    private final String textureFolderName;
    private final Class<? extends InfoType> typeClass;
    private final Class<? extends IFlanItem<?>> itemClass;
    private final boolean itemType;
    private final boolean blockType;

    EnumType(String name, String configFolder, String textureFolder, Class<? extends InfoType> type, @Nullable Class<? extends IFlanItem<?>> item, boolean isBlock)
    {
        displayName = name;
        configFolderName = configFolder;
        textureFolderName = textureFolder;
        typeClass = type;
        itemClass = item;
        itemType = (item != null);
        blockType = isBlock;
    }

    public static List<String> getFoldersList()
    {
        return Arrays.stream(EnumType.values()).map(EnumType::getConfigFolderName).toList();
    }

    public static Optional<EnumType> getType(String folderName)
    {
        return Arrays.stream(EnumType.values()).filter(type -> StringUtils.equals(type.getConfigFolderName(), folderName)).findFirst();
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
