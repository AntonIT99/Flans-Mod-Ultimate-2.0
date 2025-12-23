package com.flansmodultimate.common.guns.penetration;

import com.flansmodultimate.common.types.BulletType;

public record PenetrationLoss(float loss, Type type)
{
    public enum Type
    {
        PLAYER, ENTITY, BLOCK, DECAY;

        public float getEffectOnDamage(BulletType bulletType)
        {
            return switch (this)
            {
                case BLOCK -> bulletType.getBlockPenetrationEffectOnDamage();
                case DECAY -> bulletType.getPenetrationDecayEffectOnDamage();
                case ENTITY -> bulletType.getEntityPenetrationEffectOnDamage();
                case PLAYER -> bulletType.getPlayerPenetrationEffectOnDamage();
            };
        }
    }
}

