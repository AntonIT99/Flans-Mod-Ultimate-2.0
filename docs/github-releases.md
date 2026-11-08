# GitHub releases and tags

Use `<modid>-<minecraft-version>-<module-version>` for Git tags and GitHub
release titles. Each tag identifies the release commit for that Minecraft version.

| Release scope | Example tag and release title |
| --- | --- |
| Main mod only | `flansmodultimate-1.20.1-2.2` |
| Main mod patch | `flansmodultimate-1.20.1-2.2.1` |
| NPC submod only | `wolffsmodnpcs-1.20.1-2.2.1` |
| Main mod and NPC submod together | `flansmodultimate-1.20.1-2.2` |

Use the releasing module's actual version. The main mod may use `2.X` for a
major release and `2.X.Y` for a patch; do not append `.0` to a `2.X` version.
NPC versions use their independent `2.X.Y` version, so their patch number may
differ from the main mod's.

For a combined release, the main mod determines the tag and release title.
List both module versions in the release description and attach the intended
module and content-pack JARs, preserving each artifact's own version. For
example, `flansmodultimate-1.20.1-2.2` may include
`flansmodultimate-1.20.1-2.2.jar` and `wolffsmodnpcs-1.20.1-2.2.0.jar`.

If only the NPC submod changes after a combined release, create an NPC tag;
do not move or reuse the published main-mod tag. Create separate tags and
releases for each Minecraft version, using its matching branch and commit.

## Create and push tags

Create an annotated tag with `git tag -a`. Replace `<commit-hash>` with the
release commit; omit it to tag the current `HEAD`. The `-m` option supplies
the annotation message.

Main mod release:

```bash
git tag -a flansmodultimate-1.20.1-2.2 <commit-hash> -m "Flan's Mod Ultimate 2.2 for Minecraft 1.20.1"
git push origin flansmodultimate-1.20.1-2.2
```

NPC submod release:

```bash
git tag -a wolffsmodnpcs-1.20.1-2.2.0 <commit-hash> -m "NPC Vehicles & Soldiers 2.2.0 for Minecraft 1.20.1"
git push origin wolffsmodnpcs-1.20.1-2.2.0
```

For a combined release, use the main-mod example. Optionally create the NPC
tag at the same commit as well: multiple tags can point to one commit, while
a GitHub release is associated with one tag. Attach the combined GitHub release
to the main-mod tag.

Pushing a tag does not create a GitHub release. Create the release separately
on GitHub and select the existing tag. Use these commands for new tags only;
the examples above may already exist in the repository.

For CurseForge destinations, metadata and upload tasks, see
[CurseForge releases](curseforge-releases.md).
