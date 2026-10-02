# CurseForge releases

Every artifact has an independent upload task. A task is enabled only when its
configured project URL is nonblank. All destinations start blank;
regular `build`, `assemble`, and Maven publishing never upload to CurseForge.

Run the task from the checkout for the Minecraft version you are releasing:

| Branch | Minecraft tag | Loader tag | JDK |
| --- | --- | --- | --- |
| `master` | 1.20.1 | Forge | 17 |
| `1.21.1` | 1.21.1 | NeoForge | 21 |

Each invocation builds and publishes only that checkout's version. Run the same
task separately in the other checkout to release its version. Do not override
`minecraft_version` to build another version. Publishing is limited to these two
versions for now. Files are tagged for both Client and Server.

## Separate tasks

| Upload task | Artifact task | Configuration prefix |
| --- | --- | --- |
| `publishCurseForgeMod` | `jar` | `curseforgeMod` |
| `publishCurseForgePacksManager` | `packsManagerJar` | `curseforgePacksManager` |
| `publishCurseForgeOfficialPacks` | `officialPacksJar` | `curseforgeOfficialPacks` |
| `publishCurseForgeManusPacks` | `manusPacksJar` | `curseforgeManusPacks` |
| `publishCurseForgeWarfare44Pack` | `warfare44Pack` | `curseforgeWarfare44Pack` |
| `publishCurseForgeNpcs` | `npcsJar` | `curseforgeNpcs` |

Every additional module registered by `src/*/fmu-module.gradle` gets its own task
automatically. The suffix is its JAR task name with a trailing `Jar` removed and
the first letter capitalized: `myPackJar` gets `publishCurseForgeMyPack` and the
prefix `curseforgeMyPack`. If a JAR task has no `Jar` suffix, its name is otherwise
unchanged. List the tasks, including local/private modules, with:

```powershell
.\gradlew.bat tasks --group publishing
```

## Configure a destination

For a module, fill in the `curseforge` map inside its `registerFmuModule(...)`
call in `src/<module>/fmu-module.gradle`:

```groovy
curseforge: [
    projectUrl: 'https://www.curseforge.com/minecraft/mc-mods/your-pack-slug',
    projectId: '123456'
],
```

All existing module descriptors, including Wolff's packs, have a blank map ready
to fill in. Optional keys are `changelog` (path relative to the checkout root),
`releaseType`, and `requiredDependencies` (a list of project slugs or a
comma-separated string). For example:

```groovy
curseforge: [
    projectUrl: 'https://www.curseforge.com/minecraft/mc-mods/your-pack-slug',
    projectId: '123456',
    changelog: 'release-notes/my-pack.md',
    releaseType: 'beta',
    requiredDependencies: ['your-custom-npcs-port-slug']
],
```

The main mod and Packs Manager keep their destination properties in
`gradle.properties` because they have no `fmu-module.gradle` descriptor.
Per-artifact Gradle properties, including `-P` options, override module settings.
An explicitly blank URL property disables a module even when its descriptor
contains a URL. URLs enable publishing; the author upload API
also requires the numeric Project ID displayed on the CurseForge project page.

```properties
curseforgeOfficialPacksProjectUrl=https://www.curseforge.com/minecraft/mc-mods/your-pack-slug
curseforgeOfficialPacksProjectId=123456
```

The main mod and each module need their own CurseForge project. Both Minecraft
versions may use the same project's URL and ID; each upload gets its own version
and loader tags. A blank URL skips that task, even if an ID is supplied. It also
prevents that task from building its artifact.

Generate an **author API token** at [CurseForge API Tokens](https://authors.curseforge.com/account/api-tokens).
Supply it through the `CURSEFORGE_API_TOKEN` environment variable. The separate
CurseForge discovery API key is not used. The token is read at task execution,
kept out of configuration-cache inputs, and never printed by the release tooling.

Prepare a Markdown changelog. The default is the local, gitignored `CHANGELOG.md`
in the checkout root. These settings apply to all tasks unless overridden:

```properties
curseforgeChangelog=CHANGELOG.md
curseforgeReleaseType=release
```

Each artifact can override `Changelog`, `ReleaseType`, and `RequiredDependencies`:

```properties
curseforgeOfficialPacksChangelog=release-notes/classic-packs.md
curseforgeOfficialPacksReleaseType=beta
curseforgeNpcsRequiredDependencies=your-custom-npcs-port-slug
```

Release types are `release`, `beta`, and `alpha`. Required dependencies are
comma-separated CurseForge project slugs. Modules automatically declare the main
mod as required, using its configured URL's slug and optional Project ID. If the
main mod URL is blank, its dependency slug defaults to `flans-mod-ultimate-2`;
override `curseforgeModSlug` to point to another main-mod project. This lets you
release a module independently without enabling the main mod's upload task.
Configure additional dependencies for modules such as NPC Vehicles & Soldiers
(Custom NPCs) and packs that require Classic Packs.

Artifact filenames, display names, and versions come from their existing JAR
tasks. Module versions remain independent of `mod_version`. Forge uploads wait
for their `reobf` tasks; NeoForge uploads use the compiled JAR directly. The Packs
Manager retains its existing packaging and does not require reobfuscation.

## Preview and upload one module

Validate settings without building or making network requests (no token needed
when previewing):

```powershell
.\gradlew.bat validateCurseForgeOfficialPacks -PcurseforgeDryRun=true
```

Build the artifact and print its destination and metadata without uploading:

```powershell
.\gradlew.bat publishCurseForgeOfficialPacks -PcurseforgeDryRun=true
```

After setting the token in the environment, publish just that artifact:

```powershell
.\gradlew.bat publishCurseForgeOfficialPacks
```

Use another module's upload task to publish it separately. With no URL configured,
the task reports `SKIPPED`. The optional `validateCurseForge` and
`publishCurseForge` tasks operate on all URL-enabled destinations in that checkout.
When several uploads are requested together, all selected settings and artifacts
are checked/built before the first upload, and uploads run in a deterministic
order. One artifact's task does not upload any other artifact.

A successful response prints the new CurseForge file ID. CurseForge approval
still applies. Uploads are never automatically retried: a network failure may
happen after the server accepted the file. Check the project's file list before
retrying. If a later upload in a group fails, earlier successful uploads remain;
use the individual task to retry only the failed artifact.

## Verify the tooling

The local integration checks exercise multipart requests, artifact selection,
disabled modules, dependencies, credentials, previews, version restrictions,
reobfuscation, configuration-cache reuse, and upload failures against a localhost
server with synthetic JARs. They never send requests to CurseForge:

```powershell
python scripts/testCurseForgePublishing.py --java-home "C:/path/to/matching-jdk"
```

The task implementation follows the [CurseForge author Upload API](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api).
