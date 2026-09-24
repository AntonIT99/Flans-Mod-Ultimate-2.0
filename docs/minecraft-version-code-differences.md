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

### Build, metadata, and registration

| Class or location | Source API and destination adaptation |
| --- | --- |
| `build.gradle`, `settings.gradle`, `src/*/fmu-module.gradle` | The ForgeGradle / Java 17 build becomes ModDevGradle / Java 21. Optional modules retain their descriptors, and the root build binds their source sets, main-module dependency, NeoForge dev-run mod, and separate jar tasks. A source merge must not replace that setup with Forge build logic. |
| `src/main/templates/META-INF/neoforge.mods.toml`, `src/*/resources/META-INF/neoforge.mods.toml` | NeoForge metadata replaces Forge `META-INF/mods.toml`. The main mod, bundled packs, official packs, and optional modules remain separate artifacts with their own metadata. |
| `src/main/java/com/flansmodultimate/network/PacketHandler.java` | NeoForge payload registration and handlers replace the Forge network channel setup. New packet classes need explicit destination registration and the correct direction; compiling a packet class alone does not make it usable. |

### State, events, and menus

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/platform/item/ItemStackData.java`; `common/driveables/DriveableData.java` | Minecraft 1.21 item stacks no longer expose the old mutable NBT tag API. The target uses `ItemStackData` for custom stack data and passes `HolderLookup.Provider` when saving or parsing stacks, including nested driveable inventory and magazines. |
| `src/main/java/com/flansmodultimate/common/entity/Driveable.java`, `Mecha.java`, `Seat.java`, `Shootable.java`, and other entity subclasses defining accessors | Minecraft 1.21 defines synchronized entity data through `SynchedEntityData.Builder`. Each subclass with its own accessors adds them in `defineSynchedData(builder)` and calls the superclass implementation where needed. |
| `src/main/java/com/flansmodultimate/event/*.java`; `common/entity/Driveable.java`, `Mecha.java`, `Seat.java` | NeoForge custom cancellable events implement `ICancellableEvent`. `NeoForge.EVENT_BUS.post(...)` returns the event, so callers inspect `.isCanceled()` on that result rather than treating `post` as a boolean. This preserves firing and seat-entry cancellation. |
| `src/main/java/com/flansmodultimate/common/block/*Block.java`; `common/inventory/*Menu.java` | `ServerPlayer.openMenu` replaces Forge `NetworkHooks` opening. Each block or entity writes the complete buffer expected by its menu's network constructor. Target screen callbacks also use the 1.21 mouse-coordinate, partial-tick, and two-axis scroll signatures. |
| `src/main/java/com/flansmodultimate/apocalyse/client/ApocalypseWorldChoice.java` | Minecraft 1.21 world data IO uses `Path` and an `NbtAccounter` for compressed NBT reads, plus `Path`-based replacement of `level.dat`. The target keeps the prior-world backup when recording the Apocalypse data-pack choice. |
| `src/test/java/com/flansmodultimate/config/TestConfigLoader.java` | NeoForge seals `IConfigSpec.ILoadedConfig`; an anonymous Forge-style test implementation cannot be used. The target test fixture attaches an in-memory NeoForge loaded config. |

### Recipes, packs, and data resources

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/common/recipe/{RecipeDataCompatibility,RecipeJsonGenerator}.java`; `src/main/java/com/flansmodultimate/ContentManager.java` | Both branches use the same recipe-data conversion logic to fill a missing `recipes/` or `recipe/` counterpart without replacing an authored version. Generated `.txt` recipes are written in both formats. Crafting outputs use `result.item` on 1.20.1 and `result.id` on 1.21.1; cooking outputs use a string versus an object with `id`. Minecraft 1.21.1 rejects an output `count` above the registered item's stack limit, so the modern generated result is capped while the legacy result keeps the requested count. NeoForge also detects oversized existing generated recipes for regeneration. Shootable parsing can raise `MaxStackSize` to `RecipeOutput`; durability-based fuel parts, guns, and tools remain single items. |
| `src/main/resources/data/*/recipe/`; `src/*/resources/flans_content/*/data/*/{recipes,recipe}/` | Minecraft 1.21.1 loads recipes from singular `recipe/`, while 1.20.1 loads plural `recipes/`. Source content packs ship both directories in both branches; Minecraft ignores the inactive directory. Ordinary content packs can supply either or both directories, and the mod fills missing counterparts for supported vanilla recipe types. Main mod recipes remain in the version's active directory. Java compilation alone cannot verify these resources. |
| `src/main/resources/data/*/loot_table/`, `tags/block/`, `neoforge/biome_modifier/` | Minecraft 1.21.1 uses singular `loot_table/` and `tags/block/`. NeoForge biome modifiers use `neoforge/biome_modifier/` and `neoforge:add_features`; corresponding 1.20.1 paths and Forge type IDs are not portable. |
| `src/main/java/com/flansmodultimate/common/recipe/GunpowderRecipeCondition.java`; `src/main/resources/data/flansmod/recipe/gunpowder_from_charcoal.json` | NeoForge recipe conditions use its condition codec and the `neoforge:conditions` JSON member rather than the Forge condition representation. |
| `src/main/java/com/flansmodultimate/PackagedContentRepositorySource.java` | Logical packaged recipe roots contain no `pack.mcmeta`. Forge 1.20.1 supplied metadata when constructing `Pack.Info`; the 1.21.1 target constructs `Pack.Metadata` directly. `Pack.readMetaAndCreate` would reject these roots and silently omit their recipes. |

### Client rendering and mixins

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/client/model/ModelRenderer.java`; `client/render/CustomRenderType.java` and related renderers | `VertexConsumer`, `BufferBuilder`, shader, GUI-layer, and post-processing APIs changed. The target retains its NeoForge rendering implementation while carrying source behavior. A successful Java compile does not establish a correct frame; client resource reload and representative in-world scenes remain necessary checks. |
| `src/main/resources/assets/flansmodultimate/shaders/core/rigid_model.vsh` | Minecraft 1.21.1 `fog_distance` takes `(vec3 position, int shape)`. The target calls `fog_distance(position, FogShape)` and removed the old unused model-view uniform; the old call failed at client shader reload. |
| `src/main/java/com/flansmodultimate/mixin/PlayerDriveableEdgeMixin.java`; `BufferSourceAccessor.java` | The player sneaking-edge hook moved into `canFallAtLeast`, and `MultiBufferSource.BufferSource` now tracks `startedBuilders`. The target injectors/accessor use those 1.21.1 bytecode locations. |
| `src/main/java/com/flansmodultimate/mixin/DriveableCameraMixin.java`; `SeatedPlayerRendererMixin.java`; `CreateWorldScreenApocalypseMixin.java` | The camera boom constant became a float, player renderer rotations gained a scale parameter, and world creation no longer ticks. The target mixins use the revised constants, method signatures, and screen lifecycle. |
| `src/main/resources/flansmodultimate.mixins.json` | NeoForge 1.21.1 runs official Minecraft names and ModDevGradle does not generate the Forge refmap, so the target omits the stale `refmap` entry. Every mixin class must also appear in the proper common or client roster; source files can compile while unregistered hooks remain inactive. |

### Additional parity adaptations

| Class or location | Source API and destination adaptation |
| --- | --- |
| `src/main/java/com/flansmodultimate/common/item/GunItem.java`; `GrenadeItem.java`; `CustomArmorItem.java` | NeoForge's `ItemStack.getEquipmentSlot()` hook reports an explicit override, not every slot in which an item can be equipped. The target builds attribute modifiers with the appropriate `EquipmentSlotGroup` so guns, grenades, and armor retain their configured effects. |
| `src/main/java/com/flansmodultimate/mixin/LivingEntityDamageMixin.java`; `event/handler/CommonEventHandler.java` | NeoForge `LivingIncomingDamageEvent` runs earlier than Forge `LivingHurtEvent`. The target calls the ported hurt logic from `LivingEntity.actuallyHurt` using the active `DamageContainer`, after shield and cooldown handling. The mixin must remain registered in `flansmodultimate.mixins.json`. |
| `src/main/resources/data/flansmodultimate/enchantment/*.json`; `src/main/java/com/flansmodultimate/EnchantmentItemRepositorySource.java`; `src/main/resources/data/minecraft/tags/enchantment/` | Minecraft 1.21.1 defines enchantments and acquisition through data files and tags. The target supplies Flan item-support tags from registered items and assigns the six enchantments to the appropriate acquisition tags. |
| `src/main/java/com/flansmodultimate/event/handler/ModClientEventHandler.java` (`RegisterMenuScreensEvent`) | NeoForge requires the mecha inventory menu screen to be registered explicitly, alongside the menu type; the target registers `MechaInventoryScreen`. |
| `src/main/java/com/flansmodultimate/event/handler/ClientEventHandler.java` (`ClientTickEvent.Pre`) | The target claims conflicting vanilla key clicks at the start of the NeoForge client tick so driveable input does not also trigger inventory or drop actions. |
| `src/main/java/com/flansmodultimate/event/handler/ModClientEventHandler.java` (`RegisterGuiLayersEvent`) | The target registers flashbang and wounded-screen effects as NeoForge GUI layers above the camera overlays. |
| `src/main/java/com/flansmodultimate/event/handler/ClientEventHandler.java` (world render stage) | The target calls `OpStickConnectionRenderer` after particles to draw the pending world-space connection line. |
| `src/main/java/com/flansmodultimate/common/entity/Driveable.java`; `platform/item/ItemStackData.java` | In Minecraft 1.21, `ItemStackData.copy(sourceStack)` returns detached tag data. After removing serialized driveable state from that copy, the target writes it back with `ItemStackData.set(sourceStack, sourceData)`. |

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
