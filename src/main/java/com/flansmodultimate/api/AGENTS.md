# Public API

`com.flansmodultimate.api` is the supported surface for other mods, submods and addons,
starting with the NPC Vehicles & Soldiers module. Treat everything here as a contract that
outlives the implementation behind it.

## What belongs here

- Behaviour and data another mod can plausibly reuse, and that can be abstracted without much
  effort: reading content-pack definitions, querying or acting on Flan's entities and items,
  hooks and events, extension points.
- Nothing that only exists to serve one internal call site, and no convenience wrappers around
  vanilla or loader APIs.

## Design rules

- Expose interfaces, records, enums and small static entry points. Implement them in the main
  mod, outside this package.
- API signatures may only use API types, Java standard types and Minecraft types. Never expose
  main-mod internals such as `common.*`, `client.*`, `ContentManager` or legacy `com.flansmod`
  model classes. When an internal type is needed, add a slim API interface for what callers
  need and have the internal type implement it, as `ISeat` does for `Seat` and `IContentPack`
  for `IContentProvider`. Internal code keeps using the internal type through covariant returns.
- Static entry points such as `FlansModApi` may delegate to internals; their signatures still
  follow the rule above.
- The API is MIT licensed (`LICENSE-API`), unlike the rest of the mod. Start every file in the
  package, `package-info.java` included, with the same copyright and
  `SPDX-License-Identifier: MIT` header as the existing ones.
- Write every API type fresh, in your own words. Never port or adapt API code from the
  original Flan's Mod or other projects, so that the package can carry its own licence. The
  legacy `com.flansmod.api` interfaces were replaced for that reason: `IContentType` and
  `IBullet` are the new designs, and `IControllable` became internal.
- Keep loader types out of signatures where a Minecraft or API type can do, so the API ports
  cleanly between the Forge and NeoForge branches. Loader-specific registration stays in the
  implementation.
- Keep client-only API in a `client` subpackage, so common code can use the rest safely on a
  dedicated server.
- Server-authoritative: API calls that change gameplay state act on the server, and document
  the side each method may be called on.
- Return `Optional` or annotate `@Nullable` rather than returning null silently. Collections are
  unmodifiable.
- Javadoc every public type and method: what it represents, units and coordinate conventions
  (blocks or model pixels, legacy or modern basis), threading and side.
- Evolve compatibly: add rather than change. Deprecate before removing, and keep a deprecated
  member at least until the next major version. Prefer default methods when adding to an
  interface that other mods may implement.

## Stability markers

- Annotate every API type with `@ApiStatus.Experimental` until it is declared stable; stable
  types then carry `@ApiStatus.AvailableSince("<api version>")` instead.
- Add `@ApiStatus.NonExtendable` to types only Flan's Mod Ultimate implements. Leave it off
  genuine extension points meant for other mods to implement.
- `ApiBoundaryTest` enforces the signature rule and the stability markers. Never weaken it to
  make a change pass; fix the API instead.

## Workflow

- The API ships on its own as `com.flansmodultimate:flansmodultimate-api:<minecraft>-<api_version>`,
  built by `apiJar` (classes named like the mod jar's: reobfuscated on Forge, Mojang names on
  NeoForge), `apiSourcesJar` and `apiJavadocJar`, and published by
  the `api` Maven publication. Run `apiJavadoc` after API changes: it fails on broken javadoc
  references and malformed HTML.
- Bump `api_version` in `gradle.properties` with every released API change: the minor version for
  additions, the major version for breaking changes. It is independent of `mod_version`.

- When adding to the API, update the wiki too (an API page, created if missing).
- Port API changes to the maintained version branches with the rest of the feature.

## Releasing the API

The API is published to a static Maven repository, the `AntonIT99/flansmodultimate-maven`
GitHub repository served by GitHub Pages at `https://antonit99.github.io/flansmodultimate-maven`.
The `Publish API` workflow (`.github/workflows/publish-api.yml`) runs only when started by hand.
Never start it, push to the Maven repository or tag a release without the user asking.

One-time setup, done by the user:

1. Create the public repository `AntonIT99/flansmodultimate-maven` with a `main` branch holding a
   `README.md` and an empty `.nojekyll`, and enable GitHub Pages for `main`, root folder.
2. Create a fine-grained personal access token with read and write access to the contents of that
   repository only, and store it as the `API_MAVEN_TOKEN` Actions secret of this repository.

Each release, per Minecraft branch:

1. Bump `api_version` if the API changed since its last release. Published versions are
   immutable; publishing an existing one fails.
2. Make sure `gradlew test apiJavadoc` passes.
3. Run `Publish API` on the branch, from the Actions tab.
4. Update the version in the wiki's API page examples.

Branches publish independently: a branch can only be published once it has the API tasks and this
workflow, ported from master.
