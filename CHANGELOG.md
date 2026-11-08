# Flan's Mod Ultimate 2.2

## Teams

* New Teams command centre (F7 or `/teams admin`) for rounds, maps, rules and server settings, with reusable presets and JSON import/export
* Expanded round rules for respawn waves, limited lives, overtime, headquarters protection, vehicle limits and warm-up, plus multi-team alliances and attackers-versus-defenders battles
* Spawn protection, configurable player and bot health and regeneration, and optional retained bot corpses
* Optional map restoration after rounds (`/teams restoreBlocks true`), including block entities and changes saved across server restarts
* Improved deployment map with terrain previews and boarding through vehicle spawners; spawners support fuel, ammunition, NPC allocations and shared deployment delays
* Unified Teams menu (U), clearer objective and team flags, and end-of-round results with personal scores
* Restored legacy Teams commands and rules; `/teams stop` and `/teams restart` clean up round wrecks, and round objectives and spawners are protected from indirect destruction

## Vehicles, Aircraft & Ships

* Progressive aircraft damage affects lift, control, engines, fuel and rotors, with pilot warnings, autorotation, belly landings and distinct crash behaviour
* Driveables leave persistent burning wrecks, with detached parts, terrain-aware settling and ship sinking; wreck lifetime is configurable through `driveableWreckLifetime`
* Reworked vehicle armour and shell penetration, including damage through destroyed parts, composite armour, HESH and corrected HEAT damage against unarmoured targets
* Improved ship physics and waterline placement, vehicle suspension, air brakes and aircraft engine performance
* Vehicle repairs and carrier servicing, including repair tools, part-health readouts and repair progress; damaged AA guns can be repaired, while destroyed ones leave unusable wrecks
* Expanded mounted weapons with passenger optics, multi-barrel guns, optional overheating and improved firing effects
* Fixed vehicle collision and fall damage scaling, incorrect self-damage against obstacles, and unstable movement for players standing on moving hulls and turrets

## Weapons & Combat

* New prone stance (Z), with matching hitboxes, improved accuracy and stronger recoil control
* Charged overhand and underhand grenade throws, tactical reloads, variable-zoom scopes and toggleable bayonets
* Expanded ammunition features include mixed tracer belts, depth charges and proportional-navigation missiles
* New combat effects include blood splatters, suppression, camera recoil, thermal vision, fly-by sounds and distant explosions
* Improved hit registration against moving players and NPCs; fixed shots blocked by invisible fence geometry or lost to damage cooldowns, and bullet damage outside Teams rounds
* Rebalanced weapon recoil, firing modes and armour penetration; ammunition provisioning selects compatible rounds from the weapon's own pack and supports mixed vehicle loads
* Fixed reload crashes, lingering firing animations, ammunition stack merging and supply-bag persistence; parachutes now carry their riders safely to the ground

## Rendering, Performance & Settings

* Major rendering improvements for large battles, including cached vehicle, weapon and armour meshes, armour level of detail and cheaper distant wrecks
* Improved Oculus/Iris shader compatibility, Sodium/Embeddium rendering and EntityCulling support
* Faster content loading, creative search and server weapon handling, with clearer loading progress and reusable model caches
* Reworked explosions, tactical smoke, muzzle effects and vehicle damage effects, with configurable visual intensity and quick-access controls
* Improved first-person gun placement, sights and attachment animations, with selectable legacy view frames
* Reorganized settings with per-screen Restore Defaults; suppression and combat screen shake are now controlled by the server
* `/flandebug` is renamed to `/flansdebug`; server operators gain clearer command help and expanded health, combat and sound controls

## Content Packs & Compatibility

* New Classic and Manus Legacy packs restore earlier vehicles, aircraft, weapons and uniforms alongside their remakes
* Expanded historical and modern teams, specialist loadouts, uniforms, flags and paintjobs across the bundled and official packs
* Restored Tyrants and Plebeians gun models, keyframe animations and first-person weapon effects
* Corrected legacy vehicle weapon placement, crew seats and model rotations, plus official gun sights, missing recipes and equipment compatibility
* Optional JEI integration adds vehicle assembly and gun-box recipes; crafting and furnace recipes now unlock in Minecraft's recipe book
* New companion mod, **Flan's Ultimate 2: NPC Vehicles & Soldiers**, brings Custom NPCs soldiers and vehicles into Teams rounds, skirmishes and wars
* Expanded public addon API for combatants, vehicle crews, damage, physics and Teams integration; `FlansRepairs.navalVelocity` now requires an `atSea` argument
* Clients with mismatched content packs disconnect safely with an explanation; updated to Forge 47.4.28 while retaining support for 47.4.10 and newer
