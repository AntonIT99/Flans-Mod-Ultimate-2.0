package com.flansmodultimate.api;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** A projectile fired by a Flan's weapon. */
public interface IEntityBullet
{
    /**
     * @return the entity that fired the shot, usually a player but sometimes a mob or a driveable's rider
     */
    Optional<LivingEntity> getOwner();

    /**
     * @return the type of this bullet
     */
    IInfoType getBulletInfoType();

    /**
     * @return the type that fired this bullet, such as a gun, an AA gun or a driveable, when known
     */
    Optional<IInfoType> getFiredFrom();
}
