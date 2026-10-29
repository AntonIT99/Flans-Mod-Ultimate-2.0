# Flan's Ultimate 2: NPC Vehicles & Soldiers 1.0

First release. Requires Flan's Mod Ultimate 2.2 or newer and Custom NPCs.

## Soldiers

* Custom NPCs use Flan's guns, grenades, throwables, melee weapons, shields and armour, with reloads, ammunition from their inventory and Flan's sounds and muzzle flashes; the sounds an NPC makes carry as far, and as loudly, as the same weapon in a player's hands
* Soldier class loadouts from Flan's teams and classes, with skins, themed name lists and naming by class and gender
* Flan NPC tactics for soldiers, vehicles and aircraft, including cover, peeking and Custom NPCs' own tactics with Flan's weapons
* NPCs throw what they carry by per-kind strategies, and open carried parachutes when they fall
* Presets in the Wolff's Mod tab, such as Mob-like and Player-like, with undo; presets preserve the existing `Return to start` setting
* Wandering range `0` allows unlimited roaming without triggering `Return to start` for distance; returning after combat and on respawn is preserved

## Vehicles, Aircraft & Mechas

* NPCs drawn as Flan's vehicles, planes, helicopters, ships, mechas and AA guns move, fight, sound and take damage like their driveables, with part hitboxes, armour and HEAT overrides; destroyed planes and helicopters fall under gravity; vehicle muzzle flashes replace the fallback flame particle while preserving authored shooting particles
* NPCs board, drive and fly the Flan's driveables players place, lead their targets, land planes and watch their fuel
* Vehicle NPCs carry crews in their seats; crewed seat guns pick their own targets, and crew can get out to fight or jump by parachute
* Seated crew face ahead of their vehicle or turret, while gunners turn with their gun as players do and, on vehicle NPCs, move round the gun's pivot
* Friendly players can ride in passenger seats, occupy empty driver seats and man the gunner seats of vehicle NPCs, and change seats with Flan's change-seat key; the NPC still drives when a player occupies its driver seat
* NPC vehicles manoeuvre for a shot, back away from targets under their barrel and can be repaired
* Vehicles, aircraft and ships keep clear of their own blast: they hold their fire at a target close enough for their explosive rounds to catch them and pull back out of reach first (`Keep clear of own blast`, `Blast safety margin`)
* Ships run in bow on to fire torpedoes and anything else fixed to their hull, turn away after each run, and circle their target so the turrets along their side bear once they have none left (`Ships turn to bear`)
* Players can stand on the solid hull and turret of an NPC vehicle, which carries them smoothly as it drives and turns without running them over or pushing them off; carrying also works at the bows and sterns of large ships
* The Guard job's target lists leave out Flan's driveables and NPC models, and chosen targets leave Available Targets

## Teams Bots, Factions & Battles

* Teams bots: NPC soldiers, squads and vehicles that fight Teams rounds, with player orders, a commander seat, transports, class roles, suppression and squad leading
* Manage Custom NPCs factions for battles: branches of Flan's teams, alliances, wars and peace between factions
* Players can join and leave factions; factions are invite-only by default, and operators choose who may join, the leave penalty and the rejoin cooldown
* `Attack Mobs` in Global > Factions: every NPC of the faction hunts hostile mobs on sight, slimes and ghasts included, without needing the Guard job
* Skirmishes between factions outside Teams rounds, and endless wars over chunk territory
* Factions, Skirmishes, Wars and Bots pages in the Teams command centre
* Configurable friendly fire between NPCs, players and allies (`wolffsmodnpcs-common.toml`, `/flansnpcfriendlyfire`, Server > Rules > Friendly fire in the Wolff's Mod tab)

## Tools

* The Wolff's Mod tab in the Custom NPCs editor, with combat, crew, movement, naming, model and server pages
* Flan model picker with previews and tooltips
* NPC diagnosis, server limits, rules, census and benchmarks
* Flan's debug mode marks the seats and seat-gun muzzles of vehicle NPCs, as it does for driveables
* Server > Rules in the Wolff's Mod tab can turn off item drops from killed NPCs; their experience still drops either way
* Scripting controls for Flan's features
