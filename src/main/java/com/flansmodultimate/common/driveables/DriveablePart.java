package com.flansmodultimate.common.driveables;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Mutable, persistable state for one part of a driveable. */
@Getter
public final class DriveablePart
{
    private static final int DEFAULT_FIRE_TICKS = 20;

    private final EnumDriveablePart type;
    @Nullable private final CollisionBox box;
    private final float maxHealth;
    private float health;
    private int fireTime;
    private boolean onFire;
    private boolean dead;

    public DriveablePart(EnumDriveablePart type, @Nullable CollisionBox box)
    {
        this.type = type == null ? EnumDriveablePart.CORE : type;
        this.box = box;
        maxHealth = box == null ? 0F : box.getHealth();
        health = maxHealth;
    }

    public float getPenetrationResistance()
    {
        return box == null ? 0F : box.getPenetrationResistance();
    }

    public boolean isDestroyed()
    {
        return dead || maxHealth > 0F && health <= 0F;
    }

    public void tick()
    {
        if (fireTime > 0)
            --fireTime;
        if (fireTime <= 0)
        {
            fireTime = 0;
            onFire = false;
        }
        else if (onFire)
            damage(1F, false);
    }

    public boolean damage(float amount, boolean fireDamage)
    {
        if (amount > 0F && maxHealth > 0F && !dead)
        {
            health = Math.max(0F, health - amount);
            dead = health <= 0F;
        }
        if (fireDamage && !dead)
        {
            onFire = true;
            fireTime = Math.max(fireTime, DEFAULT_FIRE_TICKS);
        }
        return isDestroyed();
    }

    public float repair(float amount)
    {
        if (amount <= 0F || maxHealth <= 0F)
            return 0F;
        float previous = health;
        health = Mth.clamp(health + amount, 0F, maxHealth);
        if (health > 0F)
            dead = false;
        return health - previous;
    }

    public void extinguish()
    {
        fireTime = 0;
        onFire = false;
    }

    /** Applies an authoritative server snapshot on the client with defensive bounds. */
    public void applyNetworkState(float health, int fireTicks, boolean onFire, boolean dead)
    {
        this.health = Mth.clamp(health, 0F, maxHealth);
        fireTime = Math.max(0, fireTicks);
        this.onFire = onFire && fireTime > 0 && this.health > 0F;
        this.dead = dead || maxHealth > 0F && this.health <= 0F;
    }

    public void setSyncedState(float health, int fireTicks, boolean onFire, boolean dead)
    {
        applyNetworkState(health, fireTicks, onFire, dead);
    }

    public CompoundTag save(CompoundTag tag)
    {
        tag.putString("part", type.getShortName());
        tag.putFloat("health", health);
        tag.putInt("fire_ticks", fireTime);
        tag.putBoolean("on_fire", onFire);
        tag.putBoolean("dead", dead);
        return tag;
    }

    public void saveLegacy(CompoundTag tag)
    {
        tag.putFloat(type.getShortName() + "_Health", health);
        tag.putBoolean(type.getShortName() + "_Fire", onFire);
    }

    public void load(CompoundTag tag)
    {
        if (tag.contains("health"))
        {
            health = Mth.clamp(tag.getFloat("health").orElse(maxHealth), 0F, maxHealth);
            fireTime = Math.max(0, tag.getInt("fire_ticks").orElse(0));
            onFire = tag.getBoolean("on_fire").orElse(false) && fireTime > 0;
            dead = tag.getBoolean("dead").orElse(false) || maxHealth > 0F && health <= 0F;
        }
    }

    public void loadLegacy(CompoundTag tag)
    {
        String healthKey = type.getShortName() + "_Health";
        if (!tag.contains(healthKey))
            return;
        health = Mth.clamp(tag.getFloat(healthKey).orElse(maxHealth), 0F, maxHealth);
        onFire = tag.getBoolean(type.getShortName() + "_Fire").orElse(false);
        fireTime = onFire ? DEFAULT_FIRE_TICKS : 0;
        dead = maxHealth > 0F && health <= 0F;
    }
}
