# Flan's Mod Ultimate 2.2

## Highlights

* New companion mod: **Flan's Ultimate 2: NPC Vehicles & Soldiers**, which lets Custom NPCs soldiers use Flan's weapons, vehicles, planes, ships and mechas, crew them and fight Teams rounds, skirmishes and wars (separate download, requires Custom NPCs)
* Driveable damage and destruction: damaged parts smoke, burn, steam and spark, each machine class dies its own way (tank cook-off, ship magazines and boilers, aircraft fuel, rotor and wing loss, mecha collapse), and parts shot off fly away; destruction bursts stay bounded, ship magazine blasts scale with ammunition rather than hull size, and machine-gun-only boats produce small cook-off pops
* Destroyed driveables stay as burning wrecks that settle or sink, keep their collision, burn out and can be blown apart (`driveableWreckLifetime`, `NoWreck`)
* Driveable repairs and carrier servicing, with repair tools, the Blowtorch, a repair progress display and an operator command to fully repair a driveable
* New Teams command centre for operators (F7, `/teams admin` or `/teams gui`) covering rounds, rotation, maps, rules, game type variables and server settings
* Reimplemented missing features of Flan's Mod Ultimate 1.7.10, including Teams rule commands, lag compensation settings, death drop rules, item spawners, `ShootWithOpenDoor`, kill messages with distance and hit markers
* New combat effects: screen shake and camera recoil, tracer beams and mixed belts, hold-to-throw grenades, thermal vision scopes, hot rounds and team IFF strobes, explosion shake, fly-by sounds and suppression
* Large rendering performance improvements and shader pack compatibility (Oculus/Iris)
* Faster content loading and startup

## Teams

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
* Team selection opens on login only while a playable round is live

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
* Both mecha guns fire along the mecha's aim from the correct muzzle positions
* Fixed the hitboxes of planes and helicopters flown by Flan's NPCs facing backwards, and pieces thrown off a wreck coming to rest in the air instead of on the ground
* Fixed animated tracks, driveable collisions against walls and ThrownGun gravity
* Pointing a driveable repair tool (such as the blow torch) at a part of a driveable or Flan's NPC shows its remaining and total HP; debug mode shows it above the armor readout

## Weapons

* Default ammunition selection prefers compatible rounds from the weapon's own content pack, including for `/defaultammo`
* Historically supported firing modes for source-pack weapons, including semi-auto selectors and two- or three-round bursts
* Variable zoom for modern scopes (binoculars up to 40x), with new default keybinds
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
* Clouds no longer draw over Flan particles
* Content caches moved to `.flansmod-cache` (migrated automatically) and temporary files to `.flansmod-temp`
* Faster content startup and creative search

## Settings & Commands

* Reorganized quick options, with precise sound range inputs, numeric inputs for wide ranges and themed General Settings sections
* Restore Defaults per screen, previewing which settings change
* New server settings `maxPlayerHealth`, `gunDevMode`, `enableKillMessages` and `showDistanceInKillMessage`; new client settings `showHitMarker` with colour and opacity, and `showDetailedItemDescriptions`
* Improved command help and discoverable `/flansmodultimate` aliases
* `/flandebug` is now `/flansdebug` (also `/flansmodultimate debug`)
* Uncensored preferences are kept, and vanilla gamerules are exposed in the settings

## Compatibility & API

* The public API is now stable (1.8) and organized into domain packages, including Teams support for non-player combatants, friendly fire rules, the command centre, debug-mode markers and the change-seat key
* Addon seat occupants can preserve their seat's body rotation when riding living mounts (`IFlanSeatOccupant.flansmodultimateHasSeatBodyRotation`)
* Addon entities can play a sound the way the mod plays the same kind of sound, carrying as far and as loudly as a gun shot, a reload, a throw or an engine does on that server (`SoundRange`, `FlansEntitySounds.getRange`, `FlansEntitySounds.playClip`)
* Addons can tell the mod's own content from other mods' by the namespaces it registers under (`FlansModApi.getModNamespaces`)
* Addons driving a seat can read how far the round its driver weapons have loaded reaches round where it goes off, and whether a weapon bank points along the hull rather than turning with the turret
* Clients disconnect safely, naming the differing content packs, when their content pack registries do not match the server's
* Fixed bullet damage to players outside Teams rounds
* Server-side weapon authority and resource accounting fixes
* Updated to Forge 47.4.28 (47.4.10 or newer still supported)

## Content Packs

* New Warfare 44, Germany WW2, Warhammer 40k classes and teams
* Sniper classes carry scopes; new WW2 riflemen
* Classes can override skins for several models
* New Roman shields
* Updated vehicle armour, categories and textures
* Fixed the Modern Warfare Cobra resting above its skids and its rotor, stub wings and tail boom being hard to hit
