# Config Reference

This page documents the legacy content-pack `.txt` parameters read by the classes in `com.flansmodultimate.common.types`.

The source of truth for this page is the current Java parser code. Some legacy keys are aliases for the same internal field; those aliases are shown together.

## Parser Rules

- Keywords are case-insensitive.
- Most scalar values use `Key value`. `Key = value` is also accepted for scalar reads.
- Boolean values accept `true`, `false`, `1`, or `0`.
- Repeated scalar keys usually use the last non-empty value.
- Repeated line-list keys, such as `Ammo`, `Paintjob`, `AddRound`, `AddGun`, and `AddAmmo`, are accumulated.
- Resource names are sanitized before use. Prefer lower-case names without spaces.
- Unless a table says otherwise, times are in ticks.
- `~` means unset, empty, or not configured.
- Vector keys read with `readVector` should be written as bracket vectors without spaces, for example `[1,2,3]`. Keys that explicitly say `float float float` use whitespace-separated values.

## Type Inheritance

Concrete type files inherit every parameter from their parent type.

```text
InfoType
|-- PaintableType
|   |-- AttachmentType
|   |-- GunType
|   `-- DriveableType (not currently registered in EnumType)
|-- ShootableType
|   |-- BulletType
|   `-- GrenadeType
|-- BlockType
|   |-- ArmorBoxType
|   `-- GunBoxType
|-- AAGunType
|-- ArmorType
|-- GloveType
|-- ItemHolderType
|-- PartType
`-- ToolType
```

| Config folder | Class | Inherits |
| --- | --- | --- |
| `aaguns` | `AAGunType` | `InfoType` |
| `armorFiles` | `ArmorType` | `InfoType` |
| `armorBoxes` | `ArmorBoxType` | `BlockType`, `InfoType` |
| `attachments` | `AttachmentType` | `PaintableType`, `InfoType` |
| `bullets` | `BulletType` | `ShootableType`, `InfoType` |
| `gloves` | `GloveType` | `InfoType` |
| `grenades` | `GrenadeType` | `ShootableType`, `InfoType` |
| `guns` | `GunType` | `PaintableType`, `InfoType` |
| `boxes` | `GunBoxType` | `BlockType`, `InfoType` |
| `itemHolders` | `ItemHolderType` | `InfoType` |
| `parts` | `PartType` | `InfoType` |
| `tools` | `ToolType` | `InfoType` |

## Common Formats

| Format | Meaning |
| --- | --- |
| `string` | One token. Spaces are not preserved unless the key uses a text-line reader. |
| `text line` | The full text after the key. Used by `Name` and `Description`. |
| `resource` | Sanitized content-pack resource or short name. |
| `sound` | Sanitized sound id registered by the mod. |
| `bool` | `true`, `false`, `1`, or `0`. |
| `enum` | One of the enum values listed in the table description. |
| `vector` | Bracket vector, for example `[0,1,2]`. |
| `effect line` | `<effectId> [duration] [amplifier] [ambient] [visible]`. Duration defaults to `250`, amplifier to `0`; ambient/visible defaults depend on the caller. |
| `amount/item pairs` | `<amount> <item> [<amount> <item> ...]`. |
| `item/amount pairs` | `<item> <amount> [<item> <amount> ...]`. |

## `InfoType`

Inherited by every concrete type.

### Identity, Assets, And Rendering

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Name` | text line | `""` | Display name read from the full remainder of the line. |
| `ShortName` | resource | `~` | Content-pack id for this type entry. |
| `Description` | text line | `""` | Description text. |
| `Icon` | resource | `~` | Icon texture/item name used by generated items and paintjobs. |
| `Texture` | resource | `~` | Main skin or item texture name. |
| `Overlay` | resource | `~` | GUI/scope overlay texture. |
| `Model` | string | `~` | Legacy model class name stem. |
| `ModelScale` | float | `1.0` | Scale applied to the model. |
| `TranslucentRendering` | bool | `false` | Enables translucent rendering mode. |
| `AdditiveBlending` | bool | `false` | Enables additive blending for rendering. |
| `Colour` / `Color` | `int int int` | `255 255 255` | RGB color packed into the internal color field. |

### Loot, Recipes, And Drops

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `DungeonProbability` / `DungeonLootChance` | int | `1` | Relative chance for dungeon loot handling. |
| `RecipeOutput` | int | `1` | Output stack count for generated crafting recipes. |
| `SmeltableFrom` | resource | `~` | Item id used to generate a smelting recipe. |
| `CanDrop` | bool | `true` | Whether the item can be dropped. |
| `Recipe` | `Recipe <char> <item> ...`, followed by 3 shape rows | none | Defines a shaped crafting recipe. Key tokens are character/item pairs; the next 3 lines are the grid. |
| `ShapelessRecipe` | `<item> [item ...]` | none | Defines a shapeless crafting recipe. |

## `PaintableType`

Inherited by `AttachmentType`, `GunType`, and the currently unregistered `DriveableType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Paintjob` | `<icon> <texture> [<dye> <amount> ...]` | default paintjob from `Icon`/`Texture` | Adds a paintjob. Dye `rainbow` marks the paintjob as legendary. |
| `AdvPaintJob` | `<name> <icon> <texture> [<dye> <amount> ...]` | none | Adds a named paintjob. |
| `AddPaintableToTables` | `<bool>` or `<texture> <bool>` | `true` | Enables/disables all paintjobs for paint tables, or toggles a specific paintjob texture. |

## `BlockType`

Inherited by `ArmorBoxType` and `GunBoxType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `TopTexture` | resource | `~` | Top block texture. |
| `BottomTexture` | resource | `~` | Bottom block texture. |
| `SideTexture` | resource | `~` | Side block texture. |

## `ShootableType`

Inherited by `BulletType` and `GrenadeType`.

### Item And Ammo Mapping

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `AddAmmoFor` | resource line, repeatable | none | Adds this shootable as extra ammo for the named gun or AA gun short name. |
| `StackSize` / `MaxStackSize` | int | `1` | Maximum item stack size. |
| `DropItemOnShoot` | resource | `~` | Item to drop when fired. |
| `DropItemOnReload` | resource | `~` | Item to drop when reloaded. |
| `DropItemOnHit` | resource | `~` | Item to drop when the projectile hits. |
| `RoundsPerItem` | int | `0` | Number of rounds contained in one ammo item. |
| `NumBullets` | int | `0` | Ammo-side bullet count override. `0` defers to the gun. |

### Physics And Direct Hit Damage

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Accuracy` / `Spread` | float | `-1.0` | Ammo-side spread value or multiplier. |
| `Dispersion` | float degrees | unset | Alternative angular spread input. Converted internally. |
| `FallSpeed` | float | `1.0` | Gravity/fall speed multiplier. |
| `ThrowSpeed` / `ShootSpeed` | float | `1.0` | Thrown/projectile launch speed. |
| `HitBoxSize` | float | `0.5` | Projectile hitbox size. |
| `Mass` | float grams | `0.0` | Enables kinetic damage when greater than `0`. |
| `Damage` / `DamageVsEntity` / `HitEntityDamage` | float | `1.0` | Base direct-hit damage. |
| `DamageVsLiving` | float | inherits | Direct-hit damage against living entities. |
| `DamageVsPlayer` / `DamageVsPlayers` | float | inherits | Direct-hit damage against players. |
| `DamageVsVehicle` / `DamageVsVehicles` / `DamageVsDrivable` / `DamageVsDrivables` | float | inherits | Direct-hit damage against driveables/vehicles. |
| `DamageVsPlane` / `DamageVsPlanes` | float | inherits | Direct-hit damage against planes. |
| `IgnoreArmorProbability` | float | `0.0` | Chance or factor used by armor ignoring logic. |
| `IgnoreArmorDamageFactor` | float | `0.0` | Damage factor used when armor is ignored. |
| `BreaksGlass` | bool | `false` | Whether impact can break glass. |
| `Bounciness` | float | `0.0` | Motion multiplier after impact. `GrenadeType` changes the absent-key default to `0.9`. |
| `HasLight` | bool | `false` | Full-bright projectile rendering. |
| `HasDynamicLight` | bool | `false` | Dynamic light around the projectile. |

Damage fallback after reading:

- If base `Damage` is absent, it is derived from the first available specific value in this order: living, vehicles, players, planes.
- Missing `DamageVsLiving` falls back to `Damage`.
- Missing `DamageVsPlayer` falls back to `DamageVsLiving`.
- Missing `DamageVsVehicle` falls back to `Damage`.
- Missing `DamageVsPlane` falls back to `DamageVsVehicle`.

### Detonation

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Fuse` | int | `0` | Detonation delay. `0` means no fuse condition. |
| `DespawnTime` | int | `0` | Quiet despawn time. `0` means no despawn timeout. |
| `ExplodeOnImpact` / `DetonateOnImpact` | bool | `false` | Detonate on hit. |
| `LivingProximityTrigger` | float | `-1.0` | Mine/proximity trigger radius for living entities. |
| `VehicleProximityTrigger` | float | `-1.0` | Mine/proximity trigger radius for driveables. |
| `DamageToTriggerer` | float | `0.0` | Damage dealt to the triggering entity. |
| `PrimeDelay` / `TriggerDelay` | int | `0` | Minimum delay before detonation can occur. |
| `FireRadius` / `Fire` | float | `0.0` | Radius for spreading fire. |
| `ExplosionBreaksBlocks` / `ExplosionsBreaksBlocks` / `ExplosionBreakBlocks` / `ExplosionsBreakBlocks` | bool | `true` | Whether the explosion can break blocks. |
| `ExplosiveMass` | float kg TNT equivalent | `0.0` | Enables the new explosion system when greater than `0`. |
| `ExplosionRadius` / `Explosion` | float | `0.0` | Explosion radius. |
| `ExplosionPower` | float | `1.0` | Explosion power multiplier. |
| `BlastRadius` | float | `0.0` | Blast radius. Also currently copied into `FragRadius`. |
| `FragIntensity` | float | `0.0` | Fragment intensity. |
| `FragType` | enum | `DEFAULT` | Fragment preset: `DEFAULT`, `LOW_FRAG`, `STD_FRAG`, `HIGH_FRAG`, `IED_SHRAPNEL`, `HE_SHELL`, `GP_BOMB`, `THICK_CASE`, `AIRBURST_AP`. |
| `DropItemOnDetonate` | resource | `~` | Item to drop when detonating. |
| `DetonateSound` | string | `""` | Detonation sound name. |

### Explosion Damage

The same damage inheritance rules used for direct-hit damage also apply to blast and frag damage groups.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `BlastDamage` / `ExplosionDamage` / `ExplosionDamageVsEntity` | float | `1.0`, then scaled | Base blast damage. The loaded blast stats are scaled by `8 * ExplosionRadius + 1`. |
| `ExplosionDamageVsLiving` | float | inherits | Blast damage against living entities. |
| `ExplosionDamageVsPlayer` / `ExplosionDamageVsPlayers` | float | inherits | Blast damage against players. |
| `ExplosionDamageVsVehicle` / `ExplosionDamageVsVehicles` / `ExplosionDamageVsDrivable` / `ExplosionDamageVsDrivables` | float | inherits | Blast damage against driveables/vehicles. |
| `ExplosionDamageVsPlane` / `ExplosionDamageVsPlanes` | float | inherits | Blast damage against planes. |
| `FragDamage` / `FragDamageVsEntity` | float | `1.0` | Base fragment damage. |
| `FragDamageVsLiving` | float | inherits | Fragment damage against living entities. |
| `FragDamageVsPlayer` / `FragDamageVsPlayers` | float | inherits | Fragment damage against players. |
| `FragDamageVsVehicle` / `FragDamageVsVehicles` / `FragDamageVsDrivable` / `FragDamageVsDrivables` | float | inherits | Fragment damage against driveables/vehicles. |
| `FragDamageVsPlane` / `FragDamageVsPlanes` | float | inherits | Fragment damage against planes. |

### Particles

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `FlareParticleCount` | int | `0` | Smoke/flare particle count. |
| `DebrisParticleCount` | int | `0` | Debris particle count. |
| `TrailParticles` / `SmokeTrail` | bool | `false` | Enables projectile trail particles. |
| `TrailParticleType` | string | `smoke` | Trail particle type id. |
| `NumExplodeParticles` | int | `0` | Number of detonation particles. |
| `ExplodeParticles` | string | `largesmoke` | Detonation particle type id. |

## `AAGunType`

Inherits `InfoType`.

### Combat

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Damage` | float | `0.0` | Multiplier applied to ammo damage for display and use. |
| `ReloadTime` | int | `0` | Reload time. |
| `Recoil` | float | `5.0` | Recoil value. |
| `Accuracy` / `Spread` | float | `0.0` | Barrel/projectile spread. |
| `Dispersion` | float degrees | unset | Alternative angular spread input. Converted internally. |
| `SpreadPattern` | enum | `CIRCLE` | Spread pattern: `CIRCLE`, `CUBE`, `TRIANGLE`, `HORIZONTAL`, `VERTICAL`. |
| `ShootDelay` | int | `0` | Delay between shots. |
| `NumBullets` | int | `1` | Number of projectiles per shot. |
| `Ammo` | resource line, repeatable | none | Allowed ammo short name. |
| `Health` | int | `0` | Entity health. |
| `CanShootHomingMissile` | bool | `false` | Allows homing missile behavior. |
| `CountExplodeAfterShoot` | int | `-1` | Counter for explode-after-shoot behavior. |
| `IsDropThis` | bool | `true` | Whether this AA gun drops itself. |

### Barrels And Positioning

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `NumBarrels` | int | `1` | Number of barrels, clamped to `1..16`. |
| `Barrel` | `<id> <x> <y> <z>` | all `0` | Barrel origin for the given barrel id. |
| `GunnerPos` | `<x> <y> <z>` | `0 0 0` | Gunner position. |
| `TopViewLimit` | float | `75.0` | Upward view limit. |
| `BottomViewLimit` | float | `0.0` | Downward view limit. |
| `SideViewLimit` | float | `180.0` | Horizontal view limit. |
| `FireAlternately` | bool | `false` | Alternates fire between barrels. |
| `ShareAmmo` | bool | `false` | All barrels share one ammo slot when true. |

### Sentry Targeting And Sounds

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `TargetMobs` | bool | `false` | Enables mob targeting. |
| `TargetPlayers` | bool | `false` | Enables player targeting. |
| `TargetVehicles` | bool | `false` | Enables vehicle targeting. |
| `TargetPlanes` | bool | `false` | Enables plane targeting. |
| `TargetMechas` | bool | `false` | Enables mecha targeting. |
| `TargetDriveables` | bool | `false` | Alias that sets vehicles, planes, and mechas together. |
| `TargetRange` | float | `10.0` | Sentry targeting radius. |
| `ShootSound` | sound | `""` | Shooting sound. |
| `ReloadSound` | sound | `""` | Reload sound. |
| `SoundLength` / `ShootSoundLength` | int | `0` | Shoot sound length. |
| `GunSoundRange` | int | `-1` | Gunshot sound range. `-1` uses common config. |
| `ReloadSoundRange` | int | `-1` | Reload sound range. `-1` uses common config. |

## `ArmorType`

Inherits `InfoType`.

### Armor Identity And Texture

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Type` | enum/synonym | helmet if missing or invalid | Armor slot. Accepted groups: `helmet`, `hat`, `head`; `chestplate`, `chest`, `body`; `leggings`, `legs`, `pants`; `boots`, `shoes`, `feet`. |
| `ArmourTexture` / `ArmorTexture` | resource | inherited `Texture` | Armor texture name. The runtime appends `_1` or `_2` depending on the slot. |

### Defense And Durability

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `DamageReduction` | double | `0.0` | Legacy ratio-based damage reduction. |
| `Defence` / `Defense` | double | `0.0` | Legacy ratio by default. Can be treated as armor points by common config. |
| `OtherDefence` / `OtherDefense` | double | `0.0` | Always legacy ratio-based reduction. |
| `BulletDefence` | double | inherits effective defense | Bullet-specific legacy defense. |
| `DamageReductionAmount` / `ArmorPoints` | double | `0.0` | Vanilla Minecraft armor points. |
| `Toughness` | int | `0` | Vanilla armor toughness. |
| `Durability` | int | `0` | Item durability. `0` means no durability. |
| `Enchantability` | int | `0` | Enchantability. If absent, config defaults may be used elsewhere. |

### Modifiers And Effects

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `MoveSpeedModifier` / `Slowness` | float | `1.0` | Movement speed multiplier. |
| `JumpModifier` | float | `1.0` | Jump modifier. |
| `KnockbackReduction` / `KnockbackModifier` | float | `0.2` | Knockback modifier/resistance value. |
| `NightVision` | bool | `false` | Applies night vision behavior. |
| `Invisible` / `Playermodel` | bool | `false` | Invisibility/player-model visibility behavior. |
| `NegateFallDamage` | bool | `false` | Prevents fall damage. |
| `FireResistance` | bool | `false` | Prevents fire damage. |
| `WaterBreathing` / `Submarine` | bool | `false` | Allows breathing underwater. |
| `SmokeProtection` | bool | `false` | Protects from smoke grenade effects. |
| `OnWaterWalking` | bool | `false` | Allows walking on water. |
| `Hunger` | bool | `false` | Applies hunger debuff behavior. |
| `Regenerate` | bool | `false` | Applies regeneration behavior. |
| `EquipSound` | sound | `""` | Sound played when equipped. |
| `AddEffect` / `AddPotionEffect` / `PotionEffect` | effect line | none | Potion effects applied by the armor. Ambient defaults to `true`, visible defaults to `false`. |

## `AttachmentType`

Inherits `PaintableType` and `InfoType`.

### Attachment Role And Function

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `AttachmentType` | enum | `GENERIC` | Attachment slot: `BARREL`, `SIGHTS`, `STOCK`, `GRIP`, `GADGET`, `SLIDE`, `PUMP`, `ACCESSORY`, `GENERIC`. |
| `Silencer` | bool | `false` | Muffles gunshot sounds. |
| `DisableMuzzleFlash` / `DisableFlash` | bool | `false` | Disables muzzle flash model rendering. |
| `Flashlight` | bool | `false` | Enables flashlight behavior. |
| `FlashlightRange` | float | `10.0` | Flashlight range. |
| `FlashlightStrength` | int | `12` | Flashlight strength, usually `0..15`. |
| `ModeOverride` | fire mode | `~` | Overrides the gun fire mode. Values: `SEMIAUTO`, `FULLAUTO`, `MINIGUN`, `BURST`. |

### Gun Modifiers

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `MeleeDamageMultiplier` | float | `1.0` | Multiplies melee damage. |
| `DamageMultiplier` | float | `1.0` | Multiplies gun damage. |
| `SpreadMultiplier` | float | `1.0` | Multiplies spread. |
| `RecoilMultiplier` | float | `1.0` | Multiplies recoil. |
| `RecoilControlMultiplier` | float | `1.0` | Multiplies recoil return-to-center force. Lower is stronger control. |
| `RecoilControlMultiplierSneaking` | float | `1.0` | Sneaking recoil-control multiplier. |
| `RecoilControlMultiplierSprinting` | float | `1.0` | Sprinting recoil-control multiplier. |
| `BulletSpeedMultiplier` | float | `1.0` | Multiplies bullet speed. |
| `ShootDelayMultiplier` | float | `1.0` | Multiplies shoot delay. |
| `MovementSpeedMultiplier` / `MoveSpeedModifier` | float | `1.0` | Multiplies player movement speed while used. |

### Secondary Fire

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `SecondaryMode` | bool | `false` | Enables secondary/underbarrel fire. |
| `SecondaryAmmo` | resource | `~` | Ammo short name for secondary fire. |
| `SecondaryDamage` | float | `1.0` | Secondary fire damage. |
| `SecondarySpread` / `SecondaryAccuracy` | float | `1.0` | Secondary fire spread. |
| `SecondaryBulletSpeed` | float | `5.0` | Secondary projectile speed. |
| `SecondaryShootDelay` | int | `1` | Secondary shoot delay. |
| `SecondaryReloadTime` | int | `1` | Secondary reload time. |
| `SecondaryNumBullets` | int | `1` | Secondary projectile count. |
| `LoadSecondaryIntoGun` | int | `1` | Secondary ammo stacks loaded into the gun. |
| `SecondaryFireMode` | fire mode | `SEMIAUTO` | Secondary fire mode. |
| `SecondaryShootSound` | sound | `~` | Secondary shoot sound. |
| `SecondaryReloadSound` | sound | `~` | Secondary reload sound. |
| `ModeSwitchSound` | sound | `~` | Sound played when toggling modes. |

### Scope

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `MinZoom` | float | `1.0` | Minimum variable zoom. |
| `MaxZoom` | float | `4.0` | Maximum variable zoom. |
| `ZoomAugment` | float | `1.0` | Zoom step/change amount. |
| `ZoomLevel` | float | `1.0` | Scope zoom level. |
| `FOVZoomLevel` | float | `1.0` | FOV zoom level. |
| `ZoomOverlay` | resource | inherited `Overlay` | Scope overlay texture. Value `none` clears it. |
| `HasNightVision` | bool | `false` | Gives night vision while scoped. |

## `BulletType`

Inherits `ShootableType` and `InfoType`. Bullet files also reread `Accuracy`, `Spread`, and `Dispersion` using the same meanings as `ShootableType`.

### Projectile And Hit Behavior

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `BulletSpeed` | float | `0.0` | Bullet speed override. Runtime fallback may use `3.0` when a default speed is required. |
| `MuzzleVelocity` | float m/s | `0.0` | Alternative speed input. Converted to blocks/tick by dividing by `20`. |
| `BulletSpeedMultiplier` | float | `1.0` | Multiplies projectile speed. |
| `PenetrationAt100m` | float | `0.0` | Penetration value at 100m. Currently marked TODO for usage. |
| `FlakParticles` | int | `0` | Number of flak particles on explosion. |
| `FlakParticleType` | string | `largesmoke` | Flak particle type. |
| `SetEntitiesOnFire` | bool | `false` | Sets hit entities on fire. |
| `HitSoundEnable` | bool | `false` | Enables hit sound. |
| `EntityHitSoundEnable` | bool | `false` | Enables entity-hit sound. |
| `HitSound` | sound | `~` | Hit sound. Blank `HitSound` lines are ignored. |
| `HitSoundRange` | float | `64.0` | Hit sound range. |
| `DragInAir` | float | `0.99` | Air drag, clamped to `0..1`. |
| `DragInWater` | float | `0.8` | Water drag, clamped to `0..1`. |
| `TrailTexture` | resource | `defaultbullettrail` if blank | Bullet trail texture. |
| `BlockHitFXScale` | float | computed | Block-hit particle velocity scale. If absent, computed from explosion radius. |
| `AddPotionEffect` / `PotionEffect` | effect line | none | Potion effects applied on hit. Ambient defaults to `false`, visible defaults to `false`. |

### Penetration

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Penetrates` | bool | `true` | Enables penetration. If set false, penetrating power is forced to `0.7`. |
| `Penetration` / `PenetratingPower` | float | `1.0` | Penetrating power. |
| `PenetrationDecay` | float | `0.0` | Penetration lost per tick. |
| `BlockPenetrationModifier` | float | `-1.0` | Block penetration modifier. |
| `PlayerPenetrationDamageEffect` | float | `0.0` | How player penetration loss affects damage. |
| `EntityPenetrationDamageEffect` | float | `0.0` | How entity penetration loss affects damage. |
| `BlockPenetrationDamageEffect` | float | `0.0` | How block penetration loss affects damage. |
| `PenetrationDecayDamageEffect` | float | `0.0` | How decay affects damage. |

### Guidance, Lock-On, And Driveable Use

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Bomb` | optional bool | `false` | Sets `WeaponType` to `BOMB`. A key with no value counts as true. |
| `Shell` | optional bool | `false` | Sets `WeaponType` to `SHELL`. A key with no value counts as true. |
| `Missile` | optional bool | `false` | Sets `WeaponType` to `MISSILE`. A key with no value counts as true. |
| `WeaponType` | enum | `GUN` | One of `MISSILE`, `BOMB`, `SHELL`, `MINE`, `GUN`. Overrides the legacy bool keys. |
| `LockOnToDriveables` | bool | `false` | Sets plane, vehicle, and mecha lock-on flags together. |
| `LockOnToVehicles` | bool | `false` | Lock on to vehicles. |
| `LockOnToPlanes` | bool | `false` | Lock on to planes. |
| `LockOnToMechas` | bool | `false` | Lock on to mechas. |
| `LockOnToPlayers` | bool | `false` | Lock on to players. |
| `LockOnToLivings` | bool | `false` | Lock on to living entities. |
| `MaxLockOnAngle` | float | `45.0` | Maximum lock-on acquisition angle. |
| `LockOnForce` / `TurningForce` | float | `1.0` | Homing turn force. |
| `MaxDegreeOfLockOnMissile` | int | `20` | Maximum lock-on missile turn degree. |
| `TickStartHoming` | int | `5` | Tick when homing begins. |
| `EnableSACLOS` | bool | `false` | Enables SACLOS guidance. |
| `MaxDegreeOFSACLOS` | int | `5` | Maximum SACLOS turn degree. |
| `MaxRangeOfMissile` | int | `256` | Maximum missile range. |
| `CanSpotEntityDriveable` | bool | `false` | Enables spotting driveable entities. |
| `ShootForSettingPos` | bool | `false` | Uses a configured/top-attack position behavior. |
| `ShootForSettingPosHeight` | int | `100` | Height used by setting-position shots. |
| `IsDoTopAttack` | bool | `false` | Enables top-attack behavior. |
| `ManualGuidance` | bool | `false` | Enables manual guidance. |
| `LaserGuidance` | bool | `false` | Enables laser guidance. |
| `MaxRange` | int | `-1` | Maximum projectile range. `-1` means unset. |
| `KnockbackModifier` | float | `0.0` | Bullet knockback modifier. |

### VLS, Dead Zone, And Submunitions

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `VLS` / `HasDeadZone` | bool | `false` | Enables vertical launch/dead-zone behavior. |
| `DeadZoneTime` | int | `0` | Dead-zone duration. |
| `FixedTrackDirection` | bool | `false` | Uses a fixed tracking direction. |
| `GuidedTurnRadius` | float | `3.0` | Guided turn radius. |
| `GuidedPhaseSpeed` | float | `2.0` | Boost/guided phase speed. |
| `GuidedPhaseTurnSpeed` | float | `0.1` | Boost/guided phase turn speed. |
| `BoostParticle` | string | `~` | Boost phase particle type. |
| `Torpedo` | bool | `false` | Enables torpedo behavior. |
| `HasSubmunitions` | bool | `false` | Enables submunitions. |
| `Submunition` | resource | `""` | Submunition bullet short name. |
| `NumSubmunitions` | int | `0` | Number of submunitions. |
| `SubmunitionDelay` | int | `0` | Delay before deploying submunitions. |
| `SubmunitionSpread` | float | `1.0` | Submunition spread. |
| `DestroyOnDeploySubmunition` | bool | `false` | Destroys parent projectile when submunitions deploy. |
| `AddRound` | `<name> <count> <massG> [explosiveMassKg] [muzzleVelocityMps] [penetrationAt100m]` | none | Adds per-round stats when `RoundsPerItem > 1`. |

## `GrenadeType`

Inherits `ShootableType` and `InfoType`.

### Throwing And Physics

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `MeleeDamage` | int | `1` | Damage from melee-hitting with the grenade item. |
| `ThrowDelay` | int | `0` | Delay between throws. |
| `ThrowSound` | string | `""` | Sound name played when thrown. |
| `DropItemOnThrow` | resource | `~` | Item dropped when thrown. |
| `CanThrow` | bool | `true` | Whether right-click throwing is allowed. |
| `PenetratesBlocks` | bool | `false` | Whether thrown grenade penetrates blocks. |
| `BounceSound` | sound | `""` | Sound played on bounce. |
| `Bounciness` | float | `0.9` if absent | Overrides inherited bounciness. If the key is absent or has no value, grenade bounciness becomes `0.9`. |

### Sticky, Remote, Flash, And Smoke

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Sticky` | bool | `false` | Sticks to surfaces. |
| `StickToThrower` | bool | `false` | Sticks to the thrower. |
| `StickToEntity` | bool | `false` | Sticks to entities. |
| `StickToDriveable` | bool | `false` | Sticks to driveables. |
| `StickToEntityAfter` | bool | `false` | Delayed entity sticking behavior. |
| `AllowStickSound` | bool | `false` | Allows stick sound playback. |
| `StickSoundRange` | int | `10` | Stick sound range. |
| `StickSound` | sound | `~` | Stick sound. |
| `SpinWhenThrown` | bool | `true` | Whether the model spins when thrown. |
| `Remote` | bool | `false` | Can be detonated by remote tools. |
| `DetonateWhenShot` | bool | `false` | Can be detonated by being shot. |
| `FlashBang` | bool | `false` | Enables flashbang behavior. |
| `FlashTime` | int | `200` | Flash duration. |
| `FlashRange` | int | `8` | Flash radius/range. |
| `FlashSoundEnable` | bool | `false` | Enables flash sound. |
| `FlashSoundRange` | int | `16` | Flash sound range. |
| `FlashSound` | sound | `~` | Flash sound. |
| `FlashDamageEnable` | bool | `false` | Enables flash damage. |
| `FlashDamage` | float | `0.0` | Flash damage. |
| `FlashEffects` | bool | `false` | Enables flash potion effect behavior. |
| `FlashEffectsID` | int | `0` | Flash effect id. |
| `FlashEffectsDuration` | int | `0` | Flash effect duration. |
| `FlashEffectsLevel` | int | `0` | Flash effect amplifier. |
| `SmokeTime` | int | `0` | Smoke duration after detonation. |
| `SmokeParticles` | string | `explode` | Smoke particle type. |
| `SmokeRadius` | float | `5.0` | Radius for smoke effects. |
| `SmokeEffect` | effect line | none | Potion effects applied in smoke. Ambient/visible default to `false`. |

### Deployable Bag

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `DeployableBag` | optional bool | `false` | Enables bag behavior. A key with no value counts as true. |
| `NumUses` | int | `1` | Uses before the bag runs out. |
| `HealAmount` | float | `0.0` | Amount healed per use. |
| `AddPotionEffect` / `PotionEffect` | effect line | none | Potion effects applied to the user. Ambient/visible default to `false`. |
| `NumClips` | int | `0` | Number of clips supplied for the user's current gun. |

## `GunType`

Inherits `PaintableType` and `InfoType`.

### Damage, Reload, Fire Rate, And Ammo

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Damage` | float | `0.0` | Gun damage multiplier. Multiplied with ammo damage. |
| `MeleeDamage` | float | `1.0` | Melee damage. Also affects inferred primary/secondary functions. |
| `MeleeDamageDriveableModifier` | float | `1.0` | Melee damage modifier against driveables. |
| `CanForceReload` | bool | `true` | Allows reload key reloads. |
| `ReloadTime` | int | `0` | Reload time. |
| `ConsumeBulletType` | int | `1` | Digital ammo type consumed by reloads. |
| `BulletsPerReload` | double | `30.0` | Digital ammo amount consumed per reload. |
| `ShootDelay` | float | `0.0` | Legacy delay between shots. |
| `RoundsPerMin` | float | `0.0` | Fire rate in RPM. |
| `NumBullets` | int | `1` | Projectiles fired per shot. |
| `NumAmmoSlots` / `NumAmmoItemsInGun` / `LoadIntoGun` | int | `1` | Number of ammo stacks loaded into the gun. |
| `NumBurstRounds` | int | `3` | Shots per burst. |
| `AllowSpreadByBullet` | bool | `false` | Uses loaded ammo spread when available. |
| `AllowNumBulletsByBulletType` | bool | `true` | Allows ammo to override projectile count. |
| `BulletSpeed` | float or `instant` | `5.0` | Bullet speed. `instant` sets speed to `0`. |
| `MuzzleVelocity` | float m/s | `0.0` | Alternative speed input. Converted to blocks/tick by dividing by `20`. |
| `Ammo` | resource line, repeatable | none | Allowed ammo short name. |
| `Mode` | fire mode list | effective default `FULLAUTO` | One or more values: `SEMIAUTO`, `FULLAUTO`, `MINIGUN`, `BURST`. First value becomes the default allowed mode. If absent, `mode` is `SEMIAUTO` but allowed/default stack mode currently remains `FULLAUTO`. |

### Spread And Recoil

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Accuracy` / `Spread` | float | `0.0` | Gun spread. |
| `Dispersion` | float degrees | unset | Alternative angular spread input. Converted internally. |
| `SpreadPattern` | enum | `CIRCLE` | `CIRCLE`, `CUBE`, `TRIANGLE`, `HORIZONTAL`, or `VERTICAL`. |
| `ADSSpreadModifier` | float | `-1.0` | ADS spread modifier. `-1` uses common config. |
| `ADSSpreadModifierShotgun` | float | `-1.0` | ADS spread modifier for multishot guns. `-1` uses common config. |
| `Recoil` | float | `0.0` | Vertical recoil. |
| `RecoilYaw` | float | `0.0` | Horizontal recoil. Divided by `10` after reading. |
| `RandomRecoilRange` | float | `0.5` | Random extra vertical recoil range. |
| `RandomRecoilYawRange` | float | `0.3` | Random yaw recoil range. |
| `CounterRecoilForce` | float | `0.8` | Return-to-center coefficient. Higher means slower return. |
| `CounterRecoilForceSneaking` | float | `0.7` | Sneaking return coefficient. |
| `CounterRecoilForceSprinting` | float | `0.9` | Sprinting return coefficient. |
| `DecreaseRecoil` | float | `0.0` | Deprecated sneaking recoil reduction. |
| `DecreaseRecoilYaw` | float | `0.5` if absent or non-positive | Deprecated yaw reduction/divisor. |
| `RecoilWalkingMultiplier` | float | `1.0` | Walking recoil pitch multiplier. |
| `RecoilWalkingMultiplierYaw` | float | `1.1` | Walking recoil yaw multiplier. |
| `RecoilSprintingMultiplier` | float | `1.0` | Sprinting recoil pitch multiplier. |
| `RecoilSprintingMultiplierYaw` | float | `1.2` | Sprinting recoil yaw multiplier. |
| `RecoilSneakingMultiplier` | float | `-1.0` | Sneaking recoil pitch multiplier. `-1` preserves legacy behavior. |
| `RecoilSneakingMultiplierYaw` | float | `0.8` | Sneaking recoil yaw multiplier. |
| `FancyRecoil` | `<vertical> [horizontal] [recovery] [recoveryScope] [fall] [increase] [sneak] [speed]` | off | Enables extended recoil. Tokens may also be written as `name=value`; only the value after `=` is used. |

### Targeting, Movement, And Use Rules

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `CanLockAngle` | int | `5` | Lock-on angle setting. |
| `LockOnToDriveables` | bool | `false` | Sets planes, vehicles, and mechas together after reading. |
| `LockOnToVehicles` | bool | `false` | Allows vehicle lock-on. |
| `LockOnToPlanes` | bool | `false` | Allows plane lock-on. |
| `LockOnToMechas` | bool | `false` | Allows mecha lock-on. |
| `LockOnToPlayers` | bool | `false` | Allows player lock-on. |
| `LockOnToLivings` | bool | `false` | Allows living entity lock-on. |
| `MaxRangeLockOn` | int | `80` | Lock-on range. |
| `Knockback` | float | `0.0` | Shooter knockback per shot. |
| `WalkSpreadModifier` / `WalkSpreadMultiplier` | float | `1.8` | Spread multiplier while walking. |
| `SneakSpreadModifier` / `SneakSpreadMultiplier` | float | `0.63` | Spread multiplier while sneaking. |
| `SprintSpreadModifier` / `SprintSpreadMultiplier` | float | `7.0` | Spread multiplier while sprinting. |
| `AirborneSpreadModifier` / `AirborneSpreadMultiplier` | float | `1.5` | Spread multiplier while airborne. |
| `AllowRearm` | bool | `true` | Allows ammo from ammo magazines/supply behavior. |
| `ConsumeGunOnUse` | bool | `false` | Consumes the gun after use. |
| `ShowCrosshair` | bool | `true` | Shows crosshair while holding the gun. |
| `DropItemOnShoot` | resource | `~` | Item dropped when shooting. |
| `MinigunStartSpeed` | float | `15.0` | Spin speed needed before minigun fires. |
| `CanShootUnderwater` | bool | `true` | Allows firing underwater. |
| `CanSetPosition` | bool | `false` | Allows setting guided/launcher target position. |
| `OneHanded` | bool | `false` | Allows one-handed/dual-wield behavior. |
| `UsableByPlayers` | bool | `true` | Allows player use. |
| `UsableByMechas` | bool | `true` | Allows mecha use. |
| `ItemUseAction` | enum | `BOW` | Minecraft `UseAnim` value. |
| `HipFireWhileSprinting` | bool | unset | If absent, uses common config. If present, `true` force allows and `false` force denies. |
| `MoveSpeedModifier` / `Slowness` | float | `1.0` | Movement speed modifier while held. |
| `KnockbackReduction` / `KnockbackModifier` | float | `0.0` | Knockback resistance modifier while held. |
| `SwitchDelay` | float | `0.0` | Delay when switching to the gun. |

### Deployable And Melee

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Deployable` | bool | `false` | Places a deployed gun instead of normal firing. |
| `DeployedModel` | string | `""` | Model for deployed gun. |
| `DeployedTexture` | resource | `""` | Texture for deployed gun. |
| `StandBackDistance` | float | `1.5` | Player distance from deployed gun. |
| `TopViewLimit` | float | `-60.0` | Upward view limit. Positive configured values are negated. |
| `BottomViewLimit` | float | `30.0` | Downward view limit. |
| `SideViewLimit` | float | `45.0` | Horizontal view limit. |
| `PivotHeight` | float | `0.375` | Deployed gun pivot height. |
| `MeleeTime` | int | `1` | Delay between custom melee attacks. |
| `AddNode` | `<x> <y> <z> <pitch> <yaw> <roll>` | none | Adds a melee path node. Position values are divided by `16`. |
| `MeleeDamagePoint` / `MeleeDamageOffset` | `<x> <y> <z>` | none | Adds melee damage point. Values are divided by `16`. |
| `UseCustomMeleeWhenShoot` | bool | `false` | Makes shooting use custom melee behavior. |
| `UseCustomMelee` | optional bool | `false` | Enables custom melee. A key with no value counts as true. |
| `PrimaryFunction` | enum | inferred, usually `SHOOT` | `shoot`, `fire`, `zoom`, `melee`, `custommelee`, `custom_melee`; otherwise defaults to `ADS_ZOOM`. |
| `SecondaryFunction` | enum | inferred, usually `ADS_ZOOM` | Same accepted values as `PrimaryFunction`. |

### Display And Sounds

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `ShowAttachments` | bool | `true` | Shows attachments in the item GUI/tooltips. |
| `ShowDamage` | bool | `true` | Shows damage stat. |
| `ShowRecoil` | bool | `true` | Shows recoil stat. |
| `ShowAccuracy` | bool | `true` | Shows spread/accuracy stat. |
| `ShowReloadTime` | bool | `true` | Shows reload stat. |
| `ShowShootDelay` | bool | `true` | Shows shoot delay stat. |
| `ShowBulletSpeed` | bool | `true` | Shows bullet speed stat. |
| `ShowMode` | bool | `true` | Shows fire mode. |
| `DistortSound` | bool | `true` | Enables sound pitch distortion. |
| `SoundLength` | int | `0` | Shoot sound length. |
| `IdleSoundLength` | int | `0` | Idle loop length. |
| `WarmupSoundLength` | int | `20` | Warmup sound length. |
| `LoopedSoundLength` / `SpinSoundLength` | int | `20` | Looped/spin sound length. |
| `IdleSoundRange` | int | `-1` | Idle sound range. `-1` uses common config. |
| `MeleeSoundRange` | int | `-1` | Melee sound range. `-1` uses common config. |
| `ReloadSoundRange` | int | `-1` | Reload sound range. `-1` uses common config. |
| `GunSoundRange` | int | `-1` | Gunshot sound range. `-1` uses common config. |
| `DistantSoundRange` | int | `-1` | Distant shoot sound range. |
| `ShootSound` | sound | `~` | Shot sound. |
| `BulletInsertSound` | sound | `defaultshellinsert` | Shell insert/reload sound. |
| `ActionSound` | sound | `~` | Pump/action sound. |
| `LastShootSound` | sound | `~` | Sound for the last round. |
| `SuppressedShootSound` | sound | `~` | Suppressed shot sound. |
| `LastSuppressedShootSound` | sound | `~` | Suppressed last-round shot sound. |
| `ReloadSound` | sound | `~` | Reload sound. |
| `EmptyReloadSound` | sound | `~` | Empty reload sound. |
| `EmptyClickSound` | sound | `~` | One-shot empty click sound. |
| `EmptyClickSoundRepeated` | sound | `~` | Repeated empty click sound. |
| `IdleSound` | sound | `~` | Idle/held sound. |
| `MeleeSound` | sound | `~` | Melee swing sound. |
| `WarmupSound` | string | `~` | Loop warmup sound. |
| `LoopedSound` / `SpinSound` | string | `~` | Main looping sound. Enables looping sound behavior when set. |
| `CooldownSound` | string | `~` | Loop cooldown sound. |
| `LockOnSound` | sound | `""` | Lock-on sound. |
| `LockOnSoundTime` | int | `0` | Lock-on sound interval/time. |
| `DistantSound` / `DistantShootSound` | sound | `""` | Distant shot sound. |

### Scope, Models, Particles, Attachments, And Shield

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Scope` | resource | inherited `Overlay` | Scope overlay. `none` clears it. |
| `ZoomLevel` | float | `1.0` | Default scope zoom. |
| `FOVZoomLevel` | float | `1.5` | Default FOV zoom. |
| `AllowNightVision` | bool | `false` | Allows night vision while scoped. |
| `HasVariableZoom` | bool | `false` | Enables variable zoom. |
| `MinZoom` | float | `1.0` | Minimum variable zoom. |
| `MaxZoom` | float | `4.0` | Maximum variable zoom. |
| `ZoomAugment` | float | `1.0` | Variable zoom step/change. |
| `CasingModel` | string | `""` | Casing model. |
| `CasingTexture` | resource | `""` | Casing texture. |
| `FlashModel` | string | `""` | Flash model. |
| `FlashTexture` | resource | `""` | Flash texture. |
| `MuzzleFlashModel` | string | `""` | Muzzle flash model. |
| `HitTexture` | resource | `""` | Hit marker texture. |
| `MuzzleFlashParticle` | string | `flansmod.muzzleflash` | Muzzle flash particle type. |
| `MuzzleFlashParticleSize` | float | `1.0` | Muzzle flash particle size. |
| `ShowMuzzleFlashParticleFirstPerson` | bool | `false` | Shows muzzle flash particles in first person. |
| `MuzzleFlashParticleShoulderOffset` | vector | `[0,0,0]` | Shoulder-fire particle offset. |
| `MuzzleFlashParticleHandOffset` | vector | `[0,0,0]` | Hand-fire particle offset. |
| `ShowMuzzleFlashParticle` | bool | `true` | Shows muzzle flash particles. |
| `AllowAllAttachments` | bool | `true` | Allows all attachments without checking restrictions. |
| `AllowAttachments` | resource list | none | Specific allowed attachment short names. |
| `AllowBarrelAttachments` | bool | `false` | Allows barrel slot. |
| `AllowScopeAttachments` | bool | `false` | Allows scope/sights slot. |
| `AllowStockAttachments` | bool | `false` | Allows stock slot. |
| `AllowGripAttachments` | bool | `false` | Allows grip slot. |
| `AllowGadgetAttachments` | bool | `false` | Allows gadget slot. |
| `AllowSlideAttachments` | bool | `false` | Allows slide slot. |
| `AllowPumpAttachments` | bool | `false` | Allows pump slot. |
| `AllowAccessoryAttachments` | bool | `false` | Allows accessory slot. |
| `NumGenericAttachmentSlots` | int | `0` | Number of generic attachment slots. |
| `Shield` | `<absorb> <originX> <originY> <originZ> <sizeX> <sizeY> <sizeZ>` | none | Adds a shield hitbox. Coordinates and size are divided by `16`; absorption is `0.0..1.0`. |

### Gun Animation Keys

All keys in this subsection are read by `GunAnimationConfig` from gun type files on the client. Defaults are `unset`, meaning the model/default animation values are used.

| Key | Format | Purpose |
| --- | --- | --- |
| `animMinigunBarrelOrigin` | vector | Minigun barrel origin. |
| `animBarrelAttachPoint` / `animScopeAttachPoint` / `animStockAttachPoint` / `animGripAttachPoint` / `animGadgetAttachPoint` / `animSlideAttachPoint` / `animPumpAttachPoint` / `animAccessoryAttachPoint` | vector | Attachment points. |
| `animDefaultBarrelFlashPoint` / `animMuzzleFlashPoint` | vector | Flash/muzzle points. |
| `animHasFlash` / `animHasArms` / `easyArms` | bool | Flash and arm-rendering toggles. |
| `armScale` | vector | Global arm scale. |
| `animLeftArmPos` / `animLeftArmRot` / `animLeftArmScale` | vector | Left arm transform. |
| `animRightArmPos` / `animRightArmRot` / `animRightArmScale` | vector | Right arm transform. |
| `animRightArmReloadPos` / `animRightArmReloadRot` / `animLeftArmReloadPos` / `animLeftArmReloadRot` | vector | Reload arm transforms. |
| `animRightArmChargePos` / `animRightArmChargeRot` / `animLeftArmChargePos` / `animLeftArmChargeRot` | vector | Charge arm transforms. |
| `animStagedRightArmReloadPos` / `animStagedRightArmReloadRot` / `animStagedLeftArmReloadPos` / `animStagedLeftArmReloadRot` | vector | Staged reload arm transforms. |
| `animRightHandAmmo` / `animLeftHandAmmo` | bool | Hand ammo display toggles. |
| `animGunSlideDistance` / `animAltGunSlideDistance` / `animRecoilSlideDistance` / `animRotatedSlideDistance` / `animShakeDistance` / `animRecoilAmount` | float | Slide, recoil, and shake movement amounts. |
| `animCasingAnimDistance` / `animCasingAnimSpread` / `animCasingRotateVector` / `animCasingAttachPoint` | vector | Casing animation vectors. |
| `animCasingAnimTime` / `animCasingDelay` | int | Casing timing. |
| `animCasingScale` / `animFlashScale` | float | Casing and flash scale. |
| `animChargeHandleDistance` / `animChargeDelay` / `animChargeDelayAfterReload` / `animChargeTime` | float/int | Charge handle movement and timing. |
| `animCountOnRightHandSide` / `animIsBulletCounterActive` / `animIsAdvBulletCounterActive` | bool | Ammo counter options. |
| `animTiltGunTime` / `animUnloadClipTime` / `animLoadClipTime` | float | Reload timing stages. |
| `animScopeIsOnSlide` / `animScopeIsOnBreakAction` | bool | Scope attachment movement flags. |
| `animNumBulletsInReloadAnimation` | float | Number of bullets represented in reload animation. |
| `animPumpDelay` / `animPumpDelayAfterReload` / `animPumpTime` / `animHammerDelay` | int | Pump and hammer timing. |
| `animPumpHandleDistance` / `animEndLoadedAmmoDistance` / `animBreakActionAmmoDistance` | float | Pump/end-loaded/break-action movement. |
| `animGripIsOnPump` / `animGadgetsOnPump` | bool | Attachment-on-pump flags. |
| `animBarrelBreakPoint` / `animAltBarrelBreakPoint` | vector | Break-action pivot points. |
| `animRevolverFlipAngle` / `animRevolver2FlipAngle` / `animBreakAngle` / `animAltBreakAngle` | float | Revolver and break-action angles. |
| `animRevolverFlipPoint` / `animRevolver2FlipPoint` | vector | Revolver flip points. |
| `animSpinningCocking` | bool | Spinning cocking behavior. |
| `animSpinPoint` / `animHammerSpinPoint` / `animAltHammerSpinPoint` | vector | Spin/hammer points. |
| `animHammerAngle` / `animAltHammerAngle` | float | Hammer angles. |
| `animIsSingleAction` / `animSlideLockOnEmpty` | bool | Single-action and slide-lock behavior. |
| `animLeftHandPump` / `animRightHandPump` / `animLeftHandCharge` / `animRightHandCharge` / `animLeftHandBolt` / `animRightHandBolt` | bool | Handedness flags for pump, charge, and bolt actions. |
| `animPumpModifier` / `animGunOffset` / `animCrouchZoom` / `animRotateGunVertical` / `animRotateGunHorizontal` / `animTiltGun` | float | General first-person transform modifiers. |
| `animChargeModifier` / `animTranslateGun` | vector | Charge/gun translation vectors. |
| `animFancyStance` / `animStagedReload` / `animStillRenderGunWhenScopedOverlay` | bool | Stance, reload, and scoped rendering toggles. |
| `animTranslateClip` | vector | Current code reads this for both stance translate and clip translate. |
| `animStanceRotate` | vector | Stance rotation. |
| `animRotateClipVertical` / `animStagedRotateClipVertical` / `animRotateClipHorizontal` / `animStagedRotateClipHorizontal` / `animTiltClip` / `animStagedTiltClip` | float | Clip rotation and tilt. |
| `animStagedTranslateClip` / `animThirdPersonOffset` / `animItemFrameOffset` | vector | Staged clip, third-person, and item-frame offsets. |
| `animAdsEffectMultiplier` | float | ADS visual effect multiplier. |

## `GunBoxType`

Inherits `BlockType` and `InfoType`.

### GUI

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `GuiTexture` | file-name resource | default gun box GUI | Custom GUI texture. |
| `GunBoxNameColor` | hex string | `404040` | Gun box title color. |
| `PageTextColor` | hex string | `FFFFFF` | Page text color. |
| `ListTextColor` | hex string | `404040` | Item list text color. |
| `ItemTextColor` | hex string | `404040` | Item text color. |
| `ButtonTextColor` | hex string | `FFFFFF` | Button text color. |
| `ButtonTextHighlight` | hex string | `FFFFA0` | Button hover text color. |

### Entries

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Page` / `SetPage` | text line | `Default` | Starts or names a page. Pages hold up to 8 guns; overflow creates another default page. |
| `AddGun` | `<shortName> [<amount> <item> ...]` | none | Adds a gun/item entry and its required parts. |
| `AddAmmo` / `AddAltAmmo` / `AddAlternateAmmo` | `<shortName> [<amount> <item> ...]` | none | Adds ammo to the most recently added gun entry. |

## `ArmorBoxType`

Inherits `BlockType` and `InfoType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `GuiTexture` | file-name resource | default armor box GUI | Custom GUI texture. |
| `AddArmour` / `AddArmor` | `<shortName> <display name...>` followed by 4 armor lines | none | Adds one armor set page. The following four lines are parsed as helmet, chest, legs, and boots. |

Armor set lines after `AddArmor` use:

```text
<armorShortName> [<item> <amount> ...]
```

The recipe parser uses item/amount pairs for those four armor lines.

## `PartType`

Inherits `InfoType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Category` | enum | `COCKPIT` | `COCKPIT`, `WING`, `ENGINE`, `PROPELLER`, `BAY`, `TAIL`, `WHEEL`, `CHASSIS`, `TURRET`, `FUEL`, `MISC`. |
| `StackSize` | int | `0` | Item stack size. |
| `FuelConsumption` | float | `1.0` | Engine fuel consumption. |
| `EngineSpeed` | float | `1.0` | Engine speed/thrust multiplier. |
| `EnginePower` | float | `10.0` | Engine power output. |
| `UseRF` / `UseRFPower` | bool | `false` | Uses RF power instead of Flan's Mod fuel. |
| `RFDrawRate` | int | `1` | RF draw per tick. |
| `Fuel` | int | `0` | Fuel amount for fuel parts. |
| `PartBoxRecipe` | current parser starts amount/item pairs at the third token | none | Part box recipe ingredients. The first two tokens after the key are currently skipped by code. |

`WorksWith` exists in commented-out code and is not currently read.

## `ToolType`

Inherits `InfoType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Parachute` | bool | `false` | Deploys a parachute on use. |
| `ExplosiveRemote` | bool | `false` | Detonates remote explosives. |
| `Key` | bool | `false` | Marks the tool as a key. |
| `Heal` / `HealPlayers` | bool | `false` | Enables player healing. |
| `Repair` / `RepairVehicles` | bool | `false` | Enables driveable repair. |
| `HealAmount` / `RepairAmount` | int | `0` | Heal/repair amount per use. |
| `ToolLife` / `ToolUes` | int | `0` | Number of uses. `0` means infinite. `ToolUes` is the typo currently accepted by code. |
| `DestroyOnEmpty` | bool | `true` | Destroys the tool when depleted. |
| `Food` / `Foodness` | int | `0` | Hunger restored when eaten/used as food. |
| `RechargeRecipe` | current parser starts amount/item pairs at the second token | none | Shapeless recharge ingredients. The first token after the key is currently skipped by code. |

## `GloveType`

Inherits `InfoType`.

| Key | Format | Default | Purpose |
| --- | --- | --- | --- |
| `Enchantability` | int | `20` | Glove enchantability. |
| `Durability` | int | `200` | Glove durability. `0` means no durability. |

## `ItemHolderType`

Inherits `InfoType`.

`ItemHolderType` currently reads no additional parameters beyond the shared `InfoType` keys.

## `DriveableType`

Extends `PaintableType`, but it is not currently registered in `EnumType` and has no `read(...)` override.

If it is wired into loading later, it will inherit `PaintableType` and `InfoType` parameters. The fields currently present in the class, such as locked-on sounds, bullet detection radius, and flares, are not read from `.txt` files yet.
