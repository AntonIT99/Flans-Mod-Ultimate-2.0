---
name: content-pack-visual-effects-expansion
description: Audit and expand the visual effects of one tracked Flan content pack, including muzzle-flash coverage for eligible small arms when hand-held, deployed, or mounted in driveables, layered muzzle-blast particles for vehicle cannons, AA-gun barrel and ammunition visuals, plus emitters, projectile and explosion effects, and grenade and attachment visuals. Takes the pack as its argument. Use for pack presentation work, not parser changes, balance, or new content.
---

# Content Pack Visual Effects Expansion

Takes one argument: the source content pack to work on, such as `ww2` or
`warfare44`. Resolve it to exactly one tracked folder
`src/*/resources/flans_content/<pack>/` and its model sources under
`src/*/java/com/flansmod/client/model/...`. If the name matches no folder, or more
than one, stop and ask. Never edit `run/flan`, archives, build output, generated
metadata, or the reference projects.

Goal: existing items, vehicles and projectiles gain the effects a player would
expect, placed where they physically belong. Every addition must be realistic for
the item, positioned from evidence, and consistent with the pack's own style. An
effect placed in the wrong spot is worse than none, so when in doubt, skip it and
report it.

Muzzle-flash coverage is exhaustive within those gates: every eligible small arm
must be checked in every supported firing context. This includes hand-held and
deployable gun models, driveable primary and secondary gun banks, pilot guns, and
passenger guns. A definition being mounted in a vehicle is never a reason to omit
its flash.

Vehicle-cannon muzzle-blast coverage is exhaustive too. Check every primary and
secondary cannon bank and every passenger cannon mount and give it a
calibre-appropriate combination of pressure flash, flame and smoke at the exact fired barrel. Preserve a good existing set;
supplement a visibly incomplete set without duplicating its existing layers.

Include every `AAGunType` in the audit. Synchronize and verify each AA barrel,
audit every referenced ammunition type for appropriate projectile and impact
visuals, and record the muzzle-effect design it should eventually use. The current
AA-gun parser and renderer expose no flash-model or shoot-particle key, so never
write an inert muzzle-effect line to an AA-gun definition; report that part as an
engine limitation.

## Before acting

1. Read the repository `AGENTS.md` and any nested `AGENTS.md` under the pack.
2. Read `../content-pack-definition-sync/SKILL.md` for definition editing
   discipline: encoding, line endings, repeatable-property safety, and no
   reformatting.
3. Read [references/workflow.md](references/workflow.md) completely. It holds the
   effect catalogue, placement math, sizing tables, budgets and report format. For
   driveable and AA weapons, also read
   [references/muzzle-blast-proposals.md](references/muzzle-blast-proposals.md),
   which provides the source-informed design palette and ready-to-adapt sets.
4. Load `../mod-class-decompilation/SKILL.md` whenever a reference model or class
   exists only as bytecode, such as Tyrants and Plebeians.
5. Use `../flans-category-research/SKILL.md` read-only, when a weapon's class
   (calibre, role, suppressed or not) is unclear from the definition. Never change
   categories in this workflow.

## Modes

- **Audit** (default when the request is a question or says "audit" or
  "recommend"): read-only. Produce the report in `references/workflow.md` §9 with
  every proposed line, its evidence and its placement.
- **Apply**: make the audited changes inside the selected pack only, then validate
  and report. Apply mode also covers the built-in `FlashModel DefaultFlash` asset
  for guns and driveables, which already ships with the mod, and syncing the pack's
  shoot points with `shootPointSync -Pwrite` before placing flashes or particles on
  them. It never authorizes Java, parser or category changes.

## Non-negotiable rules

- Add, never overwrite. Keep every existing effect line and value. There are two
  exceptions. A line that is provably broken, such as an unknown particle name or a
  flash on an integrally suppressed weapon: report it, and repair it only when the
  user confirms. And driveable position lines (`ShootPointPrimary`,
  `BarrelPosition`, `GunOrigin`, AA gun `Barrel`): these change only through
  `gradlew shootPointSync -Pwrite` filtered to the pack, in apply mode, and never
  by hand (workflow §5.1 and §5.2).
- No muzzle effect without a known position; never guess a muzzle.
  - A hand-held gun flash requires a declared `muzzleFlashPoint`, a `muzzleReport`
    measurement that passes the workflow checks, or a verified same-model-family
    match with a reference. A deployable gun flash requires a `measured` deployable
    row for its `DeployedModel` and the source-model checks in workflow §3.6.
  - A driveable flash or bank/passenger shoot-particle set requires each affected
    shoot point or passenger `GunOrigin` to be measured and synced by `shootPointSync`, or to pass
    the workflow's authored-only check. The driveable flash switch covers all of
    its gun/shell banks and passenger guns together, so mixed eligible and
    ineligible muzzles must skip that flash switch rather than partially enable it.
    Per-seat particle effects remain available for independently eligible guns.
  - Otherwise skip it and list it under "no reliable position".
- Realism gates every addition: no flash on melee weapons, bows, air or gas guns,
  or suppressed weapons, and none at the front of recoilless or rocket launchers.
  The workflow has the full eligibility table.
- Reuse before adding. If the pack already ships a flash model and texture, use
  that pair. Otherwise use the built-in `FlashModel DefaultFlash`, which needs no
  texture line. Driveables support only `FlashModel DefaultFlash`; mounted gun
  definitions may still supply their own gun flash. Never copy flash classes or
  textures between packs.
- Never force the world muzzle-flash particle on (`ShowMuzzleFlashParticle True`).
  It is governed by the server config `muzzleFlashParticlesDefault`. Writing
  `ShowMuzzleFlashParticle False` is allowed for weapons that must never flash.
- Every vehicle-cannon bank supported by `ShootParticlesPrimary` or
  `ShootParticlesSecondary`, and every passenger cannon supported by
  `ShootParticlesPassenger`, must be audited for a layered muzzle blast. Preserve
  complete existing sets and add only missing flash, flame or smoke layers to
  sparse sets. Creative combinations are encouraged when every particle has a
  clear visual role. Match calibre, rate of fire, barrel layout, muzzle-brake
  geometry and the pack's established style; workflow §5.2 defines the palette,
  example sets and placement gates. Passenger particles are per seat and do not
  require the driveable-wide flash switch; mixed flash eligibility does not block
  effects on independently eligible passenger guns.
- Every AA-gun definition must be classified and audited even though it currently
  cannot render a muzzle flash or shoot-particle set. Validate its `NumBarrels`,
  synchronized `Barrel` origins, `FireAlternately` fan-out and referenced ammo;
  apply eligible projectile effects and report the proposed muzzle design under
  `engine limitation` (workflow §5.5).
- Gameplay-bearing keys are out of scope: `AddSmokePoint`/`AddSmokeDispenser` with
  `HasFlare`, `SmokeTime`, `SmokeRadius`, `FlashBang`, explosion power or radius,
  `Fire` and damage. Report candidates only.
- Stay inside the particle budget in the workflow. The engine already scales
  explosion visuals with charge mass, so never inflate authored counts to make
  explosions look bigger.

## Checks and in-game verification

Validation is static unless the user explicitly confirms an in-game check (the
`run` skill, a client and screenshots). Always finish with a short list of
**suspicious items**, the specific definitions most likely to look wrong in game:
- measurements that disagree with the model's barrel attach point;
- models with rotated barrel parts;
- deployable models whose automatic front-face measurement may have selected a
  sight, shield, decoration, or the wrong end instead of the bore;
- family matches with scale differences;
- driveable values in the `shootPointSync` report's Check first table, and banks
  or passenger guns placed by the authored-only check;
- unusually dense cannon particle sets, or a supplemented set whose authored
  particles do not clearly reveal their intended visual layer;
- AA guns with missing, suspicious or mismatched measured barrels, simultaneous
  multi-barrel fire, or ammunition whose visual role is unclear;
- driveables with mixed flash eligibility, such as an ordinary machine gun beside
  an integrally suppressed passenger gun;
- any placement estimated rather than measured.

Offer to check them in game.

## Finishing

- Run the validation in `references/workflow.md` §8, including
  `git diff --check` on the touched files, and report anything not run.
- Check whether the wiki (`../Flans-Mod-Ultimate-2.0.wiki`, read-only unless the
  user asks) documents every key used. Report missing documentation rather than
  editing the wiki unprompted.
- Stage explicit paths only. Never stage `decompiled/`.
