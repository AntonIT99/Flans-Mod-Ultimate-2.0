package com.wolffsarmormod.common.guns;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.common.types.InfoType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class FlanDamageSource extends DamageSource
{
    /** The weapon (InfoType) used to cause this damage */
    @Getter
    private InfoType weapon;
    @Getter
    private Player shooter;
    /** True if this is a headshot, false if not */
    @Getter
    private boolean headshot;
    /**
     * @param s        Name of the damage source (Usually the shortName of the gun)
     * @param entity   The Entity causing the damage (e.g. Grenade). Can be the same as 'player'
     * @param player   The Player responsible for the damage
     * @param wep      The InfoType of weapon used
     */
    public FlanDamageSource(String s, @Nullable Entity entity, @Nullable Player player, InfoType wep)
    {
        this(s, entity, player, wep, false);
    }

    /**
     * @param s        Name of the damage source (Usually the shortName of the gun)
     * @param entity   The Entity causing the damage (e.g. Grenade). Can be the same as 'player'
     * @param player   The Player responsible for the damage
     * @param wep      The InfoType of weapon used
     * @param headshot True if this was a headshot, false if not
     */
    public FlanDamageSource(String s, @Nullable Entity entity, @Nullable Player player, InfoType wep, boolean headshot)
    {
        super(s, entity, player);
        weapon = wep;
        shooter = player;
        this.headshot = headshot;
    }

    @Override
    public Component getLocalizedDeathMessage(@NotNull LivingEntity victim)
    {
        if (!(victim instanceof Player) || shooter == null || weapon == null)
        {
            return Component.translatable("death.attack." + ArmorMod.MOD_ID + ".noshooter", victim.getDisplayName());
        }

        Component weaponName = Component.translatable("item." + ArmorMod.FLANSMOD_ID + "." + weapon.getShortName());

        if (headshot)
        {
            return Component.translatable("death.attack." + ArmorMod.MOD_ID + ".headshot", victim.getDisplayName(), shooter.getDisplayName(), weaponName);
        }
        else
        {
            return Component.translatable("death.attack." + ArmorMod.MOD_ID, victim.getDisplayName(), shooter.getDisplayName(), weaponName);
        }
    }

    @Override
    @Nullable
    public Vec3 getSourcePosition()
    {
        Entity direct = this.getDirectEntity();
        return (direct != null) ? direct.position() : new Vec3(0D, 0D, 0D);
    }
}
