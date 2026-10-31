# Flan's Mod Ultimate 2.2

## Highlights

* New companion mod: **Flan's Ultimate 2: NPC Vehicles & Soldiers**, which lets Custom NPCs soldiers use Flan's weapons, vehicles, planes, ships and mechas, crew them and fight Teams rounds, skirmishes and wars (separate download, requires Custom NPCs)
* Driveable damage and destruction: damaged parts smoke, burn, steam and spark, each machine class dies its own way (tank cook-off, ship magazines and boilers, aircraft fuel, rotor and wing loss, mecha collapse), and parts shot off fly away; destruction bursts stay bounded, ship magazine blasts scale with ammunition rather than hull size, and machine-gun-only boats produce small cook-off pops
* Destroyed driveables stay as burning wrecks that settle or sink, keep their collision, burn out and can be blown apart (`driveableWreckLifetime`, `NoWreck`)
* Driveable repairs and carrier servicing, with repair tools, the Blowtorch, a repair progress display and an operator command to fully repair a driveable
* New Teams command centre for operators (F7, `/teams admin` or `/teams gui`) covering rounds, rotation, maps, rules, game type variables and server settings
* Reimplemented missing features of Flan's Mod Ultimate 1.7.10, including Teams rule commands, lag compensation settings, death drop rules, item spawners, `ShootWithOpenDoor`, kill messages with distance and hit markers
* New combat effects: screen shake and camera recoil, tracer beams and mixed belts, hold-to-throw grenades with weight-based category speeds and weaker underhand tosses (hold and release attack for overhand, use for underhand), thermal vision scopes, hot rounds and team IFF strobes, explosion shake, fly-by sounds and suppression
* Large rendering performance improvements and shader pack compatibility (Oculus/Iris)
* Faster content loading and startup

## Teams

* `/teams regeneration <true|false>` controls passive healing for players and bots during rounds; the default is now half a heart every five seconds, preserving the value of medical supplies
* `/teams stop` stops the current round, with the same cleanup as `/teams admin stop`
* Kill messages and the kill feed name the weapon, grenade or vehicle responsible
* Holding a healing tool in either hand shows the health of the living target within five blocks, including players and Flan soldiers, inside and outside rounds; vehicle models are excluded

* `showBotKillsInKillFeed` controls whether the battle kill feed includes bot-versus-bot kills, also available in quick access Server settings; disable it to show only kills involving a player

* Unassigned flagpole bases and capture points default to the first map in their dimension when placed or loaded; the Operator Stick can still change their map
* Teams bases and capture points have configurable pole heights (`defaultFlagpoleHeight`, now 5 blocks instead of 3), editable separately with the ownership stick
* Domination and Conquest rounds can be played attackers against defenders
* Multi-team alliances
* Team flags fly on Capture the Flag flags, base allegiance banners and team selection buttons; flags can also be loaded from an http(s) PNG link (`loadTeamFlagsFromUrls` turns this off)
* Base allegiance banners linked to Teams bases
* Deathmatch names a single MVP and spawns players at any team's spawns
* Restored 1.7.10 rule commands (hunger, bombs, shells, bullets, block breaking, armour and weapon drops, despawn timers), `useRotation`, `autobalance`, `setRound`, `goToMap`, `getSticks`/`getOpSticks` and `ping`
* `scorelimit` works for every game type, and game type variables are saved after `setvariable`
* Lag compensation settings `bulletSnapshotMin` and `bulletSnapshotDivisor` (`/teams bltss`, `/teams showbltss`)
* Spectators cannot pick up items during a round; item spawner items float above their plate and only the owning team can take them
* Configurable Teams HUD control reminders
* The Teams key (U, now `Teams Menu`) opens a menu with team selection, class selection and the scoreboard, plus a Teams Command Centre shortcut for players with operator command permissions; the scoreboard and class keys are now unbound by default, freeing I and O (saved key settings are kept)
* Team selection opens on login only while a playable round is live
* Class selection tooltips show the full class name above its requirements
* A finished round opens with a Victory, Defeat or Draw banner showing the winners' flag, each side's final score and your own score, kills and deaths, before the scoreboard
* In Conquest and Assault the death message shows for a moment before the deployment map fades in, safely handling deaths without a message, and the map shows the real terrain around the capture points and headquarters unless the Teams map has a deployment image

## Vehicles, Aircraft & Ships

* Ships are now separated from ground vehicles, with improved ship physics; leaving a ship puts the player on top of its hull
* Creative players can pick up unoccupied floating ships and airborne driveables by hitting them
* Vehicle cannon screen shake
* Depth charges, HESH rounds, tracer rounds and composite armour
* Suitable ground vehicle categories squash mobs they run over
* Configurable driveable melee protection and weapon damage overrides
* Configurable render and tracking distances for driveables and Flan NPC models
* Smooth view and position for players standing or walking on moving hulls and turrets, including large ships; fixed sudden turns and drops from unstable carry transforms and replayed movement when server time is synchronized
* Passenger guns mounted on a turret aim relative to the turret
* The Change Seat key now defaults to Numpad Enter instead of Numpad 0, which Custom NPCs uses to reset scenes (saved key settings are kept)
* Both mecha guns fire along the mecha's aim from the correct muzzle positions
* Fixed the hitboxes of planes and helicopters flown by Flan's NPCs facing backwards, and pieces thrown off a wreck coming to rest in the air instead of on the ground
* Fixed animated tracks, driveable collisions against walls and ThrownGun gravity
* Pointing a driveable repair tool (such as the blow torch) at a part of a driveable or Flan's NPC shows its remaining and total HP; debug mode shows it above the armor readout

## Weapons

* Fixed automatic weapon firing animations and muzzle flashes continuing after death and respawn without firing
* Built-in gun categories balance recoil by cartridge, weapon weight, action and mounting, with consistent recovery and reduced recoil when crouching or prone
* `gunRecoilModifier` now also scales `FancyRecoil`, so the global setting works with both gun recoil systems
* Default ammunition selection prefers compatible rounds from the weapon's own content pack, including for `/defaultammo`
* Historically supported firing modes for source-pack weapons, including semi-auto selectors and two- or three-round bursts
* Variable zoom for modern scopes (binoculars up to 40x), with new default keybinds
* Lie prone with the new Go Prone key (Z): lying still narrows a gun's spread by its new `ProneSpreadModifier` (default: the gun's `SneakSpreadModifier` squared), the ground gives stronger recoil control than sneaking (`CounterRecoilForceProne`, default: `CounterRecoilForceSneaking` squared; attachments `RecoilControlMultiplierProne`), recoil and knockback are reduced as when sneaking, guns are held forward along the body and hitboxes follow; jumping stands you up, the `holdToProne` client setting makes the key hold-to-lie and the `allowProne` server setting turns prone off. Players crawling under low blocks count as prone too
* New `MeleeSpeed` and `MeleeRange`, updated melee statistics and lightsaber balance
* Tactical reloads with configurable magazine bonuses, timed secondary-mode switching and grips shown only in secondary mode where configured
* Held guns and grenades are placed exactly as in 1.7.10, and the left hand mirrors the right
* Aim offset support and debug commands for gun models
* `DisableCulling`; `DistortSound` also affects `MeleeSound`
* Configurable despawn time for recoverable thrown guns
* RPM and reload time overlap, and rocket launchers fire faster; guns, AA guns and driveable weapons firing single-round ammunition wait the shorter of their reload time and rate of fire between shots
* Fixed attachment points (including MP34 and red dot), the Springfield, Kar98k, flare gun, Olympia, WW2 flamethrower and creeper explosions
* Fixed the melee sound playing twice, gun idle sounds and Apocalypse entities
* Stereo content-pack sounds now fade with distance like mono sounds, including vehicle engines and weapons used by players and NPCs

## Rendering & Performance

* GPU mesh caching and size culling draw only visible ranges and skip hidden batches
* Oculus/Iris shader packs: correct glow layers and muzzle flashes, and a cheaper shadow pass
* Sodium and Embeddium fast vertex writing, and EntityCulling compatibility
* Normal explosions use ordinary smoke particles instead of persistent smoke-screen emitters
* Smoke grenades and shells have separate emission and live-particle limits for each source, including existing content packs; `flansmod.smoker` and `flansmod.smoker1` form broad smoke clouds at their origin instead of throwing multiplying smoke emitters away from the detonation; Warfare 44 M8 and Star Wars Smoke Detonator grenades reuse this smoke effect; `SmokeRadius` scales cloud size and the cloud fills the whole radius, positive `SmokeRadius` and `SmokeTime` appear in tooltips, and source packs balance smoke radius and duration by shell size and role, with smaller areas for traps and self-use effects; built-in smoke grenade, gas grenade and smoke shell categories set `SmokeTime` and `SmokeRadius` from real burn times
* Clouds no longer draw over Flan particles
* Content caches moved to `.flansmod-cache` (migrated automatically) and temporary files to `.flansmod-temp`
* Faster content startup and creative search
* The Forge loading screen shows content pack progress: which pack is loading, why a pack is being reprocessed, Java model compilation and a final summary
* Guns read their attachments, fire mode and loaded ammunition without copying their item data each time and look up the ammunition they take once, which makes firing, reloading and battles much cheaper for the server

## Settings & Commands

* `maxFlansParticlesPerTick` now defaults to 128 and has a maximum of 4096; it limits new particles per tick, not the total still alive
* Reorganized quick options, with precise sound range inputs, numeric inputs for wide ranges and themed General Settings sections
* Restore Defaults per screen, previewing which settings change
* New server settings `maxPlayerHealth`, `gunDevMode`, `enableKillMessages` and `showDistanceInKillMessage`; new client settings `showHitMarker` with colour and opacity, and `showDetailedItemDescriptions`
* Improved command help and discoverable `/flansmodultimate` aliases
* `/flandebug` is now `/flansdebug` (also `/flansmodultimate debug`)
* Uncensored preferences are kept, and vanilla gamerules are exposed in the settings
* Engines carry further: `vehicleSoundRange` now defaults to 128 blocks (was 50), and planes and helicopters have their own `planeSoundRange` (256); turret traverse sounds use `vehicleUtilitySoundRange` and placement sounds `reloadSoundRange`; `grenadeFlashSoundRange` now defaults to 64 (was 16). Existing config files keep their values

* Added the missing label and tooltip for `fragSparkMultiplier` in all supported languages

## Compatibility & API

* The public API is now stable (1.9) and organized into domain packages, including Teams support for non-player combatants, friendly fire rules, the command centre, debug-mode markers and the change-seat key
* Addon seat occupants can preserve their seat's body rotation when riding living mounts (`IFlanSeatOccupant.flansmodultimateHasSeatBodyRotation`)
* Addon entities can play a sound the way the mod plays the same kind of sound, carrying as far and as loudly as a gun shot, a reload, a throw or a vehicle or plane engine does on that server (`SoundRange`, `FlansEntitySounds.getRange`, `FlansEntitySounds.playClip`)
* Addons can tell the mod's own content from other mods' by the namespaces it registers under (`FlansModApi.getModNamespaces`)
* Addons can call their own lying-down entities prone, so they fire with prone accuracy and hold their guns forward (`FlansPostures.addProneRule`, `FlansPostures.isProne`)
* Addons can tell whether a Flan gun item fires, is thrown or is a shield from its definition alone, far cheaper than reading all of its statistics every tick (`FlansEquipment.getWeaponKind`), and make their dying combatants follow the Teams `weaponDrops` and `armourDrops` rules (`FlansTeams.applyDeathDropRules`)
* Addons driving a seat can read how far the round its driver weapons have loaded reaches round where it goes off, and whether a weapon bank points along the hull rather than turning with the turret
* Clients disconnect safely, naming the differing content packs, when their content pack registries do not match the server's
* Fixed bullet damage to players outside Teams rounds
* Server-side weapon authority and resource accounting fixes
* Updated to Forge 47.4.28 (47.4.10 or newer still supported)

## Content Packs

* 38 new national and historical flags in Wolff's Flag Pack, including Spain, Turkey, China, Romania, Yugoslavia and several historical German states
* New Warfare 44, Germany WW2, Warhammer 40k classes and teams
* Sniper classes carry scopes, including attachments resolved from legacy mixed-case shortnames; new WW2 riflemen; Warfare 44 scouts, machine gunners and automatic riflemen carry faction sidearms and spare ammunition
* Classes can override skins for several models
* New Roman shields
* Updated vehicle armour, categories and textures
* More accurate vehicle, aircraft, gun emplacement and weapon names in the Official, Warfare 44, Manus and Germany WW2 packs
* Fixed the Modern Warfare Cobra resting above its skids and its rotor, stub wings and tail boom being hard to hit
