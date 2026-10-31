# Flan's Ultimate 2: NPC Vehicles & Soldiers 1.0

First release. Requires Flan's Mod Ultimate 2.2 or newer and Custom NPCs.

## Soldiers

* Custom NPCs use Flan's guns, grenades, throwables, melee weapons, shields and armour, with reloads, ammunition from their inventory and Flan's sounds and muzzle flashes; the sounds an NPC makes carry as far, and as loudly, as the same weapon in a player's hands
* Soldier class loadouts from Flan's teams and classes, with skins, themed name lists and naming by class and gender
* Flan NPC tactics for soldiers, vehicles and aircraft, including cover, peeking and Custom NPCs' own tactics with Flan's weapons
* NPCs throw what they carry by per-kind strategies, and open carried parachutes when they fall
* NPCs with Custom NPCs' crawling animation fire with their gun's prone accuracy and hold their gun forward along the body
* NPCs drawn with the player model are hit limb by limb by Flan rounds like players: headshots and the other hit zones use the same damage modifiers, worn armour protects the part it covers, and rounds passing between the limbs miss. `Limb hitboxes` (Protection > General) turns this off for a single NPC
* NPCs always have their full-size hitbox: Custom NPCs left NPCs created on the server, such as Teams bots, a box one block high there, and NPCs that got off a vehicle a box 77% as high, so rounds, arrows and melee aimed at the chest or head passed over them
* Presets in the Wolff's Mod tab, such as Mob-like and Player-like, with undo; presets preserve the existing `Return to start` setting
* NPCs look for and keep targets out to their aggro range instead of only Custom NPCs' `NpcNavRange`, so planes and helicopters flying high above their targets spot and hold them; aircraft models fight no faster than lets them come round within their aggro range, so fast planes no longer fly off out of the simulated area; planes fly straight on to gain room when their target is too close beside them to turn onto, rather than circling it, and keep their target while they come round; planes and helicopters that see their goal head straight for it instead of searching a path through the air, and `Efficient path searches` covers flying NPCs too, so a single bomber no longer stalls the server
* Wandering range `0` allows unlimited roaming without triggering `Return to start` for distance; returning after combat and on respawn is preserved
* `Teleport home when inactive` (Advanced > Performance & debug > Activity): NPCs with `Return to start` left away from home outside the simulation distance, or resting beyond their dormant range, are sent straight back once no player is near enough to see them go

## Vehicles, Aircraft & Mechas

* NPCs drawn as Flan's vehicles, planes, helicopters, ships, mechas and AA guns move, fight, sound and take damage like their driveables, with part hitboxes, armour and HEAT overrides; destroyed planes and helicopters fall under gravity; vehicle muzzle flashes replace the fallback flame particle while preserving authored shooting particles
* NPCs board, drive and fly the Flan's driveables players place, lead their targets, land planes and watch their fuel
* Vehicle NPCs carry crews in their seats; seat guns use normal magazine reload delays and sounds, crewed seat guns pick their own targets, and crew can get out to fight or jump by parachute
* Seated crew keep their body aligned with their seat through rendering and turns across 180 degrees; gunners turn with their gun as players do and, on vehicle NPCs, move round the gun's pivot; NPCs riding normal aircraft inherit pitch and roll like players
* Friendly players can right-click any empty seat at its position to board vehicle NPCs, including seats outside the central hitbox, occupy empty driver seats and man gunner seats, and change seats with Flan's change-seat key; the NPC still drives when a player occupies its driver seat
* NPC vehicles manoeuvre for a shot, back away from targets under their barrel and can be repaired
* Vehicles, aircraft and ships keep clear of their own blast: they hold their fire at a target close enough for their explosive rounds to catch them and pull back out of reach first (`Keep clear of own blast`, `Blast safety margin`)
* Ships run in bow on to fire torpedoes and anything else fixed to their hull, turn away after each run, and circle their target so the turrets along their side bear once they have none left (`Ships turn to bear`)
* Vehicle, plane and mecha NPCs fire both weapon groups of their model by default (`Fire both weapon groups`): the group beside the main attack fires its guns, shells, rockets and bombs at its own cadence, and its bombs fall only on vehicles and other large targets on the ground, which planes approach in a level bomb run; a model whose chosen group is empty, such as a bomber that only carries bombs, attacks with its other group
* Players can stand and walk on the solid hull and turret of an NPC vehicle, which carries them smoothly without running them over or pushing them off; fixed sudden turns and drops from unstable carry transforms, including on large ships, and replayed movement when server time is synchronized
* The Guard job's target lists leave out Flan's driveables and NPC models, and chosen targets leave Available Targets

## Teams Bots, Factions & Battles

* Teams soldiers follow the round regeneration rule and heal half a heart every five seconds by default; medical healing and vehicle repairs remain independent

* Teams bots: NPC soldiers, squads and vehicles that fight Teams rounds, with player orders (`Squad orders` key, O), a commander seat, transports, class roles (a medic heals and an engineer repairs once every five seconds, and the same ally or vehicle at most every ten seconds), every class of the team fielded in proportion to its weight, grenades thrown at any enemy in safe reach, suppression and squad leading; bots approach CTF flags at ground level even on taller poles
* Manage Custom NPCs factions for battles: branches of Flan's teams, alliances, wars and peace between factions
* Players can join and leave factions; factions are invite-only by default, and operators choose who may join, the leave penalty and the rejoin cooldown
* `Attack Mobs` in Global > Factions: every NPC of the faction hunts hostile mobs on sight, slimes and ghasts included, without needing the Guard job
* Skirmishes between factions outside Teams rounds, and endless wars over chunk territory
* Factions, Skirmishes, Wars and Bots pages in the Teams command centre
* Configurable friendly fire between NPCs, players and allies (`wolffsmodnpcs-common.toml`, `/flansnpcfriendlyfire`, Server > Rules > Friendly fire in the Wolff's Mod tab)

## Tools

* The Wolff's Mod tab in the Custom NPCs editor, with combat, crew, movement, naming, model and server pages
* Flan model picker with previews and tooltips
* NPC diagnosis, including a flying plane's speed and circle radius, server limits, rules, census and benchmarks
* Flan's debug mode marks the seats and seat-gun muzzles of vehicle NPCs, as it does for driveables
* Killed NPCs drop no items unless Server > Rules in the Wolff's Mod tab turns item drops on; Teams bots then drop what the Teams `weaponDrops` and `armourDrops` rules let a dying player drop. Their experience still drops either way
* Scripting controls for Flan's features
