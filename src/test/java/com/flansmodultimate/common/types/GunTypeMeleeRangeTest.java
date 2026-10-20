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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        ItemStack stack = mock(ItemStack.class);
        AtomicReference<CompoundTag> tag = new AtomicReference<>();
        when(stack.getTag()).thenAnswer(ignored -> tag.get());
        doAnswer(invocation ->
        {
            tag.set(invocation.getArgument(0));
            return null;
        }).when(stack).setTag(any());
        var reach = new RangedAttribute("test.reach", 3D, 0D, 1024D);
        try (var attributes = mockStatic(ItemAttributes.class, CALLS_REAL_METHODS); var platform = mockStatic(NpcPlatform.class))
        {
            attributes.when(ItemAttributes::meleeReachAttribute).thenReturn(reach);
            platform.when(NpcPlatform::reachAttribute).thenReturn(reach);
            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            var modifier = modifiers.get(reach).iterator().next();
            assertEquals(2D, modifier.getAmount());
            assertEquals(modifier.getId(), item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack).get(reach).iterator().next().getId());
            assertTrue(item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack).get(reach).isEmpty());
            when(stack.getAttributeModifiers(EquipmentSlot.MAINHAND)).thenReturn(modifiers);
            assertEquals(5D, NpcEquipmentStats.meleeReach(stack));

            field.set(item, read("MeleeDamage 9", "MeleeRange 5", "UseCustomMelee true", "SecondaryFunction None"));
            assertTrue(item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack).get(reach).isEmpty());
            field.set(item, read("MeleeDamage 9"));
            assertTrue(item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack).get(reach).isEmpty());
        }
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
