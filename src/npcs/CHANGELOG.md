# Flan's Ultimate 2: NPC Vehicles & Soldiers 2.2.0

First release. Requires Flan's Mod Ultimate 2.2.x (including 2.2) and Custom NPCs. NPC patch versions are independent; incompatible main-mod version lines are rejected.

## Soldiers

* Custom NPCs use Flan's weapons, grenades, shields and armour, with inventory ammunition, reloads and matching sounds and effects
* Soldier loadouts inherit Flan's classes, skins and themed names; player-model NPCs support limb hitboxes and per-piece armour
* Soldiers use cover, suppression, crouching, crawling and coordinated advances, return fire on distant attackers and search buildings for lost enemies
* Medics, engineers, ammunition bearers and anti-tank specialists support their squads; soldiers use grenades, mines, remote charges and supply bags according to their role
* Configurable skill levels govern reaction time, accuracy, aiming and damage to players; optional blood effects and retained corpses accompany combat
* Soldiers can drive and ride real Flan vehicles, deploy carried guns and man AA emplacements; allied players take priority when requesting occupied seats

## Vehicles, Aircraft & Ships

* NPC models support Flan vehicles, aircraft, ships, mechas and AA guns, with part hitboxes, armour, weapon groups and damage effects
* Vehicle crews operate independent seat guns, dismount to fight and bail out with parachutes; players can board and change seats, or walk on moving hulls
* Vehicles select ammunition for their target, seek better firing angles, avoid their own blast and break contact when out of ammunition; crew and mounted weapons respect operator skill
* Ground vehicles follow hull-width paths around obstacles, align with terrain and lose mobility when critical parts are destroyed
* Aircraft support fighter, bomber and ground-attack roles, interception, strafing and terrain-aware bomb releases that favour closer hits while retaining practical drop opportunities; crippled aircraft crash with matching effects and crew evacuation
* Ships support coastal bombardment, formations and torpedo runs, with naval steering and flooding; submarines choose cruising, combat and evasion depths
* Mechas use both arm tools, melee weapons, rocket packs and supported utility addons

## Teams Bots, Factions & Battles

* NPC armies fight Teams rounds with squads, transports, class roles and player orders through the Squad orders key (O)
* AI commanders distribute forces across objectives and coordinate assaults, covering fire, scouting and withdrawals; optional commander bots disrupt the enemy's strategy when killed
* Command centre pages manage infantry, vehicle allocations, crews and squads, with searchable previews, class limits, respawn controls and reusable JSON presets
* Vehicle deployments respect spawner restrictions and cooldowns, wait for clear space and support waves or sequential spawning
* Factions support alliances, player membership and configurable friendly fire; skirmishes and persistent wars extend battles beyond Teams rounds
* Battle difficulty and behaviour profiles are configurable independently of world difficulty; NPCs can fight players on Peaceful, and optional Cinematic mode protects players while filming
* Teams gamemodes decide victory; bot tickets only limit respawns outside objective modes. The separate `playertickets` command and player-ticket toggle are removed

## Configuration & Performance

* The Wolff's Mod tab in the Custom NPCs editor provides presets, model previews, combat and movement settings, diagnostics and scripting controls
* Configurable AI update distances, asynchronous pathfinding and adaptive search budgets keep large battles responsive
* Shared target searches and cached weapon statistics reduce server cost while preserving weapon timing and vehicle physics
