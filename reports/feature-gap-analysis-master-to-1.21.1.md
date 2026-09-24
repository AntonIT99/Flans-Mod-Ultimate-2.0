# Main-module parity: `master` → `1.21.1`

## Scope and method

Compared `master` at `58ed6da207fca1b0507fb1efa0184562c686f39e` (Forge 1.20.1) with `1.21.1` at `8e939b6fc9b9ff512285c20a2225ee3e62e79a81` (NeoForge 1.21.1). This is an analysis of the **main module** (`src/main/java`, its resources, registration, packet and menu paths); separately packaged content modules are outside scope. `master` is the behavioral reference. Loader API changes, resource-path migrations, and target-only improvements are not gaps.

The audit inventoried all 332 changed `src/main` paths, then traced event and renderer registration, item and entity behavior, packet serialization, configuration, resources, and the existing port report. It cross-checked the local mapped Minecraft/NeoForge 1.21.1 source jar where API semantics mattered. The 62 packet classes, 17 mixin entries, and config key sets (101 common, 59 client, 26 apocalypse) are present on both branches. All eight built-in category JSON files are semantically equal between branches (2,134 category entries in total), as are the common localization JSON objects; their textual differences are formatting. The master-only recipe, loot, block-tag, and biome-modifier resource paths have corresponding files under the 1.21.1 path conventions. This establishes inventory parity for those surfaces, not runtime parity.

**Result: 4 MISSING, 8 PARTIAL, 1 UNCERTAIN.** Findings below concern specific observable behavior; they are not a percentage of overall port completeness.

## Networking and teams

### Reward-box type catalogue does not reach the client — MISSING

Reference: `src/main/java/com/flansmodultimate/network/client/PacketLoadoutState.java:108,140,164`; `client/gui/TeamsLoadoutHubScreen.java`, `client/gui/TeamsRewardBoxScreen.java`. The server builds the available box types, sends them with their preview and unopened count, and the two screens render the list.

Target checked: `src/main/java/com/flansmodultimate/network/client/PacketLoadoutState.java:109,124-172`; both screen consumers. The server still populates `boxTypes`, and both screens still read `getBoxTypes()`, but `encodeInto` and `decodeInto` omit the collection entirely. A decoded packet keeps its default empty list.

Missing: the catalogue/preview/count portion of the reward-box and loadout-hub UI. The owned `boxes` collection is sent; it does not replace the missing type catalogue.

## Items and enchantments

### Gun, grenade, and armor attribute modifiers are skipped — PARTIAL

Reference: `common/item/GunItem.java` and `GrenadeItem.java` implement slot-sensitive modifiers; `CustomArmorItem.java:140` adds armor movement speed and knockback modifiers for its actual armor slot.

Target checked: `common/item/GunItem.java:524-539`, `GrenadeItem.java:94-105`, `CustomArmorItem.java:145-165`, and mapped NeoForge `IItemStackExtension.getEquipmentSlot()` / `IItemExtension.getEquipmentSlot(ItemStack)` in `build/moddev/artifacts/neoforge-21.1.248-sources.jar`. The target compares `stack.getEquipmentSlot()` to `MAINHAND` or an armor slot before adding modifiers. This hook returns the item's explicit override, which defaults to `null`; the API documentation says to use `LivingEntity.getEquipmentSlotForItem` to determine the slot. These items provide no explicit stack-slot override. The code therefore returns the base modifiers before reaching its custom additions.

Missing: configured gun movement speed, knockback resistance and melee damage; grenade melee damage; custom-armor movement speed and knockback modifiers. The base material's armor/armor-toughness values can still apply.

### First preferred-ammo read returns an empty choice — PARTIAL

Reference: `common/item/GunItem.java:751-765` reads the live stack tag, writes the first allowed ammo type if the preference is absent, and returns that newly written value. `common/guns/reload/GunReloader.java` and `client/gui/GunAmmoSelectScreen.java` use that return value to prioritize ammunition and mark the selected choice.

Target checked: `common/item/GunItem.java:755-766`, `platform/item/ItemStackData.java:31-49`, `common/guns/reload/GunReloader.java:74-75`, `common/entity/Mecha.java:566-575`, and `client/gui/GunAmmoSelectScreen.java:60-75`. The target reads a detached `ItemStackData.copy(gun)`, calls `setPreferredAmmo` to write the default to the stack, then returns the old detached tag's empty string. A second call reads the new value.

Missing: on the first read of a gun without an explicit preference, reload planning and mecha hand-gun reload ignore the configured first allowed ammunition type; the ammo selection screen initially shows no selected type. Subsequent reads work because the default was persisted.

### Ignore-armor shots run before shield and damage cooldown checks — PARTIAL

Reference: `event/handler/CommonEventHandler.java:365-414` applies armor and enchantment damage logic in Forge `LivingHurtEvent`. The mapped Forge 1.20.1 `LivingEntity.hurt` source calls this event through `actuallyHurt` after shield blocking and invulnerability cooldown checks. `CustomArmorItem.tryApplyIgnoreArmorShot` directly reduces health, then cancels the event.

Target checked: `event/handler/CommonEventHandler.java:365-415`, `common/item/CustomArmorItem.java:462-507`, `common/enchantments/EnchantmentModule.java:106-140`, and the mapped NeoForge 1.21.1 `LivingEntity.hurt` / `LivingIncomingDamageEvent` sources. The target runs all this logic in `LivingIncomingDamageEvent`, which fires before shield blocking and cooldown checks. The ignore-armor branch can subtract health and cancel at that point, so those checks never run. The off-hand damage enchantment can also consume glove durability for an attack later blocked or rejected by cooldown.

Missing: master-equivalent shield/cooldown protection for ignore-armor shots and master-equivalent off-hand enchantment durability timing. This affects shots whose `ignoreArmorProbability` roll succeeds; ordinary damage still proceeds through the vanilla checks.

### Custom enchantments are defined but not normally obtainable or applicable to intended Flan items — PARTIAL

Reference: `FlansMod.java:184,380-382` registers six enchantments. `common/enchantments/OffHandEnchantment.java:24-40` accepts Flan gloves (and shields for non-glove-only variants); `EnchantmentJuggernaut.java` is armor treasure-only. Runtime effects are handled by `EnchantmentModule`.

Target checked: six `src/main/resources/data/flansmodultimate/enchantment/*.json`, `common/enchantments/EnchantmentModule.java`, and the bundled Minecraft 1.21.1 tags in `build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar`. The target has the definitions and effect handlers, but contributes no entries to `minecraft:non_treasure`, `minecraft:treasure`, or other acquisition tags. Its `supported_items` values use vanilla `#minecraft:enchantable/durability` and `#minecraft:enchantable/armor`, whose bundled lists contain no Flan gloves or custom armor. `nimble` also lists vanilla shields even though its runtime effect requires a glove.

Missing: ordinary enchanting/loot acquisition of the six enchantments and normal application of glove enchantments to Flan gloves or Juggernaut to Flan armor. Command-applied enchantments can still reach the runtime handlers.

## Menus, input, and HUD

### Mecha inventory has no registered client screen — MISSING

Reference: `event/handler/ModClientEventHandler.java:123` registers `MechaInventoryScreen`; `common/entity/Mecha.java` opens that menu.

Target checked: `event/handler/ModClientEventHandler.java:131-140`, `common/entity/Mecha.java:1002-1014`, and `common/inventory/MechaInventoryMenu.java`. The menu and screen classes exist and the server opens the menu, but the new `RegisterMenuScreensEvent` handler registers the other six screens and omits `mechaInventoryMenu`.

Missing: the mecha equipment/cargo screen when its menu opens on a client.

### Vehicle key conflicts are no longer claimed before vanilla handles them — PARTIAL

Reference: `event/handler/ClientEventHandler.java:149-160` calls `KeyInputHandler.claimConflictingVanillaKeys()` on the START client tick; `client/input/KeyInputHandler.java:287` drains inventory, drop, and other claimed vanilla clicks for a driver.

Target checked: `event/handler/ClientEventHandler.java:169-177` and all `src/main/java` callers of `claimConflictingVanillaKeys`. The target retains the function and vehicle key routing but only subscribes to `ClientTickEvent.Post`; no caller remains.

Missing: suppression of vanilla actions on shared driveable keys. A pilot/driver can trigger the vanilla inventory, item drop, or other mapped action alongside a vehicle control.

### Flashbang and wounded-screen layers are never registered — MISSING

Reference: `event/handler/ModClientEventHandler.java:196-197` registers both overlays; `client/render/ClientHudOverlays.java` implements the full-screen flash effects.

Target checked: `event/handler/ModClientEventHandler.java:215-221` and `client/render/ClientHudOverlays.java:568,580`. Both layer implementations and their timers remain, but neither layer is passed to `RegisterGuiLayersEvent` or invoked elsewhere.

Missing: the visual flashbang and wounded-screen effects. Their underlying damage/flash state is still updated.

### Operator-stick pending-connection line is never rendered — MISSING

Reference: `event/handler/ClientEventHandler.java:204` invokes `OpStickConnectionRenderer.render` in the world render stage to show the selected link endpoint.

Target checked: `event/handler/ClientEventHandler.java:207-226`, `client/render/OpStickConnectionRenderer.java`, and all callers. The renderer is ported, but the stage handler no longer calls it and no replacement caller exists.

Missing: the line from the held operator stick to the pending connection endpoint.

### Crosshair suppression loses two settings — PARTIAL

Reference: `event/handler/ClientEventHandler.java:232-242` hides the vanilla crosshair when vehicle seat optics request it or when the client `hideCrosshairForGuns` option is enabled. Scope, gun-type, and common-config checks also apply.

Target checked: `event/handler/ClientEventHandler.java:231-250`, `config/ModClientConfig.java:68,231,486`, and `client/render/VehicleOpticsHud.java`. The target retains the client option and seat optics but drops both tests from the crosshair event. Scope, gun-type, and common-config suppression remain.

Missing: the seat `showCrosshair=false` behavior and the client's `hideCrosshairForGuns` preference.

### `showFlansHud` no longer controls the Flan HUD — PARTIAL

Reference: `client/render/ClientHudOverlays.java:161-164` skips the HUD layer when `showFlansHud` is false or the vanilla GUI is hidden.

Target checked: `client/render/ClientHudOverlays.java:163-173`, `config/ModClientConfig.java:67,224,485`, and all callers. The setting is still defined and read into the snapshot but has no consumer. The target HUD layer unconditionally calls the AA/deployed gun, ammo, team, kill-message, and vehicle-debug render methods; some individual methods have their own visibility checks.

Missing: a working global Flan HUD visibility toggle. Setting `showFlansHud=false` no longer suppresses the whole HUD.

## Entities and rendering

### Client mecha legs stop following synchronized leg yaw — PARTIAL

Reference: `common/entity/Mecha.java:257-264` calls `updateLegFacing` during client ticks. That method reads `DATA_LEG_YAW`, and `client/render/entity/DriveableRenderer.java` interpolates `getLegYaw()`.

Target checked: `common/entity/Mecha.java:257-289` and `client/render/entity/DriveableRenderer.java:199-205`. The server still calculates and syncs leg yaw, but the target client tick calls only `updateLegAnimation`; the synchronized yaw is read only inside the now-unreachable client branch of `updateLegFacing`.

Missing: correct client leg orientation for moving/turning mechas. The renderer uses stale local yaw rather than the server's synchronized value.

### Placed driveable source stack is not stripped of old state — UNCERTAIN

Reference: `common/entity/Driveable.java:462` passes the mutable source stack tag to `DriveableData.removeSerializedState`, removing copied inventory/damage payloads while retaining unrelated metadata.

Target checked: `common/entity/Driveable.java:463,951-957,4745-4753`, `common/driveables/DriveableData.java:417-452`, and `platform/item/ItemStackData.java:27-49`. The target passes `ItemStackData.copy(sourceStack)`, a detached tag, then discards it. The stripping call therefore cannot alter `sourceStack`. Later save/drop paths reserialize from that stack, although `DriveableData.copyToStack` also writes current state and may mask stale fields for this mod's own loader.

Missing: the source-stack cleanup itself. Whether stale root-level legacy fields cause an observable save or integration error requires a placed-and-dropped legacy driveable round trip; the static audit does not establish one.

## Finding summary

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Teams/network | Reward-box type catalogue | MISSING | HIGH |
| Items | Gun, grenade, armor modifiers | PARTIAL | HIGH |
| Items/reload UI | First preferred-ammo read | PARTIAL | HIGH |
| Combat | Ignore-armor damage event timing | PARTIAL | HIGH |
| Enchantments | Acquisition and intended-item support | PARTIAL | HIGH |
| Menus | Mecha inventory screen | MISSING | HIGH |
| Input | Vehicle key conflict claiming | PARTIAL | HIGH |
| HUD | Flashbang and wounded layers | MISSING | HIGH |
| Rendering | Operator-stick connection line | MISSING | HIGH |
| HUD | Crosshair suppression options | PARTIAL | HIGH |
| HUD | `showFlansHud` toggle | PARTIAL | HIGH |
| Entities/rendering | Mecha leg yaw | PARTIAL | HIGH |
| Persistence | Driveable source-stack cleanup | UNCERTAIN | MEDIUM |

## Validation and limits

The 1.21.1 checkout passed `./gradlew.bat test` and `./gradlew.bat build` during this audit. No gameplay files were changed. These builds do not exercise menu opening, client HUD composition, key conflicts, enchantment acquisition, or the placed-driveable round trip. The prior port report (`reports/port-master-to-1.21.1-2026-09-23.md`) records server/client startup checks, but no interactive verification of the findings above.

## Areas requiring deeper audit

- Round-trip a legacy driveable item through placement, save/reload, destruction, and pickup to decide whether the detached source-tag cleanup has a user-visible consequence.
- Exercise representative thermal, optics, particle, and model rendering in-world; the 1.21.1 renderer API changes compile but static source comparison cannot prove visual parity.
