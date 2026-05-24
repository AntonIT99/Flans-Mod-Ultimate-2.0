package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.common.block.ArmorBoxBlock;
import com.flansmodultimate.common.block.GunBoxBlock;
import com.flansmodultimate.common.block.IFlanBlock;
import com.flansmodultimate.common.block.ItemHolderBlock;
import com.flansmodultimate.common.item.AAGunItem;
import com.flansmodultimate.common.item.ArmorBoxItem;
import com.flansmodultimate.common.item.AttachmentItem;
import com.flansmodultimate.common.item.BulletItem;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GloveItem;
import com.flansmodultimate.common.item.GrenadeItem;
import com.flansmodultimate.common.item.GunBoxItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.ItemHolderItem;
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
    AA_GUN("aagun", "aaguns", ContentManager.FOLDER_TEXTURES_SKINS, AAGunType.class, AAGunItem.class, null, false, 9),
    ARMOR("armor", "armorFiles", ContentManager.FOLDER_TEXTURES_ARMOR, ArmorType.class, CustomArmorItem.class, null, false, 0),
    ARMOR_BOX("armor_box", "armorBoxes", ContentManager.FOLDER_TEXTURES_SKINS, ArmorBoxType.class, ArmorBoxItem.class, ArmorBoxBlock.class, false, 7),
    ATTACHMENT("attachment", "attachments", ContentManager.FOLDER_TEXTURES_SKINS, AttachmentType.class, AttachmentItem.class, null, false, 1),
    BULLET("bullet", "bullets", ContentManager.FOLDER_TEXTURES_SKINS, BulletType.class, BulletItem.class, null, false, 2),
    GLOVE("glove", "gloves", ContentManager.FOLDER_TEXTURES_SKINS, GloveType.class, GloveItem.class, null, false, 10),
    GRENADE("grenade", "grenades", ContentManager.FOLDER_TEXTURES_SKINS, GrenadeType.class, GrenadeItem.class, null, false, 3),
    GUN("gun", "guns", ContentManager.FOLDER_TEXTURES_SKINS, GunType.class, GunItem.class, null, true, 4),
    GUN_BOX("gun_box", "boxes", ContentManager.FOLDER_TEXTURES_SKINS, GunBoxType.class, GunBoxItem.class, GunBoxBlock.class, false, 8),
    ITEM_HOLDER("item_holder", "itemHolders", ContentManager.FOLDER_TEXTURES_SKINS, ItemHolderType.class, ItemHolderItem.class, ItemHolderBlock.class, false, 11),
    PART("part", "parts", ContentManager.FOLDER_TEXTURES_SKINS, PartType.class, PartItem.class, null, false, 5),
    TOOL("tool", "tools", ContentManager.FOLDER_TEXTURES_SKINS, ToolType.class, ToolItem.class, null, false, 6);

    private final String identifier;
    private final String configFolderName;
    private final String textureFolderName;
    private final Class<? extends InfoType> typeClass;
    private final Class<? extends IFlanItem<?>> itemClass;
    private final Class<? extends IFlanBlock<?>> blockClass;
    private final boolean hasItem;
    private final boolean hasBlock;
    private final boolean handHeldItem;
    private final int loadOrder;

    EnumType(String name, String configFolder, String textureFolder, Class<? extends InfoType> type, @Nullable Class<? extends IFlanItem<?>> item, @Nullable Class<? extends IFlanBlock<?>> block, boolean handHeld, int order)
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
        loadOrder = order;
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
