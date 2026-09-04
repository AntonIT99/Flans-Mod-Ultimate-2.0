# Armor Categories

Read this reference before adding, changing, or auditing anything in
`armor_categories.json`.

Armor categories work differently from every other category type. Guns, vehicles,
and ammunition categories are **research products**: a real item is identified and
its documented physical values are transcribed. Armor categories are **balance
assignments**: research identifies only the material and construction of a piece,
which is then mapped onto a fixed ladder of tiers, and every number comes from the
lookup tables in this file.

Consequences:

- No armor value is ever researched, calculated, averaged, or invented. If a value
  is not a table lookup, it is wrong.
- Every armor item is eligible, including generic and fictional ones. Leaving a
  cosmetic uniform or a science-fiction trooper uncategorized is a gap, not a
  correct outcome. The exclusions in `research-policy.md` do not apply here.
- Category labels name a pragmatic group that shares statistics, not one exact
  historical model. Fine-grained categories exist only where a balance-relevant
  difference exists.

## Contents

- [Why harmonization is needed](#why-harmonization-is-needed)
- [The two axes](#the-two-axes)
- [Set shape and coverage](#set-shape-and-coverage)
- [Value tables](#value-tables)
- [Mandatory and optional properties](#mandatory-and-optional-properties)
- [Properties that must never appear](#properties-that-must-never-appear)
- [Alias and override traps](#alias-and-override-traps)
- [Grouping and naming](#grouping-and-naming)
- [Workflow](#workflow)
- [Worked examples](#worked-examples)
- [Validation checklist](#validation-checklist)
- [Completion report](#completion-report)

## Why Harmonization Is Needed

`Defence` is a fraction of all incoming damage, summed across the four worn slots
and clamped at `1.0`. Authored content ignores that budget entirely: 1386 armor
definitions across bundled and runtime packs set values from `0.0` to `0.7` with no
relation to what the item is. A Star Wars battle droid set of four pieces at `0.4`
sums to `1.6` and is clamped to complete invulnerability. Official WW2 cloth
uniforms give a flat 40% set reduction. Nothing else is harmonized at all:
`PenetrationResistance` and `BulletDefence` are effectively unused, and 220
definitions set `KnockbackModifier`, which has no effect.

The goal is that every armor item in every pack sits on one comparable scale, so
that a Stormtrooper, an Interceptor vest, and a lorica segmentata are balanced
against each other and against the kinetic penetration values of ammunition.

## The Two Axes

Every armor item is assigned exactly one **protection class** and one **ballistic
rating**. The two are independent, which is what lets pre-firearm and modern armor
coexist on one scale: a plate harness is heavily armored but useless against
bullets, and a soft aramid vest is the reverse.

### Protection Class (C0-C5)

How much physical protection the piece gives against everything: melee, blast,
fragments, falls, mob attacks. Drives `Defence`, `ArmorPoints`, `Toughness`, and
`MoveSpeedModifier`.

| Class | Name | Typical construction |
| --- | --- | --- |
| `C0` | Garment | Cloth, wool, canvas, linen. Uniforms, caps, webbing, robes, shoes, gloves, cosmetic-only items. |
| `C1` | Reinforced garment | Leather, padded cloth, gambeson, heavy greatcoats, flight and tanker suits, work boots. |
| `C2` | Light armor | Mail, scale, brigandine, steel combat helmets, WWII flak vests, ballistic nylon, light trooper shells. |
| `C3` | Medium armor | Plate harness, cuirass, lorica segmentata, aramid vests, composite combat helmets, standard sci-fi trooper armor. |
| `C4` | Heavy armor | Rifle-rated hard plates, full plate armor, heavy assault and elite trooper armor, light battle droids. |
| `C5` | Powered or exotic armor | Powered exoskeletons, Space Marine power armour, beskar, super-heavy droids, boss-tier equipment. |

### Ballistic Rating (B0-B5)

How well the piece specifically defeats projectiles. Drives `BulletDefence` and
`PenetrationResistance`.

| Rating | Name | Defeats |
| --- | --- | --- |
| `B0` | None | Nothing. Cloth and every purely cosmetic item. |
| `B1` | Incidental | Spent rounds only. Mail, leather, ancient and medieval plate, fantasy armor with no firearms in its setting. |
| `B2` | Fragmentation-rated | Fragments and low-velocity rounds. WWI/WWII steel helmets, flak vests, ballistic nylon. |
| `B3` | Pistol-rated | Handgun rounds. Aramid soft armor, composite combat helmets, light energy-weapon shells. |
| `B4` | Rifle-rated | Intermediate and most full-power rifle rounds. Hard plates, standard and elite trooper armor. |
| `B5` | Exotic | Nearly all small arms. Powered armour, beskar, super-heavy droids. |

### Pairing

Most items pair one step apart or exactly. Use these anchors:

| Item | Class | Rating |
| --- | --- | --- |
| Wool field uniform, field cap, webbing, robe | `C0` | `B0` |
| Leather jerkin, greatcoat, flight jacket, boots | `C1` | `B0` |
| Mail hauberk, scale armor | `C2` | `B1` |
| Roman legionary segmented cuirass, medieval plate harness | `C3` | `B1` |
| Full gothic plate, jousting harness | `C4` | `B1` |
| WWI/WWII steel combat helmet | `C2` | `B2` |
| WWII flak vest, ballistic nylon vest | `C2` | `B2` |
| Aramid soft vest, composite combat helmet | `C3` | `B3` |
| Plate carrier with rifle plates, modern assault kit | `C4` | `B4` |
| Standard sci-fi trooper armor | `C3` | `B4` |
| Elite sci-fi trooper, heavy assault armor | `C4` | `B4` |
| Powered armour, beskar, super-heavy droid | `C5` | `B5` |

A rating above `B1` requires that firearms or directed-energy weapons exist in the
item's setting. Never give a bronze cuirass a rifle rating because it looks thick.

## Set Shape And Coverage

This is the part most content gets wrong, and the reason a single per-slot table is
not enough. Three properties behave differently:

- `Defence`, `BulletDefence`, and `ArmorPoints` apply to the **whole body** and are
  summed across every worn piece. They must be budgeted by how much of the body the
  item represents.
- `PenetrationResistance` applies **only to the slot it occupies**. The head hitbox
  reads the helmet, the body and arm hitboxes read the chestplate, and the leg
  hitbox reads leggings plus boots. It is never scaled by coverage.

So determine the item's **set shape** first, then its **coverage**.

### Set Shapes

| Shape | Description | Coverage of each piece |
| --- | --- | --- |
| `full-set` | Four pieces designed to be worn together as one outfit. | Anatomical share of its slot |
| `partial-set` | Two or three pieces designed together, with the remaining slots deliberately left free. | Anatomical share of its slot |
| `repurposed slot` | A piece the pack put in a slot that does not match the body part, such as a cuirass in the boots slot so it can be worn over a chest uniform. | Anatomical share of the slot it **occupies**, not of the body part it depicts |
| `standalone` | One item that represents an entire protected body: a one-slot droid chassis, a one-slot creature or mecha shell, a single-item costume. | `0.50` |
| `multi-slot garment` | One item that visibly covers several anatomical regions but occupies one slot, such as a full-length coat or a coverall in the chest slot. | Sum of the shares it covers, capped at `0.70` |

**Coverage never exceeds the anatomical share of the occupied slot, except for the
two shapes that say so.** This is what keeps the summed budget bounded. A cuirass
worn in the boots slot is priced as a boots-slot piece even though it depicts a
torso, because the wearer still has a chest slot free for something else. Pricing
it as a torso would let one body be paid for twice.

### Anatomical Shares

| Slot | Share |
| --- | ---: |
| Helmet | `0.15` |
| Chestplate | `0.40` |
| Leggings | `0.30` |
| Boots | `0.15` |

A `partial-set` keeps the anatomical share of each piece and is simply less
protected overall. Do **not** renormalize a two-piece set up to `1.00`: an outfit
with no leg armor genuinely protects less, and renormalizing would let a two-piece
set outperform a four-piece one.

A `standalone` item is priced at half a body rather than a whole one. Its
`Defence`, `BulletDefence`, and `ArmorPoints` do apply to every hitbox no matter
which slot they came from, but it still leaves three slots equippable, and
`Defence` sums across slots and clamps at `1.0`. Pricing it at `1.00` lets a
mixed loadout reach the clamp and become literally invulnerable, which is the
original bug this whole exercise exists to remove. `0.50` keeps a standalone item
the strongest single piece in any pack while leaving the other three slots
harmless. Note in the report that such a category is not meant to be combined with
other armor.

`PenetrationResistance` is the one thing a `standalone` item cannot cover, because
it is read from the slot that matches the hitbox. A one-slot droid chassis in the
helmet slot resists penetration on head shots only; body and leg shots fall back to
the bare-skin value. This is a deliberate accepted limitation, not something to
compensate for by inflating the value. Never raise a `standalone` item's
`PenetrationResistance` above its ballistic-rating row to make up for the slots it
does not occupy.

A `multi-slot garment` sums only the regions it plausibly covers, for example
chest plus legs is `0.70`, or head plus chest is `0.55`.

### Applying Coverage

```text
Defence        = classFullBodyDefence   * coverage
BulletDefence  = ratingFullBodyDefence  * coverage
ArmorPoints    = round(classFullBodyArmorPoints * coverage)
PenetrationResistance = ratingMultiplier * unarmoredSlotValue    (coverage never applies)
```

Round `Defence` and `BulletDefence` to two decimals. Round `ArmorPoints` to the
nearest integer, with a minimum of `1` whenever the full-body value is above zero
and the coverage is above zero.

### Cross-Pack Slot Collisions

The same short name sometimes exists in two packs with different `Type` lines, so
one category has to serve both slots. Resolve it as follows and disclose it:

- Use a single dedicated category, never two, because a short name may appear in
  only one category.
- Set coverage from the **smallest** anatomical share among the colliding slots, so
  the budget is safe in every slot.
- Set `PenetrationResistance` from the **highest** baseline among the colliding
  slots, because a value below a slot's bare-skin baseline would make the piece
  worse than nothing there.
- If the item is `B0`, omit `PenetrationResistance` entirely. The runtime already
  defaults it to the bare-skin value of whichever slot the pack used, which is
  exactly the desired result and needs no compromise.

## Value Tables

For `full-set` and `partial-set` pieces the tables below are already multiplied by
the anatomical share, so read the value directly. For `standalone` and `multi-slot
garment` items, take the full-body column and multiply by the coverage.

### `Defence` By Protection Class

| Class | Full body | Helmet | Chestplate | Leggings | Boots | Full-set total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `C0` | `0.08` | `0.01` | `0.03` | `0.02` | `0.01` | `0.07` |
| `C1` | `0.16` | `0.02` | `0.06` | `0.05` | `0.02` | `0.15` |
| `C2` | `0.28` | `0.04` | `0.11` | `0.08` | `0.04` | `0.27` |
| `C3` | `0.40` | `0.06` | `0.16` | `0.12` | `0.06` | `0.40` |
| `C4` | `0.52` | `0.08` | `0.21` | `0.16` | `0.08` | `0.53` |
| `C5` | `0.66` | `0.10` | `0.26` | `0.20` | `0.10` | `0.66` |

No full set reaches the `1.0` clamp, and no combination of a standalone item and a
full set should be able to either. If a planned assignment would put a plausible
loadout above `0.85`, reduce the class rather than inventing an intermediate value.

### `BulletDefence` By Ballistic Rating

| Rating | Full body | Helmet | Chestplate | Leggings | Boots | Full-set total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `B0` | `0.02` | `0.00` | `0.01` | `0.01` | `0.00` | `0.02` |
| `B1` | `0.06` | `0.01` | `0.02` | `0.02` | `0.01` | `0.06` |
| `B2` | `0.12` | `0.02` | `0.05` | `0.04` | `0.02` | `0.13` |
| `B3` | `0.30` | `0.05` | `0.12` | `0.09` | `0.05` | `0.31` |
| `B4` | `0.48` | `0.07` | `0.19` | `0.14` | `0.07` | `0.47` |
| `B5` | `0.64` | `0.10` | `0.26` | `0.19` | `0.10` | `0.65` |

`BulletDefence` below `Defence` is normal and intended for `B0`-`B2`: a steel
helmet stops fragments, not rifle rounds. The runtime renormalizes correctly, so a
lower bullet value never produces more damage than the unarmored case.

### `PenetrationResistance` By Ballistic Rating

Never scaled by coverage. Never written below the `B0` row, which is the unarmored
value of the slot: a lower value would make the piece worse than bare skin.

| Rating | Multiplier | Helmet | Chestplate | Leggings | Boots |
| --- | ---: | ---: | ---: | ---: | ---: |
| `B0` | `1.00` | `1.00` | `1.00` | `0.65` | `0.35` |
| `B1` | `1.10` | `1.10` | `1.10` | `0.72` | `0.39` |
| `B2` | `1.25` | `1.25` | `1.25` | `0.81` | `0.44` |
| `B3` | `1.50` | `1.50` | `1.50` | `0.98` | `0.53` |
| `B4` | `1.90` | `1.90` | `1.90` | `1.24` | `0.67` |
| `B5` | `2.40` | `2.40` | `2.40` | `1.56` | `0.84` |

Leggings and boots are a matched pair whose values are summed for the leg hitbox,
which is why their unarmored shares are `0.65` and `0.35` rather than an even
split. A `partial-set` that has leggings but no boots is correctly weaker in the
legs.

Cross-checked against kinetic ammunition penetrating power, the thresholds behave
as follows. Threshold is `0.7 * PenetrationResistance`.

| Rating | Threshold | 9x19mm (0.70) | 5.56mm (1.04) | 7.62x51mm (1.30) | .50 BMG (2.21) |
| --- | ---: | ---: | ---: | ---: | ---: |
| `B0` | `0.70` | 100% | 100% | 100% | 100% |
| `B2` | `0.88` | 57% | 100% | 100% | 100% |
| `B3` | `1.05` | 36% | 98% | 100% | 100% |
| `B4` | `1.33` | 20% | 54% | 94% | 100% |
| `B5` | `1.68` | 11% | 30% | 53% | 100% |

### `ArmorPoints` And `Toughness` By Protection Class

| Class | Full body | Helmet | Chestplate | Leggings | Boots | Toughness |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `C0` | `0` | `0` | `0` | `0` | `0` | `0` |
| `C1` | `6` | `1` | `2` | `2` | `1` | `0` |
| `C2` | `10` | `2` | `4` | `3` | `2` | `0` |
| `C3` | `14` | `2` | `6` | `4` | `2` | `1` |
| `C4` | `18` | `3` | `7` | `5` | `3` | `2` |
| `C5` | `22` | `3` | `9` | `7` | `3` | `3` |

`Toughness` is the same for every piece of a class and is not scaled by coverage.

### `MoveSpeedModifier` By Protection Class

Applied to the chestplate and leggings only, so the penalty stays legible in
tooltips. Omit the property entirely for `C0` and `C1`.

| Class | Chestplate and leggings |
| --- | ---: |
| `C0` | omit |
| `C1` | omit |
| `C2` | `0.99` |
| `C3` | `0.98` |
| `C4` | `0.97` |
| `C5` | `0.95` |

For a `standalone` or `multi-slot garment` item of class `C2` or above, apply the
value once, on the single slot it occupies.

## Mandatory And Optional Properties

Every armor category writes all five of these, including zeros. Writing them
unconditionally is what makes harmonization deterministic: a category value is read
last and overrides the definition, so an omitted property silently leaves the
pack's unharmonized value in place.

| Property | Source |
| --- | --- |
| `Defence` | Protection class table, times coverage |
| `BulletDefence` | Ballistic rating table, times coverage |
| `PenetrationResistance` | Ballistic rating table, by slot, never scaled |
| `ArmorPoints` | Protection class table, times coverage |
| `Toughness` | Protection class table |

Optional, written only when the represented equipment genuinely has the feature:

| Property | Write it for |
| --- | --- |
| `MoveSpeedModifier` | Chestplate and leggings of `C2` and above |
| `JumpModifier` | Powered armor, jump packs, and equipment explicitly described as assisting movement |
| `SmokeProtection` | Gas masks, sealed NBC hoods, sealed powered-armor and droid helmets |
| `NightVision` | Actual night-vision or thermal devices, not merely dark visors |
| `Submarine` | Rebreathers, diving helmets, sealed suits rated for submersion |
| `FireResistance` | Fire-proximity, tanker, and heat-shielded suits |
| `NegateFallDamage` | Parachute rigs, jump packs, powered armor with described landing assistance |
| `EquipSound` | Only when the pack already ships a suitable sound |

`Durability` is deliberately left out of every armor category. With the default
`breakableArmor` setting, writing it makes clothing breakable, which is a server
policy decision rather than a property of the represented item.

`KnockbackModifier` and `KnockbackReduction` are never written. The runtime applies
the value as a `MULTIPLY_TOTAL` operation on a knockback-resistance attribute whose
base is zero, so it has no effect. 220 existing definitions set it pointlessly.

`Playermodel`, `Invisible`, `Hunger`, `Regenerate`, `OnWaterWalking`, and
`AddEffect` are gameplay effects rather than protection. Leave them to the pack.
Never add them to a category, and never remove them either.

## Properties That Must Never Appear

Identity and rendering belong to the content pack:

`Type`, `Model`, `ModelScale`, `Icon`, `ArmourTexture`, `ArmorTexture`, `Colour`,
`Name`, `ShortName`, `Description`, `Overlay`, `ItemID`, and any recipe key.

`Type` is the most dangerous of these. The texture path appends `_1` or `_2`
depending on the resolved slot, so overriding `Type` corrupts armor rendering.

## Alias And Override Traps

A category's properties are appended after the definition's own lines and, for
single-value properties, the last line read wins. The reader resolves several
aliases in a fixed order, so writing the wrong alias lets the pack win.

| Concept | Write this key | Reason |
| --- | --- | --- |
| Damage reduction | `Defence` | `Defense` is read after it, but no pack in the corpus uses `Defense`. Verify before assuming. |
| Vanilla armor points | `ArmorPoints` | Read after `DamageReductionAmount`, so it always wins. |
| Movement speed | `MoveSpeedModifier` | `Slowness` is read after it. No pack in the corpus uses `Slowness`. Verify before assuming. |

Two aliases are read after their primary key and **are** used by packs. If a
category ever needs to override them, write the alias, not the primary key:
`Playermodel` beats `Invisible`, and `Submarine` beats `WaterBreathing`.

The effective defence is `max(DamageReduction, Defence, OtherDefence)`. A category
therefore **cannot lower** `Defence` below a `DamageReduction` or `OtherDefence`
line already present in a definition. No definition in the current corpus uses
either alias, but a member that does must have all three written by the category to
the same value. Check this for every category.

## Grouping And Naming

### Grouping Rule

Two items belong in the same category when they resolve to the same **slot, set
shape, coverage, protection class, and ballistic rating**, and represent the same
kind of equipment. Group aggressively. Roughly a thousand of the 1386 armor items
are cosmetic clothing that all resolve to `C0`/`B0`, and they should land in a
handful of categories, not hundreds.

Split only when a balance-relevant difference exists, meaning a different class,
rating, coverage, or slot. Two helmets that differ only in nation, camouflage,
netting, or year belong together. A cloth field cap and a steel helmet do not,
because one is `C0`/`B0` and the other `C2`/`B2`.

Never mix slots in one category. Properties are injected identically into every
listed item, so a chest and a legs piece cannot share a category even when they are
halves of the same uniform. Split them and mark the slot in the label. The
`exceptions` map only removes a property from an item; it cannot give it a
different value.

### Naming Rule

```text
<Scope> <Equipment>[ (<Slot>)]
```

- `Scope` is the narrowest label that is true of every member: an era (`WW1`,
  `WW2`, `Modern`), a setting (`Star Wars`, `Warhammer 40k`), a culture
  (`Roman`), or `Generic` for cross-pack cosmetic baselines.
- `Equipment` names the kind of item and, where useful, the construction that
  determined the tiers, such as `Steel Combat Helmet` or `Wool Field Uniform`.
- Append the slot in parentheses whenever the same group spans more than one slot.

Examples:

```text
Generic Cloth Uniform (Chest)
Generic Cloth Uniform (Legs)
Generic Field Cap
Generic Leather Boots
WW2 Steel Combat Helmet
WW2 Wool Field Uniform (Chest)
Roman Legionary Segmented Cuirass
Modern Plate Carrier With Rifle Plates
Star Wars Clone Trooper Armour (Chest)
Star Wars Battle Droid Chassis
```

`Generic ` prefixed categories carry the cosmetic baseline for items with no
distinctive construction, and cluster together under the existing case-insensitive
alphanumeric ordering rule. They make no historical claim and may span packs,
eras, and settings freely.

Fictional categories use the in-universe name, which is already unambiguous. They
are not historical claims either, and their tiers come from narrative role, not
from research.

## Workflow

1. Run `scripts/scanShortnames.py` and filter `missing_shortnames.csv` to
   `category == armor`. Process bundled and official source packs before
   `run/flan` ZIP packs.
2. Work one pack at a time. Within a pack, sort candidates by model and texture
   family so that pieces of the same outfit are examined together.
3. For each candidate, establish identity from `item.flansmod.<shortname>` in the
   pack's `en_us.json`, falling back to `Name`, `Model`, `Description`, and file
   name. Strip colour codes such as `§c` from display names.
4. Read the definition's `Type` line to fix the slot.
5. Determine the set shape by looking for sibling definitions that share a model
   prefix, texture, or naming pattern across the four slots. A family that fills
   all four slots is a `full-set`; two or three is a `partial-set`; a lone item
   representing a whole body is `standalone`.
6. Research only the material and construction, using the source ladder in
   `research-policy.md`. Stop as soon as the construction is established. Do not
   research protection figures, standards, or thicknesses; they are not used.
7. Assign the protection class and ballistic rating from the anchor table.
8. Compute coverage and read the five mandatory values from the tables.
9. Search every bundled, official, and available runtime pack for the same
   equipment and add all matching short names to the same category. Cosmetic
   variants, skins, camouflage patterns, and national variants of one item belong
   together.
10. Add optional properties only where the equipment genuinely has the feature.
11. Insert the category in case-insensitive alphanumeric order, preserving
    four-space indentation and blank lines between categories.
12. Run the validation checklist below and rerun the scanner.

Do not stop after examples. Finish the pack or batch that was requested, decide
every ambiguous case on the best available evidence, and disclose the decision
rather than leaving the item uncategorized.

## Worked Examples

A four-piece WW2 outfit. The helmet is a separate `C2`/`B2` group; the cloth
pieces are `C0`/`B0`; the boots are leather, so `C1`/`B0`.

The helmet category also shows two things the workflow requires. Nation, year,
camouflage, and netting are not balance-relevant, so every WW2 steel combat helmet
in a pack shares one category. And membership spans packs: `ssh40` and
`ssh40amoeba` come from a runtime ZIP pack, not from the bundled one. Note also
that `44_SovietSSh40Helmet.txt` declares the short name
`44_SovietSummerObr43Helmet`; always take the short name from the definition, never
from the file name.

```json
"WW2 Steel Combat Helmet": {
    "properties": {
        "Defence": "0.04",
        "BulletDefence": "0.02",
        "PenetrationResistance": "1.25",
        "ArmorPoints": 2,
        "Toughness": 0
    },
    "items": [
        "44_americanm1helmetnetting",
        "44_americanm1helmetnonetting",
        "44_americanm1helmetmitchellcamo",
        "44_britishbrodiehelmet",
        "44_germanm42feldgraustahlhelm",
        "44_sovietsummerobr43helmet",
        "ssh40",
        "ssh40amoeba"
    ]
},

"WW2 Wool Field Uniform (Chest)": {
    "properties": {
        "Defence": "0.03",
        "BulletDefence": "0.01",
        "PenetrationResistance": "1.00",
        "ArmorPoints": 0,
        "Toughness": 0
    },
    "items": [
        "44_americanm1941combatfielduniformchest",
        "44_americanm1943combatfielduniformchest"
    ]
},

"WW2 Leather Combat Boots": {
    "properties": {
        "Defence": "0.02",
        "BulletDefence": "0.00",
        "PenetrationResistance": "0.35",
        "ArmorPoints": 1,
        "Toughness": 0
    },
    "items": [
        "44_americanboots",
        "44_britishboots",
        "44_germanboots",
        "44_japaneseboots"
    ]
}
```

A `standalone` droid chassis. One helmet-slot item represents the entire droid, so
coverage is `0.50` and the full-body column is halved. `PenetrationResistance`
still uses the helmet row unhalved, because it is never scaled by coverage and only
the head hitbox reads it.

```json
"Star Wars Battle Droid Chassis": {
    "properties": {
        "Defence": "0.26",
        "BulletDefence": "0.24",
        "PenetrationResistance": "1.90",
        "ArmorPoints": 9,
        "Toughness": 2,
        "MoveSpeedModifier": "0.97"
    },
    "items": [
        "battledroid",
        "battledroidcommander",
        "battledroidgeonosis",
        "battledroidmarine"
    ]
}
```

A `partial-set` of two pieces. The chassis occupies the helmet slot and the
deflector shield the chest slot, with legs and boots deliberately free; each keeps
its anatomical share, so the pair totals `0.34` rather than a full `C4` set's
`0.53`. The two shield colour variants share one category, because colour is not a
balance-relevant difference.

```json
"Star Wars Droideka Chassis": {
    "properties": {
        "Defence": "0.08",
        "BulletDefence": "0.07",
        "PenetrationResistance": "1.90",
        "ArmorPoints": 3,
        "Toughness": 2
    },
    "items": [
        "droideka"
    ]
},

"Star Wars Droideka Deflector Shield": {
    "properties": {
        "Defence": "0.26",
        "BulletDefence": "0.26",
        "PenetrationResistance": "2.40",
        "ArmorPoints": 9,
        "Toughness": 3,
        "MoveSpeedModifier": "0.95"
    },
    "items": [
        "droidekashield",
        "droidekashieldred"
    ]
}
```

A `multi-slot garment`: a full-length greatcoat worn in the chest slot that visibly
covers torso and legs, so coverage is `0.40 + 0.30 = 0.70` at `C1`/`B0`. Its
`Defence` is `0.16 * 0.70 = 0.11` and its `ArmorPoints` are `round(6 * 0.70) = 4`.

```json
"WW1 Wool Trenchcoat": {
    "properties": {
        "Defence": "0.11",
        "BulletDefence": "0.01",
        "PenetrationResistance": "1.00",
        "ArmorPoints": 4,
        "Toughness": 0
    },
    "items": [
        "1914frontoviktrenchcoat",
        "1914infanterietrenchcoat",
        "1914kuktrenchcoat",
        "1916frontoviktrenchcoat",
        "japanesetrenchcoat"
    ]
}
```

Numeric style follows the existing category files: decimals are quoted strings with
two decimal places so that trailing zeros survive, and integers are bare JSON
numbers.

## Validation Checklist

In addition to the shared checklist in `format-and-validation.md`:

1. Every category writes all five mandatory properties.
2. No category mixes armor slots. Cross-check each member's `Type` line against the
   slot the values were taken from.
3. No short name appears in two armor categories.
4. Every `PenetrationResistance` is greater than or equal to the `B0` value of its
   slot: `1.00` for helmets and chestplates, `0.65` for leggings, `0.35` for boots.
5. Every value is an exact table lookup, or a table value multiplied by a coverage
   and rounded as specified. Recompute each one.
6. Coverage is consistent with the declared set shape, and no `partial-set` has
   been renormalized.
7. Take the highest `Defence` of any category that can occupy each of the four
   slots, across the whole file rather than one family, and sum the four. The total
   must stay at or below `0.85`. Repeat for `BulletDefence`. This is an absolute
   check against the `1.0` clamp and it catches cross-pack loadouts that no set was
   designed for; a family-by-family check does not.
8. No member definition contains a `DamageReduction` or `OtherDefence` line whose
   value exceeds the category's `Defence`. If one does, the category writes all
   three keys to the same value.
9. No forbidden identity or rendering property appears in any armor category.
10. `Durability`, `KnockbackModifier`, and `KnockbackReduction` appear nowhere.

## Completion Report

In addition to the shared report:

- categories added, grouped by protection class and ballistic rating;
- short names newly covered, and how many remain;
- every `standalone` category, flagged as not intended to be combined with other
  armor;
- every item whose set shape could not be determined from sibling definitions, and
  the shape that was assumed;
- every cross-pack slot collision and how it was resolved;
- the worst-case four-slot `Defence` and `BulletDefence` stack for the whole file;
- every item whose class or rating was a judgement call between two adjacent tiers,
  and which was chosen;
- any member definition carrying `DamageReduction`, `OtherDefence`, `Slowness`, or
  `Defense`, since those defeat or complicate the override.
