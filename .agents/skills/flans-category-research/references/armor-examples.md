# Armor Worked Examples

Read [armor.md](armor.md) for the normative tables and rules.


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

