# Minecraft Version Code Differences

This is the cumulative record of code and resource differences required by
Minecraft, Forge, and NeoForge API changes. Entries are grouped by source and
target version, then by class or resource location. Update an existing entry
when its adaptation changes; do not add a section for each merge run. Commit
history and validation belong in the merge commit and task result.

The 1.21.1 entries consolidate the maintained porting notes, the historical
`master` to `1.21.1` port audit, and fixes prompted by the main-module parity
audit. Paths name the 1.21.1 branch unless explicitly stated otherwise.
Recheck them against the target branch during future merges.

## Minecraft 1.20.1 / Forge → Minecraft 1.21.1 / NeoForge

### Content-loading package on master

On `master` (Forge 1.20.1), the content-loading implementation now lives in
`com.flansmodultimate.content`. `ContentManager` delegates regeneration decisions
and asset generation to `ContentPackAssets`, `ContentPackModels`,
`ContentPackLocalization`, `GeneratedTextureFiles`, and `ContentPackSounds`;
`LegacyTextureAliases` owns the armor, GUI, and skin collision state. Generators
receive the provider's types and alias maps explicitly. Loading and generation
order remain unchanged. When porting this refactor, update internal imports and
resource-provider wiring while preserving the target branch's loader APIs.

`com.flansmodultimate.PackagedContentPackApi` remains at its original location
with the same registration overloads. Its implementation delegates to
`content.PackagedContentLoader`, so packaged content mods need no source changes.
The older root-package paths in the tables below still name the 1.21.1 branch.

Master additionally uses `ContentPackDiscovery` for the shared directory/ZIP/JAR
selection, `ContentProcessingCache` for successful generation, and
`PackAssetIndex` / `ContentFileCache` for persisted texture signatures and parsed
asset JSON. Warm startup validates archive metadata or directory file metadata
and effective type values after categories; it skips resource hashing, PNG
decoding, source model parsing, sound/recipe regeneration checks, and repacking.
The cache is external to packs and disposable. Failed regeneration must not
inherit a previous successful phase flag. Port these cache semantics while
retaining the target's recipe formats and resource repository APIs.

Authored custom model and blockstate filenames now remain canonical on master;
`ModernAssetAliases` maps them to registered IDs in the read-only view. Old
physical ID migrations recover missing originals from the previous ID mapping.
Generated texture manifests track ownership and output digests so authored or
edited files survive cleanup; both armor layers contribute to a collision.
English fallback generation also supports packs without an English `.lang`.

Master's `soundPackPriority` in the early content-loading TOML lists standalone
packs (`pack:<basename>`) and shared packaged modules (`mod:<id>`), highest
priority first. Missing entries are appended and removed sources retain their
positions. `SoundPriorityPlan` chooses complete event definitions and individual
.ogg resource owners independently, then resolves durations through that graph;
`SoundAssetIndex` caches the inventories and measurements. Servers resolve the
same plan without initializing client sound classes. Loader version 7 also
normalizes standalone legacy sound assets when server data is reprocessed.

`SoundPriority.repositorySource()` supplies a required fixed TOP, sound-only
pack (`!flansmodultimate_sound_priority`) through the client pack finder. Its
merged `sounds.json` sets `replace` on the selected definitions, and its .ogg
resources delegate to the selected source. It leaves texture/data pack ordering
alone. On a port, adapt `Pack.Info`, `Pack.create`, resource suppliers and file
pack constructors to the target's APIs, preserving both the sound-only scope
and priority above packaged assets. The ordinary Minecraft resource-pack order
cannot override this configured selection. Changing just the list does not
invalidate content generation or trigger audio remeasurement.

### NPC ranged projectile integration on master

Forge 1.20.1 now intercepts Custom NPCs' `EntityNPCInterface.performRangedAttack`
and its production SRG name `m_6504_` through `EntityNPCInterfaceMixin`. The module
uses API 0.6 `FlansProjectiles`, `ProjectileParameters`, `ProjectileShot` and
`WeaponMuzzle`; no Custom NPCs dependency is added to the main mod. Its settings
are mixed into `DataStats.readToNBT` and `save`, under `WolffsModWeapons`, and the
`Wolff's Mod` page uses `SPacketMenuGet`/`SPacketMenuSave` with `EnumMenuType.STATS`.
Check these names, permission paths and GUI callbacks against the installed
Custom NPCs jar when porting. Missing setting keys retain their defaults.

`NpcRangedControlsMixin` is client-only and targets `SubGuiNpcRangeProperties` and
`SubGuiNpcProjectiles`, including both `init` and production `m_7856_`. It marks
inactive fields and buttons read-only after initialization and guards their
callbacks so focus loss cannot overwrite stored defaults. Verify widget IDs and
the `GuiWrapper.parent` chain when porting: `getParent()` returns the root, or the
dialog itself before attachment. `FlansProjectiles.getSources` supplies reusable
setting authority without spawning entities; dialog IDs and tooltips stay in the
NPC module. Any still-used fallback in a mixed bank or belt remains editable.

`TYPE_PROPERTIES` adds default-enabled model inheritance through API 0.6
`FlansEntityTypes`/`EntityTypeProperties`. The NPC-specific reversible overlay
captures all replaced defaults before writes and returns stored defaults from
`DataStats`, `DataAI` and `DataAdvanced` save methods. Their load hooks invalidate
only the loaded component's captured defaults. `EntityNPCInterfaceMixin` extends the
existing `writeSpawnData()`/`readSpawnData(CompoundTag)` server-to-client transport
with switch state and the small set of stored mapped fields; the ordinary sounds
page does not request stats data itself. No new client-to-server authority is added.
The server refreshes inheritance before NPC ticks; model NBT load restores actual
saved health after resolving hull HP. Health changes preserve the damaged fraction.
`WolffsModTypeMaxHealth` records the effective saved maximum separately from
`MaxHealth`, which remains the user's fallback. Older saves use their original
`MaxHealth` when converting the saved damage fraction to inherited hull HP.

`NpcTypeControlsMixin` also targets `GuiNpcStats`, `GuiNPCSoundsMenu`,
`SubGuiNpcMeleeProperties` and `SubGuiNpcMovement`, using the installed jar's IDs
and `GuiButtonNop.setDisplay(int)`. It displays converted inherited values and
keeps focus/button callbacks from writing them back as defaults. Distinct native
mount values can be displayed together without collapsing them into one weapon.
`EntityAIRangedAttackMixin` redirects the ranged goal's `getBurst`/`getBurstDelay`
calls, retaining its targeting and navigation while removing artificial burst
gaps for inherited native banks. Its fractional cooldown accounts for the goal's
post-decrement test. Recheck `tick`/`m_8037_`, `stop`/`m_8041_`, field names, NBT
keys and the live maximum-health attribute bound when porting these hooks.

The firing path owns projectile creation on the server and reuses Flan's native
entities and damage attribution. An explicit `FiredShot` platform definition
preserves fallback ammunition overrides for NPCs whose model is a driveable but
whose live entity is a Custom NPC. The model entities remain detached, static
rendering objects. Preserve that separation and static preview coordinate frame
when adapting model muzzle transforms; do not spawn a driveable to fire its NPC model.

### NPC equipped-item authority on master

Forge 1.20.1 adds `FlansEquipment`, equipped weapon/armor inspection records,
`IEquipmentPolicy` and the client-only `FlansEquipmentRender` entry point.
Native item actions run on the server and consume real ammunition or thrown
weapons. These are separate from the older reusable-template model-bank path.
The NPC module owns vanilla weapon dispatch, charge/reload/cadence, item readouts,
and the Custom NPCs-specific `NpcWeaponAdapter` extension.

The installed Custom NPCs build overrides `getDamageAfterArmorAbsorb` to omit
native armor. `EntityNPCInterfaceMixin` calls its vanilla superclass when equipment
authority is enabled, bypasses `Resistances.applyResistance` and the knockback
resistance field, and provides mob armor/shield wear hooks. Check the descriptors
and SRG aliases against the target Custom NPCs jar; the bytecode contract test
checks those seams without loading the mod or any client classes. The existing
stats packet and spawn/update transport carry the four additional named switches.

Vanilla bows and crossbows still use 1.20.1 stack NBT for charged projectiles and
enchantments; newer branches need their native item-component APIs. Modded bow
subclasses use arrow factories/custom-arrow hooks. Player-specific weapon use is
not emulated with a fake player. Client-only hooks run after Custom NPCs'
`AnimationHandler.animateBipedPost` and in the resistance slider editor. Armor
resting poses are applied after `HumanoidModel.copyPropertiesTo` in the armor
layer; recheck Forge/NeoForge custom armor model hooks on each target branch.

Multi-target mixins into Custom NPCs data classes must mark their own field
shadows explicitly `remap = false`, even when the mixin itself disables remapping.
Mixin rejects a remappable shadow shared by multiple targets during runtime
validation. The installed 20260711 Custom NPCs jar also fails in Forge's
`GameTestServer`: its world-directory lookup attempts to load client Minecraft
and returns null on that server subtype. Common NPC equipment mixins load before
that third-party failure; this harness cannot verify live combat or editor behavior.

### NPC static world-model rendering

The Forge 1.20.1 model picker uses the client-only `NpcModelBrowserMixin` on
Custom NPCs' `GuiCreationEntities`. It targets both `init` and its production SRG
name `m_7856_`, plus the unmapped `scrollClicked` callback. It groups existing
entity types by Flan kind and the public content-pack API, then restores the
original row indices before letting Custom NPCs select a model. When porting,
verify the target Custom NPCs picker methods and scroll API, and retain the
existing entity registrations and saved model IDs.

`NpcModelPreviewMixin` redirects the model screen's `drawNpc` call in
`GuiCreationScreenInterface.render` (production `m_88315_`). The NPC module fits
the static preview using API `0.5` geometry bounds, entity dimensions and part
collision boxes. `NpcModelBrowserMixin` adds a paintjob level and stores its ID
in Custom NPCs `ExtraData` under `FlanPaintjob`; the model entity reads that key
for simple rendering while ordinary rendering retains the selected NPC skin.
These additions currently live on Forge 1.20.1; verify the GUI call descriptor,
vertex collector API and saved extra-data path when porting them.

The client-only `NpcNameplateMixin` targets Custom NPCs' unmapped
`RenderNPCInterface.renderLivingLabel`, replacing the second float local at its
store (the label height after the text-scale calculation). It raises Flan labels
above the combined model/entity/part bounds, with NPC scale and renderer Y offset;
simple rendering retains the detached model's scale. Recheck that local-variable
layout for another Custom NPCs build. The preview and nameplate share the NPC
module's `FlanModelBounds` helper and the existing public geometry API.

On Forge 1.20.1, the NPC module keeps its Custom NPCs mixins in
`com.wolffsmod.npcs.mixin` and its buffer adapter in
`com.wolffsmod.npcs.platform.render`. Its client-only `NpcRenderContextMixin`
wraps the `MultiBufferSource` argument of Custom NPCs' concrete
`RenderCustomNpc.render(EntityCustomNpc, float, float, PoseStack, MultiBufferSource, int)`.
The method descriptor is also used by the NeoForge 1.21.1 Custom NPCs build.
The main mod has no Custom NPCs dependency. Recheck that concrete method descriptor
when porting to another Custom NPCs build, rather than targeting a vanilla bridge method.

`FlansModelPreviews.renderWorld` is the reusable client API boundary. It accepts
Minecraft entity/pose/buffer types and leaves GPU compatibility, detail policy,
track envelopes and atlas generation in the main mod. The NPC adapter tags only
the normal body consumer; outlines, invisible-body passes and extra texture layers
retain the supplied consumer. The shared API, world renderer, pose measurements and
tests are identical on both branches. Both expose API `0.3`; the NPC module requires
main mod `2.2` or later for the new entry point.

The NPC module's `NpcRenderBuffers` owns the vertex/pose adaptation: Forge 1.20.1
snapshots a pose by copying its matrices into a fresh pose and forwards
`vertex`/`color`/`uv`/`endVertex` calls. NeoForge 1.21.1 uses `Pose.copy()` and forwards
`addVertex`/`setColor`/`setUv`/`setNormal`, while its model converts the vanilla packed
ARGB tint to the API's float components. The main mod's `platform/render/WorldModelBoundsCollector`
owns the matching version-specific vertex collection, so bounds and LOD policy stay shared.

Forge packages the module's own mixin configuration and generated refmap through
`npcsJar`, with dev-run mixin arguments in `src/npcs/fmu-module.gradle`. Append
the NPC `--mixin.config` argument after MixinGradle's `afterEvaluate` run setup;
earlier arguments are overwritten and generated IntelliJ runs silently omit the
NPC hooks. Regenerate IntelliJ launch configurations with `genIntellijRuns` after
changing this setup. NeoForge
registers `wolffsmodnpcs.mixins.json` in the NPC module's authored `neoforge.mods.toml`,
uses Java 21 compatibility and omits the Forge refmap and MixinGradle wiring. Preserve
these module packaging differences when merging master later.

### Build, metadata, and registration

| Class or location | Source API and destination adaptation |
| --- | --- |
| `gradle/curseforge.gradle` | Both branches share separate, opt-in CurseForge upload tasks for the main mod, Packs Manager, and all registered modules. `build.gradle` supplies the loader tag explicitly: `Forge` for 1.20.1, `NeoForge` for 1.21.1. Forge uploads depend on the matching `reobf` task (except Packs Manager); NeoForge uploads use the compiled artifact directly. Module `curseforge` maps in `fmu-module.gradle` supply destination defaults; Gradle properties override them. URLs enable destinations, IDs and changelogs remain configurable, and the author token is read during task execution to preserve NeoForge configuration-cache compatibility. |
| `build.gradle`, `settings.gradle`, `src/*/fmu-module.gradle` | The ForgeGradle / Java 17 build becomes ModDevGradle / Java 21. Both scripts keep the same section order and share `configureModSourceSet`, `registerFmuModule`, and `EncryptOptionalContent` verbatim; only the loader block, Minecraft classpath wiring (`neoForge.addModdingDependenciesTo` instead of Forge's `implementation`), dev-run mod binding, metadata expansion, dev-only mod configuration (`localRuntime` instead of `runtimeOnly fg.deobf`), publishing (`artifact jar` for the reobfuscated Forge jar), Forge-only mixin refmap and `reobfJar` handling, and 1.21.1's `-Xlint` compiler flags differ. 1.20.1 disables the Gradle configuration cache because ForgeGradle 6 and MixinGradle do not support it. A source merge must not replace the NeoForge setup with Forge build logic. |
| `src/main/templates/META-INF/neoforge.mods.toml`, `src/*/resources/META-INF/neoforge.mods.toml` | NeoForge metadata replaces Forge `META-INF/mods.toml`. The main mod, bundled packs, official packs, and optional modules remain separate artifacts with their own metadata. |
| `src/main/java/com/flansmodultimate/platform/PlatformPaths.java`; `src/packsmanager/java/com/flansmodultimate/packsmanager/platform/PlatformPaths.java` | Main-mod and standalone Packs Manager callers use small path APIs for the game, config, and (in the main mod) mods directories. Each source set keeps its loader-specific `FMLPaths` import; the helpers return raw paths so callers retain their own normalization and resolution rules. |
| `src/main/java/com/flansmodultimate/platform/PlatformEnvironment.java`; `src/packsmanager/java/com/flansmodultimate/packsmanager/platform/PlatformEnvironment.java` | Runtime side and production checks use a small API in each independent source set. Forge and NeoForge retain their own `FMLEnvironment` imports there; annotation values and Forge's client packet `DistExecutor` call still use their required loader APIs. In the main mod, `isModLoaded` and `currentServer()` also wrap `ModList` and `ServerLifecycleHooks`. |
| `src/main/java/com/flansmodultimate/CreativeTabs.java` | Creative tab definitions and ordering are identical on both branches. `FlansMod` passes a registration callback so each loader keeps its own deferred registry. Driveable stack data is read with the tab display's holder provider (ignored on 1.20.1), and items sort by their `BuiltInRegistries` key. |
| `src/main/java/com/flansmodultimate/FlansMod.java`, `apocalyse/ApocalypseContent.java`; `platform/registry/RegistryEntry.java` | Both branches create deferred registers from vanilla `Registries` keys and look entries up through `BuiltInRegistries`; only Forge/NeoForge-owned registries such as fluid types keep a loader key. Registered items, blocks and sounds are exposed as `RegistryEntry`, which wraps `RegistryObject` or `DeferredHolder` (`holder()` on 1.21.1 only). Apocalypse content fields are plain `Supplier`s. The master-only enchantment registry in `common/EnchantmentModule` keeps `RegistryObject`; its eight enchantment implementation classes are private static nested classes there. 1.21.1 retains data-driven enchantments. `EnchantmentModule` and `FlanEntityPermissions` live directly in `common` on both branches. |
| `src/main/java/com/flansmodultimate/network/PacketHandler.java`; `platform/network/NetworkPlatform.java` | `PacketHandler` is shared: it lists every packet, sorts each direction by class name, decodes packets, and offers the send API. `NetworkPlatform` is the loader transport. Forge keeps its `SimpleChannel` (protocol `15`, one id space ordered by name then direction); NeoForge carries each direction in one payload envelope (protocol `8`, ids per direction). Add new packet classes only to `PacketHandler`, in the correct direction list. |
| `src/main/java/com/flansmodultimate/platform/network/PacketIO.java`; `common/teams/PlayerLoadout.java`, `network/client/Packet{LoadoutState,TeamsState,DriveableRenderState}.java`, `common/entity/AAGun.java` | Item stacks and components use different stream codecs (`FriendlyByteBuf.writeItem`/`writeComponent` versus `ItemStack.OPTIONAL_STREAM_CODEC`/`ComponentSerialization.STREAM_CODEC`). Packet code writes them through `PacketIO` and writes collections as a VarInt size followed by the elements, which matches `writeCollection`. |
| `src/main/java/com/flansmodultimate/platform/entity/SpawnDataEntity.java`; entities with extra spawn data | Entities implement `writeSpawnData(PacketBuffer)`/`readSpawnData(PacketBuffer)` through `SpawnDataEntity`, which adapts Forge `IEntityAdditionalSpawnData` (`FriendlyByteBuf`) or NeoForge `IEntityWithComplexSpawn` (`RegistryFriendlyByteBuf`). The buffer adapter lives in `platform/network/PacketBuffer`. |
| `platform/event/ClientGameEvents.java`; `client/debug/RenderDiagnosticsCommand.java` | The existing client event subscriber handles Forge/NeoForge command registration. `RenderDiagnosticsCommand` receives the vanilla command dispatcher and is identical on both branches. |
| `common/command/SavedFlanEntityScanner.java`; `platform/world/LevelFilePlatform.java` | The scanner is shared and opens saved entity regions through `LevelFilePlatform.openEntityRegion`. 1.20.1 uses the three-argument `RegionFile` constructor; 1.21.1 supplies `RegionStorageInfo` with the dimension and entity-storage profiler labels. Neither path loads chunks. |

### State, events, and menus

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/platform/item/ItemStackData.java`; `common/driveables/DriveableData.java` | Minecraft 1.21 item stacks no longer expose the old mutable NBT tag API. Both branches use `ItemStackData` for custom stack data and pass `HolderLookup.Provider` when saving or parsing stacks, including nested driveable inventory, magazines, loadouts, and player stats; 1.20.1 ignores the provider. `ItemStackData.copy` is detached on both branches, so callers write changes back with `set` or `update`. |
| `src/main/java/com/flansmodultimate/platform/world/{SavedDataPlatform,FlanSavedData}.java`; `common/teams/TeamsSavedData.java`, `apocalyse/common/world/ApocalypseSavedData.java` | Minecraft 1.21 saved data uses `SavedData.Factory` and passes a registry provider to `save`/`load`. Both branches load through `SavedDataPlatform.computeIfAbsent` and implement `save(tag, registries)`/`load(tag, registries)`; both extend `FlanSavedData`, whose 1.20.1 version adds the one-argument `save(tag)` adapter. |
| `src/main/java/com/flansmodultimate/platform/item/ItemCapabilities.java`, `platform/fluid/FluidPlatform.java`; `common/entity/Driveable.java`, `common/driveables/{DriveableData,FluidFuel}.java`, `common/item/GunItemHandler.java` | Item energy and fluid handlers come from Forge capabilities (`LazyOptional`) or NeoForge item capabilities (nullable), `FluidStack` lost its copy-with-amount constructor, and the water fluid type moved from `ForgeMod` to `NeoForgeMod`. Callers use the nullable `ItemCapabilities` lookups, `FluidPlatform.copyWithAmount` and `FluidPlatform.isEyeInWater`; the handler interfaces themselves still come from loader packages. `FluidFuel` is identical on both branches: bucket volume comes from `FluidPlatform`, and `FluidContainerPlatform` owns single-item handler lookup and tank selection through a caller-supplied fluid predicate. |
| `src/main/java/com/flansmodultimate/platform/world/ChunkTicketPlatform.java`; `common/teams/TeamsManager.java`, `FlansMod.java` | Teams chunk forcing uses `ForgeChunkManager.forceChunk` on 1.20.1 and a registered NeoForge `TicketController` on 1.21.1 (registered from the `FlansMod` constructor). `ChunkTicketPlatform.force` has the same arguments and result on both. |
| `src/main/java/com/flansmodultimate/platform/PlatformTags.java`; `util/ModUtils.java` | Loader convention tags moved from `forge:` to `c:` and were renamed (`Tags.Blocks.GLASS` versus `GLASS_BLOCKS`). `PlatformTags` exposes the matching tag. |
| `src/main/java/com/flansmodultimate/platform/item/ItemStackData.java`, `platform/entity/EffectPlatform.java`; `apocalyse/common/util/ApocalypseLoot.java`, `common/types/InfoType.java` | Potions became holders stored in `PotionContents`, written books moved from NBT to `WrittenBookContent`, and mob effects are looked up by registry holder. `ApocalypseLoot` stores brewing-potion stack factories so its pool is shared despite `Potion` versus `Holder<Potion>`. Each selection creates a fresh stack, preserving potion order and random draws. `ItemStackData.potion`/`writtenBook` build those stacks, and `EffectPlatform.legacyEffect` maps legacy 1-based potion IDs (the 1.20.1 registry uses them directly; 1.21.1 is indexed from zero). |
| `src/main/java/com/flansmodultimate/platform/PlatformEvents.java`; `common/explosions/FlanExplosion.java` | Explosion start and detonate events are posted through `ForgeEventFactory` or NeoForge `EventHooks`; `PlatformEvents.onExplosionStart`/`onExplosionDetonate` wrap both. |
| `src/main/java/com/flansmodultimate/common/entity/Driveable.java`, `Mecha.java`, `Seat.java`, `Shootable.java`, and other entity subclasses defining accessors | Minecraft 1.21 defines synchronized entity data through `SynchedEntityData.Builder`. Platform base classes `FlanEntity` and `FlanArrow` own the version-specific `defineSynchedData` override and call the shared `defineEntityData(SynchedDataDefinition)`, which wraps `SynchedEntityData` or its builder. `FlanArrow` first defines vanilla arrow data. Empty entities inherit the no-op hook from `FlanEntity`. Subclasses override `defineEntityData` and call the superclass implementation where needed. |
| `platform/entity/FlanArrow.java`; `common/entity/ThrownGun.java` | The shared thrown-gun entity delegates vanilla arrow construction, pickup storage, save data and empty-pickup checks to `FlanArrow`. 1.20.1 persists the thrown stack under `weapon`; 1.21.1 uses the vanilla pickup item, keeps the synchronized weapon in step when that pickup changes, and discards or skips saving weaponless arrows. The thrown gun remains the pickup stack rather than vanilla's firing weapon. |
| `src/main/java/com/flansmodultimate/platform/entity/EntityPlatform.java`; apocalypse entities and worldgen, `Mecha.java`, `ShootingHelper.java`, `Bullet.java`, `PlayerHitbox.java` | Renamed or reshaped entity calls: `finalizeSpawn` (1.21 drops the NBT argument), `setSecondsOnFire` versus `igniteForSeconds`, `ServerPlayer.latency` versus `connection.latency()`, and `getStepHeight()` versus `maxUpStep()`, plus explosion immunity (`ignoreExplosion()` gains the explosion), Blast Protection knockback (a `ProtectionEnchantment` helper versus a data-driven enchantment lookup), the static versus instance `getEquipmentSlotForItem`, and crossbow charge time (1.21 takes the entity for enchantment effects). `finalizeSpawnWithEvent` posts the loader's finalize-spawn event (`ForgeEventFactory.onFinalizeSpawn` versus `EventHooks.finalizeMobSpawn`) and is used for the Apocalypse boss on both branches; other spawns call `finalizeSpawn` directly on both. Step height is supplied by overriding `maxUpStep()` on both branches. |
| `src/main/java/com/flansmodultimate/platform/block/FlanBlockEntity.java`; block entities with saved data or items | Block entities implement `saveData`/`loadData(tag, registries)` and the update-tag hooks; `FlanBlockEntity` bridges them to `saveAdditional`/`load` versus `saveAdditional`/`loadAdditional` and the update-packet methods. On 1.20.1 it also exposes `getItemHandler()` through the Forge item capability; NeoForge registers that capability separately. |
| `src/main/java/com/flansmodultimate/event/*.java`; `common/entity/Driveable.java`, `Mecha.java`, `Seat.java` | NeoForge custom cancellable events implement `ICancellableEvent` instead of carrying Forge's `@Cancelable`. Callers post through `platform/PlatformEvents`: `postCancellable(...)` returns whether a listener cancelled the event on both loaders (Forge `post` returns a boolean; NeoForge returns the event). This preserves firing and seat-entry cancellation. |
| `src/main/java/com/flansmodultimate/platform/menu/MenuPlatform.java`, `platform/block/{FlanBlock,FlanEntityBlock}.java`; `common/block/*Block.java`; `common/inventory/*Menu.java` | `MenuPlatform.open` uses Forge `NetworkHooks` or NeoForge `ServerPlayer.openMenu`; `MenuPlatform.menuType` wraps `IForgeMenuType` or `IContainerFactory`, and menus read their opening data from a `PacketBuffer`. Each block or entity writes the complete buffer expected by its menu's network constructor. Blocks extend `FlanBlock` or `FlanEntityBlock` and implement one `interact(state, level, pos, player, hand)` method; the base classes bridge the 1.20.1 `use` override or the 1.21 `useWithoutItem`/`useItemOn` pair (a pass falls through to the default interaction), and `FlanEntityBlock` supplies the 1.21 `codec()` and a vanilla-equivalent `PASS` default (used by `PowerCubeBlock`). `SulphurBlock` extends `FallingBlock` and keeps its own 1.21 `codec()`. Menu screens are listed once in `client/gui/ModMenuScreens`, registered through `MenuScreens::register` on 1.20.1 and `RegisterMenuScreensEvent` on 1.21.1. |
| `src/main/java/com/flansmodultimate/platform/client/ClientPlatform.java`; screens and renderers | Client frame timing (`getFrameTime`/`getDeltaFrameTime` versus `DeltaTracker`) and the screen background call (1.21 adds mouse position and partial tick) go through `ClientPlatform`. `ClientPlatform.modelItemId` strips the variant from baked-model keys (`ResourceLocation` versus `ModelResourceLocation`), and `ClientPlatform.skinTexture` reads a player's skin (`getSkinTextureLocation()` versus `getSkin().texture()`). Target screen callbacks still use the 1.21 mouse-coordinate and two-axis scroll signatures. |
| `src/main/java/com/flansmodultimate/platform/client/ArmPosePlatform.java`; `src/main/resources/META-INF/enumextensions.json` (1.21.1), `src/main/templates/META-INF/neoforge.mods.toml`; `client/ModClient.java` | Forge 1.20.1 adds the `both_arms_aim` `HumanoidModel.ArmPose` at runtime with `ArmPose.create`, which NeoForge removed. The 1.21.1 target declares `FLANSMODULTIMATE_BOTH_ARMS_AIM` in `enumextensions.json` (registered through `enumExtensions` in `neoforge.mods.toml`), and its constructor arguments come from the `ArmPosePlatform.BOTH_ARMS_AIM` `EnumProxy`. Both branches use the shared `ModClient.poseBothArmsAim` transform, which the server hitbox pose `PlayerSnapshot` `BOTH_AIM` mirrors. The enum extension is only applied at runtime, so a Java compile does not verify it. |
| `src/main/java/com/flansmodultimate/FlansMod.java` (`FALLBACK_TEXTURE`) | 1.20.1 still accepts the empty resource location as a texture placeholder, while 1.21 render APIs reject it. Callers use `FlansMod.FALLBACK_TEXTURE`, which is the empty location on 1.20.1 and the default bullet texture on 1.21.1. |
| `src/main/java/com/flansmodultimate/apocalyse/client/ApocalypseWorldChoice.java`; `platform/world/LevelFilePlatform.java` | Minecraft 1.21 world data IO uses `Path` and an `NbtAccounter` for compressed NBT reads, plus `Path`-based replacement of `level.dat`. `LevelFilePlatform` adapts both; the shared caller keeps the prior-world backup when recording the Apocalypse data-pack choice. |
| `src/main/java/com/flansmodultimate/platform/world/LootTablePlatform.java`; `event/handler/CommonEventHandler.java`, `common/types/PaintableType.java` | Loot-table ids are `ResourceLocation` on 1.20.1 and `ResourceKey<LootTable>` on 1.21.1, and stack NBT moved to the `custom_data` component. `LootTablePlatform.id` builds the matching id type for the injected-loot table set, and `LootTablePlatform.setCustomData` returns `SetNbtFunction` or `SetComponentsFunction` for paintjob loot. |
| `src/main/java/com/flansmodultimate/hooks/{IClientRenderHooks,client/ClientRenderHooksImpl,server/ClientRenderHooksNoop}.java`; `common/item/ICustomRendereredItem.java`, `event/handler/ModClientEventHandler.java`; `apocalyse/client/SulphuricAcidFluidExtensions.java` | Forge 1.20.1 attaches client item and fluid-type extensions through `Item.initializeClient` and `FluidType.initializeClient`; NeoForge registers them in `RegisterClientExtensionsEvent`. Both branches build the extension objects in shared code (`IClientRenderHooks.customItemExtensions()` and `SulphuricAcidFluidExtensions`); only the attaching call differs. Items that inherit two `initializeClient` defaults keep a Forge-only disambiguating override on 1.20.1. |
| `src/main/java/com/flansmodultimate/platform/event/{CommonGameEvents,ClientGameEvents}.java`; `event/handler/{Common,Client}EventHandler.java`, `apocalyse/event/handler/*` | Event classes, tick phases and accessors differ (`TickEvent` phases versus `Pre`/`Post` events, `LivingTickEvent` versus `EntityTickEvent`, `RenderGuiOverlayEvent` versus `RenderGuiLayerEvent`, pickup cancellation versus `TriState`). The `*GameEvents` subscribers adapt each event and call shared handler methods, whose bodies are identical on both branches. |
| `src/test/java/com/flansmodultimate/config/TestConfigLoader.java` | NeoForge seals `IConfigSpec.ILoadedConfig`; an anonymous Forge-style test implementation cannot be used. The target test fixture attaches an in-memory NeoForge loaded config. |

### Recipes, packs, and data resources

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/common/recipe/{RecipeDataCompatibility,RecipeJsonGenerator}.java`; `src/main/java/com/flansmodultimate/ContentManager.java` | `RecipeJsonGenerator` is identical on both branches and constructs legacy-format recipe JSON; `RecipeDataCompatibility` owns conversion, modern output-count limits, and warnings. Both branches fill missing `recipes/` or `recipe/` counterparts without replacing authored recipes and write generated `.txt` recipes in both formats. On either branch, regeneration requires both generated layouts to exist and modern crafting output counts to fit the item's stack limit. Crafting uses `result.item` in 1.20.1 and `result.id` in 1.21.1; cooking uses a string versus an object with `id`. The modern generated count is capped while the legacy count keeps the requested value. Shootable parsing can raise `MaxStackSize` to `RecipeOutput`; durability-based fuel parts, guns, and tools remain single items. |
| `src/main/resources/data/*/recipe/`; `src/*/resources/flans_content/*/data/*/{recipes,recipe}/` | Minecraft 1.21.1 loads recipes from singular `recipe/`, while 1.20.1 loads plural `recipes/`. Source content packs ship both directories in both branches; Minecraft ignores the inactive directory. Ordinary content packs can supply either or both directories, and the mod fills missing counterparts for supported vanilla recipe types. Main mod recipes remain in the version's active directory. Java compilation alone cannot verify these resources. |
| `src/main/resources/data/*/loot_table/`, `tags/block/`, `neoforge/biome_modifier/` | Minecraft 1.21.1 uses singular `loot_table/` and `tags/block/`. NeoForge biome modifiers use `neoforge/biome_modifier/` and `neoforge:add_features`; corresponding 1.20.1 paths and Forge type IDs are not portable. |
| `src/main/java/com/flansmodultimate/common/recipe/GunpowderRecipeCondition.java`; `src/main/resources/data/flansmod/recipe/gunpowder_from_charcoal.json` | NeoForge recipe conditions use its condition codec and the `neoforge:conditions` JSON member rather than the Forge condition representation. |
| `src/main/java/com/flansmodultimate/PackagedContentRepositorySource.java` | Logical packaged recipe roots contain no `pack.mcmeta`. Forge 1.20.1 supplied metadata when constructing `Pack.Info`; the 1.21.1 target constructs `Pack.Metadata` directly. `Pack.readMetaAndCreate` would reject these roots and silently omit their recipes. On both branches packaged packs are required, placed at the top and fixed in position (`Pack.create(..., TOP, true, ...)` versus `PackSelectionConfig(true, TOP, true)`), and the optional encrypted pack uses the same settings. |
| `src/main/java/com/flansmodultimate/ModRepositorySource.java`, `FilteringPackResources.java` | Content packs from the Flan folder are required, sit at the bottom so the fixed packaged official packs override them, and report the current pack format regardless of their original `pack.mcmeta`. 1.20.1 builds `Pack.Info` through `discoverPacks`; 1.21.1 enumerates directories and archives itself and calls `Pack.readMetaAndCreate` with `PackSelectionConfig(true, BOTTOM, false)`. `ContentManager` writes a `pack.mcmeta` into every processed pack and converts `.jar` packs to `.zip`, so the different discovery code selects the same packs. |
| `src/main/java/com/flansmodultimate/apocalyse/ApocalypseDatapackSource.java` | The Apocalypse data pack is built in but opt-in: it uses the `OPT_IN` pack source, which is not added to new worlds automatically, and is not required, so the world-creation and world-open choices (`CreateWorldScreenApocalypseMixin`, `ApocalypseWorldChoice`) decide whether a world enables it. 1.20.1 passes these settings to `Pack.readMetaAndCreate` directly; 1.21.1 passes a `PackLocationInfo` with `OPT_IN` and `PackSelectionConfig(false, TOP, false)`. |

### Client rendering and mixins

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/client/model/ModelRenderer.java`; `client/render/CustomRenderType.java` and related renderers | `VertexConsumer`, `BufferBuilder`, shader, GUI-layer, and post-processing APIs changed. Simple vertex emission, quad-buffer setup, immediate drawing and `ModelPart` rendering, immediate buffer sources and the global model-view matrix (`PoseStack` versus `Matrix4fStack`) go through `platform/render/VertexPlatform` (packed `vertex(...)`/`endVertex()` versus `addVertex(...)` chains and ARGB colour). Custom `VertexConsumer` implementations and shader code keep per-branch implementations. A successful Java compile does not establish a correct frame; client resource reload and representative in-world scenes remain necessary checks. |
| `src/main/java/com/flansmodultimate/platform/client/HudOverlayPlatform.java`; `client/render/ClientHudOverlays.java`, `apocalyse/client/ApocalypseHudOverlays.java` | HUD layers are registered through a shared `Registrar` (Forge GUI overlays versus NeoForge GUI layers; the camera-overlay anchor is `HELMET` versus `CAMERA_OVERLAYS`). Forge tracks the left status-bar height through `ForgeGui.leftHeight`; NeoForge layers use the vanilla layout, so the platform returns fixed row offsets there. |
| `src/main/java/com/flansmodultimate/platform/client/ParticlePlatform.java`; `client/particle/ParticleHelper.java` | The legacy `mobspell`/`mobspellambient` particles take their colour from the spawn velocity in 1.20.1 (`ENTITY_EFFECT`/`AMBIENT_ENTITY_EFFECT` providers call `setColor(xSpeed, ySpeed, zSpeed)`; ambient particles use 15% alpha). 1.21 removed `AMBIENT_ENTITY_EFFECT` and colours `ENTITY_EFFECT` through a `ColorParticleOption`. `ParticlePlatform.entityEffect` builds that option from the velocity with the same float-to-byte conversion and alpha, so these particles are not cached by name. Motion is unchanged, as `SpellParticle` treats the velocity identically on both versions. |
| `src/main/java/com/flansmod/client/model/TrackLinkLod.java`; `com/flansmodultimate/platform/render/TrackLinkLodCollector.java` | Track-link LOD selection is shared. The platform collector receives Forge's packed `vertex(...)` call on 1.20.1 or NeoForge's `addVertex(...).setUv(...).setNormal(...)` sequence on 1.21.1. Its geometry and envelope rules are the same on both branches. |
| `src/main/resources/assets/flansmodultimate/shaders/core/rigid_model.vsh` | Minecraft 1.21.1 `fog_distance` takes `(vec3 position, int shape)`. The target calls `fog_distance(position, FogShape)` and removed the old unused model-view uniform; the old call failed at client shader reload. |
| `src/main/java/com/flansmodultimate/mixin/PlayerDriveableEdgeMixin.java`; `BufferSourceAccessor.java` | The player sneaking-edge hook moved into `canFallAtLeast`, and `MultiBufferSource.BufferSource` now tracks `startedBuilders`. The target injectors/accessor use those 1.21.1 bytecode locations. |
| `src/main/java/com/flansmodultimate/mixin/DriveableCameraMixin.java`; `PlayerRendererMixin.java`; `CreateWorldScreenApocalypseMixin.java` | The camera boom constant became a float, player renderer rotations gained a scale parameter, and world creation no longer ticks. The target mixins use the revised constants, method signatures, and screen lifecycle. |
| `src/main/resources/flansmodultimate.mixins.json` | NeoForge 1.21.1 runs official Minecraft names and ModDevGradle does not generate the Forge refmap, so the target omits the stale `refmap` entry. Every mixin class must also appear in the proper common or client roster; source files can compile while unregistered hooks remain inactive. |

### Additional parity adaptations

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/common/item/GunItem.java`; `GrenadeItem.java`; `CustomArmorItem.java` | NeoForge's `ItemStack.getEquipmentSlot()` hook reports an explicit override, not every slot in which an item can be equipped. Items describe their modifiers once through `platform/item/ItemAttributes` (UUID and name on 1.20.1, `ResourceLocation` id on 1.21.1); the 1.21.1 side builds them with the appropriate `EquipmentSlotGroup` so guns, grenades, and armor retain their configured effects. |
| `src/main/java/com/flansmodultimate/mixin/LivingEntityDamageMixin.java`; `platform/damage/MutableDamageContext.java`, `platform/forge/ForgeDamageContext.java`; `event/handler/CommonEventHandler.java` | NeoForge `LivingIncomingDamageEvent` runs earlier than Forge `LivingHurtEvent`. The shared `applyLivingHurt(MutableDamageContext)` holds the hurt logic: Forge calls it from `LivingHurtEvent` through `ForgeDamageContext`, and the target calls it from `LivingEntity.actuallyHurt` using the active `DamageContainer`, after shield and cooldown handling. The mixin must remain registered in `flansmodultimate.mixins.json`. |
| `src/main/resources/data/flansmodultimate/enchantment/*.json`; `src/main/java/com/flansmodultimate/EnchantmentItemRepositorySource.java`; `src/main/resources/data/minecraft/tags/enchantment/` | Minecraft 1.21.1 defines enchantments and acquisition through data files and tags. The target supplies Flan item-support tags from registered items and assigns the six enchantments to the appropriate acquisition tags. |

### Reviewed differences without gameplay effect

These differences remain because the two APIs have different signatures or types. Each was reviewed against the other branch and changes no game logic or feature; callers share their bodies wherever a platform seam exists.

| Class or location | Difference and why behaviour is unchanged |
| --- | --- |
| Items with tooltips (`GunItem`, `GrenadeItem`, `DriveableItem`, `BulletItem`, `ToolItem`, and other `*Item.java`) | `appendHoverText` takes a `Level` on 1.20.1 and an `Item.TooltipContext` on 1.21.1; 1.21.1 reads registries from the context where 1.20.1 uses `ItemStackData.builtInRegistries()`. The tooltip lines are the same. A shared adapter would still need the per-version override signature, so it would not remove these lines. |
| `common/item/GunItem.java`, `GrenadeItem.java`, `GloveItem.java`, `CustomArmorItem.java` | `getUseDuration` gains the entity and `onEntitySwing` gains the hand. Enchantability is overridden through the `ItemStack`-sensitive `getEnchantmentValue(ItemStack)` on both loaders, which vanilla reaches through `ItemStack.getEnchantmentValue()`. `GunItem.onBlockStartBreak` is a Forge hook that NeoForge removed; guns cannot break blocks on either branch because `canAttackBlock` returns false. |
| `common/item/CustomArmorItem.java`, `CustomArmorMaterial.java`; `apocalyse/common/util/ApocalypseLoot.java`; `mixin/VehicleOpticsLightMixin.java` | Mob effects and potions are referenced as `Holder`s on 1.21.1. `ArmorMaterial` became a record held by the item; both branches build it from the same armor values (durability, defense, enchantability, equip sound, toughness, zero knockback resistance, iron-ingot repair). |
| Entities (`Driveable`, `Seat`, `Wheel`, `Flag`, `Flagpole`, `Parachute`, `Shootable`, `AAGun`, `DeployedGun`, apocalypse mobs) | All Java classes in `common/entity` are shared between 1.20.1 and 1.21.1. `FlanEntity` bridges `defineSynchedData`, `lerpTo` to `lerpEntity` (passing the 1.20.1 teleport flag, or false on 1.21.1), and `onAddedToWorld`/`onAddedToLevel` to `onEntityAdded`. `FlanSpawnEntity` and `FlanItemEntity` preserve the entities that use Forge `NetworkHooks` spawn packets on 1.20.1; NeoForge keeps its vanilla spawn path. All classes in `apocalyse/common/entity` and `apocalyse/common/util` are shared too. `platform/entity/FlanMonster` bridges spawn initialization to `finalizeEntitySpawn` (forwarding NBT on 1.20.1, null on 1.21.1), keeping equipment setup after vanilla initialization. `FlanMonster` and `FlanPathfinderMob` bridge custom death loot to `dropEntityDeathLoot`, always calling vanilla loot first. |
| Screens (`DriveableInventoryScreen`, `DriveableCraftingScreen`, `MechaInventoryScreen`, `FlansOptionsList`) | `mouseScrolled` gains a horizontal axis and list widgets take a height instead of a bottom edge; the bodies use the same vertical scroll value. |
| `config/*`, `client/gui/options/*`, `client/CommonConfigMirror.java`, `PackagedContentPackApi.java` | `ForgeConfigSpec` becomes `ModConfigSpec` (value specs are read through `getSpec()`), and `defineListAllowEmpty` reproduces Forge `defineList`, which accepts empty lists. Keys, defaults and ranges are identical. |
| `event/*Event.java`, `@EventBusSubscriber` classes, mod entry points in each source set | Cancellable events implement `ICancellableEvent` instead of `@Cancelable`; subscriber annotations and mod constructors follow each loader. |
| `mixin/*` | Injection targets follow each version's bytecode (method names, descriptors, and constants). |
| `FilteringPackResources.java`, `EncryptedResourcePack.java`, `PackagedContentRepositorySource.java`, `ModRepositorySource.java`, `ApocalypseDatapackSource.java` | `Pack.Info`/`Pack.create` become `PackLocationInfo`, `PackSelectionConfig` and `Pack.Metadata`. The selection settings match (see above). |
| `apocalyse/ApocalypseContent.java`, `apocalyse/common/block/{SulphuricAcidBlock,SulphurBlock}.java` | `ForgeFlowingFluid` becomes `BaseFlowingFluid`, bucket and liquid blocks take the fluid instead of a supplier (fluids register before blocks and items), and falling blocks need a `codec()`. |
| Custom `VertexConsumer` classes (`RigidBatch`, `DriveableImpostorCache.BoundsConsumer`, `VehicleThermalRenderer`), `client/particle/LegacyParticleRenderTypes.java`, `client/render/gpu/GpuModelCache.java`, `client/model/ModelBase.java` | Rendering interfaces changed (vertex element methods, `ParticleRenderType.begin` returning the builder with no `end`, upload buffers, the removed `IViewRotMat` uniform, packed model colours, and the packed-colour `Model.renderToBuffer`). Each 1.21 particle render type sets its own blend and depth state, so dropping `end` does not leak state. These need in-game checks rather than a compile. `BewlrRoutingModel` is shared apart from its `ModelData` import: while `SKIP_BEWLR` is set for the plain-icon fallback, it reports the wrapped model's display transforms and delegates `applyTransform`, so fallback icons keep their JSON hand, ground and frame positioning on both branches. |
| `common/raytracing/Raytracer.java`, `util/ClassLoaderUtils.java` | `Raytracer` is shared: `(Entity) null` selects the entity-taking `ClipContext` constructor on both versions, avoiding ambiguity with the 1.21 collision-context overload. ASM's `SimpleRemapper` constructor takes the API version on the newer ASM. |
| `src/main/resources` | Data directories are renamed (`recipe`, `loot_table`, `tags/block`, `neoforge/biome_modifier`), recipe results use `id`, conditions use `neoforge:conditions`, `dimension_type` uses the flattened uniform-int format, bucket models use the `neoforge:` loader, and `pack.mcmeta` formats differ. The data content is the same. Their pack descriptions differ only in wording. |

## Minecraft 1.21.1 / NeoForge → Minecraft 26.1.2 / NeoForge

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/client/render/entity/FlanEntityRenderer.java`; `DriveableRenderer.java` | The 26.1.2 entity renderer API separates `extractRenderState` from `submit` and passes `EntityRenderState`, `SubmitNodeCollector`, and `CameraRenderState`. The target extracts entity values into its state, submits model geometry from that state, and calls superclass feature submission. Mutable entity/game access belongs in extraction. |
| `src/main/java/com/flansmodultimate/client/render/InstantBulletRenderer.java` | Level effects similarly store snapshots in `LevelRenderState` during extraction and submit them afterward, rather than drawing from live world data during the render call. |
| `src/main/java/com/flansmodultimate/client/render/entity/FlanEntityRenderer.java` (`getTextureLocation`) | The 26.x code uses `Identifier` where the 1.21.1 implementation uses `ResourceLocation`. Texture and registry boundaries must use the destination identifier type. |

## Minecraft 26.1.2 / NeoForge → Minecraft 26.2 / NeoForge

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/client/render/item/LegacyItemRenderBridge.java`; `src/main/java/com/flansmodultimate/client/render/RenderTypeBufferSource.java` | The 26.1.2 bridge passed Minecraft `MultiBufferSource` to legacy item renderers. The 26.2 branch uses its own `RenderTypeBufferSource` boundary while discovering requested render types and replaying each deferred geometry pass through `SubmitNodeCollector`. Item and driveable renderers accept that destination buffer interface. |
