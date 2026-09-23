# InfoType Parameter Gap Analysis

- **Target**: `C:\Users\alpha\Documents\Minecraft-Development\Flans-Mod-Ultimate-2.0` (`com.flansmodultimate`, Forge 1.20.1)
- **Reference**: `C:\Users\alpha\Documents\Minecraft-Development\FlansMod` (Flan's Mod 5.10.0 / MC 1.12.2, `com.flansmod`)
- Analysis only. No type definitions or code were modified.

## Method

- Target hierarchy: `InfoType` → `PaintableType` → {`GunType`, `AttachmentType`, `DriveableType` → `PlaneType`/`VehicleType`/`MechaType`}, `ShootableType` → `BulletType`/`GrenadeType`, plus `AAGunType`, `ArmorType`, `ArmorBoxType`, `GunBoxType`, `BlockType`, `GloveType`, `ItemHolderType`, `MechaItemType`, `PartType`, `PlayerClass`, `RewardBox`, `Team`, `ToolType`, `LoadoutPool`.
- Target parsing is key-lookup based: `TypeReaderUtils.read*(key, default, file)` against `TypeFile`'s `configMap`, which lower-cases every key (`common/types/TypeFile.java:41,49,64`), so dispatch is case-insensitive. Alias helpers (`aliasInt`/`aliasFloat`/`aliasSound`/`hasAnyConfigLine`), multi-key readers (`addEffects`, `readArmour`, `forEachLine`, `resolveShootDelay`, `registerSoundTimer`) and the auxiliary readers `VehicleArmorSpecReader`, `RealWorldSpecReader`, `AmmoOverrides` and `GunAnimationConfig` were all inventoried. Category JSON injection (`TypeFile.addCategoryConfigMap`) feeds the same map.
- Reference parsing is line-dispatch based: `KeyMatches`/`Read(split, "Key", …)` (case-insensitive via `common/types/InfoType.java:224`), `split[0].equals("Key")` chains, and the `parsers` lambda registry in `DriveableType`/`PlaneType`/`VehicleType`/`GloveType` (`parsers.put("Key", …)`).
- Definition inventory: reference `run/Flan/**` (806 `.txt` files); target `src/manuspacks/**/definitions/**`. Keys were grouped by content-pack type folder and reconciled with the parser inventory per class. Recipe-grid rows and value tokens were excluded from key sets.
- Consumer tracing: every parser-assigned field was checked for reads, including Lombok-generated `get*`/`is*` accessors, method references (`MechaItemType::isFloater`), the legacy `com.flansmod.client.model.*` renderers still present in the target, and tests. Candidates surfaced by the automated write-only scan were then verified by hand.

## Target ghost parameters

### DriveableType (planes, vehicles, mechas)

- **`OnRadar` — GHOST_PARSED**
  - Parser: `common/types/DriveableType.java:712` → `onRadar = readValue("OnRadar", onRadar, file);`; backing field declared at `:189`.
  - Consumer investigation: `onRadar` has zero reads anywhere in `src/main/java` or `src/test/java` — no Lombok accessor (`isOnRadar`/`getOnRadar`) call site exists, and the field is not serialized, networked, or read by any radar code. The only radar-named symbols in the target are the `RADAR_1..RADAR_4` driveable parts in `common/driveables/EnumDriveablePart.java:61-64`, which are unrelated part slots.
  - Why ineffective: the parsed boolean is stored and never consulted; there is no radar detection system in the target. Setting `OnRadar` in a definition changes nothing observable. Confidence HIGH.
  - Note: the same parameter is also ghost in the reference (below), so this is inherited dead surface, not a porting regression.

### Plane / vehicle definitions (unparsed key)

- **`MomentOfInertia` — GHOST_UNPARSED**
  - Definition evidence: present in the target's own shipped packs, e.g. `src/manuspacks/resources/flans_content/modernwarfare/definitions/planes/A10.txt`, `AC130A.txt`, `AH1ZGreen.txt`, `C130HAirForce.txt` and further plane files (`MomentOfInertia 10.0` / `30.0` / `80.0`).
  - Parser investigation: the literal `MomentOfInertia` (in any casing) does not occur anywhere in the target Java sources — not in `DriveableType`/`PlaneType`/`VehicleType`, not in `common/driveables/physics/RealWorldSpecReader.java` (which reads `RealMassKg`, `RealWingAreaM2`, … but no inertia key), and not in the physics resolvers. `TypeFile` only exposes keys a `read*` call asks for, so the line is silently dropped.
  - Why ineffective: rotational inertia is derived inside `common/driveables/physics/*` from mass and geometry, never from this key. Confidence HIGH.
  - The same key is unparsed in the reference, so target packs carrying it inherited a no-op line.

No other target parameter resolved to GHOST_PARSED. The automated scan flagged ~75 further fields whose only consumers live inside `common/types/**`; each was inspected and every one feeds an externally called helper — for example `airborneSpreadModifier` → spread calculation at `common/types/GunType.java:1463`, `casingModelName` → `findModelClass` at `:965`, `guiTexturePath` → `loadGuiTextureLocation` (`ArmorBoxType.java:100`, `GunBoxType.java:200`), `fragType` → explosion scaling at `common/types/ShootableType.java:443-452`, and the `*SoundLength` fields → `registerSoundTimer`. Those are ACTIVE and are not reported.

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
- **`Mass` — GHOST_UNPARSED** — `vehicles`. The target *does* accept `Mass` (`common/types/DriveableType.java`).
- **`MomentOfInertia` — GHOST_UNPARSED** — `vehicles`.
- **`SwitchDelay` — GHOST_UNPARSED** — `guns`. The target accepts `SwitchDelay` (`GunType`).
- **`HasLandingGear` — GHOST_UNPARSED** — `planes`. The target accepts it (`PlaneType`).
- **`AddEmitter` — GHOST_UNPARSED** — `vehicles`. Reference parses `AddParticle` only; the target accepts `AddEmitter` (`DriveableType`).
- **`ExplodeParticleType` — GHOST_UNPARSED** — `bullets`, `grenades`. The target accepts it (`ShootableType`).
- **`DammageModifierPrimary` — GHOST_UNPARSED** — `vehicles`. The target accepts this misspelled key (`DriveableType`).
- **`AllowG43RelAttachments` — GHOST_UNPARSED** — `guns`. Pack-specific key, no parser.

Armour-box item shortnames (`GermanHelmet`, `SASBoots`, …) and recipe-grid rows were excluded as values, not parameter names.

## Reference parameters absent from target

Case-insensitive lexical comparison of the reference key set (parser names + aliases + real definition keys) against the target key set (including inherited parsers, aliases and auxiliary readers). Every candidate was re-verified by searching the full target sources for the literal in any casing.

| Reference key | Reference class(es) | Parsed / in defs | Reference evidence | Target parsers/aliases checked |
| --- | --- | --- | --- | --- |
| `AddType` | `GunBoxType` | parsed | `common/guns/boxes/GunBoxType.java:94` (alias of `AddGun`) | Target `GunBoxType` accepts `AddGun` only; `AddType` absent |
| `AddAltType` | `GunBoxType` | parsed | `GunBoxType.java:99` (alias of `AddAmmo`/`AddAltAmmo`) | Target accepts `AddAmmo`, `AddAltAmmo`, `AddAlternateAmmo` (`common/types/GunBoxType.java:116`); `AddAltType` absent |
| `NumGuns` | `GunBoxType` | parsed | `GunBoxType.java:61` | No literal anywhere in target; target boxes are page/entry driven (`Page`/`SetPage`) |
| `EnableReloadTime` | `DriveableType` | parsed | `DriveableType.java:713` | Target has `ReloadTime*` keys but no `EnableReloadTime` toggle |
| `GunLength` | `DriveableType` | parsed | `DriveableType.java:710` | No literal in target |
| `RecoilDistance` | `DriveableType` | parsed | `DriveableType.java:711` | Target has `RecoilTime` and the `Recoil*` multipliers; no `RecoilDistance` |
| `RadarDetectableAltitude` | `DriveableType` | parsed | `DriveableType.java:874` | No literal in target (no radar system) |
| `Stealth` | `DriveableType` | parsed | `DriveableType.java:875` | No literal in target |
| `TurretRotationSpeed` | `DriveableType` | parsed + `vehicles` defs | `DriveableType.java:262` | Target accepts `TurretOrigin`, `TurretOriginOffset`; no rotation-speed key |
| `NumWheels` | `DriveableType` | parsed + `planes` defs | `DriveableType.java:917` (in `preRead`) | Target derives wheels from `WheelPosition` lines; no `NumWheels` |
| `LegTrans` | `MechaType` | parsed | `common/driveables/mechas/MechaType.java:153` | Target `MechaType` accepts `LegLength`; no `*Trans` keys |
| `FrontLegLength` | `MechaType` | parsed | `MechaType.java:157` | Target has a single `LegLength` only |
| `RearLegLength` | `MechaType` | parsed | `MechaType.java:155` | Target has a single `LegLength` only |
| `FrontLegTrans` | `MechaType` | parsed | `MechaType.java:161` | No literal in target |
| `RearLegTrans` | `MechaType` | parsed | `MechaType.java:159` | No literal in target |
| `PenetratesEntities` | `GrenadeType` | parsed (ghost in ref) | `GrenadeType.java:185` | Target accepts `PenetratesBlocks` only |
| `ExplosionDamageVsDrivable` | `GrenadeType` | parsed (ghost in ref) | `GrenadeType.java:213` | Target uses `DeathExplosionDamageVsVehicle`/`…VsPlane`; the `Drivable` spelling is not accepted |
| `ToolUses` | `ToolType` | parsed | `common/tools/ToolType.java:99` (alias of `ToolLife`) | Target accepts `ToolLife`; the `ToolUses` alias is absent |
| `EUPerCharge` | `ToolType` | parsed | `common/tools/ToolType.java:101` | Target uses `RFDrawRate`/`UseRF`/`UseRFPower`; no EU key |
| `ItemID` | `ArmourType`, `BulletType`, `GunType`, `VehicleType` | defs only | `armorFiles`/`bullets`/`guns`/`vehicles` `.txt` | No literal in target |
| `GunBoxID` | `GunBoxType` | defs only | `boxes` `.txt` | No literal in target |
| `MeleeWeapon` | `GunType` | defs only | `guns` `.txt` | No literal in target (`MeleeDamage*` are different keys) |
| `MomentOfInertia` | `VehicleType` | defs only | `vehicles` `.txt` | No literal in target — also unparsed in the target's own packs (see GHOST_UNPARSED above) |

This table is lexical parser/definition compatibility only. Several rows have differently named target behaviour (for example `ToolUses` ↔ `ToolLife`, `EUPerCharge` ↔ `RFDrawRate`, `NumWheels` ↔ `WheelPosition` lines); they are still listed because the target does not accept the reference spelling as an alias.

## Uncertain parameters

- **Third-party content packs in `run/flan/*.zip`** (Plume, Rainfire5, TaP, WW1/WW2 armour packs, …) were not unpacked for key inventory; the target's own `src/manuspacks` definitions were used instead. Keys those packs carry that the target does not parse would be additional GHOST_UNPARSED findings. UNCERTAIN by omission, LOW impact on the parser inventory itself.
- **Category JSON injection** (`common/types/TypeFile.java:67-70`, `config/CategoryManager.java`) can inject arbitrary field/value pairs into a type's config map at load time. Any key a category file supplies is indistinguishable at the parser level from a `.txt` line, so a key could be "observed" only through category data and still be active. No ghost classification above depends on that path, but the category JSON key space was not exhaustively enumerated. UNCERTAIN, MEDIUM confidence in completeness.
- **`GunAnimationConfig`** (~115 `anim*` keys) is read into a separate animation config consumed by the legacy `com.flansmod.client.model.*` renderers. Individual `anim*` keys were not traced one-by-one to a render call site; they are excluded from ghost findings rather than reported as ghosts. UNCERTAIN, LOW risk.

## Summary — ghost findings

| Repo | Class / scope | Parameter | Status | Confidence |
| --- | --- | --- | --- | --- |
| Target | `DriveableType` | `OnRadar` | GHOST_PARSED | HIGH |
| Target | plane/vehicle defs | `MomentOfInertia` | GHOST_UNPARSED | HIGH |
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

Counts: target — 1 GHOST_PARSED, 1 GHOST_UNPARSED. Reference — 18 GHOST_PARSED, 11 GHOST_UNPARSED.

## Summary — reference keys absent from target

| Key | Reference class(es) | Parsed | In defs | Confidence |
| --- | --- | --- | --- | --- |
| `AddType` | `GunBoxType` | yes | no | HIGH |
| `AddAltType` | `GunBoxType` | yes | no | HIGH |
| `NumGuns` | `GunBoxType` | yes | no | HIGH |
| `EnableReloadTime` | `DriveableType` | yes | no | HIGH |
| `GunLength` | `DriveableType` | yes | no | HIGH |
| `RecoilDistance` | `DriveableType` | yes | no | HIGH |
| `RadarDetectableAltitude` | `DriveableType` | yes | no | HIGH |
| `Stealth` | `DriveableType` | yes | no | HIGH |
| `TurretRotationSpeed` | `DriveableType` | yes | yes (`vehicles`) | HIGH |
| `NumWheels` | `DriveableType` | yes | yes (`planes`) | HIGH |
| `LegTrans` | `MechaType` | yes | no | HIGH |
| `FrontLegLength` | `MechaType` | yes | no | HIGH |
| `RearLegLength` | `MechaType` | yes | no | HIGH |
| `FrontLegTrans` | `MechaType` | yes | no | HIGH |
| `RearLegTrans` | `MechaType` | yes | no | HIGH |
| `PenetratesEntities` | `GrenadeType` | yes (ghost) | no | HIGH |
| `ExplosionDamageVsDrivable` | `GrenadeType` | yes (ghost) | no | HIGH |
| `ToolUses` | `ToolType` | yes | no | HIGH |
| `EUPerCharge` | `ToolType` | yes | no | HIGH |
| `ItemID` | `ArmourType`, `BulletType`, `GunType`, `VehicleType` | no | yes | HIGH |
| `GunBoxID` | `GunBoxType` | no | yes | HIGH |
| `MeleeWeapon` | `GunType` | no | yes | HIGH |
| `MomentOfInertia` | `VehicleType` | no | yes | HIGH |

Total: **23** reference parameter names with no target parser or alias.

## Areas requiring deeper audit

- **Bundled third-party packs** (`run/flan/*.zip`): unpack and re-run the definition-key reconciliation to find GHOST_UNPARSED keys reaching real users of this build.
- **Category JSON key space** (`config/CategoryManager.java` + `TypeFile.addCategoryConfigMap`): keys injected there bypass the `.txt` inventory entirely; enumerating the shipped category files would harden both the target key set and the ghost classification.
- **`GunAnimationConfig` `anim*` keys**: ~115 keys whose consumers live in the legacy `com.flansmod.client.model.*` renderers; per-key consumer tracing was not performed, so individual dead animation keys may exist.
- **Reference sound-registering ghosts** (`FlareSound`, `Lock*Sound`, `PlaceSound*`, `ReloadSound*`): these have a real side effect (sound registration) even though the stored value is unused. If porting parity is judged by "the sound plays", they are reference bugs rather than features to reproduce.
