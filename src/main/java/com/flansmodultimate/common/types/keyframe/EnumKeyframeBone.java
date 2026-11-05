package com.flansmodultimate.common.types.keyframe;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Gun parts a keyframe animation can move, named by their bone in the animation file. */
@Getter
@AllArgsConstructor
public enum EnumKeyframeBone
{
    GUN("gun"),
    AMMO("ammo"),
    EXTRA_AMMO("extra_ammo"),
    PARTIAL_AMMO("partial_ammo"),
    REVOLVER_BARREL("revolver_barrel"),
    ATTACHMENT_SCOPE("attachment_scope"),
    ATTACHMENT_GRIP("attachment_grip"),
    ATTACHMENT_AMMO("attachment_ammo"),
    BREAK_ACTION("break_action"),
    HAMMER("hammer"),
    SLIDE("slide"),
    PUMP("pump"),
    BOLT("bolt"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm");

    private final String boneName;
}
