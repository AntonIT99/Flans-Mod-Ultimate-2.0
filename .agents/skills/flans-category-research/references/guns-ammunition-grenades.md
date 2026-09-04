# Guns, Ammunition, and Grenades

Read this reference when editing `gun_categories.json`, `bullet_categories.json`,
or `grenade_categories.json`.

## Guns

For each identifiable gun, research and normally define:

| Property | Unit | Requirement |
| --- | --- | --- |
| `MuzzleVelocity` | metres per second | Normal service load from the represented barrel. A cartridge velocity from another barrel length is not equivalent. |
| `RoundsPerMin` | rounds per minute | Cyclic rate for automatic weapons; credible practical/mechanical rate for manual or semiautomatic weapons. Never use magazine capacity. |
| `Dispersion` | degrees | Actual angular accuracy/spread for the configuration. Convert MOA with `degrees = MOA / 60`; do not infer it from effective range. |

Keep shotgun dispersion representative of the full shot pattern and ordinary gun
dispersion representative of the base weapon without movement or attachment
modifiers. `MuzzleVelocity` is internally divided by 20 to obtain blocks/tick.
`RoundsPerMin` overrides legacy `ShootDelay`; its delay is `1200 / RPM` ticks.
Treat 1200 RPM as the maximum ordinary supported rate because gun updates occur on
the Minecraft tick cadence.

Gun `MuzzleVelocity` combines with ammunition projectile `Mass` for kinetic damage;
keep the two categories consistent with the same service loading.

Projectile mass, explosive filler, and penetration belong to ammunition, not gun
categories. Exact aliases and skins may share one gun category; split materially
different calibres, marks, actions, or barrel lengths.

## Ammunition: classify the content pattern first

Every bullet category is exactly one of:

1. Ordinary gun ammunition selected by a gun definition.
2. A mixed small-cannon/autocannon belt represented by `AddRound`.
3. An individually selectable cannon shell or missile joined through
   `AddToAmmoGroup`.

Inspect `Shell True`, `Missile True`, `WeaponType`, `RoundsPerItem`, all consuming
guns/vehicles/aircraft, and neighboring definitions before choosing a pattern.

### Ordinary gun ammunition

Every category requires:

- `Mass`: projectile/bullet mass in grams, never complete cartridge, case,
  propellant, or loaded-magazine mass.
- `FallSpeed: 1.0` for ordinary ballistic projectiles. Omit it, or use a separately
  justified value, only for a self-propelled projectile capable of sustaining its
  flight.

Also define when applicable:

| Property | Unit | Guidance |
| --- | --- | --- |
| `PenetrationAt100m` | millimetres | Perpendicular penetration at 100 m, preferably comparable RHA, for AP and heavy anti-materiel rounds. The game currently applies the authored value at every range despite the key name. |
| `ExplosiveMass` | kilograms TNT equivalent | Only for explosive ammunition; derive from filler mass/composition, never total projectile mass. |
| `FlakParticles` | particle count | Legacy visual count, not fragment count or a researched historical statistic. Ball ammunition normally uses `0`. |

Normal small-arms muzzle velocity belongs to the matching gun category because
barrel length changes it. Equivalent items with the same projectile/load may share
a category despite magazine capacity differences. Split tracer, AP, incendiary,
subsonic, explosive, or otherwise materially different loadings. Do not add
`AddRound`, `AddToAmmoGroup`, or ammo-level `MuzzleVelocity` unless the content
actually uses one of the cannon patterns.

### Shells and missiles: mandatory fields

A definition with `Shell True`, `Missile True`, `WeaponType Shell`, or
`WeaponType Missile` requires:

- projectile `Mass` in grams unless its statistics live in each `AddRound`;
- `MuzzleVelocity` in metres per second;
- normally `FallSpeed: 1.0`, except for a self-propelled projectile capable of
  sustained flight;
- `PenetrationAt100m` in millimetres at 100 m and normal impact whenever the
  intended value is nonzero;
- `ExplosiveMass` in kilograms TNT equivalent whenever the intended value is
  nonzero. Omit it for genuinely inert ammunition such as many APCR projectiles.

These are gameplay-critical. Continue down the source ladder rather than leaving a
nonzero value unset. A weaker configuration-compatible value is preferable to an
absent value; record the fallback and any conversion.

### Mixed autocannon belts with `AddRound`

Use `AddRound` only when one ammunition item contains multiple round types and its
definition has `RoundsPerItem > 1`. Each array element becomes one repeated legacy
line:

```json
"AddRound": [
    "AP 1 162 0 800 45",
    "HE 2 135 0.016 835 0"
]
```

The exact positional format is:

```text
<name> <count> <massG> <explosiveMassKgTntEq> <muzzleVelocityMps> <penetrationAt100mMm>
```

Every entry has exactly six tokens:

- `name`: one-token round abbreviation; exact designations such as
  `Pzgr.L'Spur` and `Sprgr.` are allowed.
- `count`: positive number of consecutive shots in the repeating pattern.
- `massG`: projectile mass in grams.
- `explosiveMassKgTntEq`: kilograms TNT equivalent; use `0` deliberately for an
  inert round.
- `muzzleVelocityMps`: exact projectile/load velocity in metres per second.
- `penetrationAt100mMm`: normal-impact millimetres at 100 m; use `0` only when no
  modeled armour penetration is intended.

Prefer a documented service belt. If none can be established, inspect every gun,
ground vehicle, and aircraft consuming the item. Bias an inferred belt toward AP
for principally ground-vehicle use and toward HE for principally aircraft use. A
repeating `1 AP : 1 HE` belt is an acceptable final gameplay default for genuine
shared use. Report inferred composition.

An `AddRound` category omits top-level `Mass`, `ExplosiveMass`, `MuzzleVelocity`,
and `PenetrationAt100m`; each active round supplies its own values. It still normally
has category-level `FallSpeed: 1.0`. Because `AddRound` accumulates with lines from
the definition and other categories, inspect every affected definition and avoid
accidentally appending a second belt. Validate `RoundsPerItem`, positional fields,
and the total repeating count.

### Individual cannon shells and ammunition groups

Use one category per selectable AP, APCR, HE, HEAT, smoke, missile, or other exact
round. The item supplies its own statistics and joins a compatible cannon family:

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

- `AddToAmmoGroup` is the exact canonical compatible-ammunition family consumed by
  `UseAmmoGroup`. Matching is case-insensitive and names may contain spaces. Group
  weapons only when ammunition is actually compatible, not merely similar in
  calibre.
- `Mass` is complete fired projectile/shell mass in grams, excluding case and
  propellant. Convert kg to g by multiplying by 1000.
- `MuzzleVelocity` is for this exact shell from the represented gun.
- `FallSpeed` is `1.0` for ballistic shells; sustained-flight projectiles are the
  exception.
- `ExplosiveMass` is kg TNT equivalent of the bursting charge and is omitted only
  for genuinely non-explosive rounds.
- `PenetrationAt100m` is millimetres at 100 m and normal impact, omitted only when
  zero is intentional.
- `FragType` is optional. For explosive cannon shells, use the supported enum that
  describes construction—usually `HE_SHELL`—rather than guessed numeric tuning.

Do not model an individually selectable shell as a mixed belt, or split a mixed
belt into ammo-group items when those rounds are not individually selectable.

`UseAmmoGroup` and `AddToAmmoGroup` are repeatable. Represent multiple values as a
JSON array so each becomes a separate legacy line:

```json
"UseAmmoGroup": [
    "75mm KwK/PaK 40",
    "Compatible 75mm Smoke"
]
```

Never place several group names after one legacy `UseAmmoGroup`; the parser treats
the entire remainder as one name. Validate membership in both directions and check
for duplicate ammunition after combining groups. Every vehicle with a real main
gun should use an existing matching group when its shells already define one. Do
not invent an empty group or attach an incompatible group; if no group exists,
leave it unset and report the missing cannon family unless the task includes adding
the shell categories.

## Grenades

Research and define:

| Property | Unit | Guidance |
| --- | --- | --- |
| `ExplosiveMass` | kilograms TNT equivalent | Mandatory for every grenade with an explosive charge. Use an exact TNT-equivalent figure when available; otherwise derive it from documented filler mass and composition using a defensible TNT-equivalence factor. Omit only when the grenade has no explosive charge. |
| `FragType` | enum | Choose from casing/design and intended fragmentation: `LOW_FRAG`, `STD_FRAG`, `SLEEVE_FRAG`, `HIGH_FRAG`, `IED_SHRAPNEL`, `HE_SHELL`, `GP_BOMB`, `THICK_CASE`, or `AIRBURST_AP`; `DEFAULT` opts out of a preset. |
| `Fuse` | ticks | Use nominal timed delay multiplied by 20. Omit for impact, proximity, mine, or other non-timed behavior and when timing cannot be established defensibly. |

Distinguish nominal fuse delay from tolerance range. A fragmentation sleeve changes
`FragType`; it does not automatically alter explosive mass. Determine fragmentation
from physical construction and intended behavior, not only from the word
"fragmentation" in a name. Never use total grenade weight as filler mass or assume
TNT when a different charge is documented; record the composition, conversion, and
any lower-tier source in the task report.
