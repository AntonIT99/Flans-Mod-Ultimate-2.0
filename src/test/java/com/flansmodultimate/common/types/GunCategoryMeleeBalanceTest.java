package com.flansmodultimate.common.types;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.google.gson.*;
import com.wolffsmod.npcs.combat.EquipmentAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compare shipped melee defaults against the actual vanilla item attributes, including the base 1 damage. */
class GunCategoryMeleeBalanceTest
{
    private static final double EPSILON = 1.0E-5D;
    private static JsonObject categories;

    private record Profile(double damage, double speed)
    {
        double nominalDamagePerSecond()
        {
            return damage * speed;
        }
    }

    @BeforeAll
    static void load() throws ReflectiveOperationException, IOException
    {
        EntityTestBootstrap.initialize();
        try (var reader = new InputStreamReader(Objects.requireNonNull(GunCategoryMeleeBalanceTest.class.getResourceAsStream("/config/gun_categories.json")), StandardCharsets.UTF_8))
        {
            categories = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void ordinaryCategoriesStayWithinVanillaBurstAndSustainedDamageCeilings()
    {
        Profile diamondSword = vanilla(Items.DIAMOND_SWORD);
        Profile netheriteSword = vanilla(Items.NETHERITE_SWORD);
        Profile netheriteAxe = vanilla(Items.NETHERITE_AXE);
        for (var category : categories.entrySet())
        {
            JsonObject properties = category.getValue().getAsJsonObject().getAsJsonObject("properties");
            if (!properties.has("MeleeDamage"))
                continue;
            assertTrue(properties.has("MeleeSpeed"), category.getKey());
            Profile weapon = category(category.getKey());
            double sustainedCeiling = switch (category.getKey())
            {
                case "Lightsaber (Fictional)" -> netheriteSword.nominalDamagePerSecond() * 1.6875D;
                case "Power Melee Weapon (Fictional)", "Powered Hammer (Fictional)" -> netheriteSword.nominalDamagePerSecond() * 1.25D;
                default -> diamondSword.nominalDamagePerSecond();
            };
            double burstCeiling = netheriteAxe.damage() + switch (category.getKey())
            {
                case "Sledgehammer" -> 1D;
                case "Lightsaber (Fictional)", "Powered Hammer (Fictional)" -> 2D;
                default -> 0D;
            };
            assertTrue(weapon.damage() <= burstCeiling + EPSILON, category.getKey());
            assertTrue(weapon.nominalDamagePerSecond() <= sustainedCeiling + EPSILON, category.getKey());
            assertTrue(weapon.speed() > 0D && weapon.speed() <= 2D, category.getKey());
        }
    }

    @Test
    void dedicatedSwordsAndHeavyWeaponsHaveExplicitVanillaBenchmarks()
    {
        assertProfile(vanilla(Items.IRON_SWORD), category("Sword (Generic)"));
        assertProfile(vanilla(Items.IRON_SWORD), category("Cavalry Sabre (Generic)"));
        assertProfile(vanilla(Items.IRON_SWORD), category("Katana (Generic)"));
        assertTrue(category("Short Sword (Generic)").damage() < vanilla(Items.DIAMOND_SWORD).damage());
        Profile netheriteSword = vanilla(Items.NETHERITE_SWORD);
        assertProfile(new Profile(netheriteSword.damage() + 4D, 1.8D), category("Lightsaber (Fictional)"));
        assertProfile(vanilla(Items.NETHERITE_AXE), category("Polearm (Generic)"));
        assertEquals(vanilla(Items.NETHERITE_AXE).damage() + 1D, category("Sledgehammer").damage(), EPSILON);
        assertTrue(category("Sledgehammer").speed() < vanilla(Items.NETHERITE_AXE).speed());
        assertTrue(category("Sledgehammer").nominalDamagePerSecond() < vanilla(Items.NETHERITE_AXE).nominalDamagePerSecond());
        assertTrue(category("Combat Knife").nominalDamagePerSecond() < vanilla(Items.IRON_SWORD).nominalDamagePerSecond());
    }

    @Test
    void materialAndHandlingSeparateKnivesShortSwordsAndOrdinaryIronSwords()
    {
        Profile knife = category("Combat Knife");
        Profile shortSword = category("Short Sword (Generic)");
        Profile sword = category("Sword (Generic)");
        Profile bronzeSword = category("Bronze Sword (Generic)");
        Profile spear = category("Polearm (Generic)");
        assertProfile(new Profile(5D, 1.8D), shortSword);
        assertTrue(spear.damage() > sword.damage());
        assertTrue(sword.damage() > shortSword.damage());
        assertTrue(shortSword.damage() > knife.damage());
        assertTrue(knife.speed() > shortSword.speed());
        assertTrue(shortSword.speed() > sword.speed());
        assertTrue(sword.speed() > spear.speed());
        assertTrue(bronzeSword.damage() < sword.damage());
        assertEquals(sword.speed(), bronzeSword.speed(), EPSILON);
        assertTrue(shortSword.nominalDamagePerSecond() <= vanilla(Items.IRON_SWORD).nominalDamagePerSecond());
    }

    @Test
    void categoryMembershipKeepsWeaponFormsAndMaterialsSeparate()
    {
        assertMember("Short Sword (Generic)", "romangladius");
        assertMember("Sword (Generic)", "spatha");
        assertMember("Bronze Sword (Generic)", "aegeansword");
        assertMember("Cavalry Sabre (Generic)", "shamshir");
    }

    @Test
    void poweredWeaponsTradeMassAndAgilityRatherThanKeepingLegacyUnboundedDamage()
    {
        Profile lightsaber = category("Lightsaber (Fictional)");
        Profile powerSword = category("Power Melee Weapon (Fictional)");
        Profile hammer = category("Powered Hammer (Fictional)");
        assertEquals(hammer.damage(), lightsaber.damage(), EPSILON);
        assertTrue(lightsaber.damage() > powerSword.damage());
        assertTrue(lightsaber.speed() > powerSword.speed());
        assertTrue(powerSword.speed() > hammer.speed());
        assertTrue(hammer.speed() < category("Sledgehammer").speed());
        assertTrue(lightsaber.nominalDamagePerSecond() > vanilla(Items.NETHERITE_SWORD).nominalDamagePerSecond());
    }

    @Test
    void throwingWeaponsKeepTheirMeleeTradeoffs()
    {
        Profile gladius = category("Short Sword (Generic)");
        Profile spear = category("Polearm (Generic)");
        Profile pilum = category("Pilum");
        Profile javelin = category("Hasta Velitaris");
        assertTrue(spear.damage() > gladius.damage());
        assertTrue(spear.speed() < gladius.speed());
        assertTrue(pilum.damage() < spear.damage());
        assertTrue(pilum.speed() < spear.speed());
        assertTrue(javelin.damage() < gladius.damage());
        assertTrue(javelin.speed() < gladius.speed());
        assertProfile(pilum, category("Pilum (Generic)"));
        assertProfile(javelin, category("Javelin (Generic)"));
    }

    private static Profile vanilla(Item item)
    {
        ItemStack stack = new ItemStack(item);
        return new Profile(EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, 1D),
            EquipmentAttributes.value(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED, 4D));
    }

    private static Profile category(String name)
    {
        JsonObject properties = categories.getAsJsonObject(name).getAsJsonObject("properties");
        return new Profile(1D + properties.get("MeleeDamage").getAsDouble(), properties.get("MeleeSpeed").getAsDouble());
    }

    private static void assertProfile(Profile expected, Profile actual)
    {
        assertEquals(expected.damage(), actual.damage(), EPSILON);
        assertEquals(expected.speed(), actual.speed(), EPSILON);
    }

    private static void assertMember(String category, String alias)
    {
        assertTrue(categories.getAsJsonObject(category).getAsJsonArray("items").contains(new JsonPrimitive(alias)));
    }
}
