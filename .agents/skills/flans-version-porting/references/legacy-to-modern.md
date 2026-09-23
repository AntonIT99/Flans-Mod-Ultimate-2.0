# Legacy Flan's Mod to Modern Ultimate

Read this reference when 1.7.10, 1.12.2, an old addon, or a legacy fork supplies
the behavior to migrate. The objective is semantic recovery followed by a native
modern implementation, not compilation by substitution.

## Source authority and local repositories

Use evidence in this order unless the user specifies otherwise:

1. `../Flans-Mod-Ultimate-1.7.10` is authoritative for original gameplay,
   content-pack syntax, timing, and compatibility semantics.
2. `../FlansMod` (1.12.2) is secondary. It is especially useful for changes in
   Forge lifecycle, registries, capabilities, and intermediate rendering APIs.
3. The current destination branch establishes its architecture and invariants.
4. `../Flan's Mod Aryan Indian Edition Krishna Mk6C` is optional inspiration,
   never authority unless explicitly requested.
5. `../Flans-Mod-Ultimate-2.0.wiki` documents intended user-visible behavior but
   does not override observed 1.7.10 behavior without evidence of a deliberate
   later change.

When 1.7.10 and 1.12.2 differ, determine whether 1.12.2 fixes a bug, adapts an API,
or deliberately changes gameplay. Default to 1.7.10 semantics. State uncertainty
instead of silently choosing whichever implementation is easiest to port.

Useful starting points:

| Concern | 1.7.10 source | Modern destination |
| --- | --- | --- |
| Bootstrap/content loading | `common/FlansMod.java`, `common/CommonProxy.java` | `FlansMod.java`, `ContentManager.java` |
| Text definitions | `common/types/InfoType.java`, subtype `read` methods | `common/types/InfoType.java`, subtype parsers, `TypeFile` |
| Guns | `common/guns/GunType.java`, `ItemGun.java` | `common/types/GunType.java`, gun items/handlers |
| Driveables | `common/driveables/DriveableType.java`, `EntityDriveable.java` | `common/types/DriveableType.java`, driveable entities/handlers |
| Packets | `common/network/PacketHandler.java`, packet classes | `network/PacketHandler.java`, client/server packet interfaces |
| Client registration | `client/ClientProxy.java`, `FlansModClient.java` | client event handlers and renderer registration |
| Models/rendering | `client/model`, legacy render classes | modern model adapters, entity/item renderers, render state |

Package roots differ: legacy sources use `com.flansmod`; modern Ultimate uses
`com.flansmodultimate`. Search by simple class name, directive string, NBT key,
packet field, sound/resource path, and call-site behavior—not package alone.

## Recover the behavioral contract first

Trace one complete vertical slice before writing code:

1. Find construction/registration and every event or tick entry point.
2. Follow state from input through validation, mutation, persistence, network
   synchronization, and rendering/audio feedback.
3. Identify which side owns each mutation and which values are derived views.
4. Record units and clocks: ticks versus seconds, degrees versus radians,
   blocks versus model units, per-tick probability versus rate, and inclusive
   versus exclusive counters.
5. Record ordering and sentinel behavior: `null`, zero, negative values, empty
   strings, missing directives, default enum values, and duplicate shortnames.
6. Find content examples that exercise the feature; parser code alone often
   hides casing, aliases, optional arguments, and fallback expectations.

For large features, keep a small contract table while investigating:

| Aspect | Legacy evidence | Required modern behavior | Intentional change |
| --- | --- | --- | --- |
| Authority | side and caller | server/client ownership | reason |
| Timing | tick/cooldown order | equivalent cadence | reason |
| Data | directive/NBT/default | compatible parse/save | migration |
| Network | trigger/payload | validated request/sync | protocol change |
| Visuals | transform/texture | equivalent output | accepted difference |

## Translate concepts, not symbols

| Legacy concept | Modern destination concern |
| --- | --- |
| `@Mod`, proxies, `preInit/init/postInit` | Mod constructor, mod/game event buses, common/client setup, enqueue-work constraints |
| `GameRegistry` / `EntityRegistry` calls | Deferred or event-driven registry objects with stable resource IDs |
| `FMLCommonHandler` and old Forge event buses | Correct modern mod bus versus gameplay bus and correct logical side |
| Numeric item/block/entity IDs | Namespaced registry identity; preserve existing modern IDs and aliases |
| `World`, `EntityPlayer`, `TileEntity` | `Level`/`ServerLevel`, `Player`/`ServerPlayer`, `BlockEntity`; recheck side and lifecycle assumptions |
| `NBTTagCompound` and entity NBT hooks | `CompoundTag` and current save/load hooks; preserve stable keys and defaults |
| `IExtendedEntityProperties` or ad-hoc maps | Destination-native attachment/capability/component or `SavedData`, chosen by ownership and lifetime |
| Data watcher fields | Synced entity data or explicit packets; allocate keys in the correct declaring class |
| `SimpleNetworkWrapper`, `IMessage` handlers | Destination packet/payload registration, codecs, direction, main-thread handling, and sender validation |
| GUI IDs and `openGui` | Menu/container registration, server menu opening, and client screen registration |
| `IIcon`, icon registration, dynamic domains | Resource locations, models, atlases, reload listeners, and pack-resource integration |
| `GL11`, matrix stack globals, tessellation | `PoseStack`, render types/buffers, vertex consumers, packed light/overlay, and explicit render state |
| Legacy entity renderers reading live entities | On 26.x, extract immutable render state first; on 26.2 express feature layers through feature rendering |
| `onUpdate`, world/server tick handlers | Destination entity tick or precisely phased level/server/player events; avoid double execution |
| Integer dimensions and custom teleports | Resource-keyed dimensions and destination teleport APIs; keep server ownership |
| Ore dictionary recipes | Tags and current recipe/data-generation formats |
| Language `.lang`, old sound JSON/assets | Current localization and resource-pack layout while retaining user-visible keys where compatible |

This table selects the subsystem to inspect; it is not a one-to-one replacement
catalog. Confirm the exact API in the destination branch before coding.

## Flan-specific compatibility traps

### Text definitions and content packs

- Preserve directive meaning, aliases, defaults, tokenization, case handling,
  duplicate behavior, and the stage at which references resolve.
- Do not rename shortnames, files, model/texture keys, NBT keys, or resource paths
  merely to satisfy modern naming taste. Add normalization only at a deliberate
  boundary and retain collision detection.
- A parsed field is not necessarily an implemented feature. Trace its consumers.
  Conversely, a directive may be handled in a base type or post-load pass.
- Preserve deterministic pack and definition order. Never derive registry or
  packet IDs from nondeterministic directory/hash-map iteration.
- Keep pack classes/resources isolated from the main mod artifact according to
  the destination build. Do not edit generated/runtime packs as source.

### Gameplay and entities

- Legacy code frequently mixes client prediction, visual effects, and authority
  in one tick method. Split them explicitly: server validates/mutates; clients
  render or predict only where the modern implementation supports reconciliation.
- Recheck constructor-time world access, entity removal semantics, passenger
  APIs, damage sources, collision callbacks, chunk loading, and dimension change.
  Lifecycle timing changed substantially and literal ports often run too early or
  twice.
- Preserve formulas and operation order before refactoring. Seemingly harmless
  changes to integer division, float precision, clamp order, random-call count,
  or yaw/pitch conventions can alter weapon cadence and vehicle physics.

### Networking and persistence

- Treat every C2S payload as hostile input: recover the sender from context,
  validate distance/ownership/item/menu/state, and mutate on the server thread.
- Keep packet registration deterministic. When payload shape or order changes,
  update the destination protocol deliberately and test mismatched peers.
- Separate persistent state from transient synchronized/render state. Preserve
  stable save keys and missing-key defaults; add migration only when existing
  worlds genuinely need it.
- Do not copy entity IDs, registry ordinals, raw object references, or client-only
  instances into persistent/network state. Use destination-native IDs, UUIDs, or
  resource keys with explicit absent-value handling.

### Rendering and resources

- Reconstruct the transform hierarchy in the same order. Legacy OpenGL calls are
  order-dependent; account for handedness, axis conventions, the legacy
  `1/16` model scale, interpolation, normals, light, overlay, and buffer flushes.
- Do not call OpenGL globals as a shortcut. Use the destination render pipeline
  and established model adapters. Ensure no render class is loaded on a dedicated
  server.
- Register reload-aware caches for assets derived from resource packs. Do not
  assume a texture or model found during startup remains valid after reload.

## Efficient investigation tactics

- Start with `rg` across the relevant roots for a class/simple name plus one
  behavioral anchor (directive, NBT key, packet name, translation key, texture,
  or sound). Then inspect callers and registrars.
- Use `git log -S '<symbol-or-string>' -- <path>` and `git blame` when code alone
  cannot explain a constant, workaround, or apparent bug.
- Compare 1.7.10 -> 1.12.2 -> destination for the same vertical slice. The middle
  version can reveal intent, but should not become an obligatory extra port.
- Search the destination for the subsystem's current pattern before consulting
  generic loader documentation. Existing branch code captures local conventions
  and packaging constraints.
- Avoid broad import-fixing passes. Implement one compilable slice, compile, and
  let errors expose the next API boundary.

## Validation for a legacy migration

In addition to the main skill's build checks, validate at least one real legacy
content example and the relevant missing/old-save defaults. For gameplay, compare
cadence, damage/physics formulas, authority, and audiovisual triggers. For
rendering, test first- and third-person or entity/world contexts as applicable and
resource reload. For networking, test both directions and a dedicated server.

Do not claim parity from compilation alone. Report known semantic differences,
untested legacy edge cases, unavailable runtime checks, and any content or save
migration users must perform.
