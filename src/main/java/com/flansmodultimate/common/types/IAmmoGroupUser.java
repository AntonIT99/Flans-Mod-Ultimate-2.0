package com.flansmodultimate.common.types;

import java.util.Set;

/** Implemented by the content types that can pull ammo in with "UseAmmoGroup". */
public interface IAmmoGroupUser
{
    /** The ammo group names declared by this type, in declaration order. */
    Set<String> getAmmoGroups();
}
