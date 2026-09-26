---
name: mod-class-decompilation
description: Decompile compiled Minecraft mod or content-pack classes (directories, .jar or .zip) into this repository's gitignored decompiled/ cache with Vineflower, CFR fallback and MCP searge renaming, and keep decompiled/INVENTORY.txt current. Use when a reference model, type or mod class exists only as bytecode; not for editing reference projects or shipping decompiled code.
---

# Mod Class Decompilation

Turns reference bytecode into readable, searchable Java under `decompiled/` at the
repository root, so other workflows can read model coordinates, legacy behaviour or
parameter handling that only exists in compiled form. Decompiled output is reference
material: never copy it into tracked sources, commit it, or ship it, unless the user
explicitly clears that for a specific piece and its licensing.

## Cache layout

- `decompiled/` is gitignored. Never stage anything under it.
- `decompiled/<slug>/<package path>/<Class>.java`: one slug per source container.
  Use a stable, lowercase slug naming the source and version, such as
  `tap-hero-shooter-september` or `krishna-mk6c`.
- `decompiled/INVENTORY.txt`: one tab-separated row per class, with the date,
  slug, source container path, class path inside the source, output path,
  decompiler, mappings and class SHA-1. The script rewrites a slug's rows when it
  decompiles them again. Keep the file in that format if you ever edit it by hand.

## Before decompiling

1. Search the cache and inventory first. A class already there with the same
   source path and SHA-1 needs no work; the script skips it automatically.
2. Prefer tracked sources when they exist. Warfare 44, the Manus packs and other
   `src/*/java` model sets are already Java; decompile only what has no source.
3. Scope the request to the classes needed, such as one model package, not a whole
   modpack, unless the user asks for a full dump.
4. Pick the mappings by the source's Minecraft version: `1.7.10` for 1.7.10 packs
   and mods (Tyrants and Plebeians, Krishna), `1.12` for 1.12.2, `none` for
   modern mods or classes that reference no searge names. A pack's `pack.mcmeta`,
   mod metadata, or the loader it installs into usually settles the version.

## Running

Run with PowerShell 7 from the repository root:

```powershell
pwsh -NoProfile -File .agents/skills/mod-class-decompilation/scripts/decompile.ps1 `
  -Source "<directory, .jar or .zip>" -Slug <slug> `
  -Include "com/flansmod/client/model/NewAge/**" -Mappings 1.7.10
```

- `-Include` takes class paths relative to the source root with `/` separators,
  as a list or one comma-separated string. `*` matches within a directory level and
  `**` across levels. Inner classes (`Outer$Inner.class`) are staged with their
  outer class and decompiled into it.
- `-Force` re-decompiles unchanged classes, for example after a decompiler update.
- `-WorkDirectory` defaults to a temp folder; point it into the session scratchpad
  when one exists. It is deleted after a clean run and kept for inspection when
  classes fail.
- Exit code 2 means some classes failed in both Vineflower and CFR. Report them;
  `D:\Tools\jd-gui\jd-gui-1.6.6.jar` or IntelliJ's decompiler can be tried by hand
  for a single stubborn class, with the result saved into the cache and an inventory
  row added in the same format.

Tools the script expects:

- Vineflower `D:\Tools\Vineflower\vineflower-1.12.0.jar`, the primary decompiler.
- CFR `D:\Tools\cfr\cfr-0.152.jar`, the per-class fallback.
- MCP CSV mappings under
  `D:\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\<version> stable mappings`,
  applied by `scripts/apply_mcp_mappings.py`, which needs Python 3 (`py`) and renames
  `field_*`, `func_*` and `p_*` tokens.

The user's own workspace at `D:\Tools\Vineflower\workspace` belongs to them: read
it for reference only, never write there.

## After decompiling

- Check the mapping report. A handful of unmapped searge names is normal, since
  mappings do not cover every member. Many unmapped names suggest the wrong
  mapping version.
- Treat decompiled numbers as exact but the code shape as approximate. Constant
  folding, reordered statements and synthetic helpers are decompiler artifacts, not
  evidence of the original author's intent.
- Tell the user which slug and classes were added, and whether any failed.
