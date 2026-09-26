# NPC Vehicles & Soldiers module

- Separate mod `flansmodultimate_npcs` ("Flan's Ultimate 2: NPC Vehicles & Soldiers"), built by
  `npcsJar`. Flan's Mod Ultimate and Custom NPCs are both mandatory dependencies.
- Custom NPCs classes may only be referenced from this module. The main mod treats Custom NPCs
  as optional and must not depend on it.
- Keep NPC behaviour server-authoritative, and keep client-only code out of common entrypoints.
