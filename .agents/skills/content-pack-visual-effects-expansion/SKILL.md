---
name: content-pack-visual-effects-expansion
description: Audit and expand the visual effects of one tracked Flan content pack (gun muzzle flashes, vehicle and aircraft shoot particles, engine exhaust and damage emitters, projectile trails and explosion particles, grenade and attachment visuals) by adding only realistic, well-placed effects to existing definitions. Takes the pack as its argument. Use for pack presentation work, not parser changes, balance, or new content.
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

## Before acting

1. Read the repository `AGENTS.md` and any nested `AGENTS.md` under the pack.
2. Read `../content-pack-definition-sync/SKILL.md` for definition editing
   discipline: encoding, line endings, repeatable-property safety, and no
   reformatting.
3. Read [references/workflow.md](references/workflow.md) completely. It holds the
   effect catalogue, placement math, sizing tables, budgets and report format.
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
  and report. Apply mode also covers the built-in flash asset, which already ships
  with the mod; it never authorizes Java, parser or category changes.

## Non-negotiable rules

- Add, never overwrite. Keep every existing effect line and value. The one
  exception is a line that is provably broken, such as an unknown particle name
  or a flash on an integrally suppressed weapon. Report it; repair it only when the
  user confirms.
- No muzzle flash without a known position. A flash requires one of these:
  a declared `muzzleFlashPoint`, a `muzzleReport` measurement that passes the
  checks in the workflow, or a verified same-model-family match with a reference.
  Otherwise skip it and list it under "no reliable position".
- Realism gates every addition: no flash on melee weapons, bows, air or gas guns,
  or suppressed weapons, and none at the front of recoilless or rocket launchers.
  The workflow has the full eligibility table.
- Reuse before adding. If the pack already ships a flash model and texture, use
  that pair. Otherwise use the built-in `FlashModel DefaultFlash`, which needs no
  texture line. Never copy flash classes or textures between packs.
- Never force the world muzzle-flash particle on (`ShowMuzzleFlashParticle True`).
  It is governed by the server config `muzzleFlashParticlesDefault`. Writing
  `ShowMuzzleFlashParticle False` is allowed for weapons that must never flash.
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
- family matches with scale differences;
- vehicles whose authored shoot point sits far from the measured muzzle;
- any placement estimated rather than measured.

Offer to check them in game.

## Finishing

- Run the validation in `references/workflow.md` §8, including
  `git diff --check` on the touched files, and report anything not run.
- Check whether the wiki (`../Flans-Mod-Ultimate-2.0.wiki`, read-only unless the
  user asks) documents every key used. Report missing documentation rather than
  editing the wiki unprompted.
- Stage explicit paths only. Never stage `decompiled/`.
