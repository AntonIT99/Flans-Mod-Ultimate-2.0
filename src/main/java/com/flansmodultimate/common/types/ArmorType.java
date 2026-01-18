package com.flansmodultimate.common.types;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.DefaultArmor;
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

@NoArgsConstructor
public class ArmorType extends InfoType
{
    public static final float ARMOR_POINT_FACTOR = 25.0F;

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
        defence = readValue("DamageReduction", defence, file);
        defence = readValue("Defence", defence, file);
        defence = readValue("Defense", defence, file);
        defence = readValue("OtherDefence", defence, file);
        defence = readValue("OtherDefense", defence, file);
        defence = Math.max(readValue("DamageReductionAmount", 0, file) / ARMOR_POINT_FACTOR, defence);
        bulletDefence = readValue("BulletDefence", bulletDefence, file);
        readBulletDefence = file.hasConfigLine("BulletDefence");
        enchantability = readValue("Enchantability", enchantability, file);
        readEnchantability = file.hasConfigLine("Enchantability");
        toughness = readValue("Toughness", toughness, file);
        durability = readValue("Durability", durability, file);
        moveSpeedModifier = readValue("MoveSpeedModifier", moveSpeedModifier, file);
        moveSpeedModifier = readValue("Slowness", moveSpeedModifier, file);
        jumpModifier = Math.min(readValue("JumpModifier", jumpModifier, file), 1F);
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
    public IModelBase getDefaultModel()
    {
        return new DefaultArmor(armorItemType);
    }

    public boolean hasDurability()
    {
        return durability > 0;
    }
}