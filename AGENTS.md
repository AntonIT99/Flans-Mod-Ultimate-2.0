# AGENTS.md

Nested `AGENTS.md` files add rules for their directories.

## Task-specific workflows

- For version-dependent changes or work ported between maintained branches, read
  `.agents/skills/flans-version-porting/SKILL.md` before editing.
- For built-in `*_categories.json` research or maintenance, read
  `src/main/resources/config/AGENTS.md`, then
  `.agents/skills/flans-category-research/SKILL.md` and the references it routes to.

## Repository Rules

- Keep gameplay state server-authoritative. Do not initialize client-only classes
  from common or server code.
- Preserve deterministic legacy-content loading and compatibility. Do not bulk-change
  definition whitespace, casing, filenames, encodings, or layouts.
- Do not edit generated output, runtime files, or generated metadata; change the
  generator or its input instead.
- Keep the main mod, bundled packs, and official packs as separate artifacts. Run
  `packsManagerJar` and/or `officialPacksJar` when their inputs or packaging change.
- For every feature or significant change, check whether the locally available wiki
  repository should be updated too.
- In a mixed worktree, preserve unrelated changes and stage explicit paths only.

## External Reference Paths

These sibling directories live outside this repository, next to it in the parent
directory. Read them directly when behaviour, formats, or prior implementations need
checking. They are read-only references: never edit them unless explicitly asked.

- `../Flans-Mod-Ultimate-2.0.wiki` - this project's wiki repository. Consult it for
  documented behaviour, and update it for every feature or significant change.
- `../Flans-Mod-Ultimate-1.7.10` - the original 1.7.10 project. **Primary** reference for
  legacy behaviour, content-pack formats, and expected gameplay semantics.
- `../FlansMod` - the original 1.12.2 project. **Secondary** reference; use when 1.7.10 is
  ambiguous or the question concerns newer-loader concerns. If the two disagree, 1.7.10
  wins unless the user says otherwise.
- `../Flan's Mod Aryan Indian Edition Krishna Mk6C` - a heavily modified 1.7.10 fork, used
  occasionally as an extra reference for alternative implementations. Treat it as
  inspiration, not authority.

## Build and Validation

Use the Gradle wrapper. Common tasks are `test`, `build`, `runData`, `packsManagerJar`, and
`officialPacksJar`; use `--stacktrace` only to diagnose a failed build. Run focused
tests first. Run a full build after loader setup, registries, networking, entities,
resources, source sets, or packaging changes. Keep `gradlew` executable.

Before completion, review the scoped diff, run relevant checks and `git diff --check`,
and report validation that could not be performed.
