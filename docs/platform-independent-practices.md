# Platform-independent development practices

Keep gameplay rules and their tests shared between maintained branches. Express
Minecraft and loader API differences through the existing `platform/` helpers.
Search for a helper by the operation you need before calling a version-specific
API or adding another wrapper. The [version differences reference](minecraft-version-code-differences.md)
records the concrete adaptations; this guide explains how to use those boundaries.

## ItemStack custom data

The main mod's `com.flansmodultimate.platform.item.ItemStackData` handles custom
stack data on both Forge 1.20.1 and NeoForge 1.21.1. Shared callers, including
tests, should use it instead of `getTag()`, `getOrCreateTag()` or direct
`DataComponents.CUSTOM_DATA` access. The platform implementation owns the change
from mutable stack NBT to data components.

| Operation | Helper and ownership |
| --- | --- |
| Check for custom data | `has(stack)` |
| Read or prepare an edit | `copy(stack)` returns detached data; edits take effect only after `set(stack, tag)` |
| Change a field | `update(stack, tag -> ...)` reads, edits and writes back the data |
| Replace custom data | `set(stack, tag)`; an empty tag clears custom data |
| Read frequently without copying | `view(stack)` is read-only; never mutate or retain it or its nested tags |
| Save or restore a whole stack | `save` / `parse`, with the real registry provider when available |
| Compare stack data without count | `isSameItemSameData(first, second)` |

For example, the same code can set and check a custom field on either branch:

```java
ItemStackData.update(ammo, tag -> tag.putInt("remaining", 7));
int remaining = ItemStackData.copy(ammo).getInt("remaining");
```

Changing a compound returned by `copy` alone does not change the stack. Likewise,
mutating `view` can bypass component replacement and break the helper's contract.
Use `update` for small edits, or `copy` followed by `set` for a larger edit.

Custom data is only part of an item stack. To transfer a real magazine, copy the
stack with `copy()` or `copyWithCount(...)`, preserving its other components too.
Store it successfully before shrinking the donor stack. Test the retained data,
item counts and failed-transfer behavior, as `NpcAmmoSharingTest` does.

For persistence, `save(stack, registries, target)` preserves caller metadata such
as an inventory slot index. Do not assume vanilla `ItemStack.save` mutates its
input on every version. Use `saveBuiltIn` / `parseBuiltIn` only for stacks whose
references are confined to static built-in registries; live registry-backed
components need the appropriate registry provider.

## Keep each module's boundary intact

The main mod uses `com.flansmodultimate.platform`. The NPC module uses its own
`com.wolffsmod.npcs.platform`, including `NpcPlatform`, `CustomNpcsPlatform`,
`NpcClientPlatform`, `NpcPacketIO` and `NpcSavedData`. For example, NPC production
code compares stack data through `NpcPlatform.isSameItemSameData`.

NPC production code may consume the main mod only through
`com.flansmodultimate.api`; importing the main mod's internal platform helpers
would couple the separately packaged module to its implementation. Main-project
integration tests can use their existing internal test infrastructure, including
`ItemStackData`, without adding that dependency to NPC production code.

Put reusable Flan behavior in the public API when another mod could reasonably
use it. Keep vanilla convenience wrappers and Custom NPCs integration in their
own platform packages. Keep client-only classes behind client boundaries and
inventory mutation, ammunition consumption and gameplay decisions on the server.

## Test behavior through the same boundaries

Use `EntityTestBootstrap.initialize()` for tests that need Minecraft registries,
and `TestPacketBuffers.buffer()` / `wrap(...)` for packet tests. These test helpers
own loader bootstrap and buffer differences. Do not duplicate their branch-specific
setup in a new test.

When a port requires a helper call, check whether the source branch already
supports that call. Prefer the same caller on both branches, leaving the differing
implementation in `platform/`. Check the full call chain: a successful compile
does not verify string-based mixin targets, registration, server safety or packaged
resources. Use the relevant contract tests and the repository's validation profile;
live Minecraft checks remain on demand.
