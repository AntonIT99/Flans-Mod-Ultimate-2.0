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
- Prefer existing API types over main-mod internals. Reach into internals only when the need is
  specific to this module or cannot be abstracted reasonably, and keep such uses few and easy to
  replace later.
- Report the assessment in the change summary: what was added to the API, and what was left
  internal and why.
