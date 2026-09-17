# Content-pack parameter audit

## Scope summary

Audited `run/flan` and `src/main` using the current `ContentManager`, `EnumType`, and `TypeFile` loading rules. The corpus contains **13 ZIP content packs**, **3,461 applicable legacy `.txt` definition files**, and **1,107 distinct `(type, normalized key)` pairs**. `src/main` exists but contains no applicable legacy content-pack definitions. All archives and applicable definitions were readable.

Recipe-grid rows and the four armor-recipe body rows following `AddArmour`/`AddArmor` were treated as data, not parameters. Obvious delimiter, truncated-comment, numeric-only, array-literal, and pasted-code fragments were also excluded from findings. Case was normalized exactly as `TypeFile` does. Inherited readers, `GunAnimationConfig`, `AmmoOverrides`, `VehicleArmorSpecReader`, `RealWorldSpecReader`, category injection, and direct `TypeFile` lookups were checked.

Result: **309 UNPARSED**, **0 PARSED_UNUSED**, and **0 UNCERTAIN** distinct `(type, parameter)` pairs.

## Unparsed parameters

### ARMOR / armArmor — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Butternut Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Overcoat Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Uniform.txt; 79 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / backArmor — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/LEADER Mvssolini.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME byzan cataphract.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME Cuirassier.txt; 20 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / bodyArmor — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Butternut Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Overcoat Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Uniform.txt; 80 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / faceArmor — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME byzan cataphract helm.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME cuirassier helm.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME gallic helmet.txt; 13 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / FeatherFalling — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/armorFiles/AZABoots.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / hasBowPouch — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME jap yoroi late.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / hasOldGunPouch — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Butternut Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Overcoat Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Uniform.txt; 7 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / hasPouch — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME byzan cataphract.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME hussar armor.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME mamluk lamellar.txt; 7 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / headArmor — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/HEAD Civilian Felt Hat - Black.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/HEAD Civilian Straw Hat.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/HEAD CSA Butternut Forage Cap.txt; 66 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / ItemID — UNPARSED

Observed: `DAK by Plume.zip, Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/DAK by Plume.zip!/armorFiles/pp_afrikakorpsboots.txt; run/flan/DAK by Plume.zip!/armorFiles/pp_afrikakorpscap.txt; run/flan/DAK by Plume.zip!/armorFiles/pp_afrikakorpschest.txt; 461 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / legArmor — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/LEADER Mvssolini.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME byzan cataphract.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME French Uniform.txt; 18 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / neckArmor — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME byzan cataphract helm.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME cuirassier helm.txt; run/flan/Tyrants And Plebeians OHIO.zip!/armorFiles/MEME gallic helmet.txt; 12 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ARMOR / reloadMultiplier — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Butternut Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Overcoat Uniform.txt; run/flan/TaP Northern Aggression Pack.zip!/armorFiles/BODY CSA Grey Uniform.txt; 15 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ArmorType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ATTACHMENT / AddNode — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/attachments/musketbayonet.txt; 17 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/AttachmentType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ATTACHMENT / bayonet — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/attachments/musketbayonet.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/AttachmentType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### ATTACHMENT / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/attachments/ExperimentalBayonet.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/attachments/Suppressor.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/AttachmentType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / activationDepth — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 12cmShellASW.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 21inchTorpMarkIX.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 21inchTorpMarkVIII.txt; 17 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / angelOfDeath — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / armorPen — UNPARSED

Observed: `Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/bullets/TSA Ballista Bolt.txt; run/flan/Roman Pack.zip!/bullets/TSA Catapult Rock.txt; run/flan/Roman Pack.zip!/bullets/TSA Scorpio Bolt.txt; 396 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / barelyPenPenalty — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/MG original Gatling Ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Heavy Ram.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Light Ram.txt; 176 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / bigWater — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/BO 1000 of 250kg copy.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/BO 1000 of 500kg.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/BO 1000kg.txt; 68 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / bleedMultiplier — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAP colt 1851 ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAP LeMat Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AAMG 7mm jap.txt; 143 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / bodyArmorPen — UNPARSED

Observed: `Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/bullets/pilum.txt; run/flan/Roman Pack.zip!/bullets/SAB lead slingshot.txt; run/flan/Roman Pack.zip!/bullets/SAB rock slingshot.txt; 196 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / Bouncy — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / bulletSmokeTime — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / CanMountEntity — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA 81mm Mortar Smoke.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / CIWSable — UNPARSED

Observed: `Roman Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/bullets/pilum.txt; run/flan/Roman Pack.zip!/bullets/verutum.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/ATGM HS293.txt; 35 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / ciwsBullet — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA concentrated light flak.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA light flak.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AAMG 20mm ho05 l94.txt; 25 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / DamagaVsPlanes — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Heavy Ram.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Light Ram.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / DamageVsEntities — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/GenericMGAmmo.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/MG2015Ammo.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/MG_BrenM5-200Rounds.txt; 13 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / depthCharge — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 12cmShellASW.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Aerial Depth Charge 40m.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Aerial Depth Charge.txt; 10 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / Descriptions — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAR AK-47 Ammo Tracer.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / dynamicBodyArmorPen — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/MG original Gatling Ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .44 Henry.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .52 cal rifle shot.txt; 144 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / dynamicBulletDelay — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/MG original Gatling Ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .44 Henry.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .52 cal rifle shot.txt; 147 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / dynamicDamage — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/MG original Gatling Ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .44 Henry.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAC .52 cal rifle shot.txt; 147 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / gasmaskable — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / HasLightTrue — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/Misc_AWPRAmmo.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / HEAT — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/ATGM HS293.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/BO 100 of 50kg.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/BO 1000 of 250kg copy.txt; 64 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / HHitSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA 600mm normal.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA 600mm raiding.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/12.7cmShellAP.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/12.7cmShellHE.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/15cmSKC25AAShell.txt; 613 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / LockOnFuse — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/51cmDebugShell.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/AIM120Rocket.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/R77Rocket.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / LockOnToVehicle — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/AIM120Rocket.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/L_FliegerhammerRocketsMk2.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/L_improvedStingerAmmo.txt; 18 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / minorPenSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 39mm Williams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHET.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHETQuad.txt; 297 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / missileRadarVisible — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/ATGM HS293.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / modernTorpedo — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 19inMk24FIDO.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 21inTorpedoMk18.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS 53.3cmG7esT5.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / navalMine — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Shitty Naval Mine for battleships.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Shitty Naval Mine.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / nonPenPenalty — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/MG original Gatling Ammo.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Heavy Ram.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Light Ram.txt; 175 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / overPenSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 39mm Williams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHET.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHETQuad.txt; 297 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / papaDrill — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA 81mm Mortar.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / Parachute — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS UP thing submunition.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/unrotated (1).txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / penDecay — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Heavy Ram.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/NS Light Ram.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 13Inch Mortar Shell.txt; 342 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / penetrateSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 39mm Williams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHET.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHETQuad.txt; 297 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / ricochetSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 39mm Williams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHET.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA 28mmAmmoHETQuad.txt; 297 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / RoundsPerItem1 — UNPARSED

Observed: `Roman Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/bullets/TSA Catapult Rock.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA Catapult Rock.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / scoutBullet — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS HedgehogAmmo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS HedgehogAmmoLR.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS HedgehogAmmoSR.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / shrapnelAngel — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSM Heavy Canister.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSM Large Canister.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSM Medium Canister.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / Skin44_W44Bullet_Tracer_White — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/AA concentrated light flak.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA Generic Star shell naval.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA Generic Star shell.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / smokeDelay — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / smokeParticleCount — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / SmokeRadius — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / SmokeTime — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/MOAB.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / speedMultiplier — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/bullets/NAV_genericNavalMine.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAR Buckless Ball.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/TS 39mm Williams.txt; 54 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / starShell — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA Generic Star shell naval.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/TSA Generic Star shell.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/unrotated (1).txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / stolenSmoke — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / stolenSmokeEffect — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/bullets/SAAT Trench Whistle Ammo.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/SAAT Trench Whistle Ammo.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / SwordEnergy — UNPARSED

Observed: `Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/bullets/pilum.txt; run/flan/Roman Pack.zip!/bullets/verutum.txt; run/flan/TaP Northern Aggression Pack.zip!/bullets/SE Bayonet  Sword Energy Spike.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### BULLET / wingVisible — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Badass Torpedo air drop.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Generic Torpedo Aerial Bomb.txt; run/flan/Tyrants And Plebeians OHIO.zip!/bullets/NS Generic Torpedo air drop.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/BulletType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GRENADE / Bomb — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/grenades/test.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GrenadeType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GRENADE / f1grenade — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/FRAG F1 Frag.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GrenadeType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GRENADE / ItemID — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/grenades/Bomb.txt; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/AntiTank AT Mine.txt; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/AT GeballteLadung.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GrenadeType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GRENADE / smokerino — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/SMK M8Smoke.txt; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/SMK Nebelhandgranate.txt; run/flan/Tyrants And Plebeians OHIO.zip!/grenades/SMK No27.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GrenadeType.java#read; common/types/ShootableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / 5UsableByPlayers — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/NG 4.7inDual.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / \ShootDelay — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/MG mle 14.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / AddLeftNode — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 117 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / AddRightNode — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 108 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / AddUpNode — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1860 Sword.txt; 60 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / CanHipFireWhileSprinting — UNPARSED

Observed: `Plume Pack WW1 1.0.8.zip, Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Plume Pack WW1 1.0.8.zip!/guns/berthier.txt; run/flan/Plume Pack WW1 1.0.8.zip!/guns/fedorov.txt; run/flan/Plume Pack WW1 1.0.8.zip!/guns/g98.txt; 19 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / CanLockOnAngle — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/L_FIM92EStinger.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/L_Javelin.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / DeployableModel — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT Panzerbuechse 39.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT Wz35.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / dillElevator — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/HG VolcanicPistol.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AK47.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AVT-40.txt; 46 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / dillZoomModifier — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/HG VolcanicPistol.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AK47.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AVT-40.txt; 46 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / Durability — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/BR_BarM2.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/MG_browningM1919A6.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / firstShotRecoil — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/BR Henry 1860.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/BR SharpsRifle.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Enfield 1853.txt; 48 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / GunCategory — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT M9A1 Bazooka.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/HG C96.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/MG DT-28.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / HasNightVision — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AMR_XM109.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/ARD_K11.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/L_Javelin.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / is — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT_Lungemine.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AMR_wtfRifle.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AMR_XM109.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/ARD_K11.txt; 428 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / Load — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT_Lungemine.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / LoadSecondaryIntoGun — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / LockOnToVehicle — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/L_FIM92EStinger.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/L_Javelin.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeHitSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / meleeLeft — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeLeftDamagePoint — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 40 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeLeftTime — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / meleeRight — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeRightDamagePoint — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 40 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeRightTime — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / meleeUp — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1860 Sword.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeUpDamagePoint — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 40 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / MeleeUpTime — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / ModeSwitchSound — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / muzzleParticleHave — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/BR Henry 1860.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/BR SharpsRifle.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/HG 1851.txt; 12 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / OldGun — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Enfield 1853.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Spencer Carbine.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Springfield 1842.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / Painjob — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/BR_BarM2.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RandamRecoil — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AR_DAR21.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AR_XM29 OICW.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/Misc_FeGel 4200.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RandamRecoilRange — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AR_DAR21.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/AR_XM29 OICW.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/Misc_FeGel 4200.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RandomRecoil — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/MG_Type99ModS.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RandomRecoilYaw — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RechargeRecipe — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/BR_BarM2.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/MG_browningM1919A6.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / recoilElevator — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/BR Henry 1860.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/BR SharpsRifle.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Enfield 1853.txt; 48 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RepeatingGun — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/BR Henry 1860.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/HG 1851.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/HG LeMat Revolver.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / RPM — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AK47.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR AVT-40.txt; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AR BAR.txt; 39 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryAmmo — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryDamage — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryMode — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryNumBullets — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryReloadSound — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryReloadTime — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryShootDelay — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondaryShootSound — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / SecondarySpread — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/guns/DebugM16.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / ShieldHitSound — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / shootMelee — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/ac arkansas dagger.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac artillery sword.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/ac M1840.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / sidearm — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/HG 1851.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/HG LeMat Revolver.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/HG VolcanicPistol.txt; 25 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / spear — UNPARSED

Observed: `Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/guns/javelin.txt; run/flan/Roman Pack.zip!/guns/kontos.txt; run/flan/Roman Pack.zip!/guns/pilum.txt; 12 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / srName — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/SR Berdan 2.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / sustainedelevator — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/guns/BR Henry 1860.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/BR SharpsRifle.txt; run/flan/TaP Northern Aggression Pack.zip!/guns/SR Enfield 1853.txt; 48 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / wEmptyClickSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/BR WZ38M.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN / Xoffset — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/guns/AT_Lungemine.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunType.java#read/readLine; common/types/GunAnimationConfig.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/guns/AmmoOverrides.java#read; common/guns/RemovedAmmo.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN_BOX / GunBoxID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/BigGunsBox.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/MetallurgyStation.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/MilspecWeapons.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunBoxType.java#read/readLine; common/types/BlockType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### GUN_BOX / NumGuns — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/BigGunsBox.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/MetallurgyStation.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/boxes/MilspecWeapons.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/GunBoxType.java#read/readLine; common/types/BlockType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PART / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/parts/12CylinderEngine.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/parts/12gaugeBirdshotShellBox.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/parts/12gaugeHuntingShellBox.txt; 90 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PartType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / HealStrength — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/OBAMACARE.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/Vodka.txt; run/flan/Tyrants And Plebeians OHIO.zip!/tools/MWMedkit.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/hardtack.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/MegaBlowTorch.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/OBAMACARE.txt; 10 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / KnockbackModifier — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/OBAMACARE.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / MoveSpeedModifier — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/OBAMACARE.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / PotionEffect — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/Vodka.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### TOOL / StackSize — UNPARSED

Observed: `Plume Pack WW1 1.0.8.zip, Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Plume Pack WW1 1.0.8.zip!/tools/medicalsupplies.txt; run/flan/Plume Pack WW1 1.0.8.zip!/tools/russiantabaco.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/tools/hardtack.txt; 12 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/ToolType.java#read; common/types/InfoType.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / AccelerationSpeed — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 157 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / airship — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1p5 Balloon.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T-1p5 Balloon.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / alwaysShowTurret — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A 13in mortar.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Napoleon Gun.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Parrott Gun 20lbs.txt; 133 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / animationMultiplier — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / APSdelayMax — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / APSsound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / artilleryCalculator — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A 13in mortar.txt; 51 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / autisticHitDetection — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; 37 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / barrels — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; 507 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / barrelSpread — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; 509 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / bigDeath — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/MNT_BL15inchMk1.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_TillmanVFullScale.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 128 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / Bounciness — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip, Wolff's Germany WW2 Pack 2.3.2.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/Auto_SanicMobile.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK-SPG_SturmTiger.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK-Tism_TrollSherman.txt; 107 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / bouncy — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A M3 GMC.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1T Hanomag.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1T M3 halftrack.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / BrakeMultiplier — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 102 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / BrakeSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5H T-35.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1p5H Il Duce.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / BrakeSoundLength — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5H T-35.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1p5H Il Duce.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / BrakeSoundRange — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5H T-35.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1p5H Il Duce.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / canDabOnEntity — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / canDive — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Barb.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_cssHunley.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; 25 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / carrier — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Ark Royal.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Bearn.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Enterprise.txt; 68 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / centralControl — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Heavy Cruiser.txt; 140 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / ClutchSteer — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A Wespe.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1T MarderII.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / coolingBonus — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A M45Quad.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / damageVsCrew — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S Eh Sex.txt; 131 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / Death — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/SP_BarrackBusterBattery.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 74 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / DebrisParticleCount — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A 13in mortar.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / DecelerationSpeed — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 139 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / digitalRadar — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / Distance — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0H KV1.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0L 4TP.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0L 7TP.txt; 13 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / DiveSpeed — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Barb.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_cssHunley.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; 25 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / driftMultiplier — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0S Cockshafer.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / earRape — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0H Char2c.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1H 53TP.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1H Tog 2.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / EnableReloadTime — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AA_Type96Mount.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5A ChicagoPiano.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / epicShip — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 138 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / evilGolem — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Heavy Cruiser.txt; 22 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / evilRange — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer (B).txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Destroyer.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T0p5S Heavy Cruiser.txt; 22 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / exitTimer — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/Wagon.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/EVIL T1A MG42 Tripod.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 105 howitzer - romanian.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / explosionPush — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Barb.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Tang.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Torsk.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / explosionResistance — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5T Sumida.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A 44M Zrinyi I.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A Jagdpanzer Hetzer.txt; 20 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / FlareParticleCount — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A 13in mortar.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / FlipLinkFix — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A CV-35.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0L Panzer38t.txt; 54 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / fuelTimer — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Tang.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Torsk.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / Gunsight — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_Betushka7U.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_CalliopeSherman.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; 234 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / gunsightZoom — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_Betushka7U.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_CalliopeSherman.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; 234 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / hasAPS — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cssHunley.txt; 141 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / hasMagicArtilleryMode — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A 13in mortar.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Napoleon Gun.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Parrott Gun 20lbs.txt; 27 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / hasRadar — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 137 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / hasScope — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_Betushka7U.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_CalliopeSherman.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; 234 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / HasSmoke — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_ChesapeakeFFG.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_LongBeach1964.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; 170 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / heliGUI — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A SdKfz222.txt; 51 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / heliGuiSeat — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A SdKfz222.txt; 51 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / helipad — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Ardent.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Boston1961.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Brooke.txt; 113 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / hijackablePilot — UNPARSED

Observed: `TaP Northern Aggression Pack.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T0S FunRaft.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S gay frigate.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / invincible — UNPARSED

Observed: `Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Roman Pack.zip!/vehicles/Ballista.txt; run/flan/Roman Pack.zip!/vehicles/Onager.txt; run/flan/Roman Pack.zip!/vehicles/Polyboros.txt; 279 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / invisiblePassenger — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A SdKfz222.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0L M3A1 Stuart.txt; 115 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / isExplosionWhenDestroyedRadius — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/MNT_BL15inchMk1.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_TillmanVFullScale.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/SP_BarrackBusterBattery.txt; 200 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 308 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / labjacFuel — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Barb.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Scharnhorst1943.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Warrington.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / LeftLinkPoint+ — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1M ChiNu.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / loudCannon — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Rodney1941.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / maxAltitude — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1p5 Balloon.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T-1p5 Balloon.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / maxOxygen — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Barb.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_cssHunley.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; 23 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / MomentOfInertia — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/Auto_SanicMobile.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0T BMWR75.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0T Citroen 23.txt; 10 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / needsThrottle — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1p5 Balloon.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S Eh Sex.txt; 7 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / nightScope — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_Betushka7U.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_CalliopeSherman.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; 99 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / nuclearDeath — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T5A SchwererGustav.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / NumWheels — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Roman Pack.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 211 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / overheatLimit — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A M45Quad.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / overheatPenalty — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A M45Quad.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / oxygen — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Barb.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_cssHunley.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; 23 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / PassengerHurtable — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A LeFH18.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A LeIG18.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / passengerZoom — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_ForestedAbrams.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1H Tiger II.txt; 4 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / PlaceTime — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A Flak20mm.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A LeFH18.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A type98 20mm.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / primaryRecoilStrength — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Gatling.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T-1A Williams gun.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A TKS20.txt; 21 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / radarDetectionRangeMultiplier — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S Jastrzab.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S S44.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1S Barb.txt; 17 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / radarPositionOffset — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 126 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / radarRange — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 126 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / radarRefreshDelay — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 126 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / radarVisible — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk19Mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAH_5inMk28mod2Mount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AAM_Mk13GMLS.txt; 164 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / RecoilDistance — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/AA_Bofors57mmAAMount.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_BK1124.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/TNK_Betushka7U.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / RestrictInventoryInput — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1H Tiger II.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1M VC Firefly.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / secondaryRecoilStrength — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5A ChicagoPiano.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0U Crane.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A 14in45ArizonaTurret.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / slbmDelay — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / slbmFlightType — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / slbmRange — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / slbmStrength — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / slbmWarheadType — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T6S ARP-Haruna.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / solid — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Reshadiye.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Salami.txt; run/flan/TaP Northern Aggression Pack.zip!/vehicles/T1S cottonclad.txt; 140 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / SoundsPlaceTimePrimary — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/C Model T.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 16 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / Stabilizer — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5A ChicagoPiano.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / SurfaceSpeed — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_Barb.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_cssHunley.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; 25 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / terrainPenalty — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/Wagon.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; 29 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / transport — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/vehicles/Wagon.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Universal Carrier.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Ark Royal.txt; 23 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / TurretRotationSpeed — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip, Wolff's Germany WW2 Pack 2.3.2.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A M45Quad.txt; run/flan/Wolff's Germany WW2 Pack 2.3.2.zip!/vehicles/Tiger Snow.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / unlimitedOxygen — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/vehicles/NAV_NebraskaSSBN.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T2p5S Kim Jong Un Sub.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / walterCalculator — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5 Generic coast gun.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A BL55.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A FH18 15cm.txt; 12 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / walterMortar — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 105 howitzer - romanian.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A 81mm Generic Mortar.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T1A 81mm Mortar Battery.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / WeakspotCookTime — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A 39M Csaba.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A Panhard 178.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0A SdKfz222.txt; 74 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### VEHICLE / weightLimit — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Ark Royal.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Bearn.txt; run/flan/Tyrants And Plebeians OHIO.zip!/vehicles/T0p5S CV Enterprise.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/VehicleType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / accelBonus — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B10.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B18A.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; 50 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / afterBurnFuelPenalty — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; 75 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / afterBurnName — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; 79 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / area — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; 172 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / barrels — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ki21.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ki32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1p5F Il28.txt; 53 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / barrelSpread — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ki21.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ki32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1p5F Il28.txt; 53 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / bigDeath — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Ohka.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / Bounciness — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F 451m.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Meteor early.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Meteor.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / carrierLandable — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5F A5M4.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T D3A.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; 35 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / centralControl — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B10.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B18A.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; 44 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / climbRate — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S S9Osprey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S SOCSeagull.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T PO2.txt; 15 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / coolingBonus — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2d.txt; 128 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / cruiseSpeed — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; 61 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / ff — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Lancaster.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / flightCeiling — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; 176 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / Gunsight — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Hs129.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / gunsightZoom — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Hs129.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / hardpoint — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1P5T B-36.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1p5T G8N.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T G4M2.txt; 3 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / hasAfterBurner — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; 79 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / hasPlaneRadar — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/RU-SugoiSU57.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / hasScope — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Hs129.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / heliGUI — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9Osprey.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9OspreyFancy.txt; 42 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / heliGuiSeat — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9Osprey.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9OspreyFancy.txt; 42 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / helipadLandable — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9Osprey.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9OspreyFancy.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / invincible — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T D3A.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T Ju-87.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / invisiblePassenger — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B10.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T B18A.txt; 20 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / isExplosionWhenDestroyedRadius — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/FusoAirServiceA6MZero.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/genericPlane.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/IJNASA6M3Zero.txt; 43 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / kamikazeBonus — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Ohka.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / labjacFuel — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Chaika.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; 150 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / missileElevation — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T TBD.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T B5N.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / missileForward — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T TBD.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T B5N.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / missileVisible — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T TBD.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T B5N.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / missileWingSpan — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T G3M.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T TBD.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T B5N.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / MomentOfInertia — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F 451m.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Meteor early.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Meteor.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / muzzleVelocity — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S S9Osprey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S SOCSeagull.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T B-17G.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / needsGear — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F CR32.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F He51.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F P11.txt; 37 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / NumWheels — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/FusoAirServiceA6MZero.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/genericPlane.txt; 198 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / overheatLimit — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2d.txt; 128 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / overheatPenalty — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F D.520.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2c.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0F Hurricane Mk2d.txt; 128 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / parasitePlane — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F Goblino.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Ohka.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / passengerZoom — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9Osprey.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/S9OspreyFancy.txt; 9 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / planeCoaxSecondary — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Grasshopper.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Storch.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / planeDiveFactor — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F4u early.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F4u late.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F8 Bearcat.txt; 14 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / projectileMass — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S S9Osprey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0S SOCSeagull.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T B-17G.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / PropSoundRange — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Mosquito 57.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Mosquito.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / radarPositionOffset — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/RU-SugoiSU57.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / radarRange — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/RU-SugoiSU57.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / radarRefreshDelay — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/RU-SugoiSU57.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / radarVisible — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/AH1ZViper.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/KirovRedAlert.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/planes/RU-SugoiSU57.txt; 11 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / StukaSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T Ju-87.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / StukaSoundLength — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T Ju-87.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / stukaSoundRange — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T Ju-87.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / StukaSpeed — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0p5T Ju-87C.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T0T Ju-87.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / swapInitialWing — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F4F.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F4u early.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1F F4u late.txt; 9 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / transport — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T Ki-56.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### PLANE / urnFuelPenalty — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T G4M.txt; run/flan/Tyrants And Plebeians OHIO.zip!/planes/T1T G4M2.txt; 2 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/PlaneType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / Add — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/mechas/hapa.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / AllowAllGuns — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / canPanic — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / HasSmoke — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / invincible — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / ItemID — UNPARSED

Observed: `Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip, TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/mechas/Colorado.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/mechas/hapa.txt; run/flan/Rainfire5's Arsenal 1.9.7 - Battle Squadrons.zip!/mechas/PrinzEugen.txt; 8 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / MechStomp — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / morale — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / panicSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / panicTime — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / runAmokSound — UNPARSED

Observed: `Tyrants And Plebeians OHIO.zip; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 1 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / solid — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 6 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

### MECHA / vanillaDamage — UNPARSED

Observed: `TaP Northern Aggression Pack.zip, Tyrants And Plebeians OHIO.zip; run/flan/TaP Northern Aggression Pack.zip!/mechas/Horsey.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/cannon camel.txt; run/flan/Tyrants And Plebeians OHIO.zip!/mechas/Elefun.txt; 5 occurrence(s)`
Parser checked: `src/main/java/com/flansmodultimate/common/types/MechaType.java#read; common/types/DriveableType.java#read; common/types/PaintableType.java#read; common/types/InfoType.java#read; common/driveables/armor/VehicleArmorSpecReader.java#read; common/driveables/physics/RealWorldSpecReader.java#read`; no matching parser for this concrete type.
Consumers checked: `src/main/java` and `src/main/resources` exact-key and normalized-key searches, plus `TypeFile` direct lookups; no parsed value is created for a consumer.
Finding: The current case-insensitive definition reader does not accept this parameter for the observed type, so the configured line has no effect.
Confidence: HIGH

## Summary

| Type | Parameter | Status | Affected packs | Occurrences | Confidence |
|---|---|---:|---:|---:|---:|
| ARMOR | `armArmor` | UNPARSED | 2 | 79 | HIGH |
| ARMOR | `backArmor` | UNPARSED | 1 | 20 | HIGH |
| ARMOR | `bodyArmor` | UNPARSED | 2 | 80 | HIGH |
| ARMOR | `faceArmor` | UNPARSED | 1 | 13 | HIGH |
| ARMOR | `FeatherFalling` | UNPARSED | 1 | 1 | HIGH |
| ARMOR | `hasBowPouch` | UNPARSED | 1 | 1 | HIGH |
| ARMOR | `hasOldGunPouch` | UNPARSED | 1 | 7 | HIGH |
| ARMOR | `hasPouch` | UNPARSED | 1 | 7 | HIGH |
| ARMOR | `headArmor` | UNPARSED | 2 | 66 | HIGH |
| ARMOR | `ItemID` | UNPARSED | 7 | 461 | HIGH |
| ARMOR | `legArmor` | UNPARSED | 1 | 18 | HIGH |
| ARMOR | `neckArmor` | UNPARSED | 1 | 12 | HIGH |
| ARMOR | `reloadMultiplier` | UNPARSED | 2 | 15 | HIGH |
| ATTACHMENT | `AddNode` | UNPARSED | 1 | 17 | HIGH |
| ATTACHMENT | `bayonet` | UNPARSED | 1 | 1 | HIGH |
| ATTACHMENT | `ItemID` | UNPARSED | 1 | 2 | HIGH |
| BULLET | `activationDepth` | UNPARSED | 1 | 17 | HIGH |
| BULLET | `angelOfDeath` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `armorPen` | UNPARSED | 3 | 396 | HIGH |
| BULLET | `barelyPenPenalty` | UNPARSED | 2 | 176 | HIGH |
| BULLET | `bigWater` | UNPARSED | 1 | 68 | HIGH |
| BULLET | `bleedMultiplier` | UNPARSED | 2 | 143 | HIGH |
| BULLET | `bodyArmorPen` | UNPARSED | 3 | 196 | HIGH |
| BULLET | `Bouncy` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `bulletSmokeTime` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `CanMountEntity` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `CIWSable` | UNPARSED | 2 | 35 | HIGH |
| BULLET | `ciwsBullet` | UNPARSED | 1 | 25 | HIGH |
| BULLET | `DamagaVsPlanes` | UNPARSED | 1 | 3 | HIGH |
| BULLET | `DamageVsEntities` | UNPARSED | 1 | 13 | HIGH |
| BULLET | `depthCharge` | UNPARSED | 1 | 10 | HIGH |
| BULLET | `Descriptions` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `dynamicBodyArmorPen` | UNPARSED | 2 | 144 | HIGH |
| BULLET | `dynamicBulletDelay` | UNPARSED | 2 | 147 | HIGH |
| BULLET | `dynamicDamage` | UNPARSED | 2 | 147 | HIGH |
| BULLET | `gasmaskable` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `HasLightTrue` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `HEAT` | UNPARSED | 1 | 64 | HIGH |
| BULLET | `HHitSound` | UNPARSED | 1 | 2 | HIGH |
| BULLET | `ItemID` | UNPARSED | 4 | 613 | HIGH |
| BULLET | `LockOnFuse` | UNPARSED | 1 | 4 | HIGH |
| BULLET | `LockOnToVehicle` | UNPARSED | 2 | 18 | HIGH |
| BULLET | `minorPenSound` | UNPARSED | 2 | 297 | HIGH |
| BULLET | `missileRadarVisible` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `modernTorpedo` | UNPARSED | 1 | 3 | HIGH |
| BULLET | `navalMine` | UNPARSED | 1 | 2 | HIGH |
| BULLET | `nonPenPenalty` | UNPARSED | 2 | 175 | HIGH |
| BULLET | `overPenSound` | UNPARSED | 2 | 297 | HIGH |
| BULLET | `papaDrill` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `Parachute` | UNPARSED | 1 | 2 | HIGH |
| BULLET | `penDecay` | UNPARSED | 2 | 342 | HIGH |
| BULLET | `penetrateSound` | UNPARSED | 2 | 297 | HIGH |
| BULLET | `ricochetSound` | UNPARSED | 2 | 297 | HIGH |
| BULLET | `RoundsPerItem1` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `scoutBullet` | UNPARSED | 1 | 5 | HIGH |
| BULLET | `shrapnelAngel` | UNPARSED | 1 | 3 | HIGH |
| BULLET | `Skin` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `Skin44_W44Bullet_Tracer_White` | UNPARSED | 1 | 3 | HIGH |
| BULLET | `smokeDelay` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `smokeParticleCount` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `SmokeRadius` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `SmokeTime` | UNPARSED | 1 | 1 | HIGH |
| BULLET | `speedMultiplier` | UNPARSED | 3 | 54 | HIGH |
| BULLET | `starShell` | UNPARSED | 1 | 3 | HIGH |
| BULLET | `stolenSmoke` | UNPARSED | 2 | 2 | HIGH |
| BULLET | `stolenSmokeEffect` | UNPARSED | 2 | 6 | HIGH |
| BULLET | `SwordEnergy` | UNPARSED | 3 | 8 | HIGH |
| BULLET | `wingVisible` | UNPARSED | 1 | 3 | HIGH |
| GRENADE | `Bomb` | UNPARSED | 1 | 1 | HIGH |
| GRENADE | `f1grenade` | UNPARSED | 1 | 1 | HIGH |
| GRENADE | `ItemID` | UNPARSED | 4 | 11 | HIGH |
| GRENADE | `smokerino` | UNPARSED | 1 | 4 | HIGH |
| GUN | `5UsableByPlayers` | UNPARSED | 1 | 1 | HIGH |
| GUN | `\ShootDelay` | UNPARSED | 1 | 1 | HIGH |
| GUN | `AddLeftNode` | UNPARSED | 2 | 117 | HIGH |
| GUN | `AddRightNode` | UNPARSED | 2 | 108 | HIGH |
| GUN | `AddUpNode` | UNPARSED | 2 | 60 | HIGH |
| GUN | `CanHipFireWhileSprinting` | UNPARSED | 2 | 19 | HIGH |
| GUN | `CanLockOnAngle` | UNPARSED | 1 | 2 | HIGH |
| GUN | `DamageVsVehicles` | UNPARSED | 1 | 2 | HIGH |
| GUN | `DeployableModel` | UNPARSED | 2 | 5 | HIGH |
| GUN | `dillElevator` | UNPARSED | 2 | 46 | HIGH |
| GUN | `dillZoomModifier` | UNPARSED | 2 | 46 | HIGH |
| GUN | `Durability` | UNPARSED | 1 | 2 | HIGH |
| GUN | `firstShotRecoil` | UNPARSED | 2 | 48 | HIGH |
| GUN | `GunCategory` | UNPARSED | 1 | 3 | HIGH |
| GUN | `HasNightVision` | UNPARSED | 1 | 4 | HIGH |
| GUN | `is` | UNPARSED | 1 | 1 | HIGH |
| GUN | `ItemID` | UNPARSED | 4 | 428 | HIGH |
| GUN | `Load` | UNPARSED | 1 | 1 | HIGH |
| GUN | `LoadSecondaryIntoGun` | UNPARSED | 1 | 1 | HIGH |
| GUN | `LockOnToVehicle` | UNPARSED | 1 | 2 | HIGH |
| GUN | `MeleeHitSound` | UNPARSED | 2 | 8 | HIGH |
| GUN | `meleeLeft` | UNPARSED | 2 | 8 | HIGH |
| GUN | `MeleeLeftDamagePoint` | UNPARSED | 2 | 40 | HIGH |
| GUN | `MeleeLeftTime` | UNPARSED | 2 | 8 | HIGH |
| GUN | `meleeRight` | UNPARSED | 2 | 8 | HIGH |
| GUN | `MeleeRightDamagePoint` | UNPARSED | 2 | 40 | HIGH |
| GUN | `MeleeRightTime` | UNPARSED | 2 | 8 | HIGH |
| GUN | `meleeUp` | UNPARSED | 2 | 4 | HIGH |
| GUN | `MeleeUpDamagePoint` | UNPARSED | 2 | 40 | HIGH |
| GUN | `MeleeUpTime` | UNPARSED | 2 | 8 | HIGH |
| GUN | `ModeSwitchSound` | UNPARSED | 1 | 1 | HIGH |
| GUN | `muzzleParticleHave` | UNPARSED | 1 | 12 | HIGH |
| GUN | `OldGun` | UNPARSED | 1 | 5 | HIGH |
| GUN | `Painjob` | UNPARSED | 1 | 1 | HIGH |
| GUN | `RandamRecoil` | UNPARSED | 1 | 3 | HIGH |
| GUN | `RandamRecoilRange` | UNPARSED | 1 | 3 | HIGH |
| GUN | `RandomRecoil` | UNPARSED | 1 | 1 | HIGH |
| GUN | `RandomRecoilYaw` | UNPARSED | 1 | 1 | HIGH |
| GUN | `RechargeRecipe` | UNPARSED | 1 | 2 | HIGH |
| GUN | `recoilElevator` | UNPARSED | 2 | 48 | HIGH |
| GUN | `RepeatingGun` | UNPARSED | 1 | 5 | HIGH |
| GUN | `RPM` | UNPARSED | 1 | 39 | HIGH |
| GUN | `SecondaryAmmo` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryDamage` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryMode` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryNumBullets` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryReloadSound` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryReloadTime` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryShootDelay` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondaryShootSound` | UNPARSED | 1 | 1 | HIGH |
| GUN | `SecondarySpread` | UNPARSED | 1 | 1 | HIGH |
| GUN | `ShieldHitSound` | UNPARSED | 2 | 8 | HIGH |
| GUN | `shootMelee` | UNPARSED | 2 | 8 | HIGH |
| GUN | `sidearm` | UNPARSED | 2 | 25 | HIGH |
| GUN | `spear` | UNPARSED | 3 | 12 | HIGH |
| GUN | `srName` | UNPARSED | 1 | 1 | HIGH |
| GUN | `sustainedelevator` | UNPARSED | 2 | 48 | HIGH |
| GUN | `wEmptyClickSound` | UNPARSED | 1 | 1 | HIGH |
| GUN | `Xoffset` | UNPARSED | 1 | 1 | HIGH |
| GUN_BOX | `GunBoxID` | UNPARSED | 1 | 5 | HIGH |
| GUN_BOX | `NumGuns` | UNPARSED | 1 | 5 | HIGH |
| MECHA | `Add` | UNPARSED | 1 | 1 | HIGH |
| MECHA | `AllowAllGuns` | UNPARSED | 2 | 5 | HIGH |
| MECHA | `canPanic` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `HasSmoke` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `invincible` | UNPARSED | 2 | 5 | HIGH |
| MECHA | `ItemID` | UNPARSED | 3 | 8 | HIGH |
| MECHA | `MechStomp` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `morale` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `panicSound` | UNPARSED | 1 | 1 | HIGH |
| MECHA | `panicTime` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `runAmokSound` | UNPARSED | 1 | 1 | HIGH |
| MECHA | `solid` | UNPARSED | 2 | 6 | HIGH |
| MECHA | `vanillaDamage` | UNPARSED | 2 | 5 | HIGH |
| PART | `ItemID` | UNPARSED | 3 | 90 | HIGH |
| PLANE | `accelBonus` | UNPARSED | 1 | 50 | HIGH |
| PLANE | `afterBurnFuelPenalty` | UNPARSED | 1 | 75 | HIGH |
| PLANE | `afterBurnName` | UNPARSED | 1 | 79 | HIGH |
| PLANE | `area` | UNPARSED | 1 | 172 | HIGH |
| PLANE | `barrels` | UNPARSED | 1 | 53 | HIGH |
| PLANE | `barrelSpread` | UNPARSED | 1 | 53 | HIGH |
| PLANE | `bigDeath` | UNPARSED | 2 | 2 | HIGH |
| PLANE | `Bounciness` | UNPARSED | 1 | 8 | HIGH |
| PLANE | `carrierLandable` | UNPARSED | 1 | 35 | HIGH |
| PLANE | `centralControl` | UNPARSED | 1 | 44 | HIGH |
| PLANE | `climbRate` | UNPARSED | 1 | 15 | HIGH |
| PLANE | `coolingBonus` | UNPARSED | 1 | 128 | HIGH |
| PLANE | `cruiseSpeed` | UNPARSED | 1 | 61 | HIGH |
| PLANE | `ff` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `flightCeiling` | UNPARSED | 1 | 176 | HIGH |
| PLANE | `Gunsight` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `gunsightZoom` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `hardpoint` | UNPARSED | 1 | 3 | HIGH |
| PLANE | `hasAfterBurner` | UNPARSED | 1 | 79 | HIGH |
| PLANE | `hasPlaneRadar` | UNPARSED | 2 | 11 | HIGH |
| PLANE | `hasScope` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `heliGUI` | UNPARSED | 2 | 42 | HIGH |
| PLANE | `heliGuiSeat` | UNPARSED | 2 | 42 | HIGH |
| PLANE | `helipadLandable` | UNPARSED | 2 | 14 | HIGH |
| PLANE | `invincible` | UNPARSED | 1 | 8 | HIGH |
| PLANE | `invisiblePassenger` | UNPARSED | 2 | 20 | HIGH |
| PLANE | `isExplosionWhenDestroyedRadius` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `ItemID` | UNPARSED | 2 | 43 | HIGH |
| PLANE | `kamikazeBonus` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `labjacFuel` | UNPARSED | 1 | 150 | HIGH |
| PLANE | `missileElevation` | UNPARSED | 1 | 14 | HIGH |
| PLANE | `missileForward` | UNPARSED | 1 | 14 | HIGH |
| PLANE | `missileVisible` | UNPARSED | 1 | 14 | HIGH |
| PLANE | `missileWingSpan` | UNPARSED | 1 | 14 | HIGH |
| PLANE | `MomentOfInertia` | UNPARSED | 1 | 6 | HIGH |
| PLANE | `muzzleVelocity` | UNPARSED | 1 | 5 | HIGH |
| PLANE | `needsGear` | UNPARSED | 1 | 37 | HIGH |
| PLANE | `NumWheels` | UNPARSED | 2 | 198 | HIGH |
| PLANE | `overheatLimit` | UNPARSED | 1 | 128 | HIGH |
| PLANE | `overheatPenalty` | UNPARSED | 1 | 128 | HIGH |
| PLANE | `parasitePlane` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `passengerZoom` | UNPARSED | 2 | 9 | HIGH |
| PLANE | `planeCoaxSecondary` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `planeDiveFactor` | UNPARSED | 1 | 14 | HIGH |
| PLANE | `projectileMass` | UNPARSED | 1 | 5 | HIGH |
| PLANE | `PropSoundRange` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `radarPositionOffset` | UNPARSED | 2 | 11 | HIGH |
| PLANE | `radarRange` | UNPARSED | 2 | 11 | HIGH |
| PLANE | `radarRefreshDelay` | UNPARSED | 2 | 11 | HIGH |
| PLANE | `radarVisible` | UNPARSED | 2 | 11 | HIGH |
| PLANE | `StukaSound` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `StukaSoundLength` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `stukaSoundRange` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `StukaSpeed` | UNPARSED | 1 | 2 | HIGH |
| PLANE | `swapInitialWing` | UNPARSED | 1 | 9 | HIGH |
| PLANE | `transport` | UNPARSED | 1 | 1 | HIGH |
| PLANE | `urnFuelPenalty` | UNPARSED | 1 | 2 | HIGH |
| TOOL | `HealStrength` | UNPARSED | 2 | 3 | HIGH |
| TOOL | `ItemID` | UNPARSED | 3 | 10 | HIGH |
| TOOL | `KnockbackModifier` | UNPARSED | 1 | 1 | HIGH |
| TOOL | `MoveSpeedModifier` | UNPARSED | 1 | 1 | HIGH |
| TOOL | `PotionEffect` | UNPARSED | 1 | 4 | HIGH |
| TOOL | `StackSize` | UNPARSED | 4 | 12 | HIGH |
| VEHICLE | `AccelerationSpeed` | UNPARSED | 3 | 157 | HIGH |
| VEHICLE | `airship` | UNPARSED | 2 | 2 | HIGH |
| VEHICLE | `alwaysShowTurret` | UNPARSED | 2 | 133 | HIGH |
| VEHICLE | `animationMultiplier` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `APSdelayMax` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `APSsound` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `artilleryCalculator` | UNPARSED | 3 | 51 | HIGH |
| VEHICLE | `autisticHitDetection` | UNPARSED | 2 | 37 | HIGH |
| VEHICLE | `barrels` | UNPARSED | 2 | 507 | HIGH |
| VEHICLE | `barrelSpread` | UNPARSED | 2 | 509 | HIGH |
| VEHICLE | `bigDeath` | UNPARSED | 3 | 128 | HIGH |
| VEHICLE | `Bounciness` | UNPARSED | 3 | 107 | HIGH |
| VEHICLE | `bouncy` | UNPARSED | 1 | 3 | HIGH |
| VEHICLE | `BrakeMultiplier` | UNPARSED | 3 | 102 | HIGH |
| VEHICLE | `BrakeSound` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `BrakeSoundLength` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `BrakeSoundRange` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `canDabOnEntity` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `canDive` | UNPARSED | 3 | 25 | HIGH |
| VEHICLE | `carrier` | UNPARSED | 1 | 68 | HIGH |
| VEHICLE | `centralControl` | UNPARSED | 1 | 140 | HIGH |
| VEHICLE | `ClutchSteer` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `coolingBonus` | UNPARSED | 1 | 11 | HIGH |
| VEHICLE | `damageVsCrew` | UNPARSED | 3 | 131 | HIGH |
| VEHICLE | `Death` | UNPARSED | 2 | 74 | HIGH |
| VEHICLE | `DebrisParticleCount` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `DecelerationSpeed` | UNPARSED | 3 | 139 | HIGH |
| VEHICLE | `digitalRadar` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `Distance` | UNPARSED | 1 | 13 | HIGH |
| VEHICLE | `DiveSpeed` | UNPARSED | 3 | 25 | HIGH |
| VEHICLE | `driftMultiplier` | UNPARSED | 1 | 3 | HIGH |
| VEHICLE | `earRape` | UNPARSED | 1 | 11 | HIGH |
| VEHICLE | `EnableReloadTime` | UNPARSED | 2 | 2 | HIGH |
| VEHICLE | `epicShip` | UNPARSED | 3 | 138 | HIGH |
| VEHICLE | `evilGolem` | UNPARSED | 1 | 22 | HIGH |
| VEHICLE | `evilRange` | UNPARSED | 1 | 22 | HIGH |
| VEHICLE | `exitTimer` | UNPARSED | 2 | 14 | HIGH |
| VEHICLE | `explosionPush` | UNPARSED | 1 | 8 | HIGH |
| VEHICLE | `explosionResistance` | UNPARSED | 1 | 20 | HIGH |
| VEHICLE | `FlareParticleCount` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `FlipLinkFix` | UNPARSED | 2 | 54 | HIGH |
| VEHICLE | `fuelTimer` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `Gunsight` | UNPARSED | 2 | 234 | HIGH |
| VEHICLE | `gunsightZoom` | UNPARSED | 2 | 234 | HIGH |
| VEHICLE | `hasAPS` | UNPARSED | 3 | 141 | HIGH |
| VEHICLE | `hasMagicArtilleryMode` | UNPARSED | 2 | 27 | HIGH |
| VEHICLE | `hasRadar` | UNPARSED | 2 | 137 | HIGH |
| VEHICLE | `hasScope` | UNPARSED | 2 | 234 | HIGH |
| VEHICLE | `HasSmoke` | UNPARSED | 3 | 170 | HIGH |
| VEHICLE | `heliGUI` | UNPARSED | 2 | 51 | HIGH |
| VEHICLE | `heliGuiSeat` | UNPARSED | 2 | 51 | HIGH |
| VEHICLE | `helipad` | UNPARSED | 2 | 113 | HIGH |
| VEHICLE | `hijackablePilot` | UNPARSED | 1 | 4 | HIGH |
| VEHICLE | `invincible` | UNPARSED | 3 | 279 | HIGH |
| VEHICLE | `invisiblePassenger` | UNPARSED | 2 | 115 | HIGH |
| VEHICLE | `isExplosionWhenDestroyedRadius` | UNPARSED | 3 | 200 | HIGH |
| VEHICLE | `ItemID` | UNPARSED | 4 | 308 | HIGH |
| VEHICLE | `labjacFuel` | UNPARSED | 1 | 3 | HIGH |
| VEHICLE | `LeftLinkPoint+` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `loudCannon` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `maxAltitude` | UNPARSED | 2 | 2 | HIGH |
| VEHICLE | `maxOxygen` | UNPARSED | 2 | 23 | HIGH |
| VEHICLE | `MomentOfInertia` | UNPARSED | 2 | 10 | HIGH |
| VEHICLE | `needsThrottle` | UNPARSED | 2 | 7 | HIGH |
| VEHICLE | `nightScope` | UNPARSED | 2 | 99 | HIGH |
| VEHICLE | `nuclearDeath` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `NumWheels` | UNPARSED | 5 | 211 | HIGH |
| VEHICLE | `overheatLimit` | UNPARSED | 1 | 11 | HIGH |
| VEHICLE | `overheatPenalty` | UNPARSED | 1 | 11 | HIGH |
| VEHICLE | `oxygen` | UNPARSED | 2 | 23 | HIGH |
| VEHICLE | `PassengerHurtable` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `passengerZoom` | UNPARSED | 2 | 4 | HIGH |
| VEHICLE | `PlaceTime` | UNPARSED | 1 | 3 | HIGH |
| VEHICLE | `primaryRecoilStrength` | UNPARSED | 2 | 21 | HIGH |
| VEHICLE | `radarDetectionRangeMultiplier` | UNPARSED | 1 | 17 | HIGH |
| VEHICLE | `radarPositionOffset` | UNPARSED | 2 | 126 | HIGH |
| VEHICLE | `radarRange` | UNPARSED | 2 | 126 | HIGH |
| VEHICLE | `radarRefreshDelay` | UNPARSED | 2 | 126 | HIGH |
| VEHICLE | `radarVisible` | UNPARSED | 3 | 164 | HIGH |
| VEHICLE | `RecoilDistance` | UNPARSED | 2 | 11 | HIGH |
| VEHICLE | `RestrictInventoryInput` | UNPARSED | 1 | 2 | HIGH |
| VEHICLE | `secondaryRecoilStrength` | UNPARSED | 1 | 6 | HIGH |
| VEHICLE | `slbmDelay` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `slbmFlightType` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `slbmRange` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `slbmStrength` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `slbmWarheadType` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `solid` | UNPARSED | 3 | 140 | HIGH |
| VEHICLE | `SoundsPlaceTimePrimary` | UNPARSED | 1 | 16 | HIGH |
| VEHICLE | `Stabilizer` | UNPARSED | 1 | 1 | HIGH |
| VEHICLE | `SurfaceSpeed` | UNPARSED | 3 | 25 | HIGH |
| VEHICLE | `terrainPenalty` | UNPARSED | 2 | 29 | HIGH |
| VEHICLE | `transport` | UNPARSED | 2 | 23 | HIGH |
| VEHICLE | `TurretRotationSpeed` | UNPARSED | 2 | 2 | HIGH |
| VEHICLE | `unlimitedOxygen` | UNPARSED | 2 | 2 | HIGH |
| VEHICLE | `walterCalculator` | UNPARSED | 1 | 12 | HIGH |
| VEHICLE | `walterMortar` | UNPARSED | 1 | 5 | HIGH |
| VEHICLE | `WeakspotCookTime` | UNPARSED | 1 | 74 | HIGH |
| VEHICLE | `weightLimit` | UNPARSED | 1 | 8 | HIGH |
