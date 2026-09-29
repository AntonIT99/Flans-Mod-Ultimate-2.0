# Driveable Geometry and Damage Constraints

Use this with the selected pack's current code and definitions, not as a frozen
schema. Confirm a behavior against the implementation if a future branch differs.

## Box interpretation

- `SetupPart` and its armored/crewed variants take
  `<part> <health> <x> <y> <z> <width> <height> <depth> [resistance] [crewMultiplier]`.
  Coordinates and sizes are authored in model pixels. Use the Java model's final
  constructed geometry and runtime transform, not an asset filename, to derive
  bounds. The renderer applies `ModelScale` to visible geometry, while projectile
  tracing uses the parsed part box; account for the scale when comparing their
  world-space outlines rather than assuming the box scales automatically. Check
  model rotations/translations too. The loader converts coordinates through
  `CollisionBox`; plane boxes receive an additional facing transform. Do not
  transpose or mirror coordinates by eye.
- The `DriveableType.health` map has one collision box per `EnumDriveablePart`.
  A second `SetupPart` for the same name replaces the first. Unsupported names
  are rejected. `CollisionBox` clamps negative dimensions to zero, so a negative
  size does not describe a box extending in the opposite direction.
- Damage and projectile traces use part boxes even when `FancyCollision` uses
  separate meshes for solid collision. A destroyed part's hitbox becomes inactive,
  allowing a later projectile through that region. Large overlaps can therefore
  hide a part, redirect shots, or change exposure after destruction.
- The `core` box is more than a damage region: its dimensions contribute to
  derived ground-vehicle geometry and entity bounds. Wheel/track box bottoms also
  affect contact clearance. Recheck those behaviors when reshaping them.
- A part name can carry other behavior: seat/gun/wheel ownership, rotation,
  armor, repair, and effects. Check all consumers before repurposing or removing
  a part. A novel subdivision is useful only if its supported identity and
  destruction consequence make sense.

## Armor and HP

- `VehicleArmorResolver` uses explicit `PartArmorMm` first, then relevant
  semantic armor, then hull armor, then unarmored. `ArmorFrontMm` and siblings
  already select the struck face of hull-fixed parts. `PartArmorMm` gives one
  unsloped value on every face of that part; it cannot express a separate front,
  side, and top profile. For missing armor values directly needed by a revised
  part layout, use vehicle-specific historical evidence and record the source;
  leave uncertain values unresolved rather than copying a different vehicle.
- `EnumDriveablePart.isTurretMounted` defines which named parts rotate and can
  inherit `TurretArmor*Mm`. Inspect that method rather than trusting a part's
  English name. A fixed upper hull may warrant a separate damage box, but
  calling it `turret` can make shots and armor face in the wrong direction.
- With valid `RealMassKg` and `UseRealisticVehicleHealth true`, total HP comes
  from mass; positive authored HP values are only relative weights. Nonpositive
  authored HP stays zero. Without that opt-in and valid mass, HP is absolute.
  A split changes shares and may change how quickly each component disappears;
  compute before/after shares and compare them with the represented structure.
- For aircraft and mechas, use their actual part hierarchy and motion instead
  of a ground-vehicle hull/turret template. For an angled hull, use a sensible
  axis-aligned box plus the armor slope fields where applicable; a box does not
  need to mimic every armor plate angle.

## Evidence and validation

Start from the selected pack's tracked definition and its resolved model. Use
model Java geometry, texture or render evidence, and existing neighboring
definitions as cross-checks. Treat real-world specifications as identity and
proportion evidence when relevant, not as direct coordinates for a stylized
model. If the model is missing, only compiled, or has unresolvable moving
geometry, mark the proposed measurement as uncertain and do not silently fill
the gap with a neighboring model's numbers.

For each edited vehicle, record a concise before/after inventory: part names,
box bounds, percentage of total authored HP, armor source, and the physical
feature represented. Inspect overlaps and gaps from at least front, side, and
top, and check moving parts at more than one pose. Validate the source pack's
packaging task and perform an in-game hit check for high-risk cases when
available; report what could not be verified.
