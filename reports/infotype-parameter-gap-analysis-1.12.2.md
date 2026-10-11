# InfoType Parameter Gap Analysis

- **Target**: `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-Ultimate-2.0` (`com.flansmodultimate`, Forge 1.20.1)
- **Reference**: `C:\Users\alpha\Documents\Minecraft-Development\FlansMod` (Flan's Mod 5.10.0 / MC 1.12.2, `com.flansmod`)
- Analysis only. No type definitions or code were modified.

## Revalidation (2026-10-11)

Rechecked against target HEAD `263d1fe447df1b5cd779ef18ea6fbe37a8ff3d04` plus the current working tree, including the ongoing driveable animation changes. Both reference HEADs are unchanged. This pass revalidates the report's existing findings and caveats; it is not a new exhaustive discovery audit. Reference ghosts describe the reference and are not automatically target defects. Definition counts below describe the initial audit corpus.

## Method (initial audit)

- Target hierarchy: `InfoType` → `PaintableType` → {`GunType`, `AttachmentType`, `DriveableType` → `PlaneType`/`VehicleType`/`MechaType`}, `ShootableType` → `BulletType`/`GrenadeType`, plus `AAGunType`, `ArmorType`, `ArmorBoxType`, `GunBoxType`, `BlockType`, `GloveType`, `ItemHolderType`, `MechaItemType`, `PartType`, `PlayerClass`, `RewardBox`, `Team`, `ToolType`, `LoadoutPool`.
- Target parsing is key-lookup based: `TypeReaderUtils.read*(key, default, file)` against `TypeFile`'s `configMap`, which lower-cases every key (`common/types/TypeFile.java:41,49,64`), so dispatch is case-insensitive. Alias helpers (`aliasInt`/`aliasFloat`/`aliasSound`/`hasAnyConfigLine`), multi-key readers (`addEffects`, `readArmour`, `forEachLine`, `resolveShootDelay`, `registerSoundTimer`) and the auxiliary readers `VehicleArmorSpecReader`, `RealWorldSpecReader`, `AmmoOverrides` and `GunAnimationConfig` were all inventoried. Category JSON injection (`TypeFile.addCategoryConfigMap`) feeds the same map.
- Reference parsing is line-dispatch based: `KeyMatches`/`Read(split, "Key", …)` (case-insensitive via `common/types/InfoType.java:224`), `split[0].equals("Key")` chains, and the `parsers` lambda registry in `DriveableType`/`PlaneType`/`VehicleType`/`GloveType` (`parsers.put("Key", …)`).
- Definition inventory: reference `run/Flan/**` (806 `.txt` files); target `src/manuspacks/**/definitions/**`. Keys were grouped by content-pack type folder and reconciled with the parser inventory per class. Recipe-grid rows and value tokens were excluded from key sets.
- Consumer tracing: every parser-assigned field was checked for reads, including Lombok-generated `get*`/`is*` accessors, method references (`MechaItemType::isFloater`), the legacy `com.flansmod.client.model.*` renderers still present in the target, and tests. Candidates surfaced by the automated write-only scan were then verified by hand.

## Target ghost parameters

### DriveableType → MechaType / PlaneType / VehicleType

| Parameter | Status / confidence | Parser evidence | Consumer investigation and reason |
| --- | --- | --- | --- |
| `ExitSoundLength` | GHOST_PARSED / HIGH | `src/main/java/com/flansmodultimate/common/types/DriveableType.java#read`, `#registerSoundTimer`; `InfoType#resolveSoundLengths` | The duration is parsed, may be replaced by a measured sound length, and is used for bookkeeping/logging. `Driveable#handleRisingInputs` plays `ExitSound` once through `PacketPlaySound`, without reading its duration or scheduling repetitions. No gameplay or playback consumer of the duration was found. |

No currently observed GHOST_UNPARSED finding remains from the previous target list. `OnRadar` is no longer parsed, and the dead driveable definition lines (`Bounciness`, `MomentOfInertia`, `ClutchSteer`, `FlipLinkFix`, `FloatOnLand`, `HasSmoke`, `SoundsPlaceTimePrimary`) have been removed from the source definition corpus, including nested pack repositories. `Bounciness` remains active for shootables, which does not make it a driveable key. `VehicleType#applyLegacyTurretRotationSpeed` now accepts `TurretRotationSpeed` and converts it into the driver's aiming speed. Removed keys are documented in `docs/infotype-removed-parameters.md`; arbitrary third-party definitions may still contain them.

## Reference ghost parameters

### DriveableType

All entries below are registered in the `parsers` lambda map and write a field that is never read anywhere in `com.flansmod`.

- **`OnRadar` — GHOST_PARSED** — `common/driveables/DriveableType.java:622-623`; field `:171`. No reader.
- **`SetPlayerInvisible` — GHOST_PARSED** — `:727`; field `:230`. No reader; riders are never hidden.
- **`IsExplosionWhenDestroyed` — GHOST_PARSED** — `:652-653`; field `:194`. Death explosions are driven by other fields; this flag is never consulted.
- **`FancyCollision` — GHOST_PARSED** — `:769-770`; field `:242`. `AddCollisionMesh` data is used, but the toggle is not.
- **`FlareSound` — GHOST_PARSED (partial)** — `:764-767`; field `:208`. The lambda also calls `FlansMod.proxy.loadSound(...)`, so the sound file is registered, but the stored name is never played back.
- **`LockedOnSound` — GHOST_PARSED (partial)** — `:755-758`; field `:196`. Same pattern: registered, never played.
- **`LockingOnSound` — GHOST_PARSED (partial)** — `:760-763`; field `:201`. Same pattern.
- **`PlaceSoundPrimary` / `PlaceSoundSecondary` — GHOST_PARSED (partial)** — `:735-742`; fields `:84`. Registered via `loadSound`; fields never read.
- **`ReloadSoundPrimary` / `ReloadSoundSecondary` — GHOST_PARSED (partial)** — `:745-752`; fields `:82`. Registered via `loadSound`; fields never read.

### GunType

- **`CanShootUnderwater` — GHOST_PARSED** — `common/guns/GunType.java:453`; field `:94`. No read; underwater firing is not gated.
- **`NumBurstRounds` — GHOST_PARSED** — `:345`; field `:82`. Burst firing does not consult it.

### GrenadeType

- **`PenetratesEntities` — GHOST_PARSED** — `common/guns/GrenadeType.java:185-186`; field `:47`. Its sibling `penetratesBlocks` *is* read (`common/guns/EntityGrenade.java:324`), but the entity variant is not.
- **`ExplosionDamageVsLiving` — GHOST_PARSED** — `:211-212`; field `:87`. No read.
- **`ExplosionDamageVsDrivable` — GHOST_PARSED** — `:213-214`; field `:87`. No read.

### AAGunType

- **`TargetMechas` — GHOST_PARSED** — `common/guns/AAGunType.java:84`; field `:42`. Written by the dedicated key and by the combined line at `:90`, but never read by the AA gun targeting code (`targetMobs`/`targetPlayers` are read; the mecha flag is not).

### MechaItemType

- **`FlameBurst` — GHOST_PARSED** — `common/driveables/mechas/MechaItemType.java:129-130`; field `:64`. No read. The target does consume its equivalent (`common/entity/Mecha.java:651`).

### Reference definition keys with no parser

Observed in `run/Flan/**` definitions, with no accepting parser anywhere in `com.flansmod`:

- **`ItemID` — GHOST_UNPARSED** — `armorFiles`, `bullets`, `guns`, `vehicles`. Legacy numeric item-id line.
- **`GunBoxID` — GHOST_UNPARSED** — `boxes`.
- **`MeleeWeapon` — GHOST_UNPARSED** — `guns` (`MeleeDamage*` are separate keys).
- **`AllowG43RelAttachments` — GHOST_UNPARSED** — `run/Flan/WW2 Pack/guns/G43.txt:38` (`Allowg43relAttachments false`); no accepting reference parser. The definition is ignored by the default Git-aware search and was verified directly.
- **`Mass` — GHOST_UNPARSED** — `vehicles`. The target *does* accept `Mass` (`common/types/DriveableType.java`).
- **`MomentOfInertia` — GHOST_UNPARSED** — `vehicles`.
- **`SwitchDelay` — GHOST_UNPARSED** — `guns`. The target accepts `SwitchDelay` (`GunType`).
- **`HasLandingGear` — GHOST_UNPARSED** — `planes`. The target accepts it (`PlaneType`).
- **`AddEmitter` — GHOST_UNPARSED** — `vehicles`. Reference parses `AddParticle` only; the target accepts `AddEmitter` (`DriveableType`).
- **`ExplodeParticleType` — GHOST_UNPARSED** — `bullets`, `grenades`. The target accepts it (`ShootableType`).
- **`DammageModifierPrimary` — GHOST_UNPARSED** — `vehicles`. The target accepts this misspelled key (`DriveableType`).

Armour-box item shortnames (`GermanHelmet`, `SASBoots`, …) and recipe-grid rows were excluded as values, not parameter names.

## Reference parameters absent from target

Case-insensitive lexical comparison of the reference key set (parser names + aliases + real definition keys) against the target key set (including inherited parsers, aliases and auxiliary readers). Every candidate was re-verified by searching the full target sources for the literal in any casing.

| Reference key | Reference class(es) | Parsed / in defs | Reference evidence | Target parsers/aliases checked |
| --- | --- | --- | --- | --- |
| `NumGuns` | `GunBoxType` | parsed | `GunBoxType.java:61` | No literal anywhere in target; target boxes are page/entry driven (`Page`/`SetPage`) |
| `EnableReloadTime` | `DriveableType` | parsed | `DriveableType.java:713` | Target has `ReloadTime*` keys but no `EnableReloadTime` toggle |
| `GunLength` | `DriveableType` | parsed | `DriveableType.java:710` | No literal in target |
| `RecoilDistance` | `DriveableType` | parsed | `DriveableType.java:711` | Target has `RecoilTime` and the `Recoil*` multipliers; no `RecoilDistance` |
| `RadarDetectableAltitude` | `DriveableType` | parsed | `DriveableType.java:874` | No literal in target (no radar system) |
| `Stealth` | `DriveableType` | parsed | `DriveableType.java:875` | No literal in target |
| `NumWheels` | `DriveableType` | parsed + `planes` defs | `DriveableType.java:917` (in `preRead`) | Target derives wheels from `WheelPosition` lines; no `NumWheels` |
| `PenetratesEntities` | `GrenadeType` | parsed (ghost in ref) | `GrenadeType.java:185` | Target accepts `PenetratesBlocks` only |
| `ExplosionDamageVsDrivable` | `GrenadeType` | parsed (ghost in ref) | `GrenadeType.java:213` | Target uses `DeathExplosionDamageVsVehicle`/`…VsPlane`; the `Drivable` spelling is not accepted |
| `EUPerCharge` | `ToolType` | parsed | `common/tools/ToolType.java:101` | Target uses `RFDrawRate`/`UseRF`/`UseRFPower`; no EU key |
| `ItemID` | `ArmourType`, `BulletType`, `GunType`, `VehicleType` | defs only | `armorFiles`/`bullets`/`guns`/`vehicles` `.txt` | No literal in target |
| `GunBoxID` | `GunBoxType` | defs only | `boxes` `.txt` | No literal in target |
| `MeleeWeapon` | `GunType` | defs only | `guns` `.txt` | No literal in target (`MeleeDamage*` are different keys) |
| `AllowG43RelAttachments` | `GunType` | defs only | `run/Flan/WW2 Pack/guns/G43.txt:38` | No accepting target parser/alias or observed target definition key |
| `MomentOfInertia` | `VehicleType` | defs only | `vehicles` `.txt` | No accepting target parser; the formerly authored target driveable lines have been removed |

This table is lexical parser/definition compatibility only. Several rows have differently named target behaviour (for example `EUPerCharge` ↔ `RFDrawRate`, `NumWheels` ↔ `WheelPosition` lines); they are still listed because the target does not accept the reference spelling as an alias.

Accepted names removed in this revalidation: `AddType`/`AddAltType` in `GunBoxType#readLine`; `ToolUses` in `ToolType#read`; `TurretRotationSpeed` in `VehicleType#applyLegacyTurretRotationSpeed`; all five leg-group keys in `MechaType#read`, consumed by `ModelMecha`. These are no longer lexical gaps. `AllowG43RelAttachments` remains a reference definition ghost and is added to the lexical gap table: the initial table omitted it. The current reference definition was verified directly, including files excluded by Git-aware search.

## Uncertain parameters

- **Third-party content packs in `run/flan/*.zip`** (Plume, Rainfire5, TaP, WW1/WW2 armour packs, …) were not unpacked for key inventory; the target's own `src/manuspacks` definitions were used instead. Keys those packs carry that the target does not parse would be additional GHOST_UNPARSED findings. UNCERTAIN by omission, LOW impact on the parser inventory itself.
- **Category JSON injection** (`common/types/TypeFile.java:67-70`, `config/CategoryManager.java`) can inject arbitrary field/value pairs into a type's config map at load time. Any key a category file supplies is indistinguishable at the parser level from a `.txt` line, so a key could be "observed" only through category data and still be active. No ghost classification above depends on that path, but the category JSON key space was not exhaustively enumerated. UNCERTAIN, MEDIUM confidence in completeness.

### GunType — custom model consumers

The same five target parameters remain uncertain as in the 1.7.10 comparison. `GunAnimationConfig#read` accepts them and `ModelGun#writeToModel` copies them, but source-wide field/getter searches found no terminal built-in renderer consumer. External models may consume them; copying alone does not establish liveness.

| Parameter | Status | Confidence |
| --- | --- | --- |
| `animStagedRightArmReloadPos` | UNCERTAIN | MEDIUM |
| `animStagedRightArmReloadRot` | UNCERTAIN | MEDIUM |
| `animLeftHandBolt` | UNCERTAIN | MEDIUM |
| `animAdsEffectMultiplier` | UNCERTAIN | MEDIUM |
| `animMeleeAnimation` | UNCERTAIN | MEDIUM |

Other animation keys were not exhaustively retraced in this revalidation.

## Summary — ghost findings

| Repo | Class / scope | Parameter | Status | Confidence |
| --- | --- | --- | --- | --- |
| Target | `DriveableType` | `ExitSoundLength` | GHOST_PARSED | HIGH |
| Reference | `DriveableType` | `OnRadar` | GHOST_PARSED | HIGH |
| Reference | `DriveableType` | `SetPlayerInvisible` | GHOST_PARSED | HIGH |
| Reference | `DriveableType` | `IsExplosionWhenDestroyed` | GHOST_PARSED | HIGH |
| Reference | `DriveableType` | `FancyCollision` | GHOST_PARSED | HIGH |
| Reference | `DriveableType` | `FlareSound` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `LockedOnSound` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `LockingOnSound` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `PlaceSoundPrimary` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `PlaceSoundSecondary` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `ReloadSoundPrimary` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `DriveableType` | `ReloadSoundSecondary` | GHOST_PARSED (sound still registered) | HIGH |
| Reference | `GunType` | `CanShootUnderwater` | GHOST_PARSED | HIGH |
| Reference | `GunType` | `NumBurstRounds` | GHOST_PARSED | HIGH |
| Reference | `GrenadeType` | `PenetratesEntities` | GHOST_PARSED | HIGH |
| Reference | `GrenadeType` | `ExplosionDamageVsLiving` | GHOST_PARSED | HIGH |
| Reference | `GrenadeType` | `ExplosionDamageVsDrivable` | GHOST_PARSED | HIGH |
| Reference | `AAGunType` | `TargetMechas` | GHOST_PARSED | HIGH |
| Reference | `MechaItemType` | `FlameBurst` | GHOST_PARSED | HIGH |
| Reference | `armorFiles`/`bullets`/`guns`/`vehicles` defs | `ItemID` | GHOST_UNPARSED | HIGH |
| Reference | `boxes` defs | `GunBoxID` | GHOST_UNPARSED | HIGH |
| Reference | `guns` defs | `MeleeWeapon` | GHOST_UNPARSED | HIGH |
| Reference | `guns` defs | `AllowG43RelAttachments` | GHOST_UNPARSED | HIGH |
| Reference | `guns` defs | `SwitchDelay` | GHOST_UNPARSED | HIGH |
| Reference | `vehicles` defs | `Mass` | GHOST_UNPARSED | HIGH |
| Reference | `vehicles` defs | `MomentOfInertia` | GHOST_UNPARSED | HIGH |
| Reference | `vehicles` defs | `DammageModifierPrimary` | GHOST_UNPARSED | HIGH |
| Reference | `vehicles` defs | `AddEmitter` | GHOST_UNPARSED | HIGH |
| Reference | `planes` defs | `HasLandingGear` | GHOST_UNPARSED | HIGH |
| Reference | `bullets`/`grenades` defs | `ExplodeParticleType` | GHOST_UNPARSED | HIGH |

Counts: target — 1 GHOST_PARSED, 0 GHOST_UNPARSED, 5 UNCERTAIN. Reference — 18 GHOST_PARSED, 11 GHOST_UNPARSED. Counts describe the retained findings, not exhaustive liveness of every parameter.

## Summary — reference keys absent from target

| Key | Reference class(es) | Parsed | In defs | Confidence |
| --- | --- | --- | --- | --- |
| `NumGuns` | `GunBoxType` | yes | no | HIGH |
| `EnableReloadTime` | `DriveableType` | yes | no | HIGH |
| `GunLength` | `DriveableType` | yes | no | HIGH |
| `RecoilDistance` | `DriveableType` | yes | no | HIGH |
| `RadarDetectableAltitude` | `DriveableType` | yes | no | HIGH |
| `Stealth` | `DriveableType` | yes | no | HIGH |
| `NumWheels` | `DriveableType` | yes | yes (`planes`) | HIGH |
| `PenetratesEntities` | `GrenadeType` | yes (ghost) | no | HIGH |
| `ExplosionDamageVsDrivable` | `GrenadeType` | yes (ghost) | no | HIGH |
| `EUPerCharge` | `ToolType` | yes | no | HIGH |
| `ItemID` | `ArmourType`, `BulletType`, `GunType`, `VehicleType` | no | yes | HIGH |
| `GunBoxID` | `GunBoxType` | no | yes | HIGH |
| `MeleeWeapon` | `GunType` | no | yes | HIGH |
| `AllowG43RelAttachments` | `GunType` | no | yes | HIGH |
| `MomentOfInertia` | `VehicleType` | no | yes | HIGH |

Total: **15** reference parameter names absent from the target parser/alias and observed-definition inventory.

## Areas requiring deeper audit

- **Bundled third-party packs** (`run/flan/*.zip`): unpack and re-run the definition-key reconciliation to find GHOST_UNPARSED keys reaching real users of this build.
- **Category JSON key space** (`config/CategoryManager.java` + `TypeFile.addCategoryConfigMap`): keys injected there bypass the `.txt` inventory entirely; enumerating the shipped category files would harden both the target key set and the ghost classification.
- **External gun models and remaining animation keys**: the five named uncertain parameters have no bundled terminal consumer; external models were not executed. The remaining animation key space was not exhaustively retraced.
- **Reference sound-registering ghosts** (`FlareSound`, `Lock*Sound`, `PlaceSound*`, `ReloadSound*`): these have a real side effect (sound registration) even though the stored value is unused. If porting parity is judged by "the sound plays", they are reference bugs rather than features to reproduce.

## Validation

Rechecked every previous reference-only key against current Java sources, inherited/delegated readers and aliases. Reconciled the removed driveable keys against current source definitions, including nested pack repositories. Followed restored mecha geometry to `ModelMecha`, turret speed to driver aiming, and the retained sound-duration finding to playback. No code or definitions changed; no wiki update is needed for this report-only revalidation. Runtime packs and external models remain outside the checked corpus.
