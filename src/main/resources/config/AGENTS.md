# Built-in Category Definitions

These rules apply to the built-in `*_categories.json` files in this directory.
They supplement the repository-level `AGENTS.md`.

## Scope and sources of truth

* Check the current branch and `gradle.properties` before changing categories.
* These files are shipped defaults. At runtime they are copied to
  `config/flansmodultimate/default/`; do not edit runtime copies under `run/`.
* A category applies only to the definition type named by its file. For example,
  gun short names belong in `gun_categories.json`, while magazine, cartridge and
  shell short names belong in `bullet_categories.json`.
* Match `items` against the sanitized content-pack `ShortName`, not the display
  name, file name or registry ID. Write short names in lowercase when adding new
  entries. Search every bundled and official pack for aliases and variants before
  deciding that the item list is complete.
* When a short name is ambiguous, or when a definition's `Name`, `Model`,
  `Description` and file name disagree about what the item represents, the
  localisation entry decides. Look up `item.flansmod.<shortname>` in the pack's
  `resources/assets/flansmod/lang/en_us.json`; treat that string as the correct
  title and identify the real vehicle, aircraft or weapon from it. Do not leave an
  item unclassified merely because the definition's own fields contradict each
  other, and do not guess from the model name when a localisation entry exists.
* Use `scripts/scanShortnames.py` to identify content-pack short names that are
  not yet listed in any supported category JSON file. It scans both bundled source
  packs and `run/flan` ZIP packs, then writes `missing_shortnames.csv` at the
  repository root. Review each reported entry for its type and variant before
  adding it; an unclassified definition is a research queue, not automatic proof
  that it should share another category's values.
* Treat the category label as documentation. Name it for the exact real weapon,
  cartridge, round, grenade, vehicle or aircraft variant whose figures were used.
* Follow the **Historical research and source policy** below for every real-world
  value. Identify the exact represented variant before researching properties.
* Do not invent a value merely to complete a category. Keep all researched values
  for one category compatible with the same model, ammunition loading, engine,
  configuration and test conditions.
* Consult the sibling `Flans-Mod-Ultimate-2.0.wiki` repository first, especially
  `Category-System.md`, `ConfigReference.md`, `Realistic-Vehicle-Physics.md`, and
  `Vehicle-Armour-and-Damage.md`. Confirm parser behavior in the relevant type
  class when adding a key not already used in these JSON files.

## Historical research and source policy

Treat historical research as part of the implementation. Do not select the first
plausible value returned by a search engine. Identify the exact represented model,
variant, ammunition load, engine, weapon mounting and relevant test condition before
writing values.

### Source priority

Prefer sources in roughly this order:

1. **Primary technical sources**

   * Original military manuals, firing tables, ammunition handbooks, acceptance
     trials, flight-test reports, manufacturer data sheets and official ordnance
     documents.
   * Scans or faithful reproductions of primary documents are acceptable.
   * Useful repositories include `archive.org`, `lonesentry.com`,
     `wwiiaircraftperformance.com` and primary documents reproduced by specialist
     sites.

2. **Specialist historical/technical references**

   * Sources whose subject matter closely matches the researched field and which
     distinguish variants, ammunition types and test conditions.
   * Prefer sources that identify or reproduce their own primary references.

3. **Broad reference works**

   * Museums, established historical databases, books, Wikipedia and similar
     references.
   * Good for dimensions, identification, production variants and sanity checks,
     but do not automatically prefer them over specialist or primary technical data.

4. **Simulation/game databases**

   * War Thunder, IL-2, DCS, GHPC and similar sources are fallback or
     cross-checking sources.
   * They may use calculated, normalized, balance-adjusted or internally modeled
     values rather than historical test results.
   * Do not replace an available historical figure with a game value merely because
     the game database is easier to search.
   * They are, however, the expected fallback for the gameplay-critical vehicle
     properties listed under **Completeness over omission** below. For those keys a
     War Thunder armour layout or reverse speed is preferable to leaving the key
     unset. Record such values as game-sourced in the task summary.

5. **Weak sources**

   * Forums, unsourced wikis, Reddit posts, search-result snippets, AI-generated
     summaries and copied specification lists are not authoritative sources.
   * They may be used to discover terminology or a better source, but normally not
     as the final basis for a researched value.

Source tier is more important than apparent numerical precision. A rounded value
from an original technical manual is preferable to an overly precise value from an
unsourced database.

### Completeness over omission

The general rule elsewhere in this document is to omit a property rather than
invent one. That rule still holds for ammunition penetration, explosive mass,
muzzle velocity, dispersion, fuse timing and aircraft performance.

It does **not** hold for the gameplay-critical driveable keys below, where an
approximate value is better than no value, because omission silently disables a
whole gameplay system rather than leaving one number slightly wrong:

* `ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth`;
* the hull armour set `ArmorFrontMm`, `ArmorRearMm`, `ArmorSideMm`,
  `ArmorBottomMm` (and `ArmorTopMm`) on any clearly armoured vehicle;
* the turret armour set `TurretArmorFrontMm`, `TurretArmorRearMm`,
  `TurretArmorSideMm`, `TurretArmorTopMm` on any definition with a turret;
* `PartArmorMm` for `leftTrack` and `rightTrack` on tracked vehicles;
* `RealMaxReverseSpeedKmh` on ground vehicles.

For these keys, work down the source ladder and stop at the first tier that yields
a usable figure: primary manual, specialist reference, broad reference, then game
database. Only if every tier fails may the key be left unset, and the task summary
must then say which vehicle and which key.

An approximation used this way is still an approximation. Never describe it as
historically verified, and always list it in the task summary under the values
that relied on a fallback source.

### Preferred specialist sources by subject

These domains are preferred starting points, not automatic authorities. Always
confirm that the page actually describes the represented variant and configuration.

#### Guns and small arms

Prefer:

* original weapon manuals and military technical manuals;
* `modernfirearms.net` for weapon specifications, actions, barrel lengths and
  cyclic rates;
* `forgottenweapons.com` for identifying variants, prototypes and mechanical
  differences;
* `navweaps.com` for autocannon, AA, naval and larger-calibre weapons.

For `MuzzleVelocity`, prefer a value for the exact weapon/barrel and normal service
load. A cartridge velocity measured from a different barrel length is not equivalent.

For `RoundsPerMin`, distinguish cyclic rate from practical or sustained rate.

For `Dispersion`, prefer an actual accuracy/dispersion figure for the weapon and
configuration. Do not infer dispersion from effective range.

#### Ammunition and cannon shells

Prefer:

* original firing tables, ammunition manuals and ordnance handbooks;
* `panzerworld.com` for WWII tank/anti-tank ammunition and documented penetration
  data, especially German ammunition;
* `quarryhs.co.uk` for heavy machine-gun, aircraft-cannon and autocannon ammunition;
* `navweaps.com` for autocannon and larger-calibre projectile data;
* `bulletpicker.com` for US ammunition, projectile construction, explosive filler
  and reproduced technical manuals.

Always distinguish:

* projectile mass from complete cartridge mass;
* explosive filler mass from total projectile mass;
* actual explosive mass from TNT equivalent;
* muzzle velocity for the exact projectile/load from generic cartridge velocity;
* measured penetration from calculated penetration.

For `PenetrationAt100m`, require the exact projectile or a defensibly equivalent
service projectile. Record normal-impact penetration where available. Never silently
treat penetration quoted at 30°, 60° or another obliquity as normal-impact
penetration.

Do not silently treat a value at 100 yards as a value at 100 metres. Penetration
cannot be converted between distances with a simple unit conversion.

If a source reports several penetration criteria, such as complete penetration,
partial penetration or different plate-quality standards, prefer the criterion most
consistent with neighboring categories and document the choice in the task summary.

#### Grenades and explosive ordnance

Prefer:

* original ammunition/EOD manuals;
* `bulletpicker.com`;
* `inert-ord.net`;
* `cat-uxo.com` as a broad identification and cross-checking database.

Research the explosive filling separately from total grenade weight.

For `Fuse`, distinguish nominal delay from a quoted tolerance range. Prefer the
nominal service value where one is documented.

Determine `FragType` from physical construction and intended fragmentation behavior,
not merely from the word "fragmentation" in the weapon name.

#### Tanks and ground vehicles

Prefer:

* original vehicle technical manuals, acceptance trials and manufacturer/military
  specifications;
* `panzerworld.com`, especially for German WWII vehicles, armour layouts and engine
  data;
* `tanks-encyclopedia.com`;
* `tank-afv.com`;
* other specialist references that identify the exact vehicle mark.

War Thunder may be used as a convenient fallback or cross-check for armour layouts,
forward/reverse speed, ammunition and engine figures, but historical technical
sources take precedence.

For armour thickness, plate slope and reverse speed the War Thunder wiki is an
expected fallback rather than a last resort: if no historical plate table or
gearbox figure can be found, take the game's layout instead of leaving the keys
unset. See **Completeness over omission**. Where the thickness is documented but
the slope is not, author the thickness alone; a missing slope is fine and is not a
reason to drop the plate.

Post-1950 vehicles with composite, spaced or reactive protection still get armour
values. Express them as an approximate rolled-homogeneous-armour equivalent in
millimetres for the represented face, from published RHAe estimates, and say in the
task summary that the figure is an RHA equivalent rather than a plate thickness.

Do not mix values from different production variants. In particular verify:

* combat/loaded versus empty weight;
* engine model and power rating;
* gross versus net engine output;
* mechanical hp versus metric PS;
* production changes in armour thickness;
* appliqué armour versus base plate thickness;
* governed road maximum versus theoretical or downhill maximum;
* gearbox-limited reverse speed.

For armour, prefer plate-by-plate technical drawings or tables. Preserve nominal
plate thickness and authored slope separately. Do not replace historical nominal
thickness with calculated line-of-sight thickness.

#### Aircraft

Prefer:

* original pilot notes, manufacturer performance reports and military flight-test
  documents;
* `wwiiaircraftperformance.com` for WWII flight-test documentation;
* `kurfurst.org` for original Bf 109/German aircraft documentation where applicable;
* `airvectors.net` and other specialist references for variant identification and
  secondary cross-checking.

IL-2, DCS and War Thunder may be used as fallback or sanity-check sources, not as
the first choice for historically measured performance.

Aircraft figures are particularly configuration-sensitive. Before combining values,
check:

* exact mark/subvariant;
* engine model;
* boost/manifold-pressure setting;
* fuel grade where relevant;
* propeller configuration;
* installed equipment;
* loaded/test mass;
* altitude;
* whether maximum speed is TAS or IAS;
* whether climb is sustained climb or an instantaneous/zoom figure.

For `RealMaxSpeedKmh`, prefer the documented rated maximum under a representative
service configuration. Do not use dive speed, structural limit speed or an
unqualified maximum from another engine/boost setting.

For `RealClimbRateMs`, prefer a sustained climb-rate measurement corresponding as
closely as practical to the selected operational mass and engine setting.

### Conflict resolution

When credible sources disagree:

1. Confirm that they describe the same variant, date, configuration and ammunition.
2. Check whether the difference is caused by units or conventions.
3. Check test conditions such as altitude, temperature, plate angle, plate quality,
   engine rating, ammunition lot or loaded mass.
4. Prefer a primary source for the exact represented configuration.
5. Otherwise prefer the more specialized and better-documented source.
6. Use a second independent source as a cross-check when the disagreement is
   material.
7. Do not average conflicting historical figures merely to obtain one number.
8. If no defensible choice can be made, omit the property rather than inventing a
   compromise.

A later source is not automatically better than a contemporary source, and a more
precise-looking number is not automatically more accurate.

### Derived and converted values

Values may be derived only when the transformation is deterministic and supported
by the source data.

Allowed examples include:

* kg to g;
* mph to km/h;
* knots to km/h;
* ft to m;
* hp/PS/kW conversions when the original power convention is known;
* seconds to Minecraft ticks;
* MOA to degrees;
* explosive filler to TNT equivalent when a defensible TNT-equivalence factor for
  the documented explosive composition is available.

Preserve the source's original unit/convention where a matching category property
exists. For example, if a source explicitly specifies metric horsepower, prefer
`RealEnginePowerPS` rather than converting it to mechanical horsepower without need.

Do not derive historical values using speculative formulas merely to fill a missing
field. In particular do not calculate missing penetration from calibre, kinetic
energy or another game's penetration model unless the task explicitly requests a
modeled estimate.

### Search and research workflow

For each new category:

1. Inspect the content-pack definition and identify what real item it is intended to
   represent. Resolve any disagreement between `Name`, `Model`, `Description` and
   file name using the pack's `en_us.json` localisation entry.
2. Determine the exact variant before researching numerical properties.
3. Search using the real designation and relevant technical terminology, for example
   `technical manual`, `firing table`, `flight test`, `ammunition handbook`,
   `penetration`, `projectile weight`, `bursting charge` or the original-language
   equivalent.
4. Open the actual source page or document. Do not take numerical values directly
   from search-result snippets.
5. Check units, variant and test conditions.
6. Cross-check important or suspicious values against another source.
7. Only then write the category properties.
8. Keep all values within one category internally compatible; do not assemble an
   artificial "best specifications" vehicle or weapon from mutually incompatible
   configurations.

When a primary source is available but difficult to interpret, prefer leaving a
property unresolved over substituting an unrelated easier-to-read value.

#### What may and may not be left uncategorized

Leaving an identifiable real item uncategorized is a last resort, not a safe
default. Work through this order before skipping anything:

* **Sub-variant not separately documented.** Do not skip the item. Base it on the
  nearest documented sibling variant, adjust the properties the sub-variant
  actually changes, and name the category for what you actually authored, for
  example `Junkers Ju 87 B-2 (D-series airframe figures)`. Say in the task summary
  which figures were carried over. A Ju 87 G-1 or an F6F-5N is a real aircraft with
  real numbers; producing nothing for it is worse than producing a close one.
* **Conflicting definition fields.** Resolve with the localisation entry, then
  categorize.
* **Post-1950 or otherwise modern.** Categorize it; use RHA-equivalent armour and
  manufacturer or published figures.
* **Semi-historical, "what-if", prototype or paper design.** Categorize it when a
  documented design study, prototype trial or credible reconstruction gives
  figures, and label the category so the status is obvious. Approximate from the
  chassis or airframe it is based on when only that is documented.
* **Purely fictional, sci-fi/fantasy, joke or gameplay-only items.** These may be
  skipped. Record them and the reason in the task summary.

Only genuinely unidentifiable items — no localisation entry, no recognizable
designation, no usable model — are left uncategorized on identity grounds.

### Research traceability

Do not add comments or citation metadata to the category JSON unless the schema is
explicitly extended to support it.

For research tasks, however, keep enough source provenance during the task to audit
the result. In the final task summary, report the principal sources used and call out
any values that relied on:

* a fallback/game source;
* conversion from another unit;
* TNT-equivalent conversion;
* a disputed or approximate historical figure;
* a source describing a slightly different configuration.

Do not claim that a value is historically verified when its only source is a game or
an unsourced secondary database.

## JSON and category behavior

Use this shape:

```json
"Human-readable category name": {
    "properties": {
        "ConfigKey": 123
    },
    "items": [
        "definition_shortname"
    ]
}
```

* Preserve the existing four-space indentation, blank line between categories,
  key spelling and nearby numeric style. Do not reformat the whole file.
* Order categories by their category label using case-insensitive alphanumeric
  order. In `bullet_categories.json`, put the metric calibre first in a category
  label whenever it is known. Sort labels beginning with a numeric millimetre
  calibre by calibre, then by numeric case length when present, and then by
  case-insensitive alphanumeric order. Use case-insensitive alphanumeric order
  as the fallback for labels without a leading metric calibre.
* JSON numbers and strings are converted to legacy config-line values. Use quoted
  strings for booleans (`"true"` / `"false"`) and for a property containing
  several whitespace-separated arguments. Do not use raw JSON booleans in
  `properties`.
* Use an array when a repeatable property needs several lines, for example
  `AddRound` or `PartArmorMm`. Do not repeat the property name inside the value.
* Single-value properties normally use the last applied line and therefore
  override the content-pack definition. Repeatable properties accumulate. Check
  the parser before assuming which behavior a new property has.
* Avoid putting one short name in categories that set conflicting values. If a
  variant shares most values, use `exceptions` to exclude only the differing
  property, or create a separate exact-variant category.
* `exceptions` maps a property name to excluded item short names. It affects only
  that property in that category; another category may still apply the property.
* Do not add legacy and modern aliases for the same concept to one category. Some
  readers intentionally give one alias precedence, and property-map iteration
  must not be relied upon to choose between contradictory values.

## Guns: `gun_categories.json`

For each gun, research and normally define all three existing core properties:

| Property         | Unit                    | What to research                                                                                                                                                                                                                                                        |
| ---------------- | ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MuzzleVelocity` | metres per second (m/s) | Velocity of the normal service load from the represented barrel length. Do not copy a cartridge's fastest published velocity when the weapon has a shorter barrel.                                                                                                      |
| `RoundsPerMin`   | rounds per minute (RPM) | Cyclic rate for automatic weapons. For manually operated or semi-automatic weapons, use a credible practical/mechanical firing rate consistent with neighboring categories, not the magazine capacity.                                                                  |
| `Dispersion`     | degrees                 | Angular shot spread. Convert minutes of angle with `degrees = MOA / 60`; do not enter an MOA number directly. Keep shotgun values representative of the whole shot pattern and normal guns representative of the base weapon, without movement or attachment modifiers. |

Important distinctions:

* Gun `MuzzleVelocity` supplies the launch speed and is internally converted to
  blocks/tick by dividing by 20. The ammunition's projectile `Mass` combines with
  this speed for kinetic damage.
* `RoundsPerMin` takes precedence over legacy `ShootDelay` when nonzero. The
  internal delay is `1200 / RPM` ticks. Treat 1200 RPM as the maximum supported
  ordinary rate because gun updates occur on Minecraft's tick cadence.
* Do not use a gun category to describe cartridge mass, explosive fill or armour
  penetration; those belong to the bullet/ammunition category.
* When several content-pack definitions are skins or aliases of the exact same
  weapon, they may share a category. Split different barrel lengths, actions,
  marks or calibres when any researched core property differs materially.

## Ammunition: `bullet_categories.json`

First classify every new entry as exactly one of the three patterns below. Do not
blur these patterns merely because all three live in the bullet file.

### 1. Gun ammunition

This is an ordinary cartridge or magazine item fired by a gun whose definition
already selects its ammo. Normally define:

| Property            | Unit                                     | Importance                                                                                                                                                                                                                      |
| ------------------- | ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Mass`              | grams (g)                                | Required research: projectile/bullet mass only, not the complete cartridge or loaded magazine.                                                                                                                                  |
| `FallSpeed`         | dimensionless gravity multiplier         | Normally `1.0`. This is a gameplay gravity factor, not m/s or m/s²; research is not applicable unless deliberately tuning unusual projectiles.                                                                                  |
| `PenetrationAt100m` | millimetres (mm)                         | Research for armour-piercing and heavy anti-materiel rounds: perpendicular penetration at 100 m, preferably in comparable RHA. The current armour gate uses this authored value at every range despite the historical key name. |
| `ExplosiveMass`     | kilograms of TNT equivalent (kg TNT eq.) | Define only for explosive ammunition. Research explosive filler mass and composition; convert to TNT equivalent when the source permits. Never put projectile mass here.                                                        |
| `FlakParticles`     | particle count                           | Visual legacy flak-particle count, not fragment count or explosive mass. Small-arms ball ammunition normally uses `0`; it is not a real-world value to research.                                                                |

* Obtain ordinary muzzle velocity from the matching gun category, not the ammo
  category, because the same cartridge changes velocity with barrel length.
* Use one category for equivalent items containing the same projectile/load even
  when magazine capacities differ. Split tracer, AP, incendiary, subsonic or
  explosive loadings when their mass, penetration or explosive behavior differs.
* Ordinary gun ammunition should not gain `AddRound`, `AddToAmmoGroup` or
  `MuzzleVelocity` unless the content design intentionally uses one of the cannon
  patterns below.

### 2. Small-cannon/autocannon ammunition with mixed rounds (`AddRound`)

Use this pattern only when one ammunition item contains multiple round types and
its bullet definition has `RoundsPerItem > 1`. Each array element is one repeated
legacy config line:

```json
"AddRound": [
    "AP 1 162 0 800 45",
    "HE 2 135 0.016 835 0"
]
```

The exact positional format is:

```text
<name> <count> <massG> [explosiveMassKgTntEq] [muzzleVelocityMps] [penetrationAt100mMm]
```

* `name`: one token only; use a clear round abbreviation without spaces.
* `count`: positive integer number of consecutive shots in the repeating belt or
  feed pattern. Research the historical/service belt composition where possible.
* `massG`: projectile mass in grams, not case/cartridge mass.
* `explosiveMassKgTntEq`: kilograms TNT equivalent; use `0` for non-explosive
  rounds.
* `muzzleVelocityMps`: metres per second for that specific projectile/load.
* `penetrationAt100mMm`: millimetres at normal impact; use `0` only when the round
  truly has no modeled armour penetration or no defensible value is intended.

Every round should normally provide all five fields after the name (six tokens in
total) so zero values are deliberate and the belt is auditable. `AddRound` lines
accumulate with lines already present in the definition or another category, so
inspect the affected ammo definitions and avoid accidentally appending a second
belt. Do not also use top-level `Mass`, `ExplosiveMass`, `MuzzleVelocity`, or
`PenetrationAt100m` as a substitute for per-round stats; the active shot reads its
`AddRound` stats.

### 3. Individual cannon shells (`AddToAmmoGroup`)

Use one category per specific shell type (AP, APCR, HE, HEAT, and so on). The shell
item defines its own stats and joins the cannon family selected by a gun, AA gun,
vehicle or aircraft with `UseAmmoGroup`:

```json
"75mm Pzgr.39": {
    "properties": {
        "AddToAmmoGroup": "75mm KwK/PaK 40",
        "Mass": 6800,
        "MuzzleVelocity": 770,
        "FallSpeed": 1.0,
        "ExplosiveMass": 0.029,
        "PenetrationAt100m": 143
    },
    "items": [
        "shell_shortname"
    ]
}
```

Research and normally define:

* `AddToAmmoGroup`: no unit. Use the exact canonical cannon-family name expected
  by `UseAmmoGroup`. Matching is case-insensitive and names may contain spaces.
  Confirm that the consuming weapon/driveable actually uses the same group.
* `Mass`: complete fired projectile/shell mass in grams, excluding the cartridge
  case and propellant charge. Convert kilograms to grams by multiplying by 1000.
* `MuzzleVelocity`: metres per second for this shell from the represented gun.
* `FallSpeed`: dimensionless gravity multiplier; normally `1.0`.
* `ExplosiveMass`: kilograms TNT equivalent of the bursting charge. Omit for a
  genuinely inert projectile; never use total shell mass.
* `PenetrationAt100m`: millimetres at 100 m and normal impact. Research the exact
  shell/gun combination and identify whether a source quotes 0° from normal or
  an oblique test before using it.
* `FragType`: optional enum for intentional fragmentation behavior. When used for
  an explosive cannon shell, choose the supported value that describes the
  construction (usually `HE_SHELL`) rather than guessing numeric frag tuning.

Do not model individual selectable shells as an `AddRound` belt. Conversely, do
not put a mixed autocannon belt into an ammo group as if the belt's AP and HE
components were separately selectable items.

## Grenades: `grenade_categories.json`

Research and normally define:

| Property        | Unit                                  | Guidance                                                                                                                                                                            |
| --------------- | ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ExplosiveMass` | kilograms TNT equivalent (kg TNT eq.) | Research filler mass and composition, not total grenade mass. Convert to TNT equivalent where defensible.                                                                           |
| `FragType`      | enum, no unit                         | Choose by casing/design: `LOW_FRAG`, `STD_FRAG`, `SLEEVE_FRAG`, `HIGH_FRAG`, `IED_SHRAPNEL`, `HE_SHELL`, `GP_BOMB`, `THICK_CASE`, or `AIRBURST_AP`. `DEFAULT` opts out of a preset. |
| `Fuse`          | ticks (20 ticks = 1 second)           | Research nominal delay and multiply seconds by 20. Omit for impact, proximity, mine or otherwise non-timed behavior rather than inventing a timer.                                  |

Keep explosive filler separate from the grenade's total mass. A fragmentation
sleeve changes `FragType`; it does not automatically change `ExplosiveMass`.

## Ground vehicles: `vehicle_categories.json`

Use figures for the exact vehicle mark and configuration. Prefer loaded/combat
mass because the other researched performance figures normally describe an
operational vehicle; do not mix empty mass with loaded speed or a later engine.

Core real-world propulsion research:

| Property                                                                     | Unit                                                         | Role                                                                                                                                                                          |
| ---------------------------------------------------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `RealMassKg`                                                                 | kilograms (kg), finite and > 0                               | Required for the complete ground profile and normalized health. This is not legacy vehicle `Mass`.                                                                            |
| `RealMaxSpeedKmh`                                                            | kilometres per hour (km/h), finite and > 0                   | Required for the complete ground profile. Use governed road maximum for the represented setup, not an exceptional downhill figure.                                            |
| exactly one of `RealEnginePowerKw`, `RealEnginePowerHp`, `RealEnginePowerPS` | kW, mechanical hp, or metric PS respectively, finite and > 0 | Required for the complete ground profile. Preserve the source's actual horsepower convention; `1 hp = 0.745699872 kW`, `1 PS = 0.73549875 kW`. Do not define several aliases. |
| `DriveType`                                                                  | enum, no unit                                                | Research `RWD`, `FWD`, `AWD`, or `TRACKED`. Applies independently and contributes traction.                                                                                   |
| `RealMaxReverseSpeedKmh`                                                     | km/h, finite and > 0                                         | Research gearbox-limited reverse speed. Applies independently. Expected on every ground vehicle; fall back to a game-database figure rather than omitting it.                 |

The realistic ground propulsion profile activates only when mass, maximum speed
and one engine-power value are all valid. A partial set leaves propulsion on the
legacy model, so do not add a half-researched profile accidentally.

Other important properties:

* `UseRealisticVehicleHealth`: quoted boolean. **Always set `"true"`.** It needs a
  valid `RealMassKg`, which every category here already defines, plus positive
  authored hitbox-health weights. The mod falls back to the authored health and
  logs a warning when a definition has none, so enabling it is safe; still check
  the definitions so the warning is expected rather than a surprise.
* `ReadWeaponsFromGunTypes`: quoted boolean. **Always set `"true"`.** Mounted
  weapon stats should be derived from the gun definition rather than duplicated in
  the driveable definition, so that a gun researched in `gun_categories.json` is
  the single source of delay, speed, spread and damage for every vehicle carrying
  it.
* `ShootDelayPrimarySeconds`: seconds. Use only when the vehicle must override the
  primary weapon cadence; prefer researching sustained reload/cycle time for the
  represented mounting. It has priority over `RoundsPerMinPrimary` and tick delay
  aliases and is converted at 20 ticks per second.
* `UseAmmoGroup`: no unit. Use the exact group created by individual shell
  categories' `AddToAmmoGroup`; confirm every intended shell joins it. It is a
  repeatable property, so use an array for multiple cannon families.
  Every vehicle with a real main gun should get a `UseAmmoGroup` whenever a group
  for that gun already exists in `bullet_categories.json`. Read the current
  `AddToAmmoGroup` values out of that file first and match one exactly; do not
  invent a group name that no shell joins, and do not attach a vehicle to a group
  for a different gun just to have one. If the vehicle's gun has no group yet,
  leave `UseAmmoGroup` unset and note the missing cannon family in the task
  summary, since creating it means adding shell categories to
  `bullet_categories.json`.
* `RealDraftM`: metres, finite and > 0. Research for boats/floating vehicles; it
  applies independently and requires the definition to float.

Armour research uses nominal rolled-homogeneous-equivalent thickness in
millimetres, optionally followed by the plate slope in degrees:

```json
"ArmorFrontMm": "100 9"
```

* Hull keys are `ArmorFrontMm`, `ArmorRearMm`, `ArmorSideMm`, `ArmorTopMm`, and
  `ArmorBottomMm`; turret keys add the `Turret` prefix and also include bottom.
* Format is `<thicknessMm> [slopeDeg]`. Thickness must be non-negative; slope must
  be from `0` through `89` degrees. Confirm the repository's angle convention
  before converting a source that measures from horizontal instead of vertical.
* Research each face separately. Do not fill unknown faces by copying frontal
  armour. An explicit `0` means known and intentionally unarmoured; omission means
  unspecified.
* Every clearly armoured vehicle must define `ArmorFrontMm`, `ArmorRearMm`,
  `ArmorSideMm` and `ArmorBottomMm`, and `ArmorTopMm` where the vehicle is not
  open-topped. Every definition that has a turret must additionally define
  `TurretArmorFrontMm`, `TurretArmorRearMm`, `TurretArmorSideMm` and
  `TurretArmorTopMm`. These are not optional: an approximate face from a broad
  reference or from the War Thunder wiki is required rather than an omission. Only
  the slope may be dropped when it is genuinely unknown.
* `ArmorSideMm` applies to both sides. Split variants if left and right layouts
  cannot honestly share one value.
* `PartArmorMm` is repeatable and formatted `"<part> <thicknessMm>"`. It overrides
  semantic hull/turret armour for that named driveable part. Verify the part name
  in the affected definitions; this format has no slope field.
* Author `PartArmorMm` for `leftTrack` and `rightTrack` on every tracked vehicle,
  using the track/running-gear thickness or a reasonable approximation of it, so
  that track hits are not resolved against hull side armour. Confirm both part
  names exist in the affected definitions before adding them.

## Aircraft: `plane_categories.json`

Use the exact aircraft mark, engine, boost setting and loading condition. Prefer
normal loaded/operational mass unless a source and the category label explicitly
identify another condition.

Research and normally define the complete aircraft profile:

| Property                                                             | Unit                                       | Notes                                                                                                                                                         |
| -------------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `RealMassKg`                                                         | kilograms (kg), finite and > 0             | Loaded/operational mass consistent with the performance figures.                                                                                              |
| `RealMaxSpeedKmh`                                                    | kilometres per hour (km/h), finite and > 0 | State-dependent in real sources; use a representative rated maximum for the exact variant, not a dive limit.                                                  |
| `RealWingSpanM`                                                      | metres (m), finite and > 0                 | Physical span of the represented wing configuration.                                                                                                          |
| `RealWingAreaM2`                                                     | square metres (m²), finite and > 0         | Planform/reference wing area, not span squared or legacy `WingArea`.                                                                                          |
| one of `RealEnginePowerKw`, `RealEnginePowerHp`, `RealEnginePowerPS` | kW, mechanical hp, or metric PS            | Use for piston and turboprop aircraft. Preserve the source's power unit.                                                                                      |
| `RealEngineThrustKn`                                                 | kilonewtons (kN), finite and > 0           | Use instead of shaft power for jets. Do not confuse kN with N or kgf.                                                                                         |
| `RealClimbRateMs`                                                    | metres per second (m/s), finite and > 0    | Important calibration input but not required for profile activation. Use sustained climb for the represented loading, not an instantaneous zoom-climb figure. |

The realistic aircraft profile activates only with valid mass, maximum speed,
span, area, and either engine power or thrust. `RealClimbRateMs` is optional.
Defining both power and thrust is allowed but normally signals mixed or unclear
research; aircraft use thrust when both exist.

As with vehicles, always set quoted `ReadWeaponsFromGunTypes` and
`UseRealisticVehicleHealth` to `"true"`. Normalized health needs a valid
`RealMassKg` and positive authored hitbox-health weights; inspect the definitions
so a missing-weight warning is expected rather than a surprise.

Aircraft have no armour keys, so the completeness rule that applies to them is
simply that a recognizable aircraft should end up with a category. Where the pack
names a sub-variant whose performance is not separately documented, build it from
the nearest documented mark, name the category for what was actually authored, and
record the substitution in the task summary.

## Validation checklist

After every category edit:

1. Parse every changed JSON file with a strict JSON parser.
2. Search the source definitions for every added `items` short name and confirm it
   belongs to the file's type. Check aliases, case and variant differences.
   Run `scripts/scanShortnames.py` when looking for definitions still missing from
   categories, then review its `missing_shortnames.csv` output rather than adding
   every listed item mechanically.
3. For ammo groups, verify both directions: each individual shell has the intended
   `AddToAmmoGroup`, and the consuming gun/AA gun/driveable has the matching
   `UseAmmoGroup`.
4. For `AddRound`, verify `RoundsPerItem > 1`, the positional fields and the total
   repeating belt count. Check that no existing definition/category contributes
   unintended extra `AddRound` lines.
5. Check unit conversions explicitly, especially kg versus g, hp versus PS, TNT
   equivalent versus filler mass, seconds versus ticks, and MOA versus degrees.
6. For driveable categories, check the mandatory keys of **Completeness over
   omission**: `ReadWeaponsFromGunTypes` and `UseRealisticVehicleHealth` are
   `"true"` everywhere; every armoured vehicle has the four hull faces plus a top
   unless open-topped; every turreted definition has the four turret faces; every
   tracked vehicle has `PartArmorMm` for `leftTrack` and `rightTrack`; every ground
   vehicle has `RealMaxReverseSpeedKmh`. List any deliberate exception, with the
   vehicle and key, in the task summary.
7. Review research consistency: verify that values assigned to each category refer
   to the same real-world variant/configuration. Re-check any value sourced only
   from a game, broad reference, conversion, approximation or disputed historical
   figure, and mention such cases in the task summary.
8. Confirm the categories are still in the documented order after the edit, and
   that no identifiable real item was left uncategorized for a reason that
   **What may and may not be left uncategorized** rules out.
9. Review the scoped diff, run focused tests if parser behavior was touched, run
   `git diff --check`, and run a full Gradle build only when the repository-level
   build rules require it. Pure JSON/default-data additions at minimum need strict
   JSON parsing and relevant targeted validation.
10. Consider whether the local wiki needs updating. New data entries usually do
    not; a new supported property, format, enum, unit or workflow does.
