# Feature gap analysis: Ultimate 1.7.10 → Ultimate 2.0

Initial audit: 2026-09-10. Last updated: 2026-09-18. Direction is strictly reference → target.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-1.7.10`, HEAD `9b669b12149c3c9fa3a69f1a4ac5d8a55367506d`.
- Target initially audited: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, HEAD `08494f8a4972c249feb4e7d8522e981547439c4e`.
- Target revalidated: HEAD `e0b68dc60fc7060324ee4293678183aca93650b7` plus the current working tree.
- Sources inspected were the current working trees, not pristine commit snapshots. Existing unrelated target command and image changes were left untouched; the reference working tree was clean.
- Completed findings are removed as they are implemented, so the report remains a backlog rather than a historical snapshot.
- Remaining: **2 MISSING, 2 PARTIAL, 0 UNCERTAIN**. These counts describe the findings below, not a percentage of port completeness.

## Scope and evidence conventions

This is a static, reference-driven semantic audit. Discovery covered reference types and configuration keys, weapons and ammunition, grenades, deployable/AA guns and targeting, tools and armor, driveables/aircraft/mechas, controls, rendering and effects, teams/game modes, commands, inventories/crafting, persistence, and networking. Candidate gaps were traced to active reference callers and checked against target entities, handlers, parsers, menus, configuration, and replacement mechanisms. Commented-out implementations and unused reference declarations were excluded.

Evidence paths use these roots:

- `R/` = reference `../src/main/java/com/flansmod`.
- `T/` = target `../src/main/java/com/flansmodultimate`.
- Resource paths explicitly identify their repository.

`MISSING` means the described capability has no equivalent in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated option or secondary behavior does not. Confidence concerns the narrow finding, not full subsystem equivalence. No gameplay code, packs, wiki pages, or generated files were changed. No builds or in-game tests were run; runtime parity is not established by this report.

## Multiplayer compatibility

### Content-definition mismatch detection and optional disconnect — MISSING

Reference: `R/common/ContentManager.java` (`Sync.addHash`), `R/common/sync/Sync.java`, `R/common/sync/SyncEventHandler.java#playerJoined`, `R/common/network/PacketHashSend.java`.
Loaded definition text contributes normalized hashes. On dedicated-server login, the server sends its aggregate hash and the client replies with its own. With `kickNonMatchingHashes` enabled, the server disconnects a mismatching client. This checks normalized definitions, not byte-for-byte equality of all pack assets.

Target checked: `T/network/PacketHandler.java`, `T/event/handler/CommonEventHandler.java#onPlayerLogin`, `T/config/ModCommonConfigSync.java`, `T/network/client/PacketSyncCommonConfig.java`, `T/ContentManager.java`.
Configuration synchronization and protocol registration do not compare client/server content-definition hashes. ContentManager's file hashing concerns generated texture handling, not a login handshake. No definition fingerprint exchange or configurable mismatch disconnect was found.

Missing: Optional enforcement that clients and the server loaded matching content definitions.

## Handheld weapons and inventory preferences

### Configurable blocking of block interactions while holding guns — PARTIAL

Reference: `R/common/guns/ItemGun.java#onPlayerInteract`, registered through the first gun item in `R/common/FlansMod.java`; `holdingGunsDisablesChests` and `holdingGunsDisablesAll`.
Right-click block interaction can be canceled for inventory blocks specifically, or for all blocks, while holding a gun.

Target checked: `T/event/handler/ClientEventHandler.java` (interaction-key handler), `T/common/item/GunItem.java#use`, `T/common/item/GunItem.java#doesSneakBypassUse`, and common/client configuration.
Input suppression depends on the bound button, selected gun function, and hit result. Sneak bypass uses menu-provider detection. These mechanisms do not supply the two configurable reference policies and do not consistently apply a gun-held inventory-block/all-block rule.

Missing: Separate configurable inventory-block and all-block interaction suppression while armed.

## Teams and administration

### Detailed explosion-kill audit and spawn-kill warning log — MISSING

Reference: `R/common/eventhandlers/PlayerDeathEventListener.java#PlayerDied`, `#logKillMessage`, instantiated in `R/common/FlansMod.java`.
For player deaths entering its Flan bullet/grenade explosion branch, the listener logs the weapon, positions, victim lifetime, and armor information. It also logs a possible spawn-kill warning when the victim's lifetime is below `noticeSpawnKillTime`. This finding is limited to the branch the reference actually implements.

Target checked: `T/event/handler/CommonEventHandler.java#onLivingDeath`, `#sendKillMessage`, `T/common/teams/PlayerStats.java`, `T/common/teams/TeamsManager.java`, and target log/configuration searches.
Kill feed packets and persistent statistics do not provide this detailed event log or its configurable lifetime-based warning.

Missing: The reference's detailed explosion-kill server audit records and possible spawn-kill warning. This is logging, not spawn protection or automatic moderation.

## HUD and rendering preferences

### Independent ammo-HUD visibility and legacy layout selection — PARTIAL

Reference: `R/client/TickHandlerClient.java` (HOTBAR overlay and `renderAmmoHudPrimary`/`renderAmmoHudSecondary`), `R/common/FlansMod.java` (`bulletGuiEnable`, `fancyBulletGui`).
The ammo HUD can be disabled independently. When enabled, the fancy setting selects between two reference layouts for primary/secondary ammunition.

Target checked: `T/client/render/ClientHudOverlays.java#HUD`, `#renderPlayerAmmo`, `T/config/ModClientConfig.java`, and `T/config/CommonConfigSnapshot.java`.
The target draws an ammo HUD but exposes neither an independent ammo-HUD toggle nor the reference layout selector. The shootable durability-bar option controls item bars, and hiding the entire vanilla GUI is a different capability.

Missing: Independent ammo-HUD visibility and user-selectable legacy ammo-HUD layouts.

## Findings table

| Subsystem | Feature | Status | Confidence |
| --------- | ------- | ------ | ---------- |
| Multiplayer | Content-definition mismatch enforcement | MISSING | HIGH |
| Interactions | Configurable armed block-use suppression | PARTIAL | HIGH |
| Administration | Explosion-kill audit/spawn-kill warning | MISSING | HIGH |
| HUD | Ammo-HUD visibility/layout controls | PARTIAL | HIGH |

## Areas requiring deeper audit

- Driveable physics, collision response, aircraft controls, mecha movement, and legacy model animation have substantially different implementations. This audit does not establish trajectory-level or visual equivalence across representative packs, especially articulated vehicles and custom model transforms.
- Exercise multiplayer reconnect/reload, late entity tracking, occupied-seat synchronization, and persisted rounds in-game to assess timing-dependent parity. Source-level searches do not prove absence of subtle synchronization differences.

These follow-up areas are validation limits, not additional counted missing features.
