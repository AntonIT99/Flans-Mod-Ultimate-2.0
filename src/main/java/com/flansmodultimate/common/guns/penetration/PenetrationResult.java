package com.flansmodultimate.common.guns.penetration;

/** Result of applying a physical penetration value to one resolved armour hit. */
public record PenetrationResult(boolean penetrated, boolean armourGateRequired, float penetrationMm, float effectiveArmorMm, float overmatch) {}
