# Feature Gap Analysis — Wolff's NPCs 1.7.10 → NPC module

- **Current assessment:** 2026-10-24; merges the later temporary audit into this permanent report.
- **Reference:** `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-NPC-Vehicles`, HEAD `19a1d475af1c4729ef43bcb46cd8bc55b631d685`, `ModInfo.VERSION = 8.0.5`, Forge 1.7.10 / CustomNPC+.
- **Target:** `C:/Users/alpha/Documents/Minecraft-Development/Flans-Mod-Ultimate-2.0`, `master`, Forge 1.20.1 / Custom NPCs GBPort. The audit started at `1e1106a36435a96c827e4ad5e40f4d4d78655072`, including the then-uncommitted projectile API and NPC combat/crew changes. The initial combat completion checks used `372d04a73` plus their scoped changes; later follow-up validation is recorded separately below. Unrelated concurrent work is excluded from this thread's commits.
- **Direction:** reference → target. This is source/dependency analysis of this working directory, not a released-JAR audit or the separate 1.21.1 checkout. The current working directory includes the eight implementation changes described below. No interactive game session was performed.
- **Historical metadata correction:** the old report labelled reference `22c0b0e4` as version 8.2.2; `ModInfo.java` at that commit actually says 8.0.5. The prior target snapshot was `ac009a9f1`. Historical fixes and recorded checks are retained below and are not current test results.

**Remaining inventory: 0 MISSING, 2 PARTIAL, 0 UNCERTAIN findings; 8 findings resolved or addressed by an agreed design.** Rapid item-melee hurt-immunity handling and benchmark session controls are fixed in the current working directory. The user also chose optional melee dual wield, native projectile-slot speed overrides retaining exact item defaults, and physical-only return with teleport recovery still enabled by default. The two remaining findings concern deferred content/dependency coverage. The user deferred the model catalog and CustomNPC+ armor keyframes. Target-search overrides are now opt-in, retaining the existing defaults. The working depth limits now reach 64; the earlier temporary report incorrectly treated the broken legacy submarine minimum-depth rule as a working capability.

The remaining table is a decision list, not an implementation backlog approved by the user. Keep the current default unless a listed capability is requested. A parity status answers whether the reference capability can be expressed. Its assessment separately asks whether losing it worsens the intended product. The criteria are combat/configuration consistency, recovery versus physical movement, content/authoring coverage, tuning complexity, and bounded simulation cost. Better on one criterion does not imply better overall. No comparative client/server performance or visual measurements were taken, so performance and handling benefits are qualified rather than asserted.

`R/` paths are relative to REFERENCE and `T/` paths to TARGET. `R/java/` abbreviates `R/src/main/java/com/wolffsmod/`; `T/npcs/` abbreviates `T/src/npcs/java/com/wolffsmod/npcs/`.

The systematic audit covered registered mixins/helpers, entity/model registration, equipment imports and combat, damage, ballistics, ground/air/water movement, targeting/pathfinding, rendering/animation, editor controls, effects/audio, packets, persistence, ranges, and diagnostics. Generated per-model classes were scanned for additional behavior. Definition-driven registration itself is an equivalent implementation; only absent source content is counted in the catalog finding.

## Navigation and movement

### Independent steering and traffic controls — RESOLVED BY AGREED DESIGN

Reference: `R/java/customnpc/VehicleSteeringController.java#steer` and `GroundVehicleController.java#applyLocalSeparation,update`.
The reference smooths turn-rate changes, optionally requests a tracked slow-crawl pivot and independently controls minimum following distance. Its sampled leader-speed cap applies only on every fifth tick unless its close-obstacle flag is set. This is not a reliable continuously applied following-speed policy.

Target: `T/npcs/sim/NpcDriving.java#drive,steer,followLimit,travel`; `NpcTuning`; Driving tabs in `GuiWolffsMod`.
Optional **Steering build-up** defaults to 0 (immediate). Values 1–100 ticks ramp from zero to maximum yaw rate; sign changes unwind previous steering, idle/reset clears it, and current physical yaw/radius bounds apply immediately. **Stationary turning** defaults to allowing tracked turns; Require rolling applies rolling-radius limits to tracked vehicles too and retains enough demand to roll into a turn. Wheeled vehicles still require rolling. Both policies apply in recovery, which now also respects rolling-radius limits rather than rotating a stopped wheeled vehicle. No legacy slow-crawl pivot is copied.

**Traffic brake gap** defaults to 0 (half the following distance). Positive tenths of a block override that threshold, capped at following distance. At/below the threshold traffic requests zero throttle; above it demand rises to full at following distance. Following distance 0 disables traffic checks. The existing width-aware forward cone and persistent gap-based demand remain. A dedicated Traffic tab groups these controls; Steering retains its existing controls plus the two new options. Named settings use existing clamped stats save/load and editor transport.

Assessment: **OPTIONAL AUTHORING CONTROLS ADDED; defaults retained.** Steering inertia and queue spacing are independently expressible. A threshold does not guarantee actual clearance: braking takes time, traffic checks are ten ticks apart and recovery bypasses them. Leader-speed matching is intentionally deferred; a future convoy controller should consider gap, closing speed and braking capability, with runtime testing. The agreed design closes this decision without claiming literal legacy pivot/speed-matching parity or proven traffic safety/performance.

The controls are internal NPC policy, not new public Flan API behavior. Compatible model handling and propulsion integration already present in the working tree is preserved as separate work.

### Water-depth authoring through 64 blocks — RESOLVED

Reference: `R/java/customnpc/SubGuiVehicleMobility.java#addNumber`; `R/java/mixin/MixinDataAI.java#clampGroundDepth,clampWaterDepth`; `R/java/customnpc/TerrainMobilityRules.java#filterPathNode`. The working reference Ground and surface-watercraft depth settings reach 64. The reference's separate submarine safety rule remains excluded because its water-surface calculation is ineffective, as documented below.

Target: `T/npcs/sim/NpcTuning.java#Value.MAX_WADE_DEPTH,MIN_WATER_DEPTH`; `T/npcs/sim/NpcMobility.java#filter,watercraft,hasWaterDepth,depth`. Ground wading now accepts 0–64 and Watercraft minimum depth 1–64. Both retain default 1; the existing settings save/sync path and editor bounds use those same definitions. Submarine running depth and its shallows adjustment are unchanged.

Assessment: **AUTHORING GAP CLOSED; existing defaults retained.** Large-watercraft and map builders can now express the 17–64 range. Watercraft depth still applies under every hull column, and rejects a node as soon as any one column is too shallow. Ground still rejects on the first column exceeding its allowance. No persistent world cache was added, so route checks continue to observe current water.

Cost assessment: scanning stops when the requested threshold is proven. Default settings retain their previous scan limits. At the new maximum, a watercraft column needs at most 64 water reads to prove sufficient depth, or up to 65 reads to reject a finite shallower column including both dry boundaries. Ground needs at most 66 reads per column to distinguish 64 from deeper water. Work also scales with footprint area; increasing the configured depth increases the bounded worst case, rather than affecting every NPC by default. Regression tests count these reads and check early rejection across the hull. These are deterministic operation counts, not an in-game timing benchmark.

API assessment: this is Custom NPCs terrain-policy configuration and filtering; no public Flan API change is needed. Runtime pathfinding validation remains outstanding.

## Models and animation

### Full reference NPC model catalog — PARTIAL

Reference: `R/java/ModEntityRegistry.java#registerEntities`; `R/java/ClientProxy.java#registerRenderers`; `R/java/ContentPacks.java`; for concrete examples, `entity/akaku/EntityMeillerwagen*.java`, `model/akaku/ModelMeillerwagen*.java`, `entity/monolith/EntityMorgan.java`, `model/monolith/ModelADFX02Morgan.java`, and `model/baf/ModelWyvern.java` under `R/java/`.
The reference registers a bundled catalog of named NPC models, with their own entity setup and renderer/model/texture mappings. Registered examples include the lowered/raised Meillerwagen variants, ADFX-02 Morgan, F-15 Galm One, and Westland Wyvern.

Target checked: `T/npcs/model/FlanModelEntities.java#registerEntityTypes`; `T/npcs/model/FlanModelEntityType.java`; `T/npcs/model/FlanModelShape.java`; source definitions and model assets across `T/src/`, including the independent nested pack repositories using searches that bypass ignore rules.
The target generates NPC models from loaded AA-gun and driveable definitions. That supports the system, but does not supply absent content. The named examples above have no corresponding definitions/model assets in the checked target source content, and the NPC module contains no fallback bundled catalog for them. Category metadata mentioning a vehicle is not a loadable content definition/model.

Missing: the remaining reference catalog entries and their variant assets/setups in the supplied target content. This is a content-coverage finding, not a claim that external packs cannot add equivalent models. No missing-model count is inferred from Java class-name differences.

Assessment: **CONTENT COVERAGE LOSS; target registration architecture is better.** The named absent models reduce the available checked-in catalog for users who need those exact vehicles/variants. That is independent of the registration design: deriving NPC models from loaded content definitions avoids duplicating each model's registration/setup and automatically covers newly loaded types. Restoring hundreds of generated NPC-specific classes would increase maintenance without supplying missing meshes, textures, or definitions by itself.

Recommendation: Keep definition-driven registration and add or port requested missing content in the appropriate pack artifact. Treat this as content coverage, not a broken NPC mechanic. An unexamined external/runtime pack might supply an example; the evidence here concerns the supplied source content.

### CustomNPC+ keyframed Flan armor animation — PARTIAL

Reference: `R/java/mixin/MixinModelCustomArmour.java#applyCustomNpcFullModelTransform,applyCustomAnimationPartTransforms,renderAnimatedTurbo,getActiveCustomAnimationFrame`.
The reference explicitly supports CustomNPC+ animation frames: full-model pivot/rotation, interpolated per-limb pivots and rotations, and the matching transformation order for Flan armor. It also reads active player animation frames from `ClientCacheHandler.playerAnimations`.

Target checked: `T/npcs/client/NpcArmorParts.java#apply`; `T/npcs/mixin/NpcEquipmentAnimationMixin.java`; `T/src/main/java/com/flansmodultimate/client/render/layer/CustomArmorLayer.java#renderArmorPiece`; `T/src/main/java/com/flansmod/client/model/ModelCustomArmour.java#render`; `EquipmentAnimationSupport` and the installed Custom NPCs JAR's animation classes.
The target copies ordinary humanoid poses, applies NPC body-part proportions/offsets, and renders armor in the resulting limb transforms. That represents ordinary armor animation. The chosen Custom NPCs dependency supplies its standard animation pipeline, but no CustomNPC+ frame engine; neither the module nor main armor integration supplies the reference frame/player-animation bridge.

Missing: the reference's arbitrary CustomNPC+ keyframe-driven full-model and limb animation compatibility for NPC/player Flan armor. Standard pose copying does not supply the missing keyframe source and playback integration. This does not label ordinary walking, aiming, crawling, or part scaling as missing.

Assessment: **DEPENDENCY CAPABILITY GAP; no demonstrated ordinary-animation regression.** Arbitrary CustomNPC+ full-model/player keyframes are a real creative capability of the reference environment. The selected modern Custom NPCs dependency does not supply that frame engine, so copying the old armor bridge alone would not make the capability available. For ordinary poses, the target copies the final humanoid pose and uses the modern limb transform directly, avoiding much of the reference's special Turbo rotation-order/double-animation handling. That is a sounder integration for the dependency actually installed, but it cannot replace an absent arbitrary-keyframe authoring/playback system.

Recommendation: Keep standard pose copying and part transforms. Add a bridge only for a supported modern keyframe engine if that use case is required; do not classify the absence of a 1.7.10-only frame API as evidence that present walking/aiming armor animations are worse.

## Target search and diagnostics

### Independently configurable target-search distance bands — RESOLVED BY AGREED DESIGN

Reference: `R/java/config/TargetSearchConfig.java#load,intervalForBand`; `R/java/mixin/MixinEntityAIClosestTargetProgressive.java`.
All six search bands have independently adjustable tick intervals, each from 1 to 1200: 0–32, 32–64, 64–128, 128–256, 256–384, and 384 blocks to the combat limit. Nearby acquisition synchronization can also be disabled independently of progressive searching.

Target: `T/npcs/sim/NpcTargetSearch.java#find,interval,due`; `T/npcs/sim/NpcTuning.java`; `T/npcs/combat/NpcWeaponOptions.java`; Performance & debug controls in `GuiWolffsMod.java`.
The default near/far settings, server inheritance, interpolation and shared nearby cadence remain unchanged. **Advanced search intervals**, off by default, enables six independent per-NPC band overrides. Each accepts 1–1200 ticks, or 0 to retain that band's near/far interpolation. Explicit values need not rise with distance; the legacy 5/10/20/40/80/160 schedule can now be expressed. The last target band ends at the existing 512-block limit, and each search remains capped by the NPC's aggro range.

**Stagger nearby searches**, also off by default, disables shared near-band timing independently of advanced intervals. Its first search and later searches use the same per-NPC staggering policy as distant bands. Leaving it off preserves the immediate first nearby search and subsequent shared time slots. All controls require Progressive target search; switching them off retains their saved values. The named settings use the existing validated/clamped Custom NPCs stats persistence and editor transport, without a new packet layout.

Assessment: **OPTIONAL TUNING COVERAGE ADDED; defaults retained.** Independent bands can express nonlinear load/latency policies, while interpolation stays simpler for ordinary use. Staggering can spread simultaneous near-search opportunities, but may delay initial acquisition and no longer aligns formations. Actual scan times also depend on available targets, early acquisition, goal invocation frequency and NPC start times; distinct phases can collide. No live benchmark establishes a performance improvement.

Agreed design: keep existing defaults and expose advanced intervals plus the opt-in staggering toggle. These are internal Custom NPCs scheduling controls, with no new public Flan API dependency or version bump. The separate NeoForge checkout was not changed.

## Resolved in the current working directory

### Optional physical return home — RESOLVED BY AGREED DESIGN

Reference: `R/java/mixin/MixinEntityAIReturnNoTeleport.java#wolffsmod$replaceAutomaticTeleport,wolffsmod$retryReturnPath`; registration in `R/src/main/resources/wolffsmod.mixins.json`.
Both automatic return-home teleports are intercepted and replaced with another navigation attempt. NPCs must physically return even after the return goal becomes stuck or times out.

Target: `T/npcs/mixin/EntityAIReturnMixin.java`; `T/npcs/sim/NpcReturnHome.java`; `NpcWeaponOptions.Feature.RETURN_HOME_TELEPORT`; Pathfinding controls in `GuiWolffsMod.java`.

**Allow return-home teleport** defaults to Yes, retaining the dependency’s existing recovery. No replaces the return goal’s tick logic, covering both automatic teleport branches, with physical navigation and a shared 40-game-tick retry budget that survives goal restarts. An active path is retained; completed or failed paths alternate the dependency’s direct/intermediate searches. Existing destination, combat interruption and eligibility rules remain. Unreachable homes may leave NPCs stranded. Scripted teleports are outside this policy.

Assessment: **OPTIONAL CAPABILITY ADDED; recovery default retained.** Builders can enforce physical movement without forcing that policy on old saves. Bounded retries avoid an every-tick failure loop, while preserving the dependency’s eventual teleport recovery when allowed. This is NPC-specific goal policy and requires no public API addition.

Validation: see the current validation section below; interactive blocked-route and editor checks remain outstanding.

### Native projectile-slot launch-speed override with exact item imports — RESOLVED

Reference: `R/java/mixin/MixinEntityNPCInterface.java#shootProjectile,shootThrowable`; `R/java/customnpc/NPCInterfaceUtil.java#setThrowableHeading`. Legacy native potions, experience bottles and eggs use the NPC's ranged speed, allowing custom trajectories.

Target: `T/npcs/combat/NpcThrowables.java#flight,defaultSpeed,ownSpeed`; `NpcCombatSettings.java`; `EquipmentProperty.java#NATIVE_THROW_SPEED`; `EquipmentValues.java`; `NpcEquipmentImport.java`; `T/npcs/client/CombatPages.java`, `EquipmentReadouts.java`, and the Projectiles page link in `GuiWolffsMod.java`.

**Projectiles > Projectile slot properties > Launch speed (blocks/tick)** now supports Inherit/Custom and Import. Inherit remains the default. A first Custom selection initializes from the current item's unmodified default; subsequent toggling retains an edited value. Import always copies the item's default, never the active override. Defaults remain 0.75 for potions, 0.7 for experience bottles, 1.5 for eggs and 1.6 for firework rockets. A separate decimal value preserves fractional tenths such as 0.75, which the existing integer Custom NPCs ranged-speed field cannot represent exactly.

The effective speed is shared by ballistic aiming and actual launch, with existing native effects, accuracy, shot count and firing behavior retained. It is independent of held-item overrides and Use equipped weapons. Powered fire charges keep their original flight and expose no invented launch-speed default/import. Unsupported items and disabled Native throwables keep the ordinary Custom NPCs path. The new value and mode use existing stats persistence/synchronization; old saves still inherit their item speeds.

Assessment: **AUTHORING GAP CLOSED; predictable defaults retained.** Builders can tune native throws without coupling them to generic ranged speed, changing other held weapons, or losing real item effects. Exact default import is preserved instead of rounding a potion's 0.75 to 0.8. Custom speeds are bounded to 0.01–10 blocks/tick; malformed values fall back to the item default.

Validation: focused tests cover all five adjustable item defaults, inheritance/override transitions, exact import while overridden, NBT round trips, bounds/invalid values/old saves, powered and unsupported items, disabled-feature fallback, and the actual launched potion's direction and speed for both inherited and custom settings. Editor-control tests also check the effective speed readout, its lock explanation, and preservation of the generic NPC speed. Runtime editor and trajectory verification remains outstanding.

API assessment: native vanilla-item physics selection and Custom NPCs settings/imports belong inside the NPC module. No main-mod API or packet-schema change was needed.

### Optional melee dual wield and off-hand feedback — RESOLVED BY AGREED DESIGN

Reference: `R/java/mixin/MixinDataInventory.java#setMeleeStats`; `R/java/mixin/MixinEntityNPCInterface.java#animateFlanGunMelee`. The reference combines damage/knockback/timing from either hand into one hit and animates both held Flan guns; it does not implement two independent melee attacks.

Target: `T/npcs/combat/NpcEquipment.java#meleeHand,meleeWeapon`; `NpcMeleeState.java`; `NpcItemAttacks.java#melee,meleeFeedback`; `T/npcs/mixin/NpcEquipmentAttackAiMixin.java`; `EntityNPCInterfaceMixin.java`; `T/npcs/platform/NpcPlatform.java#meleePostDamageEffects`.

The user's chosen replacement is **Melee dual wield**, default **No**, on the Equipment page. With Use equipped weapons enabled, usable melee weapons alternate one strike at a time. The existing AI chooses that weapon's reach and full recovery before its swing/hit, retaining one cooldown rather than independent attack channels. Damage, enchantment knockback/fire, attacking enchantment hooks, durability and gun melee sound/animation belong to the striking weapon. Armor enchantment callbacks still run; the idle weapon's attacking callbacks are excluded. Break feedback follows the hand whose item broke, without moving inventory stacks.

Empty hands, shields and non-weapons are skipped. One usable weapon attacks alone, including an off-hand-only weapon; neither weapon means ordinary NPC melee. The Main hand properties > Melee Inherit/Custom controls apply to both hands, with shared NPC Custom values. The property page compares the two weapon readouts. Copy/Import intentionally still copies the main hand, and ranged dual wield remains a separate setting.

Assessment: **OPTIONAL CAPABILITY ADDED; main-hand default preserved.** This supplies coherent off-hand melee participation and feedback without constructing a synthetic strike from both weapons' strongest properties. It adds no attack-speed multiplier: a slower weapon slows its part of the sequence. Rejected/script-cancelled attempts still consume their turn and recovery. The old maximum-of-both-hands import/aggregation and simultaneous two-hand animation remain deliberately unimplemented, as the user requested a recommended dual-wield design rather than strict 1.7.10 parity.

Validation: focused regression tests cover old-save defaults and persistence, alternating damage/recovery, weapon eligibility (including Flan shields), off-hand-only attacks, shared Custom values, rejected/cancelled turns, selected-hand enchantments/durability/feedback, and correct melee AI selection with projectile-slot ammunition. A fast-to-slow follow-up test reproduced the immunity problem before its fix: an accepted fast hit can now be followed on the same victim after its full recovery and before ten ticks, without clearing immunity for a different victim or a premature attempt. Dependency bytecode checks confirm one delay read, swing and damage call, and the generated refmap maps the swing redirect to the production method. Runtime visual verification remains outstanding.

API assessment: hand selection/alternation belongs to NPC policy; the enchantment helper is a vanilla version boundary. The existing public `FlansEquipment` APIs already accept the striking hand for animation/sound, so no main-mod API addition was needed.

### Rapid melee attacks bypassing target hurt immunity — RESOLVED

Reference: `R/java/mixin/MixinEntityNPCInterface.java#attackEntityAsMob`.

Target: `T/npcs/combat/NpcItemAttacks.java#hurtMelee`; regression tests in `T/src/test/java/com/wolffsmod/npcs/combat/NpcMeleeTest.java`.

The native item-melee path now follows Custom NPCs' existing rapid-hit policy when the effective attack delay is below ten ticks. It uses the weapon's delay only when that statistic is inherited; otherwise it uses the NPC's own delay. The reset happens after the script event accepts the attack. A rejected hit restores the previous immunity, while an accepted hit retains the new immunity established by the damage engine. Ordinary-speed attacks are unaffected.

Validation: the new tests failed on the original fast-item/fast-editor cases and passed after the fix. They cover the ten-tick boundary, inherited versus own cadence, a rejected strike, and script cancellation. These verify the damage-call contract; an interactive damage-rate comparison has not been run.

API assessment: this is NPC-specific attack-policy handling using vanilla entity state. No main-mod API addition or change was needed.

### Benchmark session controls and annotations — RESOLVED

Reference: `R/java/benchmark/BenchmarkCommand.java`, `ServerBenchmarkCommand.java`.

Target: `T/npcs/diag/BenchmarkOptions.java`, `BenchmarkReport.java`, `ServerBenchmark.java`, `NpcBenchmarkCommands.java`; `T/npcs/client/ClientBenchmark.java`.

Both commands accept `[seconds] [warmup] [label]`, with warm-up 0–60 seconds and a bounded, single-line label; naming a run requires the warm-up argument first. Existing duration-only commands and editor actions retain their default warm-ups. Both have `status` and `stop`; the client also has `mark <label>`. Labels/settings/markers appear in the text report, safe label characters are included in filenames, and the raw CSV columns are preserved. Server controls remain operator-only.

Client chat now pauses measurement and its clock instead of aborting the run, so status/stop/mark are usable. Chat frames and the transition back to the world are excluded. Other screens and pausing retain their existing abort behavior after warm-up. A zero-warm-up server run resets pre-existing profiling counters before its first sample.

Validation: focused tests cover command defaults/arguments/permissions, invalid warm-ups, label bounds and filename safety, annotation ordering, a zero-warm-up run excluding earlier profiling samples, and client chat/control/sample lifecycle. In-game timing and output-file acceptance still need a client/server session.

API assessment: benchmark state and commands remain internal to the NPC module. No main-mod API, packet schema, or cross-loader boundary was added. English/German tooltips and the existing wiki benchmark section document the controls.

## Earlier audits: resolved findings

The first audit (reference `7999bb16`, version 7.1.0) recorded 35 findings; the second (reference `22c0b0e4`) recorded nine; the third (2026-10-12) recorded seven PARTIAL findings. The earlier report records those findings as closed or not applicable, with background in the wiki's `NPC-Vehicles-and-Soldiers.md`. The seven fixes below are retained as historical results.

| Subsystem | Feature | What the target does now |
| --------- | ------- | ------------------------ |
| Movement: ground driving | Braking for the end of the path | `sim/NpcDriving.arrival`: the throttle is limited to what braking can still shed before the point where the path ends (the stop distance, or the navigator's own reach for a wide NPC), down to a crawl of 20 %. A vehicle that was slowing for its goal brakes when it gets there. |
| Movement: ground driving | Turning circle of a wheeled vehicle | The turn limit is given the distance the NPC actually moved (`NpcVehicleState.getTravel`), not its motion after ground friction, so the circle has the radius that is set. |
| Movement: terrain profiles | Water depth under the whole footprint | `sim/NpcMobility.depth` measures every column of the footprint the path search uses: the deepest for Ground, the shallowest for Watercraft. |
| Movement: submarines | Following a path at running depth | `mixin/NpcGroundPathNavigatorMixin.wolffsmodnpcsPassBelowNodes`: a submerged submarine reaches a path node by passing below it. |
| Combat: aircraft | Holding fire below the attack altitude | The hold also applies in `combat/NpcEquipmentRangedGoal`, and at `performRangedAttack` and `doHurtTarget` in `mixin/EntityNPCInterfaceMixin`, which covers scripts and melee. |
| Diagnostics | Path searches of flying NPCs in the server benchmark | `mixin/PathNavigationMixin` times the search itself in every navigator an NPC uses, and failed searches are counted (`path_failures` in the CSV). A call that keeps its path is no longer counted as a search. |
| Rendering | Large NPC without a content-pack model and without a hitbox | `sim/NpcModels.unhitCullingBox`: such an NPC is culled by the width its hitbox would have. |

The following historical decisions and validation belong to the 2026-10-12 audit; they are not claims that the current checkout was runtime-tested again.

---

## Earlier audits: accepted design differences

The earlier report counted no open gaps at its then-audited snapshot. Its deliberate design decisions are preserved below. The current findings reassess their practical costs rather than automatically treating every lost control as a defect.

Not applicable to the target:

- **Per-pack model toggles** (`ContentPacks.loadPacksConfig`): Flan's Mod Ultimate 2 already switches whole content packs on and off, and only loaded packs contribute NPC models.
- **Guard for modern TAP attachment rendering** (8.0.1): fixes a 1.7.10 content-pack incompatibility.
- **Stat import for Custom NPCs' own weapons:** Custom NPCs 1.20.1 ships no weapon items.
- **Block-read cache for path searches:** Minecraft 1.20.1 already caches block reads within a search.

Previously accepted differences (several now have a more detailed capability/tradeoff assessment above):

- Every per-NPC setting lives in the Wolff's Mod tab, not in the Custom NPCs AI, display and stats pages.
- Frustum-culling exemptions are replaced by culling against the whole model's bounds.
- The ground driving controller is on by default and has fewer values than the reference's: steering rate, maximum steering angle and maximum turn rate are one hull turn rate, and the minimum following distance is half the following distance.
- A watercraft becomes a submarine by its running depth rather than by a separate type, and its minimum water depth is the Watercraft profile's.
- The path debug preview works for any NPC, not only for the driving controller.
- Server-wide target-search intervals are saved with the world and edited on the tab's Server page, not in a config file. Only the near and the far interval are set; the bands in between scale.
- Benchmarks time NPC ticks, path searches, target searches and NPC drawing as wholes. The reference's per-phase render and cache timings are not reproduced, because the target's render path is shared with real driveables and has its own diagnostics.
- A vehicle whose path is taken away while it is at speed, for instance by the ranged AI once its target is in range, rolls out at its coasting rate. The reference brakes there, because its coasting rate is never reached; the target has coasting as a setting of its own. Only a vehicle that was already slowing for the end of its path brakes.
- A submarine uses the Watercraft profile's minimum depth (now configurable through 64), instead of a second submarine-specific minimum. It rises over shallows relative to its running depth. This improves recoverability, but a stricter deep-water routing requirement would still be a legitimate optional scenario feature.

## Earlier audits: validation recorded on 2026-10-12

These were on the deeper-audit list and need no further work, short of the client checks listed below.

- **Terrain profile mixin.** It applies. The dev-client log of 2026-10-12 02:49 (`run/logs/latest.log`) shows `NpcWalkNodeEvaluatorMixin` mixed into `noppes.npcs.ai.NpcWalkNodeEvaluator` when the world started, with no injection error, and the transformed class was exported to `run/.mixin.out`. The production name it targets, `m_77567_`, is declared by that class in the Custom NPCs jar. The same log shows 18 of the module's 20 mixins applied; `NpcArmorControlsMixin` and `NpcRangedControlsMixin` were not loaded because their screens were not opened.
- **Watercraft footprint.** Shores were handled by the path search itself; depths beyond 1 are measured under the whole footprint since the fix above.
- **Server page permissions.** `network/ServerControlPacket.handle` answers a read for anyone, and requires permission level 2 or the singleplayer owner for a change or a benchmark; a refused player gets the current values back. The packet is handled on the server thread. Not exercised on a dedicated server.
- **Benchmark report location.** `diag/BenchmarkReport.write` writes a `.txt` and a `.csv` to `logs/wolffsmodnpcs-benchmarks/` under the game directory. No benchmark has been run; that folder does not exist yet in `run/`.
- **Large NPCs.** The hitbox grows with the NPC's size in both `m_6972_` implementations, so the remaining case was the NPC without a hitbox, closed above.
- **Flight arithmetic.** Climb and descent limits, the attack-altitude hysteresis, the loiter circle, the plane's turn limit and the helicopter's turn rate match the reference value for value. Flying NPCs have no gravity in Custom NPCs 1.20.1 (`EntityNPCFlying.m_7023_`), so an idle helicopter's vertical speed decays to a hover.
- **Unit tests.** `com.wolffsmod.npcs.sim.*` and `com.wolffsmod.npcs.diag.*` pass at `ac009a9f1`.

### Reference implementation caveats

The five commits without release notes (`5efc6bdc`, `71af6e1f`, `7bdf6798`, `0e4fda39`, `22c0b0e4`) were read in full, including their editor screens and saved values. Their earlier assessment is retained here; the current comparison also covers the subsequent `19a1d475` change. Four things in them do not work as their names suggest, which matters when a target vehicle is compared with a reference one:

- **Submarines do not dive or path in the reference.** `SubmarineController` and `TerrainMobilityRules.hasWaterColumnDepth` find the water surface with `World.getTopSolidOrLiquidBlock`. In 1.7.10 that method skips liquids and returns the block above the sea floor, so the block below it is never water: the controller never engages and every path node of a submarine is rejected. The target follows what the feature was meant to do, so there is no working reference to compare it with.
- **Reference aircraft measure their height from the sea floor** over water, for the same reason, and are pushed down when more than 5 blocks above the height they aim for. The target measures from the water surface, and counts leaves as terrain.
- **The reference never coasts.** `advanceSpeed` picks the coasting rate only when the wanted and the current speed are both zero.
- **A reference plane without a target circles its home only while the NPC is set to wander.** Otherwise it flies straight on. A target plane always circles.

Commit `22c0b0e4` also sends the hull direction to clients while a vehicle moves, not only while it has a target. The target sends it whenever it changes.

## Earlier combat and benchmark validation

- Focused melee and benchmark tests passed after the reproduced melee failures were fixed. Dual-melee coverage also reproduced and fixed the mixed fast-to-slow immunity failure. Native throw-speed coverage passes all 19 combat cases and both editor-readout cases.
- Java 17 / Forge 1.20.1: the final `gradlew build sonarlintNpcs sonarlintTest spotlessCheck --offline` passed. The full suite reports 2,183 tests: 2,173 passed, 10 skipped, zero failures/errors. Both SonarLint reports contain zero issues. Scoped `spotlessApply` and repository/wiki `git diff --check` passed.
- The full build produced the NPC, main, pack-manager, and official-pack artifacts. The NPC JAR contains the benchmark controls, new melee-state classes, native throw-speed implementation, and updated English/German locales. Its swing refmap and item-break override have the expected production names.
- Headless IntelliJ was run once for the dual-melee task and once for the throwable-speed task. The latest run reported the same 20 pre-existing warnings plus six nullable-flight dereferences in the new test. Those six now use a helper that asserts a flight exists before reading its speed. The remaining warnings concern existing API/projectile, editor, bow-enchantment, crew, platform-helper, readout and benchmark-test code. IntelliJ was not repeated after that correction, following the once-per-task rule.
- No interactive client/dedicated-server session was run. Actual damage cadence, dual-melee visuals/editor layout/synchronization, native throw trajectories and editor controls, benchmark timing, and written reports still require runtime checks.
- Existing unrelated source/configuration and wiki changes are preserved. Only the NPC combat/diagnostics implementation, its regression tests/localization, this report, and the relevant existing wiki explanations are in scope.
- The 1.7.10 water-surface caveat and the prior audit history remain as documented. No content catalog, steering, search-schedule, water-depth bounds, or animation-engine policy was changed. Physical-only return is now optional with teleport recovery enabled by default. Off-hand melee is now an explicit opt-in policy, documented above.

## Physical-return follow-up validation

- Focused retry-budget, option default/save/reload and installed-dependency hook tests pass. The dependency contract confirms both automatic teleports occur in the replaced tick method and verifies the existing navigation/start/eligibility hooks.
- `gradlew build sonarlintNpcs sonarlintTest -x spotlessJavaCheck --offline` completed. The full suite reported 2,241 tests: 2,231 passed, 10 skipped, zero failures/errors. Final eligibility-hook compilation and focused tests were repeated with `npcsJar` after changing the cooldown gate to run after the dependency's eligibility calculation, preserving its bookkeeping.
- The unrestricted formatting gate was blocked by concurrent, unrelated `drivers/NpcPilot.java` formatting. All return-home files passed scoped `spotlessApply`; repository and wiki diffs pass `git diff --check`. JSON/misc formatting checks passed during the build.
- SonarLint reports no issues in the return-home implementation or tests. The shared NPC report contains five unrelated findings: one existing `GuiWolffsMod.navigate` complexity finding and four in concurrent `NpcPilot` landing work; the test report is empty.
- One headless IntelliJ run reported 45 warnings: 44 in existing or unrelated code and one false positive on the shadowed timeout field. A narrow `FieldCanBeLocal` suppression now explains why that field resets the target's continuation timeout. No inspection rerun was performed.
- The NPC artifact contains the registered common-side mixin, retry state and both translated labels/tooltips. This is internal Custom NPCs navigation policy; no public Flan API change is needed. No interactive blocked-route, editor or dedicated-server session was run.

## Vehicle-model handling follow-up validation

- The revised implementation inherits only compatible definition limits: absolute yaw rate, derived wheeled rolling radius and real reverse/forward speed ratio or reverse prohibition. Existing positive saved values remain Custom. Missing/inactive data and disabling Use model properties retain the documented simulation fallbacks. No engine-dependent acceleration/braking values are copied into the speed-fraction controller.
- Regression coverage checks API inspection for wheeled/tracked/legacy/pushed/unsupported types, active speed scaling and force-legacy configuration, runtime feature gating, fractional values reaching actual steering, inherited versus custom radius behavior, reverse prohibition, saved overrides, and editor readout/mode transitions. The editor-copy test reproduced a floating-point half-rounding failure before its correction.
- `gradlew build apiJavadoc sonarlintMain sonarlintNpcs sonarlintTest spotlessCheck --offline` passed. The full suite reports 2,271 tests: 2,261 passed, 10 skipped, zero failures/errors. Main/test SonarLint reports are empty; the NPC report contains only the pre-existing `GuiWolffsMod.navigate` complexity finding (16 versus 15). Scoped formatting and repository/wiki diff checks passed.
- One headless IntelliJ run reported 49 warnings, mainly existing unused/analysis findings in type inspection and the editor. The new nullable-reference warning and redundant test import were corrected, the new method's false unused report was narrowly suppressed, and its extraction suggestion was addressed. IntelliJ was not repeated after these corrections.
- API 0.26 adds `GroundHandling` and `FlansEntityTypes.getGroundHandling`; runtime policy and Inherit/Custom controls remain in the NPC module. The API and NPC JARs contain the new contract, editor controls and both translated help texts. No API release/publication was performed. This change is on Forge 1.20.1; the separate NeoForge branch was not edited.
- No interactive editor, convoy, steering or dedicated-server acceptance session was run. Independent steering response, tracked tight-turn permission and traffic stop-gap/leader-speed policies remain design decisions; this inheritance follow-up does not claim to close that entire partial finding.

## Water-depth follow-up validation

- All 15 new depth regression cases pass. They exercise the actual terrain filter at 16/17 and 63/64/65 boundaries, whole-hull rejection, old defaults, save/reload and bounded values, open-air handling, and deterministic fluid-read budgets. Existing simulation-settings and flight/submarine tests also pass.
- The full `gradlew build sonarlintNpcs sonarlintTest spotlessCheck --offline` completed with 2,286 tests: 2,276 passed, 10 skipped, zero failures/errors. NPC/test SonarLint reports are empty. Scoped `spotlessApply` and main/wiki diff checks passed.
- One headless IntelliJ run found only the obsolete `deepest` flag, now always true after watercraft moved to its early-rejection check. The flag and unused shallowest branch were removed; the final helper was rebuilt and regression-tested. No inspection rerun was performed.
- API assessment: settings and footprint filtering are internal Custom NPCs terrain policy; no public API addition or version bump was needed. No content-pack definition, submarine running-depth behavior or default terrain value was changed.
- Cost validation counts fluid reads, rather than timing a running game. A two-by-two watercraft footprint over sufficiently deep water uses 256 reads at minimum depth 64; a shallow first column rejects after at most 65 reads and skips the other columns. Ground checks at allowance 64 stop after 65 reads on a too-deep first column, or at most 264 reads when all four columns are exactly 64 deep. Larger footprints multiply the bounded work by their area. Live pathfinding/convoy performance and editor acceptance remain unverified.

## Target-search follow-up validation

- Eight new regression tests exercise observed search times for all six default bands, the nonlinear legacy schedule and non-monotonic overrides, 1200-tick intervals, inherited bands, shared versus staggered near timing, mode changes, aggro-range caps, early acquisition and Progressive target search gating. NBT transport coverage checks retained options, old defaults, malformed values and bounds. The existing default-options test now explicitly expects both new opt-in switches to start disabled.
- The full build completed with 2,294 tests: 2,284 passed, 10 skipped, zero failures/errors. After extracting schedule initialization to resolve the new complexity finding, focused and full tests passed again and the NPC JAR was rebuilt/reobfuscated. Final NPC SonarLint contains only the pre-existing `GuiWolffsMod.navigate` complexity finding (16 versus 15); test SonarLint is empty. Scoped formatting and main/wiki diff checks passed.
- One headless IntelliJ run reported four warnings. Its unused test import was removed. The three retained pre-existing warnings concern the editor's large `Map.ofEntries` call, an existing Unicode escape and the default target-priority callback's constant return. No inspection rerun was performed.
- API assessment: the schedule and editor controls are internal Custom NPCs policy; no public Flan API addition or version bump is needed. Runtime and editor controls and both English/German locale sets were verified in the NPC JAR. The existing stats save/load mixin handles named settings, and the dependency's stats transport retains its existing NPC/tool/permission checks; no packet layout was added or changed.
- No interactive editor, dedicated-server acceptance or live target-search timing benchmark was run. Unit scan counts/timestamps establish scheduling behavior, rather than a measured server-performance gain. Models and CustomNPC+ armor animation remain deferred by the user; independent driving-policy choices remain open.

## Steering and traffic follow-up validation

- Six new policy tests cover unchanged defaults, turn-rate build-up and reversal, idle/reset, immediate rolling-limit enforcement with residual steering, wheeled stationary-turn rejection, target/yaw-wrap bounds, custom/disabled traffic curves, and bounded save/reload. All existing driving tests pass too.
- The shared checkout initially could not compile because unrelated command-centre sources were incomplete. The feature was isolated on committed base `a23fce98d`, preserving ongoing handling/propulsion, editor, team and naming changes. In that checkout, `gradlew build sonarlintNpcs sonarlintTest spotlessCheck --offline` passed: 2,363 tests, 2,353 passed, 10 skipped, zero failures/errors. NPC and test SonarLint reports are empty; the one IntelliJ inspection run reported no problems. Scoped formatting and diff checks passed. The isolated full build covers tracked pack inputs; independent nested pack repositories were not copied or changed.
- The NPC JAR contains the controls and both locale sets. No new packet layout or public API is needed: this is internal NPC driving policy using existing stats save/load and editor transport. In-game steering feel, convoy braking, recovery interactions, editor layout and dedicated-server acceptance remain unverified. No collision-safety or performance improvement is claimed.

## Remaining decisions

| Subsystem | Feature | Status | Confidence | Practical assessment |
| --------- | ------- | ------ | ---------- | -------------------- |
| Content | Full reference model catalog | PARTIAL | HIGH | Deferred by user; retain data-driven registration |
| Animation | CustomNPC+ armor keyframes | PARTIAL | HIGH | Deferred by user; standard pose path remains appropriate |

## Areas requiring deeper audit

The earlier checklist below is retained as historical outstanding runtime coverage. The current audit has not resolved it by launching a client.

- **Rapid melee acceptance:** unit coverage now verifies the fix at the damage-call boundary. In game, compare accepted hits as well as swings for native item and editor-controlled melee, including a second attacker.
- **Dual melee presentation:** enable the option, save/reload the NPC, and check mixed-speed weapons, an off-hand-only weapon, shields, left-handed NPCs, item breaks, and hand-specific gun sounds/animations. Confirm that the editor shows both weapon readouts and that clients see the same attacking hand as the server.
- **Native throw-speed controls:** check Inherit, first Custom selection, edited values, Import while overridden, save/reload and client synchronization for each supported item. Compare trajectories and the standard projectile-page readout; fire charges should retain powered flight.
- **Recovery policy:** block the return-home route and compare eventual return, teleport visibility, and repeated path-search cost. Verify the physical-only switch prevents both recovery teleports, retains an active path, and bounds failed searches across goal restarts.
- **Shared models:** visually compare scale, origin, textures, muzzle/rider positions, and animations for entries available in both source catalogs. Missing source assets and misaligned available assets are different issues.
- **Controlled comparisons:** measure steering/convoy behavior and per-band target-search cost at the same NPC count, world, and targets. More configuration controls alone do not establish better handling or performance.

All of these need a running client; none could be done in this audit.

- **In-game verification:** nothing listed in the wiki's Vehicle simulation and Movement and senses sections has been checked on screen. Still to check: the editor pages and their paging, including the armour and ranged pages whose mixins no logged run has loaded; the direction of every rotation on real models; wheel, track, propeller and leg motion; mecha arm pitch; sight and fire through transparent blocks; and combat behaviour.
- **Driving controller on real terrain:** compare a target vehicle with a reference one on straight roads, 90° turns, S-curves, dead ends and one-block steps, for each preset. Look first at the halt at the end of a path and at the turning circle of a wheeled vehicle: both were corrected by arithmetic and unit tests, not measured in game, and the turning circle is now about 1.8 times tighter than before. The target also steers for a fixed distance ahead, where the reference shortens it at low speed and lengthens it at high speed. Two vehicles that meet head-on stop for each other and stay stopped, in the reference as well.
- **Flight types:** watch a plane's attack runs and its loiter circle, and a helicopter's hover. Custom NPCs' flying movement pushes the NPC toward its own waypoint every fifth tick, which the plane's heading overrides sideways but not vertically. A plane given a set flight speed still looks only 24 blocks ahead for terrain: at 20 blocks per second it climbs about 6 blocks in that distance, on top of the 5 blocks it keeps above its attack altitude.
- **Submarines:** confirm that a submarine now travels its paths at depths 1, 2 and 4 and deeper, and check depth keeping over a sloping seabed. With swimming allowed, Custom NPCs' swim goal pushes the NPC up each tick, which by arithmetic leaves it up to half a block above its running depth.
- **Terrain profiles of wide NPCs:** a Watercraft now needs its minimum depth under its whole hull, so check that a wide ship still finds its way through the channels it used before.
- **Benchmarks:** run both once on a busy scene and confirm that the reports appear and that the NPC shares are plausible. The client benchmark counts shadow passes of shader packs as draws.
- **Server page on a dedicated server:** one pass as a non-operator, to see the refusal message and the unchanged values.
