# NPC Vehicles & Soldiers module

- Separate mod `wolffsmodnpcs` ("Flan's Ultimate 2: NPC Vehicles & Soldiers"), built by
  `npcsJar`. Flan's Mod Ultimate and Custom NPCs are both mandatory dependencies.
- Custom NPCs classes may only be referenced from this module. The main mod treats Custom NPCs
  as optional and must not depend on it.
- Keep NPC behaviour server-authoritative, and keep client-only code out of common entrypoints.
- Mixins into Custom NPCs classes use `remap = false`. Custom NPCs is SRG-named in production and
  Mojang-named in the development dependency, so a vanilla method it overrides is targeted by both
  names, for example `method = {"tick", "m_8119_"}`.
- All Custom NPCs entity hooks belong in `EntityNPCInterfaceMixin` and all fallback ranged-AI hooks
  in `EntityAIRangedAttackMixin`, following the root one-mixin-per-target rule.

## Flan's Mod Ultimate API

This module is the first consumer of the public API in `com.flansmodultimate.api`, and is used
to grow that API into something other mods can rely on. Follow the API rules in
`src/main/java/com/flansmodultimate/api/AGENTS.md`.

- For every new feature, and whenever this module needs anything from the main mod, assess
  whether it belongs in the API: behaviour or data another mod could plausibly reuse, and that
  can be abstracted without much effort. When it does, add or extend the API in the same change
  and have this module call the API rather than the internals.
- Import nothing from the main mod but `com.flansmodultimate.api`, so the module depends only on
  the MIT-licensed API and survives internal refactoring. A need the API does not cover is met by
  extending the API, or, when it is specific to this module, by the module's own code.
- Loader and Minecraft version differences go through the module's own `platform` package
  (`NpcPlatform`, `CustomNpcsPlatform`, `client.NpcClientPlatform`, `network.NpcNetworkPlatform`,
  `world.NpcSavedData`), never the main mod's `com.flansmodultimate.platform` classes. Each
  maintained branch keeps its own versions of these files.
- Report the assessment in the change summary: what was added to the API, and what the module
  implements itself and why.
