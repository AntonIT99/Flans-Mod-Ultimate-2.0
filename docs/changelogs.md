# Changelogs and version bumps

Release notes live in `changelogs/<mod id>/<version>.md`, one file per version, for example
`changelogs/flansmodultimate/2.2.2.md` and `changelogs/wolffsmodnpcs/2.2.1.md`. The file matching the
current `mod_version` or the module's `archiveVersion` holds the next release's notes. The release
tasks upload it whole (see [CurseForge releases](curseforge-releases.md)); content-pack modules without a changelog
folder of their own upload the main mod's. These are the one exception to the documentation rule
in `AGENTS.md`: keep them current.

## Choosing the file

Before writing to a changelog, determine the file to write to:

1. Find the last released version of the main mod and the NPC module for this branch's
   `minecraft_version`, after `git fetch --tags` where a remote is reachable:
   `git tag -l '<mod id>-<minecraft version>-*' --sort=-v:refname`, taking the first result. Mod ids
   are `flansmodultimate` and `wolffsmodnpcs`. Tags without a mod id are older and are ignored.
2. Compare it with the current `mod_version` (`gradle.properties`) or `archiveVersion`
   (`src/npcs/fmu-module.gradle`) of each module the change touches, and classify the change. It
   affects the API when it changes the contract or classes of `com.flansmodultimate.api`.
3. Bump only when needed, leaving versions unchanged when the change does not touch that module:
   - API change: each module still on the `2.X` line of its last release moves to the next `2.X` line;
     a module already on a newer line keeps its version. The main mod becomes `2.X` without a trailing `.0`, for example `2.2.2` to
     `2.3`; the NPC module becomes `2.X.0`, for example `2.2.1` to `2.3.0`. Also update the main-mod
     `versionRange` in `src/npcs/resources/META-INF/mods.toml`, for example to `[2.3,2.4)`, and
     set `api_version` to the new `2.X`.
   - Other change: if the module's current version equals its last release, increment `Y` of that
     module only, for example main mod `2.2` to `2.2.1` or `2.2.1` to `2.2.2`, NPC `2.2.1` to `2.2.2`.
     The NPC module always has three numbers.
   - An unreleased version is bumped again only to go from a `Y` bump to a `2.X` bump, when an API
     change arrives on top of it. Otherwise keep it and write to that version's changelog file; never
     increment `Y` or `2.X` twice between releases, so further API changes on an unreleased `2.X`
     line do not move it again.
4. For each bumped module, create `changelogs/<mod id>/<new version>.md` in the same change, starting
   with the new version's heading in the same form as the module's previous file. If an unreleased
   version moves again, rename its file and update the heading rather than leaving a stale file.

## Writing entries

- Keep release notes selective: include major features, meaningful gameplay or usability changes,
  notable fixes of released behaviour, and compatibility or migration requirements. A change being
  visible to users does not by itself warrant an entry. Skip minor content corrections, individual
  asset or translation changes, small UI tweaks, refactoring, tests, tooling and internal cleanups.
- In the same change, update the relevant feature summary when the change meets that threshold.
  Group related improvements and fixes into one concise, general bullet describing their practical
  benefit; do not enumerate every affected item, parameter, setting, implementation detail or edge case.
- Write for users, in English, with one short sentence per bullet where possible and a few clear
  topic sections. Keep essential command names, opt-in controls and breaking changes in backticks;
  leave configuration syntax and exhaustive details to the appropriate reference documentation.
- Fold changes to something not yet released into its existing bullet instead of adding a fix line.
  Avoid repeating highlights in topic sections or appending unrelated details to an ever-growing
  bullet. Review the surrounding entries for overlap before adding one; prioritize a short, readable
  overview of the release over a complete record of the work.
- Keep each heading's version equal to `mod_version` (main mod) or the module's `archiveVersion`.
- Never edit the notes of a released version except to fix an error in them.
