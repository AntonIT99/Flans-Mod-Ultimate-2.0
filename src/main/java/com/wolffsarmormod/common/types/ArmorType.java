package com.wolffsarmormod.common.types;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.client.model.DefaultArmor;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.wolffsarmormod.util.TypeReaderUtils.readValue;
import static com.wolffsarmormod.util.TypeReaderUtils.readValues;

@NoArgsConstructor
public class ArmorType extends InfoType
{
    protected String rawArmorItemType = StringUtils.EMPTY;
    @Getter
    protected ArmorItem.Type armorItemType;
    /** The amount of damage to absorb. From 0 to 1. Stacks additively between armour pieces */
    @Getter
    protected double defence;
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
    protected boolean nightVision;
    protected boolean invisible;
    protected boolean smokeProtection;
    protected boolean negateFallDamage;
    protected boolean fireResistance;
    protected boolean waterBreathing;
    protected boolean onWaterWalking;
    protected boolean hunger;
    protected boolean regeneration;
    /** Map of effects and effect Amplifiers */
    @Getter
    protected List<MobEffectInstance> effects = new ArrayList<>();

    @Override
    protected void readLine(String line, String[] split, TypeFile file)
    {
        super.readLine(line, split, file);
        rawArmorItemType = readValue(split, "Type", rawArmorItemType, file);
        textureName = readValue(split, "ArmourTexture", textureName, file).toLowerCase();
        textureName = readValue(split, "ArmorTexture", textureName, file).toLowerCase();
        defence = readValue(split, "DamageReduction", defence, file);
        defence = readValue(split, "Defence", defence, file);
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
    }

    @Override
    protected void postRead()
    {
        super.postRead();
        switch (rawArmorItemType.toLowerCase())
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
                ArmorMod.log.error("Armor Type '{}' not recognized! Defaulting to Helmet", rawArmorItemType);
                armorItemType = ArmorItem.Type.HELMET;
                break;
        }
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

    /**
     * If true, then the player gets a night vision buff every couple of seconds
     */
    public boolean hasNightVision()
    {
        return nightVision;
    }

    /**
     * If true, then the player gets a invisiblity buff every couple of seconds
     */
    public boolean hasInvisiblility()
    {
        return invisible;
    }

    /**
     * If true, then smoke effects from grenades will have no effect on players wearing this
     */
    public boolean hasSmokeProtection()
    {
        return smokeProtection;
    }

    /**
     * If ture, the player will not receive fall damage
     */
    public boolean hasNegateFallDamage()
    {
        return negateFallDamage;
    }

    /**
     * If true, the player can walk on water
     */
    public boolean hasOnWaterWalking()
    {
        return onWaterWalking;
    }

    /**
     * If true, the player can breathe underwater
     */
    public boolean hasWaterBreathing()
    {
        return waterBreathing;
    }

    /**
     * If true, the player will not receive fire damage
     */
    public boolean hasFireResistance()
    {
        return fireResistance;
    }

    /**
     * If true, then the player gets a hunger de-buff every couple of seconds
     */
    public boolean hasHunger()
    {
        return hunger;
    }

    /**
     * If true, then the player gets a regeneration buff every couple of seconds
     */
    public boolean hasRegeneration()
    {
        return regeneration;
    }
}