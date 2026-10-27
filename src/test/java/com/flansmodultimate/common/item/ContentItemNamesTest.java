package com.flansmodultimate.common.item;

import com.flansmodultimate.common.block.IFlanBlock;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.platform.block.FlanBlock;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.FlanBlockItem;
import com.flansmodultimate.platform.item.FlanItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentItemNamesTest
{
    private static final String ITEM_KEY = "item.flansmod.fallback_test_2";
    private static final String BLOCK_KEY = "block.flansmod.fallback_test_2";

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void inventoryAndPlacedBlockNamesFallBackWithoutGeneratedLanguageFiles()
    {
        InfoType type = mock(InfoType.class);
        when(type.getName()).thenReturn("Definition Name");
        FlanItem item = mock(FlanItem.class, withSettings().extraInterfaces(IFlanItem.class));
        names(item, type, ITEM_KEY);
        FlanBlockItem blockItem = mock(FlanBlockItem.class, withSettings().extraInterfaces(IFlanItem.class));
        names(blockItem, type, BLOCK_KEY);
        FlanBlock block = mock(FlanBlock.class, withSettings().extraInterfaces(IFlanBlock.class));
        doReturn(type).when((IFlanBlock<?>) block).getConfigType();
        when(block.getDescriptionId()).thenReturn(BLOCK_KEY);
        when(block.getName()).thenCallRealMethod();
        assertEquals("Definition Name", stack(item).getHoverName().getString());
        assertEquals("Definition Name", stack(blockItem).getHoverName().getString());
        assertEquals("Definition Name", block.getName().getString());
    }

    @Test
    void authoredTranslationsStillWinAndAnvilNamesRemainLiteral()
    {
        InfoType type = mock(InfoType.class);
        when(type.getName()).thenReturn("Definition Name");
        FlanItem item = mock(FlanItem.class, withSettings().extraInterfaces(IFlanItem.class));
        names(item, type, ITEM_KEY);
        ItemStack stack = stack(item);
        Language previous = Language.getInstance();
        Language translated = mock(Language.class);
        when(translated.getOrDefault(ITEM_KEY, "Definition Name")).thenReturn("Translated Name");
        try
        {
            Language.inject(translated);
            assertEquals("Translated Name", stack.getHoverName().getString());
            stack.setHoverName(Component.literal("Player's Name"));
            assertEquals("Player's Name", stack.getHoverName().getString());
            assertEquals("Definition Name", type.getName());
        }
        finally
        {
            Language.inject(previous);
        }
    }

    @Test
    void armorUsesTheDefinitionFallbackToo() throws ReflectiveOperationException
    {
        ArmorType type = mock(ArmorType.class);
        when(type.getName()).thenReturn("Definition Helmet");
        CustomArmorItem armor = mock(CustomArmorItem.class);
        var field = CustomArmorItem.class.getDeclaredField("configType");
        field.setAccessible(true);
        field.set(armor, type);
        names(armor, type, ITEM_KEY);
        assertEquals("Definition Helmet", stack(armor).getHoverName().getString());
    }

    private static void names(Item item, InfoType type, String key)
    {
        doReturn(type).when((IFlanItem<?>) item).getConfigType();
        when(item.getDescriptionId(any(ItemStack.class))).thenReturn(key);
        when(item.getName(any(ItemStack.class))).thenCallRealMethod();
    }

    /** Real ItemStack naming and NBT methods, with only registry-backed item lookup replaced. */
    private static ItemStack stack(Item item)
    {
        ItemStack stack = mock(ItemStack.class, CALLS_REAL_METHODS);
        doReturn(item).when(stack).getItem();
        return stack;
    }
}
