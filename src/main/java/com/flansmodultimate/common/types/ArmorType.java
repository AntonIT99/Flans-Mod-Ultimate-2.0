package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.DefaultArmor;
import com.flansmodultimate.util.ResourceUtils;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;
import static com.flansmodultimate.util.TypeReaderUtils.readValues;

@NoArgsConstructor
public class ArmorType extends InfoType
{
    protected String rawArmorItemType = StringUtils.EMPTY;
    @Getter
    protected ArmorItem.Type armorItemType;
    /** The amount of damage to absorb. From 0 to 1. Stacks additively between armour pieces */
    @Getter
    protected double defence;
    /** The amount of damage to absorb. From 0 to 1. Stacks additively between armour pieces. For bullet damage specifically. */
    @Getter
    protected double bulletDefence;
    protected boolean readBulletDefence;
    /** How good the armour is at stopping bullets. Same units as bullet penetration. Default 0 to emulate previous behaviour */
    @Getter
    protected float penetrationResistance;
    @Getter
    protected int damageReductionAmount;
    @Getter
    protected int durability;
    @Getter
    protected int toughness;
    @Getter
    protected int enchantability = 10;
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
    protected List<MobEffectInstance> effects = new ArrayList<>();

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);
        rawArmorItemType = readValue(split, "Type", rawArmorItemType, file);
        textureName = ResourceUtils.sanitize(readValue(split, "ArmourTexture", textureName, file));
        textureName = ResourceUtils.sanitize(readValue(split, "ArmorTexture", textureName, file));
        defence = readValue(split, "DamageReduction", defence, file);
        defence = readValue(split, "Defence", defence, file);
        defence = readValue(split, "OtherDefence", defence, file);
        if (split[0].equalsIgnoreCase("BulletDefence"))
        {
            bulletDefence = readValue(split, "BulletDefence", bulletDefence, file);
            readBulletDefence = true;
        }
        enchantability = readValue(split, "Enchantability", enchantability, file);
        toughness = readValue(split, "Toughness", toughness, file);
        durability = readValue(split, "Durability", durability, file);
        damageReductionAmount = readValue(split, "DamageReductionAmount", damageReductionAmount, file);
        moveSpeedModifier = readValue(split, "MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue(split, "Slowness", moveSpeedModifier, file);
        jumpModifier = readValue(split, "JumpModifier", jumpModifier, file);
        knockbackModifier = readValue(split, "KnockbackReduction", knockbackModifier, file);
        knockbackModifier = readValue(split, "KnockbackModifier", knockbackModifier, file);
        nightVision = readValue(split, "NightVision", nightVision, file);
        invisible = readValue(split, "Invisible", invisible, file);
        invisible = readValue(split, "playermodel", invisible, file);
        negateFallDamage = readValue(split, "NegateFallDamage", negateFallDamage, file);
        fireResistance = readValue(split, "FireResistance", fireResistance, file);
        waterBreathing = readValue(split, "WaterBreathing", waterBreathing, file);
        waterBreathing = readValue(split, "submarine", waterBreathing, file);
        smokeProtection = readValue(split, "SmokeProtection", smokeProtection, file);
        onWaterWalking = readValue(split, "OnWaterWalking", onWaterWalking, file);
        hunger = readValue(split, "hunger", hunger, file);
        regeneration = readValue(split, "regenerate", regeneration, file);

        addEffects(readValues(split, "AddEffect", file), effects, line, file, true, false);
        addEffects(readValues(split, "AddPotionEffect", file), effects, line, file, true, false);
        addEffects(readValues(split, "PotionEffect", file), effects, line, file, true, false);
    }

    @Override
    protected void postRead()
    {
        super.postRead();
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
        if (!readBulletDefence)
            bulletDefence = defence;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected String getTexturePath(String textureName)
    {
        return "textures/" + type.getTextureFolderName() + "/" + textureName + (armorItemType != ArmorItem.Type.LEGGINGS ? "_1" : "_2") + ".png";
    }

    @Override
    @Nullable
    protected IModelBase getDefaultModel()
    {
        return new DefaultArmor(armorItemType);
    }

    public boolean hasDurability()
    {
        return durability > 0;
    }
}