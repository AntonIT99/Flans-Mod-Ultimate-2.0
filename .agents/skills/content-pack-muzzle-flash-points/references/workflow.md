# Muzzle Flash Point Authoring Workflow

Paths are relative to the repository root. `<pack>` is the resolved folder
`src/<set>/resources/flans_content/<pack>/`, and `<set>` is its source set.

## 1. Build the scoped inventory

Read every file below `<pack>/definitions/guns/`. Record at least `ShortName`,
`Model`, `ModelScale`, `Deployable`, `DeployedModel`, `FlashModel`,
`MuzzleFlashModel`, `animFlashScale`, `animMuzzleFlashPoint`, the firing mechanism,
and evidence of suppression. Definition parsing is case-insensitive, but preserve
the authored spelling when reporting.

Only the hand-held `Model` resolves to the `ModelGun` field handled here.
`DeployedModel` normally resolves to `ModelMG`; inventory it only to avoid mistaking
it for the hand-held source. A gun used by a vehicle remains in scope when its
definition also names a `ModelGun`.

Resolve each `Model` exactly as the current `InfoType.findModelClass` logic does.
For ordinary legacy names, `<package>.<name>` resolves to
`com.flansmod.client.model.<package>.Model<name>`, but redirects and 1.12-style
packages exist. Verify the actual `package` declaration and source file rather than
assuming the common form. Build a reverse map from each model class to every
selected-pack definition that references it. Do not include unreferenced classes
just because they share the package.

For every distinct actual model package, run a report into ignored scratch space.
Quote every Gradle property in PowerShell so dots are not parsed as task syntax:

```powershell
.\gradlew.bat muzzleReport "-PmodelSourceSet=<set>" "-PmodelPackage=<exact.package>" "-PreportFile=build/tmp/muzzle-flash-points/<pack>-<package>.tsv"
```

Package casing must match the source declaration. Record classes that fail to
construct; they have no report evidence and cannot receive an automatic candidate.

## 2. Decide eligibility from the weapon, not the class name

Eligible by default:

- pistols, revolvers, machine pistols;
- SMGs, rifles, carbines, shotguns;
- LMGs, GPMGs, HMGs, anti-materiel rifles;
- conventional handheld or mounted autocannon represented by a `ModelGun`.

Ineligible for a conventional front muzzle flash:

- integrally suppressed firearms such as a Welrod, De Lisle, Sten Mk II(S), VSS,
  AS Val, or MP5SD;
- rocket launchers, recoilless rifles, Panzerfaust-like launchers, and launch tubes;
- flamethrowers, bows, crossbows, melee weapons, tools, throwables, and air or gas
  guns;
- mortars and low-pressure grenade launchers unless the pack already establishes a
  small front-flash convention for that exact family.

Treat fictional and energy weapons case by case. They are eligible only when the
pack's own definitions or assets establish a muzzle-origin effect and the model has
a physical emission aperture. Do not apply ordinary firearm assumptions to a
laser, plasma weapon, or magic effect.

Eligibility controls which models this skill edits; it does not authorize adding
or removing an effect. If one model is shared by eligible and ineligible consumers,
inspect whether a single harmless geometric muzzle default is semantically valid
for all of them. Skip on any conflict.

## 3. Evaluate the report measurement

The report constructs the class, so its bounds already include constructor-time
geometry mutation. Use the gun table and inspect these fields:

- `muzzlePx`: proposed bore exit in final model pixels;
- `muzzleSourceGroup`: geometry group that reached the front face;
- `bodyMuzzlePx` and `muzzleFacePx`: body endpoint and face dimensions;
- `barrelAttachPx` and `attachDeltaPx`: supporting evidence only;
- `declaredMuzzleFlashPoint`: an existing point, which makes the class add-ineligible;
- `flashScale`: final model default used for conversion;
- `notes`: rotated geometry and default-barrel warnings.

Use these decision gates:

| Report state | Decision |
|---|---|
| `measured-agrees` | Candidate, after source inspection confirms the bore. |
| `measured-no-attach-point` | Candidate only when the source group is plainly a barrel-bearing group such as `gunModel`, `slideModel`, `breakActionModel`, or `defaultBarrelModel`. |
| `measured-attach-point-disagrees` | Inspect both values and the source. Accept only when the measured face is demonstrably the bore and the authored attach point is unrelated or stale. |
| `no-geometry` / failed construction | Skip unless an exact point can be derived from clear source geometry and independently verified; never estimate from weapon length. |

Then inspect the contributing boxes in the Java source:

1. Confirm +X is the gun's final forward axis and `muzzlePx.x` is the foremost
   actual barrel endpoint, not a sight, bayonet, folded bipod, sling, stock, or
   decoration.
2. Confirm the reported Y and Z are centered on the bore. A large or asymmetric
   `muzzleFacePx` often means the report joined unrelated front geometry.
3. Read every `notes` entry. Rotated parts are excluded from the primary result.
   If a rotated part is the real barrel or extends the bore, accept only after its
   transformed endpoint is derived exactly and checked; otherwise skip.
4. When `defaultBarrelModel` reaches beyond the body, the normal no-attachment
   muzzle is its tip. Confirm attachment models can provide their own offset before
   accepting the class default.
5. For a minigun or another multi-barrel face, accept one centered point only if
   the renderer genuinely uses a shared central flash and all bores form an
   unambiguous symmetric cluster. Otherwise skip; one `ModelGun` point cannot
   represent independently firing muzzles.

`barrelAttachPoint` may agree by coincidence and may be useful corroboration, but
never copy it as the muzzle. `hasFlash` is not evidence.

## 4. Convert and author the point

The item renderer applies `flashScale` before translating by `muzzleFlashPoint`.
For each axis:

```text
point = muzzlePx / 16 / flashScale
reconstructedMuzzlePx = point * flashScale * 16
```

Use the final constructor value of `flashScale`; if none is assigned, use `1`.
Never multiply or divide by definition `ModelScale`: that outer scale affects both
the gun and flash equally.

Before editing, inspect every definition that shares the model:

- An existing `animMuzzleFlashPoint` overrides the model field at runtime. It does
  not make the geometric model default wrong, but record that the new default is
  not exercised by that definition.
- An `animFlashScale` differing from the model default moves a model-authored point
  unless that definition also supplies its own `animMuzzleFlashPoint`. If any
  current consumer has this unmatched scale override, skip the model and report
  that it needs a coordinated per-definition point under the visual-effects
  workflow.
- Several consumers with incompatible effective scales or muzzle semantics are a
  hard skip for a single model-level default.

Add one assignment in the constructor near the other rendering or animation
metadata, normally after the last `flipAll`/`translateAll` call and near
`flashScale`. Those transforms do not update the vector, so author final renderer
coordinates regardless of textual placement. Match the file's indentation,
numeric style, encoding, and line endings. Add
`com.flansmod.common.vector.Vector3f` only when the class does not already import
it. Example shape:

```java
muzzleFlashPoint = new Vector3f(1.21875F, 0.35156F, 0.00156F);
```

Keep enough decimal places that each reconstructed axis differs from `muzzlePx` by
at most 0.5 px. Do not reformat the generated-looking model geometry or reorder
unrelated constructor statements.

## 5. Validation

1. Re-run `muzzleReport` for every touched package. Each edited row must now show
   the intended `declaredMuzzleFlashPoint`. Reconstruct
   `declared point * flashScale * 16` and compare it with the accepted `muzzlePx`;
   tolerance is 0.5 px per axis.
2. Re-read every touched class and its reverse-mapped definitions. Confirm there is
   exactly one assignment, the `Vector3f` import resolves, no ineligible gun caused
   the edit, and no unmatched `animFlashScale` override invalidates it.
3. Run the pack's jar task found in `src/<set>/fmu-module.gradle`. Run
   `packsManagerJar` and/or `officialPacksJar` too when the selected pack is an
   input to those artifacts, as required by the repository rules. The report task
   already compiles the touched source set, but it does not validate packaging.
4. Run `git diff --check -- <touched paths>`, then inspect the scoped diff. Apart
   from a necessary import, every model diff should be one added assignment. Ensure
   unrelated pre-existing work remains untouched.
5. Static geometry cannot prove appearance in first and third person. List for an
   in-game firing check every accepted disagreement row, manual transformed point,
   default-barrel case, minigun/multi-barrel case, unusually broad muzzle face, or
   consumer whose own override hides the new default.

Do not edit the wiki for ordinary point additions: this is model data using an
existing field. Report a wiki need only if the field's behavior or supported
format changed, which is outside this skill.

## 6. Report

Use these sections, omitting only empty existing-issue sections:

1. **Added** or **Candidates**: model class, referencing definitions, point, source
   `muzzlePx`, `flashScale`, report status, and source-geometry evidence.
2. **Skipped**: model and one concrete reason such as `ineligible`, `already has
   point`, `no reliable bore`, `rotated barrel`, `shared-model conflict`, or
   `unmatched animFlashScale`.
3. **Existing issues**: suspicious authored points or definition overrides; do not
   repair them silently.
4. **In-game check candidates**: the short risk-ranked list from validation.
5. **Validation**: commands and results, including anything not run.
6. **Wiki**: normally `no update needed; model data only`.
