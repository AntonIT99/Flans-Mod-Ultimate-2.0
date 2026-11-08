# Flan's Mod Ultimate 2.2

* The legacy 1.7.10 Panzer IV displays its driver-controlled MG42 and fires it from the visible muzzle

* Legacy Manus F-22 and T-34 parts use corrected rotation axes; the legacy 1.7.2 Panzer IV preserves its hull and turret orientations, elevates its barrel in the correct direction and displays its driver-controlled MG42

* Classic Packs translations in German, Spanish, Russian and Simplified Chinese cover all current items, blocks and classes, including legacy variants, with consistent clothing and ammunition names

* Wolff's Star Wars Pack blasters hit harder, sniper rifles use dedicated high-power bolts, the DC-15S and DC-17 use 50-shot power packs, and the Z-6 has a separate 200-shot supply; class loadouts carry the matching ammunition

* Wolff's Germany Pack adds lightweight grey and feldgrau Wehrmacht and Special Corps uniform Stahlhelme and tunics with shared uniform trousers and boots, rendered with the default armour model for large NPC battles

* `animThirdPersonOffset x y z` overrides the Java gun model's third-person offset when specified; `/flansdebug thirdPersonOffset [x y z | reset | clear]` previews the final offset locally without editing content packs

* Driveable occupants can be hidden according to door state using `SetPlayerInvisibleOnDoorOpen` and `SetPlayerInvisibleOnDoorClose`

* API-spawned driveables and wrecks now face the requested Minecraft yaw, correcting a 90-degree offset

* Addon API: `FlansEntityTypes.getHullFootprint` reads the length, width and height of a driveable definition's hull from its collision shapes, without its turret and barrels

## Highlights

* Client `Explosion Visual Settings` offers independent particle size and density multipliers for explosion layers, plus particle lifetime and water splash scaling; defaults preserve the existing visuals; all controls have translated labels in every supported language

* HEAT charges apply full jet damage to unarmoured vehicles and aircraft regardless of `UseRealisticVehicleHealth` (legacy-health vehicles use the higher of their authored damage and the jet damage; armoured faces still require penetration); ammunition and launcher vehicle/plane damage stats show the HEAT jet range

* Driveable `FlashModel DefaultFlash` applies to `GUN` banks by default; `ShellMuzzleFlash true` also enables flashes for `SHELL` banks

* Passenger weapons support authored `barrels` / `SeatGuns`, `barrelSpread` / `SeatGunSpread`, and offset `PassengerShootParticles` gated by `PassengerHasParticles`, including NPC vehicle crews
* `PassengerZoom` enables passenger optics unless scope support is explicitly disabled; configured per-sight zooms retain priority
* Legacy `cannonRecoil` enables main-gun camera kick, with explicit `FancyScreenShake` settings taking priority
* Grip attachments support toggleable `Bayonet` mode and `BayonetMeleeTime`, using the gun's existing stabbing animation and the grip's secondary damage projectile without consuming cartridges
* Ammunition supports server-steered `ProportionalNavigation` and `ProNavGain`, retaining missile speed and existing seeker and turn limits
* Aircraft support speed-triggered `StukaSound` sirens with `StukaSpeed` in mph, `StukaSoundLength` and `StukaSoundRange`
* Addon muzzle particles can carry an optional model-space position offset through `WeaponMuzzle.Particle`, retaining the existing two-argument constructor

* Ammunition `DynamicHitSoundEnable` adds material-specific block impacts alongside authored `HitSound`; an explicit false disables material feedback while unconfigured packs retain the existing fallback
* Healing tools with `HealStrength` apply regeneration using `HealAmount` as duration in ticks and `HealStrength` as amplifier; tools without it keep instant healing, and repair amounts are unchanged
* Grenade `RandomDirection` selects server-side scatter direction, roll and speed for debris/effect launches; grenade roll is synchronized on spawn and saved with the entity

* Gun `RPM` is accepted as a fallback alias of `RoundsPerMin`; category-provided `RoundsPerMin` retains priority over pack `RPM`
* Content definitions accept `Descriptions` as an alias of `Description`, with the canonical spelling taking priority
* Gun `ActionEndSound` plays once when the native bolt or pump closes, with separate server-validated sound allowances for accepted shots and reloads
* Bullets and grenades support `DistantDetonateSound` and `DistantDetonateSoundRange` (default 900 blocks), heard outside the near explosion sound range

* Legacy `speedMultiplier` aliases `BulletSpeedMultiplier` for ammunition fallback speed; declared bullet `MuzzleVelocity` / `BulletSpeed`, belt-round velocities and ammunition-specific weapon speed overrides now bypass the ammunition multiplier
* Guns accept `CanLockOnAngle` and guns and bullets accept singular `LockOnToVehicle` as aliases; existing canonical spellings take precedence when both are present
* Gun `AimTimeConstant` controls aiming transition smoothing, and authored `RunPosTime` controls smooth entry into and recovery from the sprint pose; zero timing switches instantly and unset fields preserve existing behavior

* Legacy packs can hide individual occupants with `invisiblePassenger <seat> <true|false>` or `SetPassengerInvisible`, including NPC vehicle crews
* Guns support first-person `IronSightOffset` and `IronSightPitch` corrections without changing projectile aim
* Armor pouches support `reloadMultiplier` / `pouchMultiplier` and general or weapon-specific pouch flags for players and NPCs; the fastest matching worn pouch applies without stacking
* Armor supports `AirSupply <seconds>`, extra underwater air that adds up over worn pieces, and `SnorkelDepth <blocks>`, which lets the wearer breathe while the water surface is within that depth above their eyes
* Ammunition supports `ricochetSound`, `minorPenSound`, `penetrateSound` and `overPenSound` for resolved armor hits on driveables and NPC vehicle models, with `bounceSoundRange` for ricochets

* Driveable weapons support opt-in shared overheating (`OverheatLimit`, `OverheatPenalty`, `CoolingBonus`, `OverheatSound`), with server-enforced lockouts, cooling and a heat HUD for drivers

* Guns support optional `EquipSound` on weapon switches; driveable weapon banks support `DistantSoundPrimary`, `DistantSoundSecondary` and `DistantSoundRange` (including legacy `farSound` and `farSoundRange` aliases)

* Modern Warfare aircraft in the Classic and Manus packs use wider rear camera views where needed; both F-22 models gain dense afterburner exhaust from 0.65 throttle that follows aircraft motion

* Air brakes use configurable physical effectiveness and low-speed assistance (`airBrakeEffectiveness`, `airBrakeAssistDeceleration`, `airBrakeAssistReferenceSpeedKmh`) in both flight models, with per-aircraft `AirBrakeStrengthMultiplier` and no fixed air-brake deceleration ceiling

* Damaged AA guns can be repaired with vehicle repair tools, including their cooldown, durability costs, crosshair health HUD and repair progress display; destroyed AA guns leave unusable, charred wrecks with a modest fire and smoke effect, using `driveableWreckLifetime`, and cannot be repaired; destroyed guns and their ammunition do not drop

* Cross-pack specialist classes: national WW2 flamethrowers, a Soviet NKVD officer and German support roles including Wolff MG34/MG42 gunners, modern attachment loadouts, survivor rescue and supply roles, and Zombie weapons for modern SWAT and Criminal teams
* Player classes can use repeatable `AddToTeam <packId>:<teamShortName>` lines to extend another pack's teams without a reverse dependency
* New companion mod: **Flan's Ultimate 2: NPC Vehicles & Soldiers**, which lets Custom NPCs soldiers use Flan's weapons, vehicles, planes, ships and mechas, crew them and fight Teams rounds, skirmishes and wars (separate download, requires Custom NPCs)
* Driveable damage and destruction: damaged parts smoke, burn, steam and spark, each machine class dies its own way (tank cook-off, ship magazines and boilers, aircraft fuel, rotor and wing loss, mecha collapse), and parts shot off fly away; destruction bursts stay bounded, ship magazine blasts scale with ammunition rather than hull size, and machine-gun-only boats produce small cook-off pops
* Destroyed driveables stay as burning wrecks that tumble as they fall, come to rest along the ground they land on, slide down steep slopes, keep their collision, char and settle, burn out and sink away after five minutes by default, and can be shoved about or blown apart by explosives; destruction ends client driving prediction while preserving the visible pose, and their own destruction blasts cannot hit or launch their hull through seats or wheels; their smoke clouds are slightly smaller; the scorch mark under a wreck lasts as long as the wreck and fades when it is broken up or removed (`driveableWreckLifetime`, `NoWreck`)
* Driveable repairs and carrier servicing, with repair tools, the Blowtorch, a repair progress display and an operator command to fully repair a driveable
* New Teams command centre for operators (F7, `/teams admin` or `/teams gui`) covering rounds, rotation, maps, rules, game type variables and server settings; global Teams rules can be saved as named presets, previewed and imported/exported as JSON files alongside built-in Arena and Survival presets, with preset management pinned to the bottom of the Rules tabs; every Rules tab with more rows than fit the window (Protection, Combat, Health, Cleanup, Game type) scrolls with the mouse wheel or a scroll bar
* Reimplemented missing features of Flan's Mod Ultimate 1.7.10, including Teams rule commands, lag compensation settings, death drop rules, item spawners, `ShootWithOpenDoor`, kill messages with distance and hit markers
* Airborne wound puffs and severe-injury spray accompany temporary ground blood splatters on player and opted-in NPC injuries and deaths, controlled by the default-on Teams `Blood Effects` rule (`/teams bloodEffects true|false`); blocked hits do not bleed and fatal heavy hits do not duplicate splatters; splatters last for the client `bloodSplatterSeconds` setting (35 by default), which the Teams `Ground blood time` rule (`/teams bloodLife <seconds>`, 0 = each client's own) overrides during rounds
* New combat effects: screen shake and camera recoil, tracer beams and mixed belts, hold-to-throw grenades with realistic weight-based category speeds (up to 27 m/s) and weaker underhand tosses (hold and release attack for overhand, use for underhand); a quick click throws at full speed, holding longer charges the throw like a bow with a progress bar under the crosshair (`ThrowChargeTime`), thermal vision scopes, hot rounds and team IFF strobes, explosion shake, fly-by sounds and suppression
* Large rendering performance improvements and shader pack compatibility (Oculus/Iris)
* Faster content loading and startup, including reuse of model muzzle measurements after equivalent pack rebuilds and unrelated definition edits, with logged cache invalidation reasons

* All 19 additional Labjac particle names resolve, including colored shell smoke, water plumes, blood spray, destruction columns, lock markers, canisters, the Nu muzzle sprites and working world casings; suitable weapon, vehicle and NPC events reuse them, and `flansmod.muzzleflashclassic` selects the dedicated legacy muzzle sprites; weapon smoke is subtle and short-lived, follows the rendered first-person muzzle without obscuring its flash, respects left-handed muzzle placement, and can be disabled with the client `showWeaponSmoke` option; quick-access settings include weapon smoke, personal blood visibility (`showBloodEffects`), casings, vehicle damage-effect density and detached debris

## Teams

* `Deployed guns break` (`canBreakGuns`) now defaults to off; saved settings are preserved

* The default-off `Keep corpses` rule in Rules > Cleanup (`/teams corpses true|false`) keeps the bodies of fallen bots for its `Corpse time` (`/teams corpseLife <seconds>`, 0 = until the round ends); kept bodies lie in their death pose without the red hurt tint

* Capture point letters and names render clearly over their label backgrounds

* Newly placed capture points use the first unused letter on their map, reusing freed letters; the editor and saved points support A-Z

* Newly placed Teams Capture Point Flags snap to the nearest 45-degree rotation

* Renamed the Teams Flagpole and Teams Capture Point items to Teams Base Flag and Teams Capture Point Flag in all supported languages

* Operators can assign Teams banners to bases through a right-click menu, immediately updating their team-colored cloth and flag even outside rounds

* The `destroyableDeployedGuns` round rule lets damage destroy deployed guns during a round (default false); destroyed guns and their ammunition do not drop

* Teams vehicle spawners fill driveable fuel tanks and load default weapon ammunition plus up to 32 spare ammunition items per type in available cargo space; configure `teamsSpawnVehiclesWithFuel` and `teamsSpawnVehicleAmmo` (0 disables ammunition provisioning)

* Round Rules offer `disableClassLimits` in the Teams command centre to bypass player class slot and percentage caps while keeping rank and prestige requirements (default false)

* Players can choose vehicle spawners with template items on the deployment map to spawn aboard a free driver, gunner or passenger seat; vehicle markers show full vehicles, access restrictions and live cooldown countdowns, and the server rechecks the seat before boarding

* During Teams rounds, spawners, capture points and team banners are protected from weapons, explosions, mobs and other indirect destruction; operators can remove them only by directly breaking them or using the Operator Stick, while flags and flagpoles remain damage-proof

* Addon API 1.21 adds server-side blood-effect opt-in through `ILivingVisualEffects`, the Teams bot corpse rule through `FlansTeams.getBotCorpseLife` and untinted corpse rendering through `FlansCorpses` and exposes nominal per-face vehicle armour, turret facing, loaded seat ammunition performance including weapon overrides, projectile mass, per-bank barrel recoil eligibility and confirmed armour-impact callbacks for target-aware AI, plus shared operator preset libraries and client file import/export through the command centre API, mass-scaled external impulses for model entities, server-authored mounted firing effects through `FlansProjectiles.firingEffects`, shared terrain-following model suspension through `FlansEntityTypes.sampleTerrainAttitude`, and shared vehicle spawner cooldown queries and spawn notifications through `FlansTeams`, and lists every default round of a gun, an AA gun or a driveable weapon bank through `FlansEquipment.createDefaultAmmunitionChoices`, exposes grenade purpose and additional flash/gas reach through `FlansProjectiles.getGrenadeProperties`, without changing damage, and shared naval compartment trim, list and open-water speed limiting through `FlansRepairs.navalAttitude`, `FlansRepairs.atSea` and an `atSea` argument on `FlansRepairs.navalVelocity`, which replaces its four-argument form, and supports server-controlled living operators of placed deployable and AA guns through `IEmplacement`, retaining automated aim independently of operator look updates, with validated deployment through `FlansEmplacements`, with grenade-device placement, owned remote detonation and supply-bag use through `FlansGrenadeDevices` and living machine proximity routing through `IProximityTarget` and cooperative allied-player seat handoff through `IFlanSeatOccupant`, plus mecha hand-tool, trigger-state, rocket-pack, walking, passive-addon, lighting, landing and stomp contracts for autonomous controllers

* All spawners offer a base-assignment menu and immediately show their assigned base's team color, including before a round starts and when changing bases; item and vehicle spawners have a spawn delay in seconds (default 20), shared with NPC deployments on vehicle spawners, while vehicle spawners support AA guns, wait for their previous vehicle to be destroyed or leave the map by default, offer a nearby-only replacement mode, allow NPC vehicles by all, none, multiple categories or exact definitions, and select between spawning all ready NPC vehicles into clear nearby spaces (default) or deploying them one after another

* Ship vehicle spawners use the nearest clear water surface within 64 blocks, and wait to spawn when no water is available in loaded chunks

* Global rules in the command centre's Rules > Health tab: `bodyPartDamageModifiers` disables hit-zone damage multipliers for players and humanoid NPCs when false (default true), and `playerHealth` / `botHealth` override their maximum HP separately (default 0 keeps existing health); these rules apply outside rounds too, and health changes preserve the remaining percentage

* Opt-in round rules, configurable in the Teams command centre: shared respawn waves, limited lives, objective overtime, per-side vehicle limits and replacement delays, capture restrictions, protected headquarters, warm-up and ready checks, team-switch restrictions, flag-carrier equipment and movement restrictions, and nameplate, kill-message and spectator controls; server defaults and per-round overrides are saved and apply at the next round start; the Round Rules page name and tooltip are localized

* Conquest and Domination default to `killticketvalue 1`, `scorelimit 60` and `ticketdraininterval 100` (five seconds), preserving the relative value of kills and objective control; saved game type settings and round limits are kept

* Optional `/teams restoreBlocks true` (also on Rules > Cleanup in the command centre) restores changed map blocks and block entities after a round ends, stops or switches, including explosion damage and broken glass; changes are saved across server restarts

* Owned flag poles show their team's color or flag texture outside rounds when the rounds configured for their map identify the owner unambiguously

* Teams weapon drop rules also cover grenades and tools such as bandages for players and bots

* Spawn protection: players cannot be hurt for a few seconds after taking the field in a round, until they fire, throw, swing or hit an enemy; set the time with `/teams spawnProtection <seconds>` (default 3, 0 turns it off, at most 60) or on the Protection tab of the command centre's Rules page
* `/teams regeneration <true|false>` controls passive healing for players and bots during rounds; separate `playerRegenSeconds` and `botRegenSeconds` controls in Rules > Health set the seconds per half-heart (default 5; 0 disables healing for that group)
* `/teams stop` stops the current round, with the same cleanup as `/teams admin stop`; operators can use `/teams restart` to stop it and immediately start the next configured round; both commands clear driveable and AA-gun wrecks within the current map, and wrecks of a finished round that were in unloaded chunks are removed when those chunks load; a round that ends normally clears its map's wrecks too
* Kill messages and the kill feed name the weapon, grenade or vehicle responsible
* Holding a healing tool in either hand shows the health of the living target within five blocks, including players and Flan soldiers, inside and outside rounds; vehicle models are excluded

* `showBotKillsInKillFeed` controls whether the battle kill feed includes bot-versus-bot kills, also available in quick access Server settings; disable it to show only kills involving a player

* Unassigned flagpole bases and capture points default to the first map in their dimension when placed or loaded; the Operator Stick can still change their map
* Teams bases and capture points have configurable pole heights (`defaultFlagpoleHeight`, now 5 blocks instead of 3), editable separately with the ownership stick
* Domination and Conquest rounds can be played attackers against defenders
* Multi-team alliances
* Team flags fly on Capture the Flag flags, base allegiance banners and team selection buttons, and appear beside objective HUD ticket counts; flagpole and capture-point flags preserve the PNG aspect ratio at any width, and flags can also be loaded from an http(s) PNG link (`loadTeamFlagsFromUrls` turns this off)
* Default team flag textures; `team_categories.json` assigns shared flags to matching internal and external content-pack teams, including legacy flag references
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

* `frontalArmor`, `leftsideArmor` and `rightsideArmor` own their face's `ArmorFrontMm`/`ArmorSideMm` plate while intact; after one is destroyed its hitbox disappears and the core uses that plate as fallback, including on NPC vehicle models; explicit core `PartArmor` remains additional protection, other hull parts open on the lost face, and the debug armour readout marks breached faces; on driveables and NPC vehicle models, shells carry damage left after destroying a part into hitboxes along the shot without counting the same inherited core plate twice, and HEAT carries its surplus through faces with no HEAT protection but stops at additional armor; HESH acts only on the first active part it hits

* Aircraft damage is progressive (`enableRealisticAircraftDamage`, on by default): worn wings lose lift and pull the aircraft toward the weaker side, a shot-up tail answers more slowly, hit engines lose power and misfire, holed wing tanks leak fuel, and worn rotors lift less; a helicopter that loses its engine or runs dry can autorotate down and flare to land, and a plane down without its gear slides to a stop on its belly
* Crippled and dead aircraft come down as airframes instead of tumbling: nose into the wind, spiralling over a lost wing, flat-spinning without a tail, spinning about the mast without a tail rotor or dropping without a rotor; one blown apart in the air may lose its tail or a wing as a separate piece
* Going down: burning aircraft trail black smoke visible from up to 320 blocks, wing, tail and blade pieces flutter down like leaves, rotor strikes shed blade fragments, crashes into water throw up a spray column instead of a fireball, belly slides and sliding wrecks plough scorched furrows with dust and sparks, falling aircraft scream louder as they speed up, and crash booms arrive with a delay over distance
* The pilot of a damaged aircraft sees blinking warnings on the HUD (fire, engine, fuel leak, wing, tail, rotor, autorotation, mayday) with an alarm, feels the airframe shake, and gets oil streaked across the view by a badly hit engine
* Driveables that name mounted guns in `ShootPointPrimary` or `ShootPointSecondary` without a `Primary` or `Secondary` line fire those guns again, as in 1.7.10
* Wheels and tracks throw up dust in the colour of the ground they run on, a helicopter hovering low blows a ring of dust, or of ripples over water, out from under itself, and ground vehicles smoke from their exhaust while under power; ships keep their bow spray and funnel smoke
* The vehicle HUD shows the current ammunition-switch key below the ammunition display for drivers with shells, bombs or missiles; the switch key always moves to a different round type and restarts the reload with it at once, even mid-reload, and the HUD shows the next round it would load; after a shot the bank reloads the same round type while any is left instead of switching on its own
* Driveables with real-world engine data take their power only from that data, no longer multiplied by the installed engine's `EngineSpeed`, so the same vehicle category performs alike in every pack; induced drag now matches the playability-scaled lift, so aircraft such as the Ju 87 Stuka no longer stall out around 45 km/h on the runway or in slow flight

* Armour hits on driveables and NPC vehicle models show outward sparks and metallic impacts when blocked, and tighter flashes, metal fragments and smoke when large shells penetrate, with effects scaled to projectile mass
* Ricochet star particles default to a base count of 1, scaled by projectile size; configure their base count with `ricochetSparkCount` (0 disables them)

* Ships are now separated from ground vehicles, with improved ship physics; leaving a ship puts the player on top of its hull
* Creative players can pick up unoccupied floating ships and airborne driveables by hitting them
* Vehicle cannon screen shake
* Depth charges, HESH rounds, tracer rounds and composite armour
* Suitable ground vehicle categories squash mobs they run over
* Ground vehicles ease back toward level when wheel support is lost instead of retaining a stale terrain tilt while their body rests on a ledge
* Configurable driveable melee protection and weapon damage overrides
* Configurable render and tracking distances for driveables and Flan NPC models
* Smooth view and position for players standing or walking on moving hulls and turrets, including large ships; fixed sudden turns and drops from unstable carry transforms and replayed movement when server time is synchronized
* Passenger guns mounted on a turret aim relative to the turret
* The Change Seat key now defaults to Numpad Enter instead of Numpad 0, which Custom NPCs uses to reset scenes (saved key settings are kept)
* Both mecha guns fire along the mecha's aim from the correct muzzle positions
* Wrecks lie along the ground they come to rest on instead of staying at the attitude they died at, tumble when shot down in the air or thrown by a blast, slide down slopes too steep for what is left of their running gear, crush and mark the ground where they come down hard, and ships go down at a rate set by their own flotation and stay on the sea bed for the rest of their lifetime
* A wreck's gun droops and its turret slumps off its bearing as it cools, a collapsed mecha's hull now blocks where its model actually lies, and a burnt-out hull is taken down into the ground over its last seconds instead of vanishing
* Columns of smoke over a battlefield all lean with one slow drift, stronger in the rain, and burning wrecks are heard as a roar that carries plus a crackle close to
* Fixed the hitboxes of planes and helicopters flown by Flan's NPCs facing backwards; detached parts such as tracks and armor screens now check ground support against their rendered geometry, tip into a stable resting position, remain supported on narrow blocks and land correctly on stairs and slabs without snapping onto overhead surfaces
* Fixed animated tracks, driveable collisions against walls and ThrownGun gravity
* Driveables no longer damage themselves for striking a wall: `CollisionDamageEnable`, `CollisionDamageThrottle` and `CollisionDamageTimes` again only hurt the living entities a driveable runs into, as in 1.7.10, so a pack vehicle that pairs a small authored core with a large `CollisionDamageTimes` no longer explodes against a kerb; what a crash costs the hull is its authored `CollisionPoint` strikes against blocks
* Ground vehicles take drop damage as a share of each part's own health instead of a flat figure, so a fall stays equally consequential whether a pack authors hitpoints in the hundreds or the thousands or derives them from mass through `UseRealisticVehicleHealth`; `FallDamageFactor` still scales it and zero still makes a type immune, the running gear takes a fifth of what the hull takes, and a long enough drop writes the vehicle off
* `SquashMobs` damage is derived from the vehicle's mass and the speed it carries into a body, from the energy that body actually absorbs, instead of a flat figure a bicycle and a tank delivered alike: a hull well under a tonne hurts markedly less, a car at 60 km/h is lethal, and a slow nudge is far gentler than before
* Ships that declare `RealDraftM` settle at their waterline even when their definition sets `Buoyancy` to zero, instead of hanging at whatever depth they were placed
* Pointing a driveable repair tool (such as the blow torch) at a part of a driveable or Flan's NPC shows its remaining and total HP; debug mode shows it above the armor readout
* Using a driveable repair tool on a vehicle repairs it instead of entering a seat, and repair tool tooltips show their repair amount and remaining uses

## Weapons

* Disposable guns such as the Panzerfaust are no longer removed after firing in creative mode (`ConsumeGunOnUse`)

* New gun parameter `EjectCasings` turns off casing ejection for a single gun; blasters, energy and rail weapons, rocket and missile launchers, flamethrowers, revolvers, bows, Nerf blasters, the caseless HK G11 and muzzle-loaders no longer throw out casings

* Full-power rifle and machine-gun ball, cannon high-explosive shells and armour-piercing bombs carry a `PenetrationAt100m` figure in the built-in categories, so coaxial and aircraft machine guns can defeat thin plate such as a towed gun's splinter shield again instead of being stopped outright by any armour at all; small-arms armour-piercing ratings were raised to stay above ball, rounds too weak to defeat the thinnest shield carry no figure, and blast weapons such as bombs, torpedoes, missiles and high-explosive rockets work through overpressure and need none

* `/rearm` and Teams vehicle spawning fill vehicle weapon ammunition slots to their item and inventory stack limits, including partially filled stacks; shell, bomb and missile banks receive a mix of every declared round of the bank's kind instead of a single type, and Teams spare vehicle supplies and `/defaultammo` include every such round too; rounds come from the driveable's own content pack whenever it has any, including the vehicle ammunition issued to NPC crews

* Full ammunition stacks now merge with freshly created ammunition after refilling or cycling to the next magazine; older full stacks are repaired when carried in a player inventory

* Supply bags retain their remaining uses across saves and spend a use only on delivered healing, effects or ammunition; mines respect their arming delay before applying trigger damage, and living vehicle models can participate in vehicle proximity fuses

* Open parachutes carry their rider down to the ground instead of letting go a couple of blocks above it, and nearby explosions, including that of the aircraft a crew just bailed out of, no longer collapse a canopy in mid-air; direct hits only collapse parachutes when `parachutesCanBeHit` is enabled (default false), shared by players and NPCs and adjustable under Rules > Protection in the Teams command centre

* Fixed a crash when an NPC reloads a gun without a configured reload sound
* Fixed shots sometimes having no recoil or firing animation when shooting-state updates arrive between client ticks
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
* Better hit registration: players' shots meet moving players and NPCs where the shooter saw them, allowing for the client's movement smoothing as well as ping (`bulletSnapshotMin` now adds to it), bullets fired over fences and walls are no longer stopped by their invisible upper collision box, and rounds that strike a player or NPC always deal their damage instead of being swallowed by the damage cooldown of an explosion, fire or melee hit just before; in debug mode, contacts predicted only by the client are drawn grey
* Fixed attachment points (including MP34 and red dot), the Springfield, Kar98k, flare gun, Olympia, WW2 flamethrower and creeper explosions
* Fixed the melee sound playing twice, gun idle sounds and Apocalypse entities
* Fixed the ammo HUD staying visible in spectator mode after holding a gun
* Stereo content-pack sounds now fade with distance like mono sounds, including vehicle engines and weapons used by players and NPCs

## Rendering & Performance

* Standing up after going prone or crawling clears the sideways head and hat tilt
* Guns can set `FirstPersonFrame` (`1.7.10` or `1.12.2`; old `Tap` values alias `1.7.10`) to pick the first-person placement their model was made for; the new `preferredGunFirstPersonFrame` client option (also in the quick settings) picks the placement for every gun and shifts guns made for another placement sideways while aiming so their sights stay centred, `/flansdebug viewFrame` previews each placement, and `/flansdebug authoredFrame [1.7.10 | 1.12.2 | reset | clear]` previews the held gun's authored property locally
* GPU mesh caching and size culling draw only visible ranges and skip hidden batches
* Flan armour is drawn through the GPU mesh cache like vehicles and guns, each body part as one batch, which is what a battle of hundreds of uniformed soldiers spent most of its frame time on
* Armour level of detail leaves out uniform parts too small to see, with `Enable Armor LOD` and `Armor LOD Strength` in quick options and `minimumArmorPartPixelSize` / `maximumArmorLodPartPixelSize` in the client config. Parts are measured by the size they project to, so a scope brings a distant soldier's full detail back, and no armour piece is ever hidden
* Oculus/Iris shader packs: correct glow layers and muzzle flashes, and a cheaper shadow pass
* Sodium and Embeddium fast vertex writing, and EntityCulling compatibility
* Normal explosions use ordinary smoke particles instead of persistent smoke-screen emitters; small rounds such as 20 mm keep their previous look, medium shells keep most of their previous smoke around a compact fireball, and bombs use brief charge-scaled fireballs, never smaller than the crater they dig, followed by dense dust and smoke columns that form faster with continuous emission, smoothly growing puffs and overlapping mushroom caps; heavy bombs are drawn from farther away, up to `explosionParticleRange` (default 256 blocks), including distant explosion effects
* Smoke grenades and shells have separate emission and live-particle limits for each source, including existing content packs; `flansmod.smoker` and `flansmod.smoker1` form broad smoke clouds at their origin instead of throwing multiplying smoke emitters away from the detonation; Warfare 44 M8 and Star Wars Smoke Detonator grenades reuse this smoke effect; `SmokeRadius` scales cloud size and the cloud fills the whole radius, positive `SmokeRadius` and `SmokeTime` appear in tooltips, and source packs balance smoke radius and duration by shell size and role, with smaller areas for traps and self-use effects; built-in smoke grenade, gas grenade and smoke shell categories set `SmokeTime` and `SmokeRadius` from real burn times
* Burnt-out wrecks use the far-distance impostor again once they have charred to one colour, so a field left after a big battle costs what the vehicles in it did
* Clouds no longer draw over Flan particles
* Gun attachments such as scopes follow the gun's first-person recoil and sight offset instead of staying in place while the gun kicks back
* Content caches moved to `.flansmod-cache` (migrated automatically) and temporary files to `.flansmod-temp`
* Faster content startup and creative search
* The Forge loading screen shows content pack progress: which pack is loading, why a pack is being reprocessed, Java model compilation and a final summary
* Guns read their attachments, fire mode and loaded ammunition without copying their item data each time and look up the ammunition they take once, which makes firing, reloading and battles much cheaper for the server

## Settings & Commands

* Suppression and gun, vehicle and explosion screen shake are now controlled by the server/common config; client settings, view bobbing and hiding the HUD no longer bypass them
* Tactical smoke from grenades, shells and vehicle launchers keeps its own bounded particle allowances regardless of client particle distance, density or budgets

* `breakableArmor` now offers named choices instead of a numeric slider; existing values 0, 1 and 2 keep their meaning
* Added missing settings labels for `driveableMeleeDamageThreshold`, `teamsSpawnVehiclesWithFuel` and `teamsSpawnVehicleAmmo` in all supported languages

* The `canBreakGuns` team rule now covers only deployed guns; the new `canBreakAAGuns` rule (`/teams canBreakAAGuns`) controls AA guns separately and starts from the old value in existing saves
* `maxFlansParticlesPerTick` now defaults to 128 and has a maximum of 4096; it limits new particles per tick, not the total still alive
* Added the missing label and tooltip for `fragSparkMultiplier` in all supported languages

* Reorganized quick options, with precise sound range inputs, numeric inputs for wide ranges and themed General Settings sections
* Restore Defaults per screen, previewing which settings change
* New server settings `maxPlayerHealth`, `gunDevMode`, `enableKillMessages` and `showDistanceInKillMessage`; new client settings `showHitMarker` with colour and opacity, and `showDetailedItemDescriptions`
* Improved command help and discoverable `/flansmodultimate` aliases
* `/flandebug` is now `/flansdebug` (also `/flansmodultimate debug`)
* Uncensored preferences are kept, and vanilla gamerules are exposed in the settings
* Engines carry further: `vehicleSoundRange` now defaults to 128 blocks (was 50), and planes and helicopters have their own `planeSoundRange` (256); turret traverse sounds use `vehicleUtilitySoundRange` and placement sounds `reloadSoundRange`; `grenadeFlashSoundRange` now defaults to 64 (was 16). Existing config files keep their values

## Compatibility & API

* `FlansAircraftCrashes`, `AircraftDamage`, `CrashMode` and `CrashMotion` let addons read how damaged an aircraft is and fly anything drawn as one down the way Flan's aircraft come down; `DriveableAttitude.of` and `roll()` convert between angles and attitudes
* `FlansDriveables.getDriver` reads who occupies a driveable's controlling seat, so an addon can answer for the rounds its weapons fire; a driveable's seats are entities of their own rather than passengers of it, and so cannot be read off its passenger list
* Optional JEI integration shows vehicle assembly and gun-box recipes with material quantities, compatible engines and crafting stations, plus acquisition hints for core items without ordinary recipes
* Built-in crafting and furnace recipes, and generated legacy content-pack recipes, unlock in Minecraft's recipe book when a player collects a relevant ingredient; rainbow paint cans can be crafted from an empty bucket and eight different dyes and can replace all ordinary dyes for a paintjob with one additional can by right-clicking its icon, and Silk Touch can collect sulphur blocks while ordinary mining still yields sulphur powder

* The public API is now stable (1.9) and organized into domain packages, including Teams support for non-player combatants, friendly fire rules, the command centre, debug-mode markers and the change-seat key
* Addon seat occupants can preserve their seat's body rotation when riding living mounts (`IFlanSeatOccupant.flansmodultimateHasSeatBodyRotation`)
* Addon entities can play a sound the way the mod plays the same kind of sound, carrying as far and as loudly as a gun shot, a reload, a throw or a vehicle or plane engine does on that server (`SoundRange`, `FlansEntitySounds.getRange`, `FlansEntitySounds.playClip`)
* Addons can tell the mod's own content from other mods' by the namespaces it registers under (`FlansModApi.getModNamespaces`)
* Addons can call their own lying-down entities prone, so they fire with prone accuracy and hold their guns forward (`FlansPostures.addProneRule`, `FlansPostures.isProne`)
* Addon entities drawn with the player model can be hit limb by limb like players, with headshots, hit zone damage modifiers and per-piece armour, by stating the proportions they are drawn with (`IFlanHumanoidBody`, `HumanoidBody`); debug mode outlines their limb hitboxes
* Addons can tell whether a Flan gun item fires, is thrown or is a shield from its definition alone, far cheaper than reading all of its statistics every tick (`FlansEquipment.getWeaponKind`), and make their dying combatants follow the Teams `weaponDrops` and `armourDrops` rules (`FlansTeams.applyDeathDropRules`)
* Addons driving a seat can read how far the round its driver weapons have loaded reaches round where it goes off, and whether a weapon bank points along the hull rather than turning with the turret
* Clients disconnect safely, naming the differing content packs, when their content pack registries do not match the server's
* Fixed bullet damage to players outside Teams rounds
* Server-side weapon authority and resource accounting fixes
* Updated to Forge 47.4.28 (47.4.10 or newer still supported)

## Content Packs

* Legacy 1.4.7/1.6.4 Panzer IV and Sherman machine-gun firing points now align with their roof-mounted guns

* Tyrants and Plebeians gun models that previously fell back to their item icon (Kar98k, Lee-Enfield, M16, MP40 and others) now load, and their keyframe animations play in first person: `UseGLTFAnimation`, `UseRightArmGLTFAnimation`, `UseLeftArmGLTFAnimation`, `GLTFAnimation <name> <file>` (read from `assets/flansmod/animations/<file>.json`), `SwitchAnimation`, `ShootAnimation`, `AimingShootAnimation`, `ActionAnimation`, `TacticalReloadGLTFAnimation` and the round-by-round `UseLoopReloadAnimation` with `StartReloadAnimation`, `LoopReloadAnimation` and `EndReloadAnimation`; with `UseLoopReloadAnimation`, a single-magazine gun that still holds rounds only loads the missing ones, taking that share of `StagedReloadTime` (the tactical reload time when unset); grips with `HasSecondaryModeAnimations` play `SecondarySwitchAnimation`, `SecondaryUnSwitchAnimation` and `SecondaryReloadAnimation` and animate themselves and their ammo
* Tyrants and Plebeians gun models also show their first-person effects: ammo variants picked by the bullet's `AmmoModelType <gun> <variant>`, rotating magazines, feed strips, ammo counters, muzzle and drifting chamber smoke (`MuzzleSmokeModel`, `ChamberSmokeModel` and their textures, which that pack's parser swaps, as here), and `fancyCasings` that throw casings and the empty en-bloc clip with simple physics

* Official Modern Warfare, WW2 and Zombie guns with identifiable iron sights use geometry-derived vertical ADS offsets, preserving existing X/Z offsets and prioritizing the 1.7.10 view frame

* Official gun models use the original 1.12.2 sources where appropriate; `FirstPersonFrame 1.12.2` is retained for models requiring the legacy frame correction, while Z-centered Modern Warfare models and WW2 Type 100 use the default 1.7.10 authored frame; G43, Kar98k, Kar98kSniper, MG34, Thompson, Greasegun, BAR, TrenchGun, Luger, STG44, Springfield and Springfield Sniper use centered model Z placement with matching muzzle flashes and the default 1.7.10 authored frame; Springfield geometry is recentered to match the sniper rifle in third person; both variants use zero third-person Z offsets and geometry-derived `animAimOffset` corrections for horizontal ADS alignment
* Added a crafting-table recipe for the Manus Ships German 20 cm Ship Gun used by Prinz Eugen
* New Legacy Content Pack in the Classic Packs brings back earlier official vehicle and aircraft models, from Minecraft 1.4.7 to 1.12.2, as separate `Legacy` items alongside their remakes, each name and description naming the Minecraft and Flan's Mod release its model comes from: M1A2 Abrams, Challenger 2, T-90MS, CH-47 Chinook, F-22, Bf 109, P-51 Mustang, Willys Jeep, Kübelwagen, M1 57mm, PaK 40, Bofors and Flakvierling AA guns, T-34, Panzer II Luchs, Chi-Ha, Churchill, two earlier generations each of the Panzer IV and M4 Sherman plus the unreleased 1.7.2 Panzer IV prototype, whose 1.6.4 models carry a turret machine gunner seat, and 85 earlier gun, mounted gun, attachment and grenade models from the Modern Weapons, Modern Warfare and WW2 packs, plus the original American, American snow, British, British desert, German, German desert, snow and forest, Russian, Marine and Spetsnaz uniforms, plus the French and Italian uniforms from the WW2+ pack and the alternative American, British, German, Italian, Japanese and Russian uniforms, which moved there from the WW2 pack; 19 Legacy teams wear these uniforms and use the WW2 and Modern Warfare classes and weapons, with new French and Italian classes equipped from the WW2 pack
* The Nerf pack adds a Yellow team wearing the previously unused yellow vest and kneepads
* New Manus Legacy Content Pack in the Manus Packs brings back 18 vehicle and aircraft models from the Minecraft 1.4.7-1.5.2 Manus packages as separate `Legacy` items, including the first Panzer IV, Flakpanzer IV, Hummel, T-34, M4 Sherman, He 111, civil trucks, AH-1, Dropships and TIE fighters, and the dropped Manus A-10 and F-22
* New paintjobs from the NPC Vehicles mod: grey and black AH-64 Apache, a Panzer III Legacy Panzer IV, the Zero Titan Mk II and more for the Legacy BF109, Sherman and T-34; the Lancaster B-71 and Do 17 and the Legacy Bf 109 desert paintjobs get their own icons, and every Classic Packs paintjob now shows its own icon in the inventory
* The WW2 pack adds the HG pack's Aperture Sight, Wooden Grip, Wooden Stock, Armor Piercing Bullet and Hollow Point Bullet attachments (already listed by the C96 and MP 34) and the WW2 Armour Box; the Modern Warfare pack adds the Shock-Absorbent Stock
* WW2 American, British, German, German desert, forest and snow, and Russian uniform textures match the official 4.10–1.12.2 releases again, the British desert and American snow uniforms are redrawn to match them (khaki drill, and an improvised white helmet cover and smock over olive drab trousers), and armour from the HG WW2 pack says so in its description
* The Warfare 44 Uniform Box adds the Philippine combat field uniform (short- and long-sleeve variants), the Japanese head band and the previously unobtainable Sumpftarn M45 jacket and pants; a new Warfare 44 Philippine Army team fields Philippine Army, Philippine Scouts and guerrilla classes, and the Japanese Army adds a hachimaki-wearing Raider class
* The WW2 pack adds alternative American, British, German, Italian, Japanese and Russian uniforms from the WW2 Improved pack
* The WW2 Chi-Ha uses the detailed remake from the HG WW2 pack; the previous model moved to the Legacy Content Pack
* The Modern Warfare M1A2 Abrams, Challenger 2 and Leopard 2A6 play their engine start sounds again, and the T-90 its engine sound

* Modern Warfare teams gain dedicated marksman, shotgun, anti-tank, ammunition bearer, demolition, MP5, HK416, MP7, AK74, RPK and pistol-and-shield loadouts, including suppressed MP5 and balaclava variants

* Official WW2 teams gain dedicated Bren, DP-28 and Type 99 gunners, PIAT, bazooka, Panzerschreck and Panzerfaust specialists, PIAT ammunition bearers, Thompson assault troops, Springfield riflemen, Type 38 engineers and national pilot loadouts

* 38 new national and historical flags in Wolff's Flag Pack, including Spain, Turkey, China, Romania, Yugoslavia and several historical German states
* New Warfare 44, Germany WW2, Warhammer 40k classes and teams; Warfare 44 class menus group related roles and weapon variants, use every uniform piece across winter, military police, camouflage and command loadouts, and label experimental, legacy and postwar equipment choices; additional Warfare 44 loadouts include winter BAR, Thompson, M3, MG34, MG42 and MP40 variants, M1943 airborne uniforms, a Sumpftarn M43 rifleman, an MG42 Fallschirmjager and rifle-equipped MG ammunition bearers for all seven teams; further W44 variants expand winter engineers, Browning gunners and anti-tank troops, camouflage machine gunners, G43 paratroopers and rifle-equipped engineers and leaders, plus rifle-equipped anti-tank variants for all seven teams; Warfare 44 classes include a knife as basic equipment
* Sniper classes carry scopes, including attachments resolved from legacy mixed-case shortnames; new WW2 riflemen; Warfare 44 scouts, machine gunners and automatic riflemen carry faction sidearms and spare ammunition; new Warfare 44 specialist classes use the ZF 4, ZF 41, PU and M3 infrared scopes and the Sten suppressor (Gewehr 43 and FG 42 marksmen, Kar98k sharpshooter, SVT-40 marksman, sniperscope carbine night scout, Sten Mk II(S) commando)
* Classes can override skins for several models
* Wolff's Modern Tech pack adds U.S. Army uniforms with a U.S. Army Rifleman class for the U.S. Marines team, plus diving gear: snorkel, scuba mask and helmet, single and double air tanks and an atmospheric diving suit helmet; the scuba mask, scuba helmet and diving suit helmet have their own first-person overlays
* New Roman shields
* Updated vehicle armour, categories and textures
* More accurate vehicle, aircraft, gun emplacement and weapon names in the Official, Warfare 44, Manus and Germany WW2 packs
* Fixed the Modern Warfare Cobra resting above its skids and its rotor, stub wings and tail boom being hard to hit
