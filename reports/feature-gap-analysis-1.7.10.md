# Feature gap analysis: Ultimate 1.7.10 → Ultimate 2.0

Initial audit: 2026-09-10. Last updated: 2026-09-21. Direction is strictly reference → target.

- Reference: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-1.7.10`, HEAD `9b669b12149c3c9fa3a69f1a4ac5d8a55367506d`.
- Target initially audited: `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, HEAD `08494f8a4972c249feb4e7d8522e981547439c4e`.
- Target revalidated: HEAD `e0b68dc60fc7060324ee4293678183aca93650b7` plus the current working tree.
- Sources inspected were the current working trees, not pristine commit snapshots. Existing unrelated target command and image changes were left untouched; the reference working tree was clean.
- Completed findings are removed as they are implemented, so the report remains a backlog rather than a historical snapshot.
- Remaining: **0 MISSING, 0 PARTIAL, 0 UNCERTAIN**. These counts describe the findings below, not a percentage of port completeness.
- Findings closed by a deliberately different design, rather than by reproducing the reference, are recorded under "Deliberate divergences" instead of being deleted.

## Scope and evidence conventions

This is a static, reference-driven semantic audit. Discovery covered reference types and configuration keys, weapons and ammunition, grenades, deployable/AA guns and targeting, tools and armor, driveables/aircraft/mechas, controls, rendering and effects, teams/game modes, commands, inventories/crafting, persistence, and networking. Candidate gaps were traced to active reference callers and checked against target entities, handlers, parsers, menus, configuration, and replacement mechanisms. Commented-out implementations and unused reference declarations were excluded.

Evidence paths use these roots:

- `R/` = reference `../src/main/java/com/flansmod`.
- `T/` = target `../src/main/java/com/flansmodultimate`.
- Resource paths explicitly identify their repository.

`MISSING` means the described capability has no equivalent in the inspected target paths. `PARTIAL` means the broader mechanic exists but the stated option or secondary behavior does not. Confidence concerns the narrow finding, not full subsystem equivalence. No gameplay code, packs, wiki pages, or generated files were changed. No builds or in-game tests were run; runtime parity is not established by this report.

## Findings table

No outstanding findings. Both remaining entries were closed by the deliberate divergences below.

## Deliberate divergences

Reference behavior settled by a different design decision rather than reproduced. These are closed, not outstanding.

### Blocking block interactions while armed: one client preference instead of two server booleans

Reference: `R/common/guns/ItemGun.java#onPlayerInteract`, registered through the first gun item in `R/common/FlansMod.java`; `holdingGunsDisablesChests` (default on) and `holdingGunsDisablesAll` (default off).
Right-click block interaction can be canceled for inventory blocks specifically, or for all blocks, while a gun is held.

Target: `T/config/EnumGunBlockInteraction.java`, `T/config/ModClientConfig.java` (`gunBlockInteraction`, Input Settings), `T/event/handler/ClientEventHandler.java#isBlockUseSuppressed`.
One client option with three values replaces the two booleans: `ALLOW`, `NO_CONTAINERS` (default) and `NONE`. Containers are detected with `state.getMenuProvider(level, pos)`, the same test `GunItem#doesSneakBypassUse` already uses. Under `NO_CONTAINERS` sneaking opens the container anyway, which turns that existing sneak bypass into a deliberate escape hatch; `NONE` has no way through.

Why the reference design was not reproduced:

- The two booleans overlap. `holdingGunsDisablesAll` subsumes `holdingGunsDisablesChests`, so two of the four combinations mean the same thing. Three named values say the same with no invalid state.
- It is a personal preference, not a rule of play. Giving up a block interaction costs other players nothing, so it belongs in the client config and the options screen rather than in server configuration, and needs no synchronization or packet.
- The gap was never the mechanism, but the default. Before this, aiming at a chest opened it whether or not the player sneaked; the reference's on-by-default chest rule existed to prevent exactly that, and `NO_CONTAINERS` restores that default.

Implementation note: suppression is applied to the vanilla use-item event, not to the configured aim button, so it holds whichever buttons the player has bound and aiming is unaffected since it is read from the key itself.

Not carried over: two separate booleans, server-side configuration of this behavior, and the reference's lack of any sneak bypass under the chest rule.

### Content-definition mismatch: warn per pack instead of kicking

Reference: `R/common/sync/Sync.java`, `R/common/sync/SyncEventHandler.java#playerJoined`, `R/common/network/PacketHashSend.java`, `kickNonMatchingHashes`.
The reference hashes all normalized definition text into one aggregate hash. On dedicated-server login the server sends it, the client replies with its own, and with `kickNonMatchingHashes` enabled the server disconnects a mismatching client.

Target: `T/common/sync/ContentFingerprint.java`, `T/common/sync/EnumContentMismatch.java`, `T/network/client/PacketContentFingerprint.java`, `T/ContentManager.java#registerConfigs`, `T/event/handler/CommonEventHandler.java#onPlayerLogin`.
The target fingerprints definition text per content pack (SHA-256, normalized to drop blank lines, comments and indentation) and sends the server's fingerprints to each player on login. The client compares them with its own and names the packs that differ, are missing, or are unknown to the server. Nobody is disconnected and the client sends nothing back.

Why the reference design was not reproduced:

- The reference check cannot stop a cheater. The client simply returns a string, so a modified client echoes the server's hash and passes.
- A missing pack is already caught upstream. Forge's registry synchronization refuses a login when the client lacks registered items, which is the failure a kick would otherwise cover.
- What remains is packs with the same shortnames but different numbers. Since the mod is server-authoritative, that costs the player accurate information (displayed statistics, zoom, animation timing) rather than fairness, which makes it a diagnosis problem. Naming the offending pack solves it; disconnecting does not.
- One-way exchange keeps client-supplied data out of server decisions, unlike the reference, whose server acts on a value the client chose.

Cost: one SHA-256 pass over definition text during the existing load, and one small packet per login. Nothing runs per tick.

Not carried over: `kickNonMatchingHashes` and its enforcement, and the aggregate whole-installation hash. Per-pack fingerprints replace it, since an aggregate can only say that something differs.

## Areas requiring deeper audit

- Driveable physics, collision response, aircraft controls, mecha movement, and legacy model animation have substantially different implementations. This audit does not establish trajectory-level or visual equivalence across representative packs, especially articulated vehicles and custom model transforms.
- Exercise multiplayer reconnect/reload, late entity tracking, occupied-seat synchronization, and persisted rounds in-game to assess timing-dependent parity. Source-level searches do not prove absence of subtle synchronization differences.

These follow-up areas are validation limits, not additional counted missing features.
