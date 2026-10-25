package com.flansmodultimate.common.types;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcEquipment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GunTypeMeleeSpeedTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));

    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void legacyPacksKeepTheVanillaAttackRate()
    {
        assertEquals(4F, read("ShortName testWeapon").getMeleeSpeed());
    }

    @Test
    void vehicleMeleeOverridesSupportBothSpellingsWithoutChangingLivingDamage()
    {
        for (String key : List.of("MeleeVsDriveable", "MeleeVsDriveables", "MeleeDamageVsDriveable", "MeleeDamageVsDriveables"))
        {
            GunType type = read("ShortName testWeapon", "MeleeDamage 12", "MeleeDamageDriveableModifier 3", key + " 24");
            assertEquals(12F, type.getMeleeDamage(ItemStack.EMPTY, false));
            assertEquals(24F, type.getMeleeDamage(ItemStack.EMPTY, true));
        }
        GunType legacy = read("ShortName testWeapon", "MeleeDamage 12", "MeleeDamageDriveableModifier 3");
        assertEquals(36F, legacy.getMeleeDamage(ItemStack.EMPTY, true));
        assertEquals(0F, read("ShortName testWeapon", "MeleeDamageVsDriveables 0").getMeleeDamage(ItemStack.EMPTY, true));
    }

    @Test
    void invalidVehicleDamageDoesNotReplaceAValidOverride()
    {
        for (String invalid : List.of("-1", "NaN", "Infinity", "-Infinity", "invalid"))
        {
            GunType type = read("ShortName testWeapon", "MeleeDamageVsDriveables 24");
            type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of("MeleeDamageVsDriveables " + invalid)));
            assertEquals(24F, type.getMeleeDamage(ItemStack.EMPTY, true));
        }
    }

    @Test
    void theLastAuthoredRateWins()
    {
        assertEquals(0.9F, read("ShortName testWeapon", "MeleeSpeed 1.6", "MeleeSpeed 0.9").getMeleeSpeed());
    }

    @Test
    void invalidRatesPreserveThePreviousOrDefaultValue()
    {
        for (String invalid : List.of("0", "-1", "NaN", "Infinity", "-Infinity", "invalid"))
        {
            assertEquals(4F, read("ShortName testWeapon", "MeleeSpeed " + invalid).getMeleeSpeed(), invalid);
            GunType type = read("ShortName testWeapon", "MeleeSpeed 1.6");
            type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of("MeleeSpeed " + invalid)));
            assertEquals(1.6F, type.getMeleeSpeed(), invalid);
        }
    }

    @Test
    // A plain JUnit run has frozen registries, so inject the parsed type without constructing/registering an item.
    @SuppressWarnings("java:S3011")
    void speedIsAppliedOnlyInTheMainHandAndHasAStableModifierIdentity() throws ReflectiveOperationException
    {
        GunItem item = mock(GunItem.class, CALLS_REAL_METHODS);
        var field = GunItem.class.getDeclaredField("configType");
        field.setAccessible(true);
        field.set(item, read("ShortName testWeapon", "MeleeDamage 9", "MeleeSpeed 1"));
        ItemStack stack = mock(ItemStack.class);
        AtomicReference<CompoundTag> tag = new AtomicReference<>();
        when(stack.getTag()).thenAnswer(ignored -> tag.get());
        doAnswer(invocation ->
        {
            tag.set(invocation.getArgument(0));
            return null;
        }).when(stack).setTag(any());
        var first = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack).get(Attributes.ATTACK_SPEED);
        var second = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack).get(Attributes.ATTACK_SPEED);

        assertEquals(1, first.size());
        assertEquals(-3D, first.iterator().next().getAmount());
        assertEquals(first.iterator().next().getId(), second.iterator().next().getId());
        assertTrue(item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack).get(Attributes.ATTACK_SPEED).isEmpty());

        var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
        when(stack.getAttributeModifiers(EquipmentSlot.MAINHAND)).thenReturn(modifiers);
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY)).thenReturn(Optional.empty());
            assertEquals(20, NpcEquipment.meleeDelay(stack), "Use equipped weapons reads the weapon's melee speed attribute");
            EquippedWeaponProperties properties = mock(EquippedWeaponProperties.class);
            equipment.when(() -> FlansEquipment.getWeaponProperties(stack, ItemStack.EMPTY)).thenReturn(Optional.of(properties));
            when(properties.meleeTime()).thenReturn(10);
            assertEquals(20, NpcEquipment.meleeDelay(stack), "custom melee cannot bypass the weapon's recovery");
            when(properties.meleeTime()).thenReturn(30);
            assertEquals(30, NpcEquipment.meleeDelay(stack), "long custom animations still finish before another attack");
        }
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testWeapon", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
