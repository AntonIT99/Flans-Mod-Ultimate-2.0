package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.common.block.ArmorBoxBlock;
import com.flansmodultimate.common.block.GunBoxBlock;
import com.flansmodultimate.common.block.IFlanBlock;
import com.flansmodultimate.common.item.ArmorBoxItem;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunBoxItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.item.ToolItem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum EnumType
{
    ARMOR("armor", "armorFiles", ContentManager.TEXTURES_ARMOR_FOLDER, ArmorType.class, CustomArmorItem.class, null, false),
    ARMOR_BOX("armor_box", "armorBoxes", ContentManager.TEXTURES_SKINS_FOLDER, ArmorBoxType.class, ArmorBoxItem.class, ArmorBoxBlock.class, false),
    ATTACHMENT("attachment", "attachments", ContentManager.TEXTURES_SKINS_FOLDER, AttachmentType.class, AttachmentItem.class, null, false),
    BULLET("bullet", "bullets", ContentManager.TEXTURES_SKINS_FOLDER, BulletType.class, BulletItem.class, null, false),
    GRENADE("grenade", "grenades", ContentManager.TEXTURES_SKINS_FOLDER, GrenadeType.class, GrenadeItem.class, null, false),
    GUN("gun", "guns", ContentManager.TEXTURES_SKINS_FOLDER, GunType.class, GunItem.class, null, true),
    GUN_BOX("gun_box", "boxes", ContentManager.TEXTURES_SKINS_FOLDER, GunBoxType.class, GunBoxItem.class, GunBoxBlock.class, false),
    PARTS("part", "parts", ContentManager.TEXTURES_SKINS_FOLDER, PartType.class, PartItem.class, null, false),
    TOOLS("tool", "tools", ContentManager.TEXTURES_SKINS_FOLDER, ToolType.class, ToolItem.class, null, false);

    private final String identifier;
    private final String configFolderName;
    private final String textureFolderName;
    private final Class<? extends InfoType> typeClass;
    private final Class<? extends IFlanItem<?>> itemClass;
    private final Class<? extends IFlanBlock<?>> blockClass;
    private final boolean hasItem;
    private final boolean hasBlock;
    private final boolean handHeldItem;

    EnumType(String name, String configFolder, String textureFolder, Class<? extends InfoType> type, @Nullable Class<? extends IFlanItem<?>> item, @Nullable Class<? extends IFlanBlock<?>> block, boolean handHeld)
    {
        identifier = name;
        configFolderName = configFolder;
        textureFolderName = textureFolder;
        typeClass = type;
        itemClass = item;
        hasItem = (itemClass != null);
        blockClass = block;
        hasBlock = (blockClass != null);
        handHeldItem = handHeld;
    }

    @Unmodifiable
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
        return identifier;
    }
}
