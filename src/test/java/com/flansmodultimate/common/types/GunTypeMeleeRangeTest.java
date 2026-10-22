package com.flansmodultimate.common.types;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.platform.item.ItemAttributes;
import com.wolffsmod.npcs.combat.NpcEquipmentStats;
import com.wolffsmod.npcs.platform.NpcPlatform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GunTypeMeleeRangeTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void defaultAndInvalidRangesPreserveLegacyReach()
    {
        assertEquals(3F, read("ShortName testWeapon").getMeleeRange());
        assertEquals(5F, read("MeleeRange 4", "MeleeRange 5").getMeleeRange());
        for (String invalid : List.of("0", "-1", "NaN", "Infinity", "-Infinity", "invalid"))
        {
            assertEquals(3F, read("MeleeRange " + invalid).getMeleeRange(), invalid);
            GunType type = read("MeleeRange 5");
            type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of("MeleeRange " + invalid)));
            assertEquals(5F, type.getMeleeRange(), invalid);
        }
    }

    @Test
    // Inject the type without registering an item in the frozen plain-JUnit registry.
    @SuppressWarnings("java:S3011")
    void ordinaryReachFeedsNpcEquipmentAndExcludesCustomPaths() throws ReflectiveOperationException
    {
        GunItem item = mock(GunItem.class, CALLS_REAL_METHODS);
        var field = GunItem.class.getDeclaredField("configType");
        field.setAccessible(true);
        field.set(item, read("MeleeDamage 9", "MeleeRange 5"));
        ItemStack stack = stack(item);
        Holder<Attribute> reach = Holder.direct(new RangedAttribute("test.reach", 3D, 0D, 1024D));
        try (var attributes = mockStatic(ItemAttributes.class, CALLS_REAL_METHODS); var platform = mockStatic(NpcPlatform.class))
        {
            attributes.when(ItemAttributes::meleeReachAttribute).thenReturn(reach);
            platform.when(NpcPlatform::reachAttribute).thenReturn(reach);
            List<AttributeModifier> mainHand = modifiers(item, stack, EquipmentSlot.MAINHAND, reach);
            assertEquals(1, mainHand.size());
            assertEquals(2D, mainHand.get(0).amount());
            assertEquals(mainHand.get(0).id(), modifiers(item, stack, EquipmentSlot.MAINHAND, reach).get(0).id());
            assertTrue(modifiers(item, stack, EquipmentSlot.OFFHAND, reach).isEmpty());
            assertEquals(5D, NpcEquipmentStats.meleeReach(stack));

            field.set(item, read("MeleeDamage 9", "MeleeRange 5", "UseCustomMelee true", "SecondaryFunction None"));
            assertTrue(modifiers(item, stack, EquipmentSlot.MAINHAND, reach).isEmpty());
            field.set(item, read("MeleeDamage 9"));
            assertTrue(modifiers(item, stack, EquipmentSlot.MAINHAND, reach).isEmpty());
        }
    }

    /** The item's own modifiers of one attribute in the slot, as a stack without enchantments reports them. */
    private static List<AttributeModifier> modifiers(GunItem item, ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute)
    {
        List<AttributeModifier> found = new ArrayList<>();
        item.getDefaultAttributeModifiers(stack).forEach(slot, (current, modifier) ->
        {
            if (current.equals(attribute))
                found.add(modifier);
        });
        return found;
    }

    /** A stack of the item that reports the item's own modifiers and holds no data components. */
    private static ItemStack stack(GunItem item)
    {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getOrDefault(any(DataComponentType.class), any())).thenAnswer(invocation -> invocation.getArgument(1));
        doAnswer(invocation ->
        {
            BiConsumer<Holder<Attribute>, AttributeModifier> action = invocation.getArgument(1);
            item.getDefaultAttributeModifiers(stack).forEach((EquipmentSlot) invocation.getArgument(0), action);
            return null;
        }).when(stack).forEachModifier(any(EquipmentSlot.class), any());
        return stack;
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
