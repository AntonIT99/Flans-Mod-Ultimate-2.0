package com.flansmodultimate.common.types;

import com.flansmodultimate.common.raytracing.EnumHitboxType;

/**
 * The body zones that worn armour can protect on its own rating, whatever equipment slot the piece is worn in. A plate suit
 * worn as boots still covers the chest and arms when its definition says so.
 */
public enum ArmorZone
{
    /** The top of the head, above the eyes */
    HEAD,
    /** The face, below the eyes and facing the shooter */
    FACE,
    /** The back of the neck, below the eyes and facing away from the shooter */
    NAPE,
    BODY,
    BACK,
    ARM,
    LEG;

    /**
     * @param hitbox     the struck hitbox
     * @param fromBehind whether the round arrived from behind the target
     * @param belowEyes  whether the round struck the head below eye level
     * @return the zone the hit lands on, or null for hitboxes that are not body parts
     */
    public static ArmorZone of(EnumHitboxType hitbox, boolean fromBehind, boolean belowEyes)
    {
        return switch (hitbox)
        {
            case HEAD -> belowEyes ? (fromBehind ? NAPE : FACE) : HEAD;
            case BODY -> fromBehind ? BACK : BODY;
            case LEFTARM, RIGHTARM -> ARM;
            case LEGS -> LEG;
            default -> null;
        };
    }
}
