# Content-Pack Ammunition Workflow

Use this workflow for a single selected content pack. Complete discovery and repairs
before adding new ammunition so recommendations reflect the pack's actual consumers
and do not conceal existing broken references.

## 1. Establish scope and inventory

- Identify the source-set module, pack root, Minecraft branch, module metadata, and
  applicable packaging task. Keep the main mod, bundled packs, and official packs as
  separate artifacts.
- Inventory ground vehicles, mounted guns and AA guns, bullet/shell definitions,
  pack-local English localization, item models, icons, recipes, and relevant category
  membership.
- Resolve ammunition reachability through every applicable path: direct `Ammo` or
  `AddAmmo`, mounted-gun ammunition, `AllowAllAmmo`, `AddToAmmoGroup`,
  `UseAmmoGroup`, removals, and per-ammo overrides. Do not infer reachability from a
  display name alone.
- Produce a vehicle-to-gun-to-ammunition matrix. Flag missing short names, wrong
  variants, anachronistic rounds, misspelled designations, stale localization,
  conflicting category values, and multi-weapon vehicles that lost one weapon's
  ammunition.
- When only an audit or recommendation was requested, stop after reporting evidence,
  uncertainty, and a prioritized essential/optional list. Do not edit files.

## 2. Repair existing content first

- Correct invalid or mismatched ammunition references before expanding the pack.
  Confirm the exact historical weapon variant, not merely its calibre.
- Preserve all weapon systems on multi-weapon vehicles. For example, a rocket-equipped
  tank may need both its rocket ammunition and its cannon shells reachable.
- Harmonize incorrect weapon and round designations across the definition `Name`,
  localization, category label, and description without changing `ShortName` merely
  for aesthetics.
- If a `ShortName` must change, add the new sanitized name to categories while
  retaining every former name. Never remove a legacy category short name unless the
  user explicitly requests retirement. This applies even when the old definition is
  absent from current source or scanner output.
- Remove an ammunition reference only when the user explicitly requests it or when
  the scoped repair proves it invalid and removal is necessary. Verify that the
  consumer retains at least one valid round.

## 3. Select historically meaningful additions

- Identify the exact gun, service period, platform, and ammunition family for each
  candidate. Prefer rounds that supply a missing tactical role actually usable by a
  pack vehicle: common HE/fragmentation, APCBC/APHE, APCR, HEAT, smoke, canister, or
  a historically documented mixed belt.
- Prioritize widely issued service ammunition over experimental, scarce, late-war,
  training, or platform-incompatible rounds. Separate essential additions from
  optional variants.
- Use the source hierarchy, unit rules, and uncertainty policy from
  `flans-category-research`. Keep projectile mass, filler, muzzle velocity,
  penetration, weapon designation, and date from one coherent configuration; never
  average incompatible sources.
- Prefer primary manuals and firing tables, then specialist references. Use game data
  only for a required gameplay field unavailable from stronger sources, disclose it,
  and keep it compatible with the repository's established balance conventions.

## 4. Author shell and belt definitions

- Follow the selected pack's filename and `ShortName` convention. Give each genuinely
  selectable new round a stable unique short name; do not rename existing items just
  to improve style.
- Use a consistent display form such as country, calibre, official projectile
  designation, functional type, and gun family where needed to disambiguate.
- At minimum, synchronize category-owned physical fields into the source definition:
  `Mass`, `MuzzleVelocity`, normally `FallSpeed 1`, explosive TNT-equivalent mass,
  penetration when nonzero, fragmentation type for explosive shells, and explicit
  submunition behavior where applicable. Preserve effective legacy damage and blast
  fallbacks unless the scoped task includes rebalance.
- Base recipes, inventory behavior, sounds, and hit behavior on the nearest compatible
  round in the same pack. Do not invent new parser keys without verifying the current
  type implementation and wiki.
- A mixed belt uses category-owned `AddRound` entries in this exact positional form:

  `AddRound <single-token-name> <count> <massG> <explosiveTNTG> <muzzleVelocityMps> <penetrationMm>`

  Use one line per component, positive counts, deliberate zeros, and a total count
  matching the repeating belt. The source definition must have `RoundsPerItem > 1`.
  Do not mirror `AddRound` into the definition. Preserve legacy scalar fallbacks when
  backward-compatible category membership supplies them; per-round values take
  precedence while firing.

## 5. Integrate categories and consumers

- Put every selectable round in one exact ammunition category unless compatible
  legacy membership must also remain. Add the round to a weapon-family group with
  `AddToAmmoGroup`, and add the exact matching `UseAmmoGroup` to each intended gun,
  AA gun, or driveable consumer.
- Keep direct `Ammo` or `AddAmmo` lines when the pack needs a legacy fallback. The
  resolved ammunition list must deduplicate direct and grouped reachability.
- Backward compatibility overrides cleanup: add new short names but retain old ones.
  Multiple same-file memberships are allowed only when shared properties agree and
  the combined behavior is intentional.
- Keep category labels in the documented natural/calibre order. Preserve indentation,
  numeric style, strict JSON, and unrelated memberships.
- Mirror single-value category scalars into maintained source definitions. Leave
  accumulating group and belt values category-owned unless duplication is proven
  idempotent. Run the definition-sync audit before and after editing.

## 6. Harmonize presentation without erasing pack identity

- Normalize round designations and obvious typos across the scoped ammunition set.
  Do not rename resources, filenames, or short names for aesthetics.
- Reuse existing source icons and textures when requested. Point each new definition
  `Icon` and item model at the selected existing asset; do not duplicate PNG files.
  Verify the referenced texture exists and that localization matches the definition
  name exactly.
- Match the pack's established visual language first. Warfare 44 may be used as a
  secondary reference: AP-family projectiles generally use tracer trails, explosive
  shells flame trails, and autocannon belts smoke trails. Scale explosion, flare, and
  debris particles by projectile class rather than copying one value everywhere.
  Keep lighting consistent for luminous tracer/flame effects.
- Harmonization is not permission for an unrelated pack-wide balance rewrite. Change
  physical statistics when category research supports them; change legacy damage,
  explosion, fire, or stack behavior only when necessary for the requested result.

## 7. Generated data and source assets

- Edit tracked source definitions and maintained assets, not `run/`, `build/`, pack
  archives, generated manifests, or generated runtime category copies.
- Let the repository's content pipeline generate recipe JSON and other derived data
  from source definitions. Add a maintained source item model or localization entry
  only when that module tracks such files and the generated/runtime path alone is not
  the repository convention.
- Preserve each touched definition's encoding and line ending. Mechanical line-ending
  normalization is allowed only to restore the file's original convention.

## 8. Validate the completed batch

Run the checks applicable to the changes:

1. Strictly parse changed JSON and reject duplicate keys.
2. Run the shortname coverage scanner; confirm every new definition is covered. Treat
   its CSV as generated output and do not leave an untracked report behind.
3. Compare category memberships before and after. No former short name may disappear
   without explicit retirement authorization.
4. Scan all category files for conflicting assignments to duplicate memberships.
5. Validate ammo groups bidirectionally and confirm every vehicle receives the
   intended AP/HE/rocket/belt choices without duplicates.
6. Validate every `AddRound`: six tokens, valid numeric values, positive count,
   deliberate filler/penetration zeros, correct total, and `RoundsPerItem > 1`.
7. Run `audit_definition_sync.py` on exactly the scoped ammunition definitions. End
   with zero scalar gaps, mismatches, uncategorized definitions, and unintended
   repeatable overlaps.
8. Confirm every new model resolves to an existing texture and every shell definition
   name matches localization.
9. Run focused ammo-group, mass-unit, belt-resolution, and recipe-parsing tests when
   relevant. Run the selected module's pack JAR task and any repository-required
   aggregate packaging/full-build task.
10. Review the scoped diff, preserve unrelated changes, check new untracked files for
    whitespace, and run `git diff --check`.

Report the repaired references, new rounds, belt composition, affected consumers,
reused assets, historical sources, weak/game fallbacks, category/definition audit
results, tests, packaging tasks, and any validation that could not be performed.
