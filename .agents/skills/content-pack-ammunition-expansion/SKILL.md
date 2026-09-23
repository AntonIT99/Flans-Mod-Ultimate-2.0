---
name: content-pack-ammunition-expansion
description: Audit, repair, and historically expand ammunition in a selected Flan content pack, including ground-vehicle ammo references, shell and mixed-belt definitions, category ammo groups, localization, reused icons, harmonized statistics and effects, and pack validation. Use for pack data work, not parser implementation or general category coverage.
---

# Content Pack Ammunition Expansion

Apply this workflow to one user-selected source content pack. Work in its tracked
`src/*/resources/flans_content/<pack>` inputs and the shipped category defaults;
never edit `run/flan`, archives, build output, or generated runtime metadata.

Before acting:

1. Read the repository `AGENTS.md` and any nested instructions.
2. Read `../flans-category-research/SKILL.md`,
   `../flans-category-research/references/format-and-validation.md`, its research
   policy, and its guns/ammunition reference. That skill owns historical evidence,
   category membership, ordering, units, and ammunition groups.
3. Read `../content-pack-definition-sync/SKILL.md` and its routed references when
   synchronizing pack definitions with category values. That skill owns scalar
   mirroring, repeatable-property safety, fallbacks, encoding, and line endings.
4. Read [references/workflow.md](references/workflow.md) completely before an audit,
   repair, or expansion.

Treat category research and definition synchronization as separate phases within the
same task: establish or repair shipped category truth under `flans-category-research`,
then synchronize the selected pack's source definitions under
`content-pack-definition-sync`. Never use the synchronization phase to invent or
silently revise category values.

Respect the requested mode. An audit or recommendation request is read-only. A
repair or expansion request authorizes changes only inside the selected pack and
the directly required shipped categories, localization, and source assets.

Do not copy Manus WW2 identities, numerical values, or ammo selections into another
pack. Re-establish every vehicle, gun, and round from that pack's own definitions
and defensible historical sources. Preserve unrelated pack behavior and avoid a
broad rebalance unless the user asks for one.
