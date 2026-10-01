---
name: content-pack-muzzle-flash-points
description: Audit and author plausible muzzleFlashPoint coordinates in existing Java ModelGun sources for eligible guns in one tracked Flan content pack, using rendered-geometry reports and source inspection. Use for model-point coverage, not flash assets, definition effects, driveable shoot points, parser changes, or new models.
---

# Content Pack Muzzle Flash Points

Take one argument: the source content pack to work on, such as `ww2` or
`modernwarfare`. Resolve it to exactly one tracked folder at
`src/*/resources/flans_content/<pack>/` and to the Java model sources shipped by
that source set. If the name is missing, matches no pack, or matches more than one,
stop and ask rather than broadening the scope.

The goal is narrow: give an existing eligible `ModelGun` a defensible
`muzzleFlashPoint` at its physical bore exit. A point does not enable a flash by
itself; it supplies a model default for definitions that render one. A missing
point is preferable to a confidently wrong point.

## Before acting

1. Read the repository `AGENTS.md` and any nested instructions that cover the
   selected pack or model sources.
2. Read [references/workflow.md](references/workflow.md) completely. It defines
   eligibility, model resolution, measurement gates, coordinate conversion,
   source-edit discipline, validation, and reporting.
3. Use `../flans-category-research/SKILL.md` read-only only when the weapon's
   identity, firing mechanism, or integral suppression is genuinely ambiguous.
4. Use `../mod-class-decompilation/SKILL.md` only when the selected pack's model
   exists solely as bytecode. Decompilation is evidence; never edit or ship the
   decompiled source.

## Modes

- **Audit** when the user asks to inspect, audit, report, or recommend. Make no
  source changes. Report candidates and skipped models with their evidence.
- **Apply** when the user asks to add, author, create, or fill the points. Add only
  candidates that pass every workflow gate, then validate the compiled result.

## Boundaries

- Edit only tracked Java classes that extend `ModelGun` and are referenced by an
  eligible gun definition in the selected pack. `ModelMG` deployable models are
  measured automatically by their renderer and do not use this field. Driveable,
  AA-gun, projectile, attachment, and flash-model classes are out of scope.
- Add missing `muzzleFlashPoint` assignments only. Do not add `FlashModel`,
  `MuzzleFlashModel`, textures, particles, `anim*` definition keys, or parser and
  renderer code. Do not edit definitions merely to make a candidate easier.
- Never edit `run/flan`, archives, build output, generated metadata, or sibling
  reference projects. Keep scratch reports under `build/` or another ignored
  temporary location.
- Preserve every existing point. Report a provably suspicious point, but replace
  it only under an explicit repair request. Treat `hasFlash` as irrelevant; the
  renderer does not use it to locate the muzzle.
- One model class has one default point. If definitions sharing it disagree about
  the muzzle, flash-scale semantics, or eligibility, skip it and report the
  conflict instead of choosing one consumer.
- Do not claim visual correctness from static checks. Always identify the riskiest
  additions for an in-game firing check.

## Evidence standard

Run the repository's `muzzleReport` for every actual package containing a scoped
model. The report is the starting measurement, not automatic authority. Inspect
the Java geometry for every candidate and accept only a point that is at the
forward end of a real barrel, centered on a plausible bore, after constructor-time
`flipAll` and `translateAll` operations.

Reject or defer candidates selected from a sight, bayonet, bipod, stock,
decoration, the wrong end of the gun, an unresolved rotated barrel, or an ambiguous
multi-barrel face. Never substitute `barrelAttachPoint`: it is usually an
attachment origin, not the muzzle.

The model field uses flash-scaled model blocks:

```text
muzzleFlashPoint = muzzlePx / (16 * flashScale)
```

Use the model's final constructor `flashScale`, defaulting to `1`. Do not include
the definition's `ModelScale`. Round only after conversion and keep enough digits
to reconstruct every measured axis within 0.5 model pixel.

## Finish

- Follow every check in the workflow, including a post-edit `muzzleReport`, the
  applicable pack jar task, scoped diff review, and `git diff --check`.
- Do not stage files unless the user asks. If staging is requested, stage only the
  explicit model and skill-related paths; never stage unrelated work.
- Report added points, skipped models with reasons, existing issues, suspicious
  in-game-check candidates, validation run or omitted, and whether the wiki needs
  any change. Ordinary model-data additions do not require a wiki edit.
