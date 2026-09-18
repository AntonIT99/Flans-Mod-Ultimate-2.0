# Compatibility Fallbacks

Read this reference only when a category activates an optional modern system or a
server/configuration switch can restore legacy behavior. Always confirm the current
implementation and defaults; these rules describe the present repository and are
not a substitute for source tracing.

## General method

1. Identify the exact activation condition, precedence, and disable/override path.
2. Record the modern inputs supplied by all matching categories plus the definition.
3. Trace the effective modern output and the legacy field actually consumed on the
   fallback path. Account for unit conversion, clamps, aliases, per-ammo overrides,
   installed parts, and post-processing.
4. Derive a legacy value only when one stable value can represent the same behavior.
   Use current default server coefficients unless the task names another profile.
   Keep sensible precision consistent with neighboring definitions and report the
   calculated full-precision value separately.
5. Validate through the same runtime helper or a focused test where practical. If
   several weapons, rounds, engines, or server settings produce different answers,
   preserve the authored fallback and report that no single synchronized value
   exists.

## Kinetic projectile damage

A positive projectile `Mass`/`MassKg` selects kinetic damage. When mass is absent,
the relevant fixed `Damage` chain is the compatibility path. With mass in grams and
the resolved firing velocity in metres per second, the current default formula is:

```text
damage = newDamageSystemDamageReference
       * 0.001
       * sqrt(massGrams)
       * (massGrams / 9)^(1/6)
       * velocityMetresPerSecond
```

The default reference is `5.0`; read `ModCommonConfig` rather than assuming it has
not changed. Resolve velocity with runtime precedence: per-round/per-ammunition
override, ammunition `MuzzleVelocity`/`BulletSpeed`, firing weapon, then the
deterministic default. Check how gun and ammunition fixed damage combine before
authoring a fallback: copying the formula into both may multiply the result. A shared
round fired at materially different velocities has no single exact `Damage`
fallback.

Do not remove existing target-specific `DamageVs*` values. Reconcile each one only
when the modern path also supplies a defensible target-specific equivalent.

## Explosive-mass system

A positive `ExplosiveMassTNTg`/`ExplosiveMassTNTKg` selects the modern explosion
path. Convert the charge to kilograms and use the current `ExplosionScaling` and
`ModCommonConfig` helpers. At present:

- effective blast damage is
  `newDamageSystemExplosiveDamageReference * cbrt(massKg)`;
- effective power is
  `newDamageSystemExplosivePowerReference * cbrt(massKg)`;
- crater, blast, and fragmentation radii use the piecewise exponents and 5 kg knee
  in `ExplosionScaling`, not a blanket cube-root rule.

For a legacy fallback, use `Explosion`/`ExplosionRadius` for the calculated crater
radius, `BlastRadius` for the calculated blast radius, and `ExplosionPower` for the
calculated power. The parser multiplies legacy `ExplosionDamage*` by
`8 * explosionRadius + 1`, so solve for the authored raw value:

```text
rawLegacyExplosionDamage = effectiveModernDamage / (8 * legacyExplosionRadius + 1)
```

Do not paste the effective damage directly into `ExplosionDamage`. Preserve
target-specific blast values unless each can be converted coherently. When
`FragType` derives fragmentation from explosive mass, use that enum's current
coefficients and `ExplosionScaling.fragRadius` to author `FragDamage`, `FragRadius`,
and `FragIntensity` only if a massless fallback genuinely needs them. Verify that
doing so does not override intentional legacy tuning.

## Real-world vehicle and aircraft physics

`forceLegacyVehiclePhysics` and `forceLegacyPlanePhysics` bypass the real-world
movement profile. Keep the legacy movement/control fields even when category data
completes the modern profile. The modern ground profile replaces propulsion and
terminal speed but retains several gameplay modifiers; the modern aircraft profile
replaces a different set. Use the current wiki's precedence tables to distinguish
replaced from retained fields.

For a ground vehicle on flat terrain, a provisional legacy `MaxThrottle` can be
estimated only when the installed engine speed and traction are known:

```text
desiredBlocksPerTick = RealMaxSpeedKmh / 72 * configuredClassSpeedScale
MaxThrottle = desiredBlocksPerTick / (engineSpeed * legacyFactor * traction)
legacyFactor = 0.26 tracked, 0.32 wheeled
```

This is not a universal conversion: engine parts, water propulsion, damage,
traction, slope, and server scaling alter it. Do not author the result when those
inputs are ambiguous. Reverse speed likewise needs both the intended legacy
`MaxNegativeThrottle` behavior and any independent real reverse-speed override.

There is no single safe algebraic conversion from the full aircraft performance
profile to legacy `Lift`, `MaxThrust`, `Mass`, `MaxSpeed`, and
`NewFlightControl`. Preserve existing aircraft fallbacks unless a focused simulation
or test establishes a coherent legacy set.

Keep collision geometry, wheel data, control-axis speeds, drag, suspension,
`SetupPart` health weights, AA-gun `Health`, and other retained gameplay fields.
For normalized vehicle health, positive authored hitbox health is also the allocation
weight; replacing it with one total value breaks both the fallback and the modern
distribution.
