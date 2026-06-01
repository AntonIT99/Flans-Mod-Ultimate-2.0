package com.flansmodultimate.common.types;

import com.flansmodultimate.util.ResourceUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@NoArgsConstructor
public class AAGunType extends InfoType
{
    @Getter protected String ammo = StringUtils.EMPTY;
    @Getter protected int numBarrels = 1;
    @Getter protected String barrel = StringUtils.EMPTY;
    @Getter protected boolean fireAlternately;
    @Getter protected int reloadTime;
    @Getter protected int health = 32767;
    @Getter protected float recoil;
    @Getter protected float damage;
    @Getter protected float accuracy = 10F;
    @Getter protected boolean shareAmmo;
    @Getter protected boolean targetMobs;
    @Getter protected boolean targetPlayers;
    @Getter protected boolean targetVehicles;
    @Getter protected boolean targetPlanes;
    @Getter protected boolean targetMechas;
    @Getter protected int targetRange = 20;
    @Getter protected int shootDelay;
    @Getter protected String shootSound = StringUtils.EMPTY;
    @Getter protected String reloadSound = StringUtils.EMPTY;
    @Getter protected String gunnerPos = StringUtils.EMPTY;
    @Getter protected float topViewLimit = 60F;
    @Getter protected float bottomViewLimit;
    @Getter protected float sideViewLimit = 180F;
    @Getter protected float standBackDist = 2F;
    @Getter protected float pivotHeight;

    protected List<ShootableType> ammoTypes;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);

        ammo = readResource("Ammo", ammo, file);
        numBarrels = readValue("NumBarrels", numBarrels, file);
        barrel = readValue("Barrel", barrel, file);
        fireAlternately = readValue("FireAlternately", fireAlternately, file);
        reloadTime = readValue("ReloadTime", reloadTime, file);
        health = readValue("Health", health, file);
        recoil = readValue("Recoil", recoil, file);
        damage = readValue("Damage", damage, file);
        accuracy = readValue("Accuracy", accuracy, file);
        shareAmmo = readValue("ShareAmmo", shareAmmo, file);
        targetMobs = readValue("TargetMobs", targetMobs, file);
        targetPlayers = readValue("TargetPlayers", targetPlayers, file);
        targetVehicles = readValue("TargetVehicles", targetVehicles, file);
        targetPlanes = readValue("TargetPlanes", targetPlanes, file);
        targetMechas = readValue("TargetMechas", targetMechas, file);
        targetRange = readValue("TargetRange", targetRange, file);
        shootDelay = readValue("ShootDelay", shootDelay, file);
        shootSound = readResource("ShootSound", shootSound, file);
        reloadSound = readResource("ReloadSound", reloadSound, file);
        gunnerPos = readValue("GunnerPos", gunnerPos, file);
        topViewLimit = readValue("TopViewLimit", topViewLimit, file);
        bottomViewLimit = readValue("BottomViewLimit", bottomViewLimit, file);
        sideViewLimit = readValue("SideViewLimit", sideViewLimit, file);
        standBackDist = readValue("StandBackDist", standBackDist, file);
        pivotHeight = readValue("PivotHeight", pivotHeight, file);
    }

    public List<ShootableType> getAmmoTypes()
    {
        if (ammoTypes == null && StringUtils.isNotBlank(ammo))
        {
            ammoTypes = new ArrayList<>();
            if (InfoType.getInfoType(ammo) instanceof ShootableType shootable)
                ammoTypes.add(shootable);
        }
        return ammoTypes == null ? List.of() : ammoTypes;
    }
}