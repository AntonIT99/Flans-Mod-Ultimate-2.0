package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.util.FileUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor
public class ArmorType extends InfoType
{
    public static final float ARMOR_POINT_FACTOR = 25.0F;

    protected String rawArmorItemType = StringUtils.EMPTY;
    @Getter
    protected ArmorItem.Type armorItemType;
    /** The amount of damage to absorb. From 0 to 1. Stacks additively between armour pieces */
    protected double damageReductionDefence;
    protected boolean readDamageReductionDefence;
    /** Value read from Defence / Defense. Default: legacy ratio. Can be forced to vanilla armour points by config. */
    protected double defence;
    protected boolean readDefence;
    /** Value read from OtherDefence / OtherDefense. Always a legacy ratio. */
    protected double otherDefence;
    protected boolean readOtherDefence;
    /** The amount of damage to absorb. From 0 to 1. Stacks additively between armour pieces. For bullet damage specifically. */
    protected double bulletDefence;
    protected boolean readBulletDefence;
    /** Vanilla Minecraft armour points. */
    protected double armorPoints;
    protected boolean readArmorPoints;
    /** How good the armour is at stopping bullets. Same units as bullet penetration. Default 0 to emulate previous behaviour */
    @Getter
    protected float penetrationResistance;
    @Getter
    protected int durability;
    @Getter
    protected int toughness;
    @Getter
    protected int enchantability;
    @Getter
    protected boolean readEnchantability;
    /** Modifier for move speed */
    @Getter
    protected float moveSpeedModifier = 1F;
    /** Modifier for knockback */
    @Getter
    protected float knockbackModifier = 0.2F;
    /** Modifier for jump (jump boost effect every couple of seconds) */
    @Getter
    protected float jumpModifier = 1F;
    /** If true, then the player gets a night vision buff every couple of seconds */
    @Getter
    protected boolean nightVision;
    /** If true, then the player gets a invisiblity buff every couple of seconds */
    @Getter
    protected boolean invisible;
    /** If true, then smoke effects from grenades will have no effect on players wearing this */
    @Getter
    protected boolean smokeProtection;
    /** If ture, the player will not receive fall damage */
    @Getter
    protected boolean negateFallDamage;
    /** If true, the player will not receive fire damage */
    @Getter
    protected boolean fireResistance;
    /** If true, the player can breathe underwater */
    @Getter
    protected boolean waterBreathing;
    /** If true, the player can walk on water */
    @Getter
    protected boolean onWaterWalking;
    /** If true, then the player gets a hunger de-buff every couple of seconds */
    @Getter
    protected boolean hunger;
    /** If true, then the player gets a regeneration buff every couple of seconds */
    @Getter
    protected boolean regeneration;
    /** Map of effects and effect Amplifiers */
    @Getter
    protected List<MobEffectInstance> effects = new ArrayList<>();@Getter
    protected String equipSound = StringUtils.EMPTY;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        rawArmorItemType = readValue("Type", rawArmorItemType, file);
        textureName = readResource("ArmourTexture", textureName, file);
        textureName = readResource("ArmorTexture", textureName, file);
        readDamageReductionDefence = file.hasConfigLine("DamageReduction");
        damageReductionDefence = readValue("DamageReduction", damageReductionDefence, file);
        readDefence = file.hasAnyConfigLine("Defence", "Defense");
        defence = readValue("Defence", defence, file);
        defence = readValue("Defense", defence, file);
        readOtherDefence = file.hasAnyConfigLine("OtherDefence", "OtherDefense");
        otherDefence = readValue("OtherDefence", otherDefence, file);
        otherDefence = readValue("OtherDefense", otherDefence, file);
        readArmorPoints = file.hasAnyConfigLine("ArmorPoints", "DamageReductionAmount");
        armorPoints = readValue("DamageReductionAmount", armorPoints, file);
        armorPoints = readValue("ArmorPoints", armorPoints, file);
        bulletDefence = readValue("BulletDefence", bulletDefence, file);
        readBulletDefence = file.hasConfigLine("BulletDefence");
        enchantability = readValue("Enchantability", enchantability, file);
        readEnchantability = file.hasConfigLine("Enchantability");
        toughness = readValue("Toughness", toughness, file);
        durability = readValue("Durability", durability, file);
        moveSpeedModifier = readValue("MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue("Slowness", moveSpeedModifier, file);
        jumpModifier = readValue("JumpModifier", jumpModifier, file);
        knockbackModifier = readValue("KnockbackReduction", knockbackModifier, file);
        knockbackModifier = readValue("KnockbackModifier", knockbackModifier, file);
        nightVision = readValue("NightVision", nightVision, file);
        invisible = readValue("Invisible", invisible, file);
        invisible = readValue("Playermodel", invisible, file);
        negateFallDamage = readValue("NegateFallDamage", negateFallDamage, file);
        fireResistance = readValue("FireResistance", fireResistance, file);
        waterBreathing = readValue("WaterBreathing", waterBreathing, file);
        waterBreathing = readValue("Submarine", waterBreathing, file);
        smokeProtection = readValue("SmokeProtection", smokeProtection, file);
        onWaterWalking = readValue("OnWaterWalking", onWaterWalking, file);
        hunger = readValue("Hunger", hunger, file);
        regeneration = readValue("Regenerate", regeneration, file);
        equipSound = readSound("EquipSound", equipSound, file);

        addEffects("AddEffect", effects, file, true, false);
        addEffects("AddPotionEffect", effects, file, true, false);
        addEffects("PotionEffect", effects, file, true, false);

        switch (rawArmorItemType.toLowerCase(Locale.ROOT))
        {
            case "helmet", "hat", "head":
                armorItemType = ArmorItem.Type.HELMET;
                break;
            case "chestplate", "chest", "body":
                armorItemType = ArmorItem.Type.CHESTPLATE;
                break;
            case "leggings", "legs", "pants":
                armorItemType = ArmorItem.Type.LEGGINGS;
                break;
            case "boots", "shoes", "feet":
                armorItemType = ArmorItem.Type.BOOTS;
                break;
            default:
                FlansMod.log.error("Armor Type '{}' not recognized! Defaulting to Helmet", rawArmorItemType);
                armorItemType = ArmorItem.Type.HELMET;
                break;
        }
    }

    @Override
    protected String getTexturePath(String textureName)
    {
        return "textures/" + type.getTextureFolderName() + "/" + textureName + (armorItemType != ArmorItem.Type.LEGGINGS ? "_1" : "_2") + FileUtils.PNG_EXTENSION;
    }

    public boolean hasDurability()
    {
        return durability > 0;
    }

    public double getDefence()
    {
        double result = 0.0;

        if (readDamageReductionDefence)
            result = Math.max(result, damageReductionDefence);
        if (!ModCommonConfig.forceDefenseAsModernArmor() && readDefence)
            result = Math.max(result, defence);
        if (readOtherDefence)
            result = Math.max(result, otherDefence);

        return result;
    }

    public double getBulletDefence()
    {
        return readBulletDefence ? bulletDefence : getDefence();
    }

    public int getMinecraftArmorPoints()
    {
        return getMinecraftArmorPoints(ModCommonConfig.forceDefenseAsModernArmor());
    }

    public int getDefaultMinecraftArmorPoints()
    {
        return getMinecraftArmorPoints(false);
    }

    private int getMinecraftArmorPoints(boolean forceDefenseAsModernArmor)
    {
        if (forceDefenseAsModernArmor)
            return Math.max(0, Math.max((int) Math.round(armorPoints), (int) Math.round(defence * ARMOR_POINT_FACTOR)));

        return Math.max(0, (int) Math.round(armorPoints));
    }
}
