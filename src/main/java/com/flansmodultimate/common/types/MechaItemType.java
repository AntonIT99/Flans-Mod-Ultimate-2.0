package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.EnumMechaItemType;
import com.flansmodultimate.common.driveables.EnumMechaToolType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@Getter
@NoArgsConstructor
public class MechaItemType extends InfoType
{
    protected EnumMechaItemType mechaItemType = EnumMechaItemType.NOTHING;
    protected EnumMechaToolType function = EnumMechaToolType.SWORD;
    protected float speed = 1F;
    protected float toolHardness = 1F;
    protected float reach = 1F;
    protected boolean floater;
    protected float speedMultiplier = 1F;
    protected float damageResistance;
    protected String soundEffect = StringUtils.EMPTY;
    protected String detectSound = StringUtils.EMPTY;
    protected float soundTime;
    protected int energyShield;
    protected int lightLevel;
    protected boolean stopMechaFallDamage;
    protected boolean forceBlockFallDamage;
    protected boolean vacuumItems;
    protected boolean refineIron;
    protected boolean autoCoal;
    protected boolean autoRepair;
    protected boolean rocketPack;
    protected boolean diamondDetect;
    protected boolean infiniteAmmo;
    protected boolean forceDark;
    protected boolean wasteCompact;
    protected boolean flameBurst;
    protected float autoRepairAmount = 1F;
    protected float fortuneDiamond = 1F;
    protected float fortuneRedstone = 1F;
    protected float fortuneCoal = 1F;
    protected float fortuneEmerald = 1F;
    protected float fortuneIron = 1F;
    protected float rocketPower = 1F;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        mechaItemType = EnumMechaItemType.parse(readValue("Type", mechaItemType.name(), file));
        function = EnumMechaToolType.parse(readValue("ToolType", function.name(), file));
        speed = Math.max(0F, readValue("Speed", speed, file));
        toolHardness = Math.max(0F, readValue("ToolHardness", toolHardness, file));
        reach = Math.max(0F, readValue("Reach", reach, file));
        autoCoal = readValue("AutoFuel", autoCoal, file);
        damageResistance = readValue("Armour", damageResistance, file);
        fortuneCoal = readValue("CoalMultiplier", fortuneCoal, file);
        diamondDetect = readValue("DiamondDetect", diamondDetect, file);
        fortuneDiamond = readValue("DiamondMultiplier", fortuneDiamond, file);
        fortuneEmerald = readValue("EmeraldMultiplier", fortuneEmerald, file);
        flameBurst = readValue("FlameBurst", flameBurst, file);
        floater = readValue("Floatation", floater, file);
        forceBlockFallDamage = readValue("ForceBlockFallDamage", forceBlockFallDamage, file);
        forceDark = readValue("ForceDark", forceDark, file);
        infiniteAmmo = readValue("InfiniteAmmo", infiniteAmmo, file);
        fortuneIron = readValue("IronMultiplier", fortuneIron, file);
        refineIron = readValue("IronRefine", refineIron, file);
        vacuumItems = readValue("ItemVacuum", vacuumItems, file);
        lightLevel = Math.max(0, Math.min(15, readValue("LightLevel", lightLevel, file)));
        autoRepair = readValue("Nanorepair", autoRepair, file);
        autoRepairAmount = Math.max(0F, readValue("NanorepairAmount", autoRepairAmount, file));
        fortuneRedstone = readValue("RedstoneMultiplier", fortuneRedstone, file);
        rocketPack = readValue("RocketPack", rocketPack, file);
        rocketPower = readValue("RocketPower", rocketPower, file);
        soundEffect = readSound("SoundEffect", soundEffect, file);
        detectSound = readSound("DetectSound", detectSound, file);
        soundTime = Math.max(0F, readValue("SoundTime", soundTime, file));
        speedMultiplier = Math.max(0F, readValue("SpeedMultiplier", speedMultiplier, file));
        stopMechaFallDamage = readValue("StopMechaFallDamage", stopMechaFallDamage, file);
        wasteCompact = readValue("WasteCompact", wasteCompact, file);
        energyShield = Math.max(0, readValue("EnergyShield", energyShield, file));
    }
}
